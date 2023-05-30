/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class updates a JavaClass from resolver.
 */
public class JavaClassUpdater {

    // The JavaClass
    protected JavaClass  _javaClass;

    // The Resolver that produced this decl
    protected Resolver  _resolver;

    // A cached list of all decls
    private List<JavaDecl>  _allDecls;

    // A count of decls added in last update
    private int  _addedDecls;

    /**
     * Constructor.
     */
    public JavaClassUpdater(JavaClass aClass)
    {
        _javaClass = aClass;
        _resolver = aClass._resolver;
    }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDecls()
    {
        try { return updateDeclsImpl(); }
        catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDeclsImpl() throws SecurityException
    {
        // If first time, set decls
        if (_javaClass._fieldDecls == null)
            _javaClass._fieldDecls = new ArrayList<>();

        // Get ClassName
        String className = _javaClass.getClassName();

        // Get real class
        Class<?> realClass = _javaClass.getRealClass();
        if (realClass == null) {
            System.err.println("JavaClass: Failed to load class: " + className);
            return false;
        }

        // Set Decls from Object[] for efficiency
        if (realClass.isArray() && realClass != Object[].class) {
            updateArrayClass();
            return true;
        }

        // Create set for added/removed decls
        Set<JavaDecl> removedDecls = new HashSet<>(getAllDecls());
        _addedDecls = 0;

        // Update modifiers
        if (_javaClass.getModifiers() != realClass.getModifiers())
            _javaClass._mods = realClass.getModifiers();

        // Update interfaces
        updateInterfaces(realClass);

        // Update type variables
        updateTypeVariables(realClass, removedDecls);

        // Update inner classes
        updateInnerClasses(realClass, removedDecls);

        // Update fields
        updateFields(realClass, removedDecls);

        // Update methods
        updateMethods(realClass, removedDecls);

        // Update constructors
        updateConstructors(realClass, removedDecls);

        // Array.length: Handle this special for Object[]
        if (_javaClass.isArray() && _javaClass.getFieldForName("length") == null) {
            JavaField javaField = getLengthField();
            _javaClass._fieldDecls = Arrays.asList(javaField);
            _addedDecls++;
        }

        // Remove unused decls
        for (JavaDecl jd : removedDecls)
            removeDecl(jd);

        // Return whether decls were changed
        boolean changed = _addedDecls > 0 || removedDecls.size() > 0;
        if (changed)
            _allDecls = null;

        // Return
        return changed;
    }

    /**
     * Updates inner classes.
     */
    private void updateInterfaces(Class<?> realClass) throws SecurityException
    {
        // Get interfaces
        Class<?>[] interfaces = realClass.getInterfaces();

        // Iterate over interfaces and add
        _javaClass._interfaces = new JavaClass[interfaces.length];
        for (int i = 0, iMax = interfaces.length; i < iMax; i++) {
            Class<?> intrface = interfaces[i];
            _javaClass._interfaces[i] = _javaClass.getJavaClassForClass(intrface);
        }
    }

    /**
     * Updates inner classes.
     */
    private void updateTypeVariables(Class<?> realClass, Set<JavaDecl> removedDecls) throws SecurityException
    {
        // Get TypeVariables
        TypeVariable<?>[] typeVariables = _resolver.getTypeParametersForClass(realClass);

        // Add JavaDecl for each Type parameter
        for (TypeVariable<?> typeVariable : typeVariables) {
            String name = typeVariable.getName();
            JavaDecl decl = _javaClass.getTypeVarForName(name);
            if (decl == null) {
                decl = new JavaTypeVariable(_resolver, _javaClass, typeVariable);
                addDecl(decl);
                _addedDecls++;
            }
            else removedDecls.remove(decl);
        }
    }

    /**
     * Updates inner classes.
     */
    private void updateInnerClasses(Class<?> realClass, Set<JavaDecl> removedDecls) throws SecurityException
    {
        // Get Inner Classes
        Class<?>[] innerClasses = _resolver.getDeclaredClassesForClass(realClass);

        // Add JavaDecl for each inner class
        for (Class<?> innerClass : innerClasses) {   //if(icls.isSynthetic()) continue;
            JavaDecl decl = _javaClass.getInnerClassForName(innerClass.getSimpleName());
            if (decl == null) {
                decl = _resolver.getJavaClassForClass(innerClass);
                addDecl(decl);
                _addedDecls++;
            }
            else removedDecls.remove(decl);
        }
    }

    /**
     * Updates fields.
     */
    private void updateFields(Class<?> realClass, Set<JavaDecl> removedDecls) throws SecurityException
    {
        // Hack to use StaticResolver for browser
        if (_resolver.isTeaVM) {
            JavaField[] fields = StaticResolver.shared().getFieldsForClass(_resolver, realClass.getName());
            _javaClass._fieldDecls = Arrays.asList(fields);
            return;
        }

        // Get Fields
        Field[] fields = realClass.getDeclaredFields();

        // Add JavaDecl for each declared field - also make sure field type is in refs
        for (Field field : fields) {
            JavaDecl decl = getJavaFieldForField(field);
            if (decl == null) {
                decl = new JavaField(_resolver, _javaClass, field);
                addDecl(decl);
                _addedDecls++;
            }
            else removedDecls.remove(decl);
        }
    }

    /**
     * Updates methods.
     */
    private void updateMethods(Class<?> realClass, Set<JavaDecl> removedDecls) throws SecurityException
    {
        // Hack to use StaticResolver for browser
        if (_resolver.isTeaVM) {
            JavaMethod[] methods = StaticResolver.shared().getMethodsForClass(_resolver, realClass.getName());
            _javaClass._methDecls = Arrays.asList(methods);
            return;
        }

        // Get Methods
        Method[] methods = realClass.getDeclaredMethods();

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            JavaMethod decl = getJavaMethodForMethod(meth);
            if (decl == null) {
                decl = new JavaMethod(_resolver, _javaClass, meth);
                addDecl(decl);
                decl.initTypes(meth);
                _addedDecls++;
            }
            else removedDecls.remove(decl);
        }
    }

    /**
     * Updates constructors.
     */
    private void updateConstructors(Class<?> realClass, Set<JavaDecl> removedDecls) throws SecurityException
    {
        // Hack to use StaticResolver for browser
        if (_resolver.isTeaVM) {
            JavaConstructor[] constructors = StaticResolver.shared().getConstructorsForClass(_resolver, realClass.getName());
            _javaClass._constrDecls = Arrays.asList(constructors);
            return;
        }

        // Get Constructors
        Constructor<?>[] constructors = realClass.getDeclaredConstructors();

        // Add JavaDecl for each constructor - also make sure parameter types are in refs
        for (Constructor<?> constr : constructors) {
            if (constr.isSynthetic()) continue;
            JavaConstructor decl = getJavaConstructorForConstructor(constr);
            if (decl == null) {
                decl = new JavaConstructor(_resolver, _javaClass, constr);
                addDecl(decl);
                decl.initTypes(constr);
                _addedDecls++;
            }
            else removedDecls.remove(decl);
        }
    }

    /**
     * Updates array class.
     */
    private void updateArrayClass()
    {
        JavaClass aryDecl = _resolver.getJavaClassForClass(Object[].class);
        _javaClass._fieldDecls = aryDecl.getFields();
        _javaClass._interfaces = aryDecl._interfaces;
        _javaClass._methDecls = aryDecl._methDecls;
        _javaClass._constrDecls = aryDecl._constrDecls;
        _javaClass._innerClasses = aryDecl._innerClasses;
        _javaClass._typeVarDecls = aryDecl._typeVarDecls;
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        // If already set, just return
        if (_allDecls != null) return _allDecls;

        // Create new AllDecls cached list with decls for fields, methods, constructors, inner classes and this class
        List<JavaField> fdecls = _javaClass.getFields();
        int memberCount = fdecls.size() + _javaClass._methDecls.size() + _javaClass._constrDecls.size();
        int declCount = memberCount + _javaClass._innerClasses.size() + 1;
        List<JavaDecl> decls = new ArrayList<>(declCount);
        decls.add(_javaClass);
        decls.addAll(_javaClass._fieldDecls);
        decls.addAll(_javaClass._methDecls);
        decls.addAll(_javaClass._constrDecls);
        decls.addAll(_javaClass._innerClasses);

        // Set/return
        return _allDecls = decls;
    }

    /**
     * Returns the JavaField for java.lang.reflect.Field.
     */
    public JavaField getJavaFieldForField(Field aField)
    {
        String name = aField.getName();
        JavaField field = _javaClass.getFieldForName(name);
        if (field == null)
            return null;

        int mods = aField.getModifiers();
        if (mods != field.getModifiers())
            return null;

        // Return
        return field;
    }

    /**
     * Returns the JavaMethod for given java.lang.reflect.method.
     */
    public JavaMethod getJavaMethodForMethod(Method aMeth)
    {
        String id = ResolverUtils.getIdForMember(aMeth);
        JavaMethod method = getMethodForId(id);
        if (method == null)
            return null;

        int mods = aMeth.getModifiers();
        if (mods != method.getModifiers())
            return null;

        // Check return type?
        return method;
    }

    /**
     * Returns the JavaMethod for id string.
     */
    private JavaMethod getMethodForId(String anId)
    {
        List<JavaMethod> methods = _javaClass.getMethods();
        for (JavaMethod method : methods)
            if (method.getId().equals(anId))
                return method;

        // Return
        return null;
    }

    /**
     * Returns the JavaConstructor for java.lang.reflect.Constructor.
     */
    public JavaConstructor getJavaConstructorForConstructor(Constructor<?> aConstr)
    {
        String id = ResolverUtils.getIdForMember(aConstr);
        JavaConstructor constructor = getConstructorForId(id);
        if (constructor == null)
            return null;

        // Check mods
        int mods = aConstr.getModifiers();
        if (mods != constructor.getModifiers())
            return null;

        // Return
        return constructor;
    }

    /**
     * Returns the Constructor decl for id string.
     */
    public JavaConstructor getConstructorForId(String anId)
    {
        List<JavaConstructor> constructors = _javaClass.getConstructors();
        for (JavaConstructor constructor : constructors)
            if (constructor.getId().equals(anId))
                return constructor;

        // Return
        return null;
    }

    /**
     * Returns a JavaMember for given java.lang.reflect.Member.
     */
    public JavaMember getJavaMemberForMember(Member aMember)
    {
        // Handle Field
        if (aMember instanceof Field)
            return getJavaFieldForField((Field) aMember);

        // Handle Method
        if (aMember instanceof Method)
            return getJavaMethodForMethod((Method) aMember);

        // Handle Constructor
        if (aMember instanceof Constructor)
            return getJavaConstructorForConstructor((Constructor<?>) aMember);

        // Handle MemberName
        throw new RuntimeException("JavaClassUpdater.getJavaMemberForMember: " + aMember);
    }

    /**
     * Adds a decl.
     */
    public void addDecl(JavaDecl aDecl)
    {
        JavaDecl.DeclType type = aDecl.getType();
        switch (type) {
            case Field: _javaClass._fieldDecls.add((JavaField) aDecl); break;
            case Method: _javaClass._methDecls.add((JavaMethod) aDecl); break;
            case Constructor: _javaClass._constrDecls.add((JavaConstructor) aDecl); break;
            case Class: _javaClass._innerClasses.add((JavaClass) aDecl); break;
            case TypeVar: _javaClass._typeVarDecls.add((JavaTypeVariable) aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.addDecl: Invalid type " + type);
        }
    }

    /**
     * Removes a decl.
     */
    public void removeDecl(JavaDecl aDecl)
    {
        JavaDecl.DeclType type = aDecl.getType();
        switch (type) {
            case Field: _javaClass._fieldDecls.remove(aDecl); break;
            case Method: _javaClass._methDecls.remove(aDecl); break;
            case Constructor: _javaClass._constrDecls.remove(aDecl); break;
            case Class: _javaClass._innerClasses.remove(aDecl); break;
            case TypeVar: _javaClass._typeVarDecls.remove(aDecl); break;
            default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
        }
    }

    /**
     * Returns the length field.
     */
    private JavaField getLengthField()
    {
        JavaField.FieldBuilder fb = new JavaField.FieldBuilder();
        fb.init(_resolver, _javaClass.getClassName());
        JavaField javaField = fb.name("length").type(int.class).build();
        return javaField;
    }

    /**
     * Returns a bogus field for Array.length.
     */
    private static Field getLenField()
    {
        try { return Array.class.getField("length"); }
        catch (Exception e) { return null; }
    }

    // Bogus class to get length
    private static class Array { public int length; }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
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
        if (_javaClass._fields == null)
            _javaClass._fields = new JavaField[0];

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

        // Whether class changed
        boolean classChanged = false;

        // Update modifiers
        if (_javaClass.getModifiers() != realClass.getModifiers())
            _javaClass._mods = realClass.getModifiers();

        // Update interfaces
        JavaClass[] interfaces = getInterfaces(realClass);
        _javaClass._interfaces = interfaces;

        // Update type variables
        JavaTypeVariable[] typeVars = getTypeVariables(realClass);
        if (!ArrayUtils.equalsId(typeVars, _javaClass._typeVars))
            classChanged = true;
        _javaClass._typeVars = typeVars;

        // Update inner classes
        JavaClass[] innerClasses = getDeclaredClasses(realClass);
        if (!ArrayUtils.equalsId(innerClasses, _javaClass._innerClasses))
            classChanged = true;
        _javaClass._innerClasses = innerClasses;

        // Update fields
        JavaField[] fields = getDeclaredFields(realClass);
        if (!ArrayUtils.equalsId(fields, _javaClass._fields))
            classChanged = true;
        _javaClass._fields = fields;

        // Update methods
        JavaMethod[] oldMethods = _javaClass._methods;
        JavaMethod[] newMethods = _javaClass._methods = getDeclaredMethods(realClass);
        if (!ArrayUtils.equalsId(oldMethods, newMethods)) {
            for (JavaMethod method : newMethods)
                method.initTypes();
            classChanged = true;
        }

        // Update constructors
        JavaConstructor[] oldConstrs = _javaClass._constructors;
        JavaConstructor[] newConstrs = _javaClass._constructors = getDeclaredConstructors(realClass);
        if (!ArrayUtils.equalsId(oldConstrs, newConstrs)) {
            for (JavaConstructor constr : newConstrs)
                constr.initTypes();
            classChanged = true;
        }

        // Array.length: Handle this special for Object[]
        if (_javaClass.isArray() && _javaClass.getFieldForName("length") == null) {
            JavaField javaField = getLengthField();
            _javaClass._fields = new JavaField[] { javaField };
            classChanged = true;
        }

        // Return whether decls were changed
        if (classChanged)
            _allDecls = null;

        // Return
        return classChanged;
    }

    /**
     * Returns interfaces.
     */
    private JavaClass[] getInterfaces(Class<?> realClass)
    {
        Class<?>[] interfaces = realClass.getInterfaces();
        return ArrayUtils.map(interfaces, cls -> _javaClass.getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns JavaTypeVariable array for given class TypeVariables.
     */
    private JavaTypeVariable[] getTypeVariables(Class<?> realClass)
    {
        TypeVariable<?>[] typeVariables = realClass.getTypeParameters();
        return ArrayUtils.map(typeVariables, tvar -> getJavaTypeVarForTypeVar(tvar), JavaTypeVariable.class);
    }

    /**
     * Returns a JavaTypeVariable for given TypeVariable from JavaClass, creating if missing.
     */
    private JavaTypeVariable getJavaTypeVarForTypeVar(TypeVariable<?> aTypeVar)
    {
        String typeVarName = aTypeVar.getName();
        JavaTypeVariable javaTypeVar = _javaClass.getTypeVarForName(typeVarName);
        if (javaTypeVar == null)
            javaTypeVar = new JavaTypeVariable(_resolver, _javaClass, aTypeVar);
        return javaTypeVar;
    }

    /**
     * Returns JavaClass array of declared inner classes for given class.
     */
    private JavaClass[] getDeclaredClasses(Class<?> realClass)
    {
        // Get Inner Classes
        Class<?>[] innerClasses;
        try { innerClasses = realClass.getDeclaredClasses(); }
        catch (Throwable e) {
            System.err.println("JavaClassUpdater.getDeclaredClasses: Can't get declared classes: " + e);
            innerClasses = new Class[0];
        }

        // Add JavaDecl for each inner class
        return ArrayUtils.map(innerClasses, cls -> getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns a JavaClass for given inner Class from JavaClass, creating if missing.
     */
    private JavaClass getJavaClassForClass(Class<?> anInnerClass)
    {
        String innerClassName = anInnerClass.getSimpleName();
        JavaClass innerClass = _javaClass.getInnerClassForName(innerClassName);
        if (innerClass == null)
            innerClass = _resolver.getJavaClassForClass(anInnerClass);
        return innerClass;
    }

    /**
     * Returns JavaField array for given class.
     */
    private JavaField[] getDeclaredFields(Class<?> realClass)
    {
        Field[] fields = realClass.getDeclaredFields();
        return ArrayUtils.map(fields, field -> getJavaFieldForField(field), JavaField.class);
    }

    /**
     * Returns a JavaField for given Field from JavaClass, creating if missing.
     */
    private JavaField getJavaFieldForField(Field aField)
    {
        JavaField javaField = _javaClass.getJavaFieldForField(aField);
        if (javaField == null)
            javaField = new JavaField(_resolver, _javaClass, aField);
        return javaField;
    }

    /**
     * Returns JavaMethod array for given class.
     */
    private JavaMethod[] getDeclaredMethods(Class<?> realClass)
    {
        Method[] methods = realClass.getDeclaredMethods();
        return ArrayUtils.map(methods, method -> getJavaMethodForMethod(method), JavaMethod.class);
    }

    /**
     * Returns a JavaMethod for given Method from JavaClass, creating if missing.
     */
    private JavaMethod getJavaMethodForMethod(Method aMethod)
    {
        JavaMethod javaMethod = _javaClass.getJavaMethodForMethod(aMethod);
        if (javaMethod == null)
            javaMethod = new JavaMethod(_resolver, _javaClass, aMethod);
        return javaMethod;
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    private JavaConstructor[] getDeclaredConstructors(Class<?> realClass)
    {
        Constructor<?>[] constrs = realClass.getDeclaredConstructors();
        return ArrayUtils.map(constrs, constr -> getJavaConstructorForConstructor(constr), JavaConstructor.class);
    }

    /**
     * Returns a JavaConstructor for given Method from JavaClass, creating if missing.
     */
    private JavaConstructor getJavaConstructorForConstructor(Constructor<?> aConstr)
    {
        JavaConstructor javaConstr = _javaClass.getJavaConstructorForConstructor(aConstr);
        if (javaConstr == null)
            javaConstr = new JavaConstructor(_resolver, _javaClass, aConstr);
        return javaConstr;
    }

    /**
     * Updates array class.
     */
    private void updateArrayClass()
    {
        JavaClass aryDecl = _resolver.getJavaClassForClass(Object[].class);
        _javaClass._fields = aryDecl.getDeclaredFields();
        _javaClass._interfaces = aryDecl._interfaces;
        _javaClass._methods = aryDecl._methods;
        _javaClass._constructors = aryDecl._constructors;
        _javaClass._innerClasses = aryDecl._innerClasses;
        _javaClass._typeVars = aryDecl._typeVars;
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        // If already set, just return
        if (_allDecls != null) return _allDecls;

        // Create new AllDecls cached list with decls for fields, methods, constructors, inner classes and this class
        JavaField[] fields = _javaClass.getDeclaredFields();
        int memberCount = fields.length + _javaClass._methods.length + _javaClass._constructors.length;
        int declCount = memberCount + _javaClass._innerClasses.length + 1;
        List<JavaDecl> decls = new ArrayList<>(declCount);
        decls.add(_javaClass);
        Collections.addAll(decls, _javaClass._fields);
        Collections.addAll(decls, _javaClass._methods);
        Collections.addAll(decls, _javaClass._constructors);
        Collections.addAll(decls, _javaClass._innerClasses);

        // Set/return
        return _allDecls = decls;
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
}

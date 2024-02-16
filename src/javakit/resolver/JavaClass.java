/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.JClassDecl;
import javakit.parse.JFile;
import snap.util.ArrayUtils;
import java.lang.reflect.*;

/**
 * This JavaType subclass represents a java.lang.Class.
 */
public class JavaClass extends JavaType {

    // The package (if root level class)
    private JavaPackage  _package;

    // The Declaring class (if member of enclosing class)
    private JavaClass  _declaringClass;

    // The super class type (could be ParameterizedType)
    private JavaType  _superType;

    // The super class name
    protected String _superClassName;

    // The super class
    protected JavaClass  _superClass;

    // The modifiers
    protected int  _mods;

    // Whether class decl is enum, interface, primitive
    private boolean  _enum, _interface, _primitive;

    // The array of interfaces
    protected JavaClass[] _interfaces;

    // The type var decls
    protected JavaTypeVariable[] _typeVars;

    // The field decls
    protected JavaField[] _fields;

    // The method decls
    protected JavaMethod[] _methods;

    // The constructor decls
    protected JavaConstructor[] _constructors;

    // The inner class decls
    protected JavaClass[] _innerClasses;

    // The array of enum constants (enum only)
    private Object[] _enumConstants;

    // The Array component type name (if Array)
    private String _componentTypeName;

    // The Array component type (if Array)
    private JavaClass  _componentType;

    // The real class
    protected Class<?> _realClass;

    // The updater
    private JavaClassUpdater  _updater;

    /**
     * Constructor.
     */
    public JavaClass(Resolver aResolver, JavaDecl declaringPkgOrClass, Class<?> aClass)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set real class
        _realClass = aClass;

        // Set Id, Name, SimpleName
        _id = _name = ResolverUtils.getIdForClass(aClass);
        _simpleName = aClass.getSimpleName();

        // Set DeclaringClass or Package
        if (declaringPkgOrClass instanceof JavaClass) {
            _declaringClass = (JavaClass) declaringPkgOrClass;
            _package = _declaringClass.getPackage();
        }
        else if (declaringPkgOrClass instanceof JavaPackage)
            _package = (JavaPackage) declaringPkgOrClass;

        // Add to decls
        aResolver._classes.put(_id, this);
        if (aClass.isArray()) {
            String altName = aClass.getName();
            if (!altName.equals(_id))
                aResolver._classes.put(altName, this);
        }

        // Set Mods, Enum, Interface, Primitive
        _mods = aClass.getModifiers();
        _enum = aClass.isEnum();
        _interface = aClass.isInterface();
        _primitive = aClass.isPrimitive();

        // If array, configure special
        if (aClass.isArray()) {
            Class<?> compClass = aClass.getComponentType();
            _componentTypeName = compClass.getName();
            configureArrayClass();
        }

        // Create/set updater
        else _updater = new JavaClassUpdater(this);

        // Get type super type and set in decl
        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null)
            _superClassName = superClass.getName();
    }

    /**
     * Constructor from JClassDecl.
     */
    public JavaClass(Resolver aResolver, JClassDecl aClassDecl)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set Id, Name, SimpleName
        _id = _name = aClassDecl.getClassName();
        _simpleName = aClassDecl.getSimpleName();

        // Set DeclaringClass or Package
        JClassDecl enclosingClass = aClassDecl.getEnclosingClassDecl();
        if (enclosingClass != null) {
            _declaringClass = enclosingClass.getJavaClass();
            _package = _declaringClass.getPackage();
        }
        else {
            JFile jFile = aClassDecl.getFile();
            String pkgName = jFile.getPackageName();
            if (pkgName == null) pkgName = "";
            _package = aResolver.getJavaPackageForName(pkgName);
        }

        // Add to decls
        aResolver._classes.put(_id, this);

        // Set Mods, Enum, Interface, Primitive
        _mods = aClassDecl.getModifiers().getValue();
        _mods |= Modifier.PUBLIC; // Lame - but used for NodeCompleter until it handles current class properly
        _enum = aClassDecl.isEnum();
        _interface = aClassDecl.isInterface();

        // Create/set updater
        _updater = new JavaClassUpdaterDecl(this, aClassDecl);

        // Get type super type and set in decl
        _superClass = aClassDecl.getSuperClass();
        if (_superClass != null)
            _superClassName = _superClass.getName();
    }

    /**
     * Returns the package that declares this class.
     */
    public JavaPackage getPackage()  { return _package; }

    /**
     * Returns the class that contains this class (if inner class).
     */
    public JavaClass getDeclaringClass()  { return _declaringClass; }

    /**
     * Override to return bounding class.
     */
    @Override
    public JavaClass getEvalClass()  { return this; }

    /**
     * Returns the class name.
     */
    @Override
    public String getClassName()  { return _name; }

    /**
     * Override to return as Class type.
     */
    public JavaType getSuperType()
    {
        // If already set or Object.class, just return
        if (_superType != null) return _superType;

        // Get super class (just return if null)
        JavaClass superClass = getSuperClass();
        if (superClass == null)
            return null;

        // Get GenericSuperClass as JavaType
        Class<?> realClass = getRealClass();
        Type superType = realClass.getGenericSuperclass();
        JavaType javaType = superType != null ? _resolver.getJavaTypeForType(superType) : null;

        // Set, return
        return _superType = javaType;
    }

    /**
     * Override to return as Class type.
     */
    public JavaClass getSuperClass()
    {
        if (_superClass != null) return _superClass;
        if (_superClassName == null)
            return null;

        JavaClass superClass = _resolver.getJavaClassForName(_superClassName);
        return _superClass = superClass;
    }

    /**
     * Returns the class this decl evaluates to when referenced.
     */
    public Class<?> getRealClass()  { return _realClass; }

    /**
     * Returns the modifiers.
     */
    public int getModifiers()  { return _mods; }

    /**
     * Returns whether decl is static.
     */
    public boolean isStatic()  { return Modifier.isStatic(_mods); }

    /**
     * Returns whether class is anonymous class.
     */
    public boolean isAnonymousClass()  { return _simpleName.length() == 0; }

    /**
     * Returns whether class is member.
     */
    public boolean isMemberClass()  { return _declaringClass != null; }

    /**
     * Returns whether is a enum reference.
     */
    public boolean isEnum()  { return _enum; }

    /**
     * Returns whether is a interface reference.
     */
    public boolean isInterface()  { return _interface; }

    /**
     * Returns whether is an array.
     */
    public boolean isArray()  { return _componentTypeName != null; }

    /**
     * Returns the Array component type (if Array).
     */
    public JavaClass getComponentType()
    {
        if (_componentType != null) return _componentType;
        if (_componentTypeName == null)
            return null;
        JavaClass componentType = _resolver.getJavaClassForName(_componentTypeName);
        return _componentType = componentType;
    }

    /**
     * Returns whether is primitive.
     */
    public boolean isPrimitive()
    {
        return _primitive;
    }

    /**
     * Returns the interfaces this class implements.
     */
    public JavaClass[] getInterfaces()
    {
        if (_interfaces != null) return _interfaces;
        return _interfaces = _updater.getInterfaces();
    }

    /**
     * Returns the type variables.
     */
    public JavaTypeVariable[] getTypeVars()
    {
        if (_typeVars != null) return _typeVars;
        return _typeVars = _updater.getTypeVariables();
    }

    /**
     * Returns the fields.
     */
    public JavaField[] getDeclaredFields()
    {
        if (_fields != null) return _fields;
        return _fields = _updater.getDeclaredFields();
    }

    /**
     * Returns the methods.
     */
    public JavaMethod[] getDeclaredMethods()
    {
        if (_methods != null) return _methods;
        return _methods = _updater.getDeclaredMethods();
    }

    /**
     * Returns the Constructors.
     */
    public JavaConstructor[] getDeclaredConstructors()
    {
        if (_constructors != null) return _constructors;
        return _constructors = _updater.getDeclaredConstructors();
    }

    /**
     * Returns the inner classes.
     */
    public JavaClass[] getDeclaredClasses()
    {
        if (_innerClasses != null) return _innerClasses;
        return _innerClasses = _updater.getDeclaredClasses();
    }

    /**
     * Returns the enum constants.
     */
    public Object[] getEnumConstants()
    {
        if (_enumConstants != null || !isEnum()) return _enumConstants;
        return _enumConstants = _updater.getEnumConstants();
    }

    /**
     * Returns a field for field name declared in this class.
     */
    public JavaField getDeclaredFieldForName(String aName)
    {
        JavaField[] fields = getDeclaredFields();
        return ArrayUtils.findMatch(fields, field -> field.getName().equals(aName));
    }

    /**
     * Returns a constructor for parameter types declared in this class.
     */
    public JavaConstructor getDeclaredConstructorForTypes(JavaType[] theTypes)
    {
        JavaConstructor[] constructors = getDeclaredConstructors();
        return ArrayUtils.findMatch(constructors, constr -> isTypesEqual(constr.getParameterTypes(), theTypes));
    }

    /**
     * Returns a method for method name and parameter types declared in this class.
     */
    public JavaMethod getDeclaredMethodForNameAndTypes(String aName, JavaType[] theTypes)
    {
        JavaMethod[] methods = getDeclaredMethods();
        for (JavaMethod method : methods) {
            if (method.getName().equals(aName)) {
                JavaType[] methodParamTypes = method.getParameterTypes();
                if (isTypesEqual(methodParamTypes, theTypes))
                    return method;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns a class for inner class simple name declared in this class.
     */
    public JavaClass getDeclaredClassForName(String aName)
    {
        JavaClass[] innerClasses = getDeclaredClasses();
        return ArrayUtils.findMatch(innerClasses, cls -> cls.getSimpleName().equals(aName));
    }

    /**
     * Returns a field for field name from this class or any superclass.
     */
    public JavaField getFieldDeepForName(String aName)
    {
        // Check for declared field of this class
        JavaField field = getDeclaredFieldForName(aName);
        if (field != null)
            return field;

        // Check superclass
        JavaClass superClass = getSuperClass();
        if (superClass != null)
            field = superClass.getFieldDeepForName(aName);

        // Return
        return field;
    }

    /**
     * Returns a constructor for parameter types from this class or any superclass.
     */
    public JavaConstructor getConstructorDeepForTypes(JavaType[] theTypes)
    {
        // Check for declared constructor of this class
        JavaConstructor constr = getDeclaredConstructorForTypes(theTypes);
        if (constr != null)
            return constr;

        // Check superclass
        JavaClass superClass = getSuperClass();
        if (superClass != null)
            constr = superClass.getConstructorDeepForTypes(theTypes);

        // Return
        return constr;
    }

    /**
     * Returns a method for method name and parameter types from this class or any superclass/interface.
     */
    public JavaMethod getMethodDeepForNameAndTypes(String aName, JavaType[] theTypes)
    {
        // Check for declared constructor of this class
        JavaMethod method = getDeclaredMethodForNameAndTypes(aName, theTypes);
        if (method != null)
            return method;

        // Check superclass
        JavaClass superClass = getSuperClass();
        if (superClass != null)
            method = superClass.getMethodDeepForNameAndTypes(aName, theTypes);

        // Return
        return method;
    }

    /**
     * Returns a class for class name from this class or any superclass.
     */
    public JavaClass getClassDeepForName(String aName)
    {
        // Check for declared class of this class
        JavaClass cls = getDeclaredClassForName(aName);
        if (cls != null)
            return cls;

        // Check super classes
        JavaClass superClass = getSuperClass();
        if (superClass != null)
            cls = superClass.getClassDeepForName(aName);

        // Check interfaces

        // Return
        return cls;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        JavaTypeVariable[] typeVars = getTypeVars();
        return ArrayUtils.findMatch(typeVars, tvar -> tvar.getName().equals(aName));
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public int getTypeVarIndexForName(String aName)
    {
        JavaTypeVariable[] typeVars = getTypeVars();
        return ArrayUtils.findMatchIndex(typeVars, tvar -> tvar.getName().equals(aName));
    }

    /**
     * Returns the lambda method (or null if this class isn't a functional interface).
     */
    public JavaMethod getLambdaMethod()
    {
        // If not interface, return null
        if (!isInterface()) {
            System.err.println("JavaClass.getLambdaMethod: Request for lambda method from non interface class: " + getClassName());
            return null;
        }

        // Find lambda method
        JavaMethod[] methods = getDeclaredMethods();
        return ArrayUtils.findMatch(methods, method -> !(method.isStatic() || method.isDefault()));
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    public boolean isAssignableFrom(JavaClass otherClass)
    {
        // If this class is primitive, forward to primitive version
        if (isPrimitive())
            return isAssignablePrimitive(otherClass);

        // If given class is null or this class is Object return true
        if (otherClass == null)
            return true;
        if (getName().equals("java.lang.Object"))
            return true;

        // If other class is primitive, promote to boxed class
        if (otherClass.isPrimitive())
            otherClass = otherClass.getPrimitiveAlt();

        // If one is array and other isn't, return false
        if (isArray() != otherClass.isArray())
            return false;

        // If both classes are array, check component types instead
        if (isArray()) {
            JavaClass compType = getComponentType();
            JavaClass otherCompType = otherClass.getComponentType();
            return compType.isAssignableFrom(otherCompType);
        }

        // Iterate up given class superclasses and check class and interfaces
        for (JavaClass cls = otherClass; cls != null; cls = cls.getSuperClass()) {

            // If classes match, return true
            if (cls == this)
                return true;

            // If any interface of this decl match, return true
            if (isInterface()) {
                for (JavaClass infc : cls.getInterfaces()) {
                    if (isAssignableFrom(infc))
                        return true;
                }
            }
        }

        // Return false since no match found
        return false;
    }

    /**
     * Returns whether given class is assignable to this class.
     */
    private boolean isAssignablePrimitive(JavaClass otherClass)
    {
        if (otherClass == null)
            return false;
        JavaClass otherPrimitive = otherClass.getPrimitive();
        if (otherPrimitive == null)
            return false;
        JavaDecl common = getCommonAncestorPrimitive(otherPrimitive);
        return common == this;
    }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitive()
    {
        if (isPrimitive())
            return this;

        // Handle primitive types
        switch (_name) {
            case "java.lang.Boolean": return getJavaClassForClass(boolean.class);
            case "java.lang.Byte": return getJavaClassForClass(byte.class);
            case "java.lang.Character": return getJavaClassForClass(char.class);
            case "java.lang.Short": return getJavaClassForClass(short.class);
            case "java.lang.Integer": return getJavaClassForClass(int.class);
            case "java.lang.Long": return getJavaClassForClass(long.class);
            case "java.lang.Float": return getJavaClassForClass(float.class);
            case "java.lang.Double": return getJavaClassForClass(double.class);
            case "java.lang.Void": return getJavaClassForClass(void.class);
            default: return null;
        }
    }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitiveAlt()
    {
        if (!isPrimitive())
            return this;

        // Handle primitive types
        switch (_name) {
            case "boolean": return getJavaClassForClass(Boolean.class);
            case "byte": return getJavaClassForClass(Byte.class);
            case "char": return getJavaClassForClass(Character.class);
            case "short": return getJavaClassForClass(Short.class);
            case "int": return getJavaClassForClass(Integer.class);
            case "long": return getJavaClassForClass(Long.class);
            case "float": return getJavaClassForClass(Float.class);
            case "double": return getJavaClassForClass(Double.class);
            case "void": return getJavaClassForClass(Void.class);
            default: return null;
        }
    }

    /**
     * Returns a ParamType decl for this base class and given types ( This<typ,type>).
     */
    public JavaType getParameterizedTypeForTypes(JavaType ... theTypes)
    {
        return _resolver.getJavaParameterizedTypeForTypes(this, theTypes);
    }

    /**
     * Returns a resolved type for given TypeVar.
     */
    @Override
    public JavaType getResolvedTypeForTypeVariable(JavaTypeVariable aTypeVar)
    {
        // If has type var, return bounds type
        String typeVarName = aTypeVar.getName();
        JavaDecl typeVar = getTypeVarForName(typeVarName);
        if (typeVar != null)
            return typeVar.getEvalType();

        // If SuerType is ParameterizedType, let it try to resolve
        JavaType superType = getSuperType();
        if (superType instanceof JavaParameterizedType)
            return superType.getResolvedTypeForTypeVariable(aTypeVar);

        // Otherwise just return EvalClass
        return aTypeVar.getEvalClass();
    }

    /**
     * Returns the JavaField for java.lang.reflect.Field.
     */
    public JavaField getJavaFieldForField(Field aField)
    {
        String name = aField.getName();
        JavaField field = getDeclaredFieldForName(name);
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
        JavaMethod[] methods = getDeclaredMethods();
        return ArrayUtils.findMatch(methods, method -> method.getId().equals(anId));
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
        JavaConstructor[] constructors = getDeclaredConstructors();
        return ArrayUtils.findMatch(constructors, constr -> constr.getId().equals(anId));
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
        throw new RuntimeException("JavaClass.getJavaMemberForMember: " + aMember);
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Start with simple class name
        String suggestionString = getSimpleName();

        // If inner class, add enclosing class name
        if (_declaringClass != null)
            return suggestionString + " - " + _declaringClass.getName();

        // If not-root package, add package name
        else if (_package != null) {
            String packageName = _package.getName();
            if (packageName.length() > 0)
                return suggestionString + " - " + _package.getName();
        }

        // Return
        return suggestionString;
    }

    /**
     * Returns the updater.
     */
    public JavaClassUpdater getUpdater()  { return _updater; }

    /**
     * Reloads the class using real class from resolver classloader. Returns whether the class changed during reload.
     */
    public boolean reloadClass()
    {
        // Update decls, if decls changed, clear AllDecls
        try {
            return _updater.reloadClass();
        }

        catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reloads the class using class decl.
     */
    public void reloadClassFromClassDecl(JClassDecl classDecl)
    {
        _updater = new JavaClassUpdaterDecl(this, classDecl);
        reloadClass();
        _updater = new JavaClassUpdater(this);
    }

    /**
     * Updates array class.
     */
    private void configureArrayClass()
    {
        // Handle Object[] special: Add Array.length field
        if (getName().equals("java.lang.Object[]")) {
            JavaField.FieldBuilder fb = new JavaField.FieldBuilder();
            fb.init(_resolver, getClassName());
            _fields = fb.name("length").type(int.class).save().buildAll();
            _interfaces = new JavaClass[0];
            _methods = new JavaMethod[0];
            _constructors = new JavaConstructor[0];
            _innerClasses = new JavaClass[0];
            _typeVars = new JavaTypeVariable[0];
            return;
        }

        // Handle other arrays: Just copy over from object array
        JavaClass arrayDecl = _resolver.getJavaClassForClass(Object[].class);
        _fields = arrayDecl.getDeclaredFields();
        _interfaces = arrayDecl._interfaces;
        _methods = arrayDecl._methods;
        _constructors = arrayDecl._constructors;
        _innerClasses = arrayDecl._innerClasses;
        _typeVars = arrayDecl._typeVars;
    }

    /**
     * Returns whether this class was loaded from source. Lame.
     */
    public boolean isFromSource()  { return _updater instanceof JavaClassUpdaterDecl; }
}
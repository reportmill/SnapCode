/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.JClassDecl;
import javakit.parse.JFile;
import snap.util.SnapUtils;
import java.lang.reflect.*;
import java.util.*;

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

    // The super class
    private JavaClass  _superClass;

    // The modifiers
    protected int  _mods;

    // Whether class decl is enum, interface, primitive
    private boolean  _enum, _interface, _primitive;

    // The array of interfaces
    protected JavaClass[]  _interfaces;

    // The field decls
    protected List<JavaField>  _fieldDecls;

    // The method decls
    protected List<JavaMethod>  _methDecls = new ArrayList<>();

    // The constructor decls
    protected List<JavaConstructor>  _constrDecls = new ArrayList<>();

    // The inner class decls
    protected List<JavaClass>  _innerClasses = new ArrayList<>();

    // The type var decls
    protected List<JavaTypeVariable>  _typeVarDecls = new ArrayList<>();

    // The Array component type (if Array)
    private JavaClass  _componentType;

    // The updater
    private JavaClassUpdater  _updater;

    /**
     * Constructor.
     */
    public JavaClass(Resolver aResolver, JavaDecl aPar, Class<?> aClass)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set Id, Name, SimpleName
        _id = _name = ResolverUtils.getIdForClass(aClass);
        _simpleName = aClass.getSimpleName();

        // Set DeclaringClass or Package
        if (aPar instanceof JavaClass) {
            _declaringClass = (JavaClass) aPar;
            _package = _declaringClass.getPackage();
        }
        else if (aPar instanceof JavaPackage)
            _package = (JavaPackage) aPar;

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
        if (_primitive && SnapUtils.isTeaVM)
            _mods |= Modifier.PUBLIC;

        // Set EvalType to this
        _evalType = this;

        // Create/set updater
        _updater = new JavaClassUpdater(this);

        // Get type super type and set in decl
        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null)
            _superClass = _resolver.getJavaClassForClass(superClass);

        // If Array, set Component class
        if (aClass.isArray()) {
            Class<?> compClass = aClass.getComponentType();
            _componentType = getJavaClassForClass(compClass);
        }
    }

    /**
     * Constructor from JClassDecl.
     */
    public JavaClass(Resolver aResolver, JClassDecl aClassDecl, String aClassName)
    {
        // Do normal version
        super(aResolver, DeclType.Class);

        // Set Id, Name, SimpleName
        _id = _name = aClassName;
        _simpleName = aClassDecl.getSimpleName();

        // Set DeclaringClass or Package
        JClassDecl enclosingClass = aClassDecl.getEnclosingClassDecl();
        if (enclosingClass != null) {
            _declaringClass = enclosingClass.getDecl();
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
        _mods = aClassDecl.getMods().getValue();
        _enum = aClassDecl.isEnum();
        _interface = aClassDecl.isInterface();

        // Set EvalType to this
        _evalType = this;

        // Create/set updater
        _updater = new JavaClassUpdaterDecl(this, aClassDecl);

        // Get type super type and set in decl
        _superClass = aClassDecl.getSuperClass();
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
     * Returns the class name.
     */
    @Override
    public String getClassName()  { return _name; }

    /**
     * Returns the top level class name.
     */
    public String getRootClassName()
    {
        if (_declaringClass != null)
            return _declaringClass.getRootClassName();
        return getClassName();
    }

    /**
     * Override to return as Class type.
     */
    public JavaType getSuperType()
    {
        // If already set or Object.class, just return
        if (_superType != null) return _superType;
        if (_superClass == null) return null;

        // Get GenericSuperClass as JavaType
        Class<?> realClass = getRealClass();
        Type superType = _resolver.getGenericSuperClassForClass(realClass);
        JavaType javaType = _resolver.getJavaTypeForType(superType);

        // Set, return
        return _superType = javaType;
    }

    /**
     * Override to return as Class type.
     */
    public JavaClass getSuperClass()
    {
        return _superClass;
    }

    /**
     * Returns the class this decl evaluates to when referenced.
     */
    public Class<?> getRealClass()
    {
        String className = getClassName();
        Class<?> realClass = className != null ? _resolver.getClassForName(className) : null;
        if (realClass == null) {

            // If from JClassDecl, not surprising
            if (_updater instanceof JavaClassUpdaterDecl) {
                JavaClassUpdaterDecl updater = (JavaClassUpdaterDecl) _updater;
                JavaClass superClass = updater._classDecl.getSuperClass();
                Class<?> superClassReal = superClass.getRealClass();
                return superClassReal;
            }

            // Bigger deal
            else System.out.println("JavaClass.getRealClass: Couldn't find real class for name: " + className);
        }
        return realClass;
    }

    /**
     * Returns the modifiers.
     */
    public int getModifiers()  { return _mods; }

    /**
     * Returns whether decl is static.
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(_mods);
    }

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
    public boolean isArray()  { return _componentType != null; }

    /**
     * Returns the Array component type (if Array).
     */
    public JavaClass getComponentType()  { return _componentType; }

    /**
     * Returns whether is primitive.
     */
    public boolean isPrimitive()
    {
        return _primitive;
    }

    /**
     * Returns the interfaces this class implments.
     */
    public JavaClass[] getInterfaces()
    {
        getFields();
        return _interfaces;
    }

    /**
     * Returns the fields.
     */
    public List<JavaField> getFields()
    {
        if (_fieldDecls == null) updateDecls();
        return _fieldDecls;
    }

    /**
     * Returns the methods.
     */
    public List<JavaMethod> getMethods()
    {
        getFields();
        return _methDecls;
    }

    /**
     * Returns the Constructors.
     */
    public List<JavaConstructor> getConstructors()
    {
        getFields();
        return _constrDecls;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaClass> getInnerClasses()
    {
        getFields();
        return _innerClasses;
    }

    /**
     * Returns the inner classes.
     */
    public List<JavaTypeVariable> getTypeVars()
    {
        getFields();
        return _typeVarDecls;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaField getFieldForName(String aName)
    {
        List<JavaField> fields = getFields();
        for (JavaField field : fields)
            if (field.getName().equals(aName))
                return field;

        // Return
        return null;
    }

    /**
     * Returns a field decl for field name.
     */
    public JavaField getFieldDeepForName(String aName)
    {
        JavaField field = getFieldForName(aName);
        if (field == null && _superClass != null)
            field = _superClass.getFieldDeepForName(aName);

        // Return
        return field;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaConstructor getConstructorForTypes(JavaType[] theTypes)
    {
        List<JavaConstructor> constructors = getConstructors();
        for (JavaConstructor constructor : constructors) {
            JavaType[] constrParamTypes = constructor.getParamTypes();
            if (isTypesEqual(constrParamTypes, theTypes))
                return constructor;
        }

        // Return
        return null;
    }

    /**
     * Returns a constructor decl for parameter types.
     */
    public JavaConstructor getConstructorDeepForTypes(JavaType[] theTypes)
    {
        JavaConstructor decl = getConstructorForTypes(theTypes);
        if (decl == null && _superClass != null)
            decl = _superClass.getConstructorDeepForTypes(theTypes);
        return decl;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaMethod getMethodForNameAndTypes(String aName, JavaType[] theTypes)
    {
        List<JavaMethod> methods = getMethods();
        for (JavaMethod method : methods) {
            if (method.getName().equals(aName)) {
                JavaType[] methodParamTypes = method.getParamTypes();
                if (isTypesEqual(methodParamTypes, theTypes))
                    return method;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns a method decl for method name and parameter types.
     */
    public JavaMethod getMethodDeepForNameAndTypes(String aName, JavaType[] theTypes)
    {
        JavaMethod method = getMethodForNameAndTypes(aName, theTypes);
        if (method == null && _superClass != null)
            method = _superClass.getMethodDeepForNameAndTypes(aName, theTypes);
        return method;
    }

    /**
     * Returns a Class decl for inner class simple name.
     */
    public JavaClass getInnerClassForName(String aName)
    {
        List<JavaClass> innerClasses = getInnerClasses();
        for (JavaClass innerClass : innerClasses)
            if (innerClass.getSimpleName().equals(aName))
                return innerClass;

        // Return not found
        return null;
    }

    /**
     * Returns a Class decl for inner class name.
     */
    public JavaClass getInnerClassDeepForName(String aName)
    {
        // Check class
        JavaClass innerClass = getInnerClassForName(aName);

        // Check super classes
        if (innerClass == null && _superClass != null)
            innerClass = _superClass.getInnerClassDeepForName(aName);

        // Check interfaces

        // Return
        return innerClass;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        // Iterate to find TypeVar for name
        List<JavaTypeVariable> typeVars = getTypeVars();
        for (JavaTypeVariable typeVar : typeVars)
            if (typeVar.getName().equals(aName))
                return typeVar;

        // Return not found
        return null;
    }

    /**
     * Returns a TypeVar decl for inner class name.
     */
    public int getTypeVarIndexForName(String aName)
    {
        // Iterate to find TypeVar index for name
        List<JavaTypeVariable> typeVars = getTypeVars();
        for (int i = 0, iMax = typeVars.size(); i < iMax; i++) {
            JavaTypeVariable typeVar = typeVars.get(i);
            if (typeVar.getName().equals(aName))
                return i;
        }

        // Return not found
        return -1;
    }

    /**
     * Returns the lambda method.
     */
    public JavaMethod getLambdaMethod(int argCount)
    {
        List<JavaMethod> methods = getMethods();
        for (JavaMethod method : methods)
            if (method.getParamCount() == argCount)
                return method;
        return null;
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    @Override
    public boolean isAssignable(JavaType aType)
    {
        // If this decl is primitive, forward to primitive version
        if (isPrimitive() && aType instanceof JavaClass)
            return isAssignablePrimitive(aType);

        // If given val is null or this decl is Object return true
        if (aType == null)
            return true;
        if (getName().equals("java.lang.Object"))
            return true;
        JavaClass otherClass = aType.getEvalClass();
        if (otherClass.isPrimitive())
            otherClass = otherClass.getPrimitiveAlt();

        // If either are array type, check ArrayItemTypes if both are (otherwise return false)
        if (isArray() || otherClass.isArray()) {
            if (isArray() && otherClass.isArray())
                return getComponentType().isAssignable(otherClass.getComponentType());
            return false;
        }

        // Iterate up given class superclasses and check class and interfaces
        for (JavaClass cls = otherClass; cls != null; cls = cls.getSuperClass()) {

            // If classes match, return true
            if (cls == this)
                return true;

            // If any interface of this decl match, return true
            if (isInterface()) {
                for (JavaClass infc : cls.getInterfaces())
                    if (isAssignable(infc))
                        return true;
            }
        }

        // Return false since no match found
        return false;
    }

    /**
     * Returns whether given type is assignable to this JavaDecl.
     */
    private boolean isAssignablePrimitive(JavaType otherType)
    {
        if (otherType == null)
            return false;
        JavaClass otherClass = otherType.getEvalClass();
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
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    @Override
    public JavaType getResolvedType(JavaType aType)
    {
        // Handle ParamType and anything not a TypeVar
        if (aType instanceof JavaParameterizedType) {
            System.err.println("JavaDecl.getResolvedType: ParamType not yet supported");
            return aType;
        }

        // If not TypeVariable, we shouldn't be here
        if (!(aType instanceof JavaTypeVariable))
            return aType;

        // If has type var, return bounds type
        String name = aType.getName();
        JavaDecl typeVar = getTypeVarForName(name);
        if (typeVar != null)
            return typeVar.getEvalType();

        // If super has type var, return mapped type //JavaDecl sdecl = getSuper();
        /*if(sdecl!=null && sdecl.getTypeVar(name)!=null) {
            int ind = sdecl.getHpr().getTypeVarDeclIndex(name);
            if(ind>=0 && ind<_paramTypes.length) return _paramTypes[ind]; }*/

        // If SuerType is ParameterizedType, let it try to resolve
        JavaType superType = getSuperType();
        if (superType instanceof JavaParameterizedType)
            return superType.getResolvedType(aType);

        // Otherwise just return EvalType
        JavaType evalType = aType.getEvalType();
        return evalType;
    }

    /**
     * Returns the full name.
     */
    @Override
    protected String getFullNameImpl()
    {
        // Get Match name
        String name = getMatchName();

        // Add mod string
        String modifierStr = Modifier.toString(_mods);
        if (modifierStr.length() > 0)
            name = modifierStr + " " + name;

        // Return
        return name;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        String simpleName = getSimpleName();
        if (_declaringClass != null)
            return simpleName + " - " + _declaringClass.getName();
        else if (_package != null)
            return simpleName + " - " + _package.getName();
        return simpleName;
    }

    /**
     * Returns the updater.
     */
    public JavaClassUpdater getUpdater()  { return _updater; }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDecls()
    {
        return _updater.updateDecls();
    }

    /**
     * Returns the list of all decls.
     */
    public List<JavaDecl> getAllDecls()
    {
        return _updater.getAllDecls();
    }
}
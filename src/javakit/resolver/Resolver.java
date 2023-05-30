/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ClassUtils;
import snap.util.SnapUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class Resolver {

    // The ClassLoader for compiled class info
    protected ClassLoader  _classLoader;

    // The ClassPaths
    private String[]  _classPaths = new String[0];

    // The ClassPathInfo
    private ClassPathInfo  _classPathInfo;

    // A cache of JavaPackages by name
    private Map<String,JavaPackage>  _packages = new HashMap<>();

    // A map of class/package names to JavaDecls to provide JavaDecls for project
    protected Map<String, JavaClass>  _classes = new HashMap<>();

    // A cache of JavaParameterizedTypes by id
    private Map<String,JavaParameterizedType>  _paramTypes = new HashMap<>();

    // A cache of JavaGenericArrayType by id
    private Map<String,JavaGenericArrayType>  _arrayTypes = new HashMap<>();

    // Global literals
    private static JavaLocalVar[]  _literals;

    // TeaVM
    public static boolean isTeaVM = SnapUtils.isTeaVM;

    /**
     * Constructor.
     */
    public Resolver(ClassLoader aClassLoader)
    {
        _classLoader = aClassLoader;
    }

    /**
     * Returns the ClassPaths.
     */
    public String[] getClassPaths()  { return _classPaths; }

    /**
     * Sets the ClassPaths.
     */
    public void setClassPaths(String[] theClassPaths)
    {
        _classPaths = theClassPaths;
    }

    /**
     * Returns the ClassPathInfo.
     */
    public ClassPathInfo getClassPathInfo()
    {
        // If already set, just return
        if (_classPathInfo != null) return _classPathInfo;

        // Create, set, return
        ClassPathInfo classPathInfo = new ClassPathInfo(this);
        return _classPathInfo = classPathInfo;
    }

    /**
     * Returns the ClassLoader.
     */
    public ClassLoader getClassLoader()  { return _classLoader; }

    /**
     * Returns a Class for given name.
     */
    public Class<?> getClassForName(String aName)
    {
        // Get Class loader, find class
        ClassLoader classLoader = getClassLoader();
        Class<?> cls = ClassUtils.getClassForName(aName, classLoader);

        // If not found and name doesn't contain '.', try java.lang.Name
        if (cls == null && aName.indexOf('.') < 0)
            cls = ClassUtils.getClassForName("java.lang." + aName, classLoader);

        // Return
        return cls;
    }

    /**
     * Returns a JavaClass for given class name.
     */
    public JavaClass getJavaClassForName(String aClassName)
    {
        // Get from Classes cache and just return if found
        JavaClass javaClass = _classes.get(aClassName);
        if (javaClass != null)
            return javaClass;

        // Otherwise lookup Class for name
        Class<?> cls = getClassForName(aClassName);
        if (cls != null)
            return getJavaClassForClass(cls);

        // Return
        return null;
    }

    /**
     * Returns a JavaClass for given Class.
     */
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        // Lookup class decl by name and return if already set
        String className = aClass.getName();
        JavaClass javaClass = _classes.get(className);
        if (javaClass != null)
            return javaClass;

        // Get parent package or class for class
        JavaDecl parDecl = getParentPackageOrClassForClass(aClass);

        // Create JavaClass and add to Classes cache map (this is done in constructor)
        javaClass = new JavaClass(this, parDecl, aClass);

        // Return
        return javaClass;
    }

    /**
     * Returns whether given package really exists. This probably needs a real implementation.
     */
    public boolean isKnownPackageName(String aName)
    {
        boolean known = _packages.containsKey(aName);
        return known;
    }

    /**
     * Returns a package decl.
     */
    public JavaPackage getJavaPackageForName(String aName)
    {
        // Get from Packages cache and just return if found
        JavaPackage pkg = _packages.get(aName);
        if (pkg != null)
            return pkg;

        // Get parent package
        JavaPackage parent = null;
        int ind = aName.lastIndexOf('.');
        if (ind >= 0) {
            String pkgName = aName.substring(0, ind);
            parent = getJavaPackageForName(pkgName);
        }

        // Create new JavaPackage and add to Packages cache
        pkg = new JavaPackage(this, parent, aName);
        _packages.put(aName, pkg);

        // Return
        return pkg;
    }

    /**
     * Returns a JavaType for given type.
     */
    public JavaType getJavaTypeForType(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return getJavaClassForClass((Class<?>) aType);

        // Handle ParameterizedType
        if (aType instanceof ParameterizedType)
            return getJavaParameterizedTypeForType((ParameterizedType) aType);

        // Handle TypeVariable
        if (aType instanceof TypeVariable)
            return getJavaTypeVariable((TypeVariable<?>) aType);

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType)
            return getGenericArrayTypeDecl((GenericArrayType) aType);

        // Handle WildCard
        Class<?> cls = ResolverUtils.getClassForType(aType);
        return getJavaClassForClass(cls);
        //throw new RuntimeException("Resolver.getTypeDecl: Unsupported type " + aType);
    }

    /**
     * Returns a JavaType array for given java.lang.reflect.Type array.
     */
    public JavaType[] getJavaTypesForTypes(Type[] theTypes)
    {
        // Create JavaTypes array
        JavaType[] javaTypes = new JavaType[theTypes.length];

        // Iterate over types and convert each to JavaType
        for (int i = 0; i < theTypes.length; i++) {
            Type type = theTypes[i];
            JavaType javaType = getJavaTypeForType(type);
            if (javaType == null)
                System.err.println("Resolver.getJavaTypeArray: Couldn't resolve type: " + type);
            else javaTypes[i] = javaType;
        }

        // Return
        return javaTypes;
    }

    /**
     * Returns a JavaGenericArrayType for java.lang.reflect.GenericArrayType.
     */
    private JavaGenericArrayType getGenericArrayTypeDecl(GenericArrayType aGAT)
    {
        // Check ArrayTypes cache and return if found
        String id = ResolverUtils.getIdForGenericArrayType(aGAT);
        JavaGenericArrayType decl = _arrayTypes.get(id);
        if (decl != null)
            return decl;

        // Create and add to cache
        decl = new JavaGenericArrayType(this, aGAT);
        _arrayTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a JavaParameterizedType for java.lang.reflect.ParameterizedType.
     */
    private JavaParameterizedType getJavaParameterizedTypeForType(ParameterizedType aPT)
    {
        // Check ParamTypes cache and return if found
        String id = ResolverUtils.getIdForParameterizedType(aPT);
        JavaParameterizedType decl = _paramTypes.get(id);
        if (decl != null)
            return decl;

        // Get RawType and ArgTypes as JavaType
        Type rawType = aPT.getRawType();
        Type[] typArgs = aPT.getActualTypeArguments();
        JavaType rawTypeDecl = getJavaTypeForType(rawType);
        JavaType[] typeArgDecls = getJavaTypesForTypes(typArgs);

        // Create and add to cache
        decl = new JavaParameterizedType(this, rawTypeDecl, typeArgDecls);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a JavaParameterizedType for given types.
     */
    protected JavaParameterizedType getJavaParameterizedTypeForTypes(JavaType aRawType, JavaType[] theTypeArgs)
    {
        // Get id and decl for id (just return if found)
        String id = ResolverUtils.getIdForParameterizedTypeParts(aRawType, theTypeArgs);
        JavaParameterizedType decl = _paramTypes.get(id);
        if (decl != null)
            return decl;

        // Create new decl, add to map
        decl = new JavaParameterizedType(this, aRawType, theTypeArgs);
        _paramTypes.put(id, decl);

        // Return
        return decl;
    }

    /**
     * Returns a JavaTypeVariable.
     */
    private JavaTypeVariable getJavaTypeVariable(TypeVariable<?> typeVar)
    {
        // Get class or method
        GenericDeclaration classOrMethod = typeVar.getGenericDeclaration();
        String typeVarName = typeVar.getName();

        // Handle class
        if (classOrMethod instanceof Class) {
            Class<?> ownerClass = (Class<?>) classOrMethod;
            JavaClass javaClass = getJavaClassForClass(ownerClass);
            return javaClass.getTypeVarForName(typeVarName);
        }

        // Handle Method/Constructor (not using Executable for TeaVM sake)
        else if (classOrMethod instanceof Method || classOrMethod instanceof Constructor) {
            Member method = (Member) classOrMethod;
            JavaExecutable javaMethod = (JavaExecutable) getJavaMemberForMember(method);
            return javaMethod.getTypeVarForName(typeVarName);
        }

        // Can't resolve
        System.out.println("Resolver.getJavaTypeVariable: Can't resolve name: " + typeVarName + " for " + classOrMethod);
        return null;
    }

    /**
     * Returns a JavaMember for given Member.
     */
    private JavaMember getJavaMemberForMember(Member aMember)
    {
        Class<?> declaringClass = aMember.getDeclaringClass();
        JavaClass javaClass = getJavaClassForClass(declaringClass);
        JavaClassUpdater updater = javaClass.getUpdater();
        return updater.getJavaMemberForMember(aMember);
    }

    /**
     * Returns the parent JavaPackage or JavaClass for a class.
     */
    private JavaDecl getParentPackageOrClassForClass(Class<?> aClass)
    {
        // Get parent class, get decl from parent decl
        Class<?> parentClass = aClass.getDeclaringClass();
        if (parentClass != null)
            return getJavaClassForClass(parentClass);

        // Get parent package
        Package pkg = aClass.getPackage();
        String pkgName = pkg != null ? pkg.getName() : null;
        if (pkgName != null && pkgName.length() > 0)
            return getJavaPackageForName(pkgName);

        // Return root package
        return null;
    }

    /**
     * Invokes a method on given object for name and args.
     */
    public Object invokeMethod(Object anObj, JavaMethod javaMethod, Object[] theArgs) throws Exception
    {
        String sig = javaMethod.getId();
        Object value = StaticResolver.shared().invokeMethod(sig, anObj, theArgs);
        return value;
    }

    /**
     * Invokes a method on given object for name and args.
     */
    public Object invokeConstructor(Class<?> aClass, JavaConstructor javaConstructor, Object[] theArgs) throws Exception
    {
        if (theArgs.length == 0)
            return aClass.newInstance();

        // Invoke
        String sig = javaConstructor.getId();
        Object newInstance = StaticResolver.shared().invokeConstructor(sig, theArgs);
        return newInstance;
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericSuperClassForClass(Class<?> aClass)
    {
        return aClass.getSuperclass();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForClass(Class<?> aClass)
    {
        return new TypeVariable<?>[0];
    }

    /**
     * Needed for TeaVM.
     */
    public Class<?>[] getDeclaredClassesForClass(Class<?> aClass)
    {
        return new Class<?>[0];
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericTypeForField(Field aField)
    {
        return aField.getType();
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericReturnTypeForMethod(Method aMethod)
    {
        return aMethod.getReturnType();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForExecutable(Member aMember)
    {
        return new TypeVariable<?>[0];
    }

    /**
     * Needed for TeaVM.
     */
    public Type[] getGenericParameterTypesForExecutable(Member aMember)
    {
        if (aMember instanceof Method)
            return ((Method) aMember).getParameterTypes();
        return ((Constructor<?>) aMember).getParameterTypes();
    }

    /**
     * Needed for TeaVM.
     */
    public boolean isDefaultMethod(Method aMethod)  { return false; }

    /**
     * Returns global literals: true, false, null, this, super.
     */
    public JavaLocalVar[] getGlobalLiterals()
    {
        // If already set, just return
        if (_literals != null) return _literals;

        // Create global literals
        JavaLocalVar TRUE = new JavaLocalVar(this, "true", getJavaClassForClass(boolean.class), "true");
        JavaLocalVar FALSE = new JavaLocalVar(this, "false", getJavaClassForClass(boolean.class), "false");
        JavaLocalVar NULL = new JavaLocalVar(this, "null", getJavaClassForClass(Object.class), "null");
        JavaLocalVar THIS = new JavaLocalVar(this, "this", getJavaClassForClass(Object.class), "this");
        JavaLocalVar SUPER = new JavaLocalVar(this, "super", getJavaClassForClass(Object.class), "super");
        return _literals = new JavaLocalVar[] { TRUE, FALSE, NULL, THIS, SUPER };
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()  { return ""; }

    /**
     * Creates a new Resolver.
     */
    public static Resolver newResolverForClassLoader(ClassLoader aClassLoader)
    {
        // If TeaVM, just use base Resolver
        if (SnapUtils.isTeaVM)
            return new Resolver(aClassLoader);

        // Otherwise, use ResolverSys (Use reflection and silly conditional to stymie TeaVM)
        try {
            String className = !SnapUtils.isTeaVM ? "javakit.resolver.ResolverSys" : "don't judge me";
            Class<?> resolverClass = Class.forName(className);
            Resolver resolver = (Resolver) resolverClass.newInstance();
            resolver._classLoader = aClassLoader;
            return resolver;
        }
        catch(Exception e) {
            throw new RuntimeException("Resolver.newResolverForClassLoader: Can't create: ResolverSys" + e);
        }
    }
}
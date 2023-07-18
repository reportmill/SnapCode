/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ArrayUtils;
import snap.util.ClassUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class Resolver {

    // The ClassLoader for compiled class info
    protected ClassLoader  _classLoader;

    // The ClassPaths
    private String[]  _classPaths = new String[0];

    // The ClassTree
    private ClassTree  _classTree;

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
     * Returns the ClassTree.
     */
    public ClassTree getClassTree()
    {
        // If already set, just return
        if (_classTree != null) return _classTree;

        // Create, set, return
        String[] classPaths = getClassPaths();
        return _classTree = new ClassTree(classPaths);
    }

    /**
     * Returns the children for given package.
     */
    protected JavaDecl[] getChildrenForPackage(JavaPackage parentPackage)
    {
        ClassTree classTree = getClassTree();
        String packageName = parentPackage.getName();
        ClassTree.ClassTreeNode[] childNodes = classTree.getChildNodesForPackageName(packageName);
        JavaDecl[] children = ArrayUtils.map(childNodes, classTreeNode -> getJavaDeclForClassTreeNode(parentPackage, classTreeNode), JavaDecl.class);

        // Filter out null
        if (ArrayUtils.hasMatch(children, child -> child == null))
            children = ArrayUtils.filter(children, child -> child != null);

        // Return
        return children;
    }

    /**
     * Returns a JavaDecl for given ClassTreeNode.
     */
    private JavaDecl getJavaDeclForClassTreeNode(JavaPackage parentPackage, ClassTree.ClassTreeNode classTreeNode)
    {
        // If package, create/return package
        if (classTreeNode.isPackage)
            return new JavaPackage(this, parentPackage, classTreeNode.fullName);

        // Otherwise, create and return class
        Class<?> cls = getClassForName(classTreeNode.fullName);
        if (cls == null) { // This should never happen
            System.err.println("Resolver.getJavaDeclForClassTreeNode: Can't find class: " + classTreeNode.fullName);
            return null;
        }
        return new JavaClass(this, parentPackage, cls);
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

        // Handle array type
        if (aClass.isArray())
            return new JavaClass(this, null, aClass);

        // Get parent package or class for class
        JavaDecl parentPackageOrClass = getParentPackageOrClassForClass(aClass);

        // Handle parent package
        if (parentPackageOrClass instanceof JavaPackage) {
            JavaPackage javaPackage = (JavaPackage) parentPackageOrClass;
            javaClass = javaPackage.getClassForFullName(className);
            if (javaClass != null)
                return javaClass;
        }

        // Handle parent class
        else if (parentPackageOrClass instanceof JavaClass) {
            JavaClass parentClass = (JavaClass) parentPackageOrClass;
            int separtorIndex = className.lastIndexOf('$');
            if (separtorIndex < 0)
                System.err.println("Resolver.getJavaClassForClass: Simple name not found for: " + className);
            String simpleName = className.substring(separtorIndex + 1);
            javaClass = parentClass.getInnerClassForName(simpleName);
            if (javaClass != null)
                return javaClass;
        }

        // Create orphan JavaClass
        //System.err.println("Resolver.getJavaClassForClass: Can't find parent for class: " + aClass);
        return new JavaClass(this, parentPackageOrClass, aClass);
    }

    /**
     * Returns whether given package really exists. This probably needs a real implementation.
     */
    public boolean isKnownPackageName(String aName)
    {
        JavaPackage pkg = getJavaPackageForName(aName);
        return pkg != null;
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

        // Create JavaPackage and add to Packages cache
        pkg = getJavaPackageForNameImpl(aName);
        if (pkg != null)
            _packages.put(aName, pkg);

        // Return
        return pkg;
    }

    /**
     * Returns a package decl.
     */
    private JavaPackage getJavaPackageForNameImpl(String aName)
    {
        // If root package, just create/return
        if (aName.length() == 0)
            return new JavaPackage(this, null, "");

        // Get parent package name
        int lastSeperatorIndex = aName.lastIndexOf('.');
        String parentPackageName = lastSeperatorIndex > 0 ? aName.substring(0, lastSeperatorIndex) : "";

        // Get parent package
        JavaPackage parentPackage = getJavaPackageForName(parentPackageName);
        if (parentPackage == null)
            return null;

        // Find and return child package
        return parentPackage.getPackageForFullName(aName);
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
        Class<?> rawType = (Class<?>) aPT.getRawType(); // Java implementation always returns Class
        Type[] typArgs = aPT.getActualTypeArguments();
        JavaClass rawTypeDecl = getJavaClassForClass(rawType);
        JavaType[] typeArgDecls = getJavaTypesForTypes(typArgs);

        // Create and return JavaParameterizedType
        decl = getJavaParameterizedTypeForTypes(rawTypeDecl, typeArgDecls);
        if (!decl.getId().equals(id))
            _paramTypes.put(id, decl); // Shouldn't need this
        return decl;
    }

    /**
     * Returns a JavaParameterizedType for given types.
     */
    protected JavaParameterizedType getJavaParameterizedTypeForTypes(JavaClass aRawType, JavaType[] theTypeArgs)
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

        // Handle Method/Constructor
        else if (classOrMethod instanceof Executable) {
            Executable method = (Executable) classOrMethod;
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

}
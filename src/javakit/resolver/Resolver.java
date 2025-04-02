/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ArrayUtils;
import snapcode.project.Project;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class Resolver {

    // The project this resolver works for
    private Project _project;

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
    public Resolver(Project aProject)
    {
        _project = aProject;
    }

    /**
     * Returns the ClassTree.
     */
    private ClassTree getClassTree()
    {
        if (_classTree != null) return _classTree;
        String[] classPaths = _project.getRuntimeClassPaths();
        return _classTree = new ClassTree(classPaths);
    }

    /**
     * Returns the children for given package.
     */
    protected JavaDecl[] getChildrenForPackage(JavaPackage parentPackage)
    {
        ClassTree classTree = getClassTree();
        String packageName = parentPackage.getName();
        ClassTree.ClassTreeNode[] childNodes = classTree.getClassTreeNodesForPackageName(packageName);
        return ArrayUtils.mapNonNull(childNodes, childNode -> getJavaDeclForClassTreeNode(childNode), JavaDecl.class);
    }

    /**
     * Returns a JavaDecl for given ClassTreeNode.
     */
    private JavaDecl getJavaDeclForClassTreeNode(ClassTree.ClassTreeNode classTreeNode)
    {
        // If package, create/return package
        if (classTreeNode.isPackage)
            return getJavaPackageForName(classTreeNode.fullName);

        // Return class
        JavaClass javaClass = getJavaClassForName(classTreeNode.fullName);
        if (javaClass == null) // This should never happen
            System.err.println("Resolver.getJavaDeclForClassTreeNode: Can't find class: " + classTreeNode.fullName);
        return javaClass;
    }

    /**
     * Returns a Class for given name.
     */
    public Class<?> getClassForName(String aName)
    {
        // Get Class loader, find class
        ClassLoader classLoader =  _project.getRuntimeClassLoader();
        Class<?> realClass = ResolverUtils.getClassForName(aName, classLoader);

        // If not found and name doesn't contain '.', try java.lang.Name
        if (realClass == null && aName.indexOf('.') < 0)
            realClass = ResolverUtils.getClassForName("java.lang." + aName, classLoader);

        // Return
        return realClass;
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

        // Handle java.lang classes special - bogus: this should be handled in JFile.getImportClassName()
        if (aClassName.indexOf('.') < 0) {
            String javaLangClassName = "java.lang." + aClassName;
            javaClass = getJavaClassForName(javaLangClassName);
            if (javaClass != null) {
                _classes.put(javaLangClassName, javaClass);
                return javaClass;
            }
        }

        // Handle array coding
        if (aClassName.startsWith("[")) {
            String className = ResolverUtils.getClassNameForClassCoding(aClassName);
            javaClass = getJavaClassForName(className);
            if (javaClass != null)
                _classes.put(aClassName, javaClass);
            return javaClass;
        }

        // Handle array
        if (aClassName.endsWith("[]")) {
            String compClassName = aClassName.substring(0, aClassName.length() - 2);
            JavaClass compClass = getJavaClassForName(compClassName);
            if (compClass != null)
                javaClass = new JavaClass(compClass);
        }

        // Otherwise lookup Class for name
        else {
            Class<?> realClass = getClassForName(aClassName);
            if (realClass != null)
                javaClass = new JavaClass(this, realClass);
        }

        // Return
        return javaClass;
    }

    /**
     * Returns a JavaClass for given Class.
     */
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        String className = aClass.getName();
        return getJavaClassForName(className);
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
     * Returns a java package.
     */
    public JavaPackage getJavaPackageForName(String pkgName)
    {
        return getJavaPackageForName(pkgName, false);
    }

    /**
     * Returns a java package, with option to force.
     */
    protected JavaPackage getJavaPackageForName(String pkgName, boolean doForce)
    {
        // Get from Packages cache and just return if found
        JavaPackage pkg = _packages.get(pkgName);
        if (pkg != null)
            return pkg;

        // If package doesn't exist, just return null
        ClassTree classTree = getClassTree();
        if (!doForce && !pkgName.isEmpty() && !classTree.isKnownPackageName(pkgName))
            return null;

        // Create JavaPackage and add to Packages cache
        pkg = getJavaPackageForNameImpl(pkgName);
        _packages.put(pkgName, pkg);

        // Return
        return pkg;
    }

    /**
     * Creates a java package.
     */
    private JavaPackage getJavaPackageForNameImpl(String aName)
    {
        // If root package, just create/return
        if (aName.isEmpty())
            return new JavaPackage(this, null, "");

        // Get parent package
        int lastSeperatorIndex = aName.lastIndexOf('.');
        String parentPackageName = lastSeperatorIndex > 0 ? aName.substring(0, lastSeperatorIndex) : "";
        JavaPackage parentPackage = getJavaPackageForName(parentPackageName);
        if (parentPackage == null) // Should never happen
            System.out.println("Resolver.getJavaPackageForNameImpl: Can't find parent package: " + parentPackageName);

        // Return new package
        return new JavaPackage(this, parentPackage, aName);
    }

    /**
     * Returns a JavaGenericArrayType for java.lang.reflect.GenericArrayType.
     */
    protected JavaGenericArrayType getJavaGenericArrayTypeForJavaType(JavaType javaType)
    {
        // Check ArrayTypes cache and return if found
        String id = javaType.getId() + "[]";
        JavaGenericArrayType genericArrayType = _arrayTypes.get(id);
        if (genericArrayType != null)
            return genericArrayType;

        // Create and add to cache
        genericArrayType = new JavaGenericArrayType(javaType);
        _arrayTypes.put(id, genericArrayType);

        // Return
        return genericArrayType;
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
            return getJavaGenericArrayTypeForType((GenericArrayType) aType);

        // Handle WildcardType: Punt for now, focus on lower bound
        if (aType instanceof WildcardType) {
            WildcardType wc = (WildcardType) aType;
            Type[] boundsTypes = wc.getLowerBounds().length > 0 ? wc.getLowerBounds() : wc.getUpperBounds();
            Type boundsType = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getJavaTypeForType(boundsType);
        }

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
     * Called when dependency changes.
     */
    public void handleProjectDependenciesChanged()
    {
        _classTree = null;
        JavaPackage rootPackage = getJavaPackageForName("");
        resetPackage(rootPackage);
    }

    /**
     * Resets given package children so it will reload from ClassTree.
     */
    private static void resetPackage(JavaPackage aPackage)
    {
        if (aPackage._children == null)
            return;
        JavaPackage[] childPackages = aPackage.getPackages();
        aPackage._children = null;
        aPackage._packages = null;
        for (JavaPackage childPackage : childPackages)
            resetPackage(childPackage);
    }

    /**
     * Returns a JavaGenericArrayType for java.lang.reflect.GenericArrayType.
     */
    private JavaGenericArrayType getJavaGenericArrayTypeForType(GenericArrayType aGAT)
    {
        // Check ArrayTypes cache and return if found
        String id = ResolverIds.getIdForGenericArrayType(aGAT);
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
        String id = ResolverIds.getIdForParameterizedType(aPT);
        JavaParameterizedType parameterizedType = _paramTypes.get(id);
        if (parameterizedType != null)
            return parameterizedType;

        // Get RawType and ArgTypes as JavaType
        Class<?> rawType = (Class<?>) aPT.getRawType(); // Java implementation always returns Class
        Type[] typArgs = aPT.getActualTypeArguments();
        JavaClass rawTypeDecl = getJavaClassForClass(rawType);
        JavaType[] typeArgDecls = getJavaTypesForTypes(typArgs);

        // Create and return JavaParameterizedType
        parameterizedType = getJavaParameterizedTypeForTypes(rawTypeDecl, typeArgDecls);
        if (!parameterizedType.getId().equals(id))
            _paramTypes.put(id, parameterizedType); // Shouldn't need this
        return parameterizedType;
    }

    /**
     * Returns a JavaParameterizedType for given types.
     */
    protected JavaParameterizedType getJavaParameterizedTypeForTypes(JavaClass aRawType, JavaType[] theTypeArgs)
    {
        // If any primitive types, promote them to real class
        if (ArrayUtils.hasMatch(theTypeArgs, type -> type.isPrimitive()))
            theTypeArgs = ArrayUtils.map(theTypeArgs, type -> type.isPrimitive() ? type.getPrimitiveAlt() : type, JavaType.class);

        // Get id and decl for id (just return if found)
        String id = ResolverIds.getIdForParameterizedTypeParts(aRawType, theTypeArgs);
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

        // Handle class: Get JavaClass and return JavaTypeVariable for name
        if (classOrMethod instanceof Class) {
            Class<?> ownerClass = (Class<?>) classOrMethod;
            JavaClass javaClass = getJavaClassForClass(ownerClass);
            return javaClass.getTypeParameterForName(typeVarName);
        }

        // Handle Method/Constructor: Get JavaMethod and return JavaTypeVariable for name
        Executable methodOrConstr = (Executable) classOrMethod;
        JavaExecutable javaMethod = getJavaExecutableForExecutable(methodOrConstr);
        return javaMethod.getTypeParameterForName(typeVarName);
    }

    /**
     * Returns a JavaExecutable for given java.lang.reflect.Executable.
     */
    private JavaExecutable getJavaExecutableForExecutable(Executable methodOrConstr)
    {
        Class<?> declaringClass = methodOrConstr.getDeclaringClass();
        JavaClass javaClass = getJavaClassForClass(declaringClass);

        // Handle Method
        if (methodOrConstr instanceof Method) {
            JavaMethod[] methods = javaClass.getDeclaredMethods();
            String id = ResolverIds.getIdForMember(methodOrConstr);
            return ArrayUtils.findMatch(methods, method -> method.getId().equals(id));
        }

        // Handle Constructor
        JavaConstructor[] constructors = javaClass.getDeclaredConstructors();
        String id = ResolverIds.getIdForMember(methodOrConstr);
        return ArrayUtils.findMatch(constructors, constr -> constr.getId().equals(id));
    }

    /**
     * Returns the parent JavaPackage or JavaClass for a class.
     */
    protected JavaDecl getParentPackageOrClassForClass(Class<?> aClass)
    {
        // If class has enclosing class, return it
        Class<?> parentClass = aClass.getEnclosingClass();
        if (parentClass != null)
            return getJavaClassForClass(parentClass);

        // Get parent package
        Package pkg = aClass.getPackage();
        String pkgName = pkg != null ? pkg.getName() : "";
        return getJavaPackageForName(pkgName);
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
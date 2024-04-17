/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.Resolver;
import javakit.resolver.ResolverUtils;
import snap.web.WebFile;
import java.io.DataInputStream;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This class finds external refs in a class file.
 */
public class ClassFileUtils {

    // The class file
    private WebFile _classFile;

    // The project
    private Project  _proj;

    // The Resolver
    private Resolver  _resolver;

    /**
     * Creates a new ClassData for given file.
     */
    private ClassFileUtils(WebFile aFile)
    {
        _classFile = aFile;
        _proj = Project.getProjectForFile(_classFile);
        _resolver = _proj.getResolver();
    }

    /**
     * Returns the set of JavaDecls that this class references.
     */
    private void findRefs(Set<JavaDecl> theRefs)
    {
        // Get bytes
        if (_classFile.getBytes() == null)
            return;

        // Get ClassFile reader and read
        ClassFileReader classFileReader = new ClassFileReader();
        try {
            DataInputStream dataInputStream = new DataInputStream(_classFile.getInputStream());
            classFileReader.read(dataInputStream);
        }
        catch (Exception e) {
            System.err.println(e);
            return;
        }

        // Get base class name
        String className = _proj.getClassNameForFile(_classFile);
        className = getRootClassName(className);

        // Iterate over constants and add to set top level class names
        for (int i = 1, iMax = classFileReader.getConstantCount(); i <= iMax; i++) {
            ClassFileReader.Constant constant = classFileReader.getConstant(i);
            if (constant.isClass() && (isInRootClassName(className, constant.getClassName()) ||
                    ResolverUtils.isPrimitiveClassName(constant.getClassName())))
                continue;
            JavaDecl ref = getRef(constant);
            if (ref != null)
                theRefs.add(ref);
        }

        // Get class and make sure TypeParameters, superclass and interfaces are in refs
        Class<?> cls = _proj.getClassForFile(_classFile);
        for (TypeVariable<?> typeVar : cls.getTypeParameters())
            addClassRef(typeVar, theRefs);
        addClassRef(cls.getGenericSuperclass(), theRefs);
        for (Type interfc : cls.getGenericInterfaces())
            addClassRef(interfc, theRefs);

        // Fields: add JavaDecl for each declared field - also make sure field type is in refs
        Field[] fields;
        try { fields = cls.getDeclaredFields(); }
        catch (Throwable e) { System.err.println(e + " in " + _classFile); return; }
        for (Field field : fields)
            addClassRef(field.getGenericType(), theRefs);

        // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
        Constructor<?>[] constrs;
        try { constrs = cls.getDeclaredConstructors(); }
        catch (Throwable e) { System.err.println(e + " in " + _classFile); return; }
        for (Constructor<?> constr : constrs) {
            if (constr.isSynthetic())
                continue;
            for (Type paramType : constr.getGenericParameterTypes())
                addClassRef(paramType, theRefs);
        }

        // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        Method[] methods;
        try { methods = cls.getDeclaredMethods(); }
        catch (Throwable e) { System.err.println(e + " in " + _classFile); return; }
        for (Method method : methods) {
            if (method.isSynthetic()) continue;
            addClassRef(method.getGenericReturnType(), theRefs);
            for (Type t : method.getGenericParameterTypes())
                addClassRef(t, theRefs);
        }
    }

    /**
     * Returns the JavaDecl for given Class ConstantPool Constant if external reference.
     */
    private JavaDecl getRef(ClassFileReader.Constant aConst)
    {
        // Handle Class reference
        if (aConst.isClass()) {
            String className = aConst.getClassName();
            if (className.startsWith("[")) return null;
            return _resolver.getJavaClassForName(className);
        }

        // Handle Field reference
        if (aConst.isField()) {

            // Get declaring class name
            String className = aConst.getDeclClassName();
            if (className.startsWith("["))
                return null;

            // Get field for name
            JavaClass javaClass = _resolver.getJavaClassForName(className);
            String fieldName = aConst.getMemberName();
            return javaClass.getFieldDeepForName(fieldName);
        }

        // Handle method reference
        if (aConst.isConstructor()) {

            // Get declaring class name
            String className = aConst.getDeclClassName();
            if (className.startsWith("["))
                return null;

            // Get parameter type names and JavaDecl array
            String[] paramNames = aConst.getParameterTypes();
            JavaClass[] paramClasses = new JavaClass[paramNames.length];
            for (int i = 0; i < paramNames.length; i++) {
                JavaClass paramClass = paramClasses[i] = _resolver.getJavaClassForName(paramNames[i]);
                if (paramClass == null) {
                    System.err.println("ClassData.getRef: Couldn't find param class: " + paramNames[i]); return null; }
            }

            // Get constructor for parameters
            JavaClass javaClass = _resolver.getJavaClassForName(className);
            return javaClass.getConstructorDeepForClasses(paramClasses);
        }

        // Handle method reference
        if (aConst.isMethod()) {

            // Get declaring class
            String className = aConst.getDeclClassName();
            if (className.startsWith("["))
                return null;

            // Get parameter type names and JavaDecl array
            String methodName = aConst.getMemberName();
            String[] paramNames = aConst.getParameterTypes();
            JavaClass[] paramClasses = new JavaClass[paramNames.length];
            for (int i = 0; i < paramNames.length; i++) {
                JavaClass paramClass = paramClasses[i] = _resolver.getJavaClassForName(paramNames[i]);
                if (paramClass == null) {
                    System.err.println("ClassData.getRef: Couldn't find param class: " + paramNames[i]); return null; }
            }

            // Get method for name and parameters
            JavaClass javaClass = _resolver.getJavaClassForName(className);
            return javaClass.getMethodDeepForNameAndClasses(methodName, paramClasses);
        }

        // Return null since unknown Constant reference
        return null;
    }

    /**
     * Adds a ref for a declaration type class.
     */
    private void addClassRef(Type aType, Set<JavaDecl> theRefs)
    {
        // Handle simple Class
        if (aType instanceof Class) {
            Class<?> cls = (Class<?>) aType;
            while (cls.isArray())
                cls = cls.getComponentType();
            if (cls.isAnonymousClass() || cls.isPrimitive() || cls.isSynthetic())
                return;

            JavaClass javaClass = _resolver.getJavaClassForClass(cls);
            theRefs.add(javaClass);
        }

        // Handle ParameterizedType
        else if (aType instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) aType;
            addClassRef(ptype.getRawType(), theRefs);
            for (Type type : ptype.getActualTypeArguments())
                addClassRef(type, theRefs);
        }

        // Handle TypeVariable
        else if (aType instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) aType;
            for (Type type : typeVar.getBounds())
                if (type instanceof Class)  // Bogus!
                    addClassRef(type, theRefs);
        }

        // Handle WildcardType
        else if (aType instanceof WildcardType) {
            WildcardType wct = (WildcardType) aType;
            for (Type type : wct.getLowerBounds())
                addClassRef(type, theRefs);
            for (Type type : wct.getUpperBounds())
                addClassRef(type, theRefs);
        }
    }

    /**
     * Returns the top level class name.
     */
    private static String getRootClassName(String className)
    {
        int i = className.indexOf('$');
        if (i > 0)
            className = className.substring(0, i);
        return className;
    }

    /**
     * Returns a simple class name.
     */
    private static boolean isInRootClassName(String aRoot, String aChild)
    {
        return aChild.startsWith(aRoot) && (aChild.length() == aRoot.length() || aChild.charAt(aRoot.length()) == '$');
    }

    /**
     * Returns external references in given class file.
     */
    public static JavaDecl[] getExternalReferencesForClassFile(WebFile classFile)
    {
        Set<JavaDecl> externalReferencesSet = new HashSet<>();
        ClassFileUtils.findExternalReferencesForClassFile(classFile, externalReferencesSet);
        return externalReferencesSet.toArray(new JavaDecl[0]);
    }

    /**
     * Returns external references in given class files.
     */
    public static JavaDecl[] getExternalReferencesForClassFiles(WebFile[] classFiles)
    {
        Set<JavaDecl> externalReferencesSet = new HashSet<>();
        for (WebFile classFile : classFiles)
            ClassFileUtils.findExternalReferencesForClassFile(classFile, externalReferencesSet);
        return externalReferencesSet.toArray(new JavaDecl[0]);
    }

    /**
     * Finds external references in given class file and adds to given set.
     */
    private static void findExternalReferencesForClassFile(WebFile classFile, Set<JavaDecl> theRefsSet)
    {
        try {
            ClassFileUtils classFileUtils = new ClassFileUtils(classFile);
            classFileUtils.findRefs(theRefsSet);
        }
        catch (Throwable t) {
            System.err.printf("ClassData.findRefsForClass: failed to get refs in %s: %s\n", classFile, t);
        }
    }
}
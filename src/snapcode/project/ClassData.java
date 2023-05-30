/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.Resolver;
import snap.util.ClassUtils;
import snap.web.WebFile;
import java.io.DataInputStream;
import java.lang.reflect.*;
import java.util.Set;

/**
 * A file to represent a Java class.
 */
public class ClassData {

    // The class file
    private WebFile  _file;

    // The project
    private Project  _proj;

    // The Resolver
    private Resolver  _resolver;

    // The class name
    String _cname;

    /**
     * Creates a new ClassData for given file.
     */
    public ClassData(WebFile aFile)
    {
        _file = aFile;
        _proj = Project.getProjectForFile(_file);
        _resolver = _proj.getResolver();
    }

    /**
     * Returns the set of JavaDecls that this class references.
     */
    public void getRefs(Set<JavaDecl> theRefs)
    {
        // Get bytes
        if (_file.getBytes() == null) return;

        // Get ClassFile reader and read
        ClassFileData classFileData = new ClassFileData();
        try {
            DataInputStream dataInputStream = new DataInputStream(_file.getInputStream());
            classFileData.read(dataInputStream);
        }
        catch (Exception e) {
            System.err.println(e);
            return;
        }

        // Iterate over constants and add to set top level class names
        String cname = _cname = getRootClassName(_proj.getClassNameForFile(_file));

        for (int i = 1, iMax = classFileData.getConstantCount(); i <= iMax; i++) {
            ClassFileData.Constant constant = classFileData.getConstant(i);
            if (constant.isClass() && (isInRootClassName(cname, constant.getClassName()) ||
                    ClassUtils.isPrimitiveClassName(constant.getClassName())))
                continue;
            JavaDecl ref = getRef(constant);
            if (ref != null)
                theRefs.add(ref);
        }

        // Get class and make sure TypeParameters, superclass and interfaces are in refs
        Class cls = _proj.getClassForFile(_file);
        for (TypeVariable tp : cls.getTypeParameters())
            addClassRef(tp, theRefs);
        addClassRef(cls.getGenericSuperclass(), theRefs);
        for (Type tp : cls.getGenericInterfaces())
            addClassRef(tp, theRefs);

        // Fields: add JavaDecl for each declared field - also make sure field type is in refs
        Field[] fields;
        try {
            fields = cls.getDeclaredFields();
        } catch (Throwable e) {
            System.err.println(e + " in " + _file);
            return;
        }
        for (Field field : fields)
            addClassRef(field.getGenericType(), theRefs);

        // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
        Constructor[] constrs;
        try {
            constrs = cls.getDeclaredConstructors();
        } catch (Throwable e) {
            System.err.println(e + " in " + _file);
            return;
        }
        for (Constructor constr : constrs) {
            if (constr.isSynthetic()) continue;
            for (Type t : constr.getGenericParameterTypes())
                addClassRef(t, theRefs);
        }

        // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        Method[] methods;
        try {
            methods = cls.getDeclaredMethods();
        } catch (Throwable e) {
            System.err.println(e + " in " + _file);
            return;
        }
        for (Method meth : methods) {
            if (meth.isSynthetic()) continue;
            addClassRef(meth.getGenericReturnType(), theRefs);
            for (Type t : meth.getGenericParameterTypes())
                addClassRef(t, theRefs);
        }
    }

    /**
     * Returns the JavaDecl for given Class ConstantPool Constant if external reference.
     */
    private JavaDecl getRef(ClassFileData.Constant aConst)
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
            return javaClass.getConstructorDeepForTypes(paramClasses);
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
            return javaClass.getMethodDeepForNameAndTypes(methodName, paramClasses);
        }

        // Return null since unknown Constant reference
        return null;
    }

    /**
     * Adds a ref for a declaration type class.
     */
    private final void addClassRef(Type aType, Set<JavaDecl> theRefs)
    {
        // Handle simple Class
        if (aType instanceof Class) {
            Class<?> cls = (Class) aType;
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
            TypeVariable tv = (TypeVariable) aType;
            for (Type type : tv.getBounds())
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
    private static String getRootClassName(String cname)
    {
        int i = cname.indexOf('$');
        if (i > 0) cname = cname.substring(0, i);
        return cname;
    }

    /**
     * Returns a simple class name.
     */
    private static boolean isInRootClassName(String aRoot, String aChild)
    {
        return aChild.startsWith(aRoot) && (aChild.length() == aRoot.length() || aChild.charAt(aRoot.length()) == '$');
    }

    /**
     * Returns the ClassData for given file.
     */
    public static ClassData getClassDataForFile(WebFile aFile)
    {
        // Get from File.Props
        ClassData classData = (ClassData) aFile.getProp(ClassData.class.getName());

        // If missing, create/set
        if (classData == null) {
            classData = new ClassData(aFile);
            aFile.setProp(ClassData.class.getName(), classData);
        }

        // Return
        return classData;
    }
}
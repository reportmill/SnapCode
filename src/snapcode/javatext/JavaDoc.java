/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.JNode;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMethod;
import snap.util.ListUtils;
import snap.util.URLUtils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class helps find and open JavaDoc for classes and JNodes.
 */
public class JavaDoc {

    // The class that JavaDoc represents
    private Class<?>  _javaDocClass;

    // The method to point to
    private Method _method;

    // The URL that is pointed to
    private String  _urlString;

    // A cache Map of classes to JavaDoc
    private static Map<Class<?>,JavaDoc> _javaDocs = new HashMap<>();

    // Constant for missing JavaDoc
    private static final JavaDoc NULL_JAVA_DOC = new JavaDoc(null);

    // Constants for Javadoc roots
    private static final String JAVADOC_ROOT = "https://docs.oracle.com/en/java/javase/21/docs/api/";
    private static final String SNAPKIT_JAVADOC_ROOT =  "http://reportmill.com/snap1/javadoc/";
    private static final String REPORTMILL_JAVADOC_ROOT =  "http://reportmill.com/rm14/javadoc/";

    // Constant for known Javadoc roots for package prefixes
    private static final List<String> KNOWN_PACKAGE_PREFIXES = List.of("java.", "javax.", "snap.", "com.reportmill.");

    /**
     * Constructor.
     */
    private JavaDoc(Class<?> aClass)
    {
        _javaDocClass = aClass;
    }

    /**
     * Returns the simple class name.
     */
    public String getSimpleName()  { return _javaDocClass.getSimpleName(); }

    /**
     * Returns the JavaDocClass.
     */
    public Class<?> getJavaDocClass()  { return _javaDocClass; }

    /**
     * Returns the URL string.
     */
    public String getUrlString()
    {
        // If already set, just return
        if (_urlString != null) return _urlString;

        // Get url for class
        String urlString = getJavaDocUrlForJavaDocClassName(_javaDocClass, _method);

        // Set and return
        return _urlString = urlString;
    }

    /**
     * Opens the URL.
     */
    public void openUrl()
    {
        String urlString = getUrlString();
        URLUtils.openURL(urlString);
    }

    /**
     * Returns a copy for given method.
     */
    public JavaDoc copyForMethod(Method aMethod)
    {
        JavaDoc copy = new JavaDoc(_javaDocClass);
        copy._method = aMethod;
        return copy;
    }

    /**
     * Returns the JavaDoc url for given JNode.
     */
    public static JavaDoc getJavaDocForNode(JNode aNode)
    {
        // Get class for node
        JavaDecl decl = aNode != null ? aNode.getDecl() : null;
        JavaMethod javaMethod = decl instanceof JavaMethod ? (JavaMethod) decl : null;
        JavaClass javaClass = aNode != null ? aNode.getEvalClass() : null;
        if (javaMethod != null)
            javaClass = javaMethod.getDeclaringClass();

        Class<?> nodeClass = javaClass != null ? javaClass.getRealClass() : null;
        JavaDoc javaDoc = getJavaDocForClass(nodeClass);
        if (javaDoc == null)
            return null;

        // If method, get copy for method
        if (javaMethod != null)
            javaDoc = javaDoc.copyForMethod(javaMethod.getMethod());

        // Return
        return javaDoc;
    }

    /**
     * Returns the JavaDoc class for given class.
     */
    private static JavaDoc getJavaDocForClass(Class<?> aClass)
    {
        // Check cache - if found, just return
        JavaDoc javaDoc = _javaDocs.get(aClass);
        if (javaDoc != null)
            return javaDoc != NULL_JAVA_DOC ? javaDoc : null;

        // Get, set, return
        javaDoc = getJavaDocForClassImpl(aClass);
        _javaDocs.put(aClass, javaDoc != null ? javaDoc : NULL_JAVA_DOC);
        return javaDoc;
    }

    /**
     * Returns the JavaDoc class for given class.
     */
    private static JavaDoc getJavaDocForClassImpl(Class<?> aClass)
    {
        // Handle null
        if (aClass == null) return null;

        // Handle array
        Class<?> javaDocClass = aClass;
        if (javaDocClass.isArray())
            javaDocClass = javaDocClass.getComponentType();

        // If class is JavaDoc class, return class
        String className = javaDocClass.getName();
        boolean isJavaDocClass = isJavaDocClassName(className);
        if (isJavaDocClass)
            return new JavaDoc(javaDocClass);

        // If superClass is valid, try to use it
        Class<?> superClass = javaDocClass.getSuperclass();
        if (superClass != null && superClass != Object.class)
            return getJavaDocForClass(superClass);

        // Return not found
        return null;
    }

    /**
     * Returns whether given class is a JavaDoc class.
     */
    private static boolean isJavaDocClassName(String className)
    {
        return ListUtils.hasMatch(KNOWN_PACKAGE_PREFIXES, prefix -> className.startsWith(prefix));
    }

    /**
     * Returns the JavaDoc url for given JavaDoc class.
     */
    private static String getJavaDocUrlForJavaDocClassName(Class<?> aClass, Method aMethod)
    {
        // Get base url for class - just return if not found
        String className = aClass.getName();
        String baseURL = getJavaDocBaseUrlForJavaDocClassName(className);
        if (baseURL == null)
            return null;

        // Add suffix
        String classPath = className.replace('.', '/');
        String suffix = classPath + ".html";

        // Add module name to suffix
        String moduleName = aClass.getModule().getName();
        if (moduleName != null)
            suffix = moduleName + '/' + suffix;

        // If method set, add hash
        if (aMethod != null) {
            String methodString = aMethod.toString();
            int index = methodString.indexOf(aMethod.getName() + '(');
            if (index > 0) {
                methodString = methodString.substring(index);
                methodString = methodString.replace("[]", ""); // replace('(', '-').replace(')', '-');
                suffix += '#' + methodString;
            }
        }

        // Return combined
        return baseURL + suffix;
    }

    /**
     * Returns the JavaDoc url for given JavaDoc class.
     */
    private static String getJavaDocBaseUrlForJavaDocClassName(String className)
    {
        // Handle standard java classes
        if (className.startsWith("java.") || className.startsWith("javax."))
            return JAVADOC_ROOT;

        // Handle snap classes
        if (className.startsWith("snap."))
            return SNAPKIT_JAVADOC_ROOT;

        // Handle ReportMill classes
        if (className.startsWith("com.reportmill."))
            return REPORTMILL_JAVADOC_ROOT;

        // Return not found
        return null;
    }
}

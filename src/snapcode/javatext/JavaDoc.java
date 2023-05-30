/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.JNode;
import javakit.resolver.JavaClass;
import snap.util.URLUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * This class helps find and open JavaDoc for classes and JNodes.
 */
public class JavaDoc {

    // The class that JavaDoc represents
    private Class<?>  _javaDocClass;

    // The URL that is pointed to
    private String  _urlString;

    // A cache Map of classes to JavaDoc
    private static Map<Class<?>,JavaDoc>  _javaDocs = new HashMap<>();

    // Constant for missing JavaDoc
    private static final JavaDoc  NULL_JAVA_DOC = new JavaDoc(null);

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

        // Get, set, return
        String className = _javaDocClass.getName();
        return _urlString = getJavaDocUrlForJavaDocClassName(className);
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
     * Returns the JavaDoc url for given JNode.
     */
    public static JavaDoc getJavaDocForNode(JNode aNode)
    {
        JavaClass javaClass = aNode != null ? aNode.getEvalClass() : null;
        Class<?> nodeClass = javaClass != null ? javaClass.getRealClass() : null;
        return getJavaDocForClass(nodeClass);
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
        if (className.startsWith("snap.") ||
                className.startsWith("com.reportmill.") ||
                className.startsWith("java.") ||
                className.startsWith("javax."))
            return true;
        return false;
    }

    /**
     * Returns the JavaDoc url for given JavaDoc class.
     */
    private static String getJavaDocUrlForJavaDocClassName(String className)
    {
        // Handle snap classes
        if (className.startsWith("snap."))
            return "http://reportmill.com/snap1/javadoc/index.html?" + className.replace('.', '/') + ".html";

            // Handle ReportMill classes
        else if (className.startsWith("com.reportmill."))
            return "http://reportmill.com/rm14/javadoc/index.html?" + className.replace('.', '/') + ".html";

            // Handle standard java classes
        else if (className.startsWith("java.") || className.startsWith("javax."))
            return "http://docs.oracle.com/javase/8/docs/api/index.html?" + className.replace('.', '/') + ".html";

        // Return not found
        return null;
    }
}

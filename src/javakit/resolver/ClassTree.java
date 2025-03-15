/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.StringUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The array of class path sites
    private WebSite[] _classPathSites;

    // Constants
    public static final ClassTreeNode[] EMPTY_NODE_ARRAY = new ClassTreeNode[0];

    /**
     * Constructor.
     */
    public ClassTree(String[] classPaths)
    {
        super();

        // Get ClassPathSites for ClassPaths
        _classPathSites = getClassPathSitesForClassPaths(classPaths);
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    protected ClassTreeNode[] getClassTreeNodesForPackageName(String packageName)
    {
        // Get files
        WebFile[] nodeFiles = getFilesForPackageName(packageName);
        if (nodeFiles.length == 0)
            return EMPTY_NODE_ARRAY;

        // Create nodes list
        List<ClassTreeNode> classTreeNodes = new ArrayList<>(nodeFiles[0].getFileCount());

        // If root package, add primitives
        if (packageName.isEmpty()) {
            Class<?>[] primitives = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class };
            ClassTreeNode[] primitiveNodes = ArrayUtils.map(primitives, cls -> new ClassTreeNode(cls.getName(), false), ClassTreeNode.class);
            Collections.addAll(classTreeNodes, primitiveNodes);
        }

        // Iterate over files and Find child classes and packages for each
        for (WebFile nodeFile : nodeFiles)
            findChildNodesForDirFile(nodeFile, classTreeNodes);

        // Return array
        return classTreeNodes.toArray(EMPTY_NODE_ARRAY);
    }

    /**
     * Returns a file for given package name.
     */
    private WebFile[] getFilesForPackageName(String packageName)
    {
        // Get file path
        String filePath = '/' + packageName.replace(".", "/");
        WebFile[] files = new WebFile[0];

        // Iterate over sites and return first match
        for (WebSite classPathSite : _classPathSites) {
            WebFile nodeFile = classPathSite.getFileForPath(filePath);
            if (nodeFile != null)
                files = ArrayUtils.add(files, nodeFile);
        }

        // Return files
        return files;
    }

    /**
     * Finds child packages and classes for given package node.
     */
    private void findChildNodesForDirFile(WebFile dirFile, List<ClassTreeNode> classTreeNodes)
    {
        // Get directory files
        List<WebFile> dirFiles = dirFile.getFiles();

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle class file
            if (isClassFile(file)) {
                String className = getClassNameForClassFile(file);
                ClassTreeNode classNode = new ClassTreeNode(className, false);
                classTreeNodes.add(classNode);
            }

            // Handle package
            if (isPackageDir(file)) {
                String packageName = getPackageNameForPackageDirFile(file);
                if (!ListUtils.hasMatch(classTreeNodes, classTreeNode -> classTreeNode.fullName.equals(packageName))) {
                    ClassTreeNode packageNode = new ClassTreeNode(packageName, true);
                    classTreeNodes.add(packageNode);
                }
            }
        }
    }

    /**
     * Returns whether given package name is known.
     */
    public boolean isKnownPackageName(String packageName)
    {
        // Get path for package name
        String filePath = '/' + packageName.replace(".", "/");

        // If any site has dir with package name (and case matches), return true
        for (WebSite site : _classPathSites) {
            WebFile file = site.getFileForPath(filePath);
            if (file != null && file.isDir() && file.getPath().equals(filePath))
                return true;
        }

        // Return not known
        return false;
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toString()
    {
        String sitesString = Arrays.toString(_classPathSites);
        return getClass().getSimpleName() + ": " + sitesString;
    }

    /**
     * Returns a simple class name for given node name.
     */
    protected static String getSimpleNodeName(String aNodeName)
    {
        // Get index of last '$' or '.'
        int sepIndex = aNodeName.lastIndexOf('$');
        if (sepIndex < 0)
            sepIndex = aNodeName.lastIndexOf('.');

        // Return ClassName stripped of package and/or parent-class
        return aNodeName.substring(sepIndex + 1);
    }

    /**
     * Returns an array of WebSites for given resolver class path.
     */
    private static WebSite[] getClassPathSitesForClassPaths(String[] classPaths)
    {
        // Create sites
        List<WebSite> classFileSites = new ArrayList<>();

        // Add JRE jar file site
        WebURL jreURL = WebURL.getURL(List.class);
        assert (jreURL != null);
        WebSite jreSite = jreURL.getSite();
        classFileSites.add(jreSite);

        // Try again for Swing (different site for Java 9+: jrt:/java.desktop/javax/swing/JFrame.class)
        Class<?> swingClass = null;
        try { swingClass = Class.forName("javax.swing.JFrame"); }
        catch (Exception ignore) { }
        if (swingClass != null) {
            WebURL swingURL = WebURL.getURL(swingClass);
            assert (swingURL != null);
            WebSite swingSite = swingURL.getSite();
            if (swingSite != jreSite)
                classFileSites.add(swingSite);
        }

        // Add project class path sites (build dirs, jar files)
        for (String classPath : classPaths) {

            // Get URL for class path
            WebURL classPathURL = WebURL.getURL(classPath);
            if (classPathURL == null) {
                System.err.println("ClassTree.getClassFileSitesForResolver: Can't resolve class path entry: " + classPath);
                continue;
            }

            // Get site for class path entry and add to sites
            WebSite classPathSite = classPathURL.getAsSite();
            classFileSites.add(classPathSite);
        }

        // Return array
        return classFileSites.toArray(new WebSite[0]);
    }

    /**
     * Returns class name for class file.
     */
    private static String getClassNameForClassFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        String filePathNoExtension = filePath.substring(1, filePath.length() - 6);
        String className = filePathNoExtension.replace('/', '.');
        return className;
    }

    /**
     * Returns package name for package file.
     */
    private static String getPackageNameForPackageDirFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        return filePath.substring(1).replace('/', '.');
    }

    /**
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isPackageDir(WebFile aFile)
    {
        if (!aFile.isDir())
            return false;
        if (aFile.getName().indexOf('.') > 0)
            return false;
        String path = aFile.getPath();
        if (isIgnorePath(path))
            return false;
        return true;
    }

    /**
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isClassFile(WebFile aFile)
    {
        String path = aFile.getPath();
        if (!path.endsWith(".class"))
            return false;
        if (isIgnorePath(path))
            return false;
        return true;
    }

    /**
     * Returns whether given package/class path should be ignored.
     */
    private static boolean isIgnorePath(String aPath)
    {
        if (aPath.startsWith("/sun")) return true;
        if (aPath.startsWith("/apple")) return true;
        if (aPath.startsWith("/com/sun")) return true;
        if (aPath.startsWith("/com/apple")) return true;
        if (aPath.startsWith("/com/oracle")) return true;
        if (aPath.startsWith("/java/applet")) return true;
        if (aPath.startsWith("/java/awt/dnd")) return true;
        if (aPath.startsWith("/java/awt/im")) return true;
        if (aPath.startsWith("/java/awt/peer")) return true;
        if (aPath.startsWith("/java/awt/print")) return true;
        if (aPath.startsWith("/java/beans")) return true;
        if (aPath.startsWith("/java/lang/model")) return true;
        if (aPath.startsWith("/java/lang/management")) return true;
        if (aPath.startsWith("/java/nio/channels")) return true;
        if (aPath.startsWith("/java/rmi")) return true;
        if (aPath.startsWith("/java/sql")) return true;
        if (aPath.startsWith("/java/util/spi")) return true;
        if (aPath.startsWith("/java/util/Spliterators")) return true;
        if (aPath.startsWith("/javax/jws")) return true;
        if (aPath.startsWith("/javax/lang")) return true;
        if (aPath.startsWith("/javax/naming")) return true;
        if (aPath.startsWith("/javax/net")) return true;
        if (aPath.startsWith("/javax/security")) return true;
        if (aPath.startsWith("/javax/accessibility")) return true;
        if (aPath.startsWith("/javax/management")) return true;
        if (aPath.startsWith("/javax/print")) return true;
        if (aPath.startsWith("/javax/rmi")) return true;
        if (aPath.startsWith("/javax/smartcardio")) return true;
        if (aPath.startsWith("/javax/sql")) return true;
        if (aPath.startsWith("/javax/swing/b")) return true;
        if (aPath.startsWith("/javax/swing/colorchooser")) return true;
        if (aPath.startsWith("/javax/swing/filechooser")) return true;
        if (aPath.startsWith("/javax/swing/plaf")) return true;
        if (aPath.startsWith("/javax/swing/tree")) return true;
        if (aPath.startsWith("/javax/swing/undo")) return true;
        if (aPath.startsWith("/javax/transaction")) return true;
        if (aPath.startsWith("/javax/xml")) return true;
        if (aPath.startsWith("/jdk")) return true;
        if (aPath.startsWith("/org/jcp")) return true;
        if (aPath.startsWith("/org/omg")) return true;
        if (aPath.startsWith("/org/w3c")) return true;
        if (aPath.startsWith("/org/xml")) return true;
        if (aPath.startsWith("/META-INF")) return true;

        // If inner class, return false
        if (aPath.contains("$"))
            return true;

        // Return true
        return false;
    }

    /**
     * A class to hold package info.
     */
    public static class ClassTreeNode {

        // Whether node is package
        public final boolean isPackage;

        // The package full name
        public final String fullName;

        // The package simple name
        public final String simpleName;

        /**
         * Constructor.
         */
        public ClassTreeNode(String aPackageName, boolean isPackage)
        {
            super();
            this.isPackage = isPackage;
            fullName = aPackageName;
            simpleName = getSimpleNodeName(aPackageName);
        }

        /**
         * Standard toString implementation.
         */
        public String toString()
        {
            // Get class name
            String className = getClass().getSimpleName();

            // Get prop strings: FullName, SimpleName, Parent
            StringBuffer propStrings = new StringBuffer();
            if (isPackage)
                StringUtils.appendProp(propStrings, "Package", true);
            StringUtils.appendProp(propStrings, "FullName", fullName);
            StringUtils.appendProp(propStrings, "SimpleName", simpleName);

            // Return
            return className + " { " + propStrings + " }";
        }
    }
}

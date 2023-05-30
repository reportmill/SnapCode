/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.util.*;
import javakit.resolver.ClassTree.*;
import snap.util.*;
import snap.web.*;

/**
 * A class to return class file info for Project class paths.
 */
public class ClassPathInfo {

    // The shared list of class path sites
    private WebSite[]  _sites;

    // A class tree
    private ClassTree  _classTree;

    /**
     * Constructor.
     */
    public ClassPathInfo(Resolver aResolver)
    {
        // Handle TeaVM
        if (SnapUtils.isTeaVM) {
            _sites = new WebSite[0];
            _classTree = ClassTreeWeb.getShared();
            return;
        }

        // Add JRE jar file site
        WebURL jreURL = WebURL.getURL(List.class);
        WebSite jreSite = jreURL.getSite();
        List<WebSite> sites = new ArrayList<>();
        sites.add(jreSite);

        // Get Project ClassPaths (build dirs, jar files)
        String[] classPaths = aResolver.getClassPaths(); // Was ProjectSet JK

        // Add project class path sites (build dirs, jar files)
        for (String classPath : classPaths) {

            // Get URL for class path
            WebURL classPathURL = WebURL.getURL(classPath);
            if (classPathURL == null) {
                System.err.println("ClassPathInfo.init: Can't resolve class path entry: " + classPath);
                continue;
            }

            // Get site for class path entry and add to sites
            WebSite classPathSite = classPathURL.getAsSite();
            sites.add(classPathSite);
        }

        // Set Sites
        _sites = sites.toArray(new WebSite[0]);
    }

    /**
     * Returns the class path sites.
     */
    public WebSite[] getSites()  { return _sites; }

    /**
     * Returns the ClassTree.
     */
    public ClassTree getClassTree()
    {
        // If already set, just return
        if (_classTree != null) return _classTree;

        // Create, set, return
        ClassTree classTree = getClassTreeImpl();
        return _classTree = classTree;
    }

    /**
     * Returns the ClassTree.
     */
    protected ClassTree getClassTreeImpl()
    {
        // Get Sites and create ClassTree
        WebSite[] sites = getSites();
        ClassTree classTree = new ClassTree();

        // Iterate over sites
        for (WebSite site : sites) {

            // Get site root files
            WebFile siteRootDir = site.getRootDir();
            WebFile[] rootFiles = siteRootDir.getFiles();

            // Iterate over site root files and create/add packages
            for (WebFile rootFile : rootFiles) {
                if (isPackageDir(rootFile))
                    getClassesForPackageDir(classTree, rootFile);
            }
        }

        // Return
        return classTree;
    }

    /**
     * Loads classes from package dir.
     */
    private void getClassesForPackageDir(ClassTree aClassTree, WebFile aDir)
    {
        // Get directory files
        WebFile[] dirFiles = aDir.getFiles();

        // Get Package
        PackageNode packageNode = null;
        List<ClassNode> childClasses = null;

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle nested dir
            if (file.isDir()) {
                if (isPackageDir(file))
                    getClassesForPackageDir(aClassTree, file);
            }

            // Handle plain file: Add to classFiles if interesting and .class
            else {

                // If not class file we want, skip
                if (!isClassFile(file))
                    continue;

                // Create PackageNode if needed
                if (packageNode == null) {
                    String packageName = aDir.getPath().substring(1).replace('/', '.');
                    packageNode = aClassTree.getPackageForName(packageName);
                    childClasses = new ArrayList<>(dirFiles.length);
                }

                // Create/add class node
                String className = getClassNameForClassFile(file);
                ClassNode classNode = new ClassNode(packageNode, className);
                childClasses.add(classNode);
            }
        }

        // Set PackageNode child packages/classes
        if (packageNode != null)
            packageNode.classes = childClasses.toArray(new ClassNode[0]);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        WebSite[] sites = getSites();
        String sitesString = Arrays.toString(sites);
        return getClass().getSimpleName() + ": " + sitesString;
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
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isPackageDir(WebFile aFile)
    {
        if (!aFile.isDir())
            return false;
        if (aFile.getName().indexOf('.') > 0)
            return false;
        String path = aFile.getPath();
        if (!isInterestingPath(path))
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
        if (!isInterestingPath(path))
            return false;
        return true;
    }

    /**
     * Adds an entry (override to ignore).
     */
    private static boolean isInterestingPath(String aPath)
    {
        if (aPath.startsWith("/sun")) return false;
        if (aPath.startsWith("/apple")) return false;
        if (aPath.startsWith("/com/sun")) return false;
        if (aPath.startsWith("/com/apple")) return false;
        if (aPath.startsWith("/com/oracle")) return false;
        if (aPath.startsWith("/java/applet")) return false;
        if (aPath.startsWith("/java/awt/dnd")) return false;
        if (aPath.startsWith("/java/awt/datatransfer")) return false;
        if (aPath.startsWith("/java/awt/im")) return false;
        if (aPath.startsWith("/java/lang/model")) return false;
        if (aPath.startsWith("/java/nio/channels")) return false;
        if (aPath.startsWith("/java/security")) return false;
        if (aPath.startsWith("/java/util/Spliterators")) return false;
        if (aPath.startsWith("/javax")) return false;
        if (aPath.startsWith("/jdk")) return false;
        if (aPath.startsWith("/org/omg")) return false;
        if (aPath.startsWith("/org/w3c")) return false;

        // If anonymous inner class, return false
        int dollar = aPath.lastIndexOf('$');
        if (dollar > 0 && Character.isDigit(aPath.charAt(dollar + 1)))
            return false;

        // Return true
        return true;
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.util.*;

import snap.util.ArrayUtils;
import snap.web.*;

/**
 * A ClassTree subclass to provide classes/packages for an array of class paths.
 */
public class ClassTreeX extends ClassTree {

    // The array of class path sites
    private WebSite[] _classPathSites;

    /**
     * Constructor.
     */
    public ClassTreeX(String[] classPaths)
    {
        super();

        // Get ClassPathSites for ClassPaths
        _classPathSites = getClassPathSitesForClassPaths(classPaths);

        // Iterate over ClassPathSites and add classes for each
        for (WebSite site : _classPathSites) {

            // Get site root files
            WebFile siteRootDir = site.getRootDir();
            WebFile[] rootFiles = siteRootDir.getFiles();

            // Iterate over site root files and create/add packages
            for (WebFile rootFile : rootFiles) {
                if (isPackageDir(rootFile))
                    getClassesForPackageDir(rootFile);
            }
        }
    }

    /**
     * Loads classes from package dir.
     */
    private void getClassesForPackageDir(WebFile aDir)
    {
        // Get package for dir file
        String packageName = getPackageNameForPackageDirFile(aDir);
        ClassTreeNode packageNode = getPackageForName(packageName);

        // Get child class tree nodes
        ClassTreeNode[] children = getClassTreeNodesForPackageDir(packageNode, aDir);
        packageNode.classes = ArrayUtils.filter(children, classTreeNode -> !classTreeNode.isPackage());
        packageNode.packages = ArrayUtils.filter(children, classTreeNode -> classTreeNode.isPackage());

        // Recurse to get packages
        WebFile[] dirFiles = aDir.getFiles();
        for (WebFile file : dirFiles) {
            if (isPackageDir(file))
                getClassesForPackageDir(file);
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String sitesString = Arrays.toString(_classPathSites);
        return getClass().getSimpleName() + ": " + sitesString;
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

        // Add project class path sites (build dirs, jar files)
        for (String classPath : classPaths) {

            // Get URL for class path
            WebURL classPathURL = WebURL.getURL(classPath);
            if (classPathURL == null) {
                System.err.println("ClassTreeX.getClassFileSitesForResolver: Can't resolve class path entry: " + classPath);
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
     * Returns ClassTreeNode array for classes and child packages in given package dir.
     */
    private static ClassTreeNode[] getClassTreeNodesForPackageDir(ClassTreeNode parentPackage, WebFile aDir)
    {
        // Get directory files and create classTreeNode array
        WebFile[] dirFiles = aDir.getFiles();
        List<ClassTreeNode> classTreeNodes = new ArrayList<>(dirFiles.length);

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle class file
            if (isClassFile(file)) {
                String className = getClassNameForClassFile(file);
                ClassTreeNode classNode = new ClassTreeNode(parentPackage, className, false);
                classTreeNodes.add(classNode);
            }

            // Handle package
            if (isPackageDir(file)) {
                String packageName = getPackageNameForPackageDirFile(file);
                ClassTreeNode packageNode = new ClassTreeNode(parentPackage, packageName, true);
                classTreeNodes.add(packageNode);
            }
        }

        // Return array
        return classTreeNodes.toArray(EMPTY_NODE_ARRAY);
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
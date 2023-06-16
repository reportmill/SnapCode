/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.util.*;

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
                    getClassesForPackageDir(file);
            }

            // Handle plain file: Add to classFiles if interesting and .class
            else {

                // If not class file we want, skip
                if (!isClassFile(file))
                    continue;

                // Create PackageNode if needed
                if (packageNode == null) {
                    String packageName = aDir.getPath().substring(1).replace('/', '.');
                    packageNode = getPackageForName(packageName);
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
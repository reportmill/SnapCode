/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.util.*;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
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
        List<ClassTreeNode> rootPackagesList = new ArrayList<>();

        // Iterate over ClassPathSites and add classes for each
        for (WebSite site : _classPathSites) {

            // Get site root files
            WebFile siteRootDir = site.getRootDir();
            WebFile[] rootFiles = siteRootDir.getFiles();

            // Iterate over site root files and create/add packages
            for (WebFile rootFile : rootFiles) {
                if (isPackageDir(rootFile)) {
                    String packageName = rootFile.getName();
                    if (!ListUtils.hasMatch(rootPackagesList, pkgNode -> packageName.equals(pkgNode.fullName))) {
                        ClassTreeNode rootPackage = new ClassTreeNode(this, _rootPackage, packageName, true);
                        rootPackagesList.add(rootPackage);
                    }
                }
            }
        }

        // Add to RootPackage.Children
        ClassTreeNode[] rootPackages = rootPackagesList.toArray(EMPTY_NODE_ARRAY);
        _rootPackage._children = ArrayUtils.addAll(_rootPackage._children, rootPackages);
    }

    /**
     * Returns child ClassTreeNodes for child classes/packages for given package node.
     */
    @Override
    protected ClassTreeNode[] getChildNodesForNode(ClassTreeNode parentNode)
    {
        // Get files
        WebFile[] nodeFiles = getFilesForNode(parentNode);
        if (nodeFiles.length == 0)
            return EMPTY_NODE_ARRAY;

        // Iterate over files and Find child classes and packages for each
        List<ClassTreeNode> classTreeNodes = new ArrayList<>(nodeFiles[0].getFileCount());
        for (WebFile nodeFile : nodeFiles)
            findChildNodesForDirFile(parentNode, nodeFile, classTreeNodes);

        // Return array
        return classTreeNodes.toArray(EMPTY_NODE_ARRAY);
    }

    /**
     * Returns a file for given node.
     */
    private WebFile[] getFilesForNode(ClassTreeNode classTreeNode)
    {
        // Get file path
        String filePath = '/' + classTreeNode.fullName.replace(".", "/");
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
    private void findChildNodesForDirFile(ClassTreeNode parentNode, WebFile dirFile, List<ClassTreeNode> classTreeNodes)
    {
        // Get directory files
        WebFile[] dirFiles = dirFile.getFiles();

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle class file
            if (isClassFile(file)) {
                String className = getClassNameForClassFile(file);
                ClassTreeNode classNode = new ClassTreeNode(this, parentNode, className, false);
                classTreeNodes.add(classNode);
            }

            // Handle package
            if (isPackageDir(file)) {
                String packageName = getPackageNameForPackageDirFile(file);
                ClassTreeNode packageNode = new ClassTreeNode(this, parentNode, packageName, true);
                classTreeNodes.add(packageNode);
            }
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

        // Try again for Swing (different site for Java 9+: jrt:/java.desktop/javax/swing/JFrame.class)
        Class<?> swingClass = null;
        try { swingClass = Class.forName("bogus.swing.JFrame".replace("bogus", "javax")); }
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
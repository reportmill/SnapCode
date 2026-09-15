/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The list of class path sites
    private List<ClassTreeForSite> _classPathSites;

    /**
     * A record to hold package or class entry.
     */
    public record ClassTreeNode(String fullName, boolean isPackage, String simpleName) { }

    /**
     * Returns whether given package name is known.
     */
    public boolean isKnownPackageName(String packageName)
    {
        if (packageName.isEmpty()) return true;
        String filePath = '/' + packageName.replace(".", "/");
        return ListUtils.hasMatch(_classPathSites, classTreeSite -> classTreeSite.isKnownPackageFilePath(filePath));
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    public List<ClassTreeNode> getClassTreeNodesForPackageName(String packageName)
    {
        String filePath = '/' + packageName.replace(".", "/");
        List<ClassTreeNode> classTreeNodes = new ArrayList<>();
        _classPathSites.forEach(site -> site.findClassTreeNodesForPackageFilePath(filePath, classTreeNodes));
        return classTreeNodes;
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toString()  { return getClass().getSimpleName() + ": " + _classPathSites; }

    /**
     * Creates a class tree for given class paths.
     */
    public static ClassTree getClassTreeForClassPaths(String[] classPaths)
    {
        // Get class trees for base modules
        List<String> moduleNames = List.of("java.base", "java.prefs", "java.desktop");
        List<ClassTreeForSite> moduleClassTrees = ListUtils.map(moduleNames, ClassTree::getClassTreeForModuleName);
        List<ClassTreeForSite> classTreeList = new ArrayList<>(moduleClassTrees);

        // Add class trees for class paths
        List<ClassTreeForSite> classPathSites = ArrayUtils.mapNonNullToList(classPaths, ClassTree::getClassTreeForClassPath);
        classTreeList.addAll(classPathSites);

        // Return class tree for list
        ClassTree classTree = new ClassTree();
        classTree._classPathSites = classTreeList;
        return classTree;
    }

    /**
     * Returns the ClassTree for module name.
     */
    public static ClassTreeForSite getClassTreeForModuleName(String moduleName)
    {
        WebURL moduleUrl = WebURL.getUrl("jrt:/" + moduleName); assert moduleUrl != null;
        return new ClassTreeForSite(moduleUrl.getSite());
    }

    /**
     * Returns a ClassTree for given class path.
     */
    public static ClassTreeForSite getClassTreeForClassPath(String classPath)
    {
        // Get URL for class path
        WebURL classPathURL = WebURL.getUrl(classPath);
        if (classPathURL == null) {
            System.err.println("ClassTree.getClassFileSitesForResolver: Can't resolve class path entry: " + classPath);
            return null;
        }

        // Get site for class path entry and add to sites
        WebSite classPathSite = classPathURL.getAsSite();
        return new ClassTreeForSite(classPathSite);
    }

    /**
     * This class represents a class tree for a website.
     */
    public static class ClassTreeForSite {

        // The web site
        private WebSite _site;

        /**
         * Constructor.
         */
        public ClassTreeForSite(WebSite aSite)  { _site = aSite; }

        /**
         * Returns ClassTreeNode array for classes and child packages for given node.
         */
        public List<ClassTreeNode> getClassTreeNodesForPackageName(String packageName)
        {
            String filePath = '/' + packageName.replace(".", "/");
            List<ClassTreeNode> classTreeNodes = new ArrayList<>();
            findClassTreeNodesForPackageFilePath(filePath, classTreeNodes);
            return classTreeNodes;
        }

        /**
         * Returns whether given package file path is known.
         */
        boolean isKnownPackageFilePath(String filePath)
        {
            WebFile file = _site.getFileForPath(filePath);
            return file != null && file.isDir() && file.getPath().equals(filePath);
        }

        /**
         * Returns ClassTreeNode array for classes and child packages for given node.
         */
        void findClassTreeNodesForPackageFilePath(String filePath, List<ClassTreeNode> classTreeNodes)
        {
            // Get files
            WebFile nodeFile = _site.getFileForPath(filePath);
            if (nodeFile == null)
                return;

            // If root package and base module, add primitives
            if (filePath.equals("/") && _site.getName().endsWith("java.base")) {
                List<Class<?>> primitives = List.of(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
                List<ClassTreeNode> primitiveNodes = ListUtils.map(primitives, cls -> ClassTreeUtils.createClassTreeNode(cls.getName(), false));
                classTreeNodes.addAll(primitiveNodes);
            }

            // Iterate over files and Find child classes and packages for each
            ClassTreeUtils.findChildNodesForDirFile(nodeFile, classTreeNodes);
        }
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.*;
import snap.web.*;
import java.util.*;

/**
 * Represents a tree of packages/classes.
 */
public abstract class ClassTree {

    /**
     * A record to hold package or class entry.
     */
    public record ClassTreeNode(String fullName, boolean isPackage, String simpleName) { }

    /**
     * Returns whether given package name is known.
     */
    public abstract boolean isKnownPackageName(String packageName);

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    public abstract List<ClassTreeNode> getClassTreeNodesForPackageName(String packageName);

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
        return new ClassTreeForSites(classTreeList);
    }

    /**
     * Returns the ClassTree for module name.
     */
    static ClassTreeForSite getClassTreeForModuleName(String moduleName)
    {
        if (moduleName.equals("java.base") || moduleName.equals("java.desktop"))
            return new ClassTreeForModuleFile(moduleName);

        WebURL moduleUrl = WebURL.getUrl("jrt:/" + moduleName); assert moduleUrl != null;
        return new ClassTreeForSite(moduleUrl.getSite());
    }

    /**
     * Returns a ClassTree for given class path.
     */
    private static ClassTreeForSite getClassTreeForClassPath(String classPath)
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
    static class ClassTreeForSite extends ClassTree {

        // The web site
        private WebSite _site;

        /**
         * Constructor.
         */
        public ClassTreeForSite(WebSite aSite)  { _site = aSite; }

        @Override
        public boolean isKnownPackageName(String packageName)
        {
            if (packageName.isEmpty()) return true;
            String filePath = '/' + packageName.replace(".", "/");
            return isKnownPackageFilePath(filePath);
        }

        @Override
        public List<ClassTreeNode> getClassTreeNodesForPackageName(String packageName)
        {
            String filePath = '/' + packageName.replace(".", "/");
            List<ClassTreeNode> classTreeNodes = new ArrayList<>();
            findClassTreeNodesForPackageFilePath(filePath, classTreeNodes);
            return classTreeNodes;
        }

        // Returns whether given package file path is known
        boolean isKnownPackageFilePath(String packageFilePath)
        {
            WebFile file = _site.getFileForPath(packageFilePath);
            return file != null && file.isDir() && file.getPath().equals(packageFilePath);
        }

        // Finds ClassTreeNode for classes and child packages for given package file path
        void findClassTreeNodesForPackageFilePath(String packageFilePath, List<ClassTreeNode> classTreeNodes)
        {
            WebFile packageFile = _site.getFileForPath(packageFilePath);
            if (packageFile == null)
                return;

            // Iterate over files and Find child classes and packages for each
            ClassTreeUtils.findChildNodesForDirFile(packageFile, classTreeNodes);
        }

        @Override
        public String toString()  { return getClass().getSimpleName() + ": " + _site.getUrlAddress(); }
    }

    /**
     * A class tree for a list of class tree sites.
     */
    static class ClassTreeForSites extends ClassTree {

        // The list of class path sites
        private List<ClassTreeForSite> _classPathSites;

        public ClassTreeForSites(List<ClassTreeForSite> classPathSites)
        {
            super();
            _classPathSites = classPathSites;
        }

        @Override
        public boolean isKnownPackageName(String packageName)
        {
            if (packageName.isEmpty()) return true;
            String filePath = '/' + packageName.replace(".", "/");
            return ListUtils.hasMatch(_classPathSites, classTreeSite -> classTreeSite.isKnownPackageFilePath(filePath));
        }

        @Override
        public List<javakit.resolver.ClassTree.ClassTreeNode> getClassTreeNodesForPackageName(String packageName)
        {
            String filePath = '/' + packageName.replace(".", "/");

            // Create nodes list - if root package, add primitives
            List<javakit.resolver.ClassTree.ClassTreeNode> classTreeNodes = new ArrayList<>();
            if (packageName.isEmpty()) {
                List<Class<?>> primitives = List.of(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
                List<ClassTreeNode> primitiveNodes = ListUtils.map(primitives, cls -> ClassTreeUtils.createClassTreeNode(cls.getName(), false));
                classTreeNodes.addAll(primitiveNodes);
            }

            _classPathSites.forEach(site -> site.findClassTreeNodesForPackageFilePath(filePath, classTreeNodes));
            return classTreeNodes;
        }

        @Override
        public String toString()  { return getClass().getSimpleName() + ": " + _classPathSites; }
    }

    /**
     * A class tree for a module that has a provided contents file.
     */
    static class ClassTreeForModuleFile extends ClassTreeForSite {

        // Map of class nodes for class path
        private Map<String,List<ClassTreeNode>> _classPathNodes;

        public ClassTreeForModuleFile(String moduleName)
        {
            super(null);
            _classPathNodes = ClassTreeUtils.readClassNodesForModuleName(moduleName);
        }

        // Returns whether given package file path is known
        @Override
        boolean isKnownPackageFilePath(String packageFilePath)  { return _classPathNodes.containsKey(packageFilePath); }

        // Finds ClassTreeNode for classes and child packages for given package file path
        @Override
        void findClassTreeNodesForPackageFilePath(String packageFilePath, List<ClassTreeNode> classTreeNodes)
        {
            List<ClassTreeNode> nodesForPath = _classPathNodes.get(packageFilePath);
            if (nodesForPath != null)
                classTreeNodes.addAll(nodesForPath);
        }
    }
}

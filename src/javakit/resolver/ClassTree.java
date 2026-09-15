/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.*;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The list of class path sites
    private List<ClassTreeSite> _classPathSites;

    /**
     * Constructor.
     */
    public ClassTree(String[] classPaths)
    {
        super();
        _classPathSites = getClassPathSitesForClassPaths(classPaths);
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
     * Returns whether given package name is known.
     */
    public boolean isKnownPackageName(String packageName)
    {
        if (packageName.isEmpty()) return true;
        String filePath = '/' + packageName.replace(".", "/");
        return ListUtils.hasMatch(_classPathSites, classTreeSite -> classTreeSite.isKnownPackageFilePath(filePath));
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toString()  { return getClass().getSimpleName() + ": " + _classPathSites; }

    /**
     * Returns a simple class name for given node name.
     */
    private static String getSimpleNodeName(String aNodeName)
    {
        // Get index of last '$' or '.'
        int sepIndex = aNodeName.lastIndexOf('$');
        if (sepIndex < 0)
            sepIndex = aNodeName.lastIndexOf('.');

        // Return ClassName stripped of package and/or parent-class
        return aNodeName.substring(sepIndex + 1);
    }

    /**
     * Returns an array of ClassTreeSites for given resolver class path.
     */
    private static List<ClassTreeSite> getClassPathSitesForClassPaths(String[] classPaths)
    {
        List<ClassTreeSite> classFileSites = new ArrayList<>();

        // Add JRT sites
        List<String> moduleNames = ListUtils.of("java.base", "java.prefs", "java.desktop");
        moduleNames.forEach(moduleName -> classFileSites.add(ClassTreeSite.getSiteForModuleName(moduleName)));

        // Add project class path sites (build dirs, jar files)
        for (String classPath : classPaths) {

            // Get URL for class path
            WebURL classPathURL = WebURL.getUrl(classPath);
            if (classPathURL == null) {
                System.err.println("ClassTree.getClassFileSitesForResolver: Can't resolve class path entry: " + classPath);
                continue;
            }

            // Get site for class path entry and add to sites
            WebSite classPathSite = classPathURL.getAsSite();
            classFileSites.add(new ClassTreeSite(classPathSite));
        }

        return classFileSites;
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

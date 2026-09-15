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
     * A record to hold package or class entry.
     */
    public record ClassTreeNode(String fullName, boolean isPackage, String simpleName) { }

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
     * Returns an array of ClassTreeSites for given resolver class path.
     */
    private static List<ClassTreeSite> getClassPathSitesForClassPaths(String[] classPaths)
    {
        // Get sites for base modules
        List<String> moduleNames = List.of("java.base", "java.prefs", "java.desktop");
        List<ClassTreeSite> moduleSites = ListUtils.map(moduleNames, ClassTreeSite::getSiteForModuleName);
        List<ClassTreeSite> classFileSites = new ArrayList<>(moduleSites);

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
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.web.WebSite;
import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This Workspace subclass is enhanced to work with full JDK.
 */
public class WorkspaceX extends Workspace {

    /**
     * Constructor.
     */
    public WorkspaceX()
    {
        super();
    }

    /**
     * Returns the project class loader.
     */
    @Override
    protected ClassLoader createClassLoader()
    {
        if (SnapUtils.isWebVM)
            return ClassLoader.getSystemClassLoader();

        // Iterate over projects and add Project.ClassPaths for each
        Project[] projects = getProjects();
        String[] classPaths = new String[0];
        for (Project proj : projects) {
            String[] projClassPaths = proj.getRuntimeClassPaths();
            classPaths = ArrayUtils.addAllUnique(classPaths, projClassPaths);
        }

        // Remove SnapKit/SnapCharts paths
        String snapKitPath = ArrayUtils.findMatch(classPaths, path -> path.contains("snapkit"));
        String snapChartsPath = ArrayUtils.findMatch(classPaths, path -> path.contains("snapcharts"));
        if (snapKitPath != null)
            classPaths = ArrayUtils.remove(classPaths, snapKitPath);
        if (snapChartsPath != null)
            classPaths = ArrayUtils.remove(classPaths, snapChartsPath);

        // Get all project ClassPath URLs
        URL[] urls = FilePathUtils.getUrlsForPaths(classPaths);

        // Get System ClassLoader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        if (snapKitPath == null && snapChartsPath == null)
            sysClassLoader = sysClassLoader.getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        ClassLoader urlClassLoader = new ProjectClassLoaderX(urls, sysClassLoader);

        // Return
        return urlClassLoader;
    }

    /**
     * Clears the class loader.
     */
    @Override
    public void clearClassLoader()
    {
        // If ClassLoader closeable, close it
        if (_classLoader instanceof ProjectClassLoaderX && _classLoader instanceof Closeable) {
            try {  ((Closeable) _classLoader).close(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }

        // Do normal version
        super.clearClassLoader();
    }

    /**
     * Needs unique name so that when debugging SnapCode, we can ignore classes loaded by Project.
     */
    public static class ProjectClassLoaderX extends URLClassLoader {

        public ProjectClassLoaderX(URL[] urls, ClassLoader aPar)
        {
            super(urls, aPar);
        }
    }

    /**
     * Creates a project for given site.
     */
    @Override
    protected Project createProjectForSite(WebSite aSite)
    {
        return new ProjectX(this, aSite);
    }
}

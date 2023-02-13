/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.Pod;
import javakit.project.Project;
import javakit.project.ProjectSet;
import snap.util.FilePathUtils;
import snap.web.WebSite;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This Pod subclass is enhanced to work with full JDK.
 */
public class PodX extends Pod {

    /**
     * Constructor.
     */
    public PodX()
    {
        super();
    }

    /**
     * Returns the project class loader.
     */
    @Override
    protected ClassLoader createClassLoader()
    {
        // Get all project ClassPath URLs
        ProjectSet projectSet = getProjectSet();
        String[] projSetClassPaths = projectSet.getClassPaths();
        URL[] urls = FilePathUtils.getURLs(projSetClassPaths);

        // Get System ClassLoader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        ClassLoader urlClassLoader = new ProjectClassLoaderX(urls, sysClassLoader);

        // Return
        return urlClassLoader;
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
     * Returns the project class loader.
     */
    public ClassLoader createLibClassLoader()
    {
        // Create ClassLoader for ProjectSet.ClassPath URLs and SystemClassLoader.Parent and return
        ProjectSet projectSet = getProjectSet();
        String[] libPaths = projectSet.getLibPaths();
        URL[] urls = FilePathUtils.getURLs(libPaths);

        // Get System ClassLoader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        return new ProjectClassLoaderX(urls, systemClassLoader);
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

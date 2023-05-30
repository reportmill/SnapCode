/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.FilePathUtils;
import snap.web.WebSite;
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
        // Get all project ClassPath URLs
        String[] classPaths = getClassPaths();
        URL[] urls = FilePathUtils.getUrlsForPaths(classPaths);

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
     * Creates a project for given site.
     */
    @Override
    protected Project createProjectForSite(WebSite aSite)
    {
        return new ProjectX(this, aSite);
    }
}

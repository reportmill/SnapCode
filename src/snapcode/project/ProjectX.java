/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.BuildIssues;
import javakit.project.JavaFileBuilder;
import javakit.project.Project;
import javakit.project.ProjectConfig;
import javakit.resolver.Resolver;
import snap.util.FilePathUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A class to manage build attributes and behavior for a WebSite.
 */
public class ProjectX extends Project {

    // The project that loaded us
    protected ProjectX  _parent;

    // The set of projects this project depends on
    private ProjectSet  _projSet = new ProjectSet(this);

    // A class to read .classpath file
    private ProjectConfigFile  _projConfigFile;

    /**
     * Creates a new Project for WebSite.
     */
    public ProjectX(WebSite aSite)
    {
        super(aSite);

        // Create/set ProjectBuilder.JavaFileBuilderImpl
        JavaFileBuilder javaFileBuilder = new JavaFileBuilderImpl(this);
        _projBuilder.setJavaFileBuilder(javaFileBuilder);

        // Load dependent projects
        getProjects();
    }

    /**
     * Returns the top most project.
     */
    @Override
    public ProjectX getRootProject()
    {
        return _parent != null ? _parent.getRootProject() : this;
    }

    /**
     * Returns the list of projects this project depends on.
     */
    public ProjectX[] getProjects()
    {
        return _projSet.getProjects();
    }

    /**
     * Returns the set of projects this project depends on.
     */
    public ProjectSet getProjectSet()  { return _projSet; }

    /**
     * Returns the project class loader.
     */
    @Override
    protected ClassLoader createClassLoader()
    {
        // If RootProject, return RootProject.ClassLoader
        ProjectX rproj = getRootProject();
        if (rproj != this)
            return rproj.createClassLoader();

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
     * Clears the class loader.
     */
    protected void clearClassLoader()
    {
        // If ClassLoader closeable, close it
        if (_classLoader instanceof Closeable)
            try {  ((Closeable) _classLoader).close(); }
            catch (Exception e) { throw new RuntimeException(e); }

        // Clear
        _classLoader = null;
        _resolver = null;
    }

    /**
     * Returns the class for given file.
     */
    public Class<?> getClassForFile(WebFile aFile)
    {
        String className = getClassNameForFile(aFile);
        Resolver resolver = getResolver();
        return resolver.getClassForName(className);
    }

    /**
     * Override to create ProjectConfig from .classpath file.
     */
    @Override
    protected ProjectConfig createProjectConfig()
    {
        _projConfigFile = new ProjectConfigFile(this);
        return _projConfigFile.getProjectConfig();
    }

    /**
     * Returns whether file is config file.
     */
    @Override
    protected boolean isConfigFile(WebFile aFile)
    {
        WebFile configFile = _projConfigFile.getFile();
        return aFile == configFile;
    }

    /**
     * Reads the settings from project settings file(s).
     */
    public void readSettings()
    {
        _projConfigFile.readFile();
    }

    /**
     * Called when file added.
     */
    public void fileAdded(WebFile aFile)
    {
        if (isConfigFile(aFile))
            readSettings();

        // Add build file
        _projBuilder.addBuildFile(aFile, false);
    }

    /**
     * Called when file removed.
     */
    public void fileRemoved(WebFile aFile)
    {
        // Remove build files
        _projBuilder.removeBuildFile(aFile);

        // Remove BuildIssues for file
        ProjectX rootProj = getRootProject();
        BuildIssues buildIssues = rootProj.getBuildIssues();
        buildIssues.removeIssuesForFile(aFile);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        // If File is config file, read file
        if (isConfigFile(aFile))
            readSettings();

        // If plain file, add as BuildFile
        if (!aFile.isDir())
            _projBuilder.addBuildFile(aFile, false);
    }

    /**
     * Deletes the project.
     */
    public void deleteProject(TaskMonitor aTM) throws Exception
    {
        // Start TaskMonitor
        aTM.startTasks(1);
        aTM.beginTask("Deleting files", -1);

        // Clear ClassLoader
        clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandbox();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        aTM.endTask();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "Project: " + getSite();
    }

    /**
     * Returns the project for a given site.
     */
    public static ProjectX getProjectForFile(WebFile aFile)
    {
        WebSite fileSite = aFile.getSite();
        return getProjectForSite(fileSite);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized ProjectX getProjectForSite(WebSite aSite)
    {
        Project proj = Project.getProjectForSite(aSite);
        return (ProjectX) proj;
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.*;
import javakit.resolver.Resolver;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A class to manage build attributes and behavior for a WebSite.
 */
public class ProjectX extends Project {

    // A class to read .classpath file
    private ProjectConfigFile  _projConfigFile;

    /**
     * Creates a new Project for WebSite.
     */
    public ProjectX(Pod aPod, WebSite aSite)
    {
        super(aPod, aSite);

        // Create/set ProjectBuilder.JavaFileBuilderImpl
        JavaFileBuilder javaFileBuilder = new JavaFileBuilderImpl(this);
        _projBuilder.setJavaFileBuilder(javaFileBuilder);
    }

    /**
     * Returns the top most project.
     */
    @Override
    public ProjectX getRootProject()  { return this; }

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
        Pod pod = getPod();
        pod.clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandbox();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        aTM.endTask();
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
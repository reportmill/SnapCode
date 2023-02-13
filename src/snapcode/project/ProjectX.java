/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.*;
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
    public ProjectX(WorkSpace aWorkSpace, WebSite aSite)
    {
        super(aWorkSpace, aSite);

        // Create/set ProjectBuilder.JavaFileBuilderImpl
        JavaFileBuilder javaFileBuilder = new JavaFileBuilderImpl(this);
        _projBuilder.setJavaFileBuilder(javaFileBuilder);
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
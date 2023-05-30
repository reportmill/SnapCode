/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.resolver.Resolver;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class manages all aspects of a project.
 */
public class Project extends PropObject {

    // The Workspace that owns this project
    private Workspace  _workspace;

    // The encapsulated data site
    protected WebSite  _site;

    // Whether this project is read-only
    private boolean _readOnly;

    // The child projects this project depends on
    private Project[]  _projects;

    // BuildFile
    protected BuildFile _buildFile;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The set of projects this project depends on
    private ProjectSet  _projSet;

    // Constants for properties
    private static final String Projects_Prop = "Projects";

    /**
     * Creates a new Project for WebSite.
     */
    public Project(Workspace aWorkspace, WebSite aSite)
    {
        _workspace = aWorkspace;

        // Set Site
        setSite(aSite);

        // Set ReadOnly
        boolean isReadOnly = aSite.getURL().getScheme().startsWith("http");
        if (isReadOnly)
            setReadOnly(isReadOnly);

        // If site doesn't exist, create root directory, src and bin
        if (!aSite.getExists()) {
            if (!isReadOnly) {
                aSite.getRootDir().save();
                aSite.createFileForPath("/src", true).save();
                aSite.createFileForPath("/bin", true).save();
            }
        }

        // Create/set ProjectFiles, ProjectBuilder
        _projFiles = new ProjectFiles(this);
        _projBuilder = new ProjectBuilder(this);

        // Get child projects
        _projects = getProjectsFromBuildFile();
    }

    /**
     * Returns the Workspace that manages this project.
     */
    public Workspace getWorkspace()  { return _workspace; }

    /**
     * Returns root project if part of hierarchy.
     */
    public Project getRootProject()  { return this; }

    /**
     * Returns the project name.
     */
    public String getName()
    {
        return _site.getName();
    }

    /**
     * Returns the encapsulated WebSite.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Sets the encapsulated WebSite.
     */
    protected void setSite(WebSite aSite)
    {
        _site = aSite;
        _site.setProp(Project.class.getSimpleName(), this);
    }

    /**
     * Returns whether project is read-only.
     */
    public boolean isReadOnly()  { return _readOnly; }

    /**
     * Sets whether project is read-only.
     */
    public void setReadOnly(boolean aValue)  { _readOnly = aValue; }

    /**
     * Returns the BuildFile that manages project properties.
     */
    public BuildFile getBuildFile()
    {
        // If already set, just return
        if (_buildFile != null) return _buildFile;

        // Create BuildFile
        BuildFile buildFile = getBuildFileImpl();

        // Add PropChangeListener to save changes to file and clear ClassPathInfo
        buildFile.addPropChangeListener(pc -> buildFileDidChange());

        // Set, return
        return _buildFile = buildFile;
    }

    /**
     * Returns the BuildFile that manages project properties.
     */
    protected BuildFile getBuildFileImpl()
    {
        return new BuildFile(this);
    }

    /**
     * Returns the paths needed to run project.
     */
    public String[] getClassPaths()
    {
        // Get build path
        String buildPath = _buildFile.getBuildPathAbsolute();
        String[] classPaths = { buildPath };

        // Get library paths
        String[] libPaths = _buildFile.getLibPathsAbsolute();
        if (libPaths.length > 0)
            classPaths = ArrayUtils.add(libPaths, buildPath, 0);

        // Return
        return classPaths;
    }

    /**
     * Returns the paths needed to compile project (does not include project build dir).
     */
    public String[] getCompilerClassPaths()
    {
        // Get LibPaths for this proj
        String[] libPaths = _buildFile.getLibPathsAbsolute();

        // Get projects
        Project[] projects = getProjects();

        // Get list for CompilerClassPaths with base paths
        List<String> compilerClassPaths = new ArrayList<>();
        Collections.addAll(compilerClassPaths, libPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(compilerClassPaths, projClassPaths);
        }

        // Return array
        return compilerClassPaths.toArray(new String[0]);
    }

    /**
     * Returns the set of projects this project depends on.
     */
    public ProjectSet getProjectSet()
    {
        if (_projSet != null) return _projSet;

        Project[] projects = ArrayUtils.add(getProjects(), this, 0);
        return _projSet = new ProjectSet(projects);
    }

    /**
     * Returns an array of projects that this project depends on.
     */
    public Project[] getProjects()  { return _projects; }

    /**
     * Adds a project.
     */
    public void addProject(Project aProj)
    {
        // If already set, just return
        if (ArrayUtils.containsId(_projects, aProj)) return;

        // Add project
        _projects = ArrayUtils.add(_projects, aProj);
        _projSet = null;

        // Fire prop change
        int index = _projects.length - 1;
        firePropChange(Projects_Prop, null, aProj, index);
    }

    /**
     * Removes a project.
     */
    public void removeProject(Project aProj)
    {
        // If not present, just return
        int index = ArrayUtils.indexOfId(_projects, aProj);
        if (index < 0)
            return;

        // Remove project
        _projects = ArrayUtils.remove(_projects, index);
        _projSet = null;

        // Fire prop change
        firePropChange(Projects_Prop, aProj, null, index);
    }

    /**
     * Returns a child project for given name.
     */
    public Project getProjectForName(String aName)
    {
        String name = aName.startsWith("/") ? aName.substring(1) : aName;
        return ArrayUtils.findMatch(_projects, proj -> proj.getName().equals(name));
    }

    /**
     * Returns a project for given path.
     */
    public Project getProjectForPath(String projectPath)
    {
        // Get parent site
        WebSite projectSite = getSite();
        WebURL parentSiteURL = projectSite.getURL();
        WebSite parentSite = parentSiteURL.getSite();

        // Get URL and site for project path
        WebURL projURL = parentSite.getURL(projectPath);
        WebSite projSite = projURL.getAsSite();

        // Get Project
        Workspace workspace = getWorkspace();
        return workspace.getProjectForSite(projSite);
    }

    /**
     * Adds a dependent project.
     */
    public void addProjectForPath(String aPath)
    {
        // Get project path
        String projPath = aPath;
        if (!projPath.startsWith("/"))
            projPath = '/' + projPath;

        // Get project
        Project proj = getProjectForPath(aPath);
        if (proj == null)
            return;
        addProject(proj);

        // Add to BuildFile
        BuildFile buildFile = getBuildFile();
        buildFile.addProjectPath(projPath);
    }

    /**
     * Removes a dependent project.
     */
    public void removeProjectForPath(String projectPath)
    {
        // Get project and remove
        Project proj = getProjectForPath(projectPath);
        removeProject(proj);

        // Remove from build file
        BuildFile buildFile = getBuildFile();
        buildFile.removeProjectPath(projectPath);
    }

    /**
     * Returns the projects this project depends on.
     */
    private Project[] getProjectsFromBuildFile()
    {
        // Create list of projects from BuildFile.ProjectPaths
        BuildFile buildFile = getBuildFile();
        String[] projPaths = buildFile.getProjectPaths();
        List<Project> projs = new ArrayList<>();

        // Iterate over project paths
        for (String projPath : projPaths) {

            // Get Project
            Project proj = getProjectForPath(projPath);

            // Add to list
            Project[] childProjects = proj.getProjects();
            ListUtils.addAllUnique(projs, childProjects);
            ListUtils.addUnique(projs, proj);
        }

        // Return array
        return projs.toArray(new Project[0]);
    }

    /**
     * Returns the ProjectBuilder.
     */
    public ProjectBuilder getBuilder()  { return _projBuilder; }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        Workspace workspace = getWorkspace();
        return workspace.getResolver();
    }

    /**
     * Returns the ProjectFiles that manages project files.
     */
    public ProjectFiles getProjectFiles()  { return _projFiles; }

    /**
     * Returns the source directory.
     */
    public WebFile getSourceDir()  { return _projFiles.getSourceDir(); }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()  { return _projFiles.getBuildDir(); }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        return _site.getFileForPath(aPath);
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        return _projFiles.getSourceFile(aPath, doCreate, isDir);
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
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        return _projFiles.getClassNameForFile(aFile);
    }

    /**
     * Watches Project.BuildFile for dependency change to reset ClassPathInfo.
     */
    private void buildFileDidChange()
    {
        // Save build file
        if (!isReadOnly()) {
            BuildFile buildFile = getBuildFile();
            buildFile.writeFile();
        }

        // Clear Workspace classloader
        Workspace workspace = getWorkspace();
        workspace.clearClassLoader();
    }

    /**
     * Called when file added.
     */
    public void fileAdded(WebFile aFile)
    {
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
        Workspace workspace = getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();
        buildIssues.removeIssuesForFile(aFile);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
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
        Workspace workspace = getWorkspace();
        workspace.clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandbox();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        aTM.endTask();
    }

    /**
     * Returns a class loader to be used with compiler.
     */
    public ClassLoader createCompilerClassLoader()
    {
        // Should be new URLClassLoader(getClassPathURLs())
        return ClassLoader.getSystemClassLoader();
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
    public static Project getProjectForFile(WebFile aFile)
    {
        WebSite site = aFile.getSite();
        return getProjectForSite(site);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized Project getProjectForSite(WebSite aSite)
    {
        Project proj = (Project) aSite.getProp(Project.class.getSimpleName());
        return proj;
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.resolver.JavaClass;
import javakit.resolver.Resolver;
import snap.props.PropChange;
import snap.props.PropObject;
import snap.util.*;
import snap.web.*;
import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages all aspects of a project.
 */
public class Project extends PropObject {

    // The Workspace that owns this project
    private Workspace  _workspace;

    // The encapsulated data site
    protected WebSite  _site;

    // The child projects this project depends on
    private List<Project>  _projects;

    // BuildFile
    protected BuildFile _buildFile;

    // ProjectFiles
    protected ProjectFiles  _projFiles;

    // The ProjectBuilder
    protected ProjectBuilder  _projBuilder;

    // The ClassLoader for compiled class info
    protected ClassLoader _classLoader;

    // The resolver
    protected Resolver _resolver;

    // The JavaAgents created for this project
    private List<JavaAgent> _javaAgents = new ArrayList<>();

    // Constants for properties
    private static final String Projects_Prop = "Projects";

    /**
     * Creates a new Project for WebSite.
     */
    public Project(Workspace aWorkspace, WebSite aSite)
    {
        super();
        _workspace = aWorkspace;

        // Set Site
        setSite(aSite);

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
     * Returns the project name.
     */
    public String getName()  { return _site.getName(); }

    /**
     * Returns the encapsulated WebSite.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Sets the encapsulated WebSite.
     */
    protected void setSite(WebSite aSite)
    {
        // Set site and add Site.Project property to this project
        _site = aSite;
        _site.setProp(Project.class.getSimpleName(), this);

        // If site doesn't exist, create root dir, src dir and build file
        if (!aSite.getExists()) {
            WebFile srcDir = _site.createFileForPath("/src", true);
            srcDir.save();
            getBuildFile().writeFile();
        }
    }

    /**
     * Returns the source URL.
     */
    public WebURL getSourceURL()
    {
        // If VersionControl set, return remote URL
        WebSite projSite = getSite();
        VersionControl versionControl = VersionControl.getVersionControlForProjectSite(projSite);
        if (versionControl != null) {
            WebURL remoteURL = versionControl.getRemoteSiteUrl();
            if (remoteURL != null)
                return remoteURL;
        }

        // Return site URL
        return projSite.getURL();
    }

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
        buildFile.addPropChangeListener(this::handleBuildFileChange);

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
     * Returns an array of projects that this project depends on.
     */
    public List<Project> getProjects()  { return _projects; }

    /**
     * Adds a project.
     */
    public void addProject(Project aProj)
    {
        // If already set, just return
        if (_projects.contains(aProj)) return;

        // Add project
        _projects = new ArrayList<>(_projects);
        _projects.add(aProj);

        // Fire prop change
        int index = _projects.size() - 1;
        firePropChange(Projects_Prop, null, aProj, index);
    }

    /**
     * Returns a child project for given name.
     */
    public Project getProjectForName(String aName)
    {
        String name = aName.startsWith("/") ? aName.substring(1) : aName;
        return ListUtils.findMatch(_projects, proj -> proj.getName().equals(name));
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
        WebURL projURL = parentSite.getUrlForPath(projectPath);
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
        buildFile.addProjectDependencyForProjectPath(projPath);
    }

    /**
     * Returns the projects this project depends on.
     */
    private List<Project> getProjectsFromBuildFile()
    {
        // Create list of projects from BuildFile.ProjectPaths
        BuildFile buildFile = getBuildFile();
        String[] projPaths = buildFile.getProjectDependenciesNames();
        List<Project> projs = new ArrayList<>();

        // Iterate over project paths
        for (String projPath : projPaths) {

            // Get Project
            Project proj = getProjectForPath(projPath);

            // Add to list
            List<Project> childProjects = proj.getProjects();
            childProjects.forEach(childProj -> ListUtils.addUnique(projs, childProj));
            ListUtils.addUnique(projs, proj);
        }

        // Return
        return projs;
    }

    /**
     * Returns the ProjectBuilder.
     */
    public ProjectBuilder getBuilder()  { return _projBuilder; }

    /**
     * Returns the paths needed to compile project (does not include project build dir).
     */
    public String[] getCompileClassPaths()
    {
        // Get build file dependencies
        BuildFile buildFile = getBuildFile();
        BuildDependency[] dependencies = buildFile.getDependencies();
        String[] compileClassPaths = new String[0];

        // If BuildFile.IncludeSnapKitRuntime, add SnapKit jar path
        if (buildFile.isIncludeSnapKitRuntime()) {
            boolean includeGreenfoot = buildFile.isIncludeGreenfootRuntime();
            String[] snapKitPaths = ProjectUtils.getSnapKitAndSnapChartsClassPaths(includeGreenfoot);
            compileClassPaths = ArrayUtils.addAllUnique(snapKitPaths);
        }

        // Iterate over compile dependencies and add runtime class paths for each
        for (BuildDependency dependency : dependencies) {
            String[] classPaths = dependency.getClassPaths();
            if (classPaths != null)
                compileClassPaths = ArrayUtils.addAllUnique(compileClassPaths, classPaths);
            else System.err.println("Project.getCompileClassPaths: Can't get class path for: " + dependency);
        }

        // Return
        return compileClassPaths;
    }

    /**
     * Returns the paths needed to run project.
     */
    public String[] getRuntimeClassPaths()
    {
        // Get build path
        String buildPath = _buildFile.getBuildPathAbsolute(); // Will be null for Project with http site
        String[] runtimeClassPaths = buildPath != null ? new String[] { buildPath } : new String[0];

        // Add CompileClassPaths
        String[] compileClassPaths = getCompileClassPaths();
        runtimeClassPaths = ArrayUtils.addAll(runtimeClassPaths, compileClassPaths);

        // Return
        return runtimeClassPaths;
    }

    /**
     * Returns the ClassLoader for runtime class paths.
     */
    public ClassLoader getRuntimeClassLoader()
    {
        if (_classLoader != null) return _classLoader;
        return _classLoader = createRuntimeClassLoader();
    }

    /**
     * Creates the ClassLoader for runtime class paths.
     */
    private ClassLoader createRuntimeClassLoader()
    {
        // Get all project class paths
        String[] classPaths = getRuntimeClassPaths();

        // Get all project ClassPath URLs
        URL[] urls = FilePathUtils.getUrlsForPaths(classPaths);

        // Get root ClassLoader
        ClassLoader workspaceClassLoader = ClassLoader.getSystemClassLoader();

        // If IncludeSnapKitRuntime is set, use platform class loader instead
        BuildFile buildFile = getBuildFile();
        if (!buildFile.isIncludeSnapKitRuntime())
            workspaceClassLoader = workspaceClassLoader.getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        ClassLoader urlClassLoader = new SnapCodeDebugClassLoader(urls, workspaceClassLoader);

        // Return
        return urlClassLoader;
    }

    /**
     * Clears the class loader.
     */
    public void clearClassLoader()
    {
        // If ClassLoader closeable, close it
        if (_classLoader instanceof SnapCodeDebugClassLoader) {
            try {  ((Closeable) _classLoader).close(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }

        _classLoader = null;
    }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver()
    {
        if (_resolver != null) return _resolver;
        return _resolver = new Resolver(this);
    }

    /**
     * Returns a class loader to be used with compiler.
     */
    public ClassLoader createCompilerClassLoader()
    {
        // Get CompilerClassPaths and ClassPathUrls
        String[] classPaths = getCompileClassPaths();
        URL[] classPathUrls = FilePathUtils.getUrlsForPaths(classPaths);

        // Get System ClassLoader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create/Return URLClassLoader for classPath
        return new URLClassLoader(classPathUrls, systemClassLoader);
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
     * Returns the root directory.
     */
    public WebFile getRootDir()  { return _site.getRootDir(); }

    /**
     * Returns a file for given path.
     */
    public WebFile getFileForPath(String aPath)  { return _site.getFileForPath(aPath); }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFileForPath(String aPath)  { return _projFiles.getSourceFileForPath(aPath); }

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
     * Returns the java class for given file.
     */
    public JavaClass getJavaClassForFile(WebFile aFile)
    {
        String className = getClassNameForFile(aFile);
        Resolver resolver = getResolver();
        return resolver.getJavaClassForName(className);
    }

    /**
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        return _projFiles.getClassNameForFile(aFile);
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFileForClassName(String aClassName)
    {
        // Probably a stupid optimization
        if (aClassName.startsWith("java") && (aClassName.startsWith("java.") || aClassName.startsWith("javax.")))
            return null;

        // Check this project
        ProjectFiles projFiles = getProjectFiles();
        WebFile file = projFiles.getSourceFileForClassName(aClassName);
        if (file != null)
            return file;

        // Check child projects
        List<Project> projects = getProjects();
        for (Project proj : projects) {
            projFiles = proj.getProjectFiles();
            file = projFiles.getSourceFileForClassName(aClassName);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Watches Project.BuildFile for dependency change to reset ClassPathInfo.
     */
    private void handleBuildFileChange(PropChange propChange)
    {
        // Save build file
        BuildFile buildFile = getBuildFile();
        buildFile.writeFile();

        // Clear classloader
        clearClassLoader();

        // If Dependency changed, update resolver for changed class paths
        if (propChange.getPropName() == BuildFile.Dependency_Prop)
            getResolver().handleProjectDependenciesChanged();
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

        // If source file, close agent
        JavaAgent javaAgent = JavaAgent.getAgentForFile(aFile);
        if (javaAgent != null) {
            _javaAgents.remove(javaAgent);
            javaAgent.closeAgent();
        }
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        // If plain file, add as BuildFile
        if (aFile.isFile())
            _projBuilder.addBuildFile(aFile, false);
    }

    /**
     * Adds a java agent.
     */
    protected void addJavaAgent(JavaAgent javaAgent)
    {
        _javaAgents.add(javaAgent);
    }

    /**
     * Closes a project.
     */
    public void closeProject()
    {
        // If already close, just return
        if (_site == null) { System.err.println("Project.closeProject: Multiple closes"); return; }

        // Close JavaAgents
        for (JavaAgent javaAgent : _javaAgents)
            javaAgent.closeAgent();
        _javaAgents.clear();

        // Clear Site.Project
        _site.setProp(Project.class.getSimpleName(), null);

        // Close project site
        try { _site.flush(); }
        catch (Exception e) { throw new RuntimeException(e); }
        _site.resetFiles();
        _site = null;
    }

    /**
     * Deletes the project.
     */
    public void deleteProject(TaskMonitor taskMonitor) throws Exception
    {
        // Start TaskMonitor
        taskMonitor.startTasks(1);
        taskMonitor.beginTask("Deleting files", -1);

        // Clear ClassLoader
        clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandboxSite();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        taskMonitor.endTask();
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

    /**
     * Needs unique name so that when debugging SnapCode, we can ignore classes loaded by Project.
     */
    public static class SnapCodeDebugClassLoader extends URLClassLoader {

        public SnapCodeDebugClassLoader(URL[] urls, ClassLoader aPar)
        {
            super(urls, aPar);
        }
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropObject;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.app.SnapCodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class manages working with a set of one or more projects.
 */
public class Workspace extends PropObject {

    // The projects in the workspace
    private List<Project> _projects = new ArrayList<>(1);

    // The project sites
    private List<WebSite> _sites;

    // The status of the Workspace
    private String  _status;

    // The activity of the Workspace
    private String  _activity;

    // Whether Workspace is building
    private boolean  _building;

    // Whether Workspace is loading
    private boolean  _loading;

    // A helper class to do workspace builds
    private WorkspaceBuilder  _builder;

    // The list of Breakpoints
    private Breakpoints  _breakpoints;

    // A list of build issues
    private BuildIssues  _buildIssues;

    // The TaskManager
    protected TaskManager _taskManager;

    // Constants for properties
    public static final String Status_Prop = "Status";
    public static final String Activity_Prop = "Activity";
    public static final String Building_Prop = "Building";
    public static final String Loading_Prop = "Loading";
    public static final String Projects_Prop = "Projects";

    /**
     * Constructor.
     */
    public Workspace()
    {
        super();

        _builder = new WorkspaceBuilder(this);
        _taskManager = new TaskManager();
    }

    /**
     * Close workspace.
     */
    public void closeWorkspace()
    {
        // Remove all projects
        List<Project> projects = getProjects();
        for (Project project : projects)
            closeProject(project);
    }

    /**
     * Returns the projects that this workspace manages.
     */
    public List<Project> getProjects()  { return _projects; }

    /**
     * Opens given project in this workspace.
     */
    public void openProject(Project aProj)
    {
        // If already present, just return
        if (_projects.contains(aProj)) return;

        // Add project
        _projects = new ArrayList<>(_projects);
        _projects.add(aProj);
        _sites = null;

        // Fire prop change
        int index = _projects.indexOf(aProj);
        firePropChange(Projects_Prop, null, aProj, index);

        // Add dependent projects
        List<Project> childProjects = aProj.getProjects();
        childProjects.forEach(this::openProject);
    }

    /**
     * Opens a project for given site.
     */
    public Project openProjectForSite(WebSite aSite)
    {
        Project proj = Project.getProjectForSite(aSite);
        if (proj == null)
            proj = new Project(this, aSite);
        openProject(proj);
        return proj;
    }

    /**
     * Opens a project for given project path.
     */
    public Project openProjectForName(String projectName)
    {
        WebSite projSite = SnapCodeUtils.getSnapCodeProjectSiteForName(projectName);
        return openProjectForSite(projSite);
    }

    /**
     * Removes a project.
     */
    public void closeProject(Project aProj)
    {
        int index = _projects.indexOf(aProj);
        if (index < 0)
            return;

        // Remove project
        _projects = new ArrayList<>(_projects);
        _projects.remove(index);
        _sites = null;

        // Fire prop change
        firePropChange(Projects_Prop, aProj, null, index);
    }

    /**
     * Returns the project for given name.
     */
    public Project getProjectForName(String aName)
    {
        return ListUtils.findMatch(_projects, proj -> proj.getName().equals(aName));
    }

    /**
     * Returns the root project.
     */
    public Project getRootProject()  { return !_projects.isEmpty() ? _projects.get(0) : null; }

    /**
     * Returns the sites.
     */
    public List<WebSite> getSites()
    {
        if (_sites != null) return _sites;
        return _sites = ListUtils.map(_projects, proj -> proj.getSite());
    }

    /**
     * Returns the status text.
     */
    public String getStatus()  { return _status; }

    /**
     * Sets the status text.
     */
    public void setStatus(String aString)
    {
        if (Objects.equals(aString, _status)) return;
        firePropChange(Status_Prop, _status, _status = aString);
    }

    /**
     * Returns the activity text.
     */
    public String getActivity()  { return _activity; }

    /**
     * Sets the activity text.
     */
    public void setActivity(String aString)
    {
        if (Objects.equals(aString, _activity)) return;
        firePropChange(Activity_Prop, _activity, _activity = aString);
    }

    /**
     * Returns whether workspace is currently building anything.
     */
    public boolean isBuilding()  { return _building; }

    /**
     * Sets whether workspace is currently building anything.
     */
    public void setBuilding(boolean aValue)
    {
        if (aValue == _building) return;
        firePropChange(Building_Prop, _building, _building = aValue);
    }

    /**
     * Returns whether workspace is currently loading anything.
     */
    public boolean isLoading()  { return _loading; }

    /**
     * Sets whether workspace is currently loading anything.
     */
    public void setLoading(boolean aValue)
    {
        if (aValue == _loading) return;
        firePropChange(Loading_Prop, _loading, _loading = aValue);
    }

    /**
     * Returns the builder.
     */
    public WorkspaceBuilder getBuilder()  { return _builder; }

    /**
     * Returns the task manager.
     */
    public TaskManager getTaskManager()  { return _taskManager; }

    /**
     * Returns the breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        if (_breakpoints != null) return _breakpoints;
        return _breakpoints = new Breakpoints(this);
    }

    /**
     * The breakpoint list property.
     */
    public BuildIssues getBuildIssues()
    {
        if (_buildIssues != null) return _buildIssues;
        return _buildIssues = new BuildIssues();
    }

    /**
     * Saves all unsaved files.
     */
    public void saveAllFiles()
    {
        List<Project> projects = getProjects();
        for (Project project : projects) {
            WebSite projSite = project.getSite();
            WebFile rootDir = projSite.getRootDir();
            saveAllFilesImpl(project, rootDir);
        }
    }

    /**
     * Saves all unsaved files for given file and its children.
     */
    private void saveAllFilesImpl(Project project, WebFile aFile)
    {
        // Handle directory
        if (aFile.isDir()) {

            // Skip build dir
            if (aFile == project.getBuildDir())
                return;

            // Iterate over child files and recurse
            List<WebFile> dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles)
                saveAllFilesImpl(project, file);
        }

        // Handle file
        else if (aFile.isModified()) {
            try { aFile.save(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    /**
     * Opens a project with given repo URL.
     */
    public boolean openProjectForRepoUrl(WebURL repoURL, TaskMonitor taskMonitor) throws Exception
    {
        // Hack to support github repos in CheerpJ
        //if (repoURL.getFileType().equals("git") && SnapUtils.isWebVM) repoURL = GitHubUtils.downloadGithubZipFile(repoURL);

        // Get project name
        String projName = repoURL.getFilenameSimple();

        // If project already present, just return
        Project existingProj = getProjectForName(projName);
        if (existingProj != null)
            return true;

        // Get project site
        WebURL snapCodeDirURL = SnapCodeUtils.getSnapCodeDirURL();
        WebURL projDirURL = snapCodeDirURL.getChild(projName);
        WebSite projSite = projDirURL.getAsSite();

        // If exists, just add and return
        if (projSite.getExists()) {
            WebFile projRootDir = projSite.getRootDir();
            if (ProjectUtils.isProjectDir(projRootDir)) {
                openProjectForSite(projSite);
                return true;
            }
        }

        // Get version control
        VersionControlUtils.setRemoteSiteUrl(projSite, repoURL);
        VersionControl versionControl = VersionControl.getVersionControlForProjectSite(projSite);

        // Checkout project
        boolean checkoutResult = versionControl.checkout(taskMonitor);
        if (!checkoutResult)
            throw new RuntimeException("Checkout failed");

        // Open project
        openProjectForSite(projSite);

        // Return
        return checkoutResult;
    }
}

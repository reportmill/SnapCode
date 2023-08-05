/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;

import javakit.resolver.Resolver;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.web.WebSite;
import snapcode.app.WorkspaceTools;

/**
 * This class manages working with a set of one or more projects.
 */
public class Workspace extends PropObject {

    // The projects in the workspace
    private Project[] _projects = new Project[0];

    // The project sites
    private WebSite[] _sites;

    // The status of the Workspace
    private String _status;

    // The activity of the Workspace
    private String _activity;

    // Whether Workspace is building
    private boolean _building;

    // Whether Workspace is loading
    private boolean _loading;

    // A helper class to do workspace builds
    private WorkspaceBuilder _builder;

    // The list of Breakpoints
    private Breakpoints _breakpoints;

    // A list of build issues
    private BuildIssues _buildIssues;

    // The ClassLoader for compiled class info
    protected ClassLoader _classLoader;

    // The resolver
    protected Resolver _resolver;

    // Constants for properties
    public static final String Status_Prop = "Status";
    public static final String Activity_Prop = "Activity";
    public static final String Building_Prop = "Building";
    public static final String Loading_Prop = "Loading";
    public static final String Projects_Prop = "Projects";
    private WorkspaceTools workspaceTools;

    /**
     * Constructor.
     */
    public Workspace() {
        super();

        _builder = new WorkspaceBuilder(this);
    }

    /**
     * Returns the projects that this workspace manages.
     */
    public Project[] getProjects() {
        return _projects;
    }

    /**
     * Adds a project.
     */
    public void addProject(Project aProj) {
        // If already present, just return
        if (ArrayUtils.containsId(_projects, aProj)) return;

        // Add project
        _projects = ArrayUtils.addId(_projects, aProj);
        _sites = null;

        // Fire prop change
        int index = ArrayUtils.indexOfId(_projects, aProj);
        firePropChange(Projects_Prop, null, aProj, index);

        // Add dependent projects
        Project[] childProjects = aProj.getProjects();
        for (Project proj : childProjects)
            addProject(proj);
    }

    /**
     * Removes a project.
     */
    public void removeProject(Project aProj) {
        int index = ArrayUtils.indexOfId(_projects, aProj);
        if (index < 0)
            return;

        // Remove project
        _projects = ArrayUtils.remove(_projects, index);
        _sites = null;

        // Fire prop change
        firePropChange(Projects_Prop, aProj, null, index);
    }

    /**
     * Returns the root project.
     */
    public Project getRootProject() {
        return _projects[0];
    }

    /**
     * Returns the sites.
     */
    public WebSite[] getSites() {
        if (_sites != null) return _sites;
        return _sites = ArrayUtils.map(_projects, proj -> proj.getSite(), WebSite.class);
    }

    /**
     * Returns the status text.
     */
    public String getStatus() {
        return _status;
    }

    /**
     * Sets the status text.
     */
    public void setStatus(String aString) {
        if (aString.equals(_status)) return;
        firePropChange(Status_Prop, _status, _status = aString);
    }

    /**
     * Returns the activity text.
     */
    public String getActivity() {
        return _activity;
    }

    /**
     * Sets the activity text.
     */
    public void setActivity(String aString) {
        if (aString.equals(_activity)) return;
        firePropChange(Activity_Prop, _activity, _activity = aString);
    }

    /**
     * Returns whether workspace is currently building anything.
     */
    public boolean isBuilding() {
        return _building;
    }

    /**
     * Sets whether workspace is currently building anything.
     */
    public void setBuilding(boolean aValue) {
        if (aValue == _building) return;
        firePropChange(Building_Prop, _building, _building = aValue);
    }

    /**
     * Returns whether workspace is currently loading anything.
     */
    public boolean isLoading() {
        return _loading;
    }

    /**
     * Sets whether workspace is currently loading anything.
     */
    public void setLoading(boolean aValue) {
        if (aValue == _loading) return;
        firePropChange(Loading_Prop, _loading, _loading = aValue);
    }

    /**
     * Returns the builder.
     */
    public WorkspaceBuilder getBuilder() {
        return _builder;
    }

    /**
     * Returns the breakpoints.
     */
    public Breakpoints getBreakpoints() {
        if (_breakpoints != null) return _breakpoints;
        return _breakpoints = new Breakpoints(this);
    }

    /**
     * The breakpoint list property.
     */
    public BuildIssues getBuildIssues() {
        if (_buildIssues != null) return _buildIssues;
        return _buildIssues = new BuildIssues();
    }

    /**
     * Returns the ClassLoader.
     */
    public ClassLoader getClassLoader() {
        // If already set, just return
        if (_classLoader != null) return _classLoader;

        // Create, set, return ClassLoader
        ClassLoader classLoader = createClassLoader();
        return _classLoader = classLoader;
    }

    public WorkspaceTools getWorkspaceTools() {
        return this.workspaceTools;
    }

    public void setWorkspaceTools(WorkspaceTools workspaceTools) {
        this.workspaceTools = workspaceTools;
    }

    /**
     * Creates the ClassLoader.
     */
    protected ClassLoader createClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Clears the class loader.
     */
    public void clearClassLoader() {
        _classLoader = null;
        _resolver = null;
    }

    /**
     * Returns the resolver.
     */
    public Resolver getResolver() {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Create Resolver
        ClassLoader classLoader = getClassLoader();
        Resolver resolver = new Resolver(classLoader);
        Project rootProj = getRootProject();
        String[] classPaths = rootProj.getRuntimeClassPaths();
        resolver.setClassPaths(classPaths);

        // Set, return
        return _resolver = resolver;
    }

    /**
     * Returns a project for given site.
     */
    public Project getProjectForSite(WebSite aSite) {
        Project proj = Project.getProjectForSite(aSite);
        if (proj == null)
            proj = createProjectForSite(aSite);
        return proj;
    }

    /**
     * Adds a project for given site.
     */
    public Project addProjectForSite(WebSite aSite) {
        Project proj = getProjectForSite(aSite);
        addProject(proj);
        return proj;
    }

    /**
     * Creates a project for given site.
     */
    protected Project createProjectForSite(WebSite aSite) {
        return new Project(this, aSite);
    }
}

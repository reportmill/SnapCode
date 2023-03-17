package snapcode.app;
import javakit.project.Project;
import javakit.project.Workspace;
import javakit.project.WorkspaceBuilder;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.TabView;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.apptools.BuildDirTool;
import snapcode.apptools.ProjectConfigTool;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectPane extends WebPage {

    // The WorkspacePane that owns this ProjectPane
    private WorkspacePane  _workspacePane;

    // The Project
    private Project  _project;

    // The ProjectConfigTool
    private ProjectConfigTool  _configTool;

    // The BuildDirTool
    private BuildDirTool  _buildDirTool;

    // The top level TabView
    private TabView  _tabView;

    // A PropChangeListener for Site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    protected ProjectPane(WorkspacePane workspacePane, Project aProject)
    {
        super();
        _workspacePane = workspacePane;
        _project = aProject;

        // Create/set tools
        _configTool = new ProjectConfigTool(this);
        _buildDirTool = new BuildDirTool(this);

        // Set this ProjectPane as Site prop
        WebSite projSite = aProject.getSite();
        projSite.addFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), this);
    }

    /**
     * Returns the WorkspacePane.
     */
    public WorkspacePane getWorkspacePane()  { return _workspacePane; }

    /**
     * Returns the Workspace.
     */
    public Workspace getWorkspace()  { return _workspacePane.getWorkspace(); }

    /**
     * Returns the project.
     */
    public Project getProject()  { return _project; }

    /**
     * Returns the site.
     */
    public WebSite getSite()  { return _project.getSite(); }

    /**
     * Returns the ProjectConfigTool for this site.
     */
    public ProjectConfigTool getConfigTool()  { return _configTool; }

    /**
     * Opens the Site.
     */
    public void openSite()
    {
        // Activate ProjectConfigTool
        if (_configTool != null)
            _configTool.openSite();
    }

    /**
     * Closes the site.
     */
    public void closeSite()
    {
        WebSite projSite = _project.getSite();
        projSite.removeFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), null);
        _workspacePane = null;
        _configTool = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        if (_configTool != null)
            _configTool.deleteProject(aView);

        else {
            try {
                WebSite projSite = _project.getSite();
                projSite.deleteSite();
            }
            catch (Exception e) {
                e.printStackTrace();
                DialogBox.showExceptionDialog(null, "Delete Site Failed", e);
            }
        }
    }

    /**
     * Returns whether given file is a hidden file.
     */
    public boolean isHiddenFile(WebFile aFile)
    {
        if (aFile == _configTool.getProject().getBuildDir())
            return true;
        return aFile.getPath().startsWith("/.git");
    }

    /**
     * Called when a site file changes.
     */
    private void siteFileChanged(PropChange aPC)
    {
        // Get source and property name
        WebFile file = (WebFile) aPC.getSource();
        String propName = aPC.getPropName();

        // Handle Saved property: Call fileAdded or fileSaved
        if (propName == WebFile.Exists_Prop) {
            if ((Boolean) aPC.getNewValue())
                fileAdded(file);
            else fileRemoved(file);
        }

        // Handle ModifedTime property: Call file saved
        if (propName == WebFile.ModTime_Prop && file.getExists())
            fileSaved(file);
    }

    /**
     * Called when file added to project.
     */
    private void fileAdded(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Add file to project and build workspace
        _project.fileAdded(aFile);
        buildWorkspace();
    }

    /**
     * Called when file removed from project.
     */
    private void fileRemoved(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Remove from project and build workspace
        _project.fileRemoved(aFile);
        buildWorkspace();
    }

    /**
     * Called when file saved in project.
     */
    private void fileSaved(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Notify saved and build workspace
        _project.fileSaved(aFile);
        buildWorkspace();
    }

    /**
     * Called to build workspace.
     */
    private void buildWorkspace()
    {
        Workspace workspace = _project.getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuild() && builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(false);
    }

    /**
     * Initialize UI panel.
     */
    protected View createUI()
    {
        // Create TabView
        _tabView = new TabView();

        // Add ProjectConfigTool
        ProjectConfigTool projPane = getConfigTool();
        if (projPane != null)
            _tabView.addTab("Settings", projPane.getUI()); //tab.setTooltip(new Tooltip("Project Settings"));

        // Add BuildPane
        BuildDirTool buildDirTool = _buildDirTool;
        _tabView.addTab("Build Dir", buildDirTool.getUI());

        // Return
        return _tabView;
    }

    /**
     * Returns the site pane for a site.
     */
    public static ProjectPane getProjectPaneForSite(WebSite aSite)
    {
        return (ProjectPane) aSite.getProp(ProjectPane.class.getName());
    }

    /**
     * A WebPage subclass for ProjectPane.
     */
    public static class ProjectPanePage extends WebPage {

        /**
         * Initialize UI panel.
         */
        protected View createUI()
        {
            ProjectPane projectPane = ProjectPane.getProjectPaneForSite(getSite());
            return projectPane.getUI();
        }

        /**
         * Override to provide better title.
         */
        public String getTitle()
        {
            return getURL().getSite().getName() + " - Project Settings";
        }
    }
}
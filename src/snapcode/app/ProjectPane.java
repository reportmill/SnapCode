package snapcode.app;
import javakit.project.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.TaskMonitorPanel;
import snapcode.apptools.ProjectAnalysisTool;
import snapcode.apptools.VersionControlTool;
import snapcode.project.VersionControl;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectPane extends ViewOwner {

    // The WorkspacePane that owns this ProjectPane
    private WorkspacePane _workspacePane;

    // The Project
    private Project _project;

    // The ProjectTools
    private ProjectTools _projectTools;

    // The VcsTool for main project
    private VersionControlTool _versionControlTool;

    // A PropChangeListener for Site file changes
    private PropChangeListener _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    protected ProjectPane(WorkspacePane workspacePane, Project aProject)
    {
        super();
        _workspacePane = workspacePane;
        _project = aProject;

        // Create ProjectTools
        _projectTools = new ProjectTools(this);

        // Set this ProjectPane as Site prop
        WebSite projSite = aProject.getSite();
        projSite.addFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), this);

        // Set VersionControlPane
        String urls = getRemoteURLString();
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _versionControlTool = new VersionControlTool(this);
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
     * Returns the project tools.
     */
    public ProjectTools getProjectTools()  { return _projectTools; }

    /**
     * Returns the project site.
     */
    public WebSite getProjectSite()  { return _project.getSite(); }

    /**
     * Returns the VersionControlTool.
     */
    public VersionControlTool getVersionControlTool()  { return _versionControlTool; }

    /**
     * Returns the RemoteURL string.
     */
    public String getRemoteURLString()
    {
        WebSite projectSite = getProjectSite();
        return VersionControl.getRemoteURLString(projectSite);
    }

    /**
     * Sets the RemoteURL string.
     */
    public void setRemoteURLString(String urls)
    {
        // Deactivate Version control pane and re-open site
        _versionControlTool.deactivate();
        WebSite projectSite = getProjectSite();
        VersionControl.setRemoteURLString(projectSite, urls);

        // Recreate VC and set in tab
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _versionControlTool = new VersionControlTool(this);

        // Reopen site
        _versionControlTool.projectDidOpen();
    }

    /**
     * Called when project is opened.
     */
    public void workspaceDidOpen()
    {
        // Do AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(true);

        // Activate VersionControlPane
        if (_versionControlTool != null)
            _versionControlTool.projectDidOpen();
    }

    /**
     * Called when project is closed.
     */
    public void workspaceDidClose()
    {
        WebSite projSite = _project.getSite();
        projSite.removeFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), null);
        _workspacePane = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        // Disable AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        builder.setAutoBuild(false);

        // Delete project
        Project project = getProject();
        TaskMonitorPanel taskMonitorPanel = new TaskMonitorPanel(aView, "Delete Project");
        try { project.deleteProject(taskMonitorPanel); }
        catch (Exception e) { DialogBox.showExceptionDialog(aView, "Delete Project Failed", e); }
    }

    /**
     * Returns whether given file is a hidden file.
     */
    public boolean isHiddenFile(WebFile aFile)
    {
        if (aFile == getProject().getBuildDir())
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
        // Forward to VersionControl
        if (_versionControlTool != null)
            _versionControlTool.fileAdded(aFile);

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
        // Forward to VersionControl
        if (_versionControlTool != null)
            _versionControlTool.fileRemoved(aFile);

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
        // Forward to VersionControl
        if (_versionControlTool != null)
            _versionControlTool.fileSaved(aFile);

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
     * Returns the site pane for a site.
     */
    public static ProjectPane getProjectPaneForSite(WebSite aSite)
    {
        return (ProjectPane) aSite.getProp(ProjectPane.class.getName());
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        Project project = getProject();
        setViewText("ProjectNameLabel", "Project: " + project.getName());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update AutoBuildCheckBox
        Workspace workspace = getWorkspace();
        setViewValue("AutoBuildCheckBox", workspace.getBuilder().isAutoBuild());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle AutoBuildCheckBox
        if (anEvent.equals("AutoBuildCheckBox")) {
            Workspace workspace = getWorkspace();
            workspace.getBuilder().setAutoBuild(anEvent.getBoolValue());
        }

        // Handle LOCTitleView (Lines of Code) click
        if (anEvent.equals("LOCTitleView"))
            showLinesOfCode();

        // Handle SymbolCheckTitleView (click)
        if (anEvent.equals("SymbolCheckTitleView"))
            showSymbolCheck();
    }

    /**
     * Shows the lines of code in project and child projects.
     */
    private void showLinesOfCode()
    {
        TitleView titleView = getView("LOCTitleView", TitleView.class);
        if (titleView.isExpanded())
            return;
        TextView linesOfCodeText = getView("LOCText", TextView.class);
        String linesOfCodeTextString = ProjectAnalysisTool.getLinesOfCodeText(getProject());
        linesOfCodeText.setText(linesOfCodeTextString);
    }

    /**
     * Shows a list of symbols that are undefined in project source files.
     */
    private void showSymbolCheck()
    {
        TitleView titleView = getView("SymbolCheckTitleView", TitleView.class);
        if (titleView.isExpanded())
            return;

        // Get TextArea
        TextView symbolCheckTextView = getView("SymbolCheckText", TextView.class);
        TextArea symbolCheckTextArea = symbolCheckTextView.getTextArea();
        if (symbolCheckTextArea.length() > 0)
            return;

        // Initialize
        symbolCheckTextArea.addChars("Undefined Symbols:\n");
        symbolCheckTextArea.setSel(0, 0);

        // Find callbacks
        WebFile sourceDir = getProject().getSourceDir();
        Runnable run = () -> new ProjectAnalysisTool().findUndefines(sourceDir, symbolCheckTextArea);
        new Thread(run).start();
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
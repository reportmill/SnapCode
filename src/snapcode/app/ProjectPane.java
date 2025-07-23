package snapcode.app;
import snap.util.TaskMonitor;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.util.TaskMonitorPanel;
import snapcode.apptools.ProjectAnalysisTool;
import snapcode.apptools.VersionControlTool;
import snapcode.project.*;
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
        projSite.setProp(ProjectPane.class.getName(), this);

        // Create VersionControlTool
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
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        // Disable AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        builder.setAutoBuildEnabled(false);

        // Delete project
        Project project = getProject();
        TaskMonitor taskMonitor = new TaskMonitorPanel(aView, "Delete Project");
        try { project.deleteProject(taskMonitor); }
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
     * Called when project removed from workspace pane.
     */
    protected void handleProjectRemovedFromWorkspacePane()
    {
        // If already close, just return
        if (_workspacePane == null) return;

        // Unregister Project.Site.ProjectPane
        WebSite projSite = _project.getSite();
        projSite.setProp(ProjectPane.class.getName(), null);

        // Close project and clear workspace pane
        _project.closeProject();
        _workspacePane = null;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Add VersionControl UI
        ColView tabView = getView("ColView", ColView.class);
        tabView.addChild(_versionControlTool.getUI());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()  { }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
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

        // Get TextView
        TextView symbolCheckTextView = getView("SymbolCheckText", TextView.class);
        symbolCheckTextView.clear();

        // Initialize
        symbolCheckTextView.addChars("Undefined Symbols:\n");
        symbolCheckTextView.setSel(0, 0);

        // Find callbacks
        WebFile sourceDir = getProject().getSourceDir();
        Runnable run = () -> new ProjectAnalysisTool().findUndefines(sourceDir, symbolCheckTextView);
        new Thread(run).start();
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
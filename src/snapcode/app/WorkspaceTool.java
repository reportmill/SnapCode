package snapcode.app;
import snap.view.ViewEvent;
import snap.view.ViewUtils;
import snapcode.apptools.FilesTool;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snap.view.ViewOwner;
import snapcode.webbrowser.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Collections;
import java.util.List;

/**
 * This is the base class for workspace tools.
 */
public class WorkspaceTool extends ViewOwner {

    // The WorkspacePane
    protected WorkspacePane  _workspacePane;

    // The Workspace
    protected Workspace  _workspace;

    // The WorkspaceTools
    protected WorkspaceTools  _workspaceTools;

    // The PagePane
    protected PagePane  _pagePane;

    // Whether this tool was shown automatically
    private boolean _wasShownAutomatically;

    /**
     * Constructor.
     */
    public WorkspaceTool(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
        _workspace = workspacePane.getWorkspace();
        _workspaceTools = workspacePane.getWorkspaceTools();
        _pagePane = workspacePane.getPagePane();
    }

    /**
     * Returns the WorkspacePane.
     */
    public WorkspacePane getWorkspacePane()  { return _workspacePane; }

    /**
     * Returns the workspace.
     */
    public Workspace getWorkspace()  { return _workspace; }

    /**
     * Returns the files tool.
     */
    public FilesTool getFilesTool()  { return _workspaceTools.getFilesTool(); }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()  { return _workspacePane.getRootSite(); }

    /**
     * Returns the selected project.
     */
    public Project getProject()  { return _workspacePane.getRootProject(); }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _workspacePane.getSelFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _workspacePane.setSelFile(aFile);
    }

    /**
     * Returns the list of selected files.
     */
    public List<WebFile> getSelFiles()
    {
        WebFile selFile = getSelFile();
        return selFile == null ? Collections.EMPTY_LIST : Collections.singletonList(selFile);
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelSite()  { return _workspacePane.getSelSite(); }

    /**
     * Returns the selected dir.
     */
    public WebFile getSelDirOrFirst()
    {
        WebSite selSite = getSelSiteOrFirst();
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.getSite() != selSite)
            selFile = selSite.getRootDir();
        return selFile.isDir() ? selFile : selFile.getParent();
    }

    /**
     * Returns the selected site or first site.
     */
    public WebSite getSelSiteOrFirst()
    {
        WebSite selSite = getSelSite();
        if (selSite == null)
            selSite = getRootSite();
        return selSite;
    }

    /**
     * Returns the selected project.
     */
    public Project getSelProject()
    {
        WebSite selSite = getSelSite();
        return Project.getProjectForSite(selSite);
    }

    /**
     * Returns the ProjectPane for selected file.
     */
    public ProjectPane getSelProjectPane()
    {
        Project selProject = getSelProject();
        return _workspacePane.getProjectPaneForProject(selProject);
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _workspacePane.getBrowser(); }

    /**
     * Shows this tool.
     */
    public void showTool()
    {
        if (isShowing()) return;
        _workspaceTools.showTool(this);
    }

    /**
     * Hides this tool.
     */
    public void hideTool()
    {
        if (!isShowing()) return;
        ToolTray toolTray = _workspaceTools.getToolTrayForTool(this);
        toolTray.setSelTool(null);
        _wasShownAutomatically = false;
        _hideToolAutomaticallyInstance = 0;
    }

    /**
     * Shows this tool automatically (setting a flag to indicate it should close automatically).
     */
    public void showToolAutomatically()
    {
        _hideToolAutomaticallyInstance = 0;
        if (isShowing()) return;
        _workspaceTools.showTool(this);
        _wasShownAutomatically = true;
    }

    /**
     * Closes this tool automatically (if previously shown automatically).
     */
    public void hideToolAutomatically()
    {
        if (!_wasShownAutomatically) return;
        hideTool();
    }

    /**
     * Closes this tool automatically (if previously shown automatically) after given delay
     */
    public void hideToolAutomaticallyAfterDelay(int millisecondDelay)
    {
        if (!_wasShownAutomatically) return;
        double instanceValue = _hideToolAutomaticallyInstance = Math.random();
        ViewUtils.runDelayed(() -> hideToolAutomaticallyAfterDelayImpl(instanceValue), millisecondDelay);
    }

    /**
     * Called to handle hideToolAutomaticallyAfterDelay() if no other showToolAutomatically has been called.
     */
    private void hideToolAutomaticallyAfterDelayImpl(double instanceValue)
    {
        if (instanceValue == _hideToolAutomaticallyInstance)
            hideToolAutomatically();
    }
    private double _hideToolAutomaticallyInstance;

    /**
     * Override to clear WasShownAutomatically.
     */
    @Override
    protected void setShowing(boolean aValue)
    {
        if (aValue == isShowing()) return;
        super.setShowing(aValue);
        _wasShownAutomatically = false;
    }

    /**
     * Returns the title.
     */
    public String getTitle()  { return "Unknown"; }

    /**
     * Called when the workspace wants to close. Should break into workspaceRequestsClose() and workspaceIsClosing().
     */
    protected boolean workspaceIsClosing()  { return  true; }

    /**
     * Called when a project is closing.
     */
    protected void projectIsClosing(Project aProject)  { }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        if (anEvent.equals("HideButton"))
            hideTool();
    }
}

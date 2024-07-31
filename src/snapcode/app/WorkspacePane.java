package snapcode.app;
import snap.geom.Rect;
import snap.gfx.GFXEnv;
import snap.view.SharedAction;
import snap.viewx.DevPane;
import snapcode.project.*;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.view.*;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.apptools.*;

/**
 * This class is the top level controller for an open project.
 */
public class WorkspacePane extends ViewOwner {

    // The Workspace
    private Workspace _workspace;

    // The array of ProjectPanes
    protected ProjectPane[] _projectPanes = new ProjectPane[0];

    // The MainToolBar
    protected MainToolBar _toolBar;

    // The PagePane to display project files for editing
    protected PagePane _pagePane;

    // The StatusBar
    protected StatusBar _statusBar;

    // The WorkspaceTools
    protected WorkspaceTools _workspaceTools;

    // A Drag-and-drop helper class
    private WorkspacePaneDnD _workspacePaneDnD;

    /**
     * Constructor.
     */
    public WorkspacePane()
    {
        this(new Workspace());
    }

    /**
     * Constructor.
     */
    public WorkspacePane(Workspace workspace)
    {
        super();

        // Create workspace
        _workspace = workspace;
        _workspace.addPropChangeListener(pc -> workspaceDidPropChange(pc));

        // Create PagePane
        _pagePane = createPagePane();

        // Create WorkspaceTools
        _workspaceTools = createWorkspaceTools();

        // Create MainToolBar, StatusBar
        _toolBar = new MainToolBar(this);
        _statusBar = new StatusBar(this);

        // Manage projects
        Project[] projects = _workspace.getProjects();
        for (Project proj : projects)
            handleWorkspaceProjectAdded(proj);
    }

    /**
     * Returns the workspace.
     */
    public Workspace getWorkspace()  { return _workspace; }

    /**
     * Sets the workspace.
     */
    public void setWorkspace(Workspace aWorkspace)
    {
        if (aWorkspace == _workspace) return;

        // Close old workspace
        if (_workspace != null)
            _workspace.closeWorkspace();

        // Set
        _workspace = aWorkspace;

        // Install
        Project[] projects = _workspace.getProjects();
        for (Project proj : projects)
            handleWorkspaceProjectAdded(proj);

        // Install in tools
        _workspaceTools.setWorkspace(_workspace);
    }

    /**
     * Returns the PagePane.
     */
    public PagePane getPagePane()  { return _pagePane; }

    /**
     * Returns the PagePane.Browser.
     */
    public WebBrowser getBrowser()  { return _pagePane.getBrowser(); }

    /**
     * Returns the WorkspaceTools helper.
     */
    public WorkspaceTools getWorkspaceTools()  { return _workspaceTools; }

    /**
     * Returns the WorkspaceTools helper.
     */
    public WorkspacePaneDnD getWorkspacePaneDnD()  { return _workspacePaneDnD; }

    /**
     * Creates the PagePane.
     */
    protected PagePane createPagePane()  { return new PagePane(this); }

    /**
     * Creates the WorkspaceTools.
     */
    protected WorkspaceTools createWorkspaceTools()
    {
        return new WorkspaceTools(this);
    }

    /**
     * Returns the toolbar.
     */
    public MainToolBar getToolBar()  { return _toolBar; }

    /**
     * Returns the projects.
     */
    public Project[] getProjects()  { return _workspace.getProjects(); }

    /**
     * Returns the project panes.
     */
    public ProjectPane[] getProjectPanes()  { return _projectPanes; }

    /**
     * Returns the ProjectPanes.
     */
    public ProjectPane getProjectPaneForProject(Project aProject)
    {
        return ArrayUtils.findMatch(_projectPanes, prjPane -> prjPane.getProject() == aProject);
    }

    /**
     * Returns the array of sites.
     */
    public WebSite[] getSites()  { return _workspace.getSites(); }

    /**
     * Returns the selected project.
     */
    public Project getRootProject()  { return _workspace.getRootProject(); }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()
    {
        Project rootProj = _workspace.getRootProject();
        return rootProj != null ? rootProj.getSite() : null;
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _pagePane.getSelFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _pagePane.setSelFile(aFile);
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelSite()
    {
        // Get site for selected file
        WebFile selFile = getSelFile();
        WebSite selSite = selFile != null ? selFile.getSite() : null;

        // If file not in Workspace.Sites, use first site
        WebSite[] workspaceSites = getSites();
        if (!ArrayUtils.containsId(workspaceSites, selSite))
            selSite = workspaceSites.length > 0 ? workspaceSites[0] : null;

        // Return
        return selSite;
    }

    /**
     * Shows the WorkspacePane window.
     */
    public void show()
    {
        setWindowVisible(true);
    }

    /**
     * Close this WorkspacePane.
     */
    public void hide()
    {
        // Close workspace
        _workspace.closeWorkspace();

        // Close tools
        _workspaceTools.workspaceIsClosing();
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        Project proj = getRootProject();
        return proj != null ? proj.getBuildDir() : null;
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get MainSplit
        SplitView mainSplit = getUI(SplitView.class);
        mainSplit.setBorder(null);
        mainSplit.setDividerSpan(1);

        // Get MenuBar and register to process events
        MenuBar menuBar = getView("MenuBar", MenuBar.class);
        menuBar.addEventHandler(e -> { if (e.isShortcutDown()) ViewUtils.processEvent(menuBar, e); }, KeyPress);

        // Add toobar to menubar
        View toolBarUI = _toolBar.getUI();
        toolBarUI.setGrowWidth(true);
        ViewUtils.addChild(menuBar, toolBarUI);

        // Get PagePaneSplitView
        SplitView pagePaneSplitView = getView("PagePaneSplitView", SplitView.class);
        pagePaneSplitView.setBorder(null);
        pagePaneSplitView.setDividerSpan(1);

        // Install PagePage UI
        View pagePaneUI = _pagePane.getUI();
        pagePaneSplitView.addItem(pagePaneUI);

        // Add LeftTray
        ToolTray leftTray = _workspaceTools.getLeftTray();
        View leftTrayUI = leftTray.getUI();
        pagePaneSplitView.addItem(leftTrayUI, 0);

        // Add RightTray
        ToolTray rightTray = _workspaceTools.getRightTray();
        View rightTrayUI = rightTray.getUI();
        pagePaneSplitView.addItem(rightTrayUI);

        // Add SupportTray to MainSplit
        ToolTray bottomTray = _workspaceTools.getBottomTray();
        TabView bottomTrayUI = (TabView) bottomTray.getUI();
        mainSplit.addItem(bottomTrayUI);

        // Configure CutMenuItem, CopyMenuItem, PasteMenuItem, SelectAllMenuItem, UndoMenuItem, RedoMenuItem
        getView("CutMenuItem", MenuItem.class).setSharedAction(SharedAction.Cut_Action);
        getView("CopyMenuItem", MenuItem.class).setSharedAction(SharedAction.Copy_Action);
        getView("PasteMenuItem", MenuItem.class).setSharedAction(SharedAction.Paste_Action);
        getView("SelectAllMenuItem", MenuItem.class).setSharedAction(SharedAction.SelectAll_Action);
        getView("UndoMenuItem", MenuItem.class).setSharedAction(SharedAction.Undo_Action);
        getView("RedoMenuItem", MenuItem.class).setSharedAction(SharedAction.Redo_Action);

        // Configure Left/Right/BottomTrayMenuItem.Text to update with Left/Right/BottomTrayUI.Visible
        View showLeftTrayMenuItem = getView("ShowLeftTrayMenuItem");
        String showLeftTrayKey = "Visible ? \"Hide Left Tray\" : \"Show Left Tray\"";
        ViewUtils.bindExpr(leftTrayUI, View.Visible_Prop, showLeftTrayMenuItem, View.Text_Prop, showLeftTrayKey);
        View showRightTrayMenuItem = getView("ShowRightTrayMenuItem");
        String showRightTrayKey = "Visible ? \"Hide Right Tray\" : \"Show Right Tray\"";
        ViewUtils.bindExpr(rightTrayUI, View.Visible_Prop, showRightTrayMenuItem, View.Text_Prop, showRightTrayKey);
        View showBottomTrayMenuItem = getView("ShowBottomTrayMenuItem");
        String showBottomTrayKey = "Visible ? \"Hide Bottom Tray\" : \"Show Bottom Tray\"";
        ViewUtils.bindExpr(bottomTrayUI, View.Visible_Prop, showBottomTrayMenuItem, View.Text_Prop, showBottomTrayKey);

        // Add StatusBar
        TabBar tabBar = bottomTrayUI.getTabBar();
        _statusBar.addToView(tabBar);

        // Add drag drop support
        _workspacePaneDnD = new WorkspacePaneDnD(this);

        // Hide Browser menu items
        if (!SnapUtils.isWebVM) {
            getView("DownloadFileMenuItem").setVisible(false);
            getView("OpenDesktopFileMenuItem").setVisible(false);
            getView("UploadDownloadMenuItemsSeparator").setVisible(false);
        }

        // Add key binding to OpenMenuItem and CloseWindow
        //addKeyActionHandler("OpenMenuItem", "meta O");
        //addKeyActionHandler("CloseFileAction", "meta W");
    }

    /**
     * Called when WorkspacePane is first shown.
     */
    @Override
    protected void initShowing()
    {
        // Show project tool
        runLater(_workspaceTools.getProjectFilesTool()::showTool);

        // Do AutoBuild
        if (getProjects().length > 0)
            buildWorkspaceAllLater();

        // Hack when running in browser in Swing to always fill available screen size
        if (SnapUtils.isWebVM && getEnv().getClass().getSimpleName().startsWith("Swing"))
            new ViewTimer(this::checkScreenSize, 1000).start();
    }

    /**
     * Initialize window.
     */
    @Override
    protected void initWindow(WindowView aWindow)
    {
        // Configure save name and size
        aWindow.setSaveName("AppPane");
        aWindow.setSaveSize(true);

        // If browser, maximize window
        if (SnapUtils.isWebVM)
            aWindow.setMaximized(true);

        // Register for handleWinClose on window close
        aWindow.addEventHandler(this::handleWinClose, WinClose);
    }

    /**
     * Resets UI panel.
     */
    @Override
    public void resetUI()
    {
        if (getProjects().length == 0) return;

        // Reset window title
        WebPage page = _pagePane.getSelPage();
        getWindow().setTitle(page != null ? page.getTitle() : "SnapCode");

        // Reset FilesPane and SupportTray
        _workspaceTools.resetLater();
        _toolBar.resetLater();
        _statusBar.resetLater();
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle NewFileMenuItem, OpenFileMenuItem
            case "NewFileMenuItem": _workspaceTools.getNewFileTool().showNewFilePanel(); anEvent.consume(); break;
            case "OpenFileMenuItem": _workspaceTools.getFilesTool().showOpenFilePanel(); anEvent.consume(); break;

            // Handle SaveFileMenuItem
            case "SaveFileMenuItem": _workspaceTools.getFilesTool().saveSelFile(); anEvent.consume(); break;

            // Handle RevertFileMenuItem
            case "RevertFileMenuItem": _workspaceTools.getFilesTool().revertSelFiles(); anEvent.consume(); break;

            // Handle CloseFileMenuItem, CloseFileAction
            case "CloseFileMenuItem": case "CloseFileAction":
                _workspaceTools.getFilesTool().closeSelFile(); anEvent.consume(); break;

            // Handle DownloadFileMenuItem, OpenDesktopFileMenuItem
            case "DownloadFileMenuItem": _workspaceTools.getFilesTool().downloadFile(); anEvent.consume(); break;
            case "OpenDesktopFileMenuItem": _workspaceTools.getFilesTool().showOpenDesktopFilePanel(); anEvent.consume(); break;

            // Handle QuitMenuItem
            case "QuitMenuItem": App.getShared().quitApp(); anEvent.consume(); break;

            // Handle ShowLeftTrayMenuItem, ShowRightTrayMenuItem, ShowBottomTrayMenuItem
            case "ShowLeftTrayMenuItem": _workspaceTools.setShowLeftTray(!_workspaceTools.isShowLeftTray()); break;
            case "ShowRightTrayMenuItem": _workspaceTools.setShowRightTray(!_workspaceTools.isShowRightTray()); break;
            case "ShowBottomTrayMenuItem": _workspaceTools.setShowBottomTray(!_workspaceTools.isShowBottomTray()); break;

            // Handle CopyWebLinkMenuItem, OpenWebLinkMenuItem
            case "CopyWebLinkMenuItem": copyWebLink(); break;
            case "OpenWebLinkMenuItem": openWebLink(); break;

            // Handle ShowDevToolsMenuItem
            case "ShowDevToolsMenuItem": DevPane.setDevPaneShowing(getUI(), true); break;

            // Handle OpenSnapCodePageMenuItem, OpenDownloadPageMenuItem
            case "OpenSnapCodePageMenuItem": GFXEnv.getEnv().openURL("https://github.com/reportmill/SnapCode"); break;
            case "OpenDownloadPageMenuItem": GFXEnv.getEnv().openURL("https://jdeploy.com/~snapcodejava"); break;

            // Handle ShowJavaHomeMenuItem
            case "ShowJavaHomeMenuItem":
                String java = System.getProperty("java.home");
                FileUtils.openFile(java);
                break;
        }
    }

    /**
     * Called when window is closing.
     */
    private void handleWinClose(ViewEvent anEvent)
    {
        hide(); //WorkspacePaneUtils.openDefaultWorkspace();
    }

    /**
     * Called to build workspace.
     */
    private void buildWorkspaceAllLater()
    {
        // Do AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuildEnabled()) {
            builder.addAllFilesToBuild();
            builder.buildWorkspaceAfterDelay(800);
        }
    }

    /**
     * Called when Workspace does a property change.
     */
    private void workspaceDidPropChange(PropChange aPC)
    {
        switch (aPC.getPropName()) {

            // Handle Status, Activity
            case Workspace.Status_Prop: getBrowser().setStatus(_workspace.getStatus()); break;
            case Workspace.Activity_Prop: getBrowser().setActivity(_workspace.getActivity()); break;

            // Handle Building
            case Workspace.Building_Prop:
                if (!_workspace.isBuilding())
                    handleBuildCompleted();
                break;

            // Handle Projects
            case Workspace.Projects_Prop: handleWorkspaceProjectsChanged(aPC); break;
        }

        // Handle Loading, Building
        _statusBar.resetLater();
    }

    /**
     * Called when Workspace.Projects changes.
     */
    private void handleWorkspaceProjectsChanged(PropChange aPC)
    {
        Project addedProj = (Project) aPC.getNewValue();

        // Handle project added
        if (addedProj != null)
            handleWorkspaceProjectAdded(addedProj);

        // Handle project removed
        else {
            Project removedProj = (Project) aPC.getOldValue();
            if (removedProj != null)
                handleWorkspaceProjectRemoved(removedProj);
        }
    }

    /**
     * Called when Workspace adds a project.
     */
    private void handleWorkspaceProjectAdded(Project aProject)
    {
        // Create/add ProjectPane
        ProjectPane projPane = new ProjectPane(this, aProject);
        _projectPanes = ArrayUtils.addId(_projectPanes, projPane);

        // Open project pane
        projPane.openProjectPane();

        // Clear root files
        ProjectFilesTool projectFilesTool = _workspaceTools.getProjectFilesTool();
        projectFilesTool.resetRootFiles();

        // Reset UI
        resetLater();

        // If showing, build workspace
        if (isShowing())
            buildWorkspaceAllLater();
    }

    /**
     * Called when Workspace removes a project.
     */
    private void handleWorkspaceProjectRemoved(Project aProject)
    {
        // Notify tools
        _workspaceTools.projectIsClosing(aProject);

        // Remove ProjectPane
        ProjectPane projPane = getProjectPaneForProject(aProject);
        _projectPanes = ArrayUtils.remove(_projectPanes, projPane);

        // Close ProjectPane
        projPane.closeProjectPane();

        // Clear root files
        ProjectFilesTool projectFilesTool = _workspaceTools.getProjectFilesTool();
        projectFilesTool.resetRootFiles();

        // Reset UI
        resetLater();
    }

    /**
     * Called when site file changes with File PropChange.
     */
    protected void siteFileChanged(PropChange aPC)
    {
        // Get file/prop
        String propName = aPC.getPropName();
        WebFile file = (WebFile) aPC.getSource();

        // Handle LastModTime, Modified: Update file in ProjectFilesTool
        if (propName == WebFile.LastModTime_Prop || propName == WebFile.Modified_Prop) {
            ProjectFilesTool projectFilesTool = _workspaceTools.getProjectFilesTool();
            projectFilesTool.updateChangedFile(file);
        }
    }

    /**
     * Called when a build is completed.
     */
    private void handleBuildCompleted()
    {
        // If final error count non-zero, show build tool
        int errorCount = _workspace.getBuildIssues().getErrorCount();
        if (errorCount > 0)
            _workspaceTools.getBuildTool().showToolAutomatically();

        // If error count zero, hide build tool
        else if (errorCount == 0)
            _workspaceTools.getBuildTool().hideTool();

        // Make sure there is a final UI reset
        runLater(this::resetLater);
    }

    /**
     * Copies web link to clipboard.
     */
    private void copyWebLink()
    {
        String string = "https://reportmill.com/SnapCode/app/#" + _pagePane.getWindowLocationHash();
        Clipboard clipboard = Clipboard.get();
        clipboard.addData(string);
    }

    /**
     * Opens web link.
     */
    private void openWebLink()
    {
        String string = "https://reportmill.com/SnapCode/app/#" + _pagePane.getWindowLocationHash();
        GFXEnv.getEnv().openURL(string);
    }

    /**
     * Called every second to check screen size.
     */
    private void checkScreenSize()
    {
        Rect screenBounds = GFXEnv.getEnv().getScreenBoundsInset();
        WindowView window = getWindow();
        if (window.getWidth() != screenBounds.width || window.getHeight() != screenBounds.height) {
            window.setBounds(screenBounds);
            System.out.println("Changed window size");
        }
    }
}

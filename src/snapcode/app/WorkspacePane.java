package snapcode.app;
import snap.geom.Rect;
import snap.gfx.GFXEnv;
import snap.viewx.DevPane;
import snapcode.project.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.view.*;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
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

    // A PropChangeListener to watch for site file changes
    private PropChangeListener _siteFileLsnr = pc -> siteFileChanged(pc);

    // The default HomePage
    private HomePage  _homePage;

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

        // Create MainToolBar, StatusBar
        _toolBar = new MainToolBar(this);
        _statusBar = new StatusBar(this);

        // Create WorkspaceTools
        _workspaceTools = createWorkspaceTools();

        // Manage projects
        Project[] projects = _workspace.getProjects();
        for (Project proj : projects)
            workspaceDidAddProject(proj);
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

        // Uninstall
        Project[] projects = _workspace.getProjects();
        for (Project proj : projects)
            workspaceDidRemoveProject(proj);

        // Set
        _workspace = aWorkspace;

        // Install
        projects = _workspace.getProjects();
        for (Project proj : projects)
            workspaceDidAddProject(proj);

        // Install in tools
        _workspaceTools.setWorkspace(_workspace);
    }

    /**
     * Opens a source file.
     */
    public void openExternalSourceFile(WebFile sourceFile)
    {
        // Make sure workspace has temp project
        ProjectUtils.getTempProject(_workspace);

        // Create new source file for given external source file
        FilesTool filesTool = _workspaceTools.getFilesTool();
        WebFile newSourceFile = filesTool.newSourceFileForExternalSourceFile(sourceFile);
        if (newSourceFile == null) {
            System.out.println("WorkspacePane.openSourceFile: Couldn't open source file: " + sourceFile);
            return;
        }

        // If not "TempProj" file, add to RecentFiles
        WebURL sourceURL = sourceFile.getURL();
        if (!sourceURL.getString().contains("TempProj"))
            RecentFiles.addURL(sourceURL);

        // Hide LeftTray and show RunTool
        _workspaceTools.getLeftTray().setSelTool(null);
        _workspaceTools.getRightTray().setSelToolForClass(RunTool.class);

        // Show source file
        runLater(() -> {
            PagePane pagePane = getPagePane();
            pagePane.setSelFile(newSourceFile);
        });
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
        return rootProj.getSite();
    }

    /**
     * Returns the home page URL.
     */
    public WebURL getHomePageURL()  { return null; } // getHomePage().getURL();

    /**
     * Returns the HomePage.
     */
    private HomePage getHomePage()
    {
        if (_homePage != null) return _homePage;
        _homePage = new HomePage();
        _pagePane.setPageForURL(_homePage.getURL(), _homePage);
        return _homePage;
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
        // Show window
        getUI();
        getWindow().setSaveName("AppPane");
        getWindow().setSaveSize(true);
        getWindow().setVisible(true);

        // Open Projects
        ProjectPane[] projectPanes = getProjectPanes();
        for (ProjectPane projPane : projectPanes)
            projPane.workspaceDidOpen();

        // Do AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuildEnabled()) {
            builder.addAllFilesToBuild();
            builder.buildWorkspaceLater();
        }

        // Show HomePage
        WebURL homePageURL = getHomePageURL();
        if (homePageURL != null)
            _pagePane.setBrowserURL(homePageURL);
    }

    /**
     * Close this WorkspacePane.
     */
    public void hide()
    {
        ProjectPane[] projectPanes = getProjectPanes();

        // Flush and refresh sites
        for (ProjectPane projectPane : projectPanes) {

            // Close ProjectPane
            projectPane.workspaceDidClose();

            // Close project site
            WebSite projectSite = projectPane.getProjectSite();
            try { projectSite.flush(); }
            catch (Exception e) { throw new RuntimeException(e); }
            projectSite.resetFiles();
        }

        _workspaceTools.closeProject();
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
     * Returns whether ToolBar is showing.
     */
    public boolean isToolBarShowing()  { return _toolBar.isShowing(); }

    /**
     * Sets whether ToolBar is showing.
     */
    public void setToolBarShowing(boolean aValue)
    {
        if (aValue == isToolBarShowing()) return;

        // Show/hide tooBar.UI and adjacent separator rect
        _toolBar.getUI().setVisible(aValue);
        getView("MainColView", ColView.class).getChild(2).setVisible(aValue);
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

        // Install ToolBar
        ColView mainColView = getView("MainColView", ColView.class);
        mainColView.addChild(_toolBar.getUI(), 1);

        // Hide ToolBar and separator by default
        _toolBar.getUI().setVisible(false);
        mainColView.getChild(2).setVisible(false);

        // Get PagePaneSplitView
        SplitView pagePaneSplitView = getView("PagePaneSplitView", SplitView.class);
        pagePaneSplitView.setBorder(null);
        pagePaneSplitView.setDividerSpan(1);

        // Install PagePage UI
        View pagePaneUI = _pagePane.getUI();
        pagePaneSplitView.addItem(pagePaneUI);

        // Listen to PagePane
        _pagePane.addPropChangeListener(pc -> pagePaneDidPropChange(pc), PagePane.SelFile_Prop);

        // Add LeftTray
        ToolTray leftTray = _workspaceTools.getLeftTray();
        View leftTrayUI = leftTray.getUI();
        leftTray.setSelToolForClass(FileTreeTool.class);
        pagePaneSplitView.addItem(leftTrayUI, 0);

        // Add RightTray
        ToolTray rightTray = _workspaceTools.getRightTray();
        View rightTrayUI = rightTray.getUI();
        pagePaneSplitView.addItem(rightTrayUI);

        // Add SupportTray to MainSplit
        ToolTray bottomTray = _workspaceTools.getBottomTray();
        TabView bottomTrayUI = (TabView) bottomTray.getUI();
        mainSplit.addItem(bottomTrayUI);

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

        // Add key binding to OpenMenuItem and CloseWindow
        //addKeyActionHandler("OpenMenuItem", "meta O");
        //addKeyActionHandler("CloseFileAction", "meta W");

        // Register for WelcomePanel on close
        WindowView window = getWindow();
        enableEvents(window, WinClose);

        // If TeaVM, maximize window
        if (SnapUtils.isWebVM)
            window.setMaximized(true);
    }

    /**
     * Called when WorkspacePane is first shown.
     */
    @Override
    protected void initShowing()
    {
        if (SnapUtils.isWebVM && getEnv().getClass().getSimpleName().startsWith("Swing"))
            new ViewTimer(1000, e -> checkScreenSize()).start();
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
        _statusBar.resetLater();
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle OpenMenuItem
        if (anEvent.equals("OpenMenuItem")) {
            getToolBar().selectSearchText();
            anEvent.consume();
        }

        // Handle QuitMenuItem
        if (anEvent.equals("QuitMenuItem")) {
            App.getShared().quitApp();
            anEvent.consume();
        }

        // Handle NewFileMenuItem, NewFileButton
        if (anEvent.equals("NewFileMenuItem") || anEvent.equals("NewFileButton")) {
            FilesTool filesTool = _workspaceTools.getFilesTool();
            filesTool.showNewFilePanel();
            anEvent.consume();
        }

        // Handle CloseMenuItem, CloseFileAction
        if (anEvent.equals("CloseMenuItem") || anEvent.equals("CloseFileAction")) {
            _pagePane.removeOpenFile(getSelFile());
            anEvent.consume();
        }

        // Handle ShowLeftTrayMenuItem, ShowRightTrayMenuItem, ShowBottomTrayMenuItem
        if (anEvent.equals("ShowLeftTrayMenuItem"))
            _workspaceTools.setShowLeftTray(!_workspaceTools.isShowLeftTray());
        if (anEvent.equals("ShowRightTrayMenuItem"))
            _workspaceTools.setShowRightTray(!_workspaceTools.isShowRightTray());
        if (anEvent.equals("ShowBottomTrayMenuItem"))
            _workspaceTools.setShowBottomTray(!_workspaceTools.isShowBottomTray());

        // Handle CopyWebLinkMenuItem, OpenWebLinkMenuItem
        if (anEvent.equals("CopyWebLinkMenuItem"))
            copyWebLink();
        if (anEvent.equals("OpenWebLinkMenuItem"))
            openWebLink();

        // Handle ShowDevToolsMenuItem
        if (anEvent.equals("ShowDevToolsMenuItem"))
            DevPane.setDevPaneShowing(getUI(), true);

        // Handle ShowJavaHomeMenuItem
        if (anEvent.equals("ShowJavaHomeMenuItem")) {
            String java = System.getProperty("java.home");
            FileUtils.openFile(java);
        }

        // Handle WinClosing
        if (anEvent.isWinClose()) {
            hide();
            App.showWelcomePanelLater();
        }
    }

    /**
     * Called when Workspace does a property change.
     */
    private void workspaceDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle Status, Activity
        if (propName == Workspace.Status_Prop)
            getBrowser().setStatus(_workspace.getStatus());
        else if (propName == Workspace.Activity_Prop)
            getBrowser().setActivity(_workspace.getActivity());

        // Handle Building
        else if (propName == Workspace.Building_Prop) {
            if (!_workspace.isBuilding())
                handleBuildCompleted();
        }

        // Handle Projects
        else if (propName == Workspace.Projects_Prop) {
            Project addedProj = (Project) aPC.getNewValue();
            if (addedProj != null)
                workspaceDidAddProject(addedProj);
            else {
                Project removedProj = (Project) aPC.getOldValue();
                if (removedProj != null)
                    workspaceDidRemoveProject(removedProj);
            }
        }

        // Handle Loading, Building
        _statusBar.resetLater();
    }

    /**
     * Called when Workspace adds a project.
     */
    private void workspaceDidAddProject(Project aProject)
    {
        // Start listening to file changes
        WebSite projSite = aProject.getSite();
        projSite.addFileChangeListener(_siteFileLsnr);

        // Create/add ProjectPane
        ProjectPane projPane = new ProjectPane(this, aProject);
        _projectPanes = ArrayUtils.addId(_projectPanes, projPane);

        // Clear root files
        FileTreeTool fileTreeTool = _workspaceTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();

        // Reset UI
        resetLater();
    }

    /**
     * Called when Workspace removes a project.
     */
    private void workspaceDidRemoveProject(Project aProject)
    {
        // Remove ProjectPane
        ProjectPane projPane = getProjectPaneForProject(aProject);
        _projectPanes = ArrayUtils.remove(_projectPanes, projPane);

        // Stop listening to file changes
        WebSite projSite = aProject.getSite();
        projSite.removeFileChangeListener(_siteFileLsnr);

        // Clear root files
        FileTreeTool fileTreeTool = _workspaceTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();

        // Reset UI
        resetLater();
    }

    /**
     * Called when site file changes with File PropChange.
     */
    protected void siteFileChanged(PropChange aPC)
    {
        // Get file/prop
        WebFile file = (WebFile) aPC.getSource();
        String propName = aPC.getPropName();

        // Handle LastModTime, Modified: Update file in FileTreeTool
        if (propName == WebFile.LastModTime_Prop || propName == WebFile.Modified_Prop) {

            // Notify FileTreeTool to update file
            FileTreeTool fileTreeTool = _workspaceTools.getFileTreeTool();
            fileTreeTool.updateChangedFile(file);

            // If LastModTime, add build file
            if (propName == WebFile.LastModTime_Prop) {
                Project proj = Project.getProjectForFile(file);
                if (proj != null) {
                    ProjectBuilder projectBuilder = proj.getBuilder();
                    projectBuilder.addBuildFile(file, true);
                }
            }
        }
    }

    /**
     * Called when PagePane does prop change.
     */
    protected void pagePaneDidPropChange(PropChange aPC)
    {
        // Handle SelFile
        String propName = aPC.getPropName();
        if (propName == PagePane.SelFile_Prop) {
            FileTreeTool fileTreeTool = _workspaceTools.getFileTreeTool();
            fileTreeTool.resetLater();
        }
    }

    /**
     * Called when a build is completed.
     */
    private void handleBuildCompleted()
    {
        // If final error count non-zero, show problems pane
        int errorCount = _workspace.getBuildIssues().getErrorCount();
        if (errorCount > 0) {
            ToolTray bottomTray = _workspaceTools.getBottomTray();
            if (!(bottomTray.getSelTool() instanceof BuildTool))
                bottomTray.showProblemsTool();
        }

        // If error count zero and SupportTray showing problems, close
        else if (errorCount == 0) {
            ToolTray bottomTray = _workspaceTools.getBottomTray();
            if (bottomTray.getSelTool() instanceof BuildTool)
                bottomTray.hideTools();
        }
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
        Rect screenBounds = getEnv().getScreenBoundsInset();
        WindowView window = getWindow();
        if (window.getWidth() != screenBounds.width || window.getHeight() != screenBounds.height) {
            window.setBounds(screenBounds);
            System.out.println("Changed window size");
        }
    }
}

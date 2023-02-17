package snapcode.app;
import javakit.parse.JeplTextDoc;
import javakit.project.ProjectUtils;
import javakit.project.Workspace;
import javakit.project.Project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.Prefs;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.apptools.*;
import snapcode.project.WorkspaceX;
import snapcode.util.SamplesPane;

/**
 * This class is the top level controller for an open project.
 */
public class WorkspacePane extends ViewOwner {

    // The Workspace
    private Workspace  _workspace;

    // The array of ProjectPanes
    protected ProjectPane[]  _projectPanes = new ProjectPane[0];

    // The MainToolBar
    protected MainToolBar  _toolBar;

    // The PagePane to display project files for editing
    protected PagePane  _pagePane;

    // The StatusBar
    protected StatusBar  _statusBar;

    // The WorkspaceTools
    protected WorkspaceTools  _workspaceTools;

    // A PropChangeListener to watch for site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    public WorkspacePane()
    {
        this(new WorkspaceX());
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
        _pagePane = new PagePane(this);

        // Create MainToolBar, StatusBar
        _toolBar = new MainToolBar(this);
        _statusBar = new StatusBar(this);

        // Create WorkspaceTools
        _workspaceTools = new WorkspaceTools(this);
        _workspaceTools.createTools();

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
     * Opens a Jepl Workspace.
     */
    public void setWorkspaceForJeplFileSource(Object aSource)
    {
        // Delete temp project
        if (aSource == null)
            ProjectUtils.deleteTempProject();

        // Create new temp file and save
        JeplTextDoc jeplDoc = JeplTextDoc.getJeplTextDocForSourceURL(aSource);
        WebFile sourceFile = jeplDoc.getSourceFile();
        if (aSource == null) {
            sourceFile.setText("");
            sourceFile.save();
        }

        // Get project/workspace
        Project project = Project.getProjectForFile(sourceFile);
        Workspace workspace = project.getWorkspace();

        // Set Workspace
        setWorkspace(workspace);

        // Create WorkspacePane and show
        getUI();
        _workspaceTools.getLeftTray().setSelTool(null);
        _workspaceTools.getRightTray().setSelToolForClass(EvalTool.class);

        // Show JeplDoc
        runLater(() -> {
            PagePane pagePane = getPagePane();
            pagePane.setSelectedFile(sourceFile);
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
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _pagePane.getSelectedFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _pagePane.setSelectedFile(aFile);
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
            projPane.openSite();

        // Show HomePage
        _pagePane.showHomePage();
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
            projectPane.closeSite();

            // Close project site
            WebSite projectSite = projectPane.getSite();
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
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get MainSplit
        SplitView mainSplit = getUI(SplitView.class);
        mainSplit.setBorder(null);
        mainSplit.setDividerSpan(5);

        // Get MenuBar and register to process events
        MenuBar menuBar = getView("MenuBar", MenuBar.class);
        menuBar.addEventHandler(e -> { if (e.isShortcutDown()) ViewUtils.processEvent(menuBar, e); }, KeyPress);

        // Install ToolBar
        ColView mainColView = getView("MainColView", ColView.class);
        mainColView.addChild(_toolBar.getUI(), 1);

        // Get PagePaneSplitView
        SplitView pagePaneSplitView = getView("PagePaneSplitView", SplitView.class);
        pagePaneSplitView.setBorder(null);
        pagePaneSplitView.setDividerSpan(5);

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

        // Add StatusBar
        TabBar tabBar = bottomTrayUI.getTabBar();
        _statusBar.addToView(tabBar);

        // Add key binding to OpenMenuItem and CloseWindow
        //addKeyActionHandler("OpenMenuItem", "meta O");
        //addKeyActionHandler("CloseFileAction", "meta W");

        // Register for WelcomePanel on close
        enableEvents(getWindow(), WinClose);
    }

    /**
     * Resets UI panel.
     */
    @Override
    public void resetUI()
    {
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
            WelcomePanel.getShared().quitApp();
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

        // Handle ShowJavaHomeMenuItem
        if (anEvent.equals("ShowJavaHomeMenuItem")) {
            String java = System.getProperty("java.home");
            FileUtils.openFile(java);
        }

        // Handle WinClosing
        if (anEvent.isWinClose()) {
            hide();
            runLater(() -> {
                Prefs.getDefaultPrefs().flush();
                WelcomePanel.getShared().showPanel();
            });
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
        // Get file and update in FilesPane
        WebFile file = (WebFile) aPC.getSource();
        if (file.getExists()) {
            FileTreeTool fileTreeTool = _workspaceTools.getFileTreeTool();
            fileTreeTool.updateFile(file);
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
            if (!(bottomTray.getSelTool() instanceof ProblemsTool))
                bottomTray.showProblemsTool();
        }

        // If error count zero and SupportTray showing problems, close
        else if (errorCount == 0) {
            ToolTray bottomTray = _workspaceTools.getBottomTray();
            if (bottomTray.getSelTool() instanceof ProblemsTool)
                bottomTray.hideTools();
        }
    }
}
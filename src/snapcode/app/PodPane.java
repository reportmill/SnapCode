package snapcode.app;
import javakit.project.WorkSpace;
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
import snapcode.project.WorkSpaceX;

/**
 * This class is the top level controller for an open project.
 */
public class PodPane extends ViewOwner {

    // The Pod
    private WorkSpace _workSpace;

    // The list of sites
    protected WebSite[]  _sites = new WebSite[0];

    // The project
    private Project  _proj;

    // The MainToolBar
    protected MainToolBar  _toolBar;

    // The PagePane to display project files for editing
    protected PagePane  _pagePane;

    // The StatusBar
    protected StatusBar  _statusBar;

    // The PodTools
    protected PodTools  _podTools;

    // A PropChangeListener to watch for site file changes
    private PropChangeListener _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    public PodPane()
    {
        super();

        // Create Pod
        _workSpace = new WorkSpaceX();

        // Create MainToolBar, PagePane, StatusBar
        _toolBar = new MainToolBar(this);
        _pagePane = new PagePane(this);
        _statusBar = new StatusBar(this);

        // Create PodTools
        _podTools = new PodTools(this);
        _podTools.createTools();
    }

    /**
     * Returns the pod.
     */
    public WorkSpace getPod()  { return _workSpace; }

    /**
     * Returns the PagePane.
     */
    public PagePane getPagePane()  { return _pagePane; }

    /**
     * Returns the PagePane.Browser.
     */
    public WebBrowser getBrowser()  { return _pagePane.getBrowser(); }

    /**
     * Returns the PodTools helper.
     */
    public PodTools getPodTools()  { return _podTools; }

    /**
     * Returns the toolbar.
     */
    public MainToolBar getToolBar()  { return _toolBar; }

    /**
     * Returns the processes pane.
     */
    public ProcPane getProcPane()  { return _podTools.getDebugTool().getProcPane(); }

    /**
     * Returns the support tray.
     */
    public SupportTray getSupportTray()  { return _podTools.getSupportTray(); }

    /**
     * Returns the array of sites.
     */
    public WebSite[] getSites()  { return _sites; }

    /**
     * Returns the number of sites.
     */
    public int getSiteCount()  { return _sites.length; }

    /**
     * Returns the individual site at the given index.
     */
    public WebSite getSite(int anIndex)  { return _sites[anIndex]; }

    /**
     * Adds a site to sites list.
     */
    public void addSite(WebSite aSite)
    {
        // If site already added, just return
        if (ArrayUtils.contains(_sites, aSite)) return;

        // Add site
        _sites = ArrayUtils.add(_sites, aSite);

        // Start listening to file changes
        aSite.addFileChangeListener(_siteFileLsnr);

        // Get project for site
        WorkSpace workSpace = getPod();
        Project proj = workSpace.getProjectForSite(aSite);

        // Add listener to update tools when Breakpoint/BuildIssue added/removed
        proj.getBreakpoints().addPropChangeListener(pc -> _podTools.projBreakpointsDidChange(pc));
        proj.getBuildIssues().addPropChangeListener(pc -> _podTools.projBuildIssuesDidChange(pc));

        // Add site
        SitePane sitePane = SitePane.get(aSite, true);
        sitePane.setPodPane(this);

        // Add dependent sites
        for (Project p : proj.getProjects())
            addSite(p.getSite());

        // Clear root files
        FileTreeTool fileTreeTool = _podTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();

        // Reset UI
        resetLater();
    }

    /**
     * Removes a site from sites list.
     */
    public void removeSite(WebSite aSite)
    {
        // Remove site
        _sites = ArrayUtils.remove(_sites, aSite);

        // Stop listening to file changes
        aSite.removeFileChangeListener(_siteFileLsnr);

        // Clear root files
        FileTreeTool fileTreeTool = _podTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();

        // Reset UI
        resetLater();
    }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()  { return _sites[0]; }

    /**
     * Returns the selected project.
     */
    public Project getProject()
    {
        if (_proj != null) return _proj;
        return _proj = Project.getProjectForSite(getRootSite());
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
        WebFile file = getSelFile();
        WebSite site = file != null ? file.getSite() : null;
        if (!ArrayUtils.containsId(getSites(), site))
            site = getSite(0);
        return site;
    }

    /**
     * Shows the PodPane window.
     */
    public void show()
    {
        // Show window
        getUI();
        getWindow().setSaveName("AppPane");
        getWindow().setSaveSize(true);
        getWindow().setVisible(true);

        // Open site and show home page
        SitePane.get(getSite(0)).openSite();
        _pagePane.showHomePage();
    }

    /**
     * Close this PodPane.
     */
    public void hide()
    {
        // Flush and refresh sites
        for (WebSite site : getSites()) {
            SitePane.get(site).closeSite();
            try { site.flush(); }
            catch (Exception e) { throw new RuntimeException(e); }
            site.resetFiles();
        }

        _podTools.closeProject();
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        Project proj = getProject();
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

        // Add FilesPane
        SupportTray sideBar = _podTools.getSideBar();
        View sideBarUI = sideBar.getUI();
        sideBarUI.setGrowHeight(true);
        sideBar.setSelToolForClass(FileTreeTool.class);
        pagePaneSplitView.addItem(sideBarUI, 0);

        // Add SupportTray to MainSplit
        SupportTray supportTray = _podTools.getSupportTray();
        TabView supportTrayUI = (TabView) supportTray.getUI();
        mainSplit.addItem(supportTrayUI);

        // Add StatusBar
        TabBar tabBar = supportTrayUI.getTabBar();
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
        _podTools.resetLater();
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
            FilesTool filesTool = _podTools.getFilesTool();
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
     * Called when site file changes with File PropChange.
     */
    protected void siteFileChanged(PropChange aPC)
    {
        // Get file and update in FilesPane
        WebFile file = (WebFile) aPC.getSource();
        if (file.getExists()) {
            FileTreeTool fileTreeTool = _podTools.getFileTreeTool();
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
            FileTreeTool fileTreeTool = _podTools.getFileTreeTool();
            fileTreeTool.resetLater();
        }
    }
}

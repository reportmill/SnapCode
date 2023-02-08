package snapcode.app;
import javakit.ide.JavaTextPane;
import javakit.project.Breakpoints;
import javakit.project.BuildIssue;
import snap.util.ArrayUtils;
import snapcode.apptools.*;
import snapcode.project.ProjectX;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.FileUtils;
import snap.util.Prefs;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * The main view class for Projects.
 */
public class AppPane extends ProjectPane {

    // The main SplitView that holds sidebar and browser
    private SplitView  _mainSplit;

    // A PropChangeListener to watch for site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    public AppPane()
    {
        super();
    }

    /**
     * Returns the toolbar.
     */
    public MainToolBar getToolBar()  { return _toolBar; }

    /**
     * Returns the processes pane.
     */
    public ProcPane getProcPane()  { return _projTools.getDebugTool().getProcPane(); }

    /**
     * Returns the support tray.
     */
    public SupportTray getSupportTray()  { return _projTools.getSupportTray(); }

    /**
     * Adds a site to sites list.
     */
    @Override
    public void addSite(WebSite aSite)
    {
        // Do normal version
        if (ArrayUtils.contains(_sites, aSite)) return;
        super.addSite(aSite);

        // Start listening to file changes
        aSite.addFileChangeListener(_siteFileLsnr);

        // Create project for site
        ProjectX proj = ProjectX.getProjectForSite(aSite);
        if (proj == null)
            proj = new ProjectX(aSite);

        // Add listener to update ProcPane and JavaPage.TextArea(s) when Breakpoint/BuildIssue added/removed
        proj.getBreakpoints().addPropChangeListener(pc -> projBreakpointsDidChange(pc));
        proj.getBuildIssues().addPropChangeListener(pc -> projBuildIssuesDidChange(pc));

        // Add site
        SitePane sitePane = SitePane.get(aSite, true);
        sitePane.setAppPane(this);

        // Add dependent sites
        for (ProjectX p : proj.getProjects())
            addSite(p.getSite());

        // Clear root files
        FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();
    }

    /**
     * Removes a site from sites list.
     */
    @Override
    public void removeSite(WebSite aSite)
    {
        // Do normal version
        super.removeSite(aSite);

        // Stop listening to file changes
        aSite.removeFileChangeListener(_siteFileLsnr);

        // Clear root files
        FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
        fileTreeTool.resetRootFiles();
    }

    /**
     * Shows the AppPane window.
     */
    public void show()
    {
        // Set AppPane as OpenSite and show window
        getUI();
        getWindow().setSaveName("AppPane");
        getWindow().setSaveSize(true);
        getWindow().setVisible(true);

        // Open site and show home page
        SitePane.get(getSite(0)).openSite();
        _pagePane.showHomePage();
    }

    /**
     * Close this AppPane.
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

        _projTools.closeProject();
    }

    /**
     * Called when site file changes with File PropChange.
     */
    void siteFileChanged(PropChange aPC)
    {
        // Get file and update in FilesPane
        WebFile file = (WebFile) aPC.getSource();
        if (file.getExists()) {
            FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
            fileTreeTool.updateFile(file);
        }
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get MainSplit
        _mainSplit = getUI(SplitView.class);
        _mainSplit.setBorder(null);
        _mainSplit.setDividerSpan(5);

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
        SupportTray sideBar = _projTools.getSideBar();
        View sideBarUI = sideBar.getUI();
        sideBarUI.setGrowHeight(true);
        sideBar.setSelToolForClass(FileTreeTool.class);
        pagePaneSplitView.addItem(sideBarUI, 0);

        // Add SupportTray to MainSplit
        SupportTray supportTray = _projTools.getSupportTray();
        TabView supportTrayUI = (TabView) supportTray.getUI();
        _mainSplit.addItem(supportTrayUI);

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
            FilesTool filesTool = _projTools.getFilesTool();
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
     * Called when PagePane does prop change.
     */
    private void pagePaneDidPropChange(PropChange aPC)
    {
        // Handle SelFile
        String propName = aPC.getPropName();
        if (propName == PagePane.SelFile_Prop) {
            FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
            fileTreeTool.resetLater();
        }
    }

    /**
     * Called when Project.BreakPoints change.
     */
    private void projBreakpointsDidChange(PropChange pc)
    {
        DebugTool debugTool = _projTools.getDebugTool();
        debugTool.projBreakpointsDidChange(pc);
    }

    /**
     * Called when Project.BuildIssues change.
     */
    private void projBuildIssuesDidChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Handle BuildIssue added
        BuildIssue issueAdded = (BuildIssue) pc.getNewValue();
        if (issueAdded != null) {

            // Make current JavaPage.TextArea resetLater
            WebFile issueFile = issueAdded.getFile();
            WebPage page = getBrowser().getPageForURL(issueFile.getURL());
            if (page instanceof JavaPage) {
                JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
                javaTextPane.buildIssueOrBreakPointMarkerChanged();
            }

            // Update FilesPane.FilesTree
            FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
            fileTreeTool.updateFile(issueFile);
        }

        // Handle BuildIssue removed
        else {

            BuildIssue issueRemoved = (BuildIssue) pc.getOldValue();
            WebFile issueFile = issueRemoved.getFile();

            // Make current JavaPage.TextArea resetLater
            WebPage page = getBrowser().getPageForURL(issueFile.getURL());
            if (page instanceof JavaPage) {
                JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
                javaTextPane.buildIssueOrBreakPointMarkerChanged();
            }

            // Update FilesPane.FilesTree
            FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
            fileTreeTool.updateFile(issueFile);
        }
    }
}
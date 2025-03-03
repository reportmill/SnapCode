/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.GFXEnv;
import snapcode.apptools.BuildFileTool;
import snapcode.javatext.JavaTextArea;
import snapcode.project.JavaTextDoc;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.util.ClassInfoPage;
import snapcode.util.LZString;
import snapcode.webbrowser.*;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages and displays pages for editing project files.
 */
public class PagePane extends ViewOwner {

    // The WorkspacePane
    protected WorkspacePane  _workspacePane;

    // The HomePage
    private HomePage _homePage;

    // A list of open files
    List<WebFile>  _openFiles = new ArrayList<>();

    // The currently selected file
    private WebFile  _selFile;

    // The TabBar
    private TabBar  _tabBar;

    // The WebBrowser for displaying editors
    private WebBrowser  _browser;

    // Constants for properties
    public static final String OpenFiles_Prop = "OpenFiles";
    public static final String SelFile_Prop = "SelFile";

    // Constant for file tab attributes
    private static Color TAB_BAR_BORDER_COLOR = Color.GRAY8;
    private static Font TAB_BAR_FONT = new Font("Arial", 12);

    /**
     * Constructor.
     */
    public PagePane(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
    }

    /**
     * Returns the open files.
     */
    public List<WebFile> getOpenFiles()  { return _openFiles; }

    /**
     * Opens given file.
     */
    public void openFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelFile()) return;

        // If file already open, just select it
        if (_openFiles.contains(aFile)){
            getBrowser().setTransition(WebBrowser.Instant);
            setSelFile(aFile);
            return;
        }

        // If project file, add to open files and rebuild tabs
        if (shouldHaveFileTab(aFile)) {
            _openFiles.add(aFile);
            firePropChange(OpenFiles_Prop, null, aFile, _openFiles.size() - 1);
            buildFileTabs();
        }

        // Select file
        setSelFile(aFile);
    }

    /**
     * Closes the given file.
     */
    public void closeFile(WebFile aFile)
    {
        // Remove file from list (just return if not available)
        int index = ListUtils.indexOfId(_openFiles, aFile);
        if (index < 0)
            return;

        // Remove file
        _openFiles.remove(index);

        // If removed file is selected file, set browser file to last file (that is still in OpenFiles list)
        if (aFile == _selFile) {
            WebURL url = getFallbackURL();
            getBrowser().setTransition(WebBrowser.Instant);
            getBrowser().setSelUrl(url);
        }

        // Fire prop change
        firePropChange(OpenFiles_Prop, aFile, null, index);
        buildFileTabs();

        // Clear page from browser cache
        setPageForURL(aFile.getURL(), null);
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _selFile; }

    /**
     * Sets the selected site file.
     */
    protected void setSelFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelFile()) return;

        // Set SelFile
        WebFile oldSelFile = _selFile;
        _selFile = aFile;

        // Set selected file and update tree
        if (isPageAvailableForFile(_selFile))
            getBrowser().setSelFile(_selFile);

        // Fire prop change
        firePropChange(SelFile_Prop, oldSelFile, _selFile);
        resetLater();
        _workspacePane.resetLater();
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()
    {
        if (_browser != null) return _browser;
        getUI();
        return _browser;
    }

    /**
     * Sets the browser URL.
     */
    public void setBrowserURL(WebURL aURL)  { _browser.setSelUrl(aURL); }

    /**
     * Sets the browser URL.
     */
    public void setBrowserFile(WebFile aFile)  { _browser.setSelFile(aFile); }

    /**
     * Sets the browser URL.
     */
    public void setPageForURL(WebURL aURL, WebPage aPage)  { _browser.setPageForURL(aURL, aPage); }

    /**
     * Reloads a file.
     */
    public void reloadFile(WebFile aFile)  { _browser.reloadFile(aFile); }

    /**
     * Returns the selected page.
     */
    public WebPage getSelPage()  { return _browser.getSelPage(); }

    /**
     * Sets the selected page.
     */
    public void setSelPage(WebPage aPage)
    {
        _browser.setSelPage(aPage);
    }

    /**
     * Shows the home page.
     */
    public void showHomePage()
    {
        _homePage = new HomePage(_workspacePane);
        setPageForURL(_homePage.getURL(), _homePage);
        setSelPage(_homePage);
        if (!_workspacePane._workspaceTools.getHelpTool().isLesson())
            _workspacePane.getWorkspaceTools().getRightTray().hideTools();
        _workspacePane.getWorkspaceTools().getRunTool().cancelRun();
    }

    /**
     * Removes a file from OpenFiles list.
     */
    public void removeAllOpenFilesExcept(WebFile aFile)
    {
        List<WebFile> openFilesCopy = ListUtils.filter(_openFiles, file -> file != aFile);
        openFilesCopy.forEach(this::closeFile);
    }

    /**
     * Removes open files for given project.
     */
    protected void removeOpenFilesForProject(Project aProject)
    {
        List<WebFile> openFiles = getOpenFiles();
        List<WebFile> openProjectFiles = ListUtils.filter(openFiles, file -> Project.getProjectForFile(file) == aProject);
        openProjectFiles.forEach(this::closeFile);
    }

    /**
     * Shows the given exception.
     */
    public void showException(WebURL aURL, Exception e)
    {
        _browser.showException(aURL, e);
    }

    /**
     * Returns the URL to fallback on when open file is closed.
     */
    private WebURL getFallbackURL()
    {
        // Return the most recently opened of the remaining OpenFiles, or the Project.HomePageURL
        WebBrowser browser = getBrowser();
        WebURL[] urls = browser.getHistory().getNextURLs();
        for (WebURL url : urls) {
            WebFile file = url.getFile();
            if (_openFiles.contains(file))
                return url.getFileURL();
        }

        urls = browser.getHistory().getLastURLs();
        for (WebURL url : urls) {
            WebFile file = url.getFile();
            if (_openFiles.contains(file))
                return url.getFileURL();
        }

        // Return
        return null;
    }

    /**
     * Returns the Window.location.hash for current Workspace selected page.
     */
    public String getWindowLocationHash()
    {
        WebPage selPage = getSelPage();

        // Handle JavaPage: Return 'Java:...' or 'Jepl:...'
        if (selPage instanceof JavaPage) {
            JavaPage javaPage = (JavaPage) selPage;
            JavaTextArea javaTextArea = javaPage.getTextArea();
            JavaTextDoc javaTextDoc = (JavaTextDoc) javaTextArea.getSourceText();
            String prefix = javaTextDoc.isJepl() ? "Jepl:" : javaTextDoc.isJMD() ? "JMD:" : "Java:";
            String javaText = javaTextDoc.getString();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return prefix + javaTextLZ;
        }

        // Handle Java markdown
        if (selPage instanceof JMDPage) {
            JMDPage javaPage = (JMDPage) selPage;
            TextArea textArea = javaPage.getTextArea();
            String javaText = textArea.getText();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return "JMD:" + javaTextLZ;
        }

        // Return null
        return null;
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Create TabBar for open file buttons
        _tabBar = new TabBar();
        _tabBar.setFont(TAB_BAR_FONT);
        _tabBar.setGrowWidth(true);
        _tabBar.setTabCloseActionHandler((e,tab) -> handleTabCloseAction(tab));

        // Create separator
        RectView separator = new RectView();
        separator.setFill(TAB_BAR_BORDER_COLOR);
        separator.setPrefHeight(1);
        ViewUtils.bind(_tabBar, View.Visible_Prop, separator, false);

        // Set PrefHeight so it will show when empty
        Tab sampleTab = new Tab();
        sampleTab.setTitle("XXX");
        _tabBar.addTab(sampleTab);
        _tabBar.setPrefHeight(_tabBar.getPrefHeight() + 2);
        _tabBar.removeTab(0);

        // Create browser
        _browser = new WebBrowser();
        _browser.setGrowHeight(true);
        _browser.setPageClassResolver(this::getPageClassForResponse);
        _browser.addPropChangeListener(this::handleBrowserPropChange,
                WebBrowser.SelPage_Prop, WebBrowser.Activity_Prop, WebBrowser.Status_Prop, WebBrowser.Loading_Prop);

        // Create ColView to hold TabsBox and Browser
        ColView colView = new ColView();
        colView.setGrowWidth(true);
        colView.setGrowHeight(true);
        colView.setFillWidth(true);
        colView.setChildren(_tabBar, separator, _browser);

        // Return
        return colView;
    }

    /**
     * ResetUI.
     */
    @Override
    protected void resetUI()
    {
        // Update TabBar.SelIndex
        WebFile selFile = getSelFile();
        List<WebFile> openFiles = getOpenFiles();
        int selIndex = openFiles.indexOf(selFile);
        _tabBar.setSelIndex(selIndex);

        // Update TabBar Visible
        boolean showTabBar = !getOpenFiles().isEmpty();
        if (showTabBar && getOpenFiles().size() == 1 && selFile != null) {
             if ("jepl".equals(selFile.getFileType()) || selFile.getName().contains("JavaFiddle"))
                showTabBar = false;
        }
        _tabBar.setVisible(showTabBar);
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TabBar
        if (anEvent.getView() == _tabBar) {
            List<WebFile> openFiles = getOpenFiles();
            int selIndex = _tabBar.getSelIndex();
            WebFile openFile = selIndex >= 0 ? openFiles.get(selIndex) : null;
            openFile(openFile);
        }
    }

    /**
     * Builds the file tabs.
     */
    private void buildFileTabs()
    {
        // If not on event thread, come back on that
        if (!isEventThread()) {
            runLater(() -> buildFileTabs());
            return;
        }

        // Remove tabs
        _tabBar.removeTabs();

        // Iterate over OpenFiles, create FileTabs, init and add
        for (WebFile file : getOpenFiles()) {

            // Create/config/add Tab
            Tab fileTab = new Tab();
            fileTab.setTitle(file.getName());
            fileTab.setClosable(true);
            _tabBar.addTab(fileTab);

            // Configure Tab.Button
            ToggleButton tabButton = fileTab.getButton();
            tabButton.addEventFilter(e -> handleTabButtonMousePress(e, file), MousePress);
        }

        // Reset UI
        resetLater();
    }

    /**
     * Returns whether given file is a project file.
     */
    protected boolean isProjectFile(WebFile aFile)
    {
        if (aFile == null) return false;
        WebSite[] projSites = _workspacePane.getSites();
        WebSite fileSite = aFile.getSite();
        return ArrayUtils.containsId(projSites, fileSite);
    }

    /**
     * Returns whether a file is an "OpenFile" (whether it needs a file tab).
     */
    protected boolean shouldHaveFileTab(WebFile aFile)
    {
        // If directory, return false
        if (aFile.isDir()) return false;

        // Accept all Java files
        if (aFile.getFileType().equals("java"))
            return true;

        // Accept all project files
        return isProjectFile(aFile);
    }

    /**
     * Called when TabBar Tab button close box is triggered.
     */
    private void handleTabCloseAction(Tab aTab)
    {
        int index = _tabBar.getTabs().indexOf(aTab);
        if (index >= 0) {
            WebFile tabFile = getOpenFiles().get(index);
            closeFile(tabFile);
        }
    }

    /**
     * Called when tab button gets mouse press.
     */
    private void handleTabButtonMousePress(ViewEvent anEvent, WebFile aFile)
    {
        if (anEvent.isPopupTrigger()) {
            Menu contextMenu = createFileContextMenu(aFile);
            contextMenu.showMenuAtXY(anEvent.getView(), anEvent.getX(), anEvent.getY());
            anEvent.consume();
        }
    }

    /**
     * Creates popup menu for tab button.
     */
    private Menu createFileContextMenu(WebFile aFile)
    {
        ViewBuilder<MenuItem> mb = new ViewBuilder<>(MenuItem.class);
        mb.text("Revert File").save().addEventHandler(e -> revertFile(aFile), Action);
        mb.text("Show file in Finder/Explorer").save().addEventHandler(e -> showFileInFinder(aFile), Action);
        mb.text("Show file in Text Editor").save().addEventHandler(e -> showFileInTextEditor(aFile), Action);
        return mb.buildMenu("ContextMenu", null);
    }

    /**
     * Reverts a file.
     */
    public void revertFile(WebFile aFile)
    {
        boolean isSelFile = aFile == getSelFile();
        closeFile(aFile);
        aFile.resetAndVerify();
        setPageForURL(aFile.getURL(), null);
        if (isSelFile)
            openFile(aFile);
    }

    /**
     * Shows a file in finder.
     */
    public void showFileInFinder(WebFile aFile)
    {
        WebFile dirFile = aFile.isDir() ? aFile : aFile.getParent();
        GFXEnv.getEnv().openFile(dirFile);
    }

    /**
     * Shows a file in text editor.
     */
    public void showFileInTextEditor(WebFile aFile)
    {
        WebURL url = aFile.getURL();
        WebPage page = new TextPage();
        page.setURL(url);
        setPageForURL(page.getURL(), page);
        setBrowserURL(url);
    }

    /**
     * Returns whether page is available for file.
     */
    protected boolean isPageAvailableForFile(WebFile aFile)
    {
        if (_selFile.isFile())
            return true;
        if (_selFile.isRoot())
            return true;
        if (aFile == _workspacePane.getBuildDir())
            return true;
        return false;
    }

    /**
     * Called when Browser does prop change.
     */
    private void handleBrowserPropChange(PropChange aPC)
    {
        Workspace workspace = _workspacePane.getWorkspace();

        switch (aPC.getPropName()) {

            // Handle SelPage
            case WebBrowser.SelPage_Prop: openFile(_browser.getSelFile()); break;

            // Handle Activity, Status, Loading
            case WebBrowser.Activity_Prop: workspace.setActivity(_browser.getActivity()); break;
            case WebBrowser.Status_Prop: workspace.setStatus(_browser.getStatus()); break;
            case WebBrowser.Loading_Prop: workspace.setLoading(_browser.isLoading()); break;
        }
    }

    /**
     * Returns WebPage class for given response.
     */
    private Class<? extends WebPage> getPageClassForResponse(WebResponse aResp)
    {
        // Handle common types
        switch (aResp.getFileType()) {

            // Handle Java / Jepl
            case "java": case "jepl": return JavaPage.class;

            // Handle JMD
            case "jmd": return JMDPage.class;

            // Handle Snap file
            case "snp": return SnapBuilderPage.class;

            // Handle mark down file
            case "md": return MarkDownPage.class;

            // Handle build file (build.snapcode)
            case "snapcode": return BuildFileTool.BuildFilePage.class;

            // Handle project.greenfoot file
            case "greenfoot": return GreenfootPage.class;

            // Handle class file
            case "class": return ClassInfoPage.class;
        }

        // Handle Project Root directory
        WebFile file = aResp.getFile();
        if (file != null && file.isRoot() && isProjectFile(file))
            return ProjectPane.ProjectPanePage.class;

        // Handle BuildDir
        if (file == _workspacePane.getBuildDir())
            return BuildDirPage.class;

        // Return no page class
        return null;
    }
}

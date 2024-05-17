/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.GFXEnv;
import snapcode.apptools.BuildFileTool;
import snapcode.javatext.JavaTextArea;
import snapcode.project.JavaTextDoc;
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
    private static Color TAB_TEXT_COLOR = Color.GRAY2;

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
    public WebFile[] getOpenFiles()  { return _openFiles.toArray(new WebFile[0]); }

    /**
     * Adds a file to OpenFiles list.
     */
    public void addOpenFile(WebFile aFile)
    {
        // If already open file, just return
        if (aFile == null || !shouldHaveFileTab(aFile))
            return;
        if (ListUtils.containsId(_openFiles, aFile))
            return;

        // Add file
        _openFiles.add(aFile);

        // Fire prop change
        firePropChange(OpenFiles_Prop, null, aFile, _openFiles.size() - 1);
        buildFileTabs();
    }

    /**
     * Removes a file from OpenFiles list.
     */
    public void removeOpenFile(WebFile aFile)
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
    }

    /**
     * Removes a file from OpenFiles list.
     */
    public void removeAllOpenFilesExcept(WebFile aFile)
    {
        WebFile[] openFilesCopy = _openFiles.toArray(new WebFile[0]);
        for (WebFile openFile : openFilesCopy)
            if (openFile != aFile)
                removeOpenFile(openFile);
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _selFile; }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelFile()) return;

        // Set SelFile
        WebFile oldSelFile = _selFile;
        _selFile = aFile;

        // Set selected file and update tree
        if (isPageAvailableForFile(_selFile))
            getBrowser().setSelFile(_selFile);

        // Add to OpenFiles
        addOpenFile(aFile);

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
        _workspacePane.getWorkspaceTools().getRightTray().hideTools();
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
            String prefix = javaTextDoc.isJepl() ? "Jepl:" : "Java:";
            String javaText = javaTextDoc.getString();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return prefix + javaTextLZ;
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
        _browser.addPropChangeListener(this::browserDidPropChange,
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
        WebFile[] openFiles = getOpenFiles();
        int selIndex = ArrayUtils.indexOfId(openFiles, selFile);
        _tabBar.setSelIndex(selIndex);

        // Update TabBar Visible
        boolean showTabBar = getOpenFiles().length > 0;
        if (showTabBar && getOpenFiles().length == 1 && selFile != null) {
             if ("jepl".equals(selFile.getType()) || selFile.getName().contains("JavaFiddle"))
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
            int selIndex = _tabBar.getSelIndex();
            WebFile[] openFiles = getOpenFiles();
            WebFile openFile = selIndex >= 0 ? openFiles[selIndex] : null;
            getBrowser().setTransition(WebBrowser.Instant);
            setSelFile(openFile);
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
        WebFile[] openFiles = getOpenFiles();
        for (WebFile file : openFiles) {

            // Create/config/add Tab
            Tab fileTab = new Tab();
            fileTab.setTitle(file.getName());
            fileTab.setClosable(true);
            _tabBar.addTab(fileTab);

            // Configure Tab.Button
            ToggleButton tabButton = fileTab.getButton();
            tabButton.setTextFill(TAB_TEXT_COLOR);
            tabButton.addEventFilter(e -> tabButtonMousePress(e, file), MousePress);
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
        if (aFile.getType().equals("java"))
            return true;

        // Accept all project files
        return isProjectFile(aFile);
    }

    /**
     * Called when TabBar Tab button close box is triggered.
     */
    private void handleTabCloseAction(Tab aTab)
    {
        int index = ListUtils.indexOfId(_tabBar.getTabs(), aTab);
        if (index >= 0) {
            WebFile tabFile = getOpenFiles()[index];
            removeOpenFile(tabFile);
        }
    }

    /**
     * Called when tab button gets mouse press.
     */
    private void tabButtonMousePress(ViewEvent anEvent, WebFile aFile)
    {
        if (anEvent.isPopupTrigger()) {
            Menu contextMenu = createFileContextMenu(aFile);
            contextMenu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
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
        removeOpenFile(aFile);
        aFile.resetAndVerify();
        setPageForURL(aFile.getURL(), null);
        setSelFile(aFile);
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
    private void browserDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        Workspace workspace = _workspacePane.getWorkspace();

        // Handle SelPage, Activity, Status, Loading
        if (propName == WebBrowser.SelPage_Prop)
            setSelFile(_browser.getSelFile());
        if (propName == WebBrowser.Status_Prop)
            workspace.setStatus(_browser.getStatus());
        else if (propName == WebBrowser.Activity_Prop)
            workspace.setActivity(_browser.getActivity());
        else if (propName == WebBrowser.Loading_Prop)
            workspace.setLoading(_browser.isLoading());
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

            // Handle Snap file
            case "snp": return SnapBuilderPage.class;

            // Handle mark down file
            case "md": return MarkDownPage.class;

            // Handle build file (build.snapcode)
            case "snapcode": return BuildFileTool.BuildFilePage.class;

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

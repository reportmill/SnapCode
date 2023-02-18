/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.project.Workspace;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.*;
import snap.viewx.TextPage;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
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
    private WorkspacePane  _workspacePane;

    // A list of open files
    List<WebFile>  _openFiles = new ArrayList<>();

    // The currently selected file
    private WebFile  _selFile;

    // The TabBar
    private TabBar  _tabBar;

    // The WebBrowser for displaying editors
    private WebBrowser  _browser;

    // The default HomePage
    private HomePage  _homePage;

    // Constants for properties
    public static final String OpenFiles_Prop = "OpenFiles";
    public static final String SelFile_Prop = "SelFile";

    // Constant for file tab attributes
    private static Color TAB_BAR_BORDER_COLOR = Color.GRAY7;
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
            if (!url.equals(getHomePageURL()))
                getBrowser().setTransition(WebBrowser.Instant);
            getBrowser().setURL(url);
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
        for (WebFile file : _openFiles.toArray(new WebFile[0]))
            if (file != aFile) removeOpenFile(file);
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelectedFile()  { return _selFile; }

    /**
     * Sets the selected site file.
     */
    public void setSelectedFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelectedFile()) return;

        // Set SelFile
        WebFile oldSelFile = _selFile;
        _selFile = aFile;

        // Set selected file and update tree
        if (_selFile.isFile() || _selFile.isRoot())
            getBrowser().setFile(_selFile);

        // Add to OpenFiles
        addOpenFile(aFile);

        // Fire prop change
        firePropChange(SelFile_Prop, oldSelFile, _selFile);
        resetLater();
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
    public void setBrowserURL(WebURL aURL)  { _browser.setURL(aURL); }

    /**
     * Sets the browser URL.
     */
    public void setBrowserFile(WebFile aFile)  { _browser.setFile(aFile); }

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
    public WebPage getSelPage()  { return _browser.getPage(); }

    /**
     * Sets the selected page.
     */
    public void setSelPage(WebPage aPage)
    {
        _browser.setPage(aPage);
    }

    /**
     * Creates page for given URL.
     */
    public WebPage createPageForURL(WebURL aURL)
    {
        return _browser.createPageForURL(aURL);
    }

    /**
     * Shows the given exception.
     */
    public void showException(WebURL aURL, Exception e)
    {
        _browser.showException(aURL, e);
    }

    /**
     * Shows the home page.
     */
    public void showHomePage()
    {
        WebURL url = getHomePageURL();
        _browser.setURL(url);
    }

    /**
     * Returns the HomePage URL.
     */
    public WebURL getHomePageURL()  { return getHomePage().getURL(); }

    /**
     * Returns the HomePage.
     */
    public HomePage getHomePage()
    {
        if (_homePage != null) return _homePage;
        _homePage = new HomePage();
        setPageForURL(_homePage.getURL(), _homePage);
        return _homePage;
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
        return getHomePageURL();
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Create TabBar for open file buttons
        _tabBar = new TabBar();
        //_tabBar.setBorder(TAB_BAR_BORDER_COLOR, 1);
        _tabBar.setFont(TAB_BAR_FONT);
        _tabBar.setGrowWidth(true);

        // Create separator
        RectView separator = new RectView();
        separator.setFill(TAB_BAR_BORDER_COLOR);
        separator.setPrefHeight(1);
        ViewUtils.bind(_tabBar, View.Visible_Prop, separator, false);

        // Set PrefHeight so it will show when empty
        Tab sampleTab = new Tab();
        sampleTab.setTitle("XXX");
        _tabBar.addTab(sampleTab);
        _tabBar.setPrefHeight(_tabBar.getPrefHeight());
        _tabBar.removeTab(0);

        // Create browser
        _browser = new AppBrowser();
        _browser.setGrowHeight(true);
        _browser.addPropChangeListener(pc -> browserDidPropChange(pc),
                WebBrowser.Activity_Prop, WebBrowser.Status_Prop, WebBrowser.Loading_Prop);

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
        WebFile selFile = getSelectedFile();
        WebFile[] openFiles = getOpenFiles();
        int selIndex = ArrayUtils.indexOfId(openFiles, selFile);
        _tabBar.setSelIndex(selIndex);

        // Update TabBar Visible
        boolean showTabBar = getOpenFiles().length > 1;
        if (showTabBar && getOpenFiles().length == 1 && selFile != null && "jepl".equals(selFile.getType()))
            showTabBar = false;
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
            setSelectedFile(openFile);
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
        }

        // Reset UI
        resetLater();
    }

    /**
     * Returns whether given file is a project file.
     */
    protected boolean isProjectFile(WebFile aFile)
    {
        WebSite[] projSites = _workspacePane.getSites();
        WebSite fileSite = aFile.getSite();
        return ArrayUtils.containsId(projSites, fileSite);
    }

    /**
     * Called when Browser does prop change.
     */
    private void browserDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        Workspace workspace = _workspacePane.getWorkspace();

        // Handle Activity, Status, Loading
        if (propName == WebBrowser.Status_Prop)
            workspace.setStatus(_browser.getStatus());
        else if (propName == WebBrowser.Activity_Prop)
            workspace.setActivity(_browser.getActivity());
        else if (propName == WebBrowser.Loading_Prop)
            workspace.setLoading(_browser.isLoading());
    }

    /**
     * A custom browser subclass.
     */
    private class AppBrowser extends WebBrowser {

        /**
         * Override to make sure that PagePane is in sync.
         */
        public void setPage(WebPage aPage)
        {
            // Do normal version
            if (aPage == getPage()) return;
            super.setPage(aPage);

            // Select page file
            WebFile file = aPage != null ? aPage.getFile() : null;
            setSelectedFile(file);
        }

        /**
         * Creates a WebPage for given file.
         */
        protected Class<? extends WebPage> getPageClass(WebResponse aResp)
        {
            // Get file and data
            WebFile file = aResp.getFile();
            String type = aResp.getPathType();

            // Handle Project Root directory
            if (file != null && file.isRoot() && isProjectFile(file))
                return ProjectPane.ProjectPanePage.class;

            // Handle Java
            if (type.equals("java") || type.equals("jepl")) {
                if (file != null && SnapEditorPage.isSnapEditSet(file))
                    return SnapEditorPage.class;
                return JavaPage.class;
            }

            //if(type.equals("snp")) return snapbuild.app.EditorPage.class;
            if (type.equals("rpt"))
                return getPageClass("com.reportmill.app.ReportPageEditor", TextPage.class);
            if (type.equals("class") && isProjectFile(file))
                return ClassInfoPage.class;
            if (type.equals("pgd"))
                return JavaShellPage.class;

            // Do normal version
            return super.getPageClass(aResp);
        }
    }
}

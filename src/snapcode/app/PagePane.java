/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.GFXEnv;
import snapcode.project.Project;
import snap.props.PropChange;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.webbrowser.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages and displays pages for project files.
 */
public class PagePane extends WebBrowserPane {

    // The WorkspacePane
    protected WorkspacePane _workspacePane;

    // The HomePage
    private HomePage _homePage;

    // A list of open files
    List<WebFile> _openFiles = new ArrayList<>();

    // The TabBar
    private PagePaneTabBar _tabBar;

    // Constants for properties
    public static final String OpenFiles_Prop = "OpenFiles";

    /**
     * Constructor.
     */
    public PagePane(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
    }

    /**
     * Override to use PagePaneBrowser.
     */
    @Override
    protected WebBrowser createBrowser()
    {
        WebBrowser browser = new PagePaneBrowser(this);
        browser.addPropChangeListener(this::handleBrowserSelPageChange, WebBrowser.SelPage_Prop);
        return browser;
    }

    /**
     * Returns the open files.
     */
    public List<WebFile> getOpenFiles()  { return _openFiles; }

    /**
     * Adds an open file.
     */
    private void addOpenFile(WebFile aFile)
    {
        if (_openFiles.contains(aFile))
            return;
        if (!_tabBar.shouldHaveFileTab(aFile))
            return;

        _openFiles.add(aFile);
        firePropChange(OpenFiles_Prop, null, aFile, _openFiles.size() - 1);
        _tabBar.buildFileTabs();
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

        // Fire prop change
        firePropChange(OpenFiles_Prop, aFile, null, index);
        _tabBar.buildFileTabs();

        // If removed file is selected file, set browser file to last file (that is still in OpenFiles list)
        if (aFile == getSelFile()) {
            WebURL fallbackUrl = getFallbackUrl();
            getBrowser().setTransition(WebBrowser.Instant);
            getBrowser().setSelUrl(fallbackUrl);
        }

        // Clear page from browser cache
        setPageForURL(aFile.getUrl(), null);
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
     * Shows the home page.
     */
    public void showHomePage()
    {
        _homePage = new HomePage(_workspacePane);
        setPageForURL(_homePage.getURL(), _homePage);
        setSelPage(_homePage);
        if (!_workspacePane._workspaceTools.getHelpTool().isLesson())
            _workspacePane.getWorkspaceTools().getRightTray().hideTools();
        _workspacePane.getRunTool().cancelRun();
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
    private WebURL getFallbackUrl()
    {
        WebBrowser browser = getBrowser();

        // Return the most recently opened of the remaining OpenFiles, or the Project.HomePageURL
        List<WebURL> nextUrls = browser.getHistory().getNextUrls();
        WebURL nextOpenUrl = ListUtils.findMatch(nextUrls, url -> _openFiles.contains(url.getFile()));
        if (nextOpenUrl != null)
            return nextOpenUrl;

        //
        List<WebURL> lastUrls = browser.getHistory().getLastUrls();
        WebURL lastOpenUrl = ListUtils.findMatch(lastUrls, url -> _openFiles.contains(url.getFile()));
        if (lastOpenUrl != null)
            return lastOpenUrl;

        // Return
        return null;
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Create TabBar for open file buttons
        _tabBar = new PagePaneTabBar(this);

        // Create ColView to hold TabsBox and Browser
        ColView colView = new ColView();
        colView.setGrowWidth(true);
        colView.setGrowHeight(true);
        colView.setFillWidth(true);
        colView.setChildren(_tabBar, _tabBar.getSeparator(), _browser);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()  { }

    /**
     * ResetUI.
     */
    @Override
    protected void resetUI()
    {
        // Update TabBar
        _tabBar.resetUI();
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TabBar
        if (anEvent.getView() == _tabBar)
            _tabBar.handleTabBarActionEvent(anEvent);
    }

    /**
     * Returns whether given file is a project file.
     */
    protected boolean isProjectFile(WebFile aFile)
    {
        if (aFile == null) return false;
        List<WebSite> projSites = _workspacePane.getProjectSites();
        WebSite fileSite = aFile.getSite();
        return projSites.contains(fileSite);
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
        WebURL url = aFile.getUrl();
        WebPage textPage = new TextPage();
        textPage.setURL(url);
        setPageForURL(textPage.getURL(), textPage);
        setSelPage(textPage);
    }

    /**
     * Returns whether page is available for file.
     */
    protected boolean isPageAvailableForFile(WebFile aFile)
    {
        if (aFile.isFile())
            return true;
        if (aFile.isRoot())
            return true;
        if (aFile == _workspacePane.getBuildDir())
            return true;
        return false;
    }

    /**
     * Called when Browser does prop change.
     */
    private void handleBrowserSelPageChange(PropChange aPC)
    {
        // Handle SelPage
        if (aPC.getPropName() == WebBrowser.SelPage_Prop) {
            WebFile selFile = getSelFile();
            _workspacePane.getSelFileTool().setSelFile(selFile);
            if (selFile != null)
                addOpenFile(selFile);
            resetLater();
        }
    }
}
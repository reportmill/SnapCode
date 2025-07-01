/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.GFXEnv;
import snapcode.javatext.JavaTextArea;
import snapcode.project.JavaTextDoc;
import snapcode.project.Project;
import snap.props.PropChange;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.util.LZString;
import snapcode.webbrowser.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages and displays pages for project files.
 */
public class PagePane extends ViewOwner {

    // The WorkspacePane
    protected WorkspacePane _workspacePane;

    // The HomePage
    private HomePage _homePage;

    // A list of open files
    List<WebFile> _openFiles = new ArrayList<>();

    // The TabBar
    private PagePaneTabBar _tabBar;

    // The WebBrowser for displaying editors
    private WebBrowser _browser;

    // Constants for properties
    public static final String OpenFiles_Prop = "OpenFiles";

    /**
     * Constructor.
     */
    public PagePane(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;

        // Create browser
        _browser = new PagePaneBrowser(this);
        _browser.setGrowHeight(true);
        _browser.addPropChangeListener(this::handleBrowserSelPageChange, WebBrowser.SelPage_Prop);
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _browser; }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _browser.getSelFile(); }

    /**
     * Sets the selected file.
     */
    public void setSelFile(WebFile aFile)  { _browser.setSelFile(aFile); }

    /**
     * Sets the browser URL.
     */
    public void setSelURL(WebURL aURL)  { _browser.setSelUrl(aURL); }

    /**
     * Returns the selected page.
     */
    public WebPage getSelPage()  { return _browser.getSelPage(); }

    /**
     * Sets the selected page.
     */
    public void setSelPage(WebPage aPage)  { _browser.setSelPage(aPage); }

    /**
     * Sets the browser URL.
     */
    public void setPageForURL(WebURL aURL, WebPage aPage)  { _browser.setPageForURL(aURL, aPage); }

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
        setPageForURL(aFile.getURL(), null);
    }

    /**
     * Reloads a file.
     */
    public void reloadFile(WebFile aFile)  { _browser.reloadFile(aFile); }

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
            setSelFile(aFile);
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
     * Returns the Window.location.hash for current Workspace selected page.
     */
    public String getWindowLocationHash()
    {
        WebPage selPage = getSelPage();

        // Handle JavaPage: Return 'Java:...' or 'Jepl:...'
        if (selPage instanceof JavaPage javaPage) {
            JavaTextArea javaTextArea = javaPage.getTextArea();
            JavaTextDoc javaTextDoc = (JavaTextDoc) javaTextArea.getSourceText();
            String prefix = javaTextDoc.isJepl() ? "Jepl:" : javaTextDoc.isJMD() ? "JMD:" : "Java:";
            String javaText = javaTextDoc.getString();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return prefix + javaTextLZ;
        }

        // Handle Java markdown
        if (selPage instanceof JMDPage javaPage) {
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
        WebURL url = aFile.getURL();
        WebPage page = new TextPage();
        page.setURL(url);
        setPageForURL(page.getURL(), page);
        setSelURL(url);
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
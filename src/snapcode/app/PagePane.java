/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.ColView;
import snap.view.View;
import snap.view.ViewOwner;
import snap.viewx.TextPage;
import snap.viewx.WebBrowser;
import snap.viewx.WebBrowserHistory;
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

    // The PodPane
    private PodPane  _podPane;

    // A list of open files
    List<WebFile>  _openFiles = new ArrayList<>();

    // The currently selected file
    private WebFile  _selFile;

    // The WebBrowser for displaying editors
    private WebBrowser  _browser;

    // The default HomePage
    private HomePage  _homePage;

    // Constants for properties
    public static final String OpenFiles_Prop = "OpenFiles";
    public static final String SelFile_Prop = "SelFile";

    /**
     * Constructor.
     */
    public PagePane(PodPane podPane)
    {
        super();
        _podPane = podPane;
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
     * Shows history.
     */
    public void showHistory()
    {
        // Create History string
        WebBrowser browser = getBrowser();
        WebBrowserHistory history = browser.getHistory();
        StringBuilder sb = new StringBuilder();
        for (WebURL url : history.getLastURLs())
            sb.append(url.getString()).append('\n');

        // Create temp file and show
        WebURL fileURL = WebURL.getURL("/tmp/History.txt");
        WebFile file = fileURL.createFile(false);
        file.setText(sb.toString());
        browser.setFile(file);
    }

    /**
     * Returns the HomePage URL.
     */
    public WebURL getHomePageURL()
    {
        //WebURL url = SitePane.get(getRootSite()).getHomePageURL();
        //return url != null ? url : getHomePageURL();
        return getHomePage().getURL();
    }

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
        // Create TabsBox
        PagePaneTabsBox tabsBox = new PagePaneTabsBox(this);

        // Create browser
        _browser = new AppBrowser();
        _browser.setGrowHeight(true);
        _browser.addPropChangeListener(pc -> browserDidPropChange(pc),
                WebBrowser.Activity_Prop, WebBrowser.Status_Prop, WebBrowser.Loading_Prop);

        // Create ColView to hold TabsBox and Browser
        ColView colView = new ColView();
        colView.setFillWidth(true);
        colView.setGrowHeight(true);
        colView.setChildren(tabsBox.getUI(), _browser);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()  { }

    /**
     * Returns whether given file is a project file.
     */
    protected boolean isProjectFile(WebFile aFile)
    {
        WebSite[] projSites = _podPane.getSites();
        WebSite fileSite = aFile.getSite();
        return ArrayUtils.containsId(projSites, fileSite);
    }

    /**
     * Called when Browser does prop change.
     */
    private void browserDidPropChange(PropChange aPC)
    {
        // Handle Activity, Status, Loading
        String propName = aPC.getPropName();
        if (propName == WebBrowser.Activity_Prop || propName == WebBrowser.Loading_Prop || propName == WebBrowser.Status_Prop)
            _podPane.resetLater();
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
            if (type.equals("java")) {
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

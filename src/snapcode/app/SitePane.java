package snapcode.app;
import snap.util.ListUtils;
import snap.view.Tab;
import snapcode.apptools.VcsPane;
import snapcode.project.VersionControl;
//import snapcode.project.VersionControlGit;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.TabView;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.List;

/**
 * A class to manage UI aspects of a WebSite for app.
 */
public class SitePane extends WebPage {

    // The AppPane that owns this SitePane
    private AppPane  _appPane;

    // The WebSite
    private WebSite  _site;

    // The ConsolePane
    private AppConsole _consolePane = new AppConsole();

    // The ProjectPane
    private ProjectConfigPane _projPane;

    // The VersionControl pane
    private VcsPane _vcp;

    // The BuildPane
    private BuildPane _buildPane;

    // The HttpServerPane
    private HttpServerPane _httpServerPane;

    // The top level TabView
    private TabView  _tabView;

    // A PropChangeListener for Site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Creates a new SitePane for given site.
     */
    protected SitePane(WebSite aSite)
    {
        _site = aSite;
        _site.addFileChangeListener(_siteFileLsnr);

        // Set ProjectPane
        _projPane = new ProjectConfigPane(this);

        // Set BuildPane
        _buildPane = new BuildPane(this);

        // Set HttpServerPane
        _httpServerPane = new HttpServerPane(this);
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        return _appPane;
    }

    /**
     * Sets the AppPane.
     */
    protected void setAppPane(AppPane anAP)
    {
        _appPane = anAP;
        _consolePane._appPane = anAP;
        if (_projPane != null)
            _projPane.setAppPane(anAP);

        // Set VersionControlPane
        String urls = getRemoteURLString();
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _vcp = new VcsPane(this);
        _vcp.setAppPane(anAP);
    }

    /**
     * Returns the site.
     */
    public WebSite getSite()
    {
        return _site;
    }

    /**
     * Returns the ConsolePane.
     */
    public AppConsole getConsolePane()
    {
        return _consolePane;
    }

    /**
     * Returns the ProjectPane for this site.
     */
    public ProjectConfigPane getProjPane()
    {
        return _projPane;
    }

    /**
     * Returns the VersionControlPane.
     */
    public VcsPane getVersionControlPane()
    {
        return _vcp;
    }

    /**
     * Returns the HttpServerPane.
     */
    public HttpServerPane getHttpServerPane()
    {
        return _httpServerPane;
    }

    /**
     * Returns the HomePageURL.
     */
    public WebURL getHomePageURL()
    {
        if (_hpu == null) {
            String hpu = getHomePageURLString();
            if (hpu != null) _hpu = getSite().getURL(hpu);
        }
        return _hpu;
    }

    WebURL _hpu;

    /**
     * Sets the HomePageURL.
     */
    public void setHomePageURL(WebURL aURL)
    {
        setHomePageURLString(aURL != null ? aURL.getString() : null);
        _hpu = aURL;
    }

    /**
     * Returns the HomePageURL.String.
     */
    public String getHomePageURLString()
    {
        return _homePageURLS;
    }

    String _homePageURLS;

    /**
     * Sets the HomePageURL.
     */
    public void setHomePageURLString(String aURL)
    {
        _homePageURLS = aURL;
        _hpu = null;
    }

    /**
     * Returns the RemoteURL string.
     */
    public String getRemoteURLString()
    {
        return VersionControl.getRemoteURLString(_site);
    }

    /**
     * Sets the RemoteURL string.
     */
    public void setRemoteURLString(String urls)
    {
        // Sanity check
        if (_tabView == null) return;

        // Get VC tab
        List<Tab> tabs = _tabView.getTabBar().getTabs();
        Tab vcTab = ListUtils.findMatch(tabs, tab -> tab.getContentOwner() instanceof VcsPane);
        int vcIndex = vcTab.getIndex();

        // Deactivate Version control pane and re-open site
        _vcp.deactivate();
        VersionControl.setRemoteURLString(_site, urls);

        // Recreate VC and set in tab
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _vcp = new VcsPane(this);
        if (_appPane != null)
            _vcp.setAppPane(_appPane);
        vcTab.setContentOwner(_vcp);

        // Reopen site
        _vcp.openSite();

        // Reset UI
        _tabView.setSelIndex(-1);
        _tabView.setSelIndex(vcIndex);
    }

    /**
     * Returns whether to automatically build files when changes are detected.
     */
    public boolean isAutoBuild()
    {
        return _projPane != null && _projPane.isAutoBuild();
    }

    /**
     * Sets whether to automatically build files when changes are detected.
     */
    public void setAutoBuild(boolean aValue)
    {
        if (_projPane != null) _projPane.setAutoBuild(aValue);
    }

    /**
     * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean isAutoBuildEnabled()
    {
        return _projPane != null && _projPane.isAutoBuildEnabled();
    }

    /**
     * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean setAutoBuildEnabled(boolean aFlag)
    {
        return _projPane != null && _projPane.setAutoBuildEnabled(aFlag);
    }

    /**
     * Returns whether app should use visual SnapCode Java editor by default.
     */
    public boolean getUseSnapEditor()
    {
        return _useSnapEditor;
    }

    boolean _useSnapEditor;

    /**
     * Sets whether app should use visual SnapCode Java editor by default.
     */
    public void setUseSnapEditor(boolean aValue)
    {
        _useSnapEditor = aValue;
    }

    /**
     * Opens the Site.
     */
    public void openSite()
    {
        // Activate VersionControlPane
        if (_vcp != null)
            _vcp.openSite();

        // Activate ProjectPane
        if (_projPane != null)
            _projPane.openSite();
    }

    /**
     * Closes the site.
     */
    public void closeSite()
    {
        _site.removeFileChangeListener(_siteFileLsnr);
        _site.setProp(SitePane.class.getName(), null);
        _appPane = null;
        _site = null;
        _consolePane = null;
        _projPane = null;
        _vcp = null;

        _httpServerPane.stopServer();
        _httpServerPane = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        if (_projPane != null)
            _projPane.deleteProject(aView);
        else {
            try {
                _site.deleteSite();
            } catch (Exception e) {
                e.printStackTrace();
                DialogBox.showExceptionDialog(null, "Delete Site Failed", e);
            }
        }
    }

    /**
     * Builds the site (if site has project).
     */
    public void buildSite(boolean doAddFiles)
    {
        if (_projPane != null)
            _projPane.buildProjectLater(doAddFiles);
    }

    /**
     * Removes build files from site.
     */
    public void cleanSite()
    {
        if (_projPane != null)
            _projPane.cleanProject();
    }

    /**
     * Returns whether given file is a hidden file.
     */
    public boolean isHiddenFile(WebFile aFile)
    {
        if (aFile == _projPane.getProject().getBuildDir())
            return true;
        return aFile.getPath().startsWith("/.git");
    }

    /**
     * Called when a site file changes.
     */
    private void siteFileChanged(PropChange aPC)
    {
        // Get source and property name
        WebFile file = (WebFile) aPC.getSource();
        String pname = aPC.getPropName();

        // Handle Saved property: Call fileAdded or fileSaved
        if (pname == WebFile.Saved_Prop) {
            if ((Boolean) aPC.getNewValue())
                fileAdded(file);
            else fileRemoved(file);
        }

        // Handle ModifedTime property: Call file saved
        if (pname == WebFile.ModTime_Prop && file.getExists())
            fileSaved(file);
    }

    /**
     * Called when file added to project.
     */
    void fileAdded(WebFile aFile)
    {
        if (_projPane != null) _projPane.fileAdded(aFile);
        if (_vcp != null)
            _vcp.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    void fileRemoved(WebFile aFile)
    {
        if (_projPane != null) _projPane.fileRemoved(aFile);
        if (_vcp != null)
            _vcp.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    void fileSaved(WebFile aFile)
    {
        if (_projPane != null) _projPane.fileSaved(aFile);
        if (_vcp != null)
            _vcp.fileSaved(aFile);
    }

    /**
     * Initialize UI panel.
     */
    protected View createUI()
    {
        // Create TabView
        _tabView = new TabView();

        // Add ProjectPane
        ProjectConfigPane projPane = getProjPane();
        if (projPane != null)
            _tabView.addTab("Settings", projPane.getUI()); //tab.setTooltip(new Tooltip("Project Settings"));

        // Add VersionControlPane
        VcsPane vcp = getVersionControlPane();
        if (vcp != null) {
            //String title = vcp instanceof VcsPaneGit ? "Git" : "Versioning";
            String title = "Versioning";
            _tabView.addTab(title, vcp.getUI()); //tab.setTooltip(new Tooltip("Manage Remote Site"));
        }

        // Add console pane and return
        AppConsole consolePane = getConsolePane();
        _tabView.addTab("Console", consolePane.getUI());  //tab.setTooltip(new Tooltip("Console Text"));

        // Add BuildPane
        BuildPane buildPane = _buildPane;
        _tabView.addTab("Build Dir", buildPane.getUI());

        // Add HttpServerPane
        HttpServerPane httpServPane = _httpServerPane;
        _tabView.addTab("HTTP-Server", httpServPane.getUI());

        // Return
        return _tabView;
    }

    /**
     * Returns the site pane for a site.
     */
    public static SitePane get(WebSite aSite)
    {
        return get(aSite, false);
    }

    /**
     * Returns the site pane for a site.
     */
    public synchronized static SitePane get(WebSite aSite, boolean doCreate)
    {
        SitePane sp = (SitePane) aSite.getProp(SitePane.class.getName());
        if (sp == null && doCreate) aSite.setProp(SitePane.class.getName(), sp = new SitePane(aSite));
        return sp;
    }

    /**
     * A WebPage subclass for SitePane.
     */
    public static class SitePage extends WebPage {

        /**
         * Initialize UI panel.
         */
        protected View createUI()
        {
            SitePane sp = SitePane.get(getSite());
            return sp.getUI();
        }

        /**
         * Override to provide better title.
         */
        public String getTitle()
        {
            return getURL().getSite().getName() + " - Site Settings";
        }
    }

}
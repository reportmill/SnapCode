package snapcode.app;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.TabView;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A class to manage UI aspects of a WebSite for app.
 */
public class SitePane extends WebPage {

    // The AppPane that owns this SitePane
    private AppPane  _appPane;

    // The WebSite
    private WebSite  _site;

    // The ProjectPane
    private ProjectConfigPane _projPane;

    // The BuildPane
    private BuildPane _buildPane;

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
        if (_projPane != null)
            _projPane.setAppPane(anAP);
    }

    /**
     * Returns the site.
     */
    public WebSite getSite()
    {
        return _site;
    }

    /**
     * Returns the ProjectPane for this site.
     */
    public ProjectConfigPane getProjPane()
    {
        return _projPane;
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
     * Opens the Site.
     */
    public void openSite()
    {
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
        _projPane = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        if (_projPane != null)
            _projPane.deleteProject(aView);
        else {
            try { _site.deleteSite(); }
            catch (Exception e) {
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
        if (_projPane != null)
            _projPane.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    void fileRemoved(WebFile aFile)
    {
        if (_projPane != null)
            _projPane.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    void fileSaved(WebFile aFile)
    {
        if (_projPane != null)
            _projPane.fileSaved(aFile);
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

        // Add BuildPane
        BuildPane buildPane = _buildPane;
        _tabView.addTab("Build Dir", buildPane.getUI());

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
package snapcode.app;
import javakit.project.Project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.TabView;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectPane extends WebPage {

    // The WorkSpacePane that owns this ProjectPane
    private PodPane  _workSpacePane;

    // The Project
    private Project  _project;

    // The ProjectConfigPane
    private ProjectConfigPane  _configPane;

    // The BuildPane
    private BuildPane _buildPane;

    // The top level TabView
    private TabView  _tabView;

    // A PropChangeListener for Site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    protected ProjectPane(PodPane workSpacePane, Project aProject)
    {
        _workSpacePane = workSpacePane;
        _project = aProject;

        //
        WebSite projSite = aProject.getSite();
        projSite.addFileChangeListener(_siteFileLsnr);

        // Set ProjectConfigPane
        _configPane = new ProjectConfigPane(this);

        // Set BuildPane
        _buildPane = new BuildPane(this);

        // Set this ProjectPane as Site prop
        projSite.setProp(ProjectPane.class.getName(), this);
    }

    /**
     * Returns the WorkSpacePane.
     */
    public PodPane getWorkSpacePane()  { return _workSpacePane; }

    /**
     * Returns the project.
     */
    public Project getProject()  { return _project; }

    /**
     * Returns the site.
     */
    public WebSite getSite()  { return _project.getSite(); }

    /**
     * Returns the ProjectConfigPane for this site.
     */
    public ProjectConfigPane getProjPane()
    {
        return _configPane;
    }

    /**
     * Returns whether to automatically build files when changes are detected.
     */
    public boolean isAutoBuild()
    {
        return _configPane != null && _configPane.isAutoBuild();
    }

    /**
     * Sets whether to automatically build files when changes are detected.
     */
    public void setAutoBuild(boolean aValue)
    {
        if (_configPane != null) _configPane.setAutoBuild(aValue);
    }

    /**
     * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean isAutoBuildEnabled()
    {
        return _configPane != null && _configPane.isAutoBuildEnabled();
    }

    /**
     * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean setAutoBuildEnabled(boolean aFlag)
    {
        return _configPane != null && _configPane.setAutoBuildEnabled(aFlag);
    }

    /**
     * Opens the Site.
     */
    public void openSite()
    {
        // Activate ProjectConfigPane
        if (_configPane != null)
            _configPane.openSite();
    }

    /**
     * Closes the site.
     */
    public void closeSite()
    {
        WebSite projSite = _project.getSite();
        projSite.removeFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), null);
        _workSpacePane = null;
        _configPane = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        if (_configPane != null)
            _configPane.deleteProject(aView);

        else {
            try {
                WebSite projSite = _project.getSite();
                projSite.deleteSite();
            }
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
        if (_configPane != null)
            _configPane.buildProjectLater(doAddFiles);
    }

    /**
     * Removes build files from site.
     */
    public void cleanSite()
    {
        if (_configPane != null)
            _configPane.cleanProject();
    }

    /**
     * Returns whether given file is a hidden file.
     */
    public boolean isHiddenFile(WebFile aFile)
    {
        if (aFile == _configPane.getProject().getBuildDir())
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
        if (_configPane != null)
            _configPane.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    void fileRemoved(WebFile aFile)
    {
        if (_configPane != null)
            _configPane.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    void fileSaved(WebFile aFile)
    {
        if (_configPane != null)
            _configPane.fileSaved(aFile);
    }

    /**
     * Initialize UI panel.
     */
    protected View createUI()
    {
        // Create TabView
        _tabView = new TabView();

        // Add ProjectConfigPane
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
    public static ProjectPane get(WebSite aSite)
    {
        return (ProjectPane) aSite.getProp(ProjectPane.class.getName());
    }

    /**
     * A WebPage subclass for ProjectPane.
     */
    public static class SitePage extends WebPage {

        /**
         * Initialize UI panel.
         */
        protected View createUI()
        {
            ProjectPane sp = ProjectPane.get(getSite());
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
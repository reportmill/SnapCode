package snapcode.app;
import javakit.project.Project;
import snap.util.ArrayUtils;
import snap.view.ViewOwner;
import snap.viewx.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.apptools.DebugTool;
import snapcode.apptools.SearchPane;

/**
 * This class is the top level controller for an open project.
 */
public class ProjectPane extends ViewOwner {

    // The list of sites
    protected WebSite[]  _sites = new WebSite[0];

    // The project
    private Project  _proj;

    // The PagePane to display project files for editing
    protected PagePane  _pagePane;

    // The ProjectTool manager
    protected ProjectTools _projTools;

    /**
     * Constructor.
     */
    public ProjectPane()
    {
        super();

        // Create parts
        _pagePane = new PagePane(this);
        _projTools = new ProjectTools(this);
        _projTools.createTools();
    }

    /**
     * Returns the PagePane.
     */
    public PagePane getPagePane()  { return _pagePane; }

    /**
     * Returns the PagePane.Browser.
     */
    public WebBrowser getBrowser()  { return _pagePane.getBrowser(); }

    /**
     * Returns the ProjectTools helper.
     */
    public ProjectTools getProjectTools()  { return _projTools; }

    /**
     * Returns the debug tool.
     */
    public DebugTool getDebugTool()  { return _projTools.getDebugTool(); }

    /**
     * Returns the SearchPane.
     */
    public SearchPane getSearchTool()  { return _projTools.getSearchTool(); }

    /**
     * Returns the array of sites.
     */
    public WebSite[] getSites()  { return _sites; }

    /**
     * Returns the number of sites.
     */
    public int getSiteCount()  { return _sites.length; }

    /**
     * Returns the individual site at the given index.
     */
    public WebSite getSite(int anIndex)  { return _sites[anIndex]; }

    /**
     * Adds a site to sites list.
     */
    public void addSite(WebSite aSite)
    {
        // If site already added, just return
        if (ArrayUtils.contains(_sites, aSite)) return;

        // Add site
        _sites = ArrayUtils.add(_sites, aSite);

        // Reset UI
        resetLater();
    }

    /**
     * Removes a site from sites list.
     */
    public void removeSite(WebSite aSite)
    {
        // Remove site
        _sites = ArrayUtils.remove(_sites, aSite);

        // Reset UI
        resetLater();
    }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()  { return _sites[0]; }

    /**
     * Returns the selected project.
     */
    public Project getProject()
    {
        if (_proj != null) return _proj;
        return _proj = Project.getProjectForSite(getRootSite());
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _pagePane.getSelectedFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _pagePane.setSelectedFile(aFile);
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelSite()
    {
        WebFile file = getSelFile();
        WebSite site = file != null ? file.getSite() : null;
        if (!ArrayUtils.containsId(getSites(), site))
            site = getSite(0);
        return site;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        Project proj = getProject();
        return proj != null ? proj.getBuildDir() : null;
    }
}

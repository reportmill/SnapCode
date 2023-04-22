package snapcode.app;
import javakit.project.Project;
import javakit.project.Workspace;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.util.Settings;
import snapcode.webbrowser.ClientUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // The list of sites
    private List<WebSite>  _sites;

    // The selected site
    private WebSite  _selSite;

    // The shared instance
    private static WelcomePanel  _shared;

    /**
     * Returns the shared instance.
     */
    public static WelcomePanel getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new WelcomePanel();
    }

    /**
     * Shows the welcome panel.
     */
    public void showPanel()
    {
        getWindow().setVisible(true);
        resetLater();
    }

    /**
     * Hides the welcome panel.
     */
    public void hide()
    {
        // Hide window and stop animation
        getWindow().setVisible(false);

        // Write current list of sites, flush prefs
        writeSites();
        Prefs.getDefaultPrefs().flush();
    }

    /**
     * Returns the number of site.
     */
    public int getSiteCount()
    {
        return getSites().size();
    }

    /**
     * Returns the site at given index.
     */
    public WebSite getSite(int anIndex)
    {
        return getSites().get(anIndex);
    }

    /**
     * Returns the list of sites.
     */
    public List<WebSite> getSites()
    {
        if (_sites != null) return _sites;
        List<WebSite> sites = readSites();
        return _sites = sites;
    }

    /**
     * Adds a project site.
     */
    public void addSite(WebSite aSite)
    {
        List<WebSite> sites = getSites();
        int index = sites.size();
        sites.add(index, aSite);
        resetLater();
    }

    /**
     * Removes a given site from sites list.
     */
    public void removeSite(WebSite aSite)
    {
        int index = ListUtils.indexOfId(getSites(), aSite);
        if (index >= 0) {
            List<WebSite> sites = getSites();
            sites.remove(index);
            resetLater();
        }
    }

    /**
     * Returns a site for given URL or name, if available.
     */
    public WebSite getProjectSiteForName(String aName)
    {
        // Search for matching Site.URL.String
        List<WebSite> sites = getSites();
        WebSite projectSiteForName = ListUtils.findMatch(sites, site -> site.getURL().getString().equalsIgnoreCase(aName));

        // Search for matching Site.Name
        if (projectSiteForName == null)
            projectSiteForName = ListUtils.findMatch(sites, site -> site.getName().equalsIgnoreCase(aName));

        // Return
        return projectSiteForName;
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelSite()  { return _selSite; }

    /**
     * Sets the selected site.
     */
    public void setSelSite(WebSite aSite)
    {
        _selSite = aSite;
        resetLater();
    }

    /**
     * Returns the list of selected sites.
     */
    public WebSite[] getSelectedSites()
    {
        return _selSite != null ? new WebSite[] { _selSite } : new WebSite[0];
    }

    /**
     * Called to quit app.
     */
    public void quitApp()
    {
        hide();
        AppBase.getShared().quitApp();
    }

    /**
     * Reads sites from <SNAP_HOME>/UserLocal.settings.
     */
    protected List<WebSite> readSites()
    {
        // Get site names and create sites.
        _sites = new ArrayList<>();
        Settings settings = ClientUtils.getUserLocalSettings();
        List<String> projectUrls = settings.getList("Projects");
        if (projectUrls == null)
            projectUrls = settings.getList("SnapSites");
        if (projectUrls == null)
            return _sites;

        // Get site from string
        for (String projUrlStr : projectUrls) {
            if (!projUrlStr.contains(":"))
                projUrlStr = "local:/" + projUrlStr;
            WebURL projUrl = WebURL.getURL(projUrlStr);
            WebSite projSite = projUrl != null ?  projUrl.getAsSite() : null;
            if (projSite != null)
                _sites.add(projSite);
        }

        // Get Selected Sites
        List<String> selProjectUrls = settings.getList("SelectedSites", true);
        for (String projUrl : selProjectUrls) {
            WebSite projSite = getProjectSiteForName(projUrl);
            if (projSite != null)
                _selSite = projSite;
            break;
        }

        // Return
        return _sites;
    }

    /**
     * Saves sites to <SNAP_HOME>/UserLocal.settings.
     */
    protected void writeSites()
    {
        // Move selected sites to front of the list
        WebSite[] selectedSites = getSelectedSites();
        for (int i = 0; i < selectedSites.length; i++) {
            WebSite site = selectedSites[i];
            if (site != getSite(i) && ListUtils.removeId(_sites, site) >= 0)
                _sites.add(i, site);
        }

        // Put Site URLs
        List<String> urls = new ArrayList<>();
        for (WebSite site : getSites())
            urls.add(getSimpleURLString(site));
        ClientUtils.getUserLocalSettings().put("Projects", urls.size() > 0 ? urls : null);

        // Put SelectedSites URLs
        List<String> siteUrls = new ArrayList<>();
        for (WebSite site : getSelectedSites())
            siteUrls.add(getSimpleURLString(site));
        ClientUtils.getUserLocalSettings().put("SelectedSites", siteUrls.size() > 0 ? siteUrls : null);
        ClientUtils.saveUserLocalSettings();
    }

    /**
     * Returns the simple URL for a site (just the name if local).
     */
    private String getSimpleURLString(WebSite aSite)
    {
        return aSite.getURL().getScheme().equals("local") ? aSite.getName() : aSite.getURLString();
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Add WelcomePaneAnim node
        WelcomePanelAnim anim = new WelcomePanelAnim();
        getUI(ChildView.class).addChild(anim.getUI());
        anim.getUI().playAnimDeep();

        // Enable SitesTable MouseReleased
        TableView<WebSite> sitesTable = getView("SitesTable", TableView.class);
        sitesTable.setRowHeight(24);
        enableEvents(sitesTable, MouseRelease);

        // Set preferred size
        getUI().setPrefSize(400, 480);

        // Configure Window: Add WindowListener to indicate app should exit when close button clicked
        WindowView win = getWindow();
        win.setTitle("Welcome");
        win.setResizable(false);
        enableEvents(win, WinClose);
        getView("OpenButton", Button.class).setDefaultButton(true);
    }

    /**
     * Resets UI.
     */
    public void resetUI()
    {
        // Update SitesTable
        setViewItems("SitesTable", getSites());
        setViewSelItem("SitesTable", getSelSite());

        // Update OpenButton, RemoveButton
        setViewEnabled("OpenButton", getSelectedSites().length > 0);
        setViewEnabled("RemoveButton", getSelectedSites().length > 0);
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SitesTable double-click
        if (anEvent.equals("SitesTable")) {

            // Handle multi-click
            if (anEvent.getClickCount() > 1) {
                if (getView("OpenButton", Button.class).isEnabled()) {
                    hide();
                    openProjectSites();
                }
            }

            // Handle click
            else {
                Object selItem = getViewSelItem("SitesTable");
                if (selItem instanceof WebSite) {
                    WebSite site = (WebSite) selItem;
                    setSelSite(site);
                }
            }
        }

        // Handle NewButton
        if (anEvent.equals("NewButton"))
            createProjectSite();

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            if (ViewUtils.isAltDown()) {
                handleOpenButtonAlt();
                return;
            }
            hide();
            openProjectSites();
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton"))
            showRemoveSitePanel();

        // Handle QuitButton
        if (anEvent.equals("QuitButton"))
            quitApp();

        // Handle WinClosing
        if (anEvent.isWinClose())
            quitApp();
    }

    /**
     * Creates a WorkspacePane.
     */
    protected WorkspacePane createWorkspacePane()  { return  new WorkspacePaneX(); }

    /**
     * Creates a new project site.
     */
    protected void createProjectSite()
    {
        // Get name for new project/site
        DialogBox dialogBox = new DialogBox("New Project Panel");
        dialogBox.setMessage("Enter name of new project");
        String projName = dialogBox.showInputDialog(getUI(), "Untitled");
        if (projName == null)
            return;

        // If project site already exists for name, just return
        if (getProjectSiteForName(projName) != null) {
            setSelSite(getProjectSiteForName(projName));
            return;
        }

        // Create new site for name
        WebSite newProjSite = createProjectSiteForName(projName);
        setSelSite(newProjSite);
    }

    /**
     * Creates a new project site.
     */
    public WebSite createProjectSiteForName(String aName)
    {
        // Create project site for name
        String siteUrlStr = aName.indexOf(':') < 0 ? "local:/" + aName : aName;
        WebURL siteURL = WebURL.getURL(siteUrlStr);
        WebSite newSite = siteURL != null ? siteURL.getAsSite() : null;
        if (newSite == null)
            return null;

        // Add
        addSite(newSite);

        // Write sites
        writeSites();

        // Return
        return newSite;
    }

    /**
     * Opens selected sites.
     */
    public void openProjectSites()
    {
        // Create WorkspacePane and add selected sites
        WorkspacePane workspacePane = createWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();

        WebSite[] projectSites = getSelectedSites();
        for (WebSite site : projectSites)
            workspace.addProjectForSite(site);

        // Show WorkspacePane
        workspacePane.show();
    }

    /**
     * Shows the remove site panel.
     */
    public void showRemoveSitePanel()
    {
        // Get selected site (if null, just return)
        WebSite selSite = getSelSite();
        if (selSite == null)
            return;

        // Give the user a chance to bail (just return if canceled or closed)
        String msg = "Are you sure you want to remove the currently selected project?";
        DialogBox dialogBox = new DialogBox("Remove Project");
        dialogBox.setMessage(msg);
        if (!dialogBox.showConfirmDialog(getUI()))
            return;

        // Give the option to not delete resources (just return if closed)
        msg = "Also delete local project files and sandbox?";
        dialogBox = new DialogBox("Delete Local Project Files");
        dialogBox.setMessage(msg);
        boolean deleteLocal = dialogBox.showConfirmDialog(getUI());

        // If requested, delete site files and sandbox (if "local" site)
        if (deleteLocal) {

            // Handle local site
            String scheme = selSite.getURL().getScheme();
            if (scheme.equals("local")) {
                WorkspacePane workspacePane = createWorkspacePane();
                Workspace workspace = workspacePane.getWorkspace();
                Project proj = workspace.addProjectForSite(selSite);
                ProjectPane projPane = workspacePane.getProjectPaneForProject(proj);
                projPane.deleteSite(getUI());
            }
        }

        // Get site index and select next index
        int siteIndex = ListUtils.indexOfId(getSites(), selSite);

        // Remove site
        removeSite(selSite);

        // Reset SelectedSite
        int newSelIndex = Math.min(siteIndex, getSiteCount() - 1);
        WebSite newSelSite = newSelIndex >= 0 && newSelIndex < getSiteCount() ? getSite(newSelIndex) : null;
        setSelSite(newSelSite);
    }

    /**
     * Open a file viewer site.
     */
    void handleOpenButtonAlt()
    {
        // Show dialog to get project name
        String defaultDirPath = Prefs.getDefaultPrefs().getString("SnapFileViewerPath", System.getProperty("user.home"));
        DialogBox dialogBox = new DialogBox("Open File Viewer");
        dialogBox.setQuestionMessage("Enter path:");
        String projectPath = dialogBox.showInputDialog(getUI(), defaultDirPath);
        if (projectPath == null)
            return;

        // Get project site from project path
        WebURL projectURL = WebURL.getURL(projectPath);
        WebSite projectSite = projectURL != null && projectURL.getFile() != null ? projectURL.getAsSite() : null;
        if (projectSite == null)
            return;

        // Create Workspace for site and show
        WorkspacePane workspacePane = createWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();
        workspace.addProjectForSite(projectSite);
        workspacePane.show();

        // Update prefs default path
        Prefs.getDefaultPrefs().setValue("SnapFileViewerPath", projectPath);

        // Hide welcome panel
        hide();
    }

    /**
     * A viewer owner to load/view WelcomePanel animation from WelcomePanelAnim.snp.
     */
    private static class WelcomePanelAnim extends ViewOwner {

        /**
         * Initialize some fields.
         */
        protected void initUI()
        {
            setViewText("BuildText", "Build: " + SnapUtils.getBuildInfo());
            setViewText("JVMText", "JVM: " + System.getProperty("java.runtime.version"));
            DocView doc = getUI(DocView.class);
            PageView page = doc.getPage();
            page.setEffect(null);
            page.setBorder(null);
        }
    }
}
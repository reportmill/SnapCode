package snapcode.app;
import snap.web.WebURL;

/**
 * This WorkspacePane subclass enables full JRE dev features.
 */
public class WorkspacePaneX extends WorkspacePane {

    // The default HomePage
    private HomePage  _homePage;

    /**
     * Creates the WorkspaceTools.
     */
    protected WorkspaceTools createWorkspaceTools()  { return new WorkspaceToolsX(this); }

    /**
     * Returns the HomePage URL.
     */
    @Override
    public WebURL getHomePageURL()  { return getHomePage().getURL(); }

    /**
     * Returns the HomePage.
     */
    private HomePage getHomePage()
    {
        if (_homePage != null) return _homePage;
        _homePage = new HomePage();
        _pagePane.setPageForURL(_homePage.getURL(), _homePage);
        return _homePage;
    }
}

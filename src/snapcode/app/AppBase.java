package snapcode.app;

import snap.web.WebSite;

/**
 * Base class for app.
 */
public abstract class AppBase {

    // The shared instance
    private static AppBase  _shared;

    /**
     * Constructor.
     */
    public AppBase()
    {
        _shared = this;
    }

    /**
     * Called to show WelcomePanel.
     */
    public abstract void showWelcomePanel();

    /**
     * Returns a project site for given name.
     */
    public WebSite getProjectSiteForName(String aName)  { return null; }

    /**
     * Creates a project site for given name.
     */
    public WebSite createProjectSiteForName(String aName)  { return null; }

    /**
     * Called when app quits.
     */
    public abstract void quitApp();

    /**
     * Returns the shared instance.
     */
    public static AppBase getShared()  { return _shared; }
}

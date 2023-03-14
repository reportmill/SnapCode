package snapcode.app;
import snap.util.Prefs;
import snap.view.ViewUtils;
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
     * Calls showWelcomePane after delay.
     */
    public static void showWelcomePanelLater()
    {
        Prefs.getDefaultPrefs().flush();
        ViewUtils.runLater(() -> getShared().showWelcomePanel());
    }

    /**
     * Returns the shared instance.
     */
    public static AppBase getShared()  { return _shared; }
}

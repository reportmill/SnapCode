package snapcode.app;

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
     * Called when app quits.
     */
    public abstract void quitApp();

    /**
     * Returns the shared instance.
     */
    public static AppBase getShared()  { return _shared; }
}

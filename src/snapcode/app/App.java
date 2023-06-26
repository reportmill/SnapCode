package snapcode.app;
import snap.gfx.GFXEnv;
import snap.util.Prefs;
import snap.view.ViewTheme;
import snap.view.ViewUtils;
import snap.view.WindowView;

/**
 * Main App class for SnapCode.
 */
public class App {

    // The shared instance
    private static App _shared;

    // Launch args
    public static String[] APP_ARGS;

    /**
     * Standard main implementation.
     */
    public static void main(final String[] args)
    {
        APP_ARGS = args;
        ViewUtils.runLater(() -> new App());
    }

    /**
     * Constructor.
     */
    public App()
    {
        super();
        _shared = this;

        // Set Prefs Root
        Prefs prefs = Prefs.getPrefsForName("SnapCode");
        Prefs.setDefaultPrefs(prefs);

        // Set UI Theme
        ViewTheme.setThemeForName("Light");

        // Show WelcomePanel
        showWelcomePanel();
    }

    /**
     * Show WelcomePanel.
     */
    public void showWelcomePanel()
    {
        WelcomePanel.getShared().showPanel();
        ViewUtils.runLater(() -> handleAppArgs());
    }

    /**
     * Exits the application.
     */
    public void quitApp()
    {
        // Hide open WorkspacePane
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null)
            workspacePane.hide();

        // Flush prefs and exit
        Prefs.getDefaultPrefs().flush();
        GFXEnv.getEnv().exit(0);
    }

    /**
     * Called to process app args
     */
    private void handleAppArgs()
    {
        // Get AppArgs - just return if none
        String[] appArgs = App.APP_ARGS;
        if (appArgs == null)
            return;

        // Handle args
        for (String appArg : appArgs) {

            // Handle Base64
            if (appArg.startsWith("#"))
                WelcomePanel.getShared().openBase64String(appArg);
        }
    }

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
    public static App getShared()  { return _shared; }
}

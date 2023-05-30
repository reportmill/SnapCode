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

    /**
     * Standard main implementation.
     */
    public static void main(final String[] args)
    {
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

package snapcode.appjr;
import snap.gfx.GFXEnv;
import snap.util.Prefs;
import snap.view.ViewTheme;

/**
 * Main App class for SnapCode.
 */
public class App {

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        // Set Prefs Root
        Prefs prefs = Prefs.getPrefsForName("SnapCode");
        Prefs.setDefaultPrefs(prefs);

        // Set UI Theme
        ViewTheme.setThemeForName("Light");

        // Show WelcomePanel
        WelcomePanel.getShared().setOnQuit(() -> quitApp());
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Exits the application.
     */
    public static void quitApp()
    {
        Prefs.getDefaultPrefs().flush();
        GFXEnv.getEnv().exit(0);
    }
}

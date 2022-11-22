package snapcode.app;
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
        ViewTheme.setThemeForName("Light");
        WelcomePanel.getShared().setOnQuit(() -> quitApp());
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Exits the application.
     */
    public static void quitApp()
    {
        Prefs.get().flush();
        GFXEnv.getEnv().exit(0);
    }
}

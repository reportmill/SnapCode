package snapcode.appjr;
import snap.util.Prefs;
import snap.view.ViewTheme;
import snap.view.WindowView;
import snapcode.app.AppBase;
import snapcode.app.WorkspacePane;
import javax.swing.*;

/**
 * Main App class for SnapCode.
 */
public class App extends AppBase {

    /**
     * Standard main implementation.
     */
    public static void main(final String[] args)
    {
        SwingUtilities.invokeLater(() -> new App());
    }

    /**
     * Constructor.
     */
    public App()
    {
        super();

        // Set Prefs Root
        Prefs prefs = Prefs.getPrefsForName("SnapCode");
        Prefs.setDefaultPrefs(prefs);

        // Set UI Theme
        ViewTheme.setThemeForName("Light");

        // Show WelcomePanel
        showWelcomePanel();
    }

    /**
     * Override to show WelcomePanel.
     */
    @Override
    public void showWelcomePanel()
    {
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Exits the application.
     */
    @Override
    public void quitApp()
    {
        // Hide open WorkspacePane
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null)
            workspacePane.hide();

        // Flush prefs and exit
        Prefs.getDefaultPrefs().flush();
        System.exit(0);
    }
}

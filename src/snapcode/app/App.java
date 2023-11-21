package snapcode.app;
import snap.gfx.GFXEnv;
import snap.util.Prefs;
import snap.view.ViewTheme;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.ExceptionReporter;
import snapcode.apptools.RunTool;
import snapcode.util.LZString;

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

        // Install Exception reporter
        ExceptionReporter er = new ExceptionReporter("SnapCode");
        er.setToAddress("support@reportmill.com");
        String version = "2023.10", buildDate = "Oct 16, 2023";
        er.setInfo("SnapCode Version " + version + ", Build Date: " + buildDate);
        Thread.setDefaultUncaughtExceptionHandler(er);

        // Set UI Theme
        ViewTheme.setThemeForName("Light");

        // If Java string set, open Java string
        if (handleAppArgs())
            return;

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
     * Called to handle args - returns true if args started the app.
     */
    private boolean handleAppArgs()
    {
        boolean handled = false;

        // Iterate over app args
        for (String arg : APP_ARGS)
            if (handleAppArg(arg))
                handled = true;

        // Return
        return handled;
    }

    /**
     * Called to handle args - returns true if args started the app.
     */
    private boolean handleAppArg(String arg0)
    {
        // Handle 'Java:...' or "Jepl:...': Open Java String
        if (arg0.startsWith("Java:") || arg0.startsWith("Jepl:")) {
            openJavaString(arg0);
            ViewUtils.runDelayed(() -> autoRunOpenFile(), 2000);
            return true;
        }

        // Handle 'New'
        if (arg0.equalsIgnoreCase("new")) {
            WelcomePanel.getShared().newFile(false);
            return true;
        }

        // Handle 'Samples'
        if (arg0.equalsIgnoreCase("samples")) {
            WelcomePanel.getShared().newFile(true);
            return true;
        }

        // Handle 'Play'
        if (arg0.equals("autorun")) {
            ViewUtils.runDelayed(() -> autoRunOpenFile(), 800);
            return false;
        }

        // Return not handled
        return false;
    }

    /**
     * Called to open Java or Jepl string.
     */
    private void openJavaString(String aString)
    {
        // Decompress string
        boolean isJepl = aString.startsWith("Jepl:");
        String javaStrLZ = aString.substring("Java:".length());
        String javaStr = LZString.decompressFromEncodedURIComponent(javaStrLZ);

        // Open Java/Jepl string
        WelcomePanel.getShared().openJavaString(javaStr, isJepl);
    }

    /**
     * Called to auto run open workspace file.
     */
    private void autoRunOpenFile()
    {
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null) {
            RunTool runTool = workspacePane.getWorkspaceTools().getToolForClass(RunTool.class);
            runTool.runApp();
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

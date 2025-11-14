package snapcode.app;
import snap.gfx.GFXEnv;
import snap.util.*;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.DevPaneExceptions;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.HelpTool;
import snapcode.apptools.RunTool;
import snapcode.util.LZString;
import java.io.File;

/**
 * Main App class for SnapCode.
 */
public class App {

    // The shared instance
    private static App _shared;

    // Launch args
    public static String[] APP_ARGS;

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
        DevPaneExceptions.setAppName("SnapCode");
        String version = SnapCodeUtils.getBuildVersion(); //  e.g.: 2025.08
        String buildDate = SnapCodeUtils.getBuildInfo(); // e.g.: "Jan 16, 2025";
        DevPaneExceptions.setAppInfo("SnapCode Version " + version + ", Build Date: " + buildDate);
        DevPaneExceptions.setDefaultUncaughtExceptionHandler();

        // If Java string set, open Java string
        if (handleAppArgs())
            return;

        // Show default workspace - Was WelcomePanel.getShared().showPanel();
        WorkspacePaneUtils.openDefaultWorkspace();

        // Hack - delete temp files
        ViewUtils.runDelayed(App::deleteTempFiles, 1000);
    }

    /**
     * Exits the application.
     */
    public void quitApp()
    {
        // Hide open WorkspacePane
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null)
            workspacePane.closeWorkspacePane();

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
        if (arg0.startsWith("Java:") || arg0.startsWith("Jepl:") || arg0.startsWith("JMD:")) {
            openJavaString(arg0);
            ViewUtils.runDelayed(this::autoRunOpenFile, 2000);
            return true;
        }

        // Handle 'New'
        if (arg0.equalsIgnoreCase("new")) {
            WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
            ViewUtils.runLater(() -> workspacePane.getNewFileTool().createFileForType("jepl"));
            return true;
        }

        // Handle 'open:'
        if (arg0.startsWith("open:")) {
            WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
            String openUrlAddr = arg0.substring("open:".length());
            WebURL openUrl = WebURL.getUrl(openUrlAddr);
            if (openUrl != null) {
                WorkspacePaneUtils.openFileUrl(workspacePane, openUrl);
                return true;
            }
        }

        // Handle 'snapcloud:' or 'sc:'
        if (arg0.startsWith("snapcloud:") || arg0.startsWith("sc:")) {
            WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
            WebURL openUrl = WebURL.getUrl(arg0);
            if (openUrl != null) {
                WorkspacePaneUtils.openFileUrl(workspacePane, openUrl);
                return true;
            }
        }

        // Handle 'Sample:'
        if (arg0.startsWith("sample:")) {
            WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
            String sampleName = arg0.substring("sample:".length());
            ViewUtils.runLater(() -> WorkspacePaneUtils.openSampleForName(workspacePane, sampleName));
            return true;
        }

        // Handle 'greenfoot:'
        if (arg0.startsWith("greenfoot:")) {
            WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
            String scenarioIdStr = arg0.substring("greenfoot:".length());
            int scenarioId = Convert.intValue(scenarioIdStr);
            GreenImport.openProjectForGreenfootScenarioId(null, scenarioId);
            return true;
        }

        // Handle 'Play'
        if (arg0.equals("autorun")) {
            ViewUtils.runDelayed(this::autoRunOpenFile, 800);
            return false;
        }

        // Handle 'lesson'
        if (arg0.startsWith("lesson:")) {
            System.out.println("Open Lesson: " + arg0.substring("lesson:".length()));
            HelpTool.setDefaultHelpFileSource(arg0.substring("lesson:".length()));
            return false;
        }

        // Handle 'embed'
        if (arg0.equals("embed")) {
            WorkspacePaneUtils.openEmbedWorkspace(null, null);
            return true;
        }

        // Handle 'embed'
        if (arg0.startsWith("embed")) {
            openEmbedWorkspace(arg0);
            return true;
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
        String fileType = aString.startsWith("Jepl:") ? "jepl" : aString.startsWith("JMD:") ? "jmd" : "java";
        String javaStrLZ = aString.substring(fileType.length() + 1);
        String javaStr = LZString.decompressFromEncodedURIComponent(javaStrLZ);

        // Open Java/Jepl string
        WorkspacePane workspacePane = new WorkspacePane(); workspacePane.show();
        workspacePane.getNewFileTool().newJavaFileForStringAndType(javaStr, fileType);

        // If Java markdown, hide project
        if (fileType.equals("jmd")) {
            workspacePane.getRunTool().getUI().setPrefWidth(650);
            ViewUtils.runLater(workspacePane.getProjectFilesTool()::hideTool);
        }
    }

    /**
     * Opens embed workspace.
     */
    private void openEmbedWorkspace(String aString)
    {
        // Handle no code provided
        if (aString.equals("embed")) {
            WorkspacePaneUtils.openEmbedWorkspace(null, null); return; }

        // Get LZW string
        String lzwStr = aString.substring("embed:".length());

        // Extract java string from arg
        String fileType = lzwStr.startsWith("Java:") ? "java" : aString.startsWith("JMD:") ? "jmd" : "jepl";
        String javaStr = LZString.decompressFromEncodedURIComponent(lzwStr);

        // Open java string
        WorkspacePaneUtils.openEmbedWorkspace(javaStr, fileType);

        // Auto-run new file
        ViewUtils.runDelayed(this::autoRunOpenFile, 200);
    }

    /**
     * Called to auto run open workspace file.
     */
    private void autoRunOpenFile()
    {
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null) {
            RunTool runTool = workspacePane.getRunTool();
            runTool.runAppForSelFile(false);
        }
    }

    /**
     * Deletes temp files and demo sandboxes.
     */
    private static void deleteTempFiles()
    {
        if (!SnapEnv.isWebVM) return;

        // Delete temp files (that are more than 10 seconds old)
        WebFile tempDir = WebFile.getFileForPath(SnapUtils.getTempDir());
        if (tempDir != null) {
            for (WebFile tempFile : tempDir.getFiles()) {
                if (tempFile.getLastModTime() < System.currentTimeMillis() - 10000)
                    tempFile.delete();
            }
        }

        // Delete weird CJ 'imageio23452345.tmp' files
        File rootFile = new File("/files");
        File[] rootFiles = rootFile.listFiles(); if (rootFiles == null) return;
        for (File file : rootFiles)
            if (file.getName().startsWith("imageio"))
                if (!file.delete()) return;
    }

    /**
     * Returns the shared instance.
     */
    public static App getShared()  { return _shared; }

    /**
     * Standard main implementation.
     */
    public static void main(final String[] args)
    {
        APP_ARGS = args;
        ViewUtils.runLater(App::new);
    }
}

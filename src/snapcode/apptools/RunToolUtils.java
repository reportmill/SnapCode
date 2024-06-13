package snapcode.apptools;
import snap.util.SnapUtils;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.debug.*;
import snapcode.project.*;

/**
 * Utility methods for RunTool.
 */
public class RunToolUtils {

    /**
     * Returns whether given file is a java file with main() (or jepl).
     */
    public static boolean isJavaFileWithMain(WebFile aFile)
    {
        String fileType = aFile.getFileType();
        if (fileType.equals("jepl"))
            return true;
        if (!fileType.equals("java"))
            return false;
        String fileText = aFile.getText();
        return fileText.contains(" main(String");
    }

    /**
     * Creates a run app for given config and debug option.
     */
    public static RunApp createRunAppForConfig(RunTool runTool, RunConfig runConfig, boolean isDebug)
    {
        // If RunConfig is missing or invalid, just return
        if (runConfig == null || !runConfig.isRunnable())
            return null;

        // Handle debug
        if (isDebug) {

            // If in browser, complain and run normal
            if (SnapUtils.isWebVM) {
                String msg = "Debug support for browser is not currently\n available,but is coming soon.\nExecuting normal run instead";
                DialogBox.showWarningDialog(runTool.getWorkspacePane().getUI(), "Debug Support Coming Soon", msg);
                return createRunAppForConfig(runTool, runConfig, false);
            }

            // Return new debug app
            return new DebugApp(runTool, runConfig);
        }

        // Run local if (1) TempProj and (2) jepl file and (3) not swing and (4) not alt-key-down
        WebFile mainJavaFile = runConfig.getMainJavaFile();
        boolean runLocal = runLocal(mainJavaFile);
        if (runLocal)
            return new RunAppSrc(runTool, runConfig);

        // Handle web: Create and return RunAppWeb for browser launch
        if (SnapUtils.isWebVM)
            return new RunAppWeb(runTool, runConfig);

        // Create and return RunAppBin for desktop
        return new RunAppBin(runTool, runConfig);
    }

    /**
     * Returns whether to run local.
     */
    public static boolean runLocal(WebFile mainFile)
    {
        if (mainFile == null || !mainFile.getFileType().equals("jepl"))
            return false;
        Project proj = Project.getProjectForFile(mainFile);
        if (!proj.getName().equals("TempProj"))
            return false;

        boolean isSwing = mainFile.getText().contains("javax.swing");
        return !isSwing && !ViewUtils.isControlDown() && proj.getBuildFile().getDependencies().length == 0;
    }

    /**
     * Run application.
     */
    public static void runConfigForName(RunTool runTool, String configName, boolean withDebug)
    {
        WebSite rootSite = runTool.getRootSite();
        RunConfigs runConfigs = RunConfigs.get(rootSite);
        RunConfig runConfig = runConfigs.getRunConfig(configName);
        if (runConfig != null) {
            runConfigs.getRunConfigs().remove(runConfig);
            runConfigs.getRunConfigs().add(0, runConfig);
            runConfigs.writeFile();
            runTool.runAppForConfig(runConfig, withDebug);
        }
    }
}

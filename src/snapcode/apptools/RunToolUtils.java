package snapcode.apptools;
import snap.util.SnapEnv;
import snap.view.ViewUtils;
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
        if (fileType.equals("jepl") || fileType.equals("jmd"))
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
        // If RunConfig is missing, just return
        if (runConfig == null)
            return null;

        // Handle Debug: return DebugApp
        if (isDebug)
            return new DebugApp(runTool, runConfig);

        // Handle RunLocal: Return new RunAppSrc
        if (isRunLocal(runConfig.getMainJavaFile()))
            return new RunAppSrc(runTool, runConfig);

        // Handle web: Create and return RunAppWeb for browser launch
        if (SnapEnv.isWebVM)
            return new RunAppWeb(runTool, runConfig);

        // Create and return RunAppBin for desktop
        return new RunAppBin(runTool, runConfig);
    }

    /**
     * Returns whether to run local.
     */
    public static boolean isRunLocal(WebFile mainFile)
    {
        // Run local if (1) TempProj and (2) jepl file and (3) not swing and (4) not alt-key-down
        if (mainFile == null)
            return false;
        String fileType = mainFile.getFileType();
        if (fileType.equals("jmd"))
            return true;
        if (!fileType.equals("jepl"))
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
        RunConfigs runConfigs = RunConfigs.getRunConfigsForProjectSite(rootSite);
        RunConfig runConfig = runConfigs.getRunConfigForName(configName);
        if (runConfig != null) {
            runConfigs.getRunConfigs().remove(runConfig);
            runConfigs.getRunConfigs().add(0, runConfig);
            runConfigs.writeFile();
            runTool.runAppForConfig(runConfig, withDebug);
        }
    }
}

package snapcode.apptools;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.debug.*;
import snapcode.project.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        if (runConfig == null) return null;

        // Get main Java file and class file for config
        WebFile mainJavaFile = runConfig.getMainJavaFile();
        WebFile mainClassFile = runConfig.getMainClassFile();
        if (mainJavaFile == null || mainClassFile == null)
            return null;

        // Get args for given config or main file
        String[] runArgs = getRunArgsForConfig(runConfig, isDebug);

        // Handle debug
        if (isDebug) {

            // If in browser, complain and run normal
            if (SnapUtils.isWebVM) {
                String msg = "Debug support for browser is not currently\n available,but is coming soon.\nExecuting normal run instead";
                DialogBox.showWarningDialog(runTool.getWorkspacePane().getUI(), "Debug Support Coming Soon", msg);
                return createRunAppForConfig(runTool, runConfig, false);
            }

            // Create debug app
            DebugApp debugApp = new DebugApp(runTool, mainClassFile, runArgs);
            Workspace workspace = runTool.getWorkspace();
            Breakpoints breakpointsHpr = workspace.getBreakpoints();
            Breakpoint[] breakpoints = breakpointsHpr.getArray();
            for (Breakpoint breakpoint : breakpoints)
                debugApp.addBreakpoint(breakpoint);
            return debugApp;
        }

        // Run local if (1) TempProj and (2) jepl file and (3) not swing and (4) not alt-key-down
        boolean runLocal = runLocal(mainClassFile);
        if (runLocal) {
            Project proj = Project.getProjectForFile(mainClassFile);
            String className = proj.getClassNameForFile(mainClassFile);
            String[] args = { className };
            return new RunAppSrc(runTool, mainClassFile, args);
        }

        // Handle web: Create and return RunAppWeb for browser launch
        if (SnapUtils.isWebVM)
            return new RunAppWeb(runTool, mainClassFile, runArgs);

        // Create and return RunAppBin for desktop
        return new RunAppBin(runTool, mainClassFile, runArgs);
    }

    /**
     * Returns an array of args for given run config.
     */
    public static String[] getRunArgsForConfig(RunConfig runConfig, boolean isDebug)
    {
        // Get project (just complain and return if not found)
        Project project = runConfig.getProject();
        if (project == null) {
            System.err.println("RunTool: not project file: " + runConfig.getMainClassName());
            return null;
        }

        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = getJavaCmdPath();
            if (SnapUtils.isWebVM) {
                boolean isSnapKit = project.getBuildFile().isIncludeSnapKitRuntime();
                boolean isSnapKitDom = isSnapKit && !ViewUtils.isAltDown() && !runConfig.isSwing();
                if (isSnapKitDom)
                    javaCmdPath = "java-dom";
            }
            commands.add(javaCmdPath);
        }

        // Get Class path and add to list
        String[] classPaths = project.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = runConfig.getMainClassName();
        commands.add(className);

        // Add App Args
        String appArgs = runConfig.getAppArgs();
        if (appArgs != null && !appArgs.isEmpty())
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
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

    /**
     * Returns the path for the java command.
     */
    private static String getJavaCmdPath()
    {
        if (SnapUtils.isWebVM) return "java";
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
}

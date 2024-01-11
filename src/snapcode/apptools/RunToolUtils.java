package snapcode.apptools;
import snap.util.FilePathUtils;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.debug.DebugApp;
import snapcode.debug.RunApp;
import snapcode.debug.RunAppBin;
import snapcode.debug.RunAppSrc;
import snapcode.project.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for RunTool.
 */
public class RunToolUtils {

    // The last executed file
    private static WebFile _lastRunFile;

    /**
     * Creates a run app for given config and/or main file and debug option.
     */
    public static RunApp createRunAppForConfigOrFile(RunTool runTool, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get config (if no config or file provided, get default config)
        RunConfig runConfig = aConfig;
        if (runConfig == null && aFile == null) {
            WebSite rootSite = runTool.getRootSite();
            runConfig = RunConfigs.get(rootSite).getRunConfig();
        }

        // Get main file and args for given config or main file
        WebFile mainFile = RunToolUtils.getMainFileForConfigAndFile(runTool, runConfig, aFile);
        if (mainFile == null)
            return null;

        // Get args for given config or main file
        String[] runArgs = RunToolUtils.getRunArgsForConfigAndFile(runConfig, mainFile, isDebug);

        // Handle debug
        if (isDebug) {
            DebugApp debugApp = new DebugApp(runTool, mainFile, runArgs);
            Workspace workspace = runTool.getWorkspace();
            Breakpoints breakpointsHpr = workspace.getBreakpoints();
            Breakpoint[] breakpoints = breakpointsHpr.getArray();
            for (Breakpoint breakpoint : breakpoints)
                debugApp.addBreakpoint(breakpoint);
            return debugApp;
        }

        // Handle Run Remote: Create app
        if (ViewUtils.isAltDown())
            return new RunAppBin(runTool, mainFile, runArgs);

        // Handle run local
        Project proj = runTool.getProject();
        String className = proj.getClassNameForFile(mainFile);
        String[] args = { className };

        // Create app and run
        return new RunAppSrc(runTool, mainFile, args);
    }

    /**
     * Returns the main file for config and file.
     */
    public static WebFile getMainFileForConfigAndFile(RunTool runTool, RunConfig runConfig, WebFile aFile)
    {
        // Try to get main file from: (1) run config, (2) LastFile or (3) SelFile
        WebFile mainFile = aFile;
        if (mainFile == null && runConfig != null) {
            WebSite rootSite = runTool.getRootSite();
            String mainFilePath = runConfig.getMainFilePath();
            mainFile = rootSite.createFileForPath(mainFilePath, false);
        }
        if (mainFile == null)
            mainFile = _lastRunFile;
        if (mainFile == null)
            mainFile = runTool.getSelFile();

        // Try to replace file with project file
        Project proj = Project.getProjectForFile(mainFile);
        if (proj == null) {
            System.err.println("RunTool: not project file: " + mainFile);
            return null;
        }

        // Get class file for given file (should be JavaFile)
        ProjectFiles projectFiles = proj.getProjectFiles();
        WebFile classFile;
        if (mainFile.getType().equals("java"))
            classFile = projectFiles.getClassFileForJavaFile(mainFile);

            // Try generic way to get class file
        else classFile = projectFiles.getBuildFile(mainFile.getPath(), false, mainFile.isDir());

        // If ClassFile found, set run file
        if (classFile != null)
            mainFile = classFile;

        // Set last main file
        _lastRunFile = mainFile;

        // Return
        return mainFile;
    }

    /**
     * Returns an array of args for given config and file.
     */
    public static String[] getRunArgsForConfigAndFile(RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            commands.add(javaCmdPath);
        }

        // Try to replace file with project file
        Project proj = Project.getProjectForFile(aFile);
        if (proj == null) {
            System.err.println("RunTool: not project file: " + aFile);
            return null;
        }

        // Get Class path and add to list
        String[] classPaths = proj.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = proj.getClassNameForFile(aFile);
        commands.add(className);

        // Add App Args
        String appArgs = aConfig != null ? aConfig.getAppArgs() : null;
        if (appArgs != null && appArgs.length() > 0)
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
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
            runTool.runConfigOrFile(runConfig, null, withDebug);
        }
    }
}

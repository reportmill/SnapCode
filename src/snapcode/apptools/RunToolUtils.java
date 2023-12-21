package snapcode.apptools;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snapcode.project.Project;
import snapcode.project.RunConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for RunTool.
 */
public class RunToolUtils {

    /**
     * Returns an array of args for given config and file.
     */
    public static String[] getRunArgs(Project aProj, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            commands.add(javaCmdPath);
        }

        // Get Class path and add to list
        String[] classPaths = aProj.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = aProj.getClassNameForFile(aFile);
        commands.add(className);

        // Add App Args
        String appArgs = aConfig != null ? aConfig.getAppArgs() : null;
        if (appArgs != null && appArgs.length() > 0)
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
    }
}

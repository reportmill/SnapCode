package snapcode.project;
import snap.util.ArrayUtils;
import snap.web.WebFile;

/**
 * Some utility methods for Workspace.
 */
public class WorkspaceUtils {

    /**
     * Returns a good default file.
     */
    public static WebFile getGoodDefaultFile(Workspace aWorkspace)
    {
        // Get projects
        Project[] projects = aWorkspace.getProjects();
        if (projects.length == 0)
            return null;

        // Look for read me
        for (Project project : projects) {
            WebFile projRootDir = project.getSite().getRootDir();
            WebFile[] projRootFiles = projRootDir.getFiles();
            WebFile readMeFile = ArrayUtils.findMatch(projRootFiles, file -> file.getSimpleName().equalsIgnoreCase("readme"));
            if (readMeFile != null)
                return readMeFile;
        }

        // Look for first defined main class file
        for (Project project : projects) {
            String mainClassName = project.getBuildFile().getMainClassName();
            if (mainClassName != null) {
                WebFile mainClassFile = project.getJavaFileForClassName(mainClassName);
                if (mainClassFile != null)
                    return mainClassFile;
            }
        }

        // Return first source file
        for (Project project : projects) {
            WebFile srcDir = project.getSourceDir();
            if (srcDir.getFileCount() > 0) {
                WebFile srcFile = srcDir.getFiles()[0];
                return srcFile;
            }
        }

        // Return first project root dir
        return projects[0].getSite().getRootDir();
    }
}

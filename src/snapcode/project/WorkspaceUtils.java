package snapcode.project;
import javakit.resolver.JavaClass;
import snap.util.ListUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Some utility methods for Workspace.
 */
public class WorkspaceUtils {

    /**
     * Returns the dependent files for given class.
     */
    public static List<WebFile> getJavaFilesDependentOnJavaFile(WebFile javaFile)
    {
        Project javaFileProject = Project.getProjectForFile(javaFile);
        JavaClass javaClass = javaFileProject.getJavaClassForFile(javaFile);

        // Get list of project and projects dependent on project
        List<Project> dependentProjects = new ArrayList<>(javaFileProject.getProjects());
        dependentProjects.add(0, javaFileProject);

        // Find files in dependent projects that depend on class
        List<WebFile> dependentJavaFiles = new ArrayList<>();
        for (Project project : dependentProjects) {
            WebFile srcDir = project.getSourceDir();
            findJavaFilesDependentOnClass(srcDir, javaClass, dependentJavaFiles);
        }

        // Return
        return dependentJavaFiles;
    }

    /**
     * Searches given directory for files dependent on given class and adds to list.
     */
    private static void findJavaFilesDependentOnClass(WebFile searchFile, JavaClass javaClass, List<WebFile> dependentFiles)
    {
        // Handle dir: Recurse
        if (searchFile.isDir()) {
            List<WebFile> dirFiles = searchFile.getFiles();
            dirFiles.forEach(file -> findJavaFilesDependentOnClass(file, javaClass, dependentFiles));
        }

        // Handle Java file: If isDependentOnClass() add to list
        else if (searchFile.getFileType().equals("java") || searchFile.getFileType().equals("jepl")) {
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(searchFile);
            if (javaAgent.isDependentOnClass(javaClass))
                dependentFiles.add(searchFile);
        }
    }

    /**
     * Returns a good default file.
     */
    public static WebFile getGoodDefaultFile(Workspace aWorkspace)
    {
        // Get projects
        List<Project> projects = aWorkspace.getProjects();
        if (projects.isEmpty())
            return null;

        // Look for read me
        for (Project project : projects) {
            WebFile projRootDir = project.getSite().getRootDir();
            List<WebFile> projRootFiles = projRootDir.getFiles();
            WebFile readMeFile = ListUtils.findMatch(projRootFiles, file -> file.getSimpleName().equalsIgnoreCase("readme"));
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
            if (srcDir.getFileCount() > 0)
                return srcDir.getFiles().get(0);
        }

        // Return first project root dir
        return projects.get(0).getSite().getRootDir();
    }
}

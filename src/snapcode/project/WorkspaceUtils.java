package snapcode.project;
import javakit.resolver.JavaClass;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Some utility methods for Workspace.
 */
public class WorkspaceUtils {

    /**
     * Returns the dependent files for given class.
     */
    public static WebFile[] getJavaFilesDependentOnJavaFile(WebFile javaFile)
    {
        Project javaFileProject = Project.getProjectForFile(javaFile);
        JavaClass javaClass = javaFileProject.getJavaClassForFile(javaFile);

        // Get projects dependent on this project
        Project[] dependentProjects = getProjectsDependentOnProject(javaFileProject);
        List<WebFile> dependentJavaFiles = new ArrayList<>();

        // Iterate over projects and add files
        for (Project project : dependentProjects) {
            WebFile srcDir = project.getSourceDir();
            findJavaFilesDependentOnClass(javaClass, srcDir, dependentJavaFiles);
        }

        // Return
        return dependentJavaFiles.toArray(new WebFile[0]);
    }

    /**
     * Returns the workspace projects that are dependent on given project.
     */
    private static Project[] getProjectsDependentOnProject(Project aProject)
    {
        List<Project> dependentProjects = new ArrayList<>();
        Workspace workspace = aProject.getWorkspace();
        Project[] projects = workspace.getProjects();

        for (Project project : projects) {
            if (project == aProject || ArrayUtils.contains(project.getProjects(), aProject))
                dependentProjects.add(project);
        }

        // Return
        return dependentProjects.toArray(new Project[0]);
    }

    /**
     * Returns the dependent files for given class.
     */
    private static void findJavaFilesDependentOnClass(JavaClass javaClass, WebFile searchFile, List<WebFile> dependentFiles)
    {
        // Handle dir: Recurse
        if (searchFile.isDir()) {
            WebFile[] dirFiles = searchFile.getFiles();
            Stream.of(dirFiles).forEach(file -> findJavaFilesDependentOnClass(javaClass, file, dependentFiles));
        }

        // Handle Java file: If isDependentOnClass() add to list
        else if (searchFile.getType().equals("java") || searchFile.getType().equals("jepl")) {
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

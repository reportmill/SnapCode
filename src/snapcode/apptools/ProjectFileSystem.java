package snapcode.apptools;
import snap.util.ListUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages project files.
 */
public class ProjectFileSystem {

    // The shared project file system
    private static ProjectFileSystem _shared = new ProjectFileSystem();

    /**
     * Constructor.
     */
    public ProjectFileSystem()
    {
        super();
    }

    /**
     * Returns the list of child files for given file.
     */
    public List<ProjectFile> getChildFilesForFile(ProjectFile projectFile)
    {
        List<WebFile> childFiles = getRawChildFiles(projectFile);
        List<ProjectFile> projectFiles = ListUtils.mapNonNull(childFiles, file -> createProjectFile(projectFile, file));
        projectFiles.sort(ProjectFile::compareTo);
        return projectFiles;
    }

    /**
     * Returns the list of child files.
     */
    protected List<WebFile> getRawChildFiles(ProjectFile projectFile)
    {
        if (projectFile._type == ProjectFile.FileType.SOURCE_DIR)
            return getChildFilesForSourceDir(projectFile);
        return projectFile._file.getFiles();
    }

    /**
     * Creates children list from Project files.
     */
    private List<WebFile> getChildFilesForSourceDir(ProjectFile projectFile)
    {
        List<WebFile> childFiles = new ArrayList<>();

        // Iterate over source dir and add child packages and files
        for (WebFile child : projectFile._file.getFiles()) {
            if (child.isDir() && child.getFileType().isEmpty())
                findChildFilesForSourceDir(child, childFiles);
            else childFiles.add(child);
        }

        // Return
        return childFiles;
    }

    /**
     * Searches given package dir for files and adds to list.
     */
    private void findChildFilesForSourceDir(WebFile parentDir, List<WebFile> childFiles)
    {
        boolean hasNonPkgFile = false;

        // Iterate over package dir files
        for (WebFile child : parentDir.getFiles()) {
            if (child.isDir() && child.getFileType().isEmpty())
                findChildFilesForSourceDir(child, childFiles);
            else hasNonPkgFile = true;
        }

        if (hasNonPkgFile || parentDir.getFileCount() == 0)
            childFiles.add(parentDir);
    }

    /**
     * Creates a project file for real file.
     */
    private ProjectFile createProjectFile(ProjectFile parentFile, WebFile aFile)
    {
        // Get basic file info
        String name = aFile.getName();
        boolean dir = aFile.isDir();
        String type = aFile.getFileType();
        int typeLen = type.length();

        // Skip hidden files, child packages
        if (name.startsWith("."))
            return null;
        if (parentFile._type == ProjectFile.FileType.PACKAGE_DIR && dir && typeLen == 0)
            return null;

        // Create project file
        ProjectFile projectFile = new ProjectFile(parentFile, aFile);
        if (dir && parentFile._proj != null && aFile == parentFile._proj.getSourceDir() && !aFile.isRoot()) {
            projectFile._type = ProjectFile.FileType.SOURCE_DIR;
            projectFile._priority = 1;
        }
        else if (parentFile._type == ProjectFile.FileType.SOURCE_DIR && dir && typeLen == 0) {
            projectFile._type = ProjectFile.FileType.PACKAGE_DIR;
            projectFile._priority = -1;
        }

        // Set priorities for special files
        if (type.equals("java") || type.equals("snp"))
            projectFile._priority = 1;

        // Return
        return projectFile;
    }

    /**
     * Returns the default project file system.
     */
    public static ProjectFileSystem getDefaultProjectFileSystem()  { return _shared; }
}

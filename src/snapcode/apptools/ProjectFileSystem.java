package snapcode.apptools;
import snap.util.ListUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages project files.
 */
public abstract class ProjectFileSystem {

    // The RootFiles
    private List<ProjectFile> _rootFiles;

    // The shared project file system
    private static ProjectFileSystem _shared;

    /**
     * Constructor.
     */
    protected ProjectFileSystem()
    {
        super();
        _rootFiles = new ArrayList<>();
    }

    /**
     * Returns the root files.
     */
    public List<ProjectFile> getRootFiles()  { return _rootFiles; }

    /**
     * Resets the root files.
     */
    public void resetRootFiles()  { _rootFiles.clear(); }

    /**
     * Returns the list of child files for given file.
     */
    public abstract List<ProjectFile> getChildFilesForFile(ProjectFile projectFile);

    /**
     * Returns a project file for given WebFile.
     */
    public ProjectFile getProjectFileForFile(WebFile aFile)
    {
        // Handle null
        if (aFile == null) return null;

        // If root, search for file in RootFiles
        if (aFile.isRoot())
            return getProjectFileForRootFile(aFile);

        // Otherwise, getProjectFile for successive parents and search them for this file
        for (WebFile parentFile = aFile.getParent(); parentFile != null; parentFile = parentFile.getParent()) {

            // Get project file (just skip if not found)
            ProjectFile parentProjectFile = getProjectFileForFile(parentFile);
            if (parentProjectFile == null)
                continue;

            // Search parent file child files
            for (ProjectFile projectFile : parentProjectFile.getFiles())
                 if (aFile == projectFile.getFile())
                     return projectFile;
        }

        // Return not found
        return null;
    }

    /**
     * Returns a project file for given root WebFile.
     */
    public ProjectFile getProjectFileForRootFile(WebFile aFile)
    {
        // If file is known root file, just return
        List<ProjectFile> rootFiles = getRootFiles();
        ProjectFile rootFile = ListUtils.findMatch(rootFiles, file -> file.getFile() == aFile);
        if (rootFile != null)
            return rootFile;

        // Create project file, add to list and return
        rootFile = createProjectFile(null, aFile);
        _rootFiles.add(rootFile);
        return rootFile;
    }

    /**
     * Creates a project file for web file.
     */
    protected ProjectFile createProjectFile(ProjectFile parentFile, WebFile aFile)
    {
        // Skip hidden files
        if (aFile.getName().startsWith("."))
            return null;

        // Create project file
        return new ProjectFile(this, parentFile, aFile);
    }

    /**
     * Returns the default project file system.
     */
    public static ProjectFileSystem getDefaultProjectFileSystem()
    {
        if (_shared != null) return _shared;
        return _shared = new ProjectFileSystem.Default();
    }

    /**
     * This class is the default ProjectFileSystem implementation.
     */
    private static class Default extends ProjectFileSystem {

        /**
         * Constructor.
         */
        public Default()
        {
            super();
        }

        /**
         * Returns the list of child files for given file.
         */
        @Override
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
        @Override
        protected ProjectFile createProjectFile(ProjectFile parentFile, WebFile aFile)
        {
            // Do normal version
            ProjectFile projectFile = super.createProjectFile(parentFile, aFile);
            if (projectFile == null)
                return null;

            // If root file, just return project file
            if (parentFile == null)
                return projectFile;

            // Get basic file info
            boolean dir = aFile.isDir();
            String type = aFile.getFileType();
            int typeLen = type.length();

            // Skip hidden files, child packages
            if (parentFile._type == ProjectFile.FileType.PACKAGE_DIR && dir && typeLen == 0)
                return null;

            // Create project file
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
    }
}

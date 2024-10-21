package snapcode.project;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;

import java.util.ArrayList;
import java.util.List;

/**
 * A FileBuilder to build miscellaneous files.
 */
public class ResourceFileBuilder implements ProjectFileBuilder {

    // The site we work for
    private Project _proj;

    // A list of files to be built
    private List<WebFile> _buildFiles = new ArrayList<>();

    /**
     * Constructor for given Project.
     */
    public ResourceFileBuilder(Project aProject)
    {
        _proj = aProject;
    }

    /**
     * Returns whether file is build file.
     */
    @Override
    public boolean isNeedsBuild()
    {
        return !_buildFiles.isEmpty();
    }

    /**
     * Returns whether file is build file.
     */
    public boolean isBuildFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        return !filePath.equals("/Project.settings");
    }

    /**
     * Returns whether given file needs to be built.
     */
    public boolean isFileNeedsBuild(WebFile aFile)
    {
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile buildFile = projFiles.getBuildFileForPath(aFile.getPath());
        return buildFile == null || !buildFile.getExists() || buildFile.getLastModTime() < aFile.getLastModTime();
    }

    /**
     * Adds a compile file.
     */
    public void addBuildFile(WebFile aFile)
    {
        ListUtils.addUniqueId(_buildFiles, aFile);
    }

    /**
     * Remove a build file.
     */
    public void removeBuildFile(WebFile aFile)
    {
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile buildFile = projFiles.getBuildFileForPath(aFile.getPath());
        if (buildFile == null)
            return;

        // Delete
        try {
            if (buildFile.getExists())
                buildFile.delete();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles files.
     */
    public boolean buildFiles(TaskMonitor aTM)
    {
        // If no build files, just return
        if (_buildFiles.isEmpty()) return true;

        // Get build files array (and clear list)
        WebFile[] sourceFiles = _buildFiles.toArray(new WebFile[0]);
        _buildFiles.clear();

        // Copy file to build directory
        for (WebFile sourceFile : sourceFiles) {
            boolean success = buildFile(sourceFile);
            if (!success)
                return false;
        }

        // Return true
        return true;
    }

    /**
     * Builds a file.
     */
    public boolean buildFile(WebFile sourceFile)
    {
        // Get BuildFile
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile buildFile = projFiles.createBuildFileForPath(sourceFile.getPath(), sourceFile.isDir());
        if (sourceFile.isFile())
            buildFile.setBytes(sourceFile.getBytes());

        // Save file
        try {
            buildFile.save();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}

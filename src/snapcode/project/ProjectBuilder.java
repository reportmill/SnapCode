/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.Date;

/**
 * This class handles building for a project.
 */
public class ProjectBuilder {

    // The Project
    private Project  _proj;

    // The JavaFileBuilder
    private JavaFileBuilder  _javaFileBuilder;

    // The default file builder
    private ProjectFileBuilder  _defaultFileBuilder;

    // The last build date
    private Date  _buildDate;

    /**
     * Constructor.
     */
    public ProjectBuilder(Project aProject)
    {
        super();
        _proj = aProject;
        _javaFileBuilder = new JavaFileBuilder(aProject);
        _defaultFileBuilder = new ProjectFileBuilder.DefaultBuilder(aProject);

        // If Project.Workspace.UseRealCompiler, use JavaFileBuilderX
        Workspace workspace = aProject.getWorkspace();
        if (workspace.isUseRealCompiler())
            _javaFileBuilder = new JavaFileBuilderX(aProject);
    }

    /**
     * Returns the last build date.
     */
    public Date getBuildDate()  { return _buildDate; }

    /**
     * Returns whether project currently needs to be built.
     */
    public boolean isNeedsBuild()
    {
        return _javaFileBuilder.isNeedsBuild() || _defaultFileBuilder.isNeedsBuild();
    }

    /**
     * Builds the project.
     */
    public boolean buildProject(TaskMonitor taskMonitor)
    {
        // Build files
        boolean buildSuccess = _javaFileBuilder.buildFiles(taskMonitor);
        buildSuccess &= _defaultFileBuilder.buildFiles(taskMonitor);
        _buildDate = new Date();

        // Return build success
        return buildSuccess;
    }

    /**
     * Removes all build files from project.
     */
    public void cleanProject()
    {
        // If separate build directory, just delete it
        WebFile buildDir = _proj.getBuildDir();
        WebFile sourceDir = _proj.getSourceDir();
        WebFile projSiteDir = _proj.getSite().getRootDir();

        if (buildDir != sourceDir && buildDir != projSiteDir) {

            // Delete BuildDir
            try {
                if (buildDir.getExists())
                    buildDir.delete();
            }

            // Handle Exceptions
            catch (Exception e) { throw new RuntimeException(e); }
        }

        // Otherwise, remove all class files from build directory
        else removeBuildFiles(buildDir);
    }

    /**
     * Removes all build files from given directory.
     */
    private void removeBuildFiles(WebFile aDir)
    {
        // Get directory files
        WebFile[] dirFiles = aDir.getFiles();

        // Iterate over files and remove class files
        for (int i = dirFiles.length - 1; i >= 0; i--) {

            // Handle Class file
            WebFile file = dirFiles[i];
            if (file.getType().equals("class")) {
                try { file.delete(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
            // Handle Dir: Recurse
            else if (file.isDir())
                removeBuildFiles(file);
        }
    }

    /**
     * Returns whether given file is a build file.
     */
    public boolean isBuildFile(WebFile aFile)
    {
        ProjectFileBuilder builder = getFileBuilder(aFile);
        return builder != null;
    }

    /**
     * Returns the file builder for given file.
     */
    public ProjectFileBuilder getFileBuilder(WebFile aFile)
    {
        // If file not in source path, just return
        String filePath = aFile.getPath();
        WebFile sourceDir = _proj.getSourceDir();
        String sourcePath = sourceDir.getPath();
        boolean inSrcPath = sourcePath.equals("/") || filePath.startsWith(sourcePath + "/");
        if (!inSrcPath)
            return null;

        // If file already in build path, just return
        WebFile buildDir = _proj.getBuildDir();
        String buildPath = buildDir.getPath();
        boolean inBuildPath = filePath.startsWith(buildPath + "/") || filePath.equals(buildPath);
        if (inBuildPath)
            return null;

        // Return JavaFileBuilder, DefaultFileBuilder or null
        if (_javaFileBuilder.isBuildFile(aFile))
            return _javaFileBuilder;
        if (_defaultFileBuilder.isBuildFile(aFile))
            return _defaultFileBuilder;

        // Return null since nothing builds given file
        return null;
    }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()
    {
        WebFile sourceDir = _proj.getSourceDir();
        addBuildFile(sourceDir, true);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFile(WebFile aFile, boolean doForce)
    {
        // If file doesn't exist, just return
        //if (!aFile.getExists()) return;
        if (aFile.getName().startsWith(".")) return;

        // Handle directory
        if (aFile.isDir()) {

            // If build directory, just return (assuming build dir is in source dir)
            WebFile buildDir = _proj.getBuildDir();
            if (aFile == buildDir)
                return;

            for (WebFile file : aFile.getFiles())
                addBuildFile(file, doForce);
            return;
        }

        // Get FileBuilder
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder == null)
            return;

        // If file needs build, add to fileBuilder
        boolean needsBuild = fileBuilder.isFileNeedsBuild(aFile);
        if (doForce || needsBuild)
            fileBuilder.addBuildFile(aFile);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFileForce(WebFile aFile)
    {
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder == null)
            return;
        fileBuilder.addBuildFile(aFile);
    }

    /**
     * Removes a build file.
     */
    public void removeBuildFile(WebFile aFile)
    {
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder != null)
            fileBuilder.removeBuildFile(aFile);
    }
}

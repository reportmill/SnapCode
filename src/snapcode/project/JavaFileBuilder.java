/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.*;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilder implements ProjectFileBuilder {

    // The project we work for
    protected Project  _proj;

    // A list of files to be compiled
    protected Set<WebFile>  _buildFiles = Collections.synchronizedSet(new HashSet<>());

    // The final set of compiled files
    protected Set<WebFile> _compiledFiles;

    /**
     * Constructor for given Project.
     */
    public JavaFileBuilder(Project aProject)
    {
        super();
        _proj = aProject;
        _compiledFiles = new HashSet<>();
    }

    /**
     * Returns whether this builder has files to build.
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
        String type = aFile.getFileType();
        return type.equals("java") || type.equals("jepl");
    }

    /**
     * Returns whether given file needs to be built.
     */
    public boolean isFileNeedsBuild(WebFile aFile)  { return true; }

    /**
     * Adds a compile file.
     */
    public void addBuildFile(WebFile aFile)
    {
        _buildFiles.add(aFile);
    }

    /**
     * Remove a build file.
     */
    public void removeBuildFile(WebFile aFile)
    {
        _buildFiles.remove(aFile);
    }

    /**
     * Compiles files.
     */
    public boolean buildFiles(TaskMonitor taskMonitor)
    {
        // Clear compiled files
        _compiledFiles.clear();

        // If no build files, just return
        if (_buildFiles.isEmpty())
            return true;

        // Get files
        List<WebFile> javaFiles = new ArrayList<>(_buildFiles);
        _buildFiles.clear();

        // Do real build
        boolean buildSuccess = buildFilesImpl(taskMonitor, javaFiles);

        // Clear compiled files
        _compiledFiles.clear();

        // Return
        return buildSuccess;
    }

    /**
     * Compiles given java files and returns whether all were compiled successfully.
     */
    protected boolean buildFilesImpl(TaskMonitor aTaskMonitor, List<WebFile> javaFiles)
    {
        boolean buildFilesSuccess = true;

        // Iterate over files and build
        for (WebFile javaFile : javaFiles) {

            // Build file
            boolean buildFileSuccess = buildFile(javaFile);

            // If successful, add to CompiledFiles
            if (buildFileSuccess)
                _compiledFiles.add(javaFile);
            else buildFilesSuccess = false;
        }

        // Return
        return buildFilesSuccess;
    }

    /**
     * Builds a file.
     */
    protected boolean buildFile(WebFile javaFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
        javaAgent.checkFileForErrors();

        // Return
        BuildIssue[] buildIssues = javaAgent.getBuildIssues();
        return buildIssues.length == 0;
    }
}
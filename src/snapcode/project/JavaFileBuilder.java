/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.util.ArrayUtils;
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
        return _buildFiles.size() > 0;
    }

    /**
     * Returns whether file is build file.
     */
    public boolean isBuildFile(WebFile aFile)
    {
        String type = aFile.getType();
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
        if (_buildFiles.size() == 0)
            return true;

        // Get files
        List<WebFile> javaFiles = new ArrayList<>(_buildFiles);;
        _buildFiles.clear();

        // Do real build
        boolean buildSuccess = buildFilesImpl(taskMonitor, javaFiles);

        // Find unused imports
        findUnusedImports();
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
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        javaAgent.checkFileForErrors();

        // Return
        BuildIssue[] buildIssues = javaAgent.getBuildIssues();
        return buildIssues.length == 0;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()
    {
        Workspace workspace = _proj.getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();

        // Iterate over compiled files
        for (WebFile javaFile : _compiledFiles) {

            // Iterate over build issues
            BuildIssue[] unusedImportIssues = getUnusedImportBuildIssuesForFile(javaFile);
            for (BuildIssue buildIssue : unusedImportIssues)
                buildIssues.add(buildIssue);
        }
    }

    /**
     * Returns an array of unused imports for Java file.
     */
    protected BuildIssue[] getUnusedImportBuildIssuesForFile(WebFile javaFile)
    {
        // Get unused import decls
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        JFile jfile = javaAgent.getJFile();
        JImportDecl[] unusedImports = jfile.getUnusedImports();
        if (unusedImports.length == 0)
            return BuildIssues.NO_ISSUES;

        // Create BuildIssues for each and return
        return ArrayUtils.map(unusedImports, id -> createUnusedImportBuildIssue(javaFile, id), BuildIssue.class);
    }

    /**
     * Returns an "Unused Import" BuildIssue for given import decl.
     */
    private BuildIssue createUnusedImportBuildIssue(WebFile javaFile, JImportDecl importDecl)
    {
        String msg = "The import " + importDecl.getName() + " is never used";
        int lineIndex = importDecl.getLineIndex();
        int startCharIndex = importDecl.getStartCharIndex();
        int endCharIndex = importDecl.getEndCharIndex();
        return new BuildIssue().init(javaFile, BuildIssue.Kind.Warning, msg, lineIndex, 0, startCharIndex, endCharIndex);
    }
}
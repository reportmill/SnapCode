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

    // Whether current build has been interrupted
    protected boolean _interrupted;

    /**
     * Constructor for given Project.
     */
    public JavaFileBuilder(Project aProject)
    {
        _proj = aProject;
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
    public boolean getNeedsBuild(WebFile aFile)  { return true; }

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
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        // Empty case
        if (_buildFiles.size() == 0) return true;

        // Get files
        WebFile[] javaFiles = _buildFiles.toArray(new WebFile[0]);
        _buildFiles.clear();

        // Iterate over files and build
        boolean success = true;
        for (WebFile javaFile : javaFiles)
            success &= buildFile(javaFile);

        // Return
        return success;
    }

    /**
     * Builds a file.
     */
    protected boolean buildFile(WebFile javaFile)
    {
        // Get BuildIssues for parsed file
        BuildIssue[] buildIssues = getBuildIssuesForFile(javaFile);

        // Set build issues
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        javaAgent.setBuildIssues(buildIssues);

        // Return
        return buildIssues.length == 0;
    }

    /**
     * Interrupts build.
     */
    public void interruptBuild()
    {
        _interrupted = true;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()  { }

    /**
     * Returns an array of build issues for Java file.
     */
    protected BuildIssue[] getBuildIssuesForFile(WebFile javaFile)
    {
        // Get JavaAgent and JFile
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        JFile jFile = javaAgent.getJFile();
        List<NodeError> errorsList = new ArrayList<>();

        // Find errors in JFile
        findNodeErrors(jFile, errorsList);
        NodeError[] errors = errorsList.toArray(NodeError.NO_ERRORS);

        // Convert to BuildIssues and set in agent
        return ArrayUtils.map(errors, error -> BuildIssue.createIssueForNodeError(error, javaFile), BuildIssue.class);
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

    /**
     * Recurse into nodes
     */
    private static void findNodeErrors(JNode aNode, List<NodeError> theErrors)
    {
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(theErrors, errors);

        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findNodeErrors(child, theErrors);
    }
}
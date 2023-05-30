/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.parse.JFile;
import javakit.parse.JImportDecl;
import snap.util.ArrayUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilder implements ProjectFileBuilder {

    // The project we work for
    protected Project  _proj;

    // A list of files to be compiled
    protected Set<WebFile>  _buildFiles = Collections.synchronizedSet(new HashSet<>());

    // Whether to interrupt current build
    protected boolean  _interrupt;

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
    public boolean buildFiles(TaskMonitor aTaskMonitor)  { return true; }

    /**
     * Interrupts build.
     */
    public void interruptBuild()
    {
        _interrupt = true;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()  { }

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
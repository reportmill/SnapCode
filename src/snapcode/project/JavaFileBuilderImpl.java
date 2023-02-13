/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.*;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.*;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilderImpl extends JavaFileBuilder {

    // The project we work for
    private ProjectX  _projX;

    // The SnapCompiler used for last compiles
    private SnapCompiler  _compiler;

    // The final set of compiled files
    private Set<WebFile> _compiledFiles;

    // The final set of compiled files with errors
    private Set<WebFile>  _errorFiles;

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilderImpl(ProjectX aProject)
    {
        super(aProject);
        _projX = aProject;
    }

    /**
     * Returns whether given file needs to be built.
     */
    public boolean getNeedsBuild(WebFile aFile)
    {
        // See if Java file has out of date Class file
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile classFile = projectFiles.getClassFileForJavaFile(aFile);
        boolean needsBuild = !classFile.getExists() || classFile.getLastModTime() < aFile.getLastModTime();

        // If not out of date, updateDependencies, compatibilities
        if (!needsBuild) {
            JavaData javaData = JavaData.getJavaDataForFile(aFile);
            if (!javaData.isDependenciesSet()) {
                javaData.updateDependencies();
                needsBuild = true;
                //int c = updateCompatability(aFile); if(c<0) needsBuild=true; if(c!=-2) jdata.updateDependencies();
            }
        }

        // Return NeedsBuild
        return needsBuild;
    }

    /**
     * Remove a build file.
     */
    @Override
    public void removeBuildFile(WebFile aFile)
    {
        // Do normal version
        super.removeBuildFile(aFile);

        // Get dependent files and add to BuildFiles
        JavaData javaData = JavaData.getJavaDataForFile(aFile);
        Set<WebFile> dependents = javaData.getDependents();
        for (WebFile dependant : dependents)
            if (dependant.getExists())
                addBuildFile(dependant);

        // Remove JavaFile Dependencies
        javaData.removeDependencies();

        // Get JavaFile.ClassFiles and remove them
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile[] classFiles = projectFiles.getClassFilesForJavaFile(aFile);
        if (classFiles == null)
            return;

        // Iterate over class files and delete
        for (WebFile classFile : classFiles) {
            try { classFile.delete(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    /**
     * Compiles files.
     */
    @Override
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        // Empty case
        if (_buildFiles.size() == 0) return true;

        // Get files
        List<WebFile> sourceFiles = new ArrayList<>(_buildFiles);
        _buildFiles.clear();

        // Get Compiler and sets for compiled/error files
        SnapCompiler compiler = new SnapCompiler(_projX);
        Set<WebFile> compiledFiles = new HashSet<>();
        Set<WebFile> errorFiles = new HashSet<>();

        // Reset Interrupt flag
        _interrupt = false;

        // Iterate over build files and compile
        boolean compileSuccess = true; //long time = System.currentTimeMillis();
        for (int i = 0; i < sourceFiles.size(); i++) {
            WebFile sourceFile = sourceFiles.get(i);

            // If interrupted, add remaining build files and return
            if (_interrupt) {
                for (int j = i, jMax = sourceFiles.size(); j < jMax; j++)
                    addBuildFile(sourceFiles.get(j));
                return false;
            }

            // Update progress
            if (compiledFiles.contains(sourceFile))
                continue; //System.err.println("Skipping " + finfo);

            //
            ProjectFiles projectFiles = _proj.getProjectFiles();
            String className = projectFiles.getClassNameForFile(sourceFile);

            //
            int count = compiledFiles.size() + 1;
            String msg = String.format("Compiling %s (%d of %d)", className, count, sourceFiles.size());
            aTaskMonitor.beginTask(msg, -1);

            // Get compile file
            boolean result = compiler.compile(sourceFile);
            aTaskMonitor.endTask();

            // If compile failed, re-add file to BuildFiles and continue
            if (!result) {
                compiledFiles.add(sourceFile);
                errorFiles.add(sourceFile);
                addBuildFile(sourceFile);
                if (compiler._errorCount >= 1000) _interrupt = true;
                compileSuccess = false;
                continue;
            }

            // Add Compiler.CompiledFiles to CompiledFiles
            compiledFiles.addAll(compiler.getCompiledJavaFiles());

            // If there were modified files, clear Project.ClassLoader
            if (compiler.getModifiedJavaFiles().size() > 0) {
                WorkSpace workSpace = _projX.getWorkSpace();
                workSpace.clearClassLoader();
            }

            // Iterate over JavaFiles for modified ClassFiles and update
            for (WebFile jfile : compiler.getModifiedJavaFiles()) {

                // Delete class files for removed inner classes
                deleteZombieClassFiles(jfile);

                // Update dependencies and get files that need to be updated
                JavaData javaData = JavaData.getJavaDataForFile(jfile);
                boolean dependsChanged = javaData.updateDependencies();
                if (!dependsChanged)
                    continue;

                // Iterate over Java files dependent on loop JavaFile and mark for update
                Set<WebFile> updateFiles = javaData.getDependents();
                for (WebFile updateFile : updateFiles) {

                    //
                    ProjectX proj = ProjectX.getProjectForFile(updateFile);
                    if (proj == _proj) {
                        if (!compiledFiles.contains(updateFile)) {
                            if (!ListUtils.containsId(sourceFiles, updateFile))
                                sourceFiles.add(updateFile);
                        }
                    }

                    // Otherwise, add build file
                    else {
                        ProjectBuilder projectBuilder = proj.getProjectBuilder();
                        projectBuilder.addBuildFileForce(updateFile);
                    }
                }
            }
        }

        // Finalize TaskMonitor
        aTaskMonitor.beginTask("Build Completed", -1);
        aTaskMonitor.endTask();

        // Set compiler/files for findUnusedImports
        _compiler = compiler;
        _compiledFiles = compiledFiles;
        _errorFiles = errorFiles;

        // Finalize ActivityText and return
        //System.out.println("Build time: " + (System.currentTimeMillis()-time)/1000f + " seconds");
        return compileSuccess;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()
    {
        // Sanity check
        if (_compiler == null) return;

        // Iterate over compiled files
        for (WebFile javaFile : _compiledFiles) {

            // Just continue if file contains errors
            if (_errorFiles.contains(javaFile))
                continue;

            // Iterate over build issues
            BuildIssue[] unusedImportIssues = getUnusedImportBuildIssuesForFile(javaFile);
            for (BuildIssue buildIssue : unusedImportIssues)
                _compiler.report(buildIssue);
        }

        // Clear vars
        _compiler = null;
        _compiledFiles = _errorFiles = null;
    }

    /**
     * Delete inner-class class files that were generated in older version of class.
     */
    private void deleteZombieClassFiles(WebFile aJavaFile)
    {
        // Get all ClassFiles for JavaFile
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile[] classFiles = projFiles.getClassFilesForJavaFile(aJavaFile);
        if (classFiles == null)
            return;

        // Iterate over class files and delete if older than source file
        for (WebFile classFile : classFiles) {
            boolean classFileOlderThanSource = classFile.getLastModTime() < aJavaFile.getLastModTime();
            if (classFileOlderThanSource) {
                try { classFile.delete(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        }
    }
}
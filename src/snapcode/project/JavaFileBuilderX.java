/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.*;

/**
 * A FileBuilder to build Java files using JDK compiler.
 */
public class JavaFileBuilderX extends JavaFileBuilder {

    // The SnapCompiler used for last compiles
    private SnapCompiler  _compiler;

    // Whether to do source-hybrid compile
    private boolean _doSourceHybridCompile;

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilderX(Project aProject)
    {
        super(aProject);

        _doSourceHybridCompile = aProject.getBuildFile().isRunWithInterpreter();
    }

    /**
     * Returns whether given file needs to be built.
     */
    @Override
    public boolean isFileNeedsBuild(WebFile aFile)
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
    protected boolean buildFilesImpl(TaskMonitor aTaskMonitor, List<WebFile> sourceFiles)
    {
        // Create compiler
        _compiler = new SnapCompiler(_proj);
        boolean buildFilesSuccess = true;

        // Iterate over build files and compile
        for (int i = 0; i < sourceFiles.size(); i++) {

            // Get next source file - if already built, just skip
            WebFile sourceFile = sourceFiles.get(i);
            if (_compiledFiles.contains(sourceFile))
                continue;

            // If interrupted, add remaining build files and return
            if (_interrupted) {
                List<WebFile> remainingSourceFiles = sourceFiles.subList(i, sourceFiles.size());
                remainingSourceFiles.forEach(this::addBuildFile);
                return false;
            }

            // Add task manager task with message: "Compiling MyClass (X of MaxX)"
            String className = _proj.getProjectFiles().getClassNameForFile(sourceFile);
            String msg = String.format("Compiling %s (%d of %d)", className, i, sourceFiles.size());
            aTaskMonitor.beginTask(msg, -1);

            // Build file
            boolean buildFileSuccess = buildFile(sourceFile);

            // If successful compile Find dependencies for modified Java files and add to source files
            if (buildFileSuccess)
                findDependenciesForModifiedJavaFiles(sourceFiles);

            // Otherwise, mark build failure
            else buildFilesSuccess = false;

            // Stop task manager task
            aTaskMonitor.endTask();
        }

        // Finalize TaskMonitor
        aTaskMonitor.beginTask("Build Completed", -1);
        aTaskMonitor.endTask();

        // Return
        return buildFilesSuccess;
    }

    /**
     * Compiles given java file.
     */
    protected boolean buildFile(WebFile sourceFile)
    {
        // Compile file
        boolean compileSuccess = _compiler.compileFile(sourceFile);

        // If successful but doing source-hybrid compile, do super version to check source
        if (compileSuccess && _doSourceHybridCompile)
            compileSuccess = super.buildFile(sourceFile);

        // If compile failed, re-add to BuildFiles and return
        if (!compileSuccess) {
            addBuildFile(sourceFile);
            if (_compiler._errorCount >= 1000)
                _interrupted = true;
            return false;
        }

        // Add Compiler.CompiledFiles to CompiledFiles
        Set<WebFile> compiledJavaFiles = _compiler.getCompiledJavaFiles();
        _compiledFiles.addAll(compiledJavaFiles);

        // Return success
        return true;
    }

    /**
     * Find dependencies for modified Java files and add to source files.
     */
    private void findDependenciesForModifiedJavaFiles(List<WebFile> sourceFiles)
    {
        Set<WebFile> modifiedJavaFiles = _compiler.getModifiedJavaFiles();

        // Iterate over JavaFiles for modified ClassFiles and update
        for (WebFile modifiedJavaFile : modifiedJavaFiles) {

            // Update dependencies and get files that need to be updated
            JavaData javaData = JavaData.getJavaDataForFile(modifiedJavaFile);
            boolean dependsChanged = javaData.updateDependencies();
            if (!dependsChanged)
                continue;

            // Iterate over Java files dependent on loop JavaFile and mark for update
            Set<WebFile> updateFiles = javaData.getDependents();
            for (WebFile updateFile : updateFiles) {

                // Make sure updated file is in sourceFiles list
                Project proj = Project.getProjectForFile(updateFile);
                if (proj == _proj) {
                    if (!_compiledFiles.contains(updateFile))
                        ListUtils.addUniqueId(sourceFiles, updateFile);
                }

                // Otherwise, add build file
                else {
                    ProjectBuilder projectBuilder = proj.getBuilder();
                    projectBuilder.addBuildFileForce(updateFile);
                }
            }
        }
    }
}
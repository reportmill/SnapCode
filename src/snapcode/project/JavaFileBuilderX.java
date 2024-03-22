/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.resolver.JavaClass;
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

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilderX(Project aProject)
    {
        super(aProject);
    }

    /**
     * Returns whether given file needs to be built.
     */
    @Override
    public boolean isFileNeedsBuild(WebFile javaFile)
    {
        // See if Java file has out of date Class file
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile classFile = projectFiles.getClassFileForJavaFile(javaFile);
        return classFile == null || classFile.getLastModTime() < javaFile.getLastModTime();
    }

    /**
     * Remove a build file.
     */
    @Override
    public void removeBuildFile(WebFile javaFile)
    {
        // Do normal version
        super.removeBuildFile(javaFile);

        // Get JavaFile.ClassFiles and remove them
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile[] classFiles = projectFiles.getClassFilesForJavaFile(javaFile);

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
    protected boolean buildFilesImpl(TaskMonitor taskMonitor, List<WebFile> sourceFiles)
    {
        // Make sure build dir exists
        WebFile buildDir = _proj.getBuildDir();
        if (!buildDir.getExists())
            buildDir.save();

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
            if (taskMonitor.isCancelled()) {
                List<WebFile> remainingSourceFiles = sourceFiles.subList(i, sourceFiles.size());
                remainingSourceFiles.forEach(this::addBuildFile);
                return false;
            }

            // Add task manager task with message: "Compiling MyClass (X of MaxX)"
            String className = _proj.getProjectFiles().getClassNameForFile(sourceFile);
            String msg = String.format("Compiling %s (%d of %d)", className, i + 1, sourceFiles.size());
            taskMonitor.beginTask(msg, -1);

            // Build file
            boolean buildFileSuccess = buildFile(sourceFile);

            // If successful compile Find dependencies for modified Java files and add to source files
            if (buildFileSuccess)
                findDependenciesForModifiedJavaFiles(sourceFiles);

            // Otherwise, mark build failure
            else {
                buildFilesSuccess = false;
                if (_compiler._errorCount >= 1000)
                    taskMonitor.setCancelled(true);
            }

            // Stop task manager task
            taskMonitor.endTask();
        }

        // Return
        return buildFilesSuccess;
    }

    /**
     * Compiles given java file.
     */
    protected boolean buildFile(WebFile sourceFile)
    {
        // If Jepl file, check for errors first
        if (sourceFile.getType().equals("jepl")) {
            boolean checkErrorsSuccess = super.buildFile(sourceFile);
            if (!checkErrorsSuccess) {
                addBuildFile(sourceFile);
                return false;
            }
        }

        // Compile file
        boolean compileSuccess = _compiler.compileFile(sourceFile);

        // If successful but doing source-hybrid compile, do super version to check source
        //if (compileSuccess && _proj.getBuildFile().isRunWithInterpreter())
        //    compileSuccess = super.buildFile(sourceFile);

        // If compile failed, re-add to BuildFiles and return
        if (!compileSuccess) {
            addBuildFile(sourceFile);
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

            // Reload classes for Java file - just continue if no changes
            boolean classChanged = reloadClassesForJavaFile(modifiedJavaFile);
            if (!classChanged)
                continue;

            // Get Java files dependent on JavaFile and rebuild to account for any member changes
            WebFile[] dependentJavaFiles = WorkspaceUtils.getJavaFilesDependentOnJavaFile(modifiedJavaFile);
            for (WebFile dependentJavaFile : dependentJavaFiles)
                rebuildDependentJavaFileToAccountForMemberChanges(dependentJavaFile, sourceFiles);
        }
    }

    /**
     * Rebuilds a dependent java file that may have been effected by changes to a compiled java file.
     */
    private void rebuildDependentJavaFileToAccountForMemberChanges(WebFile dependentJavaFile, List<WebFile> sourceFiles)
    {
        // Make sure updated file is in sourceFiles list
        Project proj = Project.getProjectForFile(dependentJavaFile);
        if (proj == _proj) {
            if (!_compiledFiles.contains(dependentJavaFile))
                ListUtils.addUniqueId(sourceFiles, dependentJavaFile);
        }

        // Otherwise, add build file
        else {
            ProjectBuilder projectBuilder = proj.getBuilder();
            projectBuilder.addBuildFileForce(dependentJavaFile);
        }
    }

    /**
     * Reload classes for Java file and return whether class changed.
     */
    private static boolean reloadClassesForJavaFile(WebFile javaFile)
    {
        Project project = Project.getProjectForFile(javaFile);
        WebFile[] classFiles = project.getProjectFiles().getClassFilesForJavaFile(javaFile);
        boolean classChanged = false;

        // Get new declarations
        for (WebFile classFile : classFiles) {
            JavaClass javaClass = project.getJavaClassForFile(classFile);
            if (javaClass == null)
                continue;

            // Update decls
            boolean changed = javaClass.reloadClass();
            if (changed)
                classChanged = true;
        }

        // Return
        return classChanged;
    }
}
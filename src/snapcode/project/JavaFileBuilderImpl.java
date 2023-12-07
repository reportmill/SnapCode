/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.*;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilderImpl extends JavaFileBuilder {

    // The SnapCompiler used for last compiles
    private SnapCompiler  _compiler;

    // The final set of compiled files
    private Set<WebFile> _compiledFiles;

    // The final set of compiled files with errors
    private Set<WebFile>  _errorFiles;

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilderImpl(Project aProject)
    {
        super(aProject);
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
        // Get source files
        if (_buildFiles.size() == 0) return true;
        List<WebFile> sourceFiles = new ArrayList<>(_buildFiles);
        _buildFiles.clear();

        // Create compiler and sets for compiled/error files
        _compiler = new SnapCompiler(_proj);
        _compiledFiles = new HashSet<>();
        _errorFiles = new HashSet<>();
        boolean compileSuccess = true;

        // Reset Interrupt flag
        _interrupt = false;

        // Iterate over build files and compile
        for (int i = 0; i < sourceFiles.size(); i++) {

            // Get next source file - if already built, just skip
            WebFile sourceFile = sourceFiles.get(i);
            if (_compiledFiles.contains(sourceFile))
                continue;

            // If interrupted, add remaining build files and return
            if (_interrupt) {
                List<WebFile> remainingSourceFiles = sourceFiles.subList(i, sourceFiles.size());
                remainingSourceFiles.forEach(this::addBuildFile);
                return false;
            }

            // Add task manager task with message: "Compiling MyClass (X of MaxX)"
            String className = _proj.getProjectFiles().getClassNameForFile(sourceFile);
            String msg = String.format("Compiling %s (%d of %d)", className, i, sourceFiles.size());
            aTaskMonitor.beginTask(msg, -1);

            // Build file
            boolean fileCompileSuccess = buildFile(sourceFile, sourceFiles);
            if (fileCompileSuccess)
                compileSuccess = false;

            // Stop task manager task
            aTaskMonitor.endTask();
        }

        // Finalize TaskMonitor
        aTaskMonitor.beginTask("Build Completed", -1);
        aTaskMonitor.endTask();

        // Return
        return compileSuccess;
    }

    /**
     * Compiles file.
     */
    public boolean buildFile(WebFile sourceFile, List<WebFile> sourceFiles)
    {
        // Compile file
        boolean compileSuccess = _compiler.compile(sourceFile);

        // If compile failed, re-add file to BuildFiles and continue
        if (!compileSuccess) {
            _compiledFiles.add(sourceFile);
            _errorFiles.add(sourceFile);
            addBuildFile(sourceFile);
            if (_compiler._errorCount >= 1000)
                _interrupt = true;
            return false;
        }

        // Add Compiler.CompiledFiles to CompiledFiles
        Set<WebFile> compiledJavaFiles = _compiler.getCompiledJavaFiles();
        _compiledFiles.addAll(compiledJavaFiles);

        // Process modified Java files
        processModifiedJavaFiles(sourceFiles);

        // Return success
        return true;
    }

    /**
     * Processes recompiled java files (delete zombie class files and add child dependents to source files).
     */
    private void processModifiedJavaFiles(List<WebFile> sourceFiles)
    {
        Set<WebFile> modifiedJavaFiles = _compiler.getModifiedJavaFiles();

        // If there were modified files, clear Project.ClassLoader
        if (modifiedJavaFiles.size() > 0) {
            Workspace workspace = _proj.getWorkspace();
            workspace.clearClassLoader();
        }

        // Iterate over JavaFiles for modified ClassFiles and update
        for (WebFile modifiedJavaFile : modifiedJavaFiles) {

            // Delete class files for removed inner classes
            deleteZombieClassFiles(modifiedJavaFile);

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
                    if (!_compiledFiles.contains(updateFile)) {
                        if (!ListUtils.containsId(sourceFiles, updateFile))
                            sourceFiles.add(updateFile);
                    }
                }

                // Otherwise, add build file
                else {
                    ProjectBuilder projectBuilder = proj.getBuilder();
                    projectBuilder.addBuildFileForce(updateFile);
                }
            }
        }
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()
    {
        // Sanity check
        if (_interrupt) return;

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
        // Get inner ClassFiles for JavaFile
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile[] innerClassFiles = projFiles.getInnerClassFilesForJavaFile(aJavaFile);

        // Iterate over class files and delete if older than source file
        for (WebFile classFile : innerClassFiles) {
            boolean classFileOlderThanSource = classFile.getLastModTime() < aJavaFile.getLastModTime();
            if (classFileOlderThanSource) {
                try { classFile.delete(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        }
    }
}
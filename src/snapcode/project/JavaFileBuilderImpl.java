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
        _compiledFiles = new HashSet<>();
        _errorFiles = new HashSet<>();
    }

    /**
     * Returns whether file is build file.
     */
    @Override
    public boolean isBuildFile(WebFile aFile)
    {
        String type = aFile.getType();
        if (type.equals("java"))
            return true;
        if (type.equals("jepl")) {
            if (_proj.getBuildFile().isRunWithInterpreter())
                return true;
        }
        return false;
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

        // Create compiler and clear sets for compiled/error files
        _compiler = new SnapCompiler(_proj);
        _compiledFiles.clear();
        _errorFiles.clear();
        boolean compileSuccess = true;

        // Reset Interrupt flag
        _interrupted = false;

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
            boolean fileCompileSuccess = buildFile(sourceFile, sourceFiles);
            if (!fileCompileSuccess)
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
    private boolean buildFile(WebFile sourceFile, List<WebFile> sourceFiles)
    {
        // Compile file
        boolean compileSuccess = _compiler.compileFile(sourceFile);

        // If compile failed, re-add file to BuildFiles and continue
        if (!compileSuccess) {
            _compiledFiles.add(sourceFile);
            _errorFiles.add(sourceFile);
            addBuildFile(sourceFile);
            if (_compiler._errorCount >= 1000)
                _interrupted = true;
            return false;
        }

        // Add Compiler.CompiledFiles to CompiledFiles
        Set<WebFile> compiledJavaFiles = _compiler.getCompiledJavaFiles();
        _compiledFiles.addAll(compiledJavaFiles);

        // Find dependencies for modified Java files and add to source files
        findDependenciesForModifiedJavaFiles(sourceFiles);

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
        if (_interrupted || _compiler == null) return;

        // Iterate over compiled files
        for (WebFile javaFile : _compiledFiles) {

            // Just continue if file contains errors
            if (_errorFiles.contains(javaFile))
                continue;

            // Iterate over build issues
            BuildIssue[] unusedImportIssues = getUnusedImportBuildIssuesForFile(javaFile);
            for (BuildIssue buildIssue : unusedImportIssues)
                _compiler.reportBuildIssue(buildIssue);
        }

        // Clear vars
        _compiler = null;
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.resolver.JavaClass;
import snap.util.ListUtils;
import snap.util.SnapUtils;
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
    protected Set<WebFile> _buildFiles = Collections.synchronizedSet(new HashSet<>());

    // The final set of compiled files
    protected Set<WebFile> _compiledFiles;

    // The SnapCompiler used for last compiles
    private SnapCompiler _compiler;

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
    public boolean isNeedsBuild()  { return !_buildFiles.isEmpty(); }

    /**
     * Returns whether file is build file.
     */
    @Override
    public boolean isBuildFile(WebFile aFile)
    {
        String fileType = aFile.getFileType();
        return fileType.equals("java") || fileType.equals("jepl");
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
     * Adds a compile file.
     */
    @Override
    public void addBuildFile(WebFile aFile)  { _buildFiles.add(aFile); }

    /**
     * Remove a build file.
     */
    @Override
    public void removeBuildFile(WebFile javaFile)
    {
        // Remove file
        _buildFiles.remove(javaFile);

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
            if (SnapUtils.isWebVM)
                Thread.yield();

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
     * Builds a file.
     */
    protected boolean buildFile(WebFile sourceFile)
    {
        // If Jepl file, check for errors first
        if (sourceFile.getFileType().equals("jepl")) {
            boolean checkErrorsSuccess = buildJeplFile(sourceFile);
            if (!checkErrorsSuccess) {
                addBuildFile(sourceFile);
                return false;
            }
        }

        // Compile file
        boolean compileSuccess = _compiler.compileFile(sourceFile);

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

    /**
     * Builds a Jepl file.
     */
    protected boolean buildJeplFile(WebFile javaFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
        javaAgent.checkFileForErrors();
        BuildIssue[] buildIssues = javaAgent.getBuildIssues();
        return buildIssues.length == 0;
    }
}
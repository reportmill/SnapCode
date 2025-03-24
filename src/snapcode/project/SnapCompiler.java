/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.web.WebFile;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.StringWriter;
import java.util.*;

/**
 * A class to compile a Java file.
 */
public class SnapCompiler {

    // The Project
    protected Project  _proj;

    // Whether to check errors only
    private boolean _checkErrorsOnly;

    // The shared compiler
    private JavaCompiler  _compiler;

    // The options for compile
    private List<String>  _options;

    // The shared file manager for any project compile
    private SnapCompilerFM _fileManager;

    // Whether compile succeeded (no errors encountered)
    private boolean  _succeeded;

    // The Set of source files compiled by last compile
    protected Set<WebFile> _compiledJavaFiles = new HashSet<>();

    // The Set of source files that had class files modified by last compile
    protected Set<WebFile> _modifiedJavaFiles = new HashSet<>();

    // The number of errors currently encountered
    protected int  _errorCount;

    /**
     * Constructor.
     */
    public SnapCompiler(Project aProject)
    {
        _proj = aProject;
    }

    /**
     * Returns the Project.
     */
    public Project getProject()  { return _proj; }

    /**
     * Returns the java compiler.
     */
    public JavaCompiler getCompiler()
    {
        // If already set, just return
        if (_compiler != null) return _compiler;

        // Get System Java compiler - just return if found
        _compiler = ToolProvider.getSystemJavaCompiler();
        if (_compiler != null)
            return _compiler;

        // Get compiler class and instance and return
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Class<?> compilerClass = Class.forName("com.sun.tools.javac.api.JavacTool", true, classLoader);
            return _compiler = (JavaCompiler) compilerClass.newInstance();
        }

        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the options list.
     */
    protected List<String> getOptions()
    {
        // If already set, just return
        if (_options != null) return _options;

        // Create Options list, add debug flag and source/target flag for Java 1.5
        List<String> options = new ArrayList<>();
        options.add("-Xlint:all,-serial,-rawtypes,-unchecked,-fallthrough,-dep-ann");
        //options.add("-warn:-serial,-raw,-unchecked"); options.add("-proceedOnError");

        // Handle CheckErrorsOnly (either set proc none or add debug)
        if (_checkErrorsOnly)
            options.add("-proc:none");
        else options.add("-g");

        // Set release version
        if (SnapUtils.getJavaVersionInt() > 8) {
            BuildFile buildFile = _proj.getBuildFile();
            int compileRelease = buildFile.getCompileRelease();
            options.add("--release");
            options.add(Integer.toString(compileRelease));
        }

        // Probably not necessary
        else Collections.addAll(options, "-source", "8", "-target", "8");

        // Add class paths for project dependencies (libraries and child projects)
        String[] compilerClassPaths = _proj.getCompileClassPaths();
        if (compilerClassPaths.length > 0) {
            String[] classPathsNtv = FilePathUtils.getNativePaths(compilerClassPaths);
            String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
            options.add("-cp");
            options.add(classPath);
        }

        // Set/return
        return _options = options;
    }

    /**
     * Tells compiler to check errors only (don't generate classes).
     */
    public void checkErrorsOnly()  { _checkErrorsOnly = true; }

    /**
     * Returns the compiler file manager.
     */
    public SnapCompilerFM getFileManager()
    {
        if (_fileManager != null) return _fileManager;

        // Create file manager
        JavaCompiler javaCompiler = getCompiler();
        StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(null, null, null);
        return _fileManager = new SnapCompilerFM(this, standardFileManager);
    }

    /**
     * Compiles the given file.
     */
    public boolean compileFile(WebFile aFile)
    {
        // Clear files from previous compile
        _compiledJavaFiles.clear();
        _modifiedJavaFiles.clear();

        // Get compiler and file manager
        JavaCompiler compiler = getCompiler();
        StringWriter additionalOutputWriter = new StringWriter();
        SnapCompilerFM fileManager = getFileManager();
        DiagnosticListener<JavaFileObject> diagnosticListener = diag -> reportDiagnostic(diag);

        // Get JFOs
        JavaFileObject jfo = fileManager.getJavaFileObject(aFile);
        List<JavaFileObject> jfos = Collections.singletonList(jfo);

        // Get task, call and return _succeeded
        List<String> options = getOptions();
        CompilationTask task = compiler.getTask(additionalOutputWriter, fileManager, diagnosticListener, options, null, jfos);

        // Call task
        _succeeded = true;
        task.call();

        // If success - delete any zombie inner class files for compiled Java files
        if (_succeeded)
            deleteZombieInnerClassFiles();

        // Return
        return _succeeded;
    }

    /**
     * Returns the Set of source files that had class files over-written by last compile.
     */
    public Set<WebFile> getCompiledJavaFiles()  { return _compiledJavaFiles; }

    /**
     * Returns the Set of source files that had class files actually modified by last compile.
     */
    public Set<WebFile> getModifiedJavaFiles()  { return _modifiedJavaFiles; }

    /**
     * Delete zombie inner class files for recompiled Java files.
     */
    private void deleteZombieInnerClassFiles()
    {
        // Iterate over recompiled JavaFiles to delete zombie inner classes
        for (WebFile modifiedJavaFile : _modifiedJavaFiles) {

            // Get inner ClassFiles for JavaFile
            ProjectFiles projFiles = _proj.getProjectFiles();
            WebFile[] innerClassFiles = projFiles.getInnerClassFilesForJavaFile(modifiedJavaFile);

            // Iterate over class files and delete if older than source file
            for (WebFile classFile : innerClassFiles) {
                boolean classFileOlderThanSource = classFile.getLastModTime() < modifiedJavaFile.getLastModTime();
                if (classFileOlderThanSource) {
                    try { classFile.delete(); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }
            }
        }

        // If there were modified files, clear Project.ClassLoader
        if (!_modifiedJavaFiles.isEmpty())
            _proj.clearClassLoader();
    }

    /**
     * Report BuildIssue.
     */
    protected void reportBuildIssue(BuildIssue anIssue)
    {
        Workspace workspace = _proj.getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();
        buildIssues.add(anIssue);
        if (anIssue.getKind() == BuildIssue.Kind.Error)
            _errorCount++;
    }

    /**
     * Returns a BuildIssue for Diagnostic.
     */
    protected BuildIssue createBuildIssue(Diagnostic aDiagnostic)
    {
        Object diagnosticSource = aDiagnostic.getSource();
        if (!(diagnosticSource instanceof SnapCompilerJFO)) {
            if (_unknownDiagnosticSourceErrorCount++ < 5)
                System.err.println("SnapCompiler: Unknown Issue: " + aDiagnostic);
            return null;
        }

        // Get File
        SnapCompilerJFO snapFileJFO = (SnapCompilerJFO) diagnosticSource;
        WebFile file = snapFileJFO.getFile();

        // Get Kind
        BuildIssue.Kind kind = BuildIssue.Kind.Note;
        switch (aDiagnostic.getKind()) {
            case ERROR: kind = BuildIssue.Kind.Error; break;
            case WARNING: kind = BuildIssue.Kind.Warning; break;
            case MANDATORY_WARNING: kind = BuildIssue.Kind.Warning; break;
            default:
        }

        // Get message
        String msg = aDiagnostic.getMessage(Locale.ENGLISH);
        int loc = msg.indexOf("location:");
        if (loc > 0) msg = msg.substring(0, loc).trim();

        // Get LineNumber, ColumnNumber
        int line = (int) aDiagnostic.getLineNumber();
        int col = (int) aDiagnostic.getColumnNumber();
        int start = (int) aDiagnostic.getStartPosition();
        if (start < 0)
            start = 0;
        int end = (int) aDiagnostic.getEndPosition();

        // Bogus trim of "unchecked" warnings and "overrides equals
        if (line < 0 && msg.contains("unchecked"))
            return null;
        if (msg.contains("overrides equals, but"))
            return null;

        // Create and configure BuildIssue and return
        BuildIssue issue = new BuildIssue().init(file, kind, msg, line - 1, col - 1, start, end);
        return issue;
    }

    private int _unknownDiagnosticSourceErrorCount = 0;

    /**
     * Report Diagnostic.
     */
    private void reportDiagnostic(Diagnostic<?> aDiagnostic)
    {
        if (_succeeded && aDiagnostic.getKind() == Diagnostic.Kind.ERROR)
            _succeeded = false;

        // Create issue and report
        BuildIssue issue = createBuildIssue(aDiagnostic);
        if (issue != null)
            reportBuildIssue(issue);
    }
}
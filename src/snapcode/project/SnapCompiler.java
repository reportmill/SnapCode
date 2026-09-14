/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.FilePathUtils;
import snap.util.SnapEnv;
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
    protected Project _proj;

    // Whether to check errors only
    protected boolean _checkErrorsOnly;

    // The shared compiler
    private JavaCompiler _compiler;

    // The options for compile
    private List<String> _options;

    // The shared file manager for any project compile
    private SnapCompilerFM _fileManager;

    // Whether compile succeeded (no errors encountered)
    private boolean _succeeded;

    // The Set of source files compiled by last compile
    protected Set<WebFile> _compiledJavaFiles = new HashSet<>();

    // The Set of source files that had class files modified by last compile
    protected Set<WebFile> _modifiedJavaFiles = new HashSet<>();

    // The number of errors currently encountered
    protected int _errorCount;

    // The number of errors from unknown diagnostics
    private int _unknownDiagnosticSourceErrorCount;

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
        //if (_compiler == null)
        //    System.out.println("ToolProvider.getSystemJavaCompiler not found");

        // Get compiler class and instance and return
        if (_compiler == null) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                Class<?> compilerClass = Class.forName("com.sun.tools.javac.api.JavacTool", true, classLoader);
                _compiler = (JavaCompiler) compilerClass.getConstructor().newInstance();
            }

            catch (Exception e) { throw new RuntimeException(e); }
        }

        // Return
        return _compiler;
    }

    /**
     * Returns the options list.
     */
    protected List<String> getOptions()
    {
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
        if (SnapUtils.getJavaVersionInt() > 11 && !SnapEnv.isWebVM) {
            BuildFile buildFile = _proj.getBuildFile();
            int compileRelease = buildFile.getCompileRelease();
            options.add("--release");
            options.add(Integer.toString(compileRelease));
        }

        // Handle BuildFile.EnableCompilePreview -enable-preview
        BuildFile buildFile = _proj.getBuildFile();
        if (buildFile.isEnableCompilePreview())
            options.add("--enable-preview");

        // Add class paths for project dependencies (libraries and child projects)
        String[] compilerClassPaths = _proj.getCompileClassPaths();
        if (compilerClassPaths.length > 0) {
            String[] classPathsNtv = FilePathUtils.getNativePaths(compilerClassPaths);
            String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
            options.add("-cp");
            options.add(classPath);
        }

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
        DiagnosticListener<JavaFileObject> diagnosticLsnr = this::handleDiagnostic;

        // Get JFOs
        JavaFileObject jfo = fileManager.getJavaFileObject(aFile);
        List<JavaFileObject> jfos = Collections.singletonList(jfo);

        // Get task, call and return _succeeded
        List<String> options = getOptions();
        CompilationTask task = compiler.getTask(additionalOutputWriter, fileManager, diagnosticLsnr, options, null, jfos);

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
     * Handles given compiler Diagnostic.
     */
    private void handleDiagnostic(Diagnostic<?> aDiagnostic)
    {
        if (_succeeded && aDiagnostic.getKind() == Diagnostic.Kind.ERROR)
            _succeeded = false;

        // Create build issue for given diagnostic and add to workspace
        BuildIssue buildIssue = createBuildIssueForDiagnostic(aDiagnostic);
        if (buildIssue != null)
            addBuildIssueToWorkspace(buildIssue);
    }

    /**
     * Adds given BuildIssue to workspace.
     */
    protected void addBuildIssueToWorkspace(BuildIssue buildIssue)
    {
        Workspace workspace = _proj.getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();
        buildIssues.addBuildIssue(buildIssue);
        if (buildIssue.getKind() == BuildIssue.Kind.Error)
            _errorCount++;
    }

    /**
     * Returns a BuildIssue for Diagnostic.
     */
    private BuildIssue createBuildIssueForDiagnostic(Diagnostic<?> aDiagnostic)
    {
        Object diagnosticSource = aDiagnostic.getSource();
        if (diagnosticSource instanceof SnapCompilerJFO snapFileJFO)
            return createBuildIssueForDiagnosticAndSnapJFO(aDiagnostic, snapFileJFO);

        if (aDiagnostic.getKind() == Diagnostic.Kind.ERROR) {
            if (_unknownDiagnosticSourceErrorCount++ < 5)
                System.err.println("SnapCompiler: Unknown Error: " + getMessageString(aDiagnostic));
        }

        else System.out.println("SnapCompiler: Unknown warning: " + getMessageString(aDiagnostic));
        return null;
    }

    /**
     * Returns a BuildIssue for Diagnostic.
     */
    private BuildIssue createBuildIssueForDiagnosticAndSnapJFO(Diagnostic<?> aDiagnostic, SnapCompilerJFO snapFileJFO)
    {
        if (isSillyDiagnostic(aDiagnostic))
            return null;

        WebFile javaFile = snapFileJFO.getFile();

        // Get issue kind
        BuildIssue.Kind kind = switch (aDiagnostic.getKind()) {
            case ERROR -> BuildIssue.Kind.Error;
            case WARNING -> BuildIssue.Kind.Warning;
            case MANDATORY_WARNING -> BuildIssue.Kind.Warning;
            default -> BuildIssue.Kind.Note;
        };

        // Get user friendly, single line message
        String errorMsg = getMessageString(aDiagnostic);

        // Get LineNumber, ColumnNumber
        int line = (int) aDiagnostic.getLineNumber();
        int col = (int) aDiagnostic.getColumnNumber();
        int startCharIndex = (int) aDiagnostic.getStartPosition();
        if (startCharIndex < 0)
            startCharIndex = 0;
        int endCharIndex = Math.max((int) aDiagnostic.getEndPosition(), startCharIndex);

        // If Jepl, convert locations from Java back to Jepl
        if (javaFile.getFileType().equals("jepl")) {
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
            JeplToJava.JavaText javaText = javaAgent.getJeplJavaText();
            startCharIndex = javaText.getJeplCharIndexForJavaCharIndex(startCharIndex);
            endCharIndex = javaText.getJeplCharIndexForJavaCharIndex(endCharIndex);
            line = javaText.getJeplLineIndexForJeplCharIndex(startCharIndex);
        }

        // Return new BuildIssue
        return new BuildIssue().init(javaFile, kind, errorMsg, line - 1, col - 1, startCharIndex, endCharIndex);
    }

    /**
     * Returns the most user friendly single line string (no newlines) for a Diagnostic's message.
     */
    private static String getMessageString(Diagnostic<?> aDiagnostic)
    {
        // Get message
        String message = aDiagnostic.getMessage(Locale.ENGLISH);
        if (message == null)
            return "";

        // Strip trailing 'symbol:'/'location:' detail lines, which just repeat info in the source
        int stripIndex = message.indexOf("symbol:");
        if (stripIndex < 0)
            stripIndex = message.indexOf("location:");
        if (stripIndex > 0)
            message = message.substring(0, stripIndex);

        // Collapse any newlines and runs of whitespace into single spaces
        message = message.replaceAll("\n\\s*", ", ").trim();
        message = message.replaceAll("\\s+", " ");

        // Strip trailing semicolon (multi-line diagnostics often end their summary line with one)
        if (message.endsWith(";"))
            message = message.substring(0, message.length() - 1).trim();

        // Return
        return message;
    }

    /**
     * Returns whether given diagnostic should be ignored.
     */
    private static boolean isSillyDiagnostic(Diagnostic<?> aDiagnostic)
    {
        String errorMsg = aDiagnostic.getMessage(Locale.ENGLISH);

        // Skip "unchecked" warnings
        if (aDiagnostic.getLineNumber() < 0 && errorMsg.contains("unchecked"))
            return true;

        // Skip "overrides equals"
        if (errorMsg.contains("overrides equals, but"))
            return true;

        // Skip "Possible 'this' escape"
        if (errorMsg.contains("possible 'this' escape"))
            return true;

        // Skip 'preview feature' warning
        if (errorMsg.contains("preview feature"))
            return true;

        return false;
    }
}
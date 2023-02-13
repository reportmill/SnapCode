/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.BuildIssue;
import javakit.project.Project;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.StringWriter;
import java.util.*;

/**
 * A class to compile a Java file.
 */
public class SnapCompiler implements DiagnosticListener {

    // The Project
    Project  _proj;

    // The shared compiler
    JavaCompiler  _compiler;

    // The options for compile
    List<String>  _options;

    // The shared file manager for any project compile
    SnapCompilerFM  _fm;

    // Whether compile succeeded (no errors encountered)
    boolean  _succeeded;

    // The Set of source files compiled by last compile
    Set<WebFile>  _compJFs = new HashSet<>();

    // The Set of source files that had class files modified by last compile
    Set<WebFile>  _modJFs = new HashSet<>();

    // The number of errors currently encountered
    int _errorCount;

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
            Class<?> cls = Class.forName("com.sun.tools.javac.api.JavacTool", true, getClass().getClassLoader());
            return _compiler = (JavaCompiler) cls.newInstance();
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
        List<String> options = new ArrayList();
        options.add("-Xlint:all,-serial,-rawtypes,-unchecked,-fallthrough,-dep-ann");
        //options.add("-warn:-serial,-raw,-unchecked"); options.add("-proceedOnError");
        options.add("-g");
        options.add("-source");
        options.add("1.8");
        options.add("-target");
        options.add("1.8");

        // Add class path from project library paths and return
        String[] libPaths = _proj.getProjectSet().getLibPaths();
        if (libPaths.length > 0) {
            String[] libPathsNtv = FilePathUtils.getNativePaths(libPaths);
            String cpath = FilePathUtils.getJoinedPath(libPathsNtv);
            options.add("-cp");
            options.add(cpath);
        }

        return _options = options;
    }

    /**
     * Returns the compiler file manager.
     */
    public SnapCompilerFM getFileManaer()
    {
        if (_fm != null) return _fm;
        StandardJavaFileManager sfm = getCompiler().getStandardFileManager(null, null, null);
        return _fm = new SnapCompilerFM(this, sfm);
    }

    /**
     * Executes the compile task.
     */
    public boolean compile(WebFile aFile)
    {
        // Clear files from previous compile
        _compJFs.clear();
        _modJFs.clear();

        // Get compiler and file manager
        JavaCompiler compiler = getCompiler();
        SnapCompilerFM fman = getFileManaer();

        // Get JFOs
        JavaFileObject jfo = fman.getJFO(aFile.getPath(), aFile);
        List<JavaFileObject> jfos = Arrays.asList(jfo);

        // Get task, call and return _succeeded
        CompilationTask task = compiler.getTask(new StringWriter(), fman, this, getOptions(), null, jfos);
        _succeeded = true;
        task.call();
        return _succeeded;
    }

    /**
     * Report Diagnostic.
     */
    public void report(Diagnostic aDiagnostic)
    {
        if (_succeeded && aDiagnostic.getKind() == Diagnostic.Kind.ERROR) _succeeded = false;
        BuildIssue issue = createBuildIssue(aDiagnostic);
        if (issue == null) return; //System.out.println(issue.toString());
        report(issue);
    }

    /**
     * Report BuildIssue.
     */
    protected void report(BuildIssue anIssue)
    {
        _proj.getRootProject().getBuildIssues().add(anIssue);
        if (anIssue.getKind() == BuildIssue.Kind.Error) _errorCount++;
    }

    /**
     * Returns a BuildIssue for Diagnostic.
     */
    protected BuildIssue createBuildIssue(Diagnostic aDiagnostic)
    {
        if (!(aDiagnostic.getSource() instanceof SnapCompilerJFO)) {
            /*System.err.println("SnapCompiler: Unknown Issue: " + aDiagnostic); */
            return null;
        }

        // Get File
        SnapCompilerJFO snapFileJFO = (SnapCompilerJFO) aDiagnostic.getSource();
        WebFile file = snapFileJFO.getFile();

        // Get Kind
        BuildIssue.Kind kind = BuildIssue.Kind.Note;
        switch (aDiagnostic.getKind()) {
            case ERROR:
                kind = BuildIssue.Kind.Error;
                break;
            case WARNING:
                kind = BuildIssue.Kind.Warning;
                break;
            case MANDATORY_WARNING:
                kind = BuildIssue.Kind.Warning;
                break;
            default:
        }

        // Get message
        String msg = aDiagnostic.getMessage(Locale.ENGLISH);
        int loc = msg.indexOf("location:");
        if (loc > 0) msg = msg.substring(0, loc).trim();

        // Get LineNumber, ColumnNumber
        int line = (int) aDiagnostic.getLineNumber(), col = (int) aDiagnostic.getColumnNumber();
        int start = (int) aDiagnostic.getStartPosition();
        if (start < 0) start = 0;
        int end = (int) aDiagnostic.getEndPosition();

        // Bogus trim of "unchecked" warnings and "overrides equals
        if (line < 0 && msg.indexOf("unchecked") >= 0) return null;
        if (msg.indexOf("overrides equals, but") > 0) return null;

        // Create and configure BuildIssue and return
        BuildIssue issue = new BuildIssue().init(file, kind, msg, line - 1, col - 1, start, end);
        return issue;
    }

    /**
     * Returns the Set of source files that had class files modified by the compile.
     */
    public Set<WebFile> getCompiledJavaFiles()
    {
        return _compJFs;
    }

    /**
     * Returns the Set of source files that had class files modified by the compile.
     */
    public Set<WebFile> getModifiedJavaFiles()
    {
        return _modJFs;
    }

}
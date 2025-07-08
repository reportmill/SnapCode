/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.TextBlock;
import snap.text.TextBlockUtils;
import snap.util.ArrayUtils;
import snap.util.SetUtils;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.util.MDUtils;

import java.util.*;

/**
 * This class holds a parsed Java file.
 */
public class JavaAgent {

    // The java file
    private WebFile _javaFile;

    // Whether Java file is really Java REPL (.jepl)
    private boolean _isJepl;

    // Whether Java file is really Java Markdown (.jmd)
    private boolean _isJMD;

    // The Project that owns this file
    private Project _project;

    // The JavaTextDoc version of this file
    private JavaTextDoc _javaTextDoc;

    // The parsed version of this JavaFile
    protected JFile _jfile;

    // The Java text generated from Jepl, if Jepl
    private JeplToJava.JavaText _jeplJavaText;

    // The external references in Java file
    private Set<JavaDecl> _externalRefs;

    // The external class references in Java file
    private Set<JavaClass> _externalClassRefs;

    // A listener for file prop changes
    private PropChangeListener _fileBytesChangedLsnr;

    // The Jepl default imports
    private static String[] _jeplImports;

    // The Jepl default imports with charts
    private static String[] _jeplImportsWithCharts;

    /**
     * Constructor for given file.
     */
    private JavaAgent(WebFile aFile)
    {
        _javaFile = aFile;
        _isJepl = aFile.getFileType().equals("jepl");
        _isJMD = aFile.getFileType().equals("jmd");

        // Set File JavaAgent property to this agent
        _javaFile.setProp(JavaAgent.class.getName(), this);

        // Start listening for file bytes changed
        _fileBytesChangedLsnr = pc -> handleJavaFileBytesChange();
        _javaFile.addPropChangeListener(_fileBytesChangedLsnr, WebFile.Bytes_Prop);

        // Add to Project
        _project = Project.getProjectForFile(_javaFile);
        if (_project != null)
            _project.addJavaAgent(this);
    }

    /**
     * Closes this agent.
     */
    public void closeAgent()
    {
        // If already close, complain and return
        if (_javaFile == null) {
            System.err.println("JavaAgent.closeAgent: Multiple closes");
            return;
        }

        // Clear everything
        clearBuildIssues();
        _javaFile.setProp(JavaAgent.class.getName(), null);
        _javaFile.removePropChangeListener(_fileBytesChangedLsnr);
        _javaFile.reset();
        _javaFile = null;
        _project = null;
        _jfile = null;
        _jeplJavaText = null;
        _javaTextDoc = null;
        _externalRefs = null;
        _externalClassRefs = null;
    }

    /**
     * Returns whether file is really Jepl.
     */
    public boolean isJepl()  { return  _isJepl; }

    /**
     * Returns whether file is really Java markdown.
     */
    public boolean isJMD()  { return  _isJMD; }

    /**
     * Returns the WebFile.
     */
    public WebFile getFile()  { return _javaFile; }

    /**
     * Returns the project for this JavaFile.
     */
    public Project getProject()  { return _project; }

    /**
     * Returns the JavaTextDoc for this JavaFile.
     */
    public JavaTextDoc getJavaTextDoc()
    {
        if (_javaTextDoc != null) return _javaTextDoc;

        // Create/load JavaTextDoc
        _javaTextDoc = createJavaTextDoc();
        _jfile = null;
        _javaTextDoc.readFromSourceURL(_javaFile.getUrl());

        // Listen for changes
        _javaTextDoc.addPropChangeListener(this::handleJavaTextDocPropChange);

        // Set, return
        return _javaTextDoc;
    }

    /**
     * Creates the JavaTextDoc.
     */
    protected JavaTextDoc createJavaTextDoc()  { return new JavaTextDoc(); }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        // If already set, just return
        if (_jfile != null) return _jfile;

        // Create, Set, return
        JFile jfile = createJFile();
        return _jfile = jfile;
    }

    /**
     * Parses and returns JFile.
     */
    protected JFile createJFile()
    {
        // Get parsed java file
        JavaParser javaParser = JavaParser.getShared();
        CharSequence javaStr = getJavaText();

        // Parse file
        JFile jfile;
        if (_isJepl || _isJMD)
            jfile = parseJepl(javaParser, javaStr);
        else jfile = javaParser.parseFile(javaStr);

        // Set SourceFile
        jfile.setSourceFile(_javaFile);
        Project project = getProject();
        if (project != null)
            jfile.setResolverSupplier(() -> project.getResolver());

        // Return
        return jfile;
    }

    /**
     * Parses a Jepl file.
     */
    private JFile parseJepl(JavaParser javaParser, CharSequence javaStr)
    {
        String className = getFile().getSimpleName();
        String[] importNames = getJeplDefaultImports();
        if (_project.getBuildFile().isIncludeSnapChartsRuntime())
            importNames = getJeplDefaultImportsWithCharts();
        String superClassName = "Object";

        if (_isJMD) {
            javaStr = MDUtils.getJeplForJMD(className, javaStr);
            return javaParser.parseJeplFile(javaStr, className, importNames, superClassName);
        }
        return javaParser.parseJeplFile(javaStr, className, importNames, superClassName);
    }

    /**
     * Returns the Jepl Java text.
     */
    public JeplToJava.JavaText getJeplJavaText()
    {
        if (_jeplJavaText != null) return _jeplJavaText;
        return _jeplJavaText = new JeplToJava(getJFile()).getJavaText();
    }

    /**
     * Returns the build issues.
     */
    public BuildIssue[] getBuildIssues()
    {
        Workspace workspace = _project != null ? _project.getWorkspace() : null;
        if (workspace == null)
            return new BuildIssue[0];
        BuildIssues projBuildIssues = workspace.getBuildIssues();
        return projBuildIssues.getIssuesForFile(_javaFile);
    }

    /**
     * Returns the build errors.
     */
    public BuildIssue[] getBuildErrors()
    {
        BuildIssue[] buildIssues = getBuildIssues();
        return ArrayUtils.filter(buildIssues, issue -> issue.isError());
    }

    /**
     * Sets the build issues.
     */
    public void setBuildIssues(BuildIssue[] buildIssues)
    {
        // Get Workspace.BuildIssues and clear
        Workspace workspace = _project != null ? _project.getWorkspace() : null;
        if (workspace == null)
            return;
        BuildIssues buildIssuesMgr = workspace.getBuildIssues();

        // Remove old issues
        WebFile javaFile = getFile();
        BuildIssue[] oldIssues = buildIssuesMgr.getIssuesForFile(javaFile);
        for (BuildIssue buildIssue : oldIssues)
            buildIssuesMgr.remove(buildIssue);

        // Add new issues
        for (BuildIssue buildIssue : buildIssues)
            buildIssuesMgr.add(buildIssue);
    }

    /**
     * Clears build issues for java file.
     */
    public void clearBuildIssues()
    {
        setBuildIssues(new BuildIssue[0]);
    }

    /**
     * Checks this file for errors.
     */
    public void checkFileForErrors()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Check for DEPS
        JavaDeps.resolveDependenciesForFile(null, _javaFile);

        // Get parse errors
        JFile jFile = getJFile();
        if (jFile.getException() != null)
            errors = NodeError.getNodeErrorForFileParseException(jFile);

        // Get errors for body declarations
        if (errors.length == 0)
            errors = jFile.getDeclarationErrors();

        // If no declaration errors, reload class and do full error check
        if (errors.length == 0) {
            reloadClassFromClassDecl();
            errors = NodeError.getAllNodeErrors(jFile);
        }

        // Convert to BuildIssues and set in agent
        WebFile javaFile = getFile();
        BuildIssue[] buildIssues = ArrayUtils.map(errors, error -> BuildIssue.createIssueForNodeError(error, javaFile), BuildIssue.class);

        // Check for unused imports
        BuildIssue[] unusedImportErrors = getUnusedImportErrors();
        if (unusedImportErrors.length > 0)
            buildIssues = ArrayUtils.addAll(buildIssues, unusedImportErrors);

        // Set build issues
        setBuildIssues(buildIssues);

        // If no errors, let compiler have a go
        if (!ArrayUtils.hasMatch(buildIssues, buildIssue -> buildIssue.isError())) {

            // Update text
            String javaText = getJavaTextDoc().getString();
            _javaFile.setText(javaText);

            // Compile file
            SnapCompiler compiler = new SnapCompiler(getProject());
            compiler.checkErrorsOnly();
            compiler.compileFile(_javaFile);
        }
    }

    /**
     * Returns an array of unused imports for Java file.
     */
    protected BuildIssue[] getUnusedImportErrors()
    {
        // Get unused import decls
        JFile jfile = getJFile();
        JImportDecl[] unusedImports = jfile.getUnusedImports();
        if (unusedImports.length == 0)
            return BuildIssues.NO_ISSUES;

        // Create BuildIssues for each and return
        return ArrayUtils.map(unusedImports, id -> createUnusedImportBuildIssue(_javaFile, id), BuildIssue.class);
    }

    /**
     * Reload class from JClassDecl.
     */
    private void reloadClassFromClassDecl()
    {
        // Get class decl (just return if null)
        JFile jFile = getJFile();
        JClassDecl classDecl = jFile.getClassDecl();
        if (classDecl == null)
            return;

        // Reload class
        JavaClass javaClass = classDecl.getJavaClass();
        javaClass.reloadClassFromClassDecl(classDecl);
    }

    /**
     * Returns the string from Java file.
     */
    public CharSequence getJavaText()
    {
        if (_javaTextDoc != null)
            return _javaTextDoc;
        return _javaFile.getText();
    }

    /**
     * Returns whether Java file is dependent of class.
     */
    public boolean isDependentOnClass(JavaClass javaClass)
    {
        Set<JavaClass> externalClassReferences = getExternalClassReferences();
        return externalClassReferences.contains(javaClass);
    }

    /**
     * Returns the external references.
     */
    public Set<JavaDecl> getExternalReferences()
    {
        if (_externalRefs != null) return _externalRefs;
        WebFile[] classFiles = _project.getProjectFiles().getClassFilesForJavaFile(_javaFile);
        return _externalRefs = ClassFileUtils.getExternalReferencesForClassFiles(classFiles);
    }

    /**
     * Clears external references.
     */
    public void clearExternalReferences()
    {
        _externalRefs = null;
        _externalClassRefs = null;
        _jfile = null;
        _jeplJavaText = null;
    }

    /**
     * Returns the external class references.
     */
    private Set<JavaClass> getExternalClassReferences()
    {
        if (_externalClassRefs != null) return _externalClassRefs;
        Set<JavaDecl> externalReferences = getExternalReferences();
        return _externalClassRefs = SetUtils.filterByClass(externalReferences, JavaClass.class);
    }

    /**
     * Reloads agent from file.
     */
    public void reloadFile()
    {
        _javaFile.resetAndVerify();
        reloadFromFile();
    }

    /**
     * Called when JavaTextDoc does prop change.
     */
    protected void handleJavaTextDocPropChange(PropChange aPC)
    {
        // Get PropName
        String propName = aPC.getPropName();

        // Handle CharsChange: Try to update JFile with partial parse
        if (propName == TextBlock.Chars_Prop && _jfile != null)
            handleJavaTextDocCharsChange((TextBlockUtils.CharsChange) aPC);

        // Handle TextModified: Register updater to update JavaFile before save
        else if (propName == TextBlock.TextModified_Prop) {
            boolean textDocTextModified = _javaTextDoc.isTextModified();
            WebFile javaFile = getFile();
            WebFile.Updater updater = textDocTextModified ? file -> updateFileFromTextDoc() : null;
            javaFile.setUpdater(updater);
        }
    }

    /**
     * Called when JavaTextDoc does chars change to updates JFile incrementally if possible.
     */
    private void handleJavaTextDocCharsChange(TextBlockUtils.CharsChange charsChange)
    {
        // If partial parse fails, clear JFile for full reparse
        boolean jfileUpdated = !_isJepl && !_isJMD && JavaTextDocUtils.updateJFileForChange(_javaTextDoc, _jfile, charsChange);
        if (!jfileUpdated)
            _jfile = null;
        _jeplJavaText = null;
        _externalRefs = null;
        _externalClassRefs = null;
    }

    /**
     * Called when JavaFile bytes changes.
     */
    private void handleJavaFileBytesChange()
    {
        ViewUtils.runLater(this::reloadFromFile);
    }

    /**
     * Called to update File.Text before save.
     */
    private void updateFileFromTextDoc()
    {
        WebFile javaFile = getFile();
        String javaText = _javaTextDoc.getString();
        javaFile.setText(javaText);
        _javaTextDoc.setTextModified(false);
    }

    /**
     * Reloads JavaTextDoc from file.
     */
    protected void reloadFromFile()
    {
        // If JavaFile.Bytes changed externally, reset JavaTextDoc and JFile
        if (_javaTextDoc != null) {
            String fileText = _javaFile.getText();
            String textDocText = _javaTextDoc.getString();
            if (!fileText.equals(textDocText)) {
                _javaTextDoc.setString(fileText);
                _javaTextDoc.setTextModified(false);
            }
        }

        // Clear JFile and external references
        clearExternalReferences();
    }

    /**
     * Returns the JavaAgent for given file.
     */
    public static JavaAgent getAgentForFile(WebFile aFile)
    {
        // Get JavaAgent for given source file - just return if found
        JavaAgent javaAgent = (JavaAgent) aFile.getProp(JavaAgent.class.getName());
        if (javaAgent != null)
            return javaAgent;

        // If java file, create and return
        String fileType = aFile.getFileType();
        if (fileType.equals("java") || fileType.equals("jepl") || fileType.equals("jmd"))
            return new JavaAgent(aFile);

        // Return not found
        return null;
    }

    /**
     * Returns the JavaAgent for given file.
     */
    public static JavaAgent getAgentForJavaFile(WebFile javaFile)
    {
        JavaAgent javaAgent = getAgentForFile(javaFile);
        assert (javaAgent != null);
        return javaAgent;
    }

    /**
     * Returns the default JEPL imports.
     */
    public static String[] getJeplDefaultImports()
    {
        if (_jeplImports != null) return _jeplImports;

        // Initialize imports
        List<String> imports = new ArrayList<>();
        imports.add("java.util.*");
        imports.add("java.util.function.*");
        imports.add("java.util.stream.*");
        imports.add("java.io.*");
        imports.add("java.nio.file.*");
        imports.add("snap.view.*");
        imports.add("snap.gfx.*");
        imports.add("snap.geom.*");
        imports.add("snap.viewx.Quick3D");
        imports.add("snap.viewx.QuickDraw");
        imports.add("snap.viewx.QuickDrawPen");
        imports.add("static snap.viewx.ConsoleIO.*");

        // Set array and return
        return _jeplImports = imports.toArray(new String[0]);
    }

    /**
     * Returns the default JEPL imports with charts support.
     */
    public static String[] getJeplDefaultImportsWithCharts()
    {
        if (_jeplImportsWithCharts != null) return _jeplImportsWithCharts;
        String[] jeplImports = getJeplDefaultImports();
        return _jeplImportsWithCharts = ArrayUtils.addAll(jeplImports, "snapcharts.data.*", "static snapcharts.charts.SnapCharts.*");
    }

    /**
     * Returns an "Unused Import" BuildIssue for given import decl.
     */
    private static BuildIssue createUnusedImportBuildIssue(WebFile javaFile, JImportDecl importDecl)
    {
        String msg = "The import " + importDecl.getName() + " is never used";
        int lineIndex = importDecl.getLineIndex();
        int startCharIndex = importDecl.getStartCharIndex();
        int endCharIndex = importDecl.getEndCharIndex();
        return new BuildIssue().init(javaFile, BuildIssue.Kind.Warning, msg, lineIndex, 0, startCharIndex, endCharIndex);
    }
}

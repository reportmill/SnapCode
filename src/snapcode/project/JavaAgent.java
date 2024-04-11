/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.TextDoc;
import snap.text.TextBlockUtils;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import java.util.*;

/**
 * This class holds a parsed Java file.
 */
public class JavaAgent {

    // The java file
    private WebFile _javaFile;

    // Whether Java file is really Java REPL (.jepl)
    private boolean _isJepl;

    // The Project that owns this file
    private Project _project;

    // The JavaTextDoc version of this file
    private JavaTextDoc _javaTextDoc;

    // The parsed version of this JavaFile
    protected JFile _jfile;

    // The external references in Java file
    private JavaDecl[] _externalRefs;

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
        _isJepl = aFile.getType().equals("jepl");

        // Set File JavaAgent property to this agent
        _javaFile.setProp(JavaAgent.class.getName(), this);

        // Start listening for file bytes changed
        _fileBytesChangedLsnr = this::fileBytesDidChange;
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
        _javaTextDoc = null;
        _externalRefs = null;
        _externalClassRefs = null;
    }

    /**
     * Returns whether file is really Jepl.
     */
    public boolean isJepl()  { return  _isJepl; }

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
        _javaTextDoc.readFromSourceURL(_javaFile.getURL());

        // Listen for changes
        _javaTextDoc.addPropChangeListener(pc -> javaTextDocDidPropChange(pc));

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
        if (_isJepl) {
            String className = getFile().getSimpleName();
            String[] importNames = getJeplDefaultImports();
            if (_project.getBuildFile().isIncludeSnapChartsRuntime())
                importNames = getJeplDefaultImportsWithCharts();
            String superClassName = "Object";
            jfile = javaParser.parseJeplFile(javaStr, className, importNames, superClassName);
        }
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
        // Get errors for body declarations
        JFile jFile = getJFile();
        NodeError[] errors = jFile.getDeclarationErrors();

        // If no declaration errors, reload class and do full error check
        if (errors.length == 0) {
            reloadClassFromClassDecl();
            errors = NodeError.getAllNodeErrors(jFile);
        }

        // Convert to BuildIssues and set in agent
        WebFile javaFile = getFile();
        BuildIssue[] buildIssues = ArrayUtils.map(errors, error -> BuildIssue.createIssueForNodeError(error, javaFile), BuildIssue.class);

        // Set build issues
        setBuildIssues(buildIssues);
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
    public JavaDecl[] getExternalReferences()
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
    }

    /**
     * Returns the external class references.
     */
    private Set<JavaClass> getExternalClassReferences()
    {
        if (_externalClassRefs != null) return _externalClassRefs;
        JavaDecl[] externalReferences = getExternalReferences();
        JavaClass[] externalClassReferences = ArrayUtils.filterByClass(externalReferences, JavaClass.class);
        return _externalClassRefs = new HashSet<>(Arrays.asList(externalClassReferences));
    }

    /**
     * Called when JavaTextDoc does prop change.
     */
    protected void javaTextDocDidPropChange(PropChange aPC)
    {
        // Get PropName
        String propName = aPC.getPropName();

        // Handle CharsChange: Try to update JFile with partial parse
        if (propName == TextDoc.Chars_Prop && _jfile != null) {
            TextBlockUtils.CharsChange charsChange = (TextBlockUtils.CharsChange) aPC;
            updateJFileForChange(charsChange);
        }

        // Handle TextModified: Register updater to update JavaFile before save
        else if (propName == TextDoc.TextModified_Prop) {
            boolean textDocTextModified = _javaTextDoc.isTextModified();
            WebFile javaFile = getFile();
            WebFile.Updater updater = textDocTextModified ? file -> updateFileFromTextDoc() : null;
            javaFile.setUpdater(updater);
        }
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
        _javaTextDoc.getUndoer().reset();
    }

    /**
     * Called when java file changes.
     */
    private void fileBytesDidChange(PropChange aPC)
    {
        // If JavaFile.Bytes changed externally, reset JavaTextDoc and JFile
        if (_javaTextDoc != null) {
            String fileText = _javaFile.getText();
            String textDocText = _javaTextDoc.getString();
            if (!fileText.equals(textDocText)) {
                _javaTextDoc.setString(fileText);
                _javaTextDoc.setTextModified(false);
                _javaTextDoc.getUndoer().reset();
            }
        }

        // Clear JFile and external references
        clearExternalReferences();
    }

    /**
     * Updates JFile incrementally if possible.
     */
    protected void updateJFileForChange(TextBlockUtils.CharsChange charsChange)
    {
        // If partial parse fails, clear JFile for full reparse
        boolean jfileUpdated = !_isJepl && JavaTextDocUtils.updateJFileForChange(_javaTextDoc, _jfile, charsChange);
        if (!jfileUpdated)
            _jfile = null;
        _externalRefs = null;
        _externalClassRefs = null;
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
        String fileType = aFile.getType();
        if (fileType.equals("java") || fileType.equals("jepl"))
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
        imports.add("snap.view.*");
        imports.add("snap.gfx.*");
        imports.add("snap.geom.*");
        imports.add("snap.viewx.Quick3D");
        imports.add("snap.viewx.QuickDraw");
        imports.add("snap.viewx.QuickDrawPen");
        imports.add("static snap.viewx.ConsoleX.*");

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
}

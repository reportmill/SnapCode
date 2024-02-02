/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.TextDoc;
import snap.text.TextBlockUtils;
import snap.util.ArrayUtils;
import snap.view.ViewUtils;
import snap.web.WebFile;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds a parsed Java file.
 */
public class JavaAgent {

    // The java file
    private WebFile  _file;

    // Whether Java file is really Java REPL (.jepl)
    private boolean _isJepl;

    // The Project that owns this file
    private Project  _proj;

    // The JavaTextDoc version of this file
    private JavaTextDoc  _javaTextDoc;

    // The parsed version of this JavaFile
    protected JFile  _jfile;

    // The JavaData for file (provides information on dependencies)
    private JavaData _javaData;

    // A listener for file prop changes
    private PropChangeListener _fileBytesChangedLsnr;

    // A runnable to check file for errors after delay
    private Runnable _checkFileRun = () -> checkFileForErrors();

    // The Jepl default imports
    private static String[] _jeplImports;

    /**
     * Constructor for given file.
     */
    private JavaAgent(WebFile aFile)
    {
        _file = aFile;
        _isJepl = aFile.getType().equals("jepl");

        // Set File JavaAgent property to this agent
        _file.setProp(JavaAgent.class.getName(), this);

        // Start listening for file bytes changed
        _fileBytesChangedLsnr = this::fileBytesDidChange;
        _file.addPropChangeListener(_fileBytesChangedLsnr, WebFile.Bytes_Prop);

        // Add to Project
        _proj = Project.getProjectForFile(_file);
        if (_proj != null)
            _proj.addJavaAgent(this);
        else System.out.println("JavaAgent.init: No project for java file");
    }

    /**
     * Closes this agent.
     */
    public void closeAgent()
    {
        // If already close, complain and return
        if (_file == null) { System.err.println("JavaAgent.closeAgent: Multiple closes"); return; }

        // Clear everything
        clearBuildIssues();
        _file.setProp(JavaAgent.class.getName(), null);
        _file.removePropChangeListener(_fileBytesChangedLsnr);
        _file.reset();
        _file = null; _proj = null; _jfile = null; _javaTextDoc = null; _javaData = null;
    }

    /**
     * Returns whether file is really Jepl.
     */
    public boolean isJepl()  { return  _isJepl; }

    /**
     * Returns the WebFile.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns the project for this JavaFile.
     */
    public Project getProject()  { return  _proj; }

    /**
     * Returns the workspace for this JavaFile.
     */
    public Workspace getWorkspace()
    {
        Project proj = getProject();
        return proj != null ? proj.getWorkspace() : null;
    }

    /**
     * Returns the JavaTextDoc for this JavaFile.
     */
    public JavaTextDoc getJavaTextDoc()
    {
        if (_javaTextDoc != null) return _javaTextDoc;

        // Create/load JavaTextDoc
        JavaTextDoc javaTextDoc = createJavaTextDoc();
        javaTextDoc.readFromSourceURL(_file.getURL());

        // Listen for changes
        javaTextDoc.addPropChangeListener(pc -> javaTextDocDidPropChange(pc));

        // Set, return
        return _javaTextDoc = javaTextDoc;
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
        String javaStr = getJavaText();

        // Parse file
        JFile jfile;
        if (_isJepl) {
            String className = getFile().getSimpleName();
            String[] importNames = getJeplDefaultImports();
            String superClassName = "Object";
            jfile = javaParser.parseJeplFile(javaStr, className, importNames, superClassName);
        }
        else jfile = javaParser.parseFile(javaStr);

        // Set SourceFile
        jfile.setSourceFile(_file);
        Project project = getProject();
        if (project != null)
            jfile.setResolverSupplier(() -> project.getResolver());

        // Return
        return jfile;
    }

    /**
     * Returns the JavaData.
     */
    public JavaData getJavaData()
    {
        if (_javaData != null) return _javaData;
        return _javaData = new JavaData(_file, _proj);
    }

    /**
     * Returns the build issues.
     */
    public BuildIssue[] getBuildIssues()
    {
        Workspace workspace = getWorkspace();
        if (workspace == null)
            return new BuildIssue[0];
        BuildIssues projBuildIssues = workspace.getBuildIssues();
        return projBuildIssues.getIssuesForFile(_file);
    }

    /**
     * Sets the build issues.
     */
    public void setBuildIssues(BuildIssue[] buildIssues)
    {
        // Get Workspace.BuildIssues and clear
        Workspace workspace = getWorkspace();
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
        // Get JFile errors
        JFile jFile = getJFile();
        NodeError[] errors = NodeError.getAllNodeErrors(jFile);

        // Convert to BuildIssues and set in agent
        WebFile javaFile = getFile();
        BuildIssue[] buildIssues = ArrayUtils.map(errors, error -> BuildIssue.createIssueForNodeError(error, javaFile), BuildIssue.class);

        // Set build issues
        setBuildIssues(buildIssues);
    }

    /**
     * Checks this file for errors after given delay.
     */
    public void checkFileForErrorsAfterDelay(int aDelay)
    {
        ViewUtils.runDelayedCancelPrevious(_checkFileRun, aDelay);

        // Clear build issues
        setBuildIssues(new BuildIssue[0]);
    }

    /**
     * Returns the string from Java file.
     */
    public String getJavaText()
    {
        if (_javaTextDoc != null)
            return _javaTextDoc.getString();
        return _file.getText();
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
     * Called when file changes.
     */
    private void fileBytesDidChange(PropChange aPC)
    {
        // If file.Bytes changed externally, reset JavaTextDoc and JFile
        if (_javaTextDoc != null) {
            String fileText = _file.getText();
            String textDocText = _javaTextDoc.getString();
            if (!fileText.equals(textDocText)) {
                _javaTextDoc.setString(fileText);
                _javaTextDoc.setTextModified(false);
                _javaTextDoc.getUndoer().reset();
                _jfile = null;
            }
        }
    }

    /**
     * Updates JFile incrementally if possible.
     */
    protected void updateJFileForChange(TextBlockUtils.CharsChange charsChange)
    {
        // If partial parse fails, clear JFile for full reparse
        boolean jfileUpdated = JavaTextDocUtils.updateJFileForChange(_javaTextDoc, _jfile, charsChange);
        if (!jfileUpdated)
            _jfile = null;
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
        imports.add("snapcharts.data.*");
        imports.add("snapcharts.repl.*");
        imports.add("static snapcharts.repl.ReplObject.*");
        imports.add("static snapcharts.repl.QuickCharts.*");
        imports.add("static snapcharts.repl.QuickData.*");
        return _jeplImports = imports.toArray(new String[0]);
    }
}

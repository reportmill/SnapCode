/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.props.PropChange;
import snap.text.TextDoc;
import snap.text.TextBlockUtils;
import snap.util.ArrayUtils;
import snap.view.ViewUtils;
import snap.web.WebFile;

/**
 * This class holds a parsed Java file.
 */
public class JavaAgent {

    // The java file
    private WebFile  _file;

    // The Project that owns this file
    private Project  _proj;

    // The JavaTextDoc version of this file
    private JavaTextDoc  _javaTextDoc;

    // The parser to parse Java
    private JavaParser  _javaParser;

    // The parsed version of this JavaFile
    protected JFile  _jfile;

    // A runnable to check file for errors after delay
    private Runnable _checkFileRun = () -> checkFileForErrors();

    /**
     * Constructor for given file.
     */
    public JavaAgent(WebFile aFile)
    {
        _file = aFile;
        aFile.addPropChangeListener(this::fileBytesDidChange, WebFile.Bytes_Prop);
    }

    /**
     * Returns the WebFile.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns the project for this JavaFile.
     */
    public Project getProject()
    {
        // If already set, just return
        if (_proj != null) return _proj;

        // Get, set, return
        Project proj = Project.getProjectForFile(_file);
        return _proj = proj;
    }

    /**
     * Returns the workspace for this JavaFile.
     */
    public Workspace getWorkspace()
    {
        Project proj = getProject();
        return proj.getWorkspace();
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
     * Returns the parser to parse java file.
     */
    public JavaParser getJavaParser()
    {
        // If already set, just return
        if (_javaParser != null) return _javaParser;

        // Create, set, return
        JavaParser javaParser = getJavaParserImpl();
        return _javaParser = javaParser;
    }

    /**
     * Returns the parser to parse java file.
     */
    protected JavaParser getJavaParserImpl()  { return JavaParser.getShared(); }

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
        JavaParser javaParser = getJavaParser();
        String javaStr = getJavaText();
        JFile jfile = javaParser.getJavaFile(javaStr);

        // Set SourceFile
        jfile.setSourceFile(_file);
        Project project = getProject();
        jfile.setResolverSupplier(() -> project.getResolver());

        // Return
        return jfile;
    }

    /**
     * Returns the build issues.
     */
    public BuildIssue[] getBuildIssues()
    {
        Workspace workspace = getWorkspace();
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
        // Get JavaAgent for given source file
        JavaAgent javaAgent = (JavaAgent) aFile.getProp(JavaAgent.class.getName());

        // If missing, create/set
        if (javaAgent == null) {

            // Create JavaAgent for java/jepl file
            if (aFile.getType().equals("java"))
                javaAgent = new JavaAgent(aFile);
            else if (aFile.getType().equals("jepl"))
                javaAgent = new JeplAgent(aFile);

            // Set agent as file property
            if (javaAgent != null)
                aFile.setProp(JavaAgent.class.getName(), javaAgent);
        }

        // Return
        return javaAgent;
    }
}

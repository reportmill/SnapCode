/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.parse.*;
import snap.props.PropChange;
import snap.text.TextDoc;
import snap.text.TextDocUtils;
import snap.util.TaskMonitor;
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

    /**
     * Constructor for given file.
     */
    public JavaAgent(WebFile aFile)
    {
        _file = aFile;
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

        // Return
        return jfile;
    }

    /**
     * Returns the parsed statements.
     */
    public JStmt[] getJFileStatements()
    {
        // Get main method
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        JMethodDecl mainMethod = classDecl.getMethodDeclForNameAndTypes("main", null);
        if (mainMethod == null)
            return null;

        // Get statements from main method
        return JavaTextDocUtils.getStatementsForJavaNode(mainMethod);
    }

    /**
     * Builds this file.
     */
    public boolean buildFile()
    {
        // Get ProjectBuilder and add build file
        Project proj = getProject();
        ProjectBuilder projectBuilder = proj.getBuilder();
        projectBuilder.addBuildFile(_file, true);

        // Build project
        TaskMonitor taskMonitor = new TaskMonitor.Text(System.out);
        boolean success = projectBuilder.buildProject(taskMonitor);
        return success;
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
            TextDocUtils.CharsChange charsChange = (TextDocUtils.CharsChange) aPC;
            updateJFileForChange(charsChange);
        }
    }

    /**
     * Updates JFile incrementally if possible.
     */
    protected void updateJFileForChange(TextDocUtils.CharsChange charsChange)
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
            if (aFile.getType().equals("jepl"))
                javaAgent = new JeplAgent(aFile);
            else javaAgent = new JavaAgent(aFile);
            aFile.setProp(JavaAgent.class.getName(), javaAgent);
        }

        // Return
        return javaAgent;
    }
}

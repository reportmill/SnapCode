/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import javakit.parse.*;
import snap.text.TextDocUtils;
import snap.util.ArrayUtils;
import snap.web.WebFile;

/**
 * This JavaAgent subclass supports Java REPL files.
 */
public class JeplAgent extends JavaAgent {

    /**
     * Constructor for given WebFile.
     */
    public JeplAgent(WebFile aFile)
    {
        super(aFile);
    }

    /**
     * Override to return as JeplTextDoc.
     */
    @Override
    public JeplTextDoc getJavaTextDoc()
    {
        return (JeplTextDoc) super.getJavaTextDoc();
    }

    /**
     * Creates the JavaTextDoc.
     */
    protected JavaTextDoc createJavaTextDoc()
    {
        return new JeplTextDoc();
    }

    /**
     * Returns the parser to parse java file.
     */
    @Override
    protected JavaParser getJavaParserImpl()
    {
        JeplTextDoc jeplTextDoc = getJavaTextDoc();
        return new JeplParser(jeplTextDoc);
    }

    /**
     * Override to fix incomplete var decls.
     */
    @Override
    protected JFile createJFile()
    {
        JFile jfile = super.createJFile();
        JeplParser.findAndFixIncompleteVarDecls(jfile);
        return jfile;
    }

    /**
     * Override to get statements from initializers.
     */
    @Override
    public JStmt[] getJFileStatements()
    {
        // Get JFile, ClassDecl (just return if not found)
        JFile jfile = getJFile();
        JClassDecl classDecl = jfile.getClassDecl();
        if (classDecl == null)
            return new JStmt[0];

        // Get initializers
        JInitializerDecl[] initDecls = classDecl.getInitDecls();
        JStmt[] stmtsAll = new JStmt[0];

        // Iterate over initializers and add statements
        for (JInitializerDecl initDecl : initDecls) {
            JStmt[] stmts = JavaTextDocUtils.getStatementsForJavaNode(initDecl);
            stmtsAll = ArrayUtils.addAll(stmtsAll, stmts);
        }

        // Return
        return stmtsAll;
    }

    /**
     * Override to just do full re-parse.
     */
    @Override
    protected void updateJFileForChange(TextDocUtils.CharsChange charsChange)
    {
        _jfile = null;
    }

    /**
     * Returns the JavaAgent for given file.
     */
    public static JeplAgent getAgentForFile(WebFile aFile)
    {
        return (JeplAgent) JavaAgent.getAgentForFile(aFile);
    }
}

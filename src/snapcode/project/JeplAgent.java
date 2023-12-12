/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.text.TextBlockUtils;
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
        String className = getFile().getSimpleName();
        String[] importNames = jeplTextDoc.getImports();
        String superClassName = jeplTextDoc.getSuperClassName();
        return new JeplParser(className, importNames, superClassName);
    }

    /**
     * Override to just do full re-parse.
     */
    @Override
    protected void updateJFileForChange(TextBlockUtils.CharsChange charsChange)
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

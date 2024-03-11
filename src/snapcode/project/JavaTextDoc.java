/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.JFile;
import snap.gfx.Font;
import snap.text.*;
import snap.web.WebFile;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The JavaAgent
    private JavaAgent  _javaAgent;

    // A code tokenizer
    private static JavaTextTokenizer JAVA_TOKENIZER;

    /**
     * Constructor.
     */
    protected JavaTextDoc()
    {
        super();

        // Get/set default font
        Font codeFont = JavaTextDocUtils.getDefaultJavaFont();
        setDefaultFont(codeFont);

        // Reset default LineStyle for code
        TextLineStyle lineStyle = getDefaultLineStyle();
        TextLineStyle lineStyleSpaced = lineStyle.copyFor(TextLineStyle.SPACING_KEY, 4);
        //double tabW = codeTextStyle.getCharAdvance(' ') * 4;
        //lineStyleSpaced.setTabs(new double[] { tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW });
        setDefaultLineStyle(lineStyleSpaced);

        // Create tokenizer
        if (JAVA_TOKENIZER == null)
            JAVA_TOKENIZER = new JavaTextTokenizer();
    }

    /**
     * Returns whether JavaTextDoc is really Jepl.
     */
    public boolean isJepl()  { return getAgent().isJepl(); }

    /**
     * Returns the JavaAgent.
     */
    public JavaAgent getAgent()
    {
        if (_javaAgent != null) return _javaAgent;
        WebFile sourceFile = getSourceFile();
        JavaAgent javaAgent = JavaAgent.getAgentForFile(sourceFile);
        return _javaAgent = javaAgent;
    }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        JavaAgent javaAgent = getAgent();
        return javaAgent.getJFile();
    }

    /**
     * Override to create tokens.
     */
    @Override
    protected TextToken[] createTokensForTextLine(TextLine aTextLine)
    {
        return JAVA_TOKENIZER.createTokensForTextLine(aTextLine);
    }

    /**
     * Returns a new JavaTextDoc for given source file.
     */
    public static JavaTextDoc getJavaTextDocForFile(WebFile sourceFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(sourceFile);
        return javaAgent.getJavaTextDoc();
    }
}

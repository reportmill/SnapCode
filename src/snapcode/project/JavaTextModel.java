/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.JFile;
import snap.gfx.Font;
import snap.parse.Tokenizer;
import snap.text.*;
import snap.web.WebFile;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextModel extends TextModel {

    // The JavaAgent
    private JavaAgent  _javaAgent;

    // The tokenizer
    private JavaTextTokenSource _tokenizer;

    /**
     * Constructor.
     */
    protected JavaTextModel(JavaAgent javaAgent)
    {
        super();
        _javaAgent = javaAgent;

        // Get/set default font
        Font codeFont = JavaTextUtils.getDefaultJavaFont();
        setDefaultFont(codeFont);

        // Reset default LineStyle for code
        TextLineStyle lineStyle = getDefaultLineStyle();
        TextLineStyle lineStyleSpaced = lineStyle.copyForPropKeyValue(TextLineStyle.Spacing_Prop, 4);
        //double tabW = codeTextStyle.getCharAdvance(' ') * 4;
        //lineStyleSpaced.setTabs(new double[] { tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW });
        setDefaultLineStyle(lineStyleSpaced);

        // Create tokenizer to provide tokens from Java text lines
        _tokenizer = new JavaTextTokenSource(this);
    }

    /**
     * Returns whether content is really Jepl.
     */
    public boolean isJepl()  { return getAgent().isJepl(); }

    /**
     * Returns whether content is really Java markdown.
     */
    public boolean isJMD()  { return getAgent().isJMD(); }

    /**
     * Returns the JavaAgent.
     */
    public JavaAgent getAgent()  { return _javaAgent; }

    /**
     * Returns the JFile (parsed Java file).
     */
    public JFile getJFile()
    {
        JavaAgent javaAgent = getAgent();
        return javaAgent.getJFile();
    }

    /**
     * Returns tokenizer that provides tokens from lines.
     */
    public Tokenizer getTokenSource()  { return _tokenizer; }

    /**
     * Override to create tokens.
     */
    @Override
    protected TextToken[] createTokensForTextLine(TextLine aTextLine)
    {
        return JavaTextTokenizer.SHARED.createTokensForTextLine(aTextLine);
    }

    /**
     * Returns a new JavaTextModel for given source file.
     */
    public static JavaTextModel getJavaTextModelForFile(WebFile sourceFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(sourceFile);
        return javaAgent.getJavaTextModel();
    }
}

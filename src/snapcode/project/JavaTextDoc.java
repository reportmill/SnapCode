/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.parse.*;
import snap.text.*;
import snap.util.ArrayUtils;
import snap.web.WebFile;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The JavaAgent
    private JavaAgent  _javaAgent;

    // A code tokenizer
    private static CodeTokenizer JAVA_TOKENIZER;

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
        if (JAVA_TOKENIZER == null) {
            JAVA_TOKENIZER = new CodeTokenizer();
            JAVA_TOKENIZER.setReadSingleLineComments(true);
            JAVA_TOKENIZER.setReadMultiLineComments(true);
            ParseRule rule = JavaParser.getShared().getRule();
            JAVA_TOKENIZER.addPatternsForRule(rule);
        }
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
        // Simple case
        if (aTextLine.isWhiteSpace())
            return new TextToken[0];

        // Create and return text tokens
        ParseToken[] parseTokens = JavaTextDocUtils.createParseTokensForTextLine(JAVA_TOKENIZER, aTextLine);
        TextStyle textStyle = aTextLine.getRun(0).getStyle();
        return ArrayUtils.map(parseTokens, pt -> createTextTokenForParseToken(pt, aTextLine, textStyle), TextToken.class);
    }

    /**
     * Returns a TextToken for given ParseToken.
     */
    private static TextToken createTextTokenForParseToken(ParseToken parseToken, TextLine aTextLine, TextStyle textStyle)
    {
        // Get token start/end
        int tokenStart = parseToken.getStartCharIndex();
        int tokenEnd = parseToken.getEndCharIndex();

        // Create TextToken
        TextToken textToken = new TextToken(aTextLine, tokenStart, tokenEnd, textStyle);
        textToken.setName(parseToken.getName());

        // Get/set token color
        Color color = JavaTextDocUtils.getColorForParseToken(parseToken);
        if (color != null)
            textToken.setTextColor(color);

        // Return
        return textToken;
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

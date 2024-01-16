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
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextDoc extends TextDoc {

    // The JavaAgent
    private JavaAgent  _javaAgent;

    /**
     * Constructor.
     */
    protected JavaTextDoc()
    {
        super();

        // Reset default TextStyle for code
        TextStyle textStyle = getDefaultStyle();
        Font codeFont = JavaTextDocUtils.getCodeFont();
        TextStyle codeTextStyle = textStyle.copyFor(codeFont);
        setDefaultStyle(codeTextStyle);

        // Reset default LineStyle for code
        TextLineStyle lineStyle = getDefaultLineStyle();
        TextLineStyle lineStyleSpaced = lineStyle.copyFor(TextLineStyle.SPACING_KEY, 4);
        //double tabW = codeTextStyle.getCharAdvance(' ') * 4;
        //lineStyleSpaced.setTabs(new double[] { tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW, tabW });
        setDefaultLineStyle(lineStyleSpaced);
    }

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

        // Get iteration vars
        List<TextToken> tokens = new ArrayList<>();
        TextRun textRun = aTextLine.getRun(0);

        // Get tokenizer
        JavaAgent javaAgent = getAgent();
        JavaParser javaParser = javaAgent.getJavaParser();
        CodeTokenizer tokenizer = javaParser.getTokenizer();

        // Get first token in line
        Exception exception = null;
        ParseToken parseToken = null;
        try { parseToken = JavaTextDocUtils.getNextToken(tokenizer, aTextLine); }
        catch (Exception e) {
            exception = e;
            System.out.println("JavaTextDoc.createTokensForTextLine: Parse error: " + e);
        }

        // Get line parse tokens and create TextTokens
        while (parseToken != null) {

            // Get token start/end
            int tokenStart = parseToken.getStartCharIndex();
            int tokenEnd = parseToken.getEndCharIndex();

            // Create TextToken
            TextToken textToken = new TextToken(aTextLine, tokenStart, tokenEnd, textRun);
            textToken.setName(parseToken.getName());
            tokens.add(textToken);

            // Get/set token color
            Color color = JavaTextDocUtils.getColorForParseToken(parseToken);
            if (color != null)
                textToken.setTextColor(color);

            // Get next token
            try { parseToken = JavaTextDocUtils.getNextToken(tokenizer, null); }
            catch (Exception e) {
                exception = e;
                parseToken = null;
                System.out.println("JavaTextDoc.createTokensForTextLine: Parse error: " + e);
            }
        }

        // If exception was hit, create token for rest of line
        if (exception != null) {
            int tokenStart = tokenizer.getCharIndex();
            int tokenEnd = aTextLine.length();
            TextToken textToken = new TextToken(aTextLine, tokenStart, tokenEnd, textRun);
            tokens.add(textToken);
        }

        // Return
        return tokens.toArray(new TextToken[0]);
    }

    /**
     * Returns a new JavaTextDoc for given source file.
     */
    public static JavaTextDoc getJavaTextDocForFile(WebFile sourceFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForFile(sourceFile);
        return javaAgent.getJavaTextDoc();
    }
}

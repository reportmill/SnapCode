/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import javakit.resolver.*;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.parse.Tokenizer;
import snap.text.*;

/**
 * This class holds the text of a Java file with methods to easily build.
 */
public class JavaTextModel extends TextModel {

    // The JavaAgent
    private JavaAgent  _javaAgent;

    // The tokenizer
    private JavaTextTokenSource _tokenizer;

    // Context for member coloring
    public static Color FIELD_COLOR = new Color("#7C1E8F"); // Purple
    public static Color METHOD_COLOR = new Color("#286077"); // Turquoise

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
     * Sets color of member id tokens.
     */
    public void setColorOfMemberIds()
    {
        setColorOfMemberIds(getJFile());
    }

    /**
     * Sets color of member id tokens.
     */
    private void setColorOfMemberIds(JNode aNode)
    {
        if (aNode instanceof JExprId idExpr) {
            JavaDecl javaDecl = idExpr.getDecl();
            if (javaDecl instanceof JavaField) {
                TextToken textToken = getTokenForIdNode(idExpr);
                if (textToken != null)
                    textToken.setColor(FIELD_COLOR);
            }
            else if (javaDecl instanceof JavaExecutable && idExpr.getParent() instanceof JExecutableDecl) {
                TextToken textToken = getTokenForIdNode(idExpr);
                if (textToken != null)
                    textToken.setColor(METHOD_COLOR);
            }
        }

        else aNode.getChildren().forEach(this::setColorOfMemberIds);
    }

    /**
     * Returns the text token for given id node.
     */
    public TextToken getTokenForIdNode(JExprId idExpr)
    {
        // If node is zero length, return null
        if (idExpr.getCharLength() == 0)
            return null;

        // Get line index (skip if negative - assume Repl import statement or something)
        int lineIndex = idExpr.getLineIndex();
        if (lineIndex < 0)
            return null;

        // Get node line, then token from line (faster than having to find line by node startCharIndex)
        TextLine textLine = getLine(lineIndex);
        int textLineStartCharIndex = textLine.getStartCharIndex();
        int nodeStartCharIndex = idExpr.getStartCharIndex();
        int tokenStartCharIndexInLine = nodeStartCharIndex - textLineStartCharIndex;
        TextToken token = textLine.getTokenForCharIndex(tokenStartCharIndexInLine);
        if (token == null) // Should be impossible
            System.out.println("JavaTextModel.getTokenForIdNode: Can't find token for matching node: " + idExpr);

        return token;
    }

    /**
     * Override to create tokens.
     */
    @Override
    protected TextToken[] createTokensForTextLine(TextLine aTextLine)
    {
        return JavaTextTokenizer.SHARED.createTokensForTextLine(aTextLine);
    }
}

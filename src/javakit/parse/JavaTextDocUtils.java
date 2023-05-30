/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.project.JavaAgent;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.parse.*;
import snap.text.TextDocUtils;
import snap.text.TextLine;
import snap.text.TextToken;
import snap.util.CharSequenceUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods and support for JavaTextDoc.
 */
public class JavaTextDocUtils {

    // The recommended default font for code
    private static Font _codeFont;

    // Constants for Syntax Coloring
    private static Color COMMENT_COLOR = new Color("#3F7F5F"); //336633
    private static Color RESERVED_WORD_COLOR = new Color("#660033");
    private static Color STRING_LITERAL_COLOR = new Color("#C80000"); // CC0000

    /**
     * Returns a suitable code font.
     */
    public static Font getCodeFont()
    {
        if (_codeFont == null) {
            String[] names = { "Monaco", "Consolas", "Courier"};
            for (String name : names) {
                _codeFont = new Font(name, 12);
                if (_codeFont.getFamily().startsWith(name))
                    break;
            }
        }

        // Return
        return _codeFont;
    }

    /**
     * Returns the next token for tokenizer and text line.
     */
    public static ParseToken getNextToken(CodeTokenizer aTokenizer, TextLine aTextLine)
    {
        // If TextLine provided, do set up
        if (aTextLine != null) {

            // If this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
            TextLine prevTextLine = aTextLine.getPrevious();
            TextToken prevTextLineLastToken = prevTextLine != null ? prevTextLine.getLastToken() : null;
            boolean inUnterminatedComment = isTextTokenUnterminatedMultilineComment(prevTextLineLastToken);

            // Reset input for Tokenizer
            aTokenizer.setInput(aTextLine);

            // Get first line token: Handle if already in Multi-line
            if (inUnterminatedComment)
                return aTokenizer.getMultiLineCommentTokenMore();
        }

        // Return next token
        return aTokenizer.getNextSpecialTokenOrToken();
    }

    /**
     * Returns whether given TextToken is an unterminated comment.
     */
    private static boolean isTextTokenUnterminatedMultilineComment(TextToken aTextToken)
    {
        if (aTextToken == null)
            return false;
        String name = aTextToken.getName();
        if (name != Tokenizer.MULTI_LINE_COMMENT)
            return false;
        String tokenStr = aTextToken.getString();
        if (tokenStr.endsWith("*/"))
            return false;
        return true;
    }

    /**
     * Checks the given token for syntax coloring.
     */
    public static Color getColorForParseToken(ParseToken aToken)
    {
        // Handle comments
        String tokenName = aToken.getName();
        if (tokenName == CodeTokenizer.SINGLE_LINE_COMMENT || tokenName == CodeTokenizer.MULTI_LINE_COMMENT)
            return COMMENT_COLOR;

        // Handle reserved words
        char firstPatternChar = aToken.getPattern().charAt(0);
        if (Character.isLetter(firstPatternChar))
            return RESERVED_WORD_COLOR;

        // Handle string literals
        if (tokenName == "StringLiteral" || tokenName == "CharacterLiteral")
            return STRING_LITERAL_COLOR;

        // Return none
        return null;
    }

    /**
     * Returns an array of statements in given node.
     */
    public static JStmt[] getStatementsForJavaNode(JNode aJNode)
    {
        List<JStmt> stmtsList = new ArrayList<>();
        findStatementsForJavaNode(aJNode, stmtsList);
        return stmtsList.toArray(new JStmt[0]);
    }

    /**
     * Recursively finds all statements in node and adds to given list.
     */
    private static void findStatementsForJavaNode(JNode aJNode, List<JStmt> theStmtsList)
    {
        // Handle statement node (but not block), get line index and set in array
        if (aJNode instanceof JStmt && !(aJNode instanceof JStmtBlock)) {
            theStmtsList.add((JStmt) aJNode);
            return;
        }

        // Handle any node: Iterate over children and recurse
        List<JNode> children = aJNode.getChildren();
        for (JNode child : children)
            findStatementsForJavaNode(child, theStmtsList);
    }

    /**
     * Updates JFile for given range change.
     */
    public static boolean updateJFileForChange(JavaTextDoc javaTextDoc, JFile aJFile, TextDocUtils.CharsChange aCharsChange)
    {
        // If no JFile, just bail
        if (aJFile == null) return true;

        // Get CharsChange and charIndex
        CharSequence addChars = aCharsChange.getNewValue();
        CharSequence removeChars = aCharsChange.getOldValue();
        int startCharIndex = aCharsChange.getIndex();
        int endOldCharIndex = startCharIndex + (addChars != null ? 0 : removeChars.length());

        // If change is more than 50 chars or contains newline, just reparse all
        CharSequence changeChars = addChars != null ? addChars : removeChars;
        if (changeChars.length() > 50 || CharSequenceUtils.indexOfNewline(changeChars, 0) >= 0)
            return false;

        // Get outer statement enclosing range
        JNode jnode = aJFile.getNodeAtCharIndex(startCharIndex);
        JStmtBlock oldStmt = jnode instanceof JStmtBlock ? (JStmtBlock) jnode : jnode.getParent(JStmtBlock.class);
        while (oldStmt != null && oldStmt.getEndCharIndex() < endOldCharIndex)
            oldStmt = oldStmt.getParent(JStmtBlock.class);

        // If enclosing statement not found, just reparse all
        if (oldStmt == null)
            return false;

        // If StmtParser not yet set, create
        if (_stmtParser == null) {
            JavaAgent javaAgent = javaTextDoc.getAgent();
            _stmtParser = new StmtParser(javaAgent.getJavaParser());
        }

        // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
        _stmtParser.setInput(javaTextDoc);
        _stmtParser.setCharIndex(oldStmt.getStartCharIndex());
        _stmtParser.getTokenizer().setLineIndex(oldStmt.getLineIndex());

        // Parse new statement
        JStmtBlock newStmt = null;
        try { newStmt = _stmtParser.parseCustom(JStmtBlock.class); }
        catch (Exception e) { }

        // If parse failed, return failed
        ParseToken endToken = newStmt != null ? newStmt.getEndToken() : null;
        if (endToken == null || !endToken.getPattern().equals(oldStmt.getEndToken().getPattern()))
            return false;

        // Replace old statement with new statement
        JNode stmtParent = oldStmt.getParent();
        if (stmtParent instanceof WithBlockStmt)
            ((WithBlockStmt) stmtParent).setBlock(newStmt);
        else System.err.println("JavaTextDocUtils.updateJFileForChange: Parent not WithBlockStmt: " + stmtParent);

        // Extend ancestor ends if needed
        JNode ancestor = stmtParent.getParent();
        while (ancestor != null) {
            if (ancestor.getEndToken() == null || ancestor.getEndCharIndex() < endToken.getEndCharIndex()) {
                ancestor.setEndToken(endToken);
                ancestor = ancestor.getParent();
            }
            else break;
        }

        // Return success
        return true;
    }


    // Special statement parser
    private static StmtParser _stmtParser;

    /**
     * A Parser for JavaText modified statements.
     */
    private static class StmtParser extends Parser {

        private JavaParser _javaParser;

        /** Constructor. */
        StmtParser(JavaParser javaParser)
        {
            super(javaParser.getRule("Statement"));
            _javaParser = javaParser;
        }

        /** Override to use JavaParser.Tokenizer. */
        public Tokenizer getTokenizer()
        {
            return _javaParser.getTokenizer();
        }

        /** Override to ignore exception. */
        protected void parseFailed(ParseRule aRule, ParseHandler aHandler)  { }
    }
}

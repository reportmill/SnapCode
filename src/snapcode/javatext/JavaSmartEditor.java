package snapcode.javatext;
import javakit.parse.*;
import snap.parse.*;
import snap.text.*;
import static snapcode.javatext.JavaTextArea.INDENT_STRING;

/**
 * Methods to do some smart java editing.
 */
class JavaSmartEditor {

    // The text adapter
    private JavaTextAdapter _javaTextAdapter;

    // The JavaTextArea
    JavaTextArea _javaTextArea;

    // The text model
    TextModel _textModel;

    /**
     * Constructor.
     */
    public JavaSmartEditor(JavaTextAdapter javaTextAdapter)
    {
        _javaTextAdapter = javaTextAdapter;
        _javaTextArea = (JavaTextArea) javaTextAdapter.getTextArea();
        _textModel = javaTextAdapter.getTextModel();
    }

    /**
     * Returns whether text selection is in or after a multi-line comment.
     */
    public boolean isEnteringMultilineComment()
    {
        TextLine textLine = getSel().getStartLine();
        int selStartInLine = getSelStart() - textLine.getStartCharIndex();
        TextToken lastToken = getLastTokenBeforeCharIndex(textLine, selStartInLine);
        return lastToken != null && lastToken.getName() == Tokenizer.MULTI_LINE_COMMENT;
    }

    /**
     * Adds a newline when text selection is in a multi-line comment.
     */
    public void addNewlineForMultilineComment()
    {
        // Get whether given line is start of multiline comment
        TextLine textLine = getSel().getStartLine();
        String lineString = textLine.getString().trim();
        boolean isStartOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");

        // Get whether already in multiline comment
        boolean isInMultiLineComment = isLineInMultiLineComment(textLine, lineString);

        // Get basic insert chars: newline + next line indent
        String indentStr = textLine.getIndentString();
        StringBuilder insertChars = new StringBuilder().append('\n').append(indentStr);

        // If start of multi-line comment, add " * "
        if (isStartOfMultiLineComment)
            insertChars.append(" * ");

            // If in multi-line comment, add "* "
        else if (isInMultiLineComment)
            insertChars.append("* ");

            // If after multi-line comment, remove space from indent
        else if (lineString.startsWith("*") && lineString.endsWith("*/"))
            insertChars.delete(insertChars.length() - 1, insertChars.length());

        // Add insert chars
        replaceChars(insertChars);

        // If start of multi-line comment, append comment terminator
        if (isStartOfMultiLineComment && !isInMultiLineComment) {
            String commentTerminatorStr = insertChars.substring(0, insertChars.length() - 1) + "/";
            _textModel.addChars(commentTerminatorStr, getSelStart());
        }
    }

    /**
     * Returns whether text selection is in a block statement (if, for, do, while).
     */
    public boolean isEnteringBlockStatement()
    {
        // Get last token on given line
        TextLine textLine = getSel().getStartLine();
        int selStart = getSelStart();
        int selStartInLine = selStart - textLine.getStartCharIndex();
        TextToken lastToken = getLastTokenBeforeCharIndex(textLine, selStartInLine);

        // If at beginning of line, check previous line
        if (lastToken == null && selStartInLine == 0 && textLine.getPrevious() != null)
            lastToken = textLine.getPrevious().getLastToken();

        // If no last token, return false
        if (lastToken == null)
            return false;

        // If last token is open bracket, return true
        String lastTokenString = lastToken.getString();
        if (lastTokenString.equals("{"))
            return true;

        // If last node is conditional (if, for, do, while), return true
        JNode lastNode = _javaTextArea.getNodeForCharIndex(lastToken.getEndCharIndex());
        if (lastNode instanceof JStmtConditional)
            return true;

        // Return false
        return false;
    }

    /**
     * Adds a newline when text selection is in a block statement (if, for, do, while).
     */
    public void addNewlineForBlockStatement()
    {
        // Create string for new line plus indent
        TextLine textLine = getSel().getStartLine();
        String indentStr = textLine.getIndentString();
        String insertChars = '\n' + indentStr + INDENT_STRING;
        int newSelStart = getSelStart() + insertChars.length();

        // If trailing white space, remove from insertChars
        int charIndexInLine = getSelStart() - textLine.getStartCharIndex();
        while (charIndexInLine < textLine.length() - 1 && Character.isWhitespace(textLine.charAt(charIndexInLine)) && insertChars.length() > 1) {
            insertChars = insertChars.substring(0, insertChars.length() - 1);
            charIndexInLine++;
        }

        // Do normal version
        replaceChars(insertChars);
        setSel(newSelStart);

        // If next token is close bracket, move it to next line and return
        TextLine nextLine = textLine.getNext();
        TextToken nextToken = nextLine.getTokenCount() > 0 ? nextLine.getToken(0) : null;
        if (nextToken != null && nextToken.getString().equals("}")) {
            _textModel.addChars('\n' + indentStr, newSelStart);
            return;
        }

        // If last token is unbalanced open bracket, proactively append close bracket
        TextToken textToken = textLine.getLastToken();
        if (isUnbalancedOpenBracketToken(textToken)) {
            String closeBracketStr = '\n' + indentStr + "}";
            _textModel.addChars(closeBracketStr, newSelStart);
        }
    }

    /**
     * Returns whether token is an open bracket and needs a close bracket.
     */
    private boolean isUnbalancedOpenBracketToken(TextToken textToken)
    {
        // If token isn't open bracket, return false
        if (textToken == null || !textToken.getString().equals("{"))
            return false;

        // Get node for text token
        JNode textTokenNode = _javaTextArea.getNodeForCharIndex(textToken.getStartCharIndex());

        // Iterate over node and parents to see if any is unbalanced block
        for (JNode node = textTokenNode; node != null; node = node.getParent()) {

            // If node is open bracket (or class decl), return true if no close bracket
            if (node.getStartToken().getString().equals("{") || node instanceof JClassDecl) {

                // If node end token isn't close bracket return unbalanced
                ParseToken nodeEndToken = node.getEndToken();
                if (!nodeEndToken.getString().equals("}"))
                    return true;

                // Skip initializer since it does share last child (JStmtBlock) token
                if (node instanceof JInitializerDecl)
                    continue;

                // If node end token is really it's last child end token, return unbalanced
                JNode nodeLastChild = node.getLastChild();
                if (nodeLastChild != null && nodeEndToken == nodeLastChild.getEndToken())
                    return true;
            }
        }

        // Return not unbalanced
        return false;
    }

    /**
     * Returns the last token before given char index.
     */
    private static TextToken getLastTokenBeforeCharIndex(TextLine textLine, int charIndex)
    {
        // Iterate over line tokens (backwards) and return first token that starts at or before char index
        TextToken[] tokens = textLine.getTokens();
        for (int i = tokens.length - 1; i >= 0; i--) {
            TextToken token = tokens[i];
            if (charIndex > token.getStartCharIndexInLine())
                return token;
        }

        // Return not found
        return null;
    }

    /**
     * Returns whether given line is in a multi-line comment.
     */
    private static boolean isLineInMultiLineComment(TextLine textLine, String lineString)
    {
        boolean isInMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");

        if (!isInMultiLineComment) {
            TextLine nextLine = textLine.getNext();
            while (nextLine != null && nextLine.getTokenCount() == 0) nextLine = nextLine.getNext();
            if (nextLine != null) {
                TextToken nextToken = nextLine.getToken(0);
                isInMultiLineComment = nextToken.getName() == Tokenizer.MULTI_LINE_COMMENT && !nextToken.getString().startsWith("/*");
            }
        }

        return isInMultiLineComment;
    }

    // Conveniences
    private void replaceChars(CharSequence insertChars)  { _javaTextAdapter.replaceChars(insertChars); }
    private TextSel getSel()  { return _javaTextAdapter.getSel(); }
    private void setSel(int sel)  { _javaTextAdapter.setSel(sel); }
    private int getSelStart()  { return _javaTextAdapter.getSelStart(); }
}

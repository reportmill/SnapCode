/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import static snapcode.javatext.JavaTextArea.INDENT_STRING;
import static snapcode.javatext.JavaTextArea.INDENT_LENGTH;
import snap.parse.ParseToken;
import snap.parse.Tokenizer;
import snap.text.TextAdapter;
import snap.text.TextModel;
import snap.text.TextLine;
import snap.text.TextToken;
import snap.view.KeyCode;
import snap.view.ViewEvent;
import snap.view.ViewUtils;
import snapcode.project.JavaTextModel;

/**
 * This class is a helper for JavaTextArea to handle key processing.
 */
public class JavaTextAdapter extends TextAdapter {

    // The JavaTextArea
    private JavaTextArea  _javaTextArea;

    /**
     * Constructor.
     */
    public JavaTextAdapter(TextModel textModel, JavaTextArea javaTextArea)
    {
        super(textModel);
        _javaTextArea = javaTextArea;
    }

    /**
     * Override to add more shortcut key features
     */
    @Override
    protected void handleShortcutKeyPressEvent(ViewEvent anEvent)
    {
        switch (anEvent.getKeyCode()) {

            // Shortcut + Slash
            case KeyCode.SLASH -> { _javaTextArea.commentLinesWithLineComment(); anEvent.consume(); }

            // Shortcut + D
            case KeyCode.D -> { duplicate(); anEvent.consume(); }

            // Do normal version
            default -> super.handleShortcutKeyPressEvent(anEvent);
        }
    }

    /**
     * Called when a key is typed.
     */
    @Override
    public void handleKeyTypeEvent(ViewEvent anEvent)
    {
        // Get keyChar - if undefined or control char, just return
        char keyChar = anEvent.getKeyChar();
        if (keyChar == KeyCode.CHAR_UNDEFINED  || Character.isISOControl(keyChar))
            return;

        // If shortcut or control key down, just return
        if (anEvent.isShortcutDown() || anEvent.isControlDown())
            return;

        // Handle paired chars
        if (isSelEmpty()) {

            // Handle closer char: If next char is identical closer, assume this char is redundant and just return
            // TODO: Don't do if in string/comment - but should also work if chars are no longer adjacent
            if (isPairedCharCloserRedundant(keyChar)) {
                setSel(getSelStart() + 1);
                anEvent.consume();
                return;
            }
        }

        // Do normal version
        super.handleKeyTypeEvent(anEvent);

        // If opener char, insert closer char
        boolean isPairedOpener = isPairedCharOpener(keyChar);
        if (isPairedOpener)
            handlePairedCharOpener(keyChar);

        // Handle open bracket: If empty line and indented more than previous line, remove level of indent
        if (keyChar == '{') {
            TextLine thisLine = getSel().getStartLine();
            if (thisLine.getString().trim().equals("{") && thisLine.length() >= INDENT_LENGTH) {
                TextLine prevLine = thisLine.getPrevious();
                if (prevLine != null && thisLine.getIndentLength() >= prevLine.getIndentLength() + INDENT_LENGTH)
                    removeIndentLevelFromLine(thisLine);
            }
        }

        // Handle close bracket: Remove level of indent
        else if (keyChar == '}') {

            // Get indent for this line and next
            TextLine thisLine = getSel().getStartLine();
            TextLine prevLine = thisLine.getPrevious();
            int thisIndent = thisLine.getIndentLength();
            int prevIndent = prevLine != null ? prevLine.getIndentLength() : 0;

            // If this line starts with close bracket and indent is too much, remove indent level
            if (thisLine.getString().trim().startsWith("}") && thisIndent > prevIndent && thisIndent > INDENT_LENGTH)
                removeIndentLevelFromLine(thisLine);
        }
    }

    /**
     * Override to do special enter key processing.
     */
    @Override
    protected void handleEnterKeyPressEvent(ViewEvent anEvent)
    {
        // If not empty selection, just do normal version
        if (!isSelEmpty()) {
            super.handleEnterKeyPressEvent(anEvent);
            return;
        }

        // If shift or shortcut key down, just select line end and return
        if (ViewUtils.isShiftDown() || ViewUtils.isShortcutDown()) {
            int lineEndCharIndex = getSel().getLineEnd();
            setSel(lineEndCharIndex);
            return;
        }

        // Get line and its indent
        TextLine textLine = getSel().getStartLine();

        // If entering a multi-line comment, handle special
        if (isEnteringMultilineComment(textLine)) {
            processNewlineForMultilineComment(textLine);
            return;
        }

        // If entering a block statement, handle special
        if (isEnteringBlockStatement(textLine)) {
            processNewlineForBlockStatement(textLine);
            return;
        }

        // Get indent string for new line
        String indentStr = textLine.getIndentString();

        // If leaving conditional (if, for, do, while) without brackets, remove level of indent
        JNode selNode = _javaTextArea.getSelNode();
        JStmtConditional selNodeParent = selNode != null ? selNode.getParent(JStmtConditional.class) : null;
        if (selNodeParent != null &&  !(selNodeParent.getStatement() instanceof JStmtBlock)) {
            if (indentStr.length() >= INDENT_LENGTH)
                indentStr = indentStr.substring(INDENT_LENGTH);
        }

        // Get insert chars: Usually just newline + indent (when at line end or inside line text)
        String insertChars = '\n' + indentStr;

        // If text selection is inside line indent, shift newline inside indent
        int selStartInLine = getSelStart() - textLine.getStartCharIndex();
        if (selStartInLine < indentStr.length())
            insertChars = indentStr.substring(selStartInLine) + '\n' + indentStr.substring(0, selStartInLine);

        // Do normal version
        replaceChars(insertChars);
    }

    /**
     * Returns whether this line is processing a multi line comment.
     */
    private boolean isEnteringMultilineComment(TextLine aTextLine)
    {
        int selStartInLine = getSelStart() - aTextLine.getStartCharIndex();
        TextToken lastToken = getLastTokenBeforeCharIndex(aTextLine, selStartInLine);
        return lastToken != null && lastToken.getName() == Tokenizer.MULTI_LINE_COMMENT;
    }

    /**
     * Process newline key event.
     */
    protected void processNewlineForMultilineComment(TextLine aTextLine)
    {
        // Get whether given line is start of multiline comment
        String lineString = aTextLine.getString().trim();
        boolean isStartOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");

        // Get whether already in multiline comment
        boolean isInMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");
        if (!isInMultiLineComment) {
            TextLine nextLine = aTextLine.getNext();
            if (nextLine != null && nextLine.getTokenCount() > 0)
                isInMultiLineComment = nextLine.getToken(0).getName() == Tokenizer.MULTI_LINE_COMMENT;
        }

        // Get whether current line is end of multiline comment
        boolean isEndMultiLineComment = lineString.startsWith("*") && lineString.endsWith("*/");

        // Create indent string
        String indentStr = aTextLine.getIndentString();
        StringBuilder sb = new StringBuilder().append('\n').append(indentStr);

        // If start of multi-line comment, add " * "
        if (isStartOfMultiLineComment)
            sb.append(" * ");

        // If in multi-line comment, add "* "
        else if (isInMultiLineComment)
            sb.append("* ");

        // If after multi-line comment, remove space from indent
        else if (isEndMultiLineComment) {
            if (!sb.isEmpty())
                sb.delete(sb.length() - 1, sb.length());
        }

        // Do normal version
        replaceChars(sb.toString());

        // If start of multi-line comment, append terminator
        if (isStartOfMultiLineComment && !isInMultiLineComment) {
            int start = getSelStart();
            String str = sb.substring(0, sb.length() - 1) + "/";
            _textModel.addChars(str, start);
            setSel(start);
        }
    }

    /**
     * Returns whether this line is in process of entering a block statement (if, for, do, while).
     */
    private boolean isEnteringBlockStatement(TextLine aTextLine)
    {
        // Get last token on given line
        int selStart = getSelStart();
        int selStartInLine = selStart - aTextLine.getStartCharIndex();
        TextToken lastToken = getLastTokenBeforeCharIndex(aTextLine, selStartInLine);

        // If at beginning of line, check previous line
        if (lastToken == null && selStartInLine == 0 && aTextLine.getPrevious() != null)
            lastToken = aTextLine.getPrevious().getLastToken();

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
     * Process newline key event.
     */
    protected void processNewlineForBlockStatement(TextLine aTextLine)
    {
        // Create string for new line plus indent
        String indentStr = aTextLine.getIndentString();
        String insertChars = '\n' + indentStr + INDENT_STRING;
        int newSelStart = getSelStart() + insertChars.length();

        // If trailing white space, remove from insertChars
        int charIndexInLine = getSelStart() - aTextLine.getStartCharIndex();
        while (charIndexInLine < aTextLine.length() - 1 && Character.isWhitespace(aTextLine.charAt(charIndexInLine)) && insertChars.length() > 1) {
            insertChars = insertChars.substring(0, insertChars.length() - 1);
            charIndexInLine++;
        }

        // Do normal version
        replaceChars(insertChars);
        setSel(newSelStart);

        // If next token is close bracket, move it to next line and return
        TextLine nextLine = aTextLine.getNext();
        TextToken nextToken = nextLine.getTokenCount() > 0 ? nextLine.getToken(0) : null;
        if (nextToken != null && nextToken.getString().equals("}")) {
            _textModel.addChars('\n' + indentStr, newSelStart);
            return;
        }

        // If last token is unbalanced open bracket, proactively append close bracket
        TextToken textToken = aTextLine.getLastToken();
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
     * Override to handle backspace for paired chars.
     */
    @Override
    protected void handleBackSpaceKeyPressEvent(ViewEvent anEvent)
    {
        if (getSel().getSize() <= 1) {
            int prevCharIndex = getSelStart();
            if (isSelEmpty())
                prevCharIndex--;
            char prevChar = prevCharIndex >= 0 && prevCharIndex + 1 < length() ? charAt(prevCharIndex) : 0;
            if (isPairedCharOpener(prevChar)) {
                char nextChar = prevChar != 0 ? charAt(prevCharIndex + 1) : 0;
                char closeChar = getPairedCharForOpener(prevChar);
                if (nextChar == closeChar)
                    _textModel.removeChars(prevCharIndex + 1, prevCharIndex + 2);
            }
        }

        // Do normal version
        super.handleBackSpaceKeyPressEvent(anEvent);
    }

    /**
     * Override to have tab key handle indent/outdent.
     */
    @Override
    protected void handleTabKeyPressEvent(ViewEvent anEvent)
    {
        if (!anEvent.isShiftDown())
            _javaTextArea.indentLines();
        else _javaTextArea.outdentLines();
        anEvent.consume();
    }

    /**
     * Returns whether given char is paired char opener.
     */
    public boolean isPairedCharOpener(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == '(' || aChar == '[' || aChar == '{';
    }

    /**
     * Returns whether given char is paired char closer.
     */
    public boolean isPairedCharCloser(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == ')' || aChar == ']' || aChar == '}';
    }

    /**
     * Returns the paired closer char for given opener char.
     */
    public char getPairedCharForOpener(char openerChar)
    {
        return switch (openerChar) {
            case '\'', '"' -> openerChar;
            case '(' -> ')';
            case '[' -> ']';
            case '{' -> '}';
            default -> throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharCloser: Illegal char: " + openerChar);
        };
    }

    /**
     * Handles paired char opener: Insert close char as convenience.
     */
    public void handlePairedCharOpener(char aChar)
    {
        // If open bracket
        if (aChar == '{') {
            int bracketCharIndex = getSelStart() - 1;
            JNode bracketNode = _javaTextArea.getNodeForCharIndex(bracketCharIndex);
            JNode bracketNodeParent = bracketNode.getParent();
            if (bracketNodeParent instanceof JStmt && bracketNodeParent instanceof WithBlockStmt)
                return;
        }

        // If quote, just bail if prev char is quote
        if (aChar == '"') {
            int selStart = getSelStart() - 2;
            if (selStart > 0 && charAt(selStart) == '"')
                return;
        }

        // Add close char after char
        String closeCharStr = String.valueOf(getPairedCharForOpener(aChar));
        int selStart = getSelStart();
        _textModel.addChars(closeCharStr, selStart);
    }

    /**
     * Handles paired char closer: Avoid redundancy of user closing already closed pair.
     */
    public boolean isPairedCharCloserRedundant(char keyChar)
    {
        // If not paired char closer, return false
        if (!isPairedCharCloser(keyChar))
            return false;

        // Get previous char (just return if not identical)
        int start = getSelStart();
        if (start >= length())
            return false;
        char nextChar = charAt(start);
        if (keyChar != nextChar)
            return false;

        // If quote char, return whether we are in literal
        if (keyChar == '\'' || keyChar == '"') {
            JNode selNode = _javaTextArea.getSelNode();
            return selNode instanceof JExprLiteral;
        }

        // Return true
        return true;
    }

    /**
     * Remove indent level from line.
     */
    private void removeIndentLevelFromLine(TextLine textLine)
    {
        int deleteStartCharIndex = textLine.getStartCharIndex();
        int deleteEndCharIndex = deleteStartCharIndex + INDENT_LENGTH;
        _javaTextArea.getTextModel().removeChars(deleteStartCharIndex, deleteEndCharIndex);
    }

    /**
     * Duplicates the current selection - or line if selection is empty.
     */
    public void duplicate()
    {
        int selStart = getSelStart();
        int selEnd = getSelEnd();

        // If selection, duplicate selected string
        if (selStart != selEnd) {
            String selString = getSel().getString();
            _textModel.addChars(selString, selEnd);
            setSel(selEnd, selEnd + selString.length());
        }

        // If empty selection, duplicate line
        else {

            // Get selected line as string (with newline)
            TextLine selLine = getSel().getStartLine();
            String lineString = selLine.getString();
            if (!selLine.isLastCharNewline())
                lineString = '\n' + lineString;

            // Add line string to end of selLine
            int endCharIndex = selLine.getEndCharIndex();
            _textModel.addChars(lineString, endCharIndex);

            // Select start char of new line
            TextLine newLine = selLine.getNext();
            int selStart2 = newLine.getStartCharIndex() + newLine.getIndentLength();
            setSel(selStart2);
        }
    }

    /**
     * Override to remove extra indent from pasted strings.
     */
    @Override
    public void replaceCharsWithContent(Object theContent)
    {
        // If String, trim extra indent
        if (theContent instanceof String) {
            JavaTextModel javaTextModel = (JavaTextModel) getTextModel();
            if (javaTextModel.isJepl())
                theContent = JavaTextUtils.removeExtraIndentFromString((String) theContent);
        }

        // Do normal version
        super.replaceCharsWithContent(theContent);
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
}

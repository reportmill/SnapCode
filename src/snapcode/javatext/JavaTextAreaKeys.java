/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import static snapcode.javatext.JavaTextArea.INDENT_STRING;

import snap.parse.ParseToken;
import snap.parse.Tokenizer;
import snap.text.TextLine;
import snap.text.TextToken;
import snap.view.KeyCode;
import snap.view.TextAreaKeys;
import snap.view.ViewEvent;

/**
 * This class is a helper for JavaTextArea to handle key processing.
 */
public class JavaTextAreaKeys extends TextAreaKeys {

    // The JavaTextArea
    private JavaTextArea  _javaTextArea;

    /**
     * Constructor.
     */
    public JavaTextAreaKeys(JavaTextArea aJTA)
    {
        super(aJTA);
        _javaTextArea = aJTA;
    }

    /**
     * Called when a key is pressed.
     */
    @Override
    protected void keyPressed(ViewEvent anEvent)
    {
        // Get event info
        int keyCode = anEvent.getKeyCode();
        boolean shortcutDown = anEvent.isShortcutDown();
        boolean shiftDown = anEvent.isShiftDown();

        // Handle tab
        if (keyCode == KeyCode.TAB) {
            if (!anEvent.isShiftDown())
                _javaTextArea.indentLines();
            else _javaTextArea.outdentLines();
            anEvent.consume();
            return;
        }

        // Handle newline special
        if (keyCode == KeyCode.ENTER && isSelEmpty()) {
            if (shiftDown || shortcutDown)
                _textArea.selectLineEnd();
            else processNewline();
            anEvent.consume();
            return;
        }

        // Handle delete of adjacent paired chars (parens, quotes, square brackets) - TODO: don't do if in string/comment
        boolean isDelete = keyCode == KeyCode.BACK_SPACE || shortcutDown && !shiftDown && keyCode == KeyCode.X;
        if (isDelete && getSel().getSize() <= 1) {
            int start = getSelStart();
            if (isSelEmpty())
                start--;
            char char1 = start >= 0 && start + 1 < length() ? charAt(start) : 0;
            if (isPairedCharOpener(char1)) {
                char char2 = char1 != 0 ? charAt(start + 1) : 0;
                char closeChar = getPairedCharForOpener(char1);
                if (char2 == closeChar)
                    _textArea.delete(start + 1, start + 2, false);
            }
        }

        // Handle Shortcut keys
        if (shortcutDown) {

            switch (keyCode) {

                // Shortcut + Slash
                case KeyCode.SLASH: {
                    _javaTextArea.commentLinesWithLineComment();
                    anEvent.consume();
                    return;
                }

                // Shortcut + D
                case KeyCode.D: duplicate(); anEvent.consume(); return;
            }
        }

        // Do normal version
        super.keyPressed(anEvent);
    }

    /**
     * Called when a key is typed.
     */
    @Override
    protected void keyTyped(ViewEvent anEvent)
    {
        // Get keyChar - if undefined or control char, just return
        char keyChar = anEvent.getKeyChar();
        if (keyChar == KeyCode.CHAR_UNDEFINED  || Character.isISOControl(keyChar))
            return;

        // If shortcut or control key down, just return
        boolean commandDown = anEvent.isShortcutDown();
        boolean controlDown = anEvent.isControlDown();
        if (commandDown || controlDown)
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

            // Handle open bracket: If empty line and indented more than previous line, remove level of indent
            if (keyChar == '{') {
                TextLine thisLine = getSel().getStartLine();
                if (thisLine.isWhiteSpace() && thisLine.length() >= INDENT_STRING.length()) {
                    TextLine prevLine = thisLine.getPrevious();
                    if (prevLine != null && thisLine.getIndentLength() >= prevLine.getIndentLength() + INDENT_STRING.length()) {
                        int start = getSelStart();
                        _javaTextArea.delete(thisLine.getStartCharIndex(), thisLine.getStartCharIndex() + 4, false);
                        setSel(start - 4);
                    }
                }
            }
        }

        // Do normal version
        super.keyTyped(anEvent);

        // If opener char, insert closer char
        boolean isPairedOpener = isPairedCharOpener(keyChar);
        if (isPairedOpener)
            handlePairedCharOpener(keyChar);

        // Handle close bracket: Remove level of indent
        if (keyChar == '}') {

            // Get indent for this line and next
            TextLine thisLine = getSel().getStartLine();
            TextLine prevLine = thisLine.getPrevious();
            int thisIndent = thisLine.getIndentLength();
            int prevIndent = prevLine != null ? prevLine.getIndentLength() : 0;

            // If this line starts with close bracket and indent is too much, remove indent level
            if (thisLine.getString().trim().startsWith("}") && thisIndent > prevIndent && thisIndent > 4) {
                int thisLineStart = thisLine.getStartCharIndex();
                int deleteIndentEnd = thisLineStart + (thisIndent - prevIndent);
                _textArea.delete(thisLineStart, deleteIndentEnd, false);
                setSel(getSelStart() - 4);
            }
        }
    }

    /**
     * Process newline key event.
     */
    protected void processNewline()
    {
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
            if (indentStr.length() >= INDENT_STRING.length())
                indentStr = indentStr.substring(INDENT_STRING.length());
        }

        // Get insert chars: Usually just newline + indent (when at line end or inside line text)
        String insertChars = '\n' + indentStr;

        // If text selection is inside line indent, shift newline inside indent
        int selStartInLine = getSelStart() - textLine.getStartCharIndex();
        if (selStartInLine < indentStr.length())
            insertChars = indentStr.substring(selStartInLine) + '\n' + indentStr.substring(0, selStartInLine);

        // Do normal version
        _textArea.replaceChars(insertChars);
    }

    /**
     * Returns whether this line is processing a multi line comment.
     */
    private boolean isEnteringMultilineComment(TextLine aTextLine)
    {
        TextToken lastToken = aTextLine.getLastToken();
        return lastToken != null && lastToken.getName() == Tokenizer.MULTI_LINE_COMMENT;
    }

    /**
     * Process newline key event.
     */
    protected void processNewlineForMultilineComment(TextLine aTextLine)
    {
        String lineString = aTextLine.getString().trim();
        boolean isStartOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");
        boolean isInMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");
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
            if (sb.length() > 0)
                sb.delete(sb.length() - 1, sb.length());
        }

        // Do normal version
        _textArea.replaceChars(sb.toString());

        // If start of multi-line comment, append terminator
        if (isStartOfMultiLineComment) {
            int start = getSelStart();
            String str = sb.substring(0, sb.length() - 1) + "/";
            _textArea.replaceChars(str, null, start, start, false);
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
        _textArea.replaceChars(insertChars);
        _textArea.setSel(newSelStart);

        // If last token is unbalanced open bracket, proactively append close bracket
        TextToken textToken = aTextLine.getLastToken();
        if (isUnbalancedOpenBracketToken(textToken)) {
            String closeBracketStr = '\n' + indentStr + "}";
            int selStart = aTextLine.getNext().getEndCharIndex() - 1;
            _textArea.replaceChars(closeBracketStr, null, selStart, selStart, false);
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
     * Returns whether given char is paired char opener.
     */
    public boolean isPairedCharOpener(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == '(' || aChar == '[';
    }

    /**
     * Returns whether given char is paired char closer.
     */
    public boolean isPairedCharCloser(char aChar)
    {
        return aChar == '\'' || aChar == '"' || aChar == ')' || aChar == ']';
    }

    /**
     * Returns the paired closer char for given opener char.
     */
    public char getPairedCharForOpener(char openerChar)
    {
        switch (openerChar) {
            case '\'':
            case '"': return openerChar;
            case '(': return ')';
            case '[': return ']';
            default: throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharCloser: Illegal char: " + openerChar);
        }
    }

    /**
     * Handles paired char opener: Insert close char as convenience.
     */
    public void handlePairedCharOpener(char aChar)
    {
        String closer = String.valueOf(getPairedCharForOpener(aChar));

        // Add closer char
        int i = _textArea.getSelStart();
        _textArea.replaceChars(closer, null, i, i, false);
        _textArea.setSel(i);
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
     * Duplicates the current selection - or line if selection is empty.
     */
    public void duplicate()
    {
        int selStart = _textArea.getSelStart();
        int selEnd = _textArea.getSelEnd();

        // If selection, duplicate selected string
        if (selStart != selEnd) {
            String selString = _textArea.getSel().getString();
            _textArea.addChars(selString, null, selEnd);
            _textArea.setSel(selEnd, selEnd + selString.length());
        }

        // If empty selection, duplicate line
        else {

            // Get selected line as string (with newline)
            TextLine selLine = _textArea.getSel().getStartLine();
            String lineString = selLine.getString();
            if (!selLine.isLastCharNewline())
                lineString = '\n' + lineString;

            // Add line string to end of selLine
            int endCharIndex = selLine.getEndCharIndex();
            _textArea.addChars(lineString, null, endCharIndex);

            // Select start char of new line
            TextLine newLine = selLine.getNext();
            int selStart2 = newLine.getStartCharIndex() + newLine.getIndentLength();
            _textArea.setSel(selStart2);
        }
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

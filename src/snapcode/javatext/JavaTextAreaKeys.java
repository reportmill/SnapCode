/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;

import static snapcode.javatext.JavaTextArea.INDENT_STRING;

import snap.parse.CodeTokenizer;
import snap.text.TextBoxLine;
import snap.text.TextBoxToken;
import snap.view.KeyCode;
import snap.view.TextAreaKeys;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

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
        boolean commandDown = anEvent.isShortcutDown();
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
            if (anEvent.isShiftDown())
                _textArea.selectLineEnd();
            processNewline();
            anEvent.consume();
            return;
        }

        // Handle delete of adjacent paired chars (parens, quotes, square brackets) - TODO: don't do if in string/comment
        boolean isDelete = keyCode == KeyCode.BACK_SPACE || commandDown && !shiftDown && keyCode == KeyCode.X;
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

        // Handle command Slash
        if (commandDown) {
            if (keyCode == KeyCode.SLASH) {
                _javaTextArea.commentLinesWithLineComment();
                anEvent.consume();
                return;
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
        // Get event info
        char keyChar = anEvent.getKeyChar();
        if (keyChar == KeyCode.CHAR_UNDEFINED) return;
        boolean charDefined = !Character.isISOControl(keyChar);
        boolean commandDown = anEvent.isShortcutDown();
        boolean controlDown = anEvent.isControlDown();

        // Handle paired chars
        if (charDefined && !commandDown && !controlDown && isSelEmpty()) {

            // Handle closer char: If next char is identical closer, assume this char is redundant and just return
            // TODO: Don't do if in string/comment - but should also work if chars are no longer adjacent
            if (isPairedCharCloserRedundant(keyChar)) {
                setSel(getSelStart() + 1);
                anEvent.consume();
                return;
            }

            // Handle open bracket: If empty line, remove level of indent
            if (keyChar == '{') {
                TextBoxLine thisLine = getSel().getStartLine();
                String thisLineStr = thisLine.getString();
                if (thisLineStr.trim().length() == 0 && thisLineStr.length() >= 4) {
                    int start = getSelStart();
                    _javaTextArea.delete(thisLine.getStartCharIndex(), thisLine.getStartCharIndex() + 4, false);
                    setSel(start - 4);
                }
            }
        }

        // Do normal version
        super.keyTyped(anEvent);

        // Handle paired chars
        if (charDefined && !commandDown && !controlDown) {

            // If opener char, insert closer char
            boolean isPairedOpener = isPairedCharOpener(keyChar);
            if (isPairedOpener)
                handlePairedCharOpener(keyChar);

            // Handle close bracket: Remove level of indent
            if (keyChar == '}') {

                // Get indent for this line and next
                TextBoxLine thisLine = getSel().getStartLine();
                TextBoxLine prevLine = thisLine.getPrevious();
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

            // Activate PopupList
            JavaPopupList javaPopup = _javaTextArea.getPopup();
            if (!javaPopup.isShowing() && !anEvent.isSpaceKey())
                ViewUtils.runLater(() -> javaPopup.activatePopupList());
        }
    }

    /**
     * Process newline key event.
     */
    protected void processNewline()
    {
        // Get line and its indent
        TextBoxLine textLine = getSel().getStartLine();

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

        // Create string for new line plus indent
        String indentStr = textLine.getIndentString();
        StringBuffer sb = new StringBuffer().append('\n').append(indentStr);

        // If leaving conditional (if, for, do, while) without brackets, remove level of indent
        JNode selNode = _javaTextArea.getSelNode();
        JStmtConditional selNodeParent = selNode != null ? selNode.getParent(JStmtConditional.class) : null;
        if (selNodeParent != null &&  !(selNodeParent.getStatement() instanceof JStmtBlock)) {
            if (sb.length() > INDENT_STRING.length())
                sb.delete(sb.length() - INDENT_STRING.length(), sb.length());
        }

        // Do normal version
        _textArea.replaceChars(sb.toString());
    }

    /**
     * Returns whether this line is processing a multi line comment.
     */
    private boolean isEnteringMultilineComment(TextBoxLine aTextLine)
    {
        TextBoxToken lastToken = aTextLine.getTokenLast();
        return lastToken != null && lastToken.getName() == CodeTokenizer.MULTI_LINE_COMMENT;
    }

    /**
     * Process newline key event.
     */
    protected void processNewlineForMultilineComment(TextBoxLine aTextLine)
    {
        String lineString = aTextLine.getString().trim();
        boolean isStartOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");
        boolean isInMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");
        boolean isEndMultiLineComment = lineString.startsWith("*") && lineString.endsWith("*/");

        // Create indent string
        String indentStr = aTextLine.getIndentString();
        StringBuffer sb = new StringBuffer().append('\n').append(indentStr);

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
    private boolean isEnteringBlockStatement(TextBoxLine aTextLine)
    {
        // If last token is open bracket, return true
        TextBoxToken textToken = aTextLine.getTokenLast();
        String textTokenString = textToken != null ? textToken.getString() : "";
        if (textTokenString.equals("{"))
            return true;

        // If current node is conditional (if, for, do, while), return true
        JNode selNode = _javaTextArea.getSelNode();
        if (selNode instanceof JStmtConditional)
            return true;

        // Return false
        return false;
    }

    /**
     * Process newline key event.
     */
    protected void processNewlineForBlockStatement(TextBoxLine aTextLine)
    {
        // Create string for new line plus indent
        String indentStr = aTextLine.getIndentString();
        StringBuffer sb = new StringBuffer().append('\n').append(indentStr);

        // Add additional level of indent
        sb.append(INDENT_STRING);

        // Do normal version
        _textArea.replaceChars(sb.toString());

        // If start of code block, proactively append close bracket
        TextBoxToken textToken = aTextLine.getTokenLast();
        String textTokenString = textToken != null ? textToken.getString() : "";
        boolean addCloseBracket = textTokenString.equals("{");
        if (addCloseBracket) {
            if (_javaTextArea.getJFile().getException() != null && sb.length() > INDENT_STRING.length()) { // Sanity check
                int start = getSelStart();
                String str = sb.substring(0, sb.length() - INDENT_STRING.length()) + "}";
                _textArea.replaceChars(str, null, start, start, false);
                setSel(start);
            }
        }
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
            case '\'': return openerChar;
            case '"': return openerChar;
            case '(': return ')';
            case '[': return ']';
            default: throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharCloser: Illegal char: " + openerChar);
        }
    }
    /**
     * Returns the paired opener char for given closer char.
     */
    public char getPairedCharForCloser(char closerChar)
    {
        switch (closerChar) {
            case '\'': return closerChar;
            case '"': return closerChar;
            case ')': return '(';
            case ']': return '[';
            default: throw new IllegalArgumentException("JavaTextAreaKey.getPairedCharOpener: Illegal char: " + closerChar);
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
}

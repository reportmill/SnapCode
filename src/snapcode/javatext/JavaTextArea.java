/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import snapcode.project.JavaTextDoc;
import snap.geom.Rect;
import snap.gfx.*;
import snap.text.*;
import snap.props.PropChange;
import snap.util.CharSequenceUtils;
import snap.view.*;
import snap.web.WebFile;
import snapcode.project.*;

/**
 * A TextArea subclass for Java source editing.
 */
public class JavaTextArea extends TextArea {

    // Whether to draw line for print margin column
    private boolean  _showPrintMargin = true;

    // The selected JNode
    protected JNode  _selNode = new JFile();

    // The deepest child of SelNode recently selected
    protected JNode  _deepNode;

    // The node that the mouse is hovering over (if command down)
    protected JNode  _hoverNode;

    // The list of selected tokens
    protected TextBoxToken[]  _selTokens = new TextBoxToken[0];

    // A PopupList to show code completion stuff
    protected JavaPopupList  _popup;

    // Constants for properties
    public static final String SelNode_Prop = "SelNode";

    // Constants
    protected static String INDENT_STRING = "    ";
    private static JavaTextDoc DUMMY_TEXT_DOC;

    /**
     * Creates a new JavaTextArea.
     */
    public JavaTextArea()
    {
        setFill(Color.WHITE);
        setPadding(5, 5, 5,5);
        setEditable(true);

        // Create DUMMY_TEXT_DOC (first time only)
        if (DUMMY_TEXT_DOC == null) {
            WebFile tempFile = ProjectUtils.getTempSourceFile(null, "Dummy", "java");
            DUMMY_TEXT_DOC = JavaTextDoc.getJavaTextDocForSource(tempFile);
        }

        // Set default TextDoc to JavaTextDoc
        setTextDoc(DUMMY_TEXT_DOC);
    }

    /**
     * Override to create JavaText.
     */
    @Override
    protected TextAreaKeys createTextAreaKeys()  { return new JavaTextAreaKeys(this); }

    /**
     * Returns the code completion popup.
     */
    public JavaPopupList getPopup()
    {
        // If already set, just return
        if (_popup != null) return _popup;

        // Create, set, return
        JavaPopupList popupList = new JavaPopupList(this);
        return _popup = popupList;
    }

    /**
     * Returns completions for current text selection and selected node.
     */
    public JavaDecl[] getCompletionsAtCursor()
    {
        // If selection not empty, just return
        if (!isSelEmpty())
            return null;

        // If selection not at end of SelNode, just return
        int selStart = getSelStart();
        JNode selNode = getSelNode();
        int startCharIndex = getTextDoc().getStartCharIndex();
        int nodeEnd = selNode.getEndCharIndex() - startCharIndex;
        if (selStart != nodeEnd)
            return null;

        // If not id expression, just return
        JExprId idExpr = selNode instanceof JExprId ? (JExprId) selNode : null;
        if (idExpr == null)
            return null;

        // Get completions and return
        NodeCompleter javaCompleter = new NodeCompleter();
        JavaDecl[] completions = javaCompleter.getCompletionsForId(idExpr);
        return completions;
    }

    /**
     * Returns whether to draw line for print margin column.
     */
    public boolean getShowPrintMargin()  { return _showPrintMargin; }

    /**
     * Sets whether to draw line for print margin column.
     */
    public void setShowPrintMargin(boolean aValue)  { _showPrintMargin = aValue; }

    /**
     * Selects a given line number.
     */
    public void selectLine(int anIndex)
    {
        TextBoxLine textLine = anIndex >= 0 && anIndex < getLineCount() ? getLine(anIndex) : null;
        if (textLine != null)
            setSel(textLine.getStartCharIndex(), textLine.getEndCharIndex());
    }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFile getJFile()
    {
        //JavaTextBox textBox = getTextBox();
        TextDoc textDoc = getTextDoc();

        // Get JavaTextDoc and forward
        JavaTextDoc javaTextDoc = (JavaTextDoc) textDoc;
        return javaTextDoc.getJFile();
    }

    /**
     * Returns the node at given start/end char indexes.
     */
    public JNode getNodeAtCharIndex(int startCharIndex, int endCharIndex)
    {
        // If TextDoc is SubText, adjust start/end
        TextDoc textDoc = getTextDoc();
        int subTextStart = textDoc.getStartCharIndex();
        if (subTextStart > 0) {
            startCharIndex += subTextStart;
            endCharIndex += subTextStart;
        }

        // Forward to JFile
        JFile jfile = getJFile();
        return jfile.getNodeAtCharIndex(startCharIndex, endCharIndex);
    }

    /**
     * Override to update selected node and tokens.
     */
    public void setSel(int aStart, int anEnd)
    {
        // Do normal version
        super.setSel(aStart, anEnd);

        // Get node for selection
        int selStart = getSelStart(), selEnd = getSelEnd();
        JNode node = getNodeAtCharIndex(selStart, selEnd);

        // Select node
        setSelNode(node);
    }

    /**
     * Returns the selected JNode.
     */
    public JNode getSelNode()  { return _selNode; }

    /**
     * Sets the selected JNode.
     */
    public void setSelNode(JNode aNode)
    {
        // If already set, just return
        if (aNode == getSelNode()) return;

        // Set value
        JNode oldSelNode = _selNode;
        _selNode = _deepNode = aNode;

        // Reset SelTokens
        TextBoxToken[] selTokens = getTokensForNode(aNode);
        setSelTokens(selTokens);

        // Reset PopupList
        JavaPopupList javaPopup = getPopup();
        javaPopup.updatePopupList();

        // Fire prop change
        firePropChange(SelNode_Prop, oldSelNode, _selNode);
    }

    /**
     * Returns the array of selected tokens.
     */
    public TextBoxToken[] getSelTokens()  { return _selTokens; }

    /**
     * Sets the array of selected tokens.
     */
    private void setSelTokens(TextBoxToken[] theTokens)
    {
        // If new & old both empty, just return
        if (_selTokens.length == 0 && theTokens.length == 0) return;

        // Set + Repaint
        repaintTokensBounds(_selTokens);
        _selTokens = theTokens;
        repaintTokensBounds(_selTokens);
    }

    /**
     * Returns reference tokens for given node.
     */
    protected TextBoxToken[] getTokensForNode(JNode aNode)
    {
        // If not var name or type name, just return
        if (!(aNode instanceof JExprId || aNode instanceof JType))
            return new TextBoxToken[0];

        // Handle null
        JavaDecl nodeDecl = aNode.getDecl();
        if (nodeDecl == null)
            return new TextBoxToken[0];

        // Get other matching nodes
        JNode[] matchingNodes = NodeMatcher.getMatchingNodesForDecl(aNode.getFile(), nodeDecl);
        if (matchingNodes.length == 0)
            return new TextBoxToken[0];

        // Return TextBoxTokens
        return getTokensForNodes(matchingNodes);
    }

    /**
     * Returns a TextBoxToken array for given JNodes.
     */
    protected TextBoxToken[] getTokensForNodes(JNode[] theNodes)
    {
        // Convert matching JNodes to TextBoxTokens
        List<TextBoxToken> tokensList = new ArrayList<>(theNodes.length);
        TextBox textBox = getTextBox();
        int textBoxLineStart = 0;

        // Iterate over nodes and convert to TextBoxTokens
        for (JNode jnode : theNodes) {

            // Get line index (skip if negative - assume Repl import statement or something)
            int lineIndex = jnode.getLineIndex() - textBoxLineStart;
            if (lineIndex < 0)
                continue;

            // Get line and token
            TextBoxLine textBoxLine = textBox.getLine(lineIndex);
            int startCharIndex = jnode.getLineCharIndex();
            if (jnode instanceof JType && jnode.getStartToken() != jnode.getEndToken())
                startCharIndex = jnode.getEndToken().getColumnIndex();
            TextBoxToken token = textBoxLine.getTokenForCharIndex(startCharIndex);

            // Add to tokens list
            if (token != null)
                tokensList.add(token);
            else System.out.println("JavaTextArea.getTokensForNode: Can't find token for matching node: " + jnode);
        }

        // Return
        return tokensList.toArray(new TextBoxToken[0]);
    }

    /**
     * Repaints token bounds.
     */
    private void repaintTokensBounds(TextBoxToken[] theTokens)
    {
        if (theTokens.length == 0) return;
        Rect tokensBounds = getBoundsForTokens(theTokens);
        repaint(tokensBounds);
    }

    /**
     * Returns the bounds rect for tokens.
     */
    private Rect getBoundsForTokens(TextBoxToken[] theTokens)
    {
        // Get first token and bounds
        TextBoxToken token0 = theTokens[0];
        double tokenX = Math.round(token0.getTextBoxX()) - 1;
        double tokenY = Math.round(token0.getTextBoxY()) - 1;
        double tokenMaxX = Math.ceil(token0.getTextBoxMaxX()) + 1;
        double tokenMaxY = Math.ceil(token0.getTextBoxMaxY()) + 1;

        // Iterate over remaining tokens and union bounds
        for (int i = 1; i < theTokens.length; i++) {
            TextBoxToken token = theTokens[i];
            tokenX = Math.min(tokenX, Math.round(token.getTextBoxX()) - 1);
            tokenY = Math.min(tokenY, Math.round(token.getTextBoxY()) - 1);
            tokenMaxX = Math.max(tokenMaxX, Math.ceil(token.getTextBoxMaxX()) + 1);
            tokenMaxY = Math.max(tokenMaxY, Math.ceil(token.getTextBoxMaxY()) + 1);
        }

        // Return bounds
        return new Rect(tokenX, tokenY, tokenMaxX - tokenX, tokenMaxY - tokenY);
    }

    /**
     * Returns the deep node - last node selected by user mouse.
     */
    public JNode getDeepNode()  { return _deepNode; }

    /**
     * Sets the deep node.
     */
    public void setDeepNode(JNode aNode)  { _deepNode = aNode; }

    /**
     * Returns the node under the mouse (if command is down).
     */
    public JNode getHoverNode()  { return _hoverNode; }

    /**
     * Sets the node under the mouse (if command is down).
     */
    public void setHoverNode(JNode aNode)
    {
        _hoverNode = aNode;
        repaint();
    }

    /**
     * Override to draw print margin.
     */
    protected void paintBack(Painter aPntr)
    {
        // Do normal version
        super.paintBack(aPntr);

        // Configure MarginLine
        if (getShowPrintMargin()) {
            double x = getPadding().getLeft() + getFont().charAdvance('X') * 120 + .5;
            aPntr.setColor(Color.LIGHTGRAY);
            aPntr.setStroke(Stroke.Stroke1);
            aPntr.drawLine(x, 0, x, getHeight());
        }

        // Underline build issues
        BuildIssue[] issues = getBuildIssues();
        for (BuildIssue issue : issues) {

            int issueStart = issue.getStart();
            int issueEnd = issue.getEnd();
            if (issueEnd < issueStart || issueEnd > length())
                continue;

            TextBoxLine textBoxLine = getLineForCharIndex(issueEnd);
            int lineStartCharIndex = textBoxLine.getStartCharIndex();
            if (issueStart < lineStartCharIndex)
                issueStart = lineStartCharIndex;
            TextBoxToken token = getTokenForCharIndex(issueStart);
            if (token != null) {
                int tend = token.getTextLine().getStartCharIndex() + token.getEndCharIndex();
                if (issueEnd < tend)
                    issueEnd = tend;
            }

            // If possible, make sure we underline at least one char
            if (issueStart == issueEnd && issueEnd < textBoxLine.getEndCharIndex()) issueEnd++;
            int yb = (int) Math.round(textBoxLine.getBaseline()) + 2;
            double x1 = textBoxLine.getXForCharIndex(issueStart - lineStartCharIndex);
            double x2 = textBoxLine.getXForCharIndex(issueEnd - lineStartCharIndex);
            aPntr.setPaint(issue.isError() ? Color.RED : new Color(244, 198, 60));
            aPntr.setStroke(Stroke.StrokeDash1);
            aPntr.drawLine(x1, yb, x2, yb);
            aPntr.setStroke(Stroke.Stroke1);
        }

        // Paint program counter
        int progCounterLine = getProgramCounterLine();
        if (progCounterLine >= 0 && progCounterLine < getLineCount()) {
            TextBoxLine line = getLine(progCounterLine);
            aPntr.setPaint(new Color(199, 218, 175, 200));
            aPntr.fillRect(line.getX(), line.getY() + .5, line.getWidth(), line.getHeight());
        }

        // Paint selected tokens highlight rects
        TextBoxToken[] selTokens = getSelTokens();
        if (selTokens.length > 0) {
            aPntr.setColor(new Color("#FFF3AA"));
            for (TextBoxToken token : selTokens) {
                double tokenX = Math.round(token.getTextBoxX()) - 1;
                double tokenY = Math.round(token.getTextBoxY()) - 1;
                double tokenW = Math.ceil(token.getTextBoxMaxX()) - tokenX + 1;
                double tokenH = Math.ceil(token.getTextBoxMaxY()) - tokenY + 1;
                aPntr.fillRect(tokenX, tokenY, tokenW, tokenH);
            }
        }

        // If HoverNode, underline
        if (_hoverNode != null) {
            //TextBoxToken hoverToken = (TextBoxToken) _hoverNode.getStartToken();
            TextBoxToken hoverToken = getTokenForCharIndex(_hoverNode.getStartCharIndex());
            double tokenX = hoverToken.getTextBoxX();
            double tokenY = hoverToken.getTextBoxStringY() + 1;
            double tokenMaxX = tokenX + hoverToken.getWidth();
            aPntr.setColor(Color.BLACK);
            aPntr.drawLine(tokenX, tokenY, tokenMaxX, tokenY);
        }
    }

    /**
     * Indents the text.
     */
    public void indentLines()
    {
        // Get start/end lines for selection
        TextSel textSel = getSel();
        TextBoxLine startLine = textSel.getStartLine();
        TextBoxLine endLine = textSel.getEndLine();
        int selStart = textSel.getStart();
        int selEnd = textSel.getEnd();

        // Iterate over selected lines
        while (true) {

            // Add indent
            int startCharIndex = startLine.getStartCharIndex();
            addChars(INDENT_STRING, null, startLine.getStartCharIndex());

            // Adjust Sel start/end
            if (startCharIndex <= selStart)
                selStart += INDENT_STRING.length();
            if (startCharIndex <= selEnd)
                selEnd += INDENT_STRING.length();

            // If at end line, break, otherwise get next line
            if (startLine == endLine)
                break;
            startLine = startLine.getNext();
        }

        // Reset selection
        setSel(selStart, selEnd);
    }

    /**
     * Indents the text.
     */
    public void outdentLines()
    {
        // Get start/end lines for selection
        TextSel textSel = getSel();
        TextBoxLine startLine = textSel.getStartLine();
        TextBoxLine endLine = textSel.getEndLine();
        int selStart = textSel.getStart();
        int selEnd = textSel.getEnd();

        // Iterate over lines
        while (true) {

            // Get start/end of line indent
            int startCharIndex = 0;
            int endCharIndex = 0;
            while (endCharIndex < startLine.length() && startLine.charAt(endCharIndex) == ' ' && endCharIndex - startCharIndex < INDENT_STRING.length())
                endCharIndex++;

            // If indent found, remove it
            if (endCharIndex != startCharIndex) {

                // Convert indexes to TextDoc
                int lineStartCharIndex = startLine.getStartCharIndex();
                startCharIndex += lineStartCharIndex;
                endCharIndex += lineStartCharIndex;

                // Remove chars
                replaceChars("", null, startCharIndex, endCharIndex, false);

                // Adjust Sel start/end
                if (startCharIndex < selStart)
                    selStart -= Math.min(endCharIndex - startCharIndex, selStart - startCharIndex);
                if (startCharIndex < selEnd)
                    selEnd -= Math.min(endCharIndex - startCharIndex, selEnd - startCharIndex);

                // If at end line, break, otherwise get next line
                if (startLine == endLine)
                    break;
                startLine = startLine.getNext();
            }
        }

        // Reset selection
        setSel(selStart, selEnd);
    }

    /**
     * Adds or removes comments from selected lines.
     */
    public void commentLinesWithLineComment()
    {
        // Get start/end lines for selection
        TextSel textSel = getSel();
        TextBoxLine startLine = textSel.getStartLine();
        TextBoxLine endLine = textSel.getEndLine();
        int selStart = textSel.getStart();
        int selEnd = textSel.getEnd();

        // Assume adding comments if any line doesn't start with comment
        boolean addComments = false;
        for (TextBoxLine line = startLine; line != endLine.getNext(); line = line.getNext()) {
            String lineStr = line.getString().trim();
            if (!lineStr.startsWith("//")) {
                addComments = true;
                break;
            }
        }

        // Iterate over selected lines
        while (true) {

            int startCharIndex = startLine.getStartCharIndex();

            // If adding comment, add comment chars and adjust SelStart/SelEnd
            if (addComments) {
                addChars("//", null, startLine.getStartCharIndex());
                if (startCharIndex <= selStart)
                    selStart += 2;
                if (startCharIndex <= selEnd)
                    selEnd += 2;
            }

            // If removing comment, remove comment chars and adjust SelStart/SelEnd
            else {
                int commentCharIndex = startCharIndex + startLine.getString().indexOf("//");
                delete(commentCharIndex, commentCharIndex + 2, false);
                if (commentCharIndex <= selStart)
                    selStart -= 2;
                if (commentCharIndex <= selEnd)
                    selEnd -= 2;
            }

            // If at end line, break, otherwise get next line
            if (startLine == endLine)
                break;
            startLine = startLine.getNext();
        }

        // Reset selection
        setSel(selStart, selEnd);
    }

    /**
     * Override to setTextModified.
     */
    protected void textDocDidPropChange(PropChange anEvent)
    {
        // Do normal version and update TextPane.TextModified
        super.textDocDidPropChange(anEvent);

        // Get PropName
        String propName = anEvent.getPropName();

        // Handle Chars_Prop: Call didAddChars/didRemoveChars
        if (propName == TextDoc.Chars_Prop) {

            // Get CharsChange info
            TextDocUtils.CharsChange charsChange = (TextDocUtils.CharsChange) anEvent;
            int charIndex = anEvent.getIndex();
            CharSequence addChars = charsChange.getNewValue();
            CharSequence removeChars = charsChange.getOldValue();

            // Forward to did add/remove chars
            if (addChars != null)
                didAddChars(addChars, charIndex);
            else didRemoveChars(removeChars, charIndex);
        }
    }

    /**
     * Called when characters are added.
     */
    protected void didAddChars(CharSequence theChars, int charIndex)
    {
        // Iterate over BuildIssues and shift start/end for removed chars
        int charsLength = theChars.length();
        BuildIssue[] buildIssues = getBuildIssues();
        for (BuildIssue buildIssue : buildIssues) {
            int buildIssueStart = buildIssue.getStart();
            if (charIndex <= buildIssueStart)
                buildIssue.setStart(buildIssueStart + charsLength);
            int buildIssueEnd = buildIssue.getEnd();
            if (charIndex < buildIssueEnd)
                buildIssue.setEnd(buildIssueEnd + charsLength);
        }

        // If line was added or removed, iterate over Breakpoints and shift start/end for removed chars
        int newlineCount = CharSequenceUtils.getNewlineCount(theChars);
        if (newlineCount > 0) {

            // Get start/end line index
            TextBoxLine startLine = getLineForCharIndex(charIndex);
            TextBoxLine endLine = getLineForCharIndex(charIndex + charsLength);
            int startLineIndex = startLine.getIndex();
            int endLineIndex = endLine.getIndex();
            int lineIndexDelta = endLineIndex - startLineIndex;

            // Iterate over breakpoints and update
            Breakpoint[] breakpoints = getBreakpoints();
            for (Breakpoint breakpoint : breakpoints) {
                int lineIndex = breakpoint.getLine();
                if (startLineIndex < lineIndex && endLineIndex <= lineIndex) {
                    breakpoint.setLine(lineIndex + lineIndexDelta);
                    Breakpoints breakpointsHpr = getWorkspaceBreakpoints();
                    if (breakpointsHpr != null)
                        breakpointsHpr.writeFile();
                }
            }
        }
    }

    /**
     * Called when characters are removed.
     */
    protected void didRemoveChars(CharSequence theChars, int charIndex)
    {
        // See if we need to shift BuildIssues
        int endOld = charIndex + theChars.length();
        BuildIssue[] buildIssues = getBuildIssues();
        for (BuildIssue buildIssue : buildIssues) {
            int buildIssueStart = buildIssue.getStart();
            int buildIssueEnd = buildIssue.getEnd();
            if (charIndex < buildIssueStart) {
                int newStart = buildIssueStart - (Math.min(buildIssueStart, endOld) - charIndex);
                buildIssue.setStart(newStart);
            }
            if (charIndex < buildIssueEnd) {
                int newEnd = buildIssueEnd - (Math.min(buildIssueEnd, endOld) - charIndex);
                buildIssue.setEnd(newEnd);
            }
        }

        // See if we need to remove Breakpoints
        int newlineCount = CharSequenceUtils.getNewlineCount(theChars);
        if (newlineCount > 0) {

            // Get start/end lines
            TextBoxLine startLine = getLineForCharIndex(charIndex);
            int startLineIndex = startLine.getIndex();
            int endLineIndex = startLineIndex + newlineCount;
            int lineIndexDelta = endLineIndex - startLineIndex;

            // Iterate over breakpoints and see if they need to update line (or be removed)
            Breakpoint[] breakpoints = getBreakpoints();
            for (Breakpoint breakpoint : breakpoints) {
                int lineIndex = breakpoint.getLine();
                if (startLineIndex < lineIndex) {
                    Breakpoints breakpointsHpr = getWorkspaceBreakpoints();
                    if (breakpointsHpr == null) return;
                    if (endLineIndex <= lineIndex) {
                        breakpoint.setLine(lineIndex - lineIndexDelta);
                        breakpointsHpr.writeFile();
                    }
                    else breakpointsHpr.remove(breakpoint);
                }
            }
        }
    }

    /**
     * Returns the source file.
     */
    public WebFile getSourceFile()
    {
        TextDoc textDoc = getTextDoc();
        return textDoc.getSourceFile();
    }

    /**
     * Returns BuildIssues from ProjectFile.
     */
    public BuildIssue[] getBuildIssues()
    {
        WebFile javaFile = getSourceFile();
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        return javaAgent.getBuildIssues();
    }

    /**
     * Returns the workspace breakpoints.
     */
    private Breakpoints getWorkspaceBreakpoints()
    {
        WebFile file = getSourceFile();
        Project proj = Project.getProjectForFile(file);
        Workspace workspace = proj != null ? proj.getWorkspace() : null;
        return workspace != null ? workspace.getBreakpoints() : null;
    }

    /**
     * Returns Breakpoints from ProjectFile.
     */
    public Breakpoint[] getBreakpoints()
    {
        // Get project breakpoints
        Breakpoints breakpointsHpr = getWorkspaceBreakpoints();
        if (breakpointsHpr == null)
            return Breakpoints.NO_BREAKPOINTS;

        // Get java file
        WebFile file = getSourceFile();
        if (file == null)
            return Breakpoints.NO_BREAKPOINTS;

        // Return breakpoints for file
        return breakpointsHpr.getBreakpointsForFile(file);
    }

    /**
     * Adds breakpoint at line.
     */
    public void addBreakpoint(int aLine)
    {
        // Get breakpoints helper
        Breakpoints breakpointsHpr = getWorkspaceBreakpoints();
        if (breakpointsHpr == null)
            return;

        // Get java file
        WebFile file = getSourceFile();

        // Add breakpoint for file
        breakpointsHpr.addBreakpointForFile(file, aLine);
    }

    /**
     * Remove breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)
    {
        // Get project breakpoints
        Breakpoints breakpointsHpr = getWorkspaceBreakpoints();
        if (breakpointsHpr == null)
            return;

        // Remove breakpoint
        breakpointsHpr.remove(aBP);
    }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()
    {
        JavaTextPane<?> javaTextPane = getOwner(JavaTextPane.class);
        if (javaTextPane == null)
            return -1;
        return javaTextPane.getProgramCounterLine();
    }

    /**
     * Override to get string first.
     */
    @Override
    protected Object getClipboardContent(Clipboard clipboard)
    {
        // Try String first
        if (clipboard.hasString()) {
            String str = clipboard.getString();
            if (str != null && str.length() > 0)
                return str;
        }

        // Do normal version
        return super.getClipboardContent(clipboard);
    }

    /**
     * Override to remove extra indent from pasted strings.
     */
    @Override
    public void replaceCharsWithContent(Object theContent)
    {
        // If String, trim extra indent
        if (theContent instanceof String && getTextDoc() instanceof JeplTextDoc)
            theContent = JavaTextUtils.removeExtraIndentFromString((String) theContent);

        // Do normal version
        super.replaceCharsWithContent(theContent);
    }
}
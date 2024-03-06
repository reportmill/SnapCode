/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import snap.util.FileUtils;
import snap.web.WebURL;
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

    // Whether mouse move looks to highlight node under mouse
    private boolean _hoverEnabled;

    // The node that the mouse is hovering over (if command down)
    private JNode _hoverNode;

    // The list of selected tokens
    private TextToken[] _selTokens = new TextToken[0];

    // The last painted token bounds
    private Rect _selTokensBounds = new Rect();

    // A PopupList to show code completion stuff
    private JavaPopupList _popup;

    // Constants for properties
    public static final String SelNode_Prop = "SelNode";

    // Constants
    protected static String INDENT_STRING = "    ";
    private static JavaTextDoc DUMMY_TEXT_DOC;
    private static final Color ERROR_TEXT_COLOR = Color.RED.brighter().brighter();
    private static final Color PROGRAM_COUNTER_LINE_HIGHLITE_COLOR = new Color(199, 218, 175, 200);

    /**
     * Creates a new JavaTextArea.
     */
    public JavaTextArea()
    {
        super();
        setFill(Color.WHITE);
        setPadding(5, 5, 5,5);
        setEditable(true);

        // Create shared DUMMY_TEXT_DOC
        if (DUMMY_TEXT_DOC == null)
            DUMMY_TEXT_DOC = createDummyTextDoc();

        // Set default TextDoc to JavaTextDoc
        setSourceText(DUMMY_TEXT_DOC);
    }

    /**
     * Override to create JavaText.
     */
    @Override
    protected TextAreaKeys createTextAreaKeys()  { return new JavaTextAreaKeys(this); }

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
        TextLine textLine = anIndex >= 0 && anIndex < getLineCount() ? getLine(anIndex) : null;
        if (textLine != null)
            setSel(textLine.getStartCharIndex(), textLine.getEndCharIndex());
    }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFile getJFile()
    {
        // Get JavaTextDoc and forward
        JavaTextDoc javaTextDoc = (JavaTextDoc) getSourceText();
        return javaTextDoc.getJFile();
    }

    /**
     * Returns the node at given char index.
     */
    public JNode getNodeForCharIndex(int charIndex)
    {
        return getNodeForCharRange(charIndex, charIndex);
    }

    /**
     * Returns the node at given start/end char indexes.
     */
    public JNode getNodeForCharRange(int startCharIndex, int endCharIndex)
    {
        JFile jfile = getJFile();
        return jfile.getNodeForCharRange(startCharIndex, endCharIndex);
    }

    /**
     * Returns the node at given XY.
     */
    public JNode getNodeAtXY(double aX, double aY)
    {
        int charIndexForXY = getCharIndexForXY(aX, aY);
        JFile jfile = getJFile();
        JNode node = jfile.getNodeForCharIndex(charIndexForXY);
        if (node instanceof JExprId || node instanceof JType)
            return node;
        return null;
    }

    /**
     * Override to update selected node and tokens.
     */
    @Override
    public void setSel(int aStart, int anEnd)
    {
        // Do normal version
        super.setSel(aStart, anEnd);

        // Get node for selection
        int selStart = getSelStart(), selEnd = getSelEnd();
        JNode node = getNodeForCharRange(selStart, selEnd);

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
        TextToken[] selTokens = getTokensForNode(aNode);
        setSelTokens(selTokens);

        // Fire prop change
        firePropChange(SelNode_Prop, oldSelNode, _selNode);
    }

    /**
     * Returns the array of selected tokens.
     */
    public TextToken[] getSelTokens()  { return _selTokens; }

    /**
     * Sets the array of selected tokens.
     */
    private void setSelTokens(TextToken[] theTokens)
    {
        // If new & old both empty, just return
        if (_selTokens.length == 0 && theTokens.length == 0) return;

        // Set + Repaint
        repaint(_selTokensBounds);
        _selTokens = theTokens;
        repaintTokensBounds(_selTokens);
    }

    /**
     * Returns reference tokens for given node.
     */
    protected TextToken[] getTokensForNode(JNode aNode)
    {
        // If not var name or type name, just return
        if (!(aNode instanceof JExprId || aNode instanceof JType))
            return new TextToken[0];

        // Handle null
        JavaDecl nodeDecl = aNode.getDecl();
        if (nodeDecl == null)
            return new TextToken[0];

        // Get other matching nodes
        JExprId[] matchingIdNodes = NodeMatcher.getMatchingIdNodesForDecl(aNode.getFile(), nodeDecl);
        if (matchingIdNodes.length == 0)
            return new TextToken[0];

        // Return TextBoxTokens
        return getTokensForIdNodes(matchingIdNodes);
    }

    /**
     * Returns array of respective TextTokens for given id expression nodes.
     */
    protected TextToken[] getTokensForIdNodes(JExprId[] idNodes)
    {
        // Convert matching JNodes to TextBoxTokens
        List<TextToken> tokensList = new ArrayList<>(idNodes.length);
        TextBlock textBlock = getTextBlock();

        // Iterate over nodes and convert to TextBoxTokens
        for (JExprId idExpr : idNodes) {

            // If node is zero length, skip
            if (idExpr.getCharLength() == 0)
                continue;

            // Get line index (skip if negative - assume Repl import statement or something)
            int lineIndex = idExpr.getLineIndex();
            if (lineIndex < 0)
                continue;

            // Get line and token
            TextLine textLine = textBlock.getLine(lineIndex);
            int textLineStartCharIndex = textLine.getStartCharIndex();
            int nodeStartCharIndex = idExpr.getStartCharIndex();
            int tokenStartCharIndexInLine = nodeStartCharIndex - textLineStartCharIndex;
            TextToken token = textLine.getTokenForCharIndex(tokenStartCharIndexInLine);

            // Add to tokens list
            if (token != null)
                tokensList.add(token);
            else System.out.println("JavaTextArea.getTokensForNode: Can't find token for matching node: " + idExpr);
        }

        // Return
        return tokensList.toArray(new TextToken[0]);
    }

    /**
     * Repaints token bounds.
     */
    private void repaintTokensBounds(TextToken[] theTokens)
    {
        if (theTokens.length == 0) return;
        Rect tokensBounds = getBoundsForTokens(theTokens);
        repaint(tokensBounds);
    }

    /**
     * Returns the bounds rect for tokens.
     */
    private Rect getBoundsForTokens(TextToken[] theTokens)
    {
        // Get first token and bounds
        TextToken token0 = theTokens[0];
        double tokenX = Math.round(token0.getTextX()) - 1;
        double tokenY = Math.round(token0.getTextY()) - 1;
        double tokenMaxX = Math.ceil(token0.getTextMaxX()) + 1;
        double tokenMaxY = Math.ceil(token0.getTextMaxY()) + 1;

        // Iterate over remaining tokens and union bounds
        for (int i = 1; i < theTokens.length; i++) {
            TextToken token = theTokens[i];
            tokenX = Math.min(tokenX, Math.round(token.getTextX()) - 1);
            tokenY = Math.min(tokenY, Math.round(token.getTextY()) - 1);
            tokenMaxX = Math.max(tokenMaxX, Math.ceil(token.getTextMaxX()) + 1);
            tokenMaxY = Math.max(tokenMaxY, Math.ceil(token.getTextMaxY()) + 1);
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
     * Returns whether hover is enabled.
     */
    public boolean isHoverEnabled()  { return _hoverEnabled; }

    /**
     * Sets whether hover is enabled.
     */
    public void setHoverEnabled(boolean aValue)
    {
        if (aValue == _hoverEnabled) return;
        _hoverEnabled = aValue;
        setHoverNode(null);
    }

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
        paintErrors(aPntr);

        // Paint program counter
        int progCounterLine = getProgramCounterLine();
        if (progCounterLine >= 0 && progCounterLine < getLineCount()) {
            TextLine textLine = getLine(progCounterLine);
            aPntr.setPaint(PROGRAM_COUNTER_LINE_HIGHLITE_COLOR);
            aPntr.fillRect(0, textLine.getTextY() - 1, getWidth(), textLine.getHeight() + 3);
        }

        // Paint selected tokens highlight rects
        TextToken[] selTokens = getSelTokens();
        _selTokensBounds.setRect(0, 0, 0, 0);
        if (selTokens.length > 0) {
            aPntr.setColor(new Color("#FFF3AA"));
            for (TextToken token : selTokens) {
                double tokenX = Math.round(token.getTextX()) - 1;
                double tokenY = Math.round(token.getTextY()) - 1;
                double tokenW = Math.ceil(token.getTextMaxX()) - tokenX + 1;
                double tokenH = Math.ceil(token.getTextMaxY()) - tokenY + 1;
                aPntr.fillRect(tokenX, tokenY, tokenW, tokenH);
                _selTokensBounds.union(tokenX, tokenY, tokenW, tokenH);
            }
        }

        // If HoverNode, underline
        if (_hoverNode != null) {
            TextToken hoverToken = getTokenForCharIndex(_hoverNode.getStartCharIndex());
            if (hoverToken != null) {
                double tokenX = hoverToken.getTextX();
                double tokenY = hoverToken.getTextStringY() + 1;
                double tokenMaxX = tokenX + hoverToken.getWidth();
                aPntr.setColor(Color.BLACK);
                aPntr.drawLine(tokenX, tokenY, tokenMaxX, tokenY);
            }
        }
    }

    /**
     * Paints errors.
     */
    private void paintErrors(Painter aPntr)
    {
        // Set font for error messages
        aPntr.setFont(getFont());

        // Underline build issues
        BuildIssue[] issues = getBuildIssues();
        Set<TextLine> paintedLines = new HashSet<>();
        for (BuildIssue issue : issues) {
            if (issue.isError())
                paintError(aPntr, issue, paintedLines);
        }
    }

    /**
     * Paints an error.
     */
    private void paintError(Painter aPntr, BuildIssue issue, Set<TextLine> paintedLines)
    {
        int issueStart = issue.getStart();
        int issueEnd = issue.getEnd();
        if (issueEnd < issueStart || issueEnd > length())
            return;

        // Get text line for error - just return if already painted
        TextLine textLine = getLineForCharIndex(issueEnd);
        if (paintedLines.contains(textLine))
            return;
        paintedLines.add(textLine);

        // Constrain issueStart and issueEnd to textLine
        int lineStartCharIndex = textLine.getStartCharIndex();
        if (issueStart < lineStartCharIndex)
            issueStart = lineStartCharIndex;
        TextToken token = getTokenForCharIndex(issueStart);
        if (token != null) {
            int tend = token.getTextLine().getStartCharIndex() + token.getEndCharIndex();
            if (issueEnd < tend)
                issueEnd = tend;
        }

        // If possible, make sure we underline at least one char
        if (issueStart == issueEnd && issueEnd < textLine.getEndCharIndex())
            issueEnd++;
        int issueY = (int) Math.round(textLine.getTextBaseline()) + 2;
        double issueX = textLine.getTextXForCharIndex(issueStart - lineStartCharIndex);
        double issueMaxX = textLine.getTextXForCharIndex(issueEnd - lineStartCharIndex);
        aPntr.setPaint(issue.isError() ? Color.RED : new Color(244, 198, 60));
        aPntr.setStroke(Stroke.StrokeDash1);
        aPntr.drawLine(issueX, issueY, issueMaxX, issueY);
        aPntr.setStroke(Stroke.Stroke1);

        // Paint the error message
        paintErrorMessage(aPntr, issue, textLine, getFont());
    }

    /**
     * Paints an error message.
     */
    private void paintErrorMessage(Painter aPntr, BuildIssue issue, TextLine textLine, Font errorFont)
    {
        // Get error string and X, Y, W, H, MidX, MidY
        String errorStr = issue.getText();
        double errorX = textLine.getMaxX() + 60;
        double errorY = textLine.getTextBaseline() + 1;
        double errorW = errorFont.getStringAdvance(errorStr);
        double errorH = errorFont.getAscent();
        double errorMidX = errorX + errorW / 2;
        double errorMidY = errorY + errorH / 2;

        // Paint string skewed a bit (italics)
        aPntr.save();
        aPntr.translate(errorMidX, errorMidY);
        aPntr.transform(1, 0, -.15, 1, 0, 0);
        aPntr.translate(-errorMidX, -errorMidY);
        aPntr.setColor(ERROR_TEXT_COLOR);
        aPntr.drawString(errorStr, errorX, errorY);
        aPntr.restore();
    }

    /**
     * Indents the text.
     */
    public void indentLines()
    {
        // Get start/end lines for selection
        TextSel textSel = getSel();
        TextLine startLine = textSel.getStartLine();
        TextLine endLine = textSel.getEndLine();
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
        TextLine startLine = textSel.getStartLine();
        TextLine endLine = textSel.getEndLine();
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
     * Adds or removes comments from selected lines.
     */
    public void commentLinesWithLineComment()
    {
        // Get start/end lines for selection
        TextSel textSel = getSel();
        TextLine startLine = textSel.getStartLine();
        TextLine endLine = textSel.getEndLine();
        int selStart = textSel.getStart();
        int selEnd = textSel.getEnd();

        // Assume adding comments if any line doesn't start with comment
        boolean addComments = false;
        for (TextLine line = startLine; line != endLine.getNext(); line = line.getNext()) {
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
    protected void sourceTextDidPropChange(PropChange anEvent)
    {
        // Do normal version and update TextPane.TextModified
        super.sourceTextDidPropChange(anEvent);

        // Get PropName
        String propName = anEvent.getPropName();

        // Handle Chars_Prop: Call didAddChars/didRemoveChars
        if (propName == TextDoc.Chars_Prop) {

            // Get CharsChange info
            TextBlockUtils.CharsChange charsChange = (TextBlockUtils.CharsChange) anEvent;
            int charIndex = anEvent.getIndex();
            CharSequence addChars = charsChange.getNewValue();
            CharSequence removeChars = charsChange.getOldValue();

            // Forward to did add/remove chars
            if (addChars != null)
                didAddChars(addChars, charIndex);
            else didRemoveChars(removeChars, charIndex);

            // Reset sel tokens
            setSelTokens(new TextToken[0]);
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
            TextLine startLine = getLineForCharIndex(charIndex);
            TextLine endLine = getLineForCharIndex(charIndex + charsLength);
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
            TextLine startLine = getLineForCharIndex(charIndex);
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
        TextDoc textDoc = (TextDoc) getSourceText();
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
        JavaTextPane javaTextPane = getOwner(JavaTextPane.class);
        if (javaTextPane == null)
            return -1;
        return javaTextPane.getProgramCounterLine();
    }

    /**
     * Override to enable hover node.
     */
    @Override
    protected void keyPressed(ViewEvent anEvent)
    {
        // If Shortcut, set HoverEnabled
        int keyCode = anEvent.getKeyCode();
        setHoverEnabled(keyCode == KeyCode.COMMAND || keyCode == KeyCode.CONTROL);

        // Do normal version
        super.keyPressed(anEvent);
    }

    /**
     * Override for hover node.
     */
    @Override
    protected void keyReleased(ViewEvent anEvent)
    {
        setHoverEnabled(false);
        super.keyReleased(anEvent);

        // Forward to java popup list
        JavaPopupList javaPopupList = getPopup();
        javaPopupList.updateForTextAreaKeyReleasedEvent(anEvent);
    }

    /**
     * Override to forward to popup list.
     */
    @Override
    protected void mousePressed(ViewEvent anEvent)
    {
        // If java popup showing, hide
        JavaPopupList javaPopupList = getPopup();
        if (javaPopupList.isShowing())
            javaPopupList.hide();

        // Do normal version
        super.mousePressed(anEvent);
    }

    /**
     * Override to set hover node.
     */
    @Override
    protected void mouseMoved(ViewEvent anEvent)
    {
        // Handle HoverEnabled + HoverNode
        if (isHoverEnabled()) {
            if (!anEvent.isShortcutDown())
                setHoverEnabled(false);
            else {
                JNode hoverNode = getNodeAtXY(anEvent.getX(), anEvent.getY());
                setHoverNode(hoverNode);
            }
        }

        // Do normal version
        super.mouseMoved(anEvent);
    }

    /**
     * Override to reset hover enabled.
     */
    @Override
    protected void setFocused(boolean aValue)
    {
        if (aValue == isFocused()) return;
        super.setFocused(aValue);
        if (!aValue)
            setHoverEnabled(false);
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
        if (theContent instanceof String) {
            JavaTextDoc javaTextDoc = (JavaTextDoc) getSourceText();
            if (javaTextDoc.isJepl())
                theContent = JavaTextUtils.removeExtraIndentFromString((String) theContent);
        }

        // Do normal version
        super.replaceCharsWithContent(theContent);
    }

    /**
     * Creates a dummy Java text doc. I don't like this!
     */
    private static JavaTextDoc createDummyTextDoc()
    {
        // Get/Create dummy java file
        String tempFilePath = FileUtils.getTempFile("Dummy.java").getAbsolutePath();
        WebURL tempFileURL = WebURL.getURL(tempFilePath);
        assert (tempFileURL != null);
        WebFile tempFile = tempFileURL.getFile();
        if (tempFile == null) {
            tempFile = tempFileURL.createFile(false);
            tempFile.setBytes(new byte[0]);
            tempFile.save();
        }

        // Return text doc for temp file
        return JavaTextDoc.getJavaTextDocForFile(tempFile);
    }
}
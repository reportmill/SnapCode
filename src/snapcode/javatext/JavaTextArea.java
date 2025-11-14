/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import javakit.parse.*;
import snap.geom.RoundRect;
import snap.parse.Tokenizer;
import snap.util.ArrayUtils;
import snapcode.project.JavaTextModel;
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

    // A helper class to manipulate text via nodes
    private JavaTextAreaNodeHpr _nodeHpr;

    // A PopupList to show code completion stuff
    private JavaPopupList _popup;

    // The Java File
    private WebFile _javaFile;

    // Whether to draw boxes around scope levels (class, methods, code blocks)
    private static boolean _showScopeBoxes;

    // Constants for properties
    public static final String SelNode_Prop = "SelNode";

    // Constants for indent
    protected static String INDENT_STRING = "    ";
    protected static int INDENT_LENGTH = INDENT_STRING.length();

    // Constants for colors
    private static final Color ERROR_TEXT_COLOR = Color.RED.brighter().brighter();
    private static final Color PROGRAM_COUNTER_LINE_HIGHLITE_COLOR = new Color(199, 218, 175, 200);
    private static final Color PRINT_MARGIN_COLOR = Color.GRAY9;
    private static final Color METHOD_COLOR = Color.get("#FAFAB4");
    private static final Color METHOD_STROKE_COLOR = METHOD_COLOR.darker();
    private static final Color CODE_BLOCK_COLOR = Color.get("#E9E9F8");
    private static final Color CODE_BLOCK_STROKE_COLOR = CODE_BLOCK_COLOR.darker();
    private static final Color CLASS_DECL_COLOR = Color.get("#E1F8E1");
    private static final Color CLASS_DECL_STROKE_COLOR = CLASS_DECL_COLOR.darker();

    // Temporary stand-in for text area init until real java text is assigned
    private static JavaTextModel _dummyJavaTextModel;

    /**
     * Constructor.
     */
    public JavaTextArea()
    {
        super(getDummyJavaTextModel());
        setFill(ViewTheme.get().getContentColor());
        setPadding(5, 5, 5,5);
        setSyncTextFont(false);
        setEditable(true);
        setUndoActivated(true);
    }

    /**
     * Override to create JavaText.
     */
    @Override
    protected TextAdapter createTextAdapter(TextModel textModel)  { return new JavaTextAdapter(textModel, this); }

    /**
     * Returns whether to paint boxes around .
     */
    public boolean isShowPrintMargin()  { return _showPrintMargin; }

    /**
     * Sets whether to draw line for print margin column.
     */
    public void setShowPrintMargin(boolean aValue)  { _showPrintMargin = aValue; }

    /**
     * Returns whether to draw boxes around scope levels (class, methods, code blocks).
     */
    public static boolean isShowScopeBoxes()  { return _showScopeBoxes; }

    /**
     * Sets whether to draw boxes around scope levels (class, methods, code blocks).
     */
    public static void setShowScopeBoxes(boolean aValue)
    {
        if (aValue == isShowScopeBoxes()) return;
        _showScopeBoxes = aValue;
    }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFile getJFile()
    {
        JavaTextModel javaTextModel = (JavaTextModel) getTextModel();
        return javaTextModel.getJFile();
    }

    /**
     * Returns the JavaTextAreaNodeHpr, a helper class to manipulate text with nodes.
     */
    public JavaTextAreaNodeHpr getNodeHpr()
    {
        if (_nodeHpr != null) return _nodeHpr;
        return _nodeHpr = new JavaTextAreaNodeHpr(this);
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
     * Returns all matching tokens for given node.
     */
    private TextToken[] getTokensForNode(JNode aNode)
    {
        // Get simple id node, if not found, return empty list
        JExprId idExpr = aNode instanceof JExprId ? (JExprId) aNode : null;
        if (idExpr == null)
            return new TextToken[0];

        // Get other matching nodes
        JExprId[] matchingIdNodes = NodeMatcher.getMatchingIdNodesForIdNode(idExpr);
        if (matchingIdNodes.length == 0)
            return new TextToken[0];

        // Return TextTokens for nodes
        return ArrayUtils.mapNonNull(matchingIdNodes, idnode -> getTokenForIdNode(idnode), TextToken.class);
    }

    /**
     * Returns the text token for given id node.
     */
    private TextToken getTokenForIdNode(JExprId idExpr)
    {
        // If node is zero length, return null
        if (idExpr.getCharLength() == 0)
            return null;

        // Get line index (skip if negative - assume Repl import statement or something)
        int lineIndex = idExpr.getLineIndex();
        if (lineIndex < 0)
            return null;

        // Get node line, then token from line (faster than having to find line by node startCharIndex)
        TextModel textModel = getTextModel();
        TextLine textLine = textModel.getLine(lineIndex);
        int textLineStartCharIndex = textLine.getStartCharIndex();
        int nodeStartCharIndex = idExpr.getStartCharIndex();
        int tokenStartCharIndexInLine = nodeStartCharIndex - textLineStartCharIndex;
        TextToken token = textLine.getTokenForCharIndex(tokenStartCharIndexInLine);
        if (token == null) // Should be impossible
            System.out.println("JavaTextArea.getTokensForNode: Can't find token for matching node: " + idExpr);

        // Return
        return token;
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
        if (isShowPrintMargin()) {
            double x = getPadding().getLeft() + getFont().charAdvance('X') * 120 + .5;
            aPntr.setColor(PRINT_MARGIN_COLOR);
            aPntr.setStroke(Stroke.Stroke1);
            aPntr.drawLine(x, 0, x, getHeight());
        }

        // Paint scope boxes (boxes around class, method, code blocks, etc.)
        if (_showScopeBoxes)
            paintScopeBoxes(aPntr);

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
            int tokenEnd = token.getEndCharIndex();
            if (issueEnd < tokenEnd)
                issueEnd = tokenEnd;
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
        double errorX = textLine.getMaxX() + 25;
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
        aPntr.drawString(errorStr, errorX, errorY, 0);
        aPntr.restore();
    }

    /**
     * Paints scope boxes around methods, code blocks, etc.
     */
    private void paintScopeBoxes(Painter aPntr)
    {
        // Get method/constructor/initializer decls
        JFile jFile = getJFile();
        JClassDecl classDecl = jFile.getClassDecl(); if (classDecl == null) return;
        JBodyDecl[] bodyDecls = classDecl.getBodyDecls();
        bodyDecls = ArrayUtils.filter(bodyDecls, bdecl -> bdecl instanceof WithBlockStmt);
        Rect clipBounds = aPntr.getClipBounds();

        // Paint box for classDecl
        paintScopeBoxForNodes(aPntr, classDecl, classDecl, 1);

        // Iterate over body decls
        for (JBodyDecl bodyDecl : bodyDecls) {

            // If body decl not visible, skip it
            int bodyDeclEndCharIndex = bodyDecl.getEndCharIndex();
            TextLine bodyDeclEndLine = getLineForCharIndex(bodyDeclEndCharIndex);
            if (bodyDeclEndLine.getMaxY() < clipBounds.y)
                continue;

            // Paint method box
            paintScopeBoxForNodeWithBlock(aPntr, bodyDecl, 2);

            // If body decl end not visible, stop loop
            if (bodyDeclEndLine.getTextMaxY() >= clipBounds.getMaxY())
                break;
        }
    }

    /**
     * Paints scope box for given node with block.
     */
    private void paintScopeBoxForNodeWithBlock(Painter aPntr, JNode nodeWithBlock, int level)
    {
        // Paint outer box
        paintScopeBoxForNodes(aPntr, nodeWithBlock, nodeWithBlock, level);

        // Paint content box
        JStmtBlock blockStmt = ((WithBlockStmt) nodeWithBlock).getBlock();
        List<JStmt> blockStmts = blockStmt != null ? blockStmt.getStatements() : null;
        if (blockStmts == null || blockStmts.isEmpty())
            return;

        // Paint inner box (white)
        JStmt startStmt = blockStmts.get(0);
        while (blockStmt.getLineIndex() == startStmt.getLineIndex() && startStmt.getNextStmt() != null)
            startStmt = startStmt.getNextStmt();
        JStmt endStmt = blockStmts.get(blockStmts.size() - 1);
        if (blockStmt.getLineIndex() == endStmt.getLineIndex())
            return;
        paintScopeBoxForNodes(aPntr, startStmt, endStmt, level + 1);

        // Paint inner blocks
        for (JStmt stmt : blockStmts) {
            if (stmt instanceof WithBlockStmt && ((WithBlockStmt) stmt).isBlockSet())
                paintScopeBoxForNodeWithBlock(aPntr, stmt, level + 2);
        }
    }

    /**
     * Paints scope box.
     */
    private void paintScopeBoxForNodes(Painter aPntr, JNode startNode, JNode endNode, int level)
    {
        // Get start/end line for nodes
        TextModel textModel = getTextModel();
        int startCharIndex = startNode.getStartCharIndex();
        int endCharIndex = endNode.getEndCharIndex();
        TextLine startLine = textModel.getLineForCharIndex(startCharIndex);
        while (isPreviousLineComment(startLine))
            startLine = startLine.getPrevious();
        TextLine endLine = textModel.getLineForCharIndex(endCharIndex);

        // Get scope rect for start/end lines
        double strokeX = startLine.getTextXForCharIndex(startLine.getIndentLength()) - 3;
        double strokeY = startLine.getTextY() - 3;
        double strokeW = getWidth() - strokeX - 4 * level;
        double strokeH = endLine.getTextMaxY() + 3 - strokeY;
        RoundRect methodRoundRect = new RoundRect(strokeX, strokeY, strokeW, strokeH, 4);

        Color fillColor = getScopeColor(startNode, endNode, false);
        Color strokeColor = getScopeColor(startNode, endNode, true);

        // Paint scope box
        aPntr.setColor(fillColor);
        aPntr.fill(methodRoundRect);
        aPntr.setColor(strokeColor);
        aPntr.setStrokeWidth(1);
        aPntr.draw(methodRoundRect);
    }

    /**
     * Returns the color to use for scope box for given node.
     */
    private static Color getScopeColor(JNode aNode, JNode endNode, boolean isStrokeColor)
    {
        // Method/constructor/initializer all paint in yellow
        if (aNode instanceof JExecutableDecl || aNode instanceof JInitializerDecl)
            return isStrokeColor ? METHOD_STROKE_COLOR : METHOD_COLOR;

        // Blocks paint purple
        if (aNode instanceof WithBlockStmt && ((WithBlockStmt) aNode).isBlockSet() && aNode == endNode)
            return isStrokeColor ? CODE_BLOCK_STROKE_COLOR : CODE_BLOCK_COLOR;

        // Class paints green
        if (aNode instanceof JClassDecl)
            return isStrokeColor ? CLASS_DECL_STROKE_COLOR : CLASS_DECL_COLOR;

        // Method body is white
        return isStrokeColor ? CLASS_DECL_STROKE_COLOR : Color.WHITE;
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
            getTextModel().addChars(INDENT_STRING, startLine.getStartCharIndex());

            // Adjust Sel start/end
            if (startCharIndex <= selStart)
                selStart += INDENT_LENGTH;
            if (startCharIndex <= selEnd)
                selEnd += INDENT_LENGTH;

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
            while (endCharIndex < startLine.length() && startLine.charAt(endCharIndex) == ' ' && endCharIndex - startCharIndex < INDENT_LENGTH)
                endCharIndex++;

            // If indent found, remove it
            if (endCharIndex != startCharIndex) {

                // Convert indexes to TextModel
                int lineStartCharIndex = startLine.getStartCharIndex();
                startCharIndex += lineStartCharIndex;
                endCharIndex += lineStartCharIndex;

                // Remove chars
                getTextModel().removeChars(startCharIndex, endCharIndex);

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
                getTextModel().addChars("//", startLine.getStartCharIndex());
                if (startCharIndex <= selStart)
                    selStart += 2;
                if (startCharIndex <= selEnd)
                    selEnd += 2;
            }

            // If removing comment, remove comment chars and adjust SelStart/SelEnd
            else {
                int commentCharIndex = startCharIndex + startLine.getString().indexOf("//");
                getTextModel().removeChars(commentCharIndex, commentCharIndex + 2);
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
    protected void handleSourceTextPropChange(PropChange anEvent)
    {
        // Do normal version and update TextPane.TextModified
        super.handleSourceTextPropChange(anEvent);

        // Get PropName
        String propName = anEvent.getPropName();

        // Handle Chars_Prop: Call didAddChars/didRemoveChars
        if (propName == TextModel.Chars_Prop) {

            // Get CharsChange info
            TextModelUtils.CharsChange charsChange = (TextModelUtils.CharsChange) anEvent;
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
            int startLineIndex = startLine.getLineIndex();
            int endLineIndex = endLine.getLineIndex();
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
            int startLineIndex = startLine.getLineIndex();
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
    public WebFile getJavaFile()
    {
        if (_javaFile != null) return _javaFile;
        JavaTextPane javaTextPane = getOwner(JavaTextPane.class);
        if (javaTextPane != null)
            _javaFile = javaTextPane.getTextFile();
        return _javaFile;
    }

    /**
     * Sets the source file.
     */
    public void setJavaFile(WebFile javaFile)  { _javaFile = javaFile; }

    /**
     * Returns BuildIssues from ProjectFile.
     */
    public BuildIssue[] getBuildIssues()
    {
        WebFile javaFile = getJavaFile(); if (javaFile == null) return new BuildIssue[0];
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
        return javaAgent.getBuildIssues();
    }

    /**
     * Returns the workspace breakpoints.
     */
    private Breakpoints getWorkspaceBreakpoints()
    {
        WebFile javaFile = getJavaFile();
        Project proj = javaFile != null ? Project.getProjectForFile(javaFile) : null;
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

        // Get java file and return breakpoints for file
        WebFile javaFile = getJavaFile();
        return breakpointsHpr.getBreakpointsForFile(javaFile);
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

        // Get java file and add breakpoint for file line
        WebFile javaFile = getJavaFile();
        breakpointsHpr.addBreakpointForFile(javaFile, aLine);
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
     * Override to handle TextAdapter changes.
     */
    @Override
    protected void handleTextAdapterPropChange(PropChange aPC)
    {
        // Do normal version
        super.handleTextAdapterPropChange(aPC);

        switch (aPC.getPropName()) {

            // Handle SourceText change: Init SelNode, DeepNode
            case TextAdapter.TextModel_Prop: _selNode = _deepNode = getJFile(); break;

            // Handle Selection change: Reset SelNode
            case TextAdapter.Selection_Prop:
                int selStart = getSelStart(), selEnd = getSelEnd();
                JNode node = getNodeForCharRange(selStart, selEnd);
                setSelNode(node);
                break;
        }
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
     * Override to update fill.
     */
    @Override
    protected void handleThemeChange(ViewTheme oldTheme, ViewTheme newTheme)
    {
        super.handleThemeChange(oldTheme, newTheme);
        setFill(ViewTheme.get().getContentColor());
        setTextColor(ViewTheme.get().getTextColor());
    }

    /**
     * Creates a dummy JavaTextModel. I don't like this!
     */
    private static JavaTextModel getDummyJavaTextModel()
    {
        if (_dummyJavaTextModel != null) return _dummyJavaTextModel;

        // Get/Create dummy java file
        WebFile tempFile = WebFile.createTempFileForName("Dummy.java", false);
        if (!tempFile.getExists()) {
            tempFile.setBytes(new byte[0]);
            tempFile.save();
        }

        // Return JavaTextModel for temp file
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(tempFile);
        return _dummyJavaTextModel = javaAgent.getJavaTextModel();
    }

    /**
     * Returns whether previous line is comment.
     */
    private static boolean isPreviousLineComment(TextLine aLine)
    {
        // Get name of first token in previous line (just return if not found)
        TextLine prevLine = aLine.getPrevious();
        TextToken prevLineToken = prevLine != null && prevLine.getTokenCount() > 0 ? prevLine.getToken(0) : null;
        String tokenName = prevLineToken != null ? prevLineToken.getName() : null;
        if (tokenName == null)
            return false;

        // Return true if name is comment
        return tokenName.equals(Tokenizer.SINGLE_LINE_COMMENT) || tokenName.equals(Tokenizer.MULTI_LINE_COMMENT);
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.parse.JExprId;
import javakit.parse.JNode;
import javakit.parse.JStmtBlock;
import javakit.resolver.JavaClass;
import snap.geom.*;
import snap.parse.ParseToken;
import snap.text.*;
import snap.view.*;
import snapcode.app.WorkspacePane;
import snapcode.javatext.JavaTextUtils;
import java.util.Objects;

/**
 * A class to manage a Java inspector.
 */
public class CompleterTool extends SnippetTool.ChildTool {

    // The selected class
    private JavaClass _selClass;

    // The selected prefix
    private String _selPrefix;

    // The suggestion list
    private ListView<CompleterBlock> _suggestionsList;

    // The dragging CodeBlock
    private CompleterBlock _dragCodeBlock;

    // The current drag point
    private Point _dragPoint;

    // The current node at drag point
    private JNode _dragNode;

    // The current statement node at drag point
    private JNode _dragBlock;

    // The drag text
    private TextBox _dragText;

    /**
     * Constructor.
     */
    public CompleterTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Sets the selected class and prefix.
     */
    public void setSelClassAndPrefix(JavaClass javaClass, String prefix)
    {
        // If already set, just return
        if (javaClass == _selClass && Objects.equals(prefix, _selPrefix)) return;

        // Set values
        _selClass = javaClass;
        _selPrefix = prefix;

        // Get suggested CodeBlocks for class and set in Suggestions list
        CompleterBlock[] codeBlocks = CompleterBlock.getCodeBlocksForNode(javaClass, prefix);
        _suggestionsList.setItems(codeBlocks);
        resetLater();
    }

    /**
     * Sets CodeBlocks for current TextArea.SelectedNode.
     */
    public void resetSelClassFromJavaTextArea()
    {
        // Get SelectedNode (or first node parent with class) and its class
        JNode selNode = _javaTextArea.getSelNode();
        while (selNode != null && selNode.getEvalClass() == null)
            selNode = selNode.getParent();

        // Get JavaClass for SelNode and prefix if SelNode is local variable name
        JavaClass javaClass = selNode != null ? selNode.getEvalClass() : null;
        String localVarName = selNode instanceof JExprId && ((JExprId) selNode).isVarId() ? selNode.getName() : null;

        // Set selected class and prefix
        setSelClassAndPrefix(javaClass, localVarName);
    }

    /**
     * Initializes UI panel.
     */
    public void initUI()
    {
        // Get suggestion list
        _suggestionsList = getView("SuggestionsList", ListView.class);
        _suggestionsList.setCellConfigure(this::configureSuggestionsList);
        _suggestionsList.addEventHandler(this::handleSuggestingListDragGestureEvent, ViewEvent.Type.DragGesture);
        _suggestionsList.addEventHandler(e -> _dragCodeBlock = null, ViewEvent.Type.DragSourceEnd);
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        String className = _selClass != null ? _selClass.getClassName() : null;
        setViewValue("ClassText", className != null ? className + " Methods" : "No Selection");
    }

    /**
     * Called when SuggestionList gets DragGesture.
     */
    private void handleSuggestingListDragGestureEvent(ViewEvent anEvent)
    {
        // Set DragSuggestion and DragString
        _dragCodeBlock = (CompleterBlock) anEvent.getSelItem();
        String dragString = _dragCodeBlock.getString();

        // Get event clipboard and start drag
        Clipboard clipboard = anEvent.getClipboard();
        clipboard.addData(dragString);
        clipboard.startDrag();
    }

    /**
     * Called to configure ListCell.
     */
    protected void configureSuggestionsList(ListCell<CompleterBlock> aCell)
    {
        CompleterBlock codeBlock = aCell.getItem();
        if (codeBlock == null) return;
        aCell.setText(codeBlock.getString());
        aCell.setImage(JavaTextUtils.CodeImage);
        aCell.getGraphic().setPadding(4, 4, 4, 4);
    }

    /**
     * Called when JavaTextArea SelNode prop changes.
     */
    @Override
    protected void javaTextAreaSelNodeChanged()
    {
        resetSelClassFromJavaTextArea();
    }

    /**
     * Called when JavaTextArea gets drag events.
     */
    protected void handleJavaTextAreaDragEvent(ViewEvent anEvent)
    {
        if (anEvent.isDragOver())
            dragOver(anEvent.getX(), anEvent.getY());
        else if (anEvent.isDragExit())
            dragExit();
        else if (anEvent.isDragDropEvent())
            drop(0, 0);
    }

    /**
     * Called when drag is over TextArea.
     */
    public void dragOver(double anX, double aY)
    {
        // Bail?
        if (_dragCodeBlock == null) return;

        // Set DragPoint and register TextArea to repaint
        _dragPoint = new Point(anX, aY);
        _javaTextArea.repaint();

        // Set DragNode
        int dragCharIndex = _javaTextArea.getCharIndexForXY(anX, aY);
        _dragNode = _javaTextArea.getJFile().getNodeForCharIndex(dragCharIndex);

        // Get DragBlock
        _dragBlock = _dragNode instanceof JStmtBlock ? (JStmtBlock) _dragNode : _dragNode.getParent(JStmtBlock.class);
        if (_dragBlock == null) {
            clearDrag();
            return;
        }

        // Get token
        ParseToken dragNodeToken = _dragBlock.getStartToken();
        if (!(dragNodeToken instanceof TextToken)) {
            System.out.println("CompleterTool: Node token no longer TextToken - update this code");
            return;
        }

        // Make sure Y is below DragBlock first line
        TextToken dragBlockToken = (TextToken) _dragBlock.getStartToken();
        TextLine dragBlockLine = dragBlockToken.getTextLine();
        if (aY < dragBlockLine.getY() + dragBlockLine.getMetrics().getLineAdvance())
            _dragPoint = new Point(anX, dragBlockLine.getY() + dragBlockLine.getMetrics().getLineAdvance() + 1);

        // Get DragBlock.String with indent
        TextToken dragToken = (TextToken) _dragNode.getStartToken();
        TextLine textLine = dragToken.getTextLine();
        String indent = textLine.getIndentString();
        String dragString = indent + _dragCodeBlock.getString();

        // If DragText needs to be reset, create and reset
        if (_dragText == null || !_dragText.getString().equals(dragString)) {
            TextBlock textBlock = _javaTextArea.getTextBlock();
            _dragText = new TextBox();
            _dragText.setX(textBlock.getX());
            _dragText.setString(dragString);
        }
    }

    /**
     * Called when drag exits TextArea.
     */
    public void dragExit()
    {
        clearDrag();
    }

    /**
     * Drop suggestion at point.
     */
    public void drop(double anX, double aY)
    {
        // Get insert string for drop
        TextBlock textBlock = _javaTextArea.getTextBlock();
        TextLine textLine = textBlock.getLineForY(_dragPoint.getY());
        CharSequence indentString = textLine.getIndentString();
        String dropCodeString = _dragCodeBlock.getReplaceString();
        String insertStr = indentString + dropCodeString + "\n";

        // Add insert string
        int selStart = textLine.getStartCharIndex();
        _javaTextArea.replaceChars(insertStr, null, selStart, selStart, false);
        _javaTextArea.setSel(selStart + indentString.length(), selStart + indentString.length() + dropCodeString.length());

        // Clear drag
        _javaTextArea.requestFocus();
        clearDrag();
    }

    /**
     * Clears the drag information.
     */
    private void clearDrag()
    {
        _dragText = null;
        _dragNode = _dragBlock = null;
    }

    /**
     * Override to provide hook for CodeBuilder to paint.
     */
    /*protected boolean paintTextSelection(JavaTextArea aTextArea, Graphics2D aGraphics)
    {
        // If DragBlock is null, return false
        if(_dragBlock==null || _dragText==null) return false;

        // Get SelectionPath for DragBlock and paint
        TextToken startToken = aTextArea.getText().getTokenAt(_dragBlock.getStart());
        TextToken endToken = aTextArea.getText().getTokenAt(_dragBlock.getEnd());
        double x = Math.min(startToken.getX(), endToken.getX()) - 2, w = aTextArea.getWidth() - x - 10;
        double y = startToken.getY() - 3, h = endToken.getMaxY() - y + _dragText.getPrefHeight() + 2;
        Rectangle2D rect = new Rectangle2D(x, y, w, h);
        aGraphics.setColor(_dbFill); aGraphics.fill(rect);
        aGraphics.setColor(_dbStroke); aGraphics.setStroke(new BasicStroke(2)); aGraphics.draw(rect);
        return true;
    }*/

    // DragBlock paint colors
    //private static Color _dbFill = new Color(255, 235, 235), _dbStroke = new Color(160,160,255);

    /**
     * Paints a TextLine.
     */
    /*protected boolean paintLine(JavaTextArea aTextArea, Graphics2D aGraphics, TextLine aLine, double anX, double aY)
    {
        // Get DragText and DragPoint (return false if no DragText or not to DragPoint yet)
        Text dragText = _dragText; if(dragText==null) return false;
        Point2D dragPoint = _dragPoint; if(aLine.getMaxY()<=dragPoint.getY()) return false;
        double y = aY;

        // If Line straddles DragPoint, paint DragText.Line
        if(aLine.getMaxY() - aLine.getLineAdvance()<=dragPoint.getY()) { // Draw DragText if Line straddles DragPoint
            TextLine dragLine = dragText.getLine(0); TextToken dragLineToken0 = dragLine.getToken(0);
            double x = dragLineToken0.getX(), w = dragLine.getMaxX() - x;
            aGraphics.setColor(Color.decode("#FFF280"));
            aGraphics.fillRect((int)x, (int)aLine.getY()-2, (int)w, (int)aLine.getHeight()+2);
            //aTextArea.paintLineImpl(aGraphics, dragLine, 0, y);
        }
        y += aLine.getLineAdvance();

        // Do normal version
        aTextArea.paintLineImpl(aGraphics, aLine, anX, y);
        return true;
    }*/

    /**
     * Returns the tool title.
     */
    @Override
    public String getTitle()  { return "Completer"; }
}
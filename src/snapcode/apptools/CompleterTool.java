/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import java.lang.reflect.Method;
import java.util.*;
import javakit.parse.JNode;
import javakit.parse.JStmtBlock;
import javakit.resolver.JavaClass;
import snap.geom.*;
import snap.parse.ParseToken;
import snap.props.PropChangeListener;
import snap.text.*;
import snap.view.*;
import snap.view.EventListener;
import snapcode.app.JavaPage;
import snapcode.app.PagePane;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextUtils;
import snapcode.webbrowser.WebPage;

/**
 * A class to manage a Java inspector.
 */
public class CompleterTool extends WorkspaceTool {

    // The JavaTextArea this inspector works for.
    private JavaTextArea _textArea;

    // The selected node
    private JNode  _node;

    // The suggestion list
    private ListView<CompleterBlock>  _suggestionsList;

    // The dragging CodeBlock
    private CompleterBlock _dragCodeBlock;

    // The current drag point
    private Point  _dragPoint;

    // The current node at drag point
    private JNode  _dragNode, _dragBlock;

    // The drag text
    private TextBox _dragText;

    // Listener for JavaTextArea prop change
    private PropChangeListener _textAreaPropChangeLsnr = pc -> javaTextAreaSelNodeChanged();

    // Listener for JavaTextArea drag events
    private EventListener _textAreaDragEventLsnr = e -> handleJavaTextAreaDragEvent(e);

    /**
     * Creates a new JavaInspector.
     */
    public CompleterTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the JavaTextArea associated with text pane.
     */
    public JavaTextArea getTextArea()  { return _textArea; }

    /**
     * Sets the JavaTextArea associated with text pane.
     */
    private void setTextArea(JavaTextArea textArea)
    {
        if (textArea == _textArea) return;

        if (_textArea != null) {
            _textArea.removePropChangeListener(_textAreaPropChangeLsnr);
            _textArea.removeEventHandler(_textAreaDragEventLsnr);
        }

        _textArea = textArea;

        // Start listening to JavaTextArea SelNode prop and drag events
        if (_textArea != null) {
            _textArea.addPropChangeListener(_textAreaPropChangeLsnr, JavaTextArea.SelNode_Prop);
            _textArea.addEventHandler(_textAreaDragEventLsnr, View.DragEvents);
            javaTextAreaSelNodeChanged();
        }
    }

    /**
     * Whether inspector is visible.
     */
    public boolean isVisible()
    {
        return isUISet() && getUI().isVisible();
    }

    /**
     * Sets CodeBlocks for current TextArea.SelectedNode.
     */
    public void setCodeBlocks()
    {
        // Get SelectedNode (or first node parent with class) and its class
        _node = getTextArea().getSelNode();
        while (_node != null && _node.getEvalClass() == null)
            _node = _node.getParent();

        // Get suggested CodeBlocks for class and set in Suggestions list
        Object[] items = getCodeBlocks(_node);
        setViewItems(_suggestionsList, items);
        resetLater();
    }

    /**
     * Returns suggestions for class.
     */
    private CompleterBlock[] getCodeBlocks(JNode aNode)
    {
        JavaClass javaClass = _node != null ? _node.getEvalClass() : null;
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        Method[] methods = realClass != null ? realClass.getMethods() : null;
        if (methods == null)
            return new CompleterBlock[0];

        List<CompleterBlock> codeBlocks = new ArrayList<>();

        for (Method method : methods) {
            if (method.getDeclaringClass() == Object.class)
                continue;
            CompleterBlock codeBlock = new CompleterBlock().init(aNode, method);
            codeBlocks.add(codeBlock);
        }

        // Return
        return codeBlocks.toArray(new CompleterBlock[0]);
    }

    /**
     * Initializes UI panel.
     */
    public void initUI()
    {
        // Get suggestion list
        _suggestionsList = getView("SuggestionsList", ListView.class);
        enableEvents(_suggestionsList, ViewEvent.Type.DragGesture, ViewEvent.Type.DragSourceEnd);
        _suggestionsList.setCellConfigure(this::configureSuggestionsList);
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        String className = _node != null ? _node.getEvalClassName() : null;
        setViewValue("ClassText", className != null ? className + " Methods" : "No Selection");
    }

    /**
     * Responds to UI.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SuggestionsList
        if (anEvent.is("SuggestionsList")) {

            // Handle DragGesture
            if (anEvent.isDragGesture()) {

                // Set DragSuggestion and DragString
                _dragCodeBlock = (CompleterBlock) anEvent.getSelItem();
                String dragString = _dragCodeBlock.getString();

                // Get event dboard and start drag
                Clipboard cboard = anEvent.getClipboard();
                cboard.addData(dragString);
                //cboard.setDragImageFromString(dragString, getTextArea().getFont().deriveFont(10f));
                //cboard.setDragImagePoint(0, dboard.getDragImage().getHeight()/2);
                cboard.startDrag();
            }

            // Handle DragSourceEnd
            if (anEvent.isDragSourceEnd()) _dragCodeBlock = null;
        }
    }

    /**
     * Init showing.
     */
    @Override
    protected void initShowing()
    {
        // Start listening to PagePane.SelFile prop change
        _pagePane.addPropChangeListener(pc -> pagePaneSelFileChanged(), PagePane.SelFile_Prop);
        pagePaneSelFileChanged();
    }

    /**
     * Called to configure ListCell.
     */
    protected void configureSuggestionsList(ListCell<CompleterBlock> aCell)
    {
        CompleterBlock cb = aCell.getItem();
        if (cb == null) return;
        aCell.setText(cb.getString());
        aCell.setImage(JavaTextUtils.CodeImage);
        aCell.getGraphic().setPadding(4, 4, 4, 4);
    }

    /**
     * Called when JavaTextArea gets drag events.
     */
    private void handleJavaTextAreaDragEvent(ViewEvent anEvent)
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
        JavaTextArea textArea = getTextArea();
        textArea.repaint();

        // Set DragNode
        int index = textArea.getCharIndexForXY(anX, aY);
        _dragNode = textArea.getJFile().getNodeForCharIndex(index);

        // Get DragBlock
        _dragBlock = _dragNode;
        while (_dragBlock != null && !(_dragBlock instanceof JStmtBlock)) _dragBlock = _dragBlock.getParent();
        if (_dragBlock == null) {
            clearDrag();
            return;
        }

        ParseToken dragNodeToken = _dragBlock.getStartToken();
        if (!(dragNodeToken instanceof TextBoxToken)) {
            System.out.println("CompleterTool: Node token no longer TextBoxToken - update this code");
            return;
        }

        // Make sure Y is below DragBlock first line
        TextBoxToken dragBlockToken = (TextBoxToken) _dragBlock.getStartToken();
        TextBoxLine dragBlockLine = dragBlockToken.getTextLine();
        if (aY < dragBlockLine.getY() + dragBlockLine.getLineAdvance())
            _dragPoint = new Point(anX, dragBlockLine.getY() + dragBlockLine.getLineAdvance() + 1);

        // Get DragBlock.String with indent
        TextBoxToken dragToken = (TextBoxToken) _dragNode.getStartToken();
        TextBoxLine line = dragToken.getTextLine();
        String indent = getIndentString(line.getIndex());
        String dragString = indent + _dragCodeBlock.getString();

        // If DragText needs to be reset, create and reset
        if (_dragText == null || !_dragText.getString().equals(dragString)) {
            TextBox textBox = textArea.getTextBox();
            _dragText = new TextBox();
            _dragText.setX(textBox.getX());
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
        JavaTextArea textArea = getTextArea();
        TextBoxLine line = textArea.getTextBox().getLineForY(_dragPoint.getY());
        CharSequence indent = getIndentString(line.getIndex());
        String string = _dragCodeBlock.getReplaceString(), fullString = indent + string + "\n";
        int selStart = line.getStartCharIndex();
        textArea.replaceChars(fullString, null, selStart, selStart, false);
        textArea.setSel(selStart + indent.length(), selStart + indent.length() + string.length());
        //int argStart = string.indexOf('('), argEnd = argStart>0? string.indexOf(')', argStart) : -1;
        //if(argEnd>argStart+1) textArea.setSelection(selStart + argStart + 1, selStart + argEnd);
        textArea.requestFocus();
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
     * Returns the number of indent spaces for line at given index.
     */
    public int getIndentCount(int anIndex)
    {
        if (anIndex == 0) return 0;

        JavaTextArea textArea = getTextArea();
        TextBox textBox = textArea.getTextBox();
        TextBoxLine line = textBox.getLine(anIndex - 1);

        int indentCount = 0;
        while (indentCount < line.length() && Character.isWhitespace(line.charAt(indentCount)))
            indentCount++;
        if (!line.getString().trim().endsWith(";"))
            indentCount += 4;

        // Return
        return indentCount;
    }

    /**
     * Returns the indent string for line at given index.
     */
    public String getIndentString(int anIndex)
    {
        int indentCount = getIndentCount(anIndex);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indentCount; i++) sb.append(' ');
        return sb.toString();
    }

    /**
     * Called when PagePane.SelFile property changes
     */
    private void pagePaneSelFileChanged()
    {
        WebPage selPage = _pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        JavaTextArea javaTextArea = javaPage != null ? javaPage.getTextArea() : null;
        setTextArea(javaTextArea);
    }

    /**
     * Called when JavaTextArea SelNode prop changes.
     */
    private void javaTextAreaSelNodeChanged()
    {
        if (isVisible())
            setCodeBlocks();
    }

    /**
     * Returns the tool title.
     */
    @Override
    public String getTitle()  { return "Completer"; }
}
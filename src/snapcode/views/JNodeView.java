package snapcode.views;
import javakit.parse.*;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeView<JNODE extends JNode> extends JNodeViewBase {

    // The JNode
    private JNODE _jnode;

    // The children node owners
    protected List<JNodeView<?>> _jnodeViews;

    // The current drag over node
    private static JNodeViewBase _dragOver;

    // The SnapPart being dragged
    public static JNodeView<?>  _dragSnapPart;

    // Constants for colors
    public static Color PieceColor = Color.get("#4C67d6");
    public static Color BlockStmtColor = Color.get("#8f56e3");
    public static Color MemberDeclColor = Color.get("#f0a822");
    public static Color ClassDeclColor = MemberDeclColor; //Color.get("#27B31E");

    // Constants for fonts
    protected static final Font LABEL_FONT = new Font("Arial Bold", 13);
    protected static final Font TEXTFIELD_FONT = new Font("Arial", 12);

    /**
     * Constructor.
     */
    public JNodeView()
    {
        super();
    }

    /**
     * Constructor for given JNode.
     */
    public JNodeView(JNODE aJN)
    {
        super();
        setJNode(aJN);
    }

    /**
     * Returns the JNode.
     */
    public JNODE getJNode()  { return _jnode; }

    /**
     * Sets the JNode.
     */
    public void setJNode(JNODE aJNode)
    {
        _jnode = aJNode;
        updateUI();
    }

    /**
     * Updates the UI.
     */
    protected void updateUI()
    {
        // Add child UI
        if (isBlock()) {
            List<JNodeView<?>> nodeViews = getJNodeViews();
            if (nodeViews.size() > 0) {
                ColView colView = getColView();
                nodeViews.forEach(colView::addChild);
            }
        }

        if (_jnode.getFile() == null)
            return;
        enableEvents(DragEvents);
    }

    /**
     * Returns the SnapEditor.
     */
    public SnapEditor getEditor()
    {
        return getJNodeViewParent() != null ? getJNodeViewParent().getEditor() : null;
    }

    /**
     * Returns whether NodePane is a block.
     */
    public boolean isBlock()
    {
        return getJNode() instanceof WithBlockStmt;
    }

    /**
     * Returns the parent.
     */
    public JNodeView<?> getJNodeViewParent()
    {
        return getParent(JNodeView.class);
    }

    /**
     * Returns the number of children.
     */
    public int getJNodeViewCount()
    {
        return _jnodeViews != null ? _jnodeViews.size() : 0;
    }

    /**
     * Returns the individual child.
     */
    public JNodeView<?> getJNodeView(int anIndex)
    {
        return _jnodeViews.get(anIndex);
    }

    /**
     * Returns the individual child.
     */
    public JNodeView<?> getJNodeViewLast()
    {
        int cc = getJNodeViewCount();
        return cc > 0 ? _jnodeViews.get(cc - 1) : null;
    }

    /**
     * Returns the children.
     */
    public List<JNodeView<?>> getJNodeViews()
    {
        if (_jnodeViews != null) return _jnodeViews;
        List<JNodeView<?>> nodeViews = createJNodeViews();
        return _jnodeViews = nodeViews;
    }

    /**
     * Creates the children.
     */
    protected List<JNodeView<?>> createJNodeViews()
    {
        JNode jnode = getJNode();
        JStmtBlock blockStmt = jnode instanceof WithBlockStmt ? ((WithBlockStmt) jnode).getBlock() : null;
        if (blockStmt == null)
            return Collections.EMPTY_LIST;

        // Get statements
        List<JStmt> statements = blockStmt.getStatements();
        List<JNodeView<?>> children = new ArrayList<>();

        // Iterate over statements and create views
        for (JStmt stmt : statements) {
            JNodeView<?> stmtView = createView(stmt);
            if (stmtView == null)
                continue;
            children.add(stmtView);
        }

        // Return children
        return children;
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        return getJNode().getNodeString();
    }

    /**
     * Creates a label for this block.
     */
    protected Label createLabel(String aString)
    {
        Label label = new Label(aString);
        label.setPadding(2, 4, 2, 0);
        label.setFont(LABEL_FONT);
        label.setTextFill(Color.WHITE);
        return label;
    }

    /**
     * Creates a textfield for this block.
     */
    protected TextField createTextField(String aString)
    {
        TextField textField = new TextField();
        textField.setPadding(2, 6, 2, 6);
        textField.setAlign(Pos.CENTER);
        textField.setColCount(0);
        textField.setFont(TEXTFIELD_FONT);
        textField.setText(aString);
        textField.setMinWidth(36);
        textField.setPrefHeight(18);
        return textField;
    }

    /**
     * ProcessEvent.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        if (anEvent.isDragEvent()) handleDragEvent(anEvent);
        else super.processEvent(anEvent);
    }

    /**
     * Responds to drag events.
     */
    protected void handleDragEvent(ViewEvent anEvent)
    {
        // Handle DragEvent: Accept drag event
        //DragEvent de = anEvent.getEvent(DragEvent.class); de.acceptTransferModes(TransferMode.COPY); de.consume();
        anEvent.acceptDrag();
        anEvent.consume();

        // Handle DragEnter, DragOver, DragExit: Apply/clear effect from DragEffectNode
        if (anEvent.isDragEnter() || anEvent.isDragOver() && _dragOver != this) {
            if (_dragOver != null) _dragOver.setUnderDrag(false);
            setUnderDrag(true);
            _dragOver = this;
        }
        if (anEvent.isDragExit()) {
            setUnderDrag(false);
            if (_dragOver == this) _dragOver = null;
        }

        // Handle DragDropEvent
        if (anEvent.isDragDropEvent() && _dragSnapPart != null) {
            if (_dragOver != null) _dragOver.setUnderDrag(false);
            _dragOver = null;
            dropNode(_dragSnapPart.getJNode(), anEvent.getX(), anEvent.getY());
            anEvent.dropComplete(); //de.setDropCompleted(true);
        }
    }

    /**
     * Drops a node at center of part.
     */
    protected void dropNode(JNode aJNode)
    {
        dropNode(aJNode, getWidth() / 2, getHeight() / 2);
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        System.out.println("Cannot add node to " + getClass().getSimpleName());
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getJNode().toString();
    }

    /**
     * Returns the SnapPart of a node.
     */
    public static JNodeView<?> getJNodeView(View aView)
    {
        if (aView instanceof JNodeView)
            return (JNodeView<?>) aView;
        return aView.getParent(JNodeView.class);
    }

    /**
     * Creates a SnapPart for a JNode.
     */
    public static JNodeView<?> createView(JNode aNode)
    {
        JNodeView<?> nodeView = null;
        if (aNode instanceof JFile)
            nodeView = new JFileView();
        else if (aNode instanceof JMemberDecl)
            nodeView = JMemberDeclView.createView(aNode);
        else if (aNode instanceof JStmt)
            nodeView = JStmtView.createView(aNode);
        else if (aNode instanceof JExpr)
            nodeView = JExprView.createView((JExpr) aNode);
        else if (aNode instanceof JType)
            nodeView = new JTypeView<>();
        if (nodeView == null)
            return null;

        ((JNodeView<JNode>) nodeView).setJNode(aNode);

        // Return
        return nodeView;
    }
}
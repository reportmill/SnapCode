package snapcode.views;
import javakit.parse.*;
import snap.geom.Insets;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.Painter;
import snap.util.ListUtils;
import snap.view.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeView<JNODE extends JNode> extends ChildView {

    // The JNode
    protected JNODE _jnode;

    // Background shape
    protected BlockView _blockView;

    // The row view
    private RowView _rowView;

    // The col view
    private ColView _colView;

    // Whether part is selected
    private boolean _selected;

    // whether under drag
    private boolean _underDrag;

    // Constants for block insets
    private double _paddingLeft;

    // The child node views
    protected JNodeView<?>[] _jnodeViews;

    // The block statement views
    protected JNodeView<?>[] _blockStmtViews;

    // The current drag over node
    private static JNodeView<?> _dragOver;

    // The SnapPart being dragged
    public static JNodeView<?> _dragNodeView;

    // Constants for colors
    public static Color PieceColor = Color.get("#4C67d6");
    public static Color BlockStmtColor = Color.get("#8f56e3");
    public static Color MemberDeclColor = Color.get("#f0a822");
    public static Color ClassDeclColor = MemberDeclColor; //Color.get("#27B31E");

    // Constants for fonts
    protected static final Font LABEL_FONT = new Font("Arial Bold", 13);
    protected static final Font TEXTFIELD_FONT = new Font("Arial", 12);

    // Colors
    private static final Color SELECTED_COLOR = Color.get("#FFFFFFCC");
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

    // Constant for padding on notched blocks
    private static final Insets PLAIN_PADDING = new Insets(0, 8, 0, 8);
    private static final Insets NOTCHED_PADDING = new Insets(0, 8, BlockView.NOTCH_HEIGHT, 8);

    /**
     * Constructor.
     */
    public JNodeView()
    {
        super();
        setAlign(Pos.TOP_LEFT);
        setMargin(-BlockView.NOTCH_HEIGHT, 0, -BlockView.NOTCH_HEIGHT, 0);

        // Set background
        _blockView = new BlockView();

        // Create RowView
        _rowView = createRowView();

        // Set children
        setChildren(_blockView, _rowView);
    }

    /**
     * Constructor for given JNode.
     */
    public JNodeView(JNODE aJNode)
    {
        this();
        setJNode(aJNode);
    }

    /**
     * Sets the block type.
     */
    public void setBlockType(BlockType aBlockType)
    {
        _blockView.setBlockType(aBlockType);

        // Set padding for notched
        boolean isNotched = aBlockType == BlockType.Piece || aBlockType == BlockType.Left || aBlockType == BlockType.Box;
        Insets padding = isNotched ? NOTCHED_PADDING : PLAIN_PADDING;
        setPadding(padding);

        // Set padding for box
        boolean isBox = aBlockType == BlockType.Box || aBlockType == BlockType.PlainBox;
        _paddingLeft = isBox ? BlockView.BOX_BORDER_WIDTH : 0;
    }

    /**
     * Returns the color.
     */
    public Color getColor()  { return _blockView.getFillColor(); }

    /**
     * Sets the color.
     */
    public void setColor(Color aColor)
    {
        _blockView.setFill(aColor);
        _blockView.setBorder(aColor != null ? aColor.darker() : null, 1);
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
        // Add row views
        View[] rowViews = createRowViews();
        if (rowViews != null)
            Stream.of(rowViews).forEach(this::addChildToRowView);

        // Add col views
        JNodeView<?>[] colViews = createColViews();
        if (colViews != null)
            Stream.of(colViews).forEach(this::addChildToColView);

        if (_jnode.getFile() != null)
            enableEvents(DragEvents);
    }

    /**
     * Returns the row view.
     */
    public RowView getRowView()  { return _rowView; }

    /**
     * Creates the row view.
     */
    protected RowView createRowView()
    {
        RowView rowView = new RowView();
        rowView.setPadding(NOTCHED_PADDING);
        rowView.setAlign(Pos.CENTER_LEFT);
        rowView.setMinHeight(BlockView.DEFAULT_HEIGHT);
        return rowView;
    }

    /**
     * Returns the col view.
     */
    public ColView getColView()
    {
        if (_colView != null) return _colView;
        return _colView = createColView();
    }

    /**
     * Creates the col view.
     */
    protected ColView createColView()
    {
        ColView colView = new ColView();
        colView.setMinHeight(BlockView.DEFAULT_HEIGHT);
        colView.setSpacing(-BlockView.NOTCH_HEIGHT);
        colView.setFillWidth(true);
        addChild(colView);
        return colView;
    }

    /**
     * Adds a view to row.
     */
    protected void addChildToRowView(View aView)
    {
        // Add child
        RowView rowView = getRowView();
        rowView.addChild(aView);

        // If JNodeView, reset padding
        if (aView instanceof JNodeView && !(aView instanceof JExprEditor))
            rowView.setPadding(Insets.EMPTY);
    }

    /**
     * Adds a view to column view.
     */
    protected void addChildToColView(View aView)
    {
        ColView colView = getColView();
        colView.addChild(aView);
    }

    /**
     * Creates row views.
     */
    protected View[] createRowViews()  { return null; }

    /**
     * Creates col views.
     */
    protected JNodeView<?>[] createColViews()
    {
        if (isBlock())
            return getBlockStmtViews();
        return null;
    }

    /**
     * Returns whether part is selected.
     */
    public boolean isSelected()  { return _selected; }

    /**
     * Sets whether part is selected.
     */
    public void setSelected(boolean aValue)
    {
        _selected = aValue;
        repaint(-2, -2, getWidth() + 4, getHeight() + 4);
    }

    /**
     * Sets whether part is being dragged over.
     */
    public void setUnderDrag(boolean aValue)  { _underDrag = aValue; }

    /**
     * Override.
     */
    protected double getPrefWidthImpl(double aH)
    {
        double prefW = getRowView().getBestWidth(aH);
        if (_colView != null || isBlock()) {
            double colW = getColView().getBestWidth(aH);
            prefW = Math.max(prefW, _paddingLeft + colW);
        }

        return prefW;
    }

    /**
     * Override.
     */
    protected double getPrefHeightImpl(double aW)
    {
        RowView rowView = getRowView();
        double rowH = rowView.getBestHeight(aW);
        double prefH = rowH;

        // If ColView is present, add ColView pref height minus notch height plus tail height
        if (_colView != null || isBlock()) {
            double colH = getColView().getBestHeight(aW);
            prefH += colH - BlockView.NOTCH_HEIGHT + BlockView.BOX_TAIL_HEIGHT;
        }

        return prefH;
    }

    /**
     * Override to resize rects.
     */
    protected void layoutImpl()
    {
        // Get size
        double viewW = getWidth();
        double viewH = getHeight();

        // Layout RowView
        RowView rowView = getRowView();
        double rowH = rowView.getBestHeight(viewW);
        rowView.setBounds(0, 0, viewW, rowH);

        // Layout ColView
        if (_colView != null) {
            ColView colView = getColView();
            double colY = rowH - BlockView.NOTCH_HEIGHT;
            double colW = viewW - _paddingLeft;
            double colH = viewH - colY - BlockView.BOX_TAIL_HEIGHT + BlockView.NOTCH_HEIGHT;
            colView.setBounds(_paddingLeft, colY, colW, colH);
        }

        // Layout Background, Foreground block views
        _blockView.resizeBlock(viewW, viewH);
    }

    /**
     * Override to paint selected.
     */
    @Override
    protected void paintFront(Painter aPntr)
    {
        if (isSelected()) {
            if (_underDrag) {
                aPntr.setColor(UNDER_DRAG_COLOR);
                aPntr.fill(_blockView.getPath());
            }
            aPntr.setColor(SELECTED_COLOR);
            aPntr.setStrokeWidth(2);
            aPntr.draw(_blockView.getPath());
        }
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
        JNode jNode = getJNode();
        return jNode instanceof WithBlockStmt && ((WithBlockStmt) jNode).getBlock() != null;
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
        return _jnodeViews != null ? _jnodeViews.length : 0;
    }

    /**
     * Returns the individual child.
     */
    public JNodeView<?> getJNodeView(int anIndex)
    {
        return _jnodeViews[anIndex];
    }

    /**
     * Returns the individual child.
     */
    public JNodeView<?> getJNodeViewLast()
    {
        int cc = getJNodeViewCount();
        return cc > 0 ? _jnodeViews[cc - 1] : null;
    }

    /**
     * Returns the children.
     */
    public JNodeView<?>[] getJNodeViews()
    {
        if (_jnodeViews != null) return _jnodeViews;
        return _jnodeViews = createJNodeViews();
    }

    /**
     * Creates the children.
     */
    protected JNodeView<?>[] createJNodeViews()
    {
        return getBlockStmtViews();
    }

    /**
     * Returns the children.
     */
    public JNodeView<?>[] getBlockStmtViews()
    {
        if (_blockStmtViews != null) return _blockStmtViews;
        return _blockStmtViews = createBlockStmtViews();
    }

    /**
     * Creates the children.
     */
    protected JNodeView<?>[] createBlockStmtViews()
    {
        JNode jnode = getJNode();
        JStmtBlock blockStmt = jnode instanceof WithBlockStmt ? ((WithBlockStmt) jnode).getBlock() : null;
        if (blockStmt == null)
            return new JNodeView[0];

        // Get statements and return statement views
        List<JStmt> statements = blockStmt.getStatements();
        return ListUtils.mapNonNullToArray(statements, stmt -> createView(stmt), JNodeView.class);
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
        if (anEvent.isDragDropEvent() && _dragNodeView != null) {
            if (_dragOver != null) _dragOver.setUnderDrag(false);
            _dragOver = null;
            dropNode(_dragNodeView.getJNode(), anEvent.getX(), anEvent.getY());
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
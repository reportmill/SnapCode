package snapcode.views;
import javakit.parse.JNode;
import javakit.parse.JStmt;
import javakit.parse.JStmtBlock;
import javakit.parse.WithBlockStmt;
import snap.geom.Insets;
import snap.geom.Point;
import snap.geom.Pos;
import snap.geom.Rect;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.util.ListUtils;
import snap.view.ColView;
import snap.view.RowView;
import snap.view.View;
import snap.view.ViewEvent;
import java.util.List;
import java.util.stream.Stream;

/**
 * This subclass of JNodeView renders as a block.
 */
public class JBlockView<JNODE extends JNode> extends JNodeView<JNODE> {

    // Background shape
    protected BlockView _blockView;

    // The row view
    private RowView _rowView;

    // The col view
    private ColView _colView;

    // Constants for block insets
    private double _paddingLeft;

    // The child node views
    protected JNodeView<?>[] _jnodeViews;

    // The current block view under drag (during drag)
    private static JBlockView<?> _blockViewUnderDrag;

    // Colors
    private static final Color SELECTED_COLOR = Color.get("#FFFFFFCC");
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

    // Constant for padding on notched blocks
    private static final Insets PLAIN_PADDING = new Insets(0, 8, 0, 8);
    private static final Insets NOTCHED_PADDING = new Insets(0, 8, BlockView.NOTCH_HEIGHT, 8);

    /**
     * Constructor.
     */
    public JBlockView()
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
     * Constructor.
     */
    public JBlockView(JNODE aJNode)
    {
        this();
        setJNode(aJNode);
    }

    /**
     * Sets the JNode.
     */
    @Override
    public void setJNode(JNODE aJNode)
    {
        super.setJNode(aJNode);
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
        rowView.setSpacing(5);
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
        RowView rowView = getRowView();
        rowView.addChild(aView);
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
            return getNodeViews();
        return null;
    }

    /**
     * Returns the number of children.
     */
    public int getNodeViewCount()  { return _jnodeViews != null ? _jnodeViews.length : 0; }

    /**
     * Returns the individual child.
     */
    public JNodeView<?> getNodeView(int anIndex)  { return _jnodeViews[anIndex]; }

    /**
     * Returns the children.
     */
    public JNodeView<?>[] getNodeViews()
    {
        if (_jnodeViews != null) return _jnodeViews;
        return _jnodeViews = createNodeViews();
    }

    /**
     * Creates the child block views array.
     */
    protected JNodeView<?>[] createNodeViews()
    {
        // Get block statement (method, constructor, conditional statement (if/for/while/do))
        JNode jnode = getJNode();
        JStmtBlock blockStmt = jnode instanceof WithBlockStmt ? ((WithBlockStmt) jnode).getBlock() : null;
        if (blockStmt == null)
            return new JNodeView[0];

        // Get statements and return statement views
        List<JStmt> statements = blockStmt.getStatements();
        return ListUtils.mapNonNullToArray(statements, stmt -> JNodeView.createNodeViewForNode(stmt), JNodeView.class);
    }

    /**
     * Returns the last child block view.
     */
    public JNodeView<?> getLastNodeView()
    {
        int nodeViewCount = getNodeViewCount();
        return nodeViewCount > 0 ? _jnodeViews[nodeViewCount - 1] : null;
    }

    /**
     * Moves the block by given offset X/Y.
     */
    public void moveBy(double offsetX, double offsetY)
    {
        // Handle normal blocks
        if (isManaged()) {
            setTransX(getTransX() + offsetX);
            setTransY(getTransY() + offsetY);
        }

        // Handle shelf blocks
        else {
            setX(getX() + offsetX);
            setY(getY() + offsetY);
        }
    }

    /**
     * Returns whether block is outside
     */
    public boolean isOutsideParent()
    {
        Point middlePoint = localToParent(getWidth(), getHeight());
        Rect parentBounds = getParent().getBoundsLocal();
        return !parentBounds.contains(middlePoint);
    }

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
    protected void paintAbove(Painter aPntr)
    {
        // If selected, paint highlighted path border
        if (isSelected()) {
            aPntr.setColor(SELECTED_COLOR);
            aPntr.setStrokeWidth(2);
            aPntr.draw(_blockView.getPath());
        }

        // If under dragged piece, paint highlight
        if (this == _blockViewUnderDrag) {
            aPntr.setColor(UNDER_DRAG_COLOR);
            aPntr.fill(_blockView.getPath());
        }
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
        anEvent.acceptDrag();
        anEvent.consume();

        // Handle DragEnter and DragOver: Set block view under drag
        if (anEvent.isDragEnter() || anEvent.isDragOver())
            setBlockViewUnderDrag(this);

        // Handle DragExit
        else if (anEvent.isDragExit()) {
            if (_blockViewUnderDrag == this)
                setBlockViewUnderDrag(null);
        }

        // Handle DragDropEvent
        else if (anEvent.isDragDropEvent()) {
            setBlockViewUnderDrag(null);
            if (_dragNodeView != null)
                dropNode(_dragNodeView.getJNode(), anEvent.getX(), anEvent.getY());
            anEvent.dropComplete();
        }
    }

    /**
     * Sets the block view under drag.
     */
    private static void setBlockViewUnderDrag(JBlockView<?> blockView)
    {
        if (blockView == _blockViewUnderDrag) return;

        // Set value with repaints
        if (_blockViewUnderDrag != null) _blockViewUnderDrag.repaint();
        _blockViewUnderDrag = blockView;
        if (_blockViewUnderDrag != null) _blockViewUnderDrag.repaint();
    }

    /**
     * Returns the BlockView of a node.
     */
    public static JBlockView<?> getBlockView(View aView)
    {
        if (aView instanceof JBlockView)
            return (JBlockView<?>) aView;
        return aView.getParent(JBlockView.class);
    }
}

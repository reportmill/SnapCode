package snapcode.views;
import javakit.parse.JNode;
import javakit.parse.JStmt;
import javakit.parse.JStmtBlock;
import javakit.parse.WithBlockStmt;
import snap.geom.Insets;
import snap.geom.Pos;
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
public class JNodeBlockView<JNODE extends JNode> extends JNodeView<JNODE> {

    // Background shape
    protected BlockView _blockView;

    // The row view
    private RowView _rowView;

    // The col view
    private ColView _colView;

    // whether under drag
    private boolean _underDrag;

    // Constants for block insets
    private double _paddingLeft;

    // The block statement views
    protected JNodeView<?>[] _blockStmtViews;

    // The current drag over node
    private static JNodeBlockView<?> _dragOver;

    // Colors
    private static final Color SELECTED_COLOR = Color.get("#FFFFFFCC");
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

    // Constant for padding on notched blocks
    private static final Insets PLAIN_PADDING = new Insets(0, 8, 0, 8);
    private static final Insets NOTCHED_PADDING = new Insets(0, 8, BlockView.NOTCH_HEIGHT, 8);

    /**
     * Constructor.
     */
    public JNodeBlockView()
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
    public JNodeBlockView(JNODE aJNode)
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
     * Creates col views.
     */
    protected JNodeView<?>[] createColViews()
    {
        if (isBlock())
            return getBlockStmtViews();
        return null;
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
        return ListUtils.mapNonNullToArray(statements, stmt -> createNodeViewForNode(stmt), JNodeView.class);
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
            if (_dragOver != null)
                _dragOver.setUnderDrag(false);
            setUnderDrag(true);
            _dragOver = this;
        }
        if (anEvent.isDragExit()) {
            setUnderDrag(false);
            if (_dragOver == this)
                _dragOver = null;
        }

        // Handle DragDropEvent
        if (anEvent.isDragDropEvent() && _dragNodeView != null) {
            if (_dragOver != null)
                _dragOver.setUnderDrag(false);
            _dragOver = null;
            dropNode(_dragNodeView.getJNode(), anEvent.getX(), anEvent.getY());
            anEvent.dropComplete(); //de.setDropCompleted(true);
        }
    }
}

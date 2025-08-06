package snapcode.views;
import javakit.parse.*;
import snap.geom.Insets;
import snap.geom.Point;
import snap.geom.Pos;
import snap.geom.Rect;
import snap.gfx.*;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.javatext.JavaTextAreaNodeHpr;
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

    // The child block views
    protected JBlockView<?>[] _childBlockViews;

    // The current block view under drag (during drag)
    private static JBlockView<?> _blockViewUnderDrag;

    // Constant for padding on notched blocks
    public static final Insets PLAIN_PADDING = new Insets(0, 8, 0, 8);
    public static final Insets NOTCHED_PADDING = new Insets(0, 8, BlockView.NOTCH_HEIGHT, 8);

    // Constants for selection painting
    private static final Color SELECTED_OVERLAY_COLOR = Color.get("#FFFFFF20");
    private static final Color SELECTED_OVERLAY_STROKE_COLOR = Color.get("#FFFFFFB0");
    private static final Color SELECT_GLOW_COLOR = Color.get("#039ed3").brighter().brighter();
    private static final Color SELECT_GLOW_COLOR_TOP_LEVEL = Color.get("#039ed3").brighter();
    private static final Effect SELECT_EFFECT = new ShadowEffect(6, SELECT_GLOW_COLOR, 0, 0);
    private static final Effect SELECT_EFFECT_TOP_LEVEL = new ShadowEffect(6, SELECT_GLOW_COLOR_TOP_LEVEL, 0, 0);
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

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
        rebuildChildViews();
    }

    /**
     * Rebuilds child views.
     */
    protected void rebuildChildViews()
    {
        // Rebuild row views and add to RowView
        View[] rowViews = createRowViews();
        if (rowViews != null)
            Stream.of(rowViews).forEach(this::addChildToRowView);

        // Rebuild child block views and add to colView
        JBlockView<?>[] childBlockViews = getChildBlockViews();
        if (childBlockViews.length > 0) {
            ColView colView = getColView();
            Stream.of(childBlockViews).forEach(colView::addChild);
        }

        // If part of hierarchy, enable drag events
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
     * Creates row views.
     */
    protected View[] createRowViews()  { return null; }

    /**
     * Returns whether this BlockView can have child blocks.
     */
    public boolean isParentBlock()
    {
        JNode jNode = getJNode();
        return jNode instanceof WithBlockStmt && ((WithBlockStmt) jNode).getBlock() != null;
    }

    /**
     * Returns the child block views array.
     */
    public JBlockView<?>[] getChildBlockViews()
    {
        if (_childBlockViews != null) return _childBlockViews;
        return _childBlockViews = createChildBlockViews();
    }

    /**
     * Creates the child block views array.
     */
    protected JBlockView<?>[] createChildBlockViews()
    {
        // Get block statement (method, constructor, conditional statement (if/for/while/do))
        JNode jnode = getJNode();
        JStmtBlock blockStmt = jnode instanceof WithBlockStmt ? ((WithBlockStmt) jnode).getBlock() : null;
        if (blockStmt == null)
            return new JBlockView[0];

        // Get statements and return statement views
        List<JStmt> statements = blockStmt.getStatements();
        return ListUtils.mapNonNullToArray(statements, stmt -> createBlockViewForNode(stmt), JBlockView.class);
    }

    /**
     * Returns the number of child block views.
     */
    public int getChildBlockViewCount()  { return getChildBlockViews().length; }

    /**
     * Returns the individual child block view at given index.
     */
    public JBlockView<?> getChildBlockView(int anIndex)  { return getChildBlockViews()[anIndex]; }

    /**
     * Returns the last child block view.
     */
    public JBlockView<?> getLastChildBlockView()
    {
        int nodeViewCount = getChildBlockViewCount();
        return nodeViewCount > 0 ? _childBlockViews[nodeViewCount - 1] : null;
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
        Point middlePoint = localToParent(getWidth() / 2, getHeight() / 2);
        Rect parentBounds = getParent().getBoundsLocal();
        return !parentBounds.contains(middlePoint);
    }

    /**
     * Override to set glow effect when selected.
     */
    @Override
    public void setSelected(boolean aValue)
    {
        // Do normal version
        if (aValue == isSelected()) return;
        super.setSelected(aValue);

        // Set SELECT_EFFECT (or clear)
        boolean isTopLevel = _jnode instanceof JBodyDecl || getParent() instanceof JFileView;
        Effect selEffect = isTopLevel ? SELECT_EFFECT_TOP_LEVEL : SELECT_EFFECT;
        Effect effect = aValue ? selEffect : null;
        setEffect(effect);
    }

    /**
     * Override to paint selected.
     */
    @Override
    protected void paintAbove(Painter aPntr)
    {
        // If under dragged piece, paint bright highlight overlay
        if (this == _blockViewUnderDrag) {
            aPntr.setColor(UNDER_DRAG_COLOR);
            aPntr.fill(_blockView.getPath());
        }

        // If selected, paint slight highlight overlay
        else if (isSelected()) {
            aPntr.setColor(SELECTED_OVERLAY_COLOR);
            aPntr.fill(_blockView.getPath());
            aPntr.setColor(SELECTED_OVERLAY_STROKE_COLOR);
            aPntr.setStroke(Stroke.Stroke1);
            aPntr.draw(_blockView.getPath());
        }
    }

    /**
     * Override.
     */
    protected double getPrefWidthImpl(double aH)
    {
        double prefW = getRowView().getBestWidth(aH);
        if (_colView != null || isParentBlock()) {
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
        if (_colView != null || isParentBlock()) {
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
     * Override to drop statement node.
     */
    @Override
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        // If not statement, bail
        if (!(aNode instanceof JStmt)) {
            System.out.println("JStmtView.dropNode: Can't drop " + aNode);
            return;
        }

        // If not a top level block, see if statement goes before or after
        boolean isTopLevelBlock = _jnode instanceof JBodyDecl;
        boolean isParentBlock = isParentBlock();
        JavaTextAreaNodeHpr nodeHpr = getNodeHpr();

        // If not top level, see if drop statement should go before or after
        if (!isTopLevelBlock) {

            // If drop Y less than half block base height, insert node before statement
            if (aY < BlockView.BASE_HEIGHT / 2) {
                nodeHpr.insertNode(_jnode, aNode, -1);
                return;
            }

            // If not parent block or drop Y greater than half block tail height, insert node after statement
            else if (!isParentBlock || aY >= getHeight() - BlockView.BOX_TAIL_HEIGHT / 2) {
                nodeHpr.insertNode(_jnode, aNode, 1);
                return;
            }
        }

        // If top level block but no children, insert inside statement
        if (getChildBlockViewCount() == 0)
            nodeHpr.insertNode(_jnode, aNode, 0);

        // If before first child statement, have first child dropNode, otherwise have last child dropNode
        else if (aY < getHeight() / 2) {
            JBlockView<?> firstChildBlockView = getChildBlockView(0);
            firstChildBlockView.dropNode(aNode, anX, 0);
        }

        // Otherwise add after last node
        else {
            JBlockView<?> lastChildBlockView = getLastChildBlockView();
            lastChildBlockView.dropNode(aNode, anX, lastChildBlockView.getHeight());
        }
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

    /**
     * Creates a new BlockView for given node.
     */
    public static JBlockView<?> createBlockViewForNode(JNode aNode)
    {
        JNodeView<?> nodeView = JNodeView.createNodeViewForNode(aNode);
        if (nodeView instanceof JBlockView)
            return (JBlockView<?>) nodeView;
        //System.out.println("JBlockView: Attempt to create block for illegal node type: " + aNode);
        return null;
    }
}

package snapcode.views;
import snap.geom.Insets;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.view.ChildView;
import snap.view.ColView;
import snap.view.RowView;
import snap.view.View;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeViewBase extends ChildView {

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
    public double BlockLeft = 12;

    // Colors
    private static final Color SELECTED_COLOR = Color.get("#FFFFFFCC");
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

    // Constant for padding on notched blocks
    private static final Insets PLAIN_PADDING = new Insets(0, 8, 0, 8);
    private static final Insets NOTCHED_PADDING = new Insets(0, 8, BlockView.NOTCH_HEIGHT, 8);

    /**
     * Create new background pane.
     */
    protected JNodeViewBase()
    {
        super();
        setAlign(Pos.TOP_LEFT);
        setMargin(-BlockView.NOTCH_HEIGHT, 0, -BlockView.NOTCH_HEIGHT, 0);

        // Set background
        _blockView = new BlockView(true);

        // Create RowView
        _rowView = createRowView();

        // Set children
        setChildren(_blockView, _rowView);
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
        _blockView.setColor(aColor);
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
     * Returns whether NodePane is a block.
     */
    public boolean isBlock()  { return false; }

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
            prefW = Math.max(prefW, BlockLeft + colW);
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
            double colW = viewW - BlockLeft;
            double colH = viewH - colY - BlockView.BOX_TAIL_HEIGHT + BlockView.NOTCH_HEIGHT;
            colView.setBounds(BlockLeft, colY, colW, colH);
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
}
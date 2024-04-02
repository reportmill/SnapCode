package snapcode.views;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.view.ChildView;
import snap.view.ColView;
import snap.view.RowView;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeViewBase extends ChildView {

    // Background shape
    protected BlockView _blockView;

    // whether under drag
    private boolean _underDrag;

    // The row view
    RowView _rowView;

    // The col view
    ColView _colView;

    // Whether part is selected
    boolean _selected;

    // Constants for block insets
    public double BlockTop = 4;
    public double BlockLeft = 12;
    public double BlockBottom = 5;

    // Colors
    private static final Color SELECTED_COLOR = Color.get("#FFFFFFCC");
    private static final Color UNDER_DRAG_COLOR = Color.get("#FFFFFF88");

    /**
     * Create new background pane.
     */
    protected JNodeViewBase()
    {
        // Configure
        setAlign(Pos.TOP_LEFT); //setFillWidth(true);

        // Set background
        _blockView = new BlockView(true);

        // Create RowView
        _rowView = new RowView();
        _rowView.setAlign(Pos.CENTER_LEFT);
        _rowView.setSpacing(2);
        _rowView.setMinHeight(BlockView.DEFAULT_HEIGHT);

        // Set children
        setChildren(_blockView, _rowView);
    }

    /**
     * Sets the type.
     */
    public void setType(BlockView.Type aType)
    {
        _blockView.setType(aType);
    }

    /**
     * Sets the segment position.
     */
    public void setSeg(BlockView.Seg aSeg)
    {
        _blockView.setSeg(aSeg);
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
     * Returns the horizontal box.
     */
    public RowView getRowView()  { return _rowView; }

    /**
     * Returns the vertical box.
     */
    public ColView getColView()
    {
        if (_colView != null) return _colView;
        return _colView = createColView();
    }

    /**
     * Creates the vertical box.
     */
    protected ColView createColView()
    {
        ColView colView = new ColView();
        colView.setMinHeight(35);
        colView.setFillWidth(true);
        addChild(colView);
        return colView;
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
            double colW = _colView.getBestWidth(aH);
            prefW = Math.max(prefW, BlockLeft + colW);
        }
        return prefW;
    }

    /**
     * Override.
     */
    protected double getPrefHeightImpl(double aW)
    {
        double prefH = getRowView().getBestHeight(aW);
        if (_colView != null || isBlock()) {
            double colH = _colView.getBestHeight(aW);
            prefH += BlockTop + colH + BlockBottom + _blockView.BlockTailHeight;
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
            double colW = viewW - BlockLeft;
            double colH = viewH - rowH - BlockBottom - _blockView.BlockTailHeight;
            colView.setBounds(BlockLeft, rowH + BlockTop, colW, colH);
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
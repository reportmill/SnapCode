package snapcode.views;
import snap.geom.Path2D;
import snap.geom.Pos;
import snap.geom.Rect;
import snap.geom.RoundRect;
import snap.gfx.Border;
import snap.gfx.Color;
import snap.view.ChildView;
import snap.view.ColView;
import snap.view.PathView;
import snap.view.RowView;

/**
 * A pane that draws background for puzzle pieces.
 */
public class JNodeViewBase extends ChildView {

    // The type
    Type _type = Type.Piece;

    // The segment position, if piece
    Seg _seg = Seg.Only;

    // The color
    Color _color;

    // Background shape
    PathView _bg;

    // The HBox
    RowView _hbox;

    // The VBox
    ColView _colView;

    // The foreground shape
    PathView _fg;

    // Whether part is selected
    boolean _selected;

    // Constants for type
    public enum Type {Piece, BlockStmt, MemberDecl, Plain, None}

    // Constants for segment position
    public enum Seg {Only, First, Middle, Last}

    // Constant for piece height
    double PieceHeight = 26, BlockTop = 3, BlockLeft = 12, BlockBottom = 0, BlockTailHeight = 14;

    /**
     * Create new background pane.
     */
    protected JNodeViewBase()
    {
        // Configure
        setAlign(Pos.TOP_LEFT); //setFillWidth(true);

        // Set background
        _bg = new PathView();
        _bg.setManaged(false); //_bg.setEffect(new EmbossEffect(68,112,4));

        // Create HBox
        _hbox = new RowView();
        _hbox.setAlign(Pos.CENTER_LEFT);
        _hbox.setSpacing(2);
        _hbox.setMinHeight(PieceHeight);
        //_hbox.setBorder(Color.PINK,1);

        // Create/set foreground
        _fg = new PathView();
        _fg.setManaged(false); //_fg.setStroke(null); _fg.setMouseTransparent(true);
        setChildren(_bg, _fg, _hbox);
    }

    /**
     * Returns the type.
     */
    public Type getType()  { return _type; }

    /**
     * Sets the type.
     */
    public void setType(Type aType)
    {
        _type = aType;
    }

    /**
     * Returns the segment position.
     */
    public Seg getSeg()  { return _seg; }

    /**
     * Sets the segment position.
     */
    public void setSeg(Seg aSeg)  { _seg = aSeg; }

    /**
     * Returns the color.
     */
    public Color getColor()  { return _color; }

    /**
     * Sets the color.
     */
    public void setColor(Color aColor)
    {
        _color = aColor;
        _bg.setFill(aColor);
        _bg.setBorder(aColor != null ? aColor.darker() : null, 1);
    }

    /**
     * Returns the horizontal box.
     */
    public RowView getRowView()  { return _hbox; }

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
        _fg.setBorder(aValue ? Border.createLineBorder(Color.get("#FFFFFFCC"), 2) : null);
        _fg.setVisible(aValue);
    }

    /**
     * Sets whether part is being dragged over.
     */
    public void setUnderDrag(boolean aValue)
    {
        _fg.setFill(aValue ? Color.get("#FFFFFF88") : null);
    }

    /**
     * Override.
     */
    protected double getPrefWidthImpl(double aH)
    {
        double prefW = getRowView().getBestWidth(aH);
        if (_colView != null || isBlock())
            prefW = Math.max(prefW, BlockLeft + getColView().getBestWidth(aH));
        return prefW;
    }

    /**
     * Override.
     */
    protected double getPrefHeightImpl(double aW)
    {
        double prefH = getRowView().getBestHeight(aW);
        if (_colView != null || isBlock())
            prefH += BlockTop + getColView().getBestHeight(aW) + BlockBottom + BlockTailHeight;
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

        // Layout VBox
        if (_colView != null) {
            ColView colView = getColView();
            colView.setBounds(BlockLeft, rowH + BlockTop, viewW - BlockLeft, viewH - rowH - BlockBottom - BlockTailHeight);
        }

        // Layout Background, Foreground pathviews
        resizeBG(_bg, viewW, viewH);
        resizeBG(_fg, viewW, viewH);
    }

    /**
     * Resizes background Path to size.
     */
    protected void resizeBG(PathView aPath, double aW, double aH)
    {
        aPath.setSize(aW, aH);
        switch (_type) {
            case Piece: resizeBGPiece(aPath, aW, aH); break;
            case BlockStmt: resizeBGBlock(aPath, aW, aH, true); break;
            case MemberDecl: resizeBGBlock(aPath, aW, aH, false); break;
            case Plain: resizeBGPlain(aPath, aW, aH); break;
        }
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPiece(PathView aPath, double aW, double aH)
    {
        switch (_seg) {
            case First: resizeBGPieceFirst(aPath, aW, aH); break;
            case Middle: resizeBGPieceMiddle(aPath, aW, aH); break;
            case Last: resizeBGPieceLast(aPath, aW, aH); break;
            default: resizeBGPieceOnly(aPath, aW, aH);
        }
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPieceOnly(PathView aPath, double aW, double aH)
    {
        Path2D path = aPath.getPath();
        path.clear();
        double RADIUS = 5;
        path.moveTo(RADIUS, 0);
        path.hlineTo(12);
        path.lineTo(15, 3);
        path.hlineTo(25);
        path.lineTo(28, 0); // Divit
        path.hlineTo(aW - RADIUS);
        path.arcTo(aW, 0, aW, RADIUS);
        path.vlineTo(aH - RADIUS);
        path.arcTo(aW, aH, aW - RADIUS, aH);
        path.hlineTo(28);
        path.lineTo(25, aH + 3);
        path.hlineTo(15);
        path.lineTo(12, aH); // Divit
        path.hlineTo(RADIUS);
        path.arcTo(0, aH, 0, aH - RADIUS);
        path.vlineTo(RADIUS);
        path.arcTo(0, 0, RADIUS, 0);
        path.close();
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPieceFirst(PathView aPath, double aW, double aH)
    {
        Path2D path = aPath.getPath();
        path.clear();
        double RADIUS = 5;
        path.moveTo(RADIUS, 0);
        path.hlineTo(12);
        path.lineTo(15, 3);
        path.hlineTo(25);
        path.lineTo(28, 0); // Divit
        path.hlineTo(aW); //e.add(new HLineTo(aW-r)); e.add(new ArcTo(r,r,0,aW,r,false,true));
        path.vlineTo(aH); //e.add(new VLineTo(aH-r)); e.add(new ArcTo(r,r,0,aW-r,aH,false,true));
        path.hlineTo(28);
        path.lineTo(25, aH + 3);
        path.hlineTo(15);
        path.lineTo(12, aH); // Divit
        path.hlineTo(RADIUS);
        path.arcTo(0, aH, 0, aH - RADIUS);
        path.vlineTo(RADIUS);
        path.arcTo(0, 0, RADIUS, 0);
        path.close();
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPieceMiddle(PathView aPath, double aW, double aH)
    {
        Path2D path = aPath.getPath();
        path.clear();
        path.appendShape(new Rect(0, 0, aW, aH - 3));
        aPath.setSize(aW, aH - 3);
        //p.moveTo(r,0); p.hlineTo(aW-r); p.arcTo(aW,0,aW,r); p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
        //p.hlineTo(r); p.arcTo(0,aH,0,aH-r); p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPieceLast(PathView aPath, double aW, double aH)
    {
        Path2D path = aPath.getPath();
        path.clear();
        double RADIUS = 5;
        aPath.setSize(aW, aH - 3);
        path.moveTo(0, 0);
        path.hlineTo(aW - RADIUS);
        path.arcTo(aW, 0, aW, RADIUS);
        path.vlineTo(aH - RADIUS);
        path.arcTo(aW, aH, aW - RADIUS, aH);
        path.hlineTo(0);
        path.vlineTo(0);
    }

    /**
     * Resizes background Path to size as puzzle block.
     */
    protected void resizeBGBlock(PathView aPath, double aW, double aH, boolean doOuter)
    {
        Path2D path = aPath.getPath();
        path.clear();
        double RADIUS = 5;
        double h1 = PieceHeight;
        double h2 = aH - BlockTailHeight;

        path.moveTo(RADIUS, 0);
        if (doOuter) {
            path.hlineTo(12);
            path.lineTo(15, 3);
            path.hlineTo(25);
            path.lineTo(28, 0);
        }
        path.hlineTo(aW - RADIUS);
        path.arcTo(aW, 0, aW, RADIUS);
        path.vlineTo(h1 - RADIUS);
        path.arcTo(aW, h1, aW - RADIUS, h1);
        path.hlineTo(40);
        path.lineTo(37, h1 + 3);
        path.hlineTo(27);
        path.lineTo(24, h1); // Divit
        path.hlineTo(10 + RADIUS);
        path.arcTo(10, h1, 10, h1 + RADIUS);
        path.vlineTo(h2 - RADIUS);
        path.arcTo(10, h2, 10 + RADIUS, h2);
        path.hlineTo(24);
        path.lineTo(27, h2 + 3);
        path.hlineTo(37);
        path.lineTo(40, h2); // Divit
        path.hlineTo(aW - RADIUS);
        path.arcTo(aW, h2, aW, h2 + RADIUS);
        path.vlineTo(aH - RADIUS);
        path.arcTo(aW, aH, aW - RADIUS, aH);
        if (doOuter) {
            path.hlineTo(28);
            path.lineTo(25, aH + 3);
            path.hlineTo(15);
            path.lineTo(12, aH);
        }
        path.hlineTo(RADIUS);
        path.arcTo(0, aH, 0, aH - RADIUS);
        path.vlineTo(RADIUS);
        path.arcTo(0, 0, RADIUS, 0);
        path.close();
    }

    /**
     * Resizes background Path to size as simple puzzle piece.
     */
    protected void resizeBGPlain(PathView aPath, double aW, double aH)
    {
        Path2D path = aPath.getPath();
        path.clear();
        path.appendShape(new RoundRect(1, 1, aW - 2, aH - 2, 5));
    }
}
package snapcode.views;
import snap.geom.Path2D;
import snap.geom.Rect;
import snap.geom.RoundRect;
import snap.gfx.Color;
import snap.gfx.Effect;
import snap.gfx.EmbossEffect;
import snap.view.PathView;

/**
 * A base view class to display snap blocks.
 */
public class BlockView extends PathView {

    // The block type
    private BlockType _blockType = BlockType.Piece;

    // Constant for default height
    public static final double DEFAULT_HEIGHT = 32;

    // Constant for box tail height
    public static final double BOX_TAIL_HEIGHT = 14;

    // Constant for border radius
    private static final double DEFAULT_BORDER_RADIUS = 5;

    // Constant for background effect
    private static final Effect BACKGROUND_EFFECT = new EmbossEffect(68,112,4);

    /**
     * Constructor.
     */
    public BlockView(boolean isBackground)
    {
        super();
        setManaged(false);
        if (isBackground)
            setEffect(BACKGROUND_EFFECT);
    }

    /**
     * Returns the block type.
     */
    public BlockType getBlockType()  { return _blockType; }

    /**
     * Sets the block type.
     */
    public void setBlockType(BlockType aBlockType)
    {
        _blockType = aBlockType;
    }

    /**
     * Returns the color.
     */
    public Color getColor()  { return getFillColor(); }

    /**
     * Sets the color.
     */
    public void setColor(Color aColor)
    {
        setFill(aColor);
        setBorder(aColor != null ? aColor.darker() : null, 1);
    }

    /**
     * Resets block path for size.
     */
    protected void resizeBlock(double aW, double aH)
    {
        setSize(aW, aH);

        // Reset block path
        switch (_blockType) {
            case Piece: resetBlockPathAsFullPiece(aW, aH); break;
            case Left: resetBlockPathAsLeftPiece(aW, aH); break;
            case Middle: resetBlockPathAsMiddlePiece(aW, aH); break;
            case Right: resetBlockPathAsRightPiece(aW, aH); break;
            case Plain: resetBlockPathAsRectanglePiece(aW, aH); break;
            case Box: resetBlockPathAsContainerPiece(aW, aH, true); break;
            case PlainBox: resetBlockPathAsContainerPiece(aW, aH, false); break;
        }
    }

    /**
     * Resets block path for size as one piece.
     */
    protected void resetBlockPathAsFullPiece(double aW, double aH)
    {
        Path2D path = getPath();
        path.clear();
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);
        path.hlineTo(12);
        path.lineTo(15, 3);
        path.hlineTo(25);
        path.lineTo(28, 0); // Divit
        path.hlineTo(aW - DEFAULT_BORDER_RADIUS);
        path.arcTo(aW, 0, aW, DEFAULT_BORDER_RADIUS);
        path.vlineTo(aH - DEFAULT_BORDER_RADIUS);
        path.arcTo(aW, aH, aW - DEFAULT_BORDER_RADIUS, aH);
        path.hlineTo(28);
        path.lineTo(25, aH + 3);
        path.hlineTo(15);
        path.lineTo(12, aH); // Divit
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, aH, 0, aH - DEFAULT_BORDER_RADIUS);
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as left piece.
     */
    protected void resetBlockPathAsLeftPiece(double aW, double aH)
    {
        Path2D path = getPath();
        path.clear();
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);
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
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, aH, 0, aH - DEFAULT_BORDER_RADIUS);
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as middle piece.
     */
    protected void resetBlockPathAsMiddlePiece(double aW, double aH)
    {
        Path2D path = getPath();
        path.clear();
        path.appendShape(new Rect(0, 0, aW, aH - 3));
        setSize(aW, aH - 3);
        //p.moveTo(r,0); p.hlineTo(aW-r); p.arcTo(aW,0,aW,r); p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
        //p.hlineTo(r); p.arcTo(0,aH,0,aH-r); p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
    }

    /**
     * Resets block path for size as right piece.
     */
    protected void resetBlockPathAsRightPiece(double aW, double aH)
    {
        Path2D path = getPath();
        path.clear();
        setSize(aW, aH - 3);
        path.moveTo(0, 0);
        path.hlineTo(aW - DEFAULT_BORDER_RADIUS);
        path.arcTo(aW, 0, aW, DEFAULT_BORDER_RADIUS);
        path.vlineTo(aH - DEFAULT_BORDER_RADIUS);
        path.arcTo(aW, aH, aW - DEFAULT_BORDER_RADIUS, aH);
        path.hlineTo(0);
        path.vlineTo(0);
    }

    /**
     * Resets block path for size as container piece.
     */
    protected void resetBlockPathAsContainerPiece(double blockW, double blockH, boolean doOuter)
    {
        Path2D path = getPath();
        path.clear();
        double h1 = DEFAULT_HEIGHT;
        double boxTailY = blockH - BOX_TAIL_HEIGHT;

        path.moveTo(DEFAULT_BORDER_RADIUS, 0);
        if (doOuter) {
            path.hlineTo(12);
            path.lineTo(15, 3);
            path.hlineTo(25);
            path.lineTo(28, 0);
        }
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);
        path.vlineTo(h1 - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, h1, blockW - DEFAULT_BORDER_RADIUS, h1);
        path.hlineTo(40);
        path.lineTo(37, h1 + 3);
        path.hlineTo(27);
        path.lineTo(24, h1); // Divit
        path.hlineTo(10 + DEFAULT_BORDER_RADIUS);
        path.arcTo(10, h1, 10, h1 + DEFAULT_BORDER_RADIUS);
        path.vlineTo(boxTailY - DEFAULT_BORDER_RADIUS);
        path.arcTo(10, boxTailY, 10 + DEFAULT_BORDER_RADIUS, boxTailY);
        path.hlineTo(24);
        path.lineTo(27, boxTailY + 3);
        path.hlineTo(37);
        path.lineTo(40, boxTailY); // Divit
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, boxTailY, blockW, boxTailY + DEFAULT_BORDER_RADIUS);
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);
        if (doOuter) {
            path.hlineTo(28);
            path.lineTo(25, blockH + 3);
            path.hlineTo(15);
            path.lineTo(12, blockH);
        }
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, blockH, 0, blockH - DEFAULT_BORDER_RADIUS);
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as simple rectangular piece.
     */
    protected void resetBlockPathAsRectanglePiece(double aW, double aH)
    {
        Path2D path = getPath();
        path.clear();
        path.appendShape(new RoundRect(1, 1, aW - 2, aH - 2, 5));
    }
}

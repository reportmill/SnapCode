package snapcode.views;
import snap.geom.Path2D;
import snap.geom.Rect;
import snap.geom.RoundRect;
import snap.geom.Shape;
import snap.gfx.Color;
import snap.gfx.Effect;
import snap.gfx.EmbossEffect;
import snap.view.View;

/**
 * A base view class to display snap blocks.
 */
public class BlockView extends View {

    // The block type
    private BlockType _blockType = BlockType.Piece;

    // The path shape
    private Path2D _path = new Path2D();

    // Constant for base height
    public static final double BASE_HEIGHT = 32;

    // Constant for notch width
    public static final double NOTCH_WIDTH = 10;

    // Constant for notch height
    public static final double NOTCH_HEIGHT = 3;

    // Constant for default height
    public static final double DEFAULT_HEIGHT = BASE_HEIGHT + NOTCH_HEIGHT;

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
     * Returns the path.
     */
    public Path2D getPath()  { return _path; }

    /**
     * Override to return path as bounds shape.
     */
    public Shape getBoundsShape()  { return _path; }

    /**
     * Resets block path for size.
     */
    protected void resizeBlock(double aW, double aH)
    {
        setSize(aW, aH);
        double blockH = aH - NOTCH_HEIGHT;

        // Reset block path
        switch (_blockType) {
            case Piece: resetBlockPathAsFullPiece(aW, blockH); break;
            case Left: resetBlockPathAsLeftPiece(aW, blockH); break;
            case Middle: resetBlockPathAsMiddlePiece(aW, blockH); break;
            case Right: resetBlockPathAsRightPiece(aW, blockH); break;
            case Plain: resetBlockPathAsRectanglePiece(aW, blockH); break;
            case Box: resetBlockPathAsContainerPiece(aW, blockH, true); break;
            case PlainBox: resetBlockPathAsContainerPiece(aW, blockH, false); break;
        }
    }

    /**
     * Resets block path for size as one piece.
     */
    protected void resetBlockPathAsFullPiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();

        // Draw top notch
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);
        path.hlineTo(12);
        path.lineTo(12 + NOTCH_HEIGHT, NOTCH_HEIGHT);
        path.hlineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);

        // Add top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);

        // Add bottom right corner
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);

        // Add bottom notch
        path.hlineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
        path.hlineTo(12 + NOTCH_HEIGHT);
        path.lineTo(12, blockH);

        // Add bottom left corner
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, blockH, 0, blockH - DEFAULT_BORDER_RADIUS);

        // Add top left corner
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as left piece.
     */
    protected void resetBlockPathAsLeftPiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();

        // Add top notch
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);
        path.hlineTo(12);
        path.lineTo(12 + NOTCH_HEIGHT, NOTCH_HEIGHT);
        path.hlineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);

        // Add top right corner
        path.hlineTo(blockW);
        path.vlineTo(blockH);

        // Add bottom notch
        path.hlineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
        path.hlineTo(12 + NOTCH_HEIGHT);
        path.lineTo(12, blockH);

        // Add bottom left corner
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, blockH, 0, blockH - DEFAULT_BORDER_RADIUS);

        // Add top left corner
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as middle piece.
     */
    protected void resetBlockPathAsMiddlePiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();

        // Append rounded rect
        path.appendShape(new Rect(0, 0, blockW, blockH));
        //setSize(blockW, blockH - NOTCH_HEIGHT);
    }

    /**
     * Resets block path for size as right piece.
     */
    protected void resetBlockPathAsRightPiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();
        //setSize(blockW, blockH - NOTCH_HEIGHT);

        // Append top left
        path.moveTo(0, 0);

        // Append top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);

        // Append bottom right corner
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);

        // Append bottom left corner
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
        double headH = BASE_HEIGHT;
        double tailY = blockH - BOX_TAIL_HEIGHT;

        // Append head top left
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);

        // Append head top notch
        if (doOuter) {
            path.hlineTo(12);
            path.lineTo(12 + NOTCH_HEIGHT, NOTCH_HEIGHT);
            path.hlineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH);
            path.lineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);
        }

        // Append head top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);

        // Append head bottom right corner
        path.vlineTo(headH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, headH, blockW - DEFAULT_BORDER_RADIUS, headH);

        // Append head bottom notch
        path.hlineTo(24 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(24 + NOTCH_HEIGHT + NOTCH_WIDTH, headH + NOTCH_HEIGHT);
        path.hlineTo(24 + NOTCH_HEIGHT);
        path.lineTo(24, headH);

        // Append head bottom left corner
        path.hlineTo(10 + DEFAULT_BORDER_RADIUS);
        path.arcTo(10, headH, 10, headH + DEFAULT_BORDER_RADIUS);

        // Append tail top left corner
        path.vlineTo(tailY - DEFAULT_BORDER_RADIUS);
        path.arcTo(10, tailY, 10 + DEFAULT_BORDER_RADIUS, tailY);

        // Append tail top notch
        path.hlineTo(24);
        path.lineTo(24 + NOTCH_HEIGHT, tailY + NOTCH_HEIGHT);
        path.hlineTo(24 + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(24 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, tailY);

        // Append tail top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, tailY, blockW, tailY + DEFAULT_BORDER_RADIUS);

        // Append tail bottom right corner
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);

        // Append tail bottom notch
        if (doOuter) {
            path.hlineTo(12 + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
            path.lineTo(12 + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
            path.hlineTo(12 + NOTCH_HEIGHT);
            path.lineTo(12, blockH);
        }

        // Append tail bottom left corner
        path.hlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, blockH, 0, blockH - DEFAULT_BORDER_RADIUS);

        // Append head top left corner
        path.vlineTo(DEFAULT_BORDER_RADIUS);
        path.arcTo(0, 0, DEFAULT_BORDER_RADIUS, 0);
        path.close();
    }

    /**
     * Resets block path for size as simple rectangular piece.
     */
    protected void resetBlockPathAsRectanglePiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();
        path.appendShape(new RoundRect(1, 1, blockW - 2, blockH - 2, 5));
    }
}

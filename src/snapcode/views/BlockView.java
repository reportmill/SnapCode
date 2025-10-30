package snapcode.views;
import snap.geom.Path2D;
import snap.geom.Rect;
import snap.geom.RoundRect;
import snap.geom.Shape;
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
    public static final double BASE_HEIGHT = 34;

    // Constant for notch X offset
    public static final double NOTCH_X = 12;

    // Constant for notch width
    public static final double NOTCH_WIDTH = 12;

    // Constant for notch height
    public static final double NOTCH_HEIGHT = 5;

    // Constant for default height
    public static final double DEFAULT_HEIGHT = BASE_HEIGHT + NOTCH_HEIGHT;

    // Constant for box border width
    public static final double BOX_BORDER_WIDTH = 12;

    // Constant for box tail height
    public static final double BOX_TAIL_HEIGHT = 14;

    // Constant for border radius
    private static final double DEFAULT_BORDER_RADIUS = 5;

    // Constant for background effect
    private static final Effect BACKGROUND_EFFECT = new EmbossEffect(68,112,4);

    /**
     * Constructor.
     */
    public BlockView()
    {
        super();
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
        path.hlineTo(NOTCH_X);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT, NOTCH_HEIGHT);
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);

        // Add top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);

        // Add bottom right corner
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);

        // Add bottom notch
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT);
        path.lineTo(NOTCH_X, blockH);

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
        path.hlineTo(NOTCH_X);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT, NOTCH_HEIGHT);
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);

        // Add top right corner
        path.hlineTo(blockW);
        path.vlineTo(blockH);

        // Add bottom notch
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
        path.hlineTo(NOTCH_X + NOTCH_HEIGHT);
        path.lineTo(NOTCH_X, blockH);

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
    }

    /**
     * Resets block path for size as right piece.
     */
    protected void resetBlockPathAsRightPiece(double blockW, double blockH)
    {
        Path2D path = getPath();
        path.clear();

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
    protected void resetBlockPathAsContainerPiece(double blockW, double blockH, boolean doOuterNotch)
    {
        Path2D path = getPath();
        path.clear();
        double headH = BASE_HEIGHT;
        double tailY = blockH - BOX_TAIL_HEIGHT;

        // Append head top left
        path.moveTo(DEFAULT_BORDER_RADIUS, 0);

        // Append head top notch
        if (doOuterNotch) {
            path.hlineTo(NOTCH_X);
            path.lineTo(NOTCH_X + NOTCH_HEIGHT, NOTCH_HEIGHT);
            path.hlineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH);
            path.lineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, 0);
        }

        // Append head top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, 0, blockW, DEFAULT_BORDER_RADIUS);

        // Append head bottom right corner
        path.vlineTo(headH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, headH, blockW - DEFAULT_BORDER_RADIUS, headH);

        // Append head bottom notch
        path.hlineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
        path.lineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH, headH + NOTCH_HEIGHT);
        path.hlineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT);
        path.lineTo(BOX_BORDER_WIDTH + NOTCH_X, headH);

        // Append head bottom left corner
        path.hlineTo(BOX_BORDER_WIDTH + DEFAULT_BORDER_RADIUS);
        path.arcTo(BOX_BORDER_WIDTH, headH, BOX_BORDER_WIDTH, headH + DEFAULT_BORDER_RADIUS);

        // Append tail top left corner
        path.vlineTo(tailY - DEFAULT_BORDER_RADIUS);
        path.arcTo(BOX_BORDER_WIDTH, tailY, BOX_BORDER_WIDTH + DEFAULT_BORDER_RADIUS, tailY);

        // Append tail top notch
        path.hlineTo(BOX_BORDER_WIDTH + NOTCH_X);
        path.lineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT, tailY + NOTCH_HEIGHT);
        path.hlineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH);
        path.lineTo(BOX_BORDER_WIDTH + NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH, tailY);

        // Append tail top right corner
        path.hlineTo(blockW - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, tailY, blockW, tailY + DEFAULT_BORDER_RADIUS);

        // Append tail bottom right corner
        path.vlineTo(blockH - DEFAULT_BORDER_RADIUS);
        path.arcTo(blockW, blockH, blockW - DEFAULT_BORDER_RADIUS, blockH);

        // Append tail bottom notch
        if (doOuterNotch) {
            path.hlineTo(NOTCH_X + NOTCH_HEIGHT * 2 + NOTCH_WIDTH);
            path.lineTo(NOTCH_X + NOTCH_HEIGHT + NOTCH_WIDTH, blockH + NOTCH_HEIGHT);
            path.hlineTo(NOTCH_X + NOTCH_HEIGHT);
            path.lineTo(NOTCH_X, blockH);
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

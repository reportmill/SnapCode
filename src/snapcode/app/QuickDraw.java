/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.util.ArrayUtils;
import snap.view.View;
import java.util.Arrays;

/**
 * This class supports convenient drawing methods for quick and convenient vector graphics drawing.
 */
public class QuickDraw extends View {

    // The pens
    private QuickDrawPen[]  _pens = new QuickDrawPen[1];

    // Whether this view is animating
    private QuickDrawPenAnim[]  _animPens = new QuickDrawPenAnim[0];

    /**
     * Constructor.
     */
    public QuickDraw(int aWidth, int aHeight)
    {
        setPrefSize(aWidth, aHeight);
        setBorder(Color.GRAY9, 2);
        setBorderRadius(4);
        setFill(Color.WHITE);
    }

    /**
     * Returns the main pen.
     */
    public QuickDrawPen getPen()
    {
        return getPen(0);
    }

    /**
     * Returns the pen at given index.
     */
    public QuickDrawPen getPen(int anIndex)
    {
        // Constrain number of pens to 100
        int index = Math.abs(anIndex) % 100;
        if (_pens.length <= index)
            _pens = Arrays.copyOf(_pens, index + 1);

        // Get pen - create if missing
        QuickDrawPen pen = _pens[index];
        if (pen == null)
            pen = _pens[index] = new QuickDrawPen(this);

        // Return
        return pen;
    }

    /**
     * Sets the current pen color.
     */
    public void setPenColor(Color aColor)
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.setColor(aColor);
    }

    /**
     * Moves the current path point to given point.
     */
    public void moveTo(double aX, double aY)
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.moveTo(aX, aY);
    }

    /**
     * Adds a line segment to current draw path from last path point to given point.
     */
    public void lineTo(double aX, double aY)
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.lineTo(aX, aY);
    }

    /**
     * Adds a line segment to current draw path from last path point to last moveTo point.
     */
    public void closePath()
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.closePath();
    }

    /**
     * Moves the default pen forward by given length for current Direction.
     */
    public void forward(double aLength)
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.forward(aLength);
    }

    /**
     * Sets the path drawing direction to the current direction plus given angle.
     */
    public void turn(double anAngle)
    {
        QuickDrawPen pen = getPen().getAnimPen();
        pen.turn(anAngle);
    }

    /**
     * Override to paint pen paths.
     */
    @Override
    protected void paintFront(Painter aPntr)
    {
        for (QuickDrawPen pen : _pens)
            if (pen != null)
                pen.paintPaths(aPntr);
    }

    // A run to trigger anim pen updates
    private Runnable _repaintRun = () -> updateAnimPens();

    /**
     * Updates animations.
     */
    private void updateAnimPens()
    {
        QuickDrawPenAnim[] animPens = _animPens.clone();
        for (QuickDrawPenAnim pen : animPens)
            pen.processInstructions();
    }

    /**
     * Adds an anim pen.
     */
    protected void addAnimPen(QuickDrawPenAnim aPen)
    {
        // Add pen
        _animPens = ArrayUtils.add(_animPens, aPen);

        // If first pen, start animation
        if (_animPens.length == 1)
            getEnv().runIntervals(_repaintRun, 10, 0, false, true);
    }

    /**
     * Removes an animating pen.
     */
    protected void removeAnimPen(QuickDrawPenAnim aPen)
    {
        // Remove pen
        _animPens = ArrayUtils.removeId(_animPens, aPen);

        // If no pens, stop animation
        if (_animPens.length == 0)
            getEnv().stopIntervals(_repaintRun);
    }

    /**
     * Creates and returns a default view with size 400 x 400.
     */
    public static QuickDraw createDrawView()
    {
        return createDrawView(400, 400);
    }

    /**
     * Creates and returns a view for given width and height.
     */
    public static QuickDraw createDrawView(int aWidth, int aHeight)
    {
        QuickDraw quickDraw = new QuickDraw(aWidth, aHeight);
        return quickDraw;
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.Color;
import java.util.Arrays;

/**
 * This pen subclass animates a pen.
 */
class QuickDrawPenAnim extends QuickDrawPen {

    // The real pen
    private QuickDrawPen  _realPen;

    // The array of anim instructions
    private int[]  _instructions = new int[0];

    // The array of anim instruction args
    private Object[]  _instrArgs = new Object[0];

    // The array of anim instruction times
    private int[]  _instrTimes = new int[0];

    // The current start instruction index
    private int  _instrStart;

    // The current end instruction index
    private int  _instrEnd;

    // The last anim time
    protected long  _lastAnimTime;

    // The cumulative elapsed time for current (continuing) instruction
    private long  _instrElapsedTime;

    // Constants for instructions
    private static final int Forward_Id = 1;
    private static final int Turn_Id = 2;
    private static final int Color_Id = 3;
    private static final int Width_Id = 4;

    /**
     * Constructor.
     */
    public QuickDrawPenAnim(QuickDraw aDrawView, QuickDrawPen aPen)
    {
        super(aDrawView);
        _realPen = aPen;
    }

    /**
     * Sets pen color.
     */
    public void setColor(Color aColor)
    {
        addInstruction(Color_Id, aColor, 0);
    }

    /**
     * Sets pen width.
     */
    public void setWidth(double aValue)
    {
        addInstruction(Width_Id, aValue, 0);
    }

    /**
     * Animates pen forward.
     */
    public void forward(double aLength)
    {
        int time = (int) Math.round(aLength * 1000 / 200);
        addInstruction(Forward_Id, aLength, time);
    }

    /**
     * Animates pen turn.
     */
    public void turn(double theDegrees)
    {
        addInstruction(Turn_Id, theDegrees, 0);
    }

    /**
     * Sets whether pen is animating.
     */
    protected void setAnimating(boolean aValue)
    {
        if (aValue == isAnimating()) return;
        super.setAnimating(aValue);

        // Handle Animating: Add to DrawView.AnimPens
        if (aValue) {
            _drawView.addAnimPen(this);
            _lastAnimTime = System.currentTimeMillis();
        }

        // Handle Animating false: Remove from DrawView.AnimPens
        else _drawView.removeAnimPen(this);
    }

    /**
     * Adds an instruction.
     */
    private void addInstruction(int anId, Object theArgs, int aTime)
    {
        // Extend Instructions/Args arrays if needed
        if (_instrEnd + 1 >= _instructions.length) {
            _instructions = Arrays.copyOf(_instructions, Math.max(_instructions.length * 2, 16));
            _instrArgs = Arrays.copyOf(_instrArgs, _instructions.length);
            _instrTimes = Arrays.copyOf(_instrTimes, _instructions.length);
        }

        // Add Instruction and Args to arrays
        _instructions[_instrEnd] = anId;
        _instrArgs[_instrEnd] = theArgs;
        _instrTimes[_instrEnd] = aTime;
        _instrEnd++;

        // Turn on
        setAnimating(true);
    }

    /**
     * Process instruction.
     */
    protected void processInstructions()
    {
        // Get CurrentTime, ElapsedTime
        long currTime = System.currentTimeMillis();
        long elapsedTime = currTime - _lastAnimTime;

        // Disable AutoAnimate
        boolean autoAnim = _realPen.isAutoAnimate();
        _realPen.setAutoAnimate(false);

        // Process available instructions within ElapsedTime
        int procTime = 0;
        while (_instrStart < _instrEnd && procTime < elapsedTime) {
            int instrTime = processInstruction(elapsedTime);
            procTime += instrTime;
        }

        // Restore AutoAnimate and update LastTime
        _realPen.setAutoAnimate(autoAnim);
        _lastAnimTime = currTime;

        // If done, stop animating
        int instrCount = _instrEnd - _instrStart;
        if (instrCount == 0) {
            _instrStart = _instrEnd = 0;
            setAnimating(false);
        }
    }

    /**
     * Process next instruction.
     */
    private int processInstruction(long elapsedTime)
    {
        // Get next instruction Id, Args and Time
        int instrId = _instructions[_instrStart];
        Object args = _instrArgs[_instrStart];
        int instrTime = _instrTimes[_instrStart];

        // Handle instruction id
        switch (instrId) {

            // Handle Color
            case Color_Id:
                _realPen.setColor((Color) args);
                _instrStart++;
                break;

            // Handle Width
            case Width_Id:
                _realPen.setWidth((Double) args);
                _instrStart++;
                break;

            // Handle Turn
            case Turn_Id:
                _realPen.turn((Double) args);
                _instrStart++;
                break;

            // Handle Forward
            case Forward_Id:

                // If instruction is continued from previous processing, remove last path seg
                if (_instrElapsedTime > 0) {
                    PenPath penPath = _realPen.getPenPath();
                    penPath.removeLastSeg();
                    elapsedTime += _instrElapsedTime;
                }
                _instrElapsedTime = elapsedTime;

                // Get adjusted length for ElapsedTime
                double length = (Double) args;
                double timeRatio = Math.min(elapsedTime / (double) instrTime, 1);
                double adjustedLength = length * timeRatio;

                // Add new forward
                _realPen.forward(adjustedLength);

                // If completed, increment InstrStart and reset Continued
                if (timeRatio >= 1) {
                    _instrStart++;
                    _instrElapsedTime = 0;
                }
        }

        // Return
        return instrTime;
    }
}

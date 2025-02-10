/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.util.ArrayUtils;
import snap.view.*;

/**
 * A component to paint line numbers and markers for JavaTextPane/JavaTextArea.
 */
public class LineHeadView extends View {

    // The JavaTextPane that contains this view
    private JavaTextPane _textPane;

    // The JavaTextArea
    private JavaTextArea  _textArea;

    // Whether to show line numbers
    private boolean  _showLineNumbers = true;

    // Whether to show line markers
    private boolean  _showLineMarkers = true;

    // The list of line markers
    private LineMarker<?>[] _lineMarkers;

    // The last mouse moved position
    private double  _mx, _my;

    // Constants
    private static Color LINE_NUMBERS_COLOR = Color.GRAY8;

    // Constant for line markers width
    public static final int LINE_MARKERS_WIDTH = 12;

    /**
     * Creates a new RowHeader.
     */
    public LineHeadView(JavaTextPane aJTP)
    {
        // Set ivars
        _textPane = aJTP;
        _textArea = aJTP.getTextArea();

        // Config
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setFill(ViewTheme.get().getContentColor());

        // Set PrefSize
        setPrefSizeForText();

        // Set Padding
        Insets padding = _textArea.getPadding().clone();
        padding.left = 6;
        padding.right = 12;
        setPadding(padding);
    }

    /**
     * Returns whether to show line numbers.
     */
    public boolean isShowLineNumbers()  { return _showLineNumbers; }

    /**
     * Sets whether to show line numbers.
     */
    public void setShowLineNumbers(boolean aValue)
    {
        if (aValue == isShowLineNumbers()) return;
        _showLineNumbers = aValue;

        // Adjust PrefWidth
        setPrefWidth(getSuggestedPrefWidth());
    }

    /**
     * Returns whether to show line markers.
     */
    public boolean isShowLineMarkers()  { return _showLineMarkers; }

    /**
     * Sets whether to show line markers.
     */
    public void setShowLineMarkers(boolean aValue)
    {
        if (aValue == isShowLineMarkers()) return;
        _showLineMarkers = aValue;

        // Adjust PrefWidth
        setPrefWidth(getSuggestedPrefWidth());
    }

    /**
     * Returns the line markers.
     */
    public LineMarker<?>[] getLineMarkers()
    {
        if (_lineMarkers != null) return _lineMarkers;
        return _lineMarkers = LineMarker.getMarkersForJavaTextPane(_textPane);
    }

    /**
     * Override to reset line markers.
     */
    public void resetAll()
    {
        _lineMarkers = null;
        setPrefSizeForText();
        repaint();
    }

    /**
     * Calculates pref width based on TextArea font size and ShowLineNumbers, ShowLineMarkers.
     */
    private double getSuggestedPrefWidth()
    {
        Insets ins = getPadding();
        double prefW = ins.getWidth();

        // Add width for line numbers
        if (_showLineNumbers) {
            Font font = _textArea.getTextFont();
            int lineCount = _textArea.getLineCount();
            double colCount = Math.ceil(Math.log10(lineCount) + .0001);
            double charWidth = Math.ceil(font.charAdvance('0'));
            double colsWidth = colCount * charWidth;
            prefW += colsWidth;
        }

        // Add Width for line markers
        if (_showLineMarkers) {
            prefW += LINE_MARKERS_WIDTH;
        }

        // Return
        return prefW;
    }

    /**
     * Sets the PrefSize for current text.
     */
    private void setPrefSizeForText()
    {
        double prefW = getSuggestedPrefWidth();
        double prefH = _textArea.getPrefHeight();
        setPrefSize(prefW, prefH);
    }

    /**
     * Handle events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MouseClick
        if (anEvent.isMouseClick())
            handleMouseClick(anEvent);

        // Handle MouseMoved
        else if (anEvent.isMouseMove()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            for (LineMarker<?> marker : getLineMarkers()) {
                if (marker.contains(_mx, _my)) {
                    setCursor(Cursor.HAND);
                    return;
                }
            }
            setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Handle mouse click.
     */
    private void handleMouseClick(ViewEvent anEvent)
    {
        // Get reversed markers (so click effects top marker)
        LineMarker<?>[] lineMarkers = getLineMarkers().clone();
        ArrayUtils.reverse(lineMarkers);
        double eventX = anEvent.getX();
        double eventY = anEvent.getY();

        // If mouse hits marker, forward to marker
        for (LineMarker<?> marker : lineMarkers) {
            if (marker.contains(eventX, eventY)) {
                marker.mouseClicked(anEvent);
                return;
            }
        }

        // Add breakpoint
        TextBlock textBlock = _textArea.getTextBlock();
        TextLine textLine = textBlock.getLineForY(anEvent.getY());
        int lineIndex = textLine.getLineIndex();
        _textArea.addBreakpoint(lineIndex);
        resetAll();
    }

    /**
     * Paint line markers.
     */
    protected void paintFront(Painter aPntr)
    {
        // Get line markers and paint each
        LineMarker<?>[] lineMarkers = getLineMarkers();
        for (LineMarker<?> lineMarker : lineMarkers)
            aPntr.drawImage(lineMarker._image, lineMarker.x, lineMarker.y);

        if (isShowLineNumbers())
            paintLineNumbers(aPntr);
    }

    /**
     * Paint line numbers.
     */
    protected void paintLineNumbers(Painter aPntr)
    {
        // Get/set Font and TextColor
        Font font = _textArea.getTextFont();
        aPntr.setFont(font);
        aPntr.setColor(LINE_NUMBERS_COLOR);

        // Get current Painter.ClipBounds to restrict painted line numbers
        Rect clipRect = aPntr.getClipBounds();
        double clipY = Math.max(clipRect.y, 0);
        double clipMaxY = clipRect.getMaxY();

        // Get start line index for ClipY
        TextBlock textBlock = _textArea.getTextBlock();
        TextLine startLine = textBlock.getLineForY(clipY);
        int startLineIndex = startLine.getLineIndex();
        int lineCount = _textArea.getLineCount();
        double maxX = getWidth() - getPadding().right;

        // Iterate over lines and paint line number for each
        for (int i = startLineIndex; i < lineCount; i++) {

            // Get lineY (baseline)
            TextLine textLine = _textArea.getLine(i);
            double lineY = textLine.getTextY() + textLine.getMetrics().getAscent();

            // Get String, Width and X
            String str = String.valueOf(i + 1);
            double strW = font.getStringAdvance(str);
            double strX = maxX - strW;
            aPntr.drawString(String.valueOf(i+1), strX, lineY);

            // If below clip, just return
            if (lineY > clipMaxY)
                return;
        }
    }

    /**
     * Override to return tool tip text.
     */
    public String getToolTip(ViewEvent anEvent)
    {
        LineMarker<?>[] lineMarkers = getLineMarkers();
        LineMarker<?> lineMarker = ArrayUtils.findMatch(lineMarkers, m -> m.contains(_mx, _my));
        return lineMarker != null ? lineMarker.getToolTip() : null;
    }

    @Override
    protected void themeChanged(ViewTheme oldTheme, ViewTheme newTheme)
    {
        super.themeChanged(oldTheme, newTheme);
        setFill(ViewTheme.get().getBackFill());
    }
}
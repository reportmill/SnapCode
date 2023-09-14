/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import javakit.parse.JClassDecl;
import javakit.parse.JMemberDecl;
import snapcode.project.Breakpoint;
import snapcode.project.BuildIssue;
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
    private JavaTextPane<?>  _textPane;

    // The JavaTextArea
    private JavaTextArea  _textArea;

    // Whether to show line numbers
    private boolean  _showLineNumbers = true;

    // Whether to show line markers
    private boolean  _showLineMarkers = true;

    // The list of markers
    private LineMarker<?>[]  _markers;

    // The last mouse moved position
    private double  _mx, _my;

    // Constants
    private static Color BACKGROUND_FILL = new Color(.98);
    private static Color LINE_NUMBERS_COLOR = Color.GRAY6;

    // Constants for Markers width
    public static final int LINE_MARKERS_WIDTH = 12;

    /**
     * Creates a new RowHeader.
     */
    public LineHeadView(JavaTextPane<?> aJTP)
    {
        // Set ivars
        _textPane = aJTP;
        _textArea = aJTP.getTextArea();

        // Config
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setFill(BACKGROUND_FILL);

        // Set PrefSize
        setPrefSizeForText();

        // Set Padding
        Insets padding = _textArea.getPadding().clone();
        padding.left = padding.right = 6;
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
     * Returns the markers.
     */
    public LineMarker<?>[] getMarkers()
    {
        // If already set, just return
        if (_markers != null) return _markers;

        // Get, set, return
        LineMarker<?>[] markers = createMarkers();
        return _markers = markers;
    }

    /**
     * Returns the list of markers.
     */
    protected LineMarker<?>[] createMarkers()
    {
        // Create list
        List<LineMarker<?>> markers = new ArrayList<>();

        // Add markers for member Overrides/Implements
        JClassDecl cd = _textArea.getJFile().getClassDecl();
        if (cd != null)
            getSuperMemberMarkers(cd, markers);

        // Add markers for BuildIssues
        BuildIssue[] buildIssues = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            if (issue.getEnd() <= _textArea.length())
                markers.add(new LineMarker.BuildIssueMarker(_textPane, issue));

        // Add markers for breakpoints
        Breakpoint[] breakpoints = _textArea.getBreakpoints();
        if (breakpoints != null) {
            for (Breakpoint bp : breakpoints) {
                if (bp.getLine() < _textArea.getLineCount())
                    markers.add(new LineMarker.BreakpointMarker(_textPane, bp));
                else _textArea.removeBreakpoint(bp);
            }
        }

        // Return markers
        return markers.toArray(new LineMarker[0]);
    }

    /**
     * Loads a list of SuperMemberMarkers for a class declaration (recursing for inner classes).
     */
    private void getSuperMemberMarkers(JClassDecl aCD, List<LineMarker<?>> theMarkers)
    {
        for (JMemberDecl md : aCD.getMemberDecls()) {
            if (md.getSuperDecl() != null && md.getEndCharIndex() < _textArea.length())
                theMarkers.add(new LineMarker.SuperMemberMarker(_textPane, md));
            if (md instanceof JClassDecl)
                getSuperMemberMarkers((JClassDecl) md, theMarkers);
        }
    }

    /**
     * Override to reset markers.
     */
    public void resetAll()
    {
        _markers = null;
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
            Font font = _textArea.getFont();
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
        if (anEvent.isMouseClick()) {

            // Get reversed markers (so click effects top marker)
            LineMarker<?>[] markers = getMarkers().clone();
            ArrayUtils.reverse(markers);
            double x = anEvent.getX(), y = anEvent.getY();

            // Handle double click
            if (anEvent.getClickCount() == 2) {
                for (LineMarker<?> marker : markers) {
                    if (marker.contains(x, y) && marker instanceof LineMarker.BreakpointMarker) {
                        marker.mouseClicked(anEvent);
                        return;
                    }
                }

                TextBlock textBlock = _textArea.getTextBlock();
                TextLine line = textBlock.getLineForY(anEvent.getY());
                int lineIndex = line.getIndex();
                _textArea.addBreakpoint(lineIndex);
                resetAll();
                return;
            }

            // Handle normal click
            for (LineMarker<?> marker : markers) {
                if (marker.contains(x, y)) {
                    marker.mouseClicked(anEvent);
                    return;
                }
            }
        }

        // Handle MouseMoved
        else if (anEvent.isMouseMove()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            for (LineMarker<?> marker : getMarkers()) {
                if (marker.contains(_mx, _my)) {
                    setCursor(Cursor.HAND);
                    return;
                }
            }
            setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Paint markers.
     */
    protected void paintFront(Painter aPntr)
    {
        // Get markers and paint each
        LineMarker<?>[] markers = getMarkers();
        for (LineMarker<?> marker : markers)
            aPntr.drawImage(marker._image, marker.x, marker.y);

        if (isShowLineNumbers())
            paintLineNumbers(aPntr);
    }

    /**
     * Paint line numbers.
     */
    protected void paintLineNumbers(Painter aPntr)
    {
        // Get/set Font and TextColor
        Font font = _textArea.getDefaultStyle().getFont();
        aPntr.setFont(font);
        aPntr.setColor(LINE_NUMBERS_COLOR);

        // Get current Painter.ClipBounds to restrict painted line numbers
        Rect clipRect = aPntr.getClipBounds();
        double clipY = Math.max(clipRect.y, 0);
        double clipMaxY = clipRect.getMaxY();

        // Get start line index for ClipY
        TextBlock textBlock = _textArea.getTextBlock();
        TextLine startLine = textBlock.getLineForY(clipY);
        int startLineIndex = startLine.getIndex();
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
        LineMarker<?>[] markers = getMarkers();
        LineMarker<?> marker = ArrayUtils.findMatch(markers, m -> m.contains(_mx, _my));
        return marker != null ? marker.getToolTip() : null;
    }
}
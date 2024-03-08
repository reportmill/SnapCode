/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import snapcode.project.BuildIssue;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.view.*;

/**
 * A view to show locations of Errors, warnings, selected symbols, etc.
 */
public class LineFootView extends View {

    // The JavaTextPane holding this Overview
    private JavaTextPane _textPane;

    // The JavaTextArea
    private JavaTextArea _textArea;

    // The list of markers
    private Marker<?>[]  _markers;

    // The last mouse point
    private double  _mx, _my;

    // Colors
    private static final Color _error = new Color(236, 175, 205), _errorBorder = new Color(248, 50, 147);
    private static final Color _warning = new Color(252, 240, 203), _warningBorder = new Color(246, 209, 95);

    /**
     * Creates a new OverviewPane.
     */
    public LineFootView(JavaTextPane aJTP)
    {
        // Set vars
        _textPane = aJTP;
        _textArea = aJTP.getTextArea();

        // Configure
        enableEvents(MouseMove, MouseRelease);
        setToolTipEnabled(true);
        setPrefWidth(14);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return _textArea;
    }

    /**
     * Sets the JavaTextArea selection.
     */
    public void setTextSel(int aStart, int anEnd)
    {
        _textArea.setSel(aStart, anEnd);
    }

    /**
     * Returns the list of markers.
     */
    public Marker<?>[] getMarkers()
    {
        // If already set, just return
        if (_markers != null) return _markers;

        // Get, set, return
        Marker<?>[] markers = createMarkers();
        return _markers = markers;
    }

    /**
     * Returns the list of markers.
     */
    protected Marker<?>[] createMarkers()
    {
        // Create list
        List<Marker<?>> markers = new ArrayList<>();

        // Add markers for TextArea.JavaSource.BuildIssues
        BuildIssue[] buildIssues = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            markers.add(new BuildIssueMarker(issue));

        // Add markers for TextArea.SelectedTokens
        TextToken[] selTokens = _textArea.getSelTokens();
        for (TextToken token : selTokens)
            markers.add(new TokenMarker(token));

        // Return markers
        return markers.toArray(new Marker<?>[0]);
    }

    /**
     * Override to reset marker nodes.
     */
    protected void resetAll()
    {
        _markers = null;
        repaint();
    }

    /**
     * Called on mouse click to select marker line.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MouseClicked
        if (anEvent.isMouseClick()) {
            for (Marker<?> marker : getMarkers()) {
                if (marker.contains(anEvent.getX(), anEvent.getY())) {
                    setTextSel(marker.getSelStart(), marker.getSelEnd());
                    return;
                }
            }

            TextBlock textBlock = _textArea.getTextBlock();
            TextLine line = textBlock.getLineForY(anEvent.getY() / getHeight() * _textArea.getHeight());
            setTextSel(line.getStartCharIndex(), line.getEndCharIndex());
        }

        // Handle MouseMoved
        if (anEvent.isMouseMove()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            for (Marker<?> marker : getMarkers()) {
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
        double textH = _textArea.getHeight();
        double markerH = Math.min(getHeight(), textH);

        aPntr.setStroke(Stroke.Stroke1);

        for (Marker<?> marker : getMarkers()) {
            marker.setY(marker._y / textH * markerH);
            aPntr.setPaint(marker.getColor());
            aPntr.fill(marker);
            aPntr.setPaint(marker.getStrokeColor());
            aPntr.draw(marker);
        }
    }

    /**
     * Override to return tool tip text.
     */
    public String getToolTip(ViewEvent anEvent)
    {
        for (Marker<?> marker : getMarkers()) {
            if (marker.contains(_mx, _my))
                return marker.getToolTip();
        }

        TextBlock textBlock = _textArea.getTextBlock();
        TextLine line = textBlock.getLineForY(_my / getHeight() * _textArea.getHeight());
        return "Line: " + (line.getLineIndex() + 1);
    }

    /**
     * The class that describes a overview marker.
     */
    public abstract static class Marker<T> extends Rect {

        /**
         * The object that is being marked.
         */
        T _target;
        double _y;

        /**
         * Creates a new marker for target.
         */
        public Marker(T aTarget)
        {
            _target = aTarget;
            setRect(2, 0, 9, 5);
        }

        /**
         * Returns the color.
         */
        public abstract Color getColor();

        /**
         * Returns the stroke color.
         */
        public abstract Color getStrokeColor();

        /**
         * Returns the selection start.
         */
        public abstract int getSelStart();

        /**
         * Returns the selection start.
         */
        public abstract int getSelEnd();

        /**
         * Returns a tooltip.
         */
        public abstract String getToolTip();
    }

    /**
     * The class that describes an overview marker.
     */
    public class BuildIssueMarker extends Marker<BuildIssue> {

        /**
         * Creates a new marker for target.
         */
        public BuildIssueMarker(BuildIssue anIssue)
        {
            super(anIssue);
            int charIndex = Math.min(anIssue.getEnd(), _textArea.length());
            TextLine line = _textArea.getLineForCharIndex(charIndex);
            _y = line.getTextY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()  { return _target.isError() ? _error : _warning; }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()  { return _target.isError() ? _errorBorder : _warningBorder; }

        /**
         * Returns the selection start.
         */
        public int getSelStart()  { return _target.getStart(); }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()  { return _target.getEnd(); }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()  { return _target.getText(); }
    }

    /**
     * The class that describes a overview marker.
     */
    public static class TokenMarker extends Marker<TextToken> {

        /**
         * Creates a new TokenMarker.
         */
        public TokenMarker(TextToken aToken)
        {
            super(aToken);
            TextLine line = aToken.getTextLine();
            _y = line.getTextY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()  { return _warning; }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()  { return _warningBorder; }

        /**
         * Returns the selection start.
         */
        public int getSelStart()
        {
            return _target.getTextLine().getStartCharIndex() + _target.getStartCharIndex();
        }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()
        {
            return _target.getTextLine().getStartCharIndex() + _target.getEndCharIndex();
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()  { return "Occurrence of '" + _target.getString() + "'"; }
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.*;
import snap.util.ArrayUtils;
import snapcode.project.BuildIssue;
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
    private LineFootMarker<?>[]  _markers;

    // The marker under the mouse
    private LineFootMarker<?> _hoverMarker;

    /**
     * Constructor.
     */
    public LineFootView(JavaTextPane textPane)
    {
        // Set vars
        _textPane = textPane;
        _textArea = textPane.getTextArea();

        // Configure
        setPrefWidth(14);
        setCursor(Cursor.HAND);
        enableEvents(MouseMove, MouseRelease, MouseExit);
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
    public LineFootMarker<?>[] getMarkers()
    {
        // If already set, just return
        if (_markers != null) return _markers;

        // Get, set, return
        LineFootMarker<?>[] markers = createMarkers();
        return _markers = markers;
    }

    /**
     * Returns the list of markers.
     */
    protected LineFootMarker<?>[] createMarkers()
    {
        // Create list
        List<LineFootMarker<?>> markers = new ArrayList<>();

        // Add markers for TextArea.JavaSource.BuildIssues
        BuildIssue[] buildIssues = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            markers.add(new LineFootMarker.BuildIssueMarker(_textPane, issue));

        // Add markers for TextArea.SelectedTokens
        TextToken[] selTokens = _textArea.getSelTokens();
        for (TextToken token : selTokens)
            markers.add(new LineFootMarker.TokenMarker(_textPane, token));

        // Return markers
        return markers.toArray(new LineFootMarker<?>[0]);
    }

    /**
     * Returns the marker at given point XY.
     */
    private LineFootMarker<?> getMarkerAtXY(double aX, double aY)
    {
        return ArrayUtils.findMatch(getMarkers(), marker -> marker.contains(aX, aY));
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
        if (anEvent.isMouseClick())
            handleMouseClick(anEvent);

        // Handle MouseMoved
        else if (anEvent.isMouseMove() || anEvent.isMouseExit())
            handleMouseMove(anEvent);
    }

    /**
     * Called on Mouse click event.
     */
    private void handleMouseClick(ViewEvent anEvent)
    {
        double mouseX = anEvent.getX();
        double mouseY = anEvent.getY();

        // If marker found at event point, select chars and return
        LineFootMarker<?> markerAtPoint = getMarkerAtXY(mouseX, mouseY);
        if (markerAtPoint != null) {
            setTextSel(markerAtPoint.getSelStart(), markerAtPoint.getSelEnd());
            return;
        }

        TextModel textModel = _textArea.getTextModel();
        TextLine line = textModel.getLineForY(mouseY / getHeight() * _textArea.getHeight());
        setTextSel(line.getStartCharIndex(), line.getEndCharIndex());
    }

    /**
     * Called on MouseMove event.
     */
    private void handleMouseMove(ViewEvent anEvent)
    {
        double mouseX = anEvent.getX();
        double mouseY = anEvent.getY();

        // If marker was under mouse, either return or hide
        if (_hoverMarker != null) {
            if (_hoverMarker.contains(mouseX, mouseY))
                return;
            _hoverMarker.hidePopup();
            _hoverMarker = null;
        }

        // If marker found at event point, show popup and return
        _hoverMarker = getMarkerAtXY(mouseX, mouseY);
        if (_hoverMarker != null)
            _hoverMarker.showPopup(this);
    }

    /**
     * Paint markers.
     */
    protected void paintFront(Painter aPntr)
    {
        double textH = _textArea.getHeight();
        double markerH = Math.min(getHeight(), textH);

        aPntr.setStroke(Stroke.Stroke1);

        for (LineFootMarker<?> marker : getMarkers()) {
            marker.setY(marker._y / textH * markerH);
            aPntr.setPaint(marker.getColor());
            aPntr.fill(marker);
            aPntr.setPaint(marker.getStrokeColor());
            aPntr.draw(marker);
        }
    }
}
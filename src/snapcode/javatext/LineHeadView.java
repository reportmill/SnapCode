/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.JClassDecl;
import javakit.parse.JConstrDecl;
import javakit.parse.JMethodDecl;
import javakit.resolver.JavaConstructor;
import javakit.resolver.JavaMethod;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.project.Breakpoint;
import snapcode.project.BuildIssue;
import java.util.ArrayList;
import java.util.List;

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
    private LineHeadMarker<?>[] _lineMarkers;

    // The marker under the mouse
    private LineHeadMarker<?> _hoverMarker;

    // Constants
    private static Color LINE_NUMBERS_COLOR = Color.GRAY8;
    private static Color LINE_NUMBERS_COLOR_SEL = Color.GRAY2;

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
        setFill(ViewTheme.get().getContentColor());
        setCursor(Cursor.HAND);
        enableEvents(MouseMove, MouseRelease, MouseExit);

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
    public LineHeadMarker<?>[] getLineMarkers()
    {
        if (_lineMarkers != null) return _lineMarkers;
        return _lineMarkers = getLineMarkersImpl();
    }

    /**
     * Returns the list of markers.
     */
    private LineHeadMarker<?>[] getLineMarkersImpl()
    {
        // Create list
        List<LineHeadMarker<?>> markers = new ArrayList<>();

        // Add markers for member Overrides/Implements
        JClassDecl classDecl = _textArea.getJFile().getClassDecl();
        if (classDecl != null)
            findMarkersForMethodAndConstructorOverrides(classDecl, _textPane, markers);

        // Add markers for BuildIssues
        BuildIssue[] buildIssues = _textArea.getBuildIssues();
        for (BuildIssue issue : buildIssues)
            if (issue.getEnd() <= _textArea.length())
                markers.add(new LineHeadMarker.BuildIssueMarker(_textPane, issue));

        // Add markers for breakpoints
        Breakpoint[] breakpoints = _textArea.getBreakpoints();
        if (breakpoints != null) {
            for (Breakpoint bp : breakpoints) {
                if (bp.getLine() < _textArea.getLineCount())
                    markers.add(new LineHeadMarker.BreakpointMarker(_textPane, bp));
                else _textArea.removeBreakpoint(bp);
            }
        }

        // Return markers
        return markers.toArray(new LineHeadMarker[0]);
    }

    /**
     * Loads a list of SuperMemberMarkers for a class declaration (recursing for inner classes).
     */
    private void findMarkersForMethodAndConstructorOverrides(JClassDecl aClassDecl, JavaTextPane textPane, List<LineHeadMarker<?>> theMarkers)
    {
        // Check constructors
        JConstrDecl[] constrDecls = aClassDecl.getConstructorDecls();
        for (JConstrDecl constrDecl : constrDecls) {
            JavaConstructor constr  = constrDecl.getConstructor();
            if (constr != null && constr.getSuper() != null && constrDecl.getEndCharIndex() < _textArea.length())
                theMarkers.add(new LineHeadMarker.SuperMemberMarker(textPane, constrDecl));
        }

        // Check methods
        JMethodDecl[] methodDecls = aClassDecl.getMethodDecls();
        for (JMethodDecl methodDecl : methodDecls) {
            JavaMethod method  = methodDecl.getMethod();
            if (method != null && method.getSuper() != null && methodDecl.getEndCharIndex() < _textArea.length())
                theMarkers.add(new LineHeadMarker.SuperMemberMarker(textPane, methodDecl));
        }

        // Recurse into inner classes. What about anonymous inner classes?
        JClassDecl[] innerClasses = aClassDecl.getEnclosedClassDecls();
        for (JClassDecl classDecl : innerClasses)
            findMarkersForMethodAndConstructorOverrides(classDecl, textPane, theMarkers);
    }

    /**
     * Returns the marker at given point XY.
     */
    private LineHeadMarker<?> getMarkerAtXY(double aX, double aY)
    {
        return ArrayUtils.findMatch(getLineMarkers(), marker -> marker.contains(aX, aY));
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
        else if (anEvent.isMouseMove() || anEvent.isMouseExit())
            handleMouseMove(anEvent);
    }

    /**
     * Handle mouse click.
     */
    private void handleMouseClick(ViewEvent anEvent)
    {
        // Get reversed markers (so click effects top marker)
        LineHeadMarker<?>[] lineMarkers = getLineMarkers().clone();
        ArrayUtils.reverse(lineMarkers);

        // If mouse hits marker, forward to marker
        LineHeadMarker<?> markerAtPoint = getMarkerAtXY(anEvent.getX(), anEvent.getY());
        if (markerAtPoint != null) {
            markerAtPoint.mouseClicked(anEvent);
            return;
        }

        // Add breakpoint
        TextModel textModel = _textArea.getTextModel();
        TextLine textLine = textModel.getLineForY(anEvent.getY());
        int lineIndex = textLine.getLineIndex();
        _textArea.addBreakpoint(lineIndex);
        resetAll();
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
     * Paint line markers.
     */
    protected void paintFront(Painter aPntr)
    {
        // Get line markers and paint each
        LineHeadMarker<?>[] lineMarkers = getLineMarkers();
        for (LineHeadMarker<?> lineMarker : lineMarkers)
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
        TextModel textModel = _textArea.getTextModel();
        TextLine startLine = textModel.getLineForY(clipY);
        int startLineIndex = startLine.getLineIndex();
        int lineCount = _textArea.getLineCount();
        double maxX = getWidth() - getPadding().right;
        int selStartLineIndex = _textArea.getSel().getStartLine().getLineIndex();
        int selEndLineIndex = _textArea.getSel().getEndLine().getLineIndex();

        // Iterate over lines and paint line number for each
        for (int i = startLineIndex; i < lineCount; i++) {

            // Get lineY (baseline)
            TextLine textLine = _textArea.getLine(i);
            double lineY = textLine.getTextY() + textLine.getMetrics().getAscent();

            // If entering sel text range, set line number color for selected
            if (i == selStartLineIndex)
                aPntr.setColor(LINE_NUMBERS_COLOR_SEL);

            // Get String, Width and X
            String str = String.valueOf(i + 1);
            double strW = font.getStringAdvance(str);
            double strX = maxX - strW;
            aPntr.drawString(String.valueOf(i+1), strX, lineY);

            // If leaving sel text range, reset line number color
            if (i == selEndLineIndex)
                aPntr.setColor(LINE_NUMBERS_COLOR);

            // If below clip, just return
            if (lineY > clipMaxY)
                return;
        }
    }

    @Override
    protected void themeChanged(ViewTheme oldTheme, ViewTheme newTheme)
    {
        super.themeChanged(oldTheme, newTheme);
        setFill(ViewTheme.get().getContentColor());
        LINE_NUMBERS_COLOR = newTheme == ViewTheme.getLight() ? Color.GRAY8 : Color.GRAY3;
        LINE_NUMBERS_COLOR_SEL = newTheme == ViewTheme.getLight() ? Color.GRAY4 : Color.GRAY7;
    }
}
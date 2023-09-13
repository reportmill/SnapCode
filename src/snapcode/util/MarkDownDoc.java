/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.util;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This RichText subclass can be created with MarkDown.
 */
public class MarkDownDoc extends TextDoc {

    // The header 1 style
    private TextStyle  _header1Style;

    // The header 2 style
    private TextStyle  _header2Style;

    // The content style
    private TextStyle  _contentStyle;

    // The code style
    private TextStyle  _codeStyle;

    // The MarkDownRuns
    private MarkDownRun[]  _markDownRuns;

    // The code MarkDownRuns
    private MarkDownRun[]  _codeRuns;

    // Constants
    public static final String CODE_MARKER = "```";

    // Constants for marked type
    public enum MarkType { Header1, Header2, Content, Code }

    /**
     * Constructor.
     */
    public MarkDownDoc()
    {
        super(true);
    }

    /**
     * Sets MarkDown.
     */
    public void setMarkDown(String markDown)
    {
        setDefaultStyle(getContentStyle());

        setString(markDown);

        formatHeader(2);
        formatHeader(1);
        formatCode();
    }

    /**
     * Format headers.
     */
    protected void formatHeader(int aLevel)
    {
        // Get Header marker
        String headerMarker = aLevel == 2 ? "##" : "#";

        // Get index of head
        int markerIndex = indexOf(headerMarker, 0);
        while (markerIndex >= 0) {

            // Get marker end
            int markerEndIndex = markerIndex + 1;
            for (char ch = charAt(markerEndIndex); ch == '#' || Character.isWhitespace(ch); ) {
                markerEndIndex++;
                ch = charAt(markerEndIndex);
            }

            // Remove marker
            removeChars(markerIndex, markerEndIndex);

            // Get Header line start/end
            TextLine textLine = getLineForCharIndex(markerIndex);
            int lineStartCharIndex = textLine.getStartCharIndex();
            int lineEndCharIndex = textLine.getEndCharIndex();

            // Get header style
            TextStyle headerStyle = aLevel == 1 ? getHeader1Style() : getHeader2Style();
            setStyle(headerStyle, lineStartCharIndex, lineEndCharIndex);

            // Get next marker
            markerIndex = indexOf(headerMarker, lineEndCharIndex);
        }
    }

    /**
     * Format code.
     */
    protected void formatCode()
    {
        // Replace code style
        int codeIndex = indexOf(CODE_MARKER, 0);
        while (codeIndex >= 0) {

            // Find end marker
            int codeMarkerLength = CODE_MARKER.length();
            int codeEndIndex = indexOf(CODE_MARKER, codeIndex + codeMarkerLength);
            if (codeEndIndex < 0)
                break;

            // Change font to codeStyle
            TextStyle codeStyle = getCodeStyle();
            setStyle(codeStyle, codeIndex + codeMarkerLength, codeEndIndex);

            // Remove end code marker
            TextLine endLine = getLineForCharIndex(codeEndIndex);
            removeChars(endLine.getStartCharIndex(), endLine.getEndCharIndex());

            // Remove start code marker
            TextLine startLine = getLineForCharIndex(codeIndex);
            removeChars(startLine.getStartCharIndex(), startLine.getEndCharIndex());

            // Look for next block
            codeIndex = indexOf(CODE_MARKER, codeIndex);
        }
    }

    /**
     * Returns the header 1 style.
     */
    public TextStyle getHeader1Style()
    {
        // If already set, just return
        if (_header1Style != null) return _header1Style;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font headerFont = Font.Arial16.deriveFont(24).getBold();
        Color headerColor = new Color(.5d, .5d, 1d);
        TextStyle headerStyle = textStyle.copyFor(headerFont, headerColor);

        // Set, return
        return _header1Style = headerStyle;
    }

    /**
     * Returns the header 2 style.
     */
    public TextStyle getHeader2Style()
    {
        // If already set, just return
        if (_header2Style != null) return _header2Style;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font headerFont = Font.Arial16.getBold();
        Color headerColor = Color.GRAY3;
        TextStyle headerStyle = textStyle.copyFor(headerFont, headerColor);

        // Set, return
        return _header2Style = headerStyle;
    }

    /**
     * Returns the content style.
     */
    public TextStyle getContentStyle()
    {
        // If already set, just return
        if (_contentStyle != null) return _contentStyle;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font contentFont = Font.Arial14;
        Color contentColor = Color.GRAY3;
        TextStyle contentStyle = textStyle.copyFor(contentFont, contentColor);

        // Set, return
        return _contentStyle = contentStyle;
    }

    /**
     * Returns the code style.
     */
    public TextStyle getCodeStyle()
    {
        // If already set, just return
        if (_codeStyle != null) return _codeStyle;

        // Get code font
        Font codeFont = null;
        String[] names = { "Monaco", "Consolas", "Courier" };
        for (String name : names) {
            codeFont = new Font(name, 13);
            if (codeFont.getFamily().startsWith(name))
                break;
        }

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Color codeColor = Color.GRAY3;
        TextStyle codeStyle = textStyle.copyFor(codeFont, codeColor);

        // Set, return
        return _codeStyle = codeStyle;
    }

    /**
     * Returns the MarkDown runs.
     */
    public MarkDownRun[] getMarkDownRuns()
    {
        if (_markDownRuns != null) return _markDownRuns;
        MarkDownRun[] markDownRuns = createMarkDownRuns();
        return _markDownRuns = markDownRuns;
    }

    /**
     * Returns the MarkDown runs.
     */
    protected MarkDownRun[] createMarkDownRuns()
    {
        // Get LineCount - if none, just return
        List<TextLine> lines = getLines();
        if (lines.size() == 0)
            return new MarkDownRun[0];

        // Create running list and loop run var
        List<MarkDownRun> markDownRunList = new ArrayList<>();
        MarkDownRun markDownRun = null;

        // Iterate over lines
        for (TextLine textLine : lines) {

            // Get TextLine runs
            TextRun[] runs = textLine.getRuns();

            // Iterate over runs
            for (TextRun run : runs) {

                // Get Run TextStyle/MarkType
                TextStyle runStyle = run.getStyle();
                MarkType runMarkType = getMarkTypeForStyle(runStyle);

                // Handle first MarkDownRun/TextRun special: Create new run and continue
                if (markDownRun == null) {
                    markDownRun = new MarkDownRun();
                    markDownRun.startCharIndex = run.getStartCharIndex() + textLine.getStartCharIndex();
                    markDownRun.endCharIndex = run.getEndCharIndex() + textLine.getStartCharIndex();
                    markDownRun.markType = runMarkType;
                    continue;
                }

                // If MarkType hasn't changed or isn't set, just add range to MarkDownRun and continue
                if (markDownRun.markType == runMarkType || runMarkType == null || markDownRun.markType == null) {
                    markDownRun.endCharIndex = run.getEndCharIndex() + textLine.getStartCharIndex();
                    if (markDownRun.markType == null)
                        markDownRun.markType = runMarkType;
                    continue;
                }

                // Since MarkType has changed, add MarkDownRun to list and start new one
                markDownRunList.add(markDownRun);
                markDownRun = new MarkDownRun();
                markDownRun.startCharIndex = run.getStartCharIndex() + textLine.getStartCharIndex();
                markDownRun.endCharIndex = run.getEndCharIndex() + textLine.getStartCharIndex();
                markDownRun.markType = runMarkType;
            }
        }

        // Add last run to list
        markDownRunList.add(markDownRun);

        // Return array
        return markDownRunList.toArray(new MarkDownRun[0]);
    }

    /**
     * Returns the code runs.
     */
    public MarkDownRun[] getCodeRuns()
    {
        if (_codeRuns != null) return _codeRuns;

        MarkDownRun[] markDownRuns = getMarkDownRuns();
        Stream<MarkDownRun> markDownRunsStream = Stream.of(markDownRuns);
        Stream<MarkDownRun> codeRunsStream = markDownRunsStream.filter(run -> run.markType == MarkType.Code);
        return _codeRuns = codeRunsStream.toArray(size -> new MarkDownRun[size]);
    }

    /**
     * Returns the code run at given index.
     */
    public MarkDownRun getCodeRunForCharIndex(int charIndex)
    {
        MarkDownRun[] codeRuns = getCodeRuns();
        for (MarkDownRun codeRun : codeRuns)
            if (charIndex <= codeRun.endCharIndex)
                return codeRun;

        // Return not found
        return null;
    }

    /**
     * Returns a MarkType for TextStyle.
     */
    public MarkType getMarkTypeForStyle(TextStyle aTextStyle)
    {
        if (aTextStyle == getHeader1Style()) return MarkType.Header1;
        if (aTextStyle == getHeader2Style()) return MarkType.Header2;
        if (aTextStyle == getContentStyle()) return MarkType.Content;
        if (aTextStyle == getCodeStyle()) return MarkType.Code;
        return null;
    }

    /**
     * This class represents a range of chars of a given type.
     */
    public static class MarkDownRun {

        // The start/end char indexes
        public int startCharIndex, endCharIndex;

        // The MarkType
        public MarkType markType;

    }
}

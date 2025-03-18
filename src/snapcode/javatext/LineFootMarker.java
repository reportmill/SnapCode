package snapcode.javatext;
import snap.gfx.Color;
import snap.text.TextLine;
import snap.text.TextToken;
import snapcode.project.BuildIssue;

/**
 * The class that describes a overview marker.
 */
public abstract class LineFootMarker<T> extends LineMarker<T> {

    // Y value
    protected double _y;

    // Colors
    private static final Color ERROR_COLOR = new Color(236, 175, 205);
    private static final Color ERROR_BORDER_COLOR = new Color(248, 50, 147);
    private static final Color WARNING_COLOR = new Color(252, 240, 203);
    private static final Color WARNING_BORDER_COLOR = new Color(246, 209, 95);

    /**
     * Constructor.
     */
    public LineFootMarker(JavaTextPane textPane, T aTarget)
    {
        super(textPane, aTarget);
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
     * The class that describes an overview marker.
     */
    public static class BuildIssueMarker extends LineFootMarker<BuildIssue> {

        /**
         * Constructor.
         */
        public BuildIssueMarker(JavaTextPane textPane, BuildIssue anIssue)
        {
            super(textPane, anIssue);
            int charIndex = Math.min(anIssue.getEnd(), _textArea.length());
            TextLine line = _textArea.getLineForCharIndex(charIndex);
            _y = line.getTextY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()  { return _target.isError() ? ERROR_COLOR : WARNING_COLOR; }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()  { return _target.isError() ? ERROR_BORDER_COLOR : WARNING_BORDER_COLOR; }

        /**
         * Returns the selection start.
         */
        public int getSelStart()  { return _target.getStart(); }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()  { return _target.getEnd(); }

        /**
         * Returns the marker text.
         */
        public String getMarkerText()  { return _target.getText(); }
    }

    /**
     * The class that describes a overview marker.
     */
    public static class TokenMarker extends LineFootMarker<TextToken> {

        /**
         * Constructor.
         */
        public TokenMarker(JavaTextPane textPane, TextToken aToken)
        {
            super(textPane, aToken);
            TextLine line = aToken.getTextLine();
            _y = line.getTextY() + line.getHeight() / 2;
        }

        /**
         * Returns the color.
         */
        public Color getColor()  { return WARNING_COLOR; }

        /**
         * Returns the stroke color.
         */
        public Color getStrokeColor()  { return WARNING_BORDER_COLOR; }

        /**
         * Returns the selection start.
         */
        public int getSelStart()  { return _target.getStartCharIndex(); }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()  { return _target.getEndCharIndex(); }

        /**
         * Returns the marker text.
         */
        public String getMarkerText()  { return "Occurrence of '" + _target.getString() + "'"; }
    }
}

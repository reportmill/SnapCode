package snapcode.javatext;
import javakit.parse.JMemberDecl;
import snapcode.project.Breakpoint;
import snapcode.project.BuildIssue;
import javakit.resolver.JavaExecutable;
import snap.geom.Rect;
import snap.gfx.Image;
import snap.text.TextLine;
import snap.view.ViewEvent;

/**
 * The class that describes a overview marker.
 */
public abstract class LineMarker<T> extends Rect {

    // The JavaTextPane
    protected JavaTextPane _textPane;

    // The JavaTextArea
    protected JavaTextArea _textArea;

    // The object that is being marked.
    protected T  _target;

    // The image
    protected Image  _image;

    // The marker images for Error, Warning, Breakpoint, Implements, Override
    private static Image ERROR_IMAGE = Image.getImageForClassResource(LineMarker.class, "ErrorMarker.png");
    private static Image WARNING_IMAGE = Image.getImageForClassResource(LineMarker.class, "WarningMarker.png");
    private static Image BREAKPOINT_IMAGE = Image.getImageForClassResource(LineMarker.class, "Breakpoint.png");
    private static Image IMPLEMENTS_IMAGE = Image.getImageForClassResource(LineMarker.class, "ImplementsMarker.png");
    private static Image OVERRIDE_IMAGE = Image.getImageForClassResource(LineMarker.class, "OverrideMarker.png");

    /**
     * Creates a new marker for target.
     */
    public LineMarker(JavaTextPane aJavaTextPane, T aTarget)
    {
        _textPane = aJavaTextPane;
        _textArea = aJavaTextPane.getTextArea();
        _target = aTarget;
        setRect(2, 0, LineHeadView.LINE_MARKERS_WIDTH, LineHeadView.LINE_MARKERS_WIDTH);
    }

    /**
     * Returns a tooltip.
     */
    public abstract String getToolTip();

    /**
     * Handles MouseClick.
     */
    public abstract void mouseClicked(ViewEvent anEvent);

    /**
     * A Marker for super members.
     */
    public static class SuperMemberMarker extends LineMarker<JMemberDecl> {

        // Ivars
        private JavaExecutable _superDecl;
        private boolean  _interface;

        /**
         * Creates a new marker for target.
         */
        public SuperMemberMarker(JavaTextPane aJTP, JMemberDecl aTarget)
        {
            super(aJTP, aTarget);
            _superDecl = aTarget.getSuperDecl();
            _interface = aTarget.isSuperDeclInterface();
            _image = isInterface() ? IMPLEMENTS_IMAGE : OVERRIDE_IMAGE;

            // Set Y to center image in line
            int lineIndex = aTarget.getLineIndex();
            TextLine textLine = _textArea.getLine(lineIndex);
            y = getYForTextLineAndImage(textLine, _image);
        }

        /**
         * Returns whether is interface.
         */
        public boolean isInterface()  { return _interface;  }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()
        {
            String className = _superDecl.getDeclaringClassName();
            return (isInterface() ? "Implements " : "Overrides ") + className + '.' + _target.getName();
        }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            _textPane.openSuperDeclaration(_target);
        }
    }

    /**
     * A Marker subclass for BuildIssues.
     */
    public static class BuildIssueMarker extends LineMarker<BuildIssue> {

        // Whether issue is error
        private boolean  _isError;

        /**
         * Creates a new marker for target.
         */
        public BuildIssueMarker(JavaTextPane aJTP, BuildIssue aTarget)
        {
            super(aJTP, aTarget);
            _isError = aTarget.isError();
            _image = _isError ? ERROR_IMAGE : WARNING_IMAGE;

            // Set Y to center image in line
            int charIndex = aTarget.getEnd();
            TextLine textLine = _textArea.getLineForCharIndex(charIndex);
            y = getYForTextLineAndImage(textLine, _image);
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()  { return _target.getText(); }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            _textArea.setSel(_target.getStart(), _target.getEnd());
        }
    }

    /**
     * A Marker subclass for Breakpoints.
     */
    public static class BreakpointMarker extends LineMarker<Breakpoint> {

        /**
         * Creates a BreakpointMarker.
         */
        public BreakpointMarker(JavaTextPane aJTP, Breakpoint aBP)
        {
            super(aJTP, aBP);
            _image = BREAKPOINT_IMAGE;

            // Set Y to center image in line
            TextLine textLine = _textArea.getLine(aBP.getLine());
            y = getYForTextLineAndImage(textLine, _image);
        }

        /**
         * Returns a tooltip.
         */
        public String getToolTip()  { return _target.toString(); }

        /**
         * Handles MouseClick.
         */
        public void mouseClicked(ViewEvent anEvent)
        {
            _textArea.removeBreakpoint(_target);
            _textPane._lineNumView.resetAll();
        }
    }

    /**
     * Returns the Y value to center given image in given line.
     */
    private static double getYForTextLineAndImage(TextLine textLine, Image image)
    {
        double lineY = textLine.getTextY();
        double lineH = textLine.getHeight();
        double imageH = image.getHeight();
        return Math.round(lineY + (lineH - imageH) / 2);
    }
}

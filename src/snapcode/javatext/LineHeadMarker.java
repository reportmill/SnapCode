package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import snapcode.project.Breakpoint;
import snapcode.project.BuildIssue;
import javakit.resolver.JavaExecutable;
import snap.gfx.Image;
import snap.text.TextLine;
import snap.view.ViewEvent;

/**
 * The class that describes a overview marker.
 */
public abstract class LineHeadMarker<T> extends LineMarker<T> {

    // The image
    protected Image  _image;

    // The marker images for Error, Warning, Breakpoint, Implements, Override
    private static Image ERROR_IMAGE = Image.getImageForClassResource(LineHeadMarker.class, "ErrorMarker.png");
    private static Image WARNING_IMAGE = Image.getImageForClassResource(LineHeadMarker.class, "WarningMarker.png");
    private static Image BREAKPOINT_IMAGE = Image.getImageForClassResource(LineHeadMarker.class, "Breakpoint.png");
    private static Image IMPLEMENTS_IMAGE = Image.getImageForClassResource(LineHeadMarker.class, "ImplementsMarker.png");
    private static Image OVERRIDE_IMAGE = Image.getImageForClassResource(LineHeadMarker.class, "OverrideMarker.png");

    /**
     * Constructor.
     */
    public LineHeadMarker(JavaTextPane textPane, T aTarget)
    {
        super(textPane, aTarget);
        setRect(2, 0, LineHeadView.LINE_MARKERS_WIDTH, LineHeadView.LINE_MARKERS_WIDTH);
    }

    /**
     * Handles MouseClick.
     */
    public abstract void mouseClicked(ViewEvent anEvent);

    /**
     * A Marker for super members.
     */
    public static class SuperMemberMarker extends LineHeadMarker<JExecutableDecl> {

        // Ivars
        private JavaExecutable _superMethodOrConstr;
        private boolean  _interface;

        /**
         * Constructor.
         */
        public SuperMemberMarker(JavaTextPane aJTP, JExecutableDecl aTarget)
        {
            super(aJTP, aTarget);
            JavaExecutable methodOrConstr = aTarget.getExecutable();
            _superMethodOrConstr = methodOrConstr.getSuper();
            JavaClass declaringClass = _superMethodOrConstr.getDeclaringClass();
            _interface = declaringClass.isInterface();
            _image = _interface ? IMPLEMENTS_IMAGE : OVERRIDE_IMAGE;

            // Set Y to center image in line
            int lineIndex = aTarget.getLineIndex();
            TextLine textLine = _textArea.getLine(lineIndex);
            y = getYForTextLineAndImage(textLine, _image);
        }

        /**
         * Returns the marker text.
         */
        public String getMarkerText()
        {
            String className = _superMethodOrConstr.getDeclaringClassName();
            return (_interface ? "Implements " : "Overrides ") + className + '.' + _target.getName();
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
    public static class BuildIssueMarker extends LineHeadMarker<BuildIssue> {

        // Whether issue is error
        private boolean  _isError;

        /**
         * Constructor.
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
         * Returns the marker text.
         */
        public String getMarkerText()  { return _target.getText(); }

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
    public static class BreakpointMarker extends LineHeadMarker<Breakpoint> {

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
         * Returns the marker text.
         */
        public String getMarkerText()  { return _target.toString(); }

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

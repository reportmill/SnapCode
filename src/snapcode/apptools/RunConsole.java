package snapcode.apptools;
import snapcode.app.AppPane;
import snapcode.app.ProjectTool;
import snapcode.debug.RunApp;
import snap.gfx.Color;
import snap.gfx.Font;
import snapcode.project.ProjectX;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.view.ViewEvent;
import snap.viewx.ConsoleView;
import snap.web.WebFile;

/**
 * A panel to run a process.
 */
public class RunConsole extends ProjectTool {

    // The ProcPane
    private ProcPane _procPane;

    // The output text
    private RCTextView  _textView;

    // The error color
    private static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Creates a new DebugPane.
     */
    public RunConsole(AppPane projPane)
    {
        super(projPane);
        _procPane = projPane.getProcPane();
    }

    /**
     * Returns the ProcPane.
     */
    public ProcPane getProcPane()  { return _procPane; }

    /**
     * Clears the RunConsole text.
     */
    public void clear()
    {
        if (_textView != null)
            _textView.clear();
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        // Make sure we're in app event thread
        if (!isEventThread()) {
            runLater(() -> appendOut(aStr)); return; }

        // Append string in black
        appendString(aStr, Color.BLACK);
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        // Make sure we're in app event thread
        if (!isEventThread()) {
            runLater(() -> appendErr(aStr)); return; }

        // Append string in red
        appendString(aStr, ERROR_COLOR);
    }

    /**
     * Appends text with given color.
     */
    void appendString(String aStr, Color aColor)
    {
        // Get default style modified for color
        TextStyle style = _textView.getStyleForCharIndex(_textView.length());
        if (_textView.length() > 100000) return;
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int parenStartIndex = aStr.lastIndexOf("(", i);
            int parenEndIndex = aStr.indexOf(")", i);
            if (parenStartIndex < start || parenEndIndex < 0) {
                _textView.addChars(aStr.substring(start, start = i + 6), style);
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, parenStartIndex + 1);
            _textView.addChars(prefix, style);

            // Get link text, link address, TextLink
            String linkText = aStr.substring(parenStartIndex + 1, parenEndIndex);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle lstyle = style.copyFor(textLink);
            _textView.addChars(linkText, lstyle);

            // Update start to end of link text and continue
            start = parenEndIndex;
        }

        // Add remainder normally
        _textView.addChars(aStr.substring(start), style);
    }

    /**
     * Returns a link for a StackString.
     */
    String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0)
            return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start)
            end = aPrefix.lastIndexOf('.');
        if (end < start)
            end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = SnapUtils.intValue(lineStr);
        if (line > 0)
            path += "#LineNumber=" + line;
        return path;
    }

    /**
     * Returns a source URL for path.
     */
    String getSourceURL(String aPath)
    {
        if (aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
            return "https://reportmill.com/jars/8u05/src.zip!" + aPath;
        if (aPath.startsWith("/javafx/"))
            return "https://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;
        ProjectX proj = ProjectX.getProjectForSite(getRootSite());
        if (proj == null) return aPath;
        WebFile file = proj.getProjectSet().getSourceFile(aPath);
        return file != null ? file.getURL().getString() : aPath;
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get font
        String[] names = {"Monoco", "Consolas", "Courier"};
        Font defaultFont = null;
        for (String name : names) {
            defaultFont = new Font(name, 12);
            if (defaultFont.getFamily().startsWith(name))
                break;
        }

        // Get output text
        _textView = getView("OutputText", RCTextView.class);
        _textView._runConsole = this;
        _textView.setFont(defaultFont);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        ProcPane procPane = getProcPane();
        RunApp selApp = procPane.getSelApp();
        String labelText = selApp != null ? selApp.getName() + " Console" : null;
        setViewText("NameLabel", labelText);
        setViewEnabled("TerminateButton", selApp != null && !selApp.isTerminated());
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        ProcPane procPane = getProcPane();
        RunApp selApp = procPane.getSelApp();

        // Handle ClearButton
        if (anEvent.equals("ClearButton")) {
            clear();
            selApp.clearOutput();
        }

        // Handle TerminateButton
        if (anEvent.equals("TerminateButton"))
            selApp.terminate();
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Console"; }

    /**
     * A TextView subclass to open links.
     */
    public static class RCTextView extends ConsoleView {

        RunConsole  _runConsole;

        /**
         * Override to open in browser.
         */
        protected void openLink(String aLink)
        {
            _runConsole.getBrowser().setURLString(aLink);
        }

        /**
         * Override to send to process.
         */
        protected void processEnterAction()
        {
            RunApp proc = _runConsole.getProcPane().getSelApp();
            if (proc == null) return;
            String str = getInput();
            proc.sendInput(str);
        }
    }
}
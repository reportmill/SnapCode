package snapcode.app;
import snap.props.PropObject;
import snap.view.TextArea;
import snap.web.WebFile;
import snapcode.javatext.JavaTextArea;
import snapcode.project.JavaTextDoc;
import snapcode.util.LZString;
import snapcode.webbrowser.WebPage;

/**
 * This class manages the selected file for WorkspacePane.
 */
public class SelFileTool extends PropObject {

    // The WorkspacePane
    private WorkspacePane _workspacePane;

    // The PagePane
    private PagePane _pagePane;

    // The currently selected file
    private WebFile _selFile;

    // Constants for properties
    public static final String SelFile_Prop = "SelFile";

    /**
     * Constructor.
     */
    public SelFileTool(WorkspacePane aWorkspacePane)
    {
        super();
        _workspacePane = aWorkspacePane;
        _pagePane = _workspacePane.getPagePane();
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _selFile; }

    /**
     * Sets the selected file.
     */
    protected void setSelFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == getSelFile()) return;

        // Set SelFile
        WebFile oldSelFile = _selFile;
        _selFile = aFile;

        // If page available, set in PagePane
        if (_selFile != null && _pagePane.isPageAvailableForFile(_selFile))
            _pagePane.getBrowser().setSelFile(_selFile);

        // Fire prop change
        firePropChange(SelFile_Prop, oldSelFile, _selFile);

        // Reset UI
        _workspacePane.resetLater();
        _pagePane.resetLater();
    }

    /**
     * Returns the selected page.
     */
    public WebPage getSelPage()  { return _pagePane.getSelPage(); }

    /**
     * Returns the Window.location.hash for current Workspace selected page.
     */
    public String getWindowLocationHash()
    {
        WebPage selPage = getSelPage();

        // Handle JavaPage: Return 'Java:...' or 'Jepl:...'
        if (selPage instanceof JavaPage javaPage) {
            JavaTextArea javaTextArea = javaPage.getTextArea();
            JavaTextDoc javaTextDoc = (JavaTextDoc) javaTextArea.getSourceText();
            String prefix = javaTextDoc.isJepl() ? "Jepl:" : javaTextDoc.isJMD() ? "JMD:" : "Java:";
            String javaText = javaTextDoc.getString();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return prefix + javaTextLZ;
        }

        // Handle Java markdown
        if (selPage instanceof JMDPage javaPage) {
            TextArea textArea = javaPage.getTextArea();
            String javaText = textArea.getText();
            String javaTextLZ = LZString.compressToEncodedURIComponent(javaText);
            return "JMD:" + javaTextLZ;
        }

        // Return null
        return null;
    }
}

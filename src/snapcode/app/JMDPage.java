package snapcode.app;
import snap.gfx.Font;
import snap.text.TextDoc;
import snap.view.TextArea;
import snap.view.View;
import snap.view.ViewEvent;
import snap.web.WebFile;
import snapcode.apptools.RunTool;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JMDPage extends WebPage {

    // The JMDPane
    private JMDPane _javaTextPane;

    /**
     * Constructor.
     */
    public JMDPage()
    {
        super();
        _javaTextPane = new JMDPane();
    }

    /**
     * Return the WorkspacePane.
     */
    WorkspacePane getWorkspacePane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(WorkspacePane.class);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JMDPane getTextPane()  { return _javaTextPane; }

    /**
     * Returns the TextArea.
     */
    public TextArea getTextArea()  { return _javaTextPane.getTextArea(); }

    /**
     * Creates UI panel.
     */
    protected View createUI()
    {
        return _javaTextPane.getUI();
    }

    /**
     * Init UI.
     */
    protected void initUI()
    {
        // Create TextDoc
        WebFile jmdFile = getFile();
        TextDoc javaTextDoc = new TextDoc();
        javaTextDoc.setDefaultFont(Font.getCodeFontForSize(14));
        javaTextDoc.readFromSourceURL(jmdFile.getURL());

        // Set TextArea.TextDoc and FirstFocus
        TextArea textArea = getTextArea();
        textArea.setSourceText(javaTextDoc);
        setFirstFocus(textArea);

        // Register for enter action
        addKeyActionFilter("EnterAction", "Shortcut+ENTER");
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle EnterAction (Shortcut+Enter): Run app
        if (anEvent.equals("EnterAction")) {
            WorkspacePane workspacePane = getWorkspacePane();
            WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
            RunTool runTool = workspaceTools.getRunTool();
            runTool.runAppForSelFile(false);
        }
    }
}
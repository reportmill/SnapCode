package snapcode.app;
import snap.gfx.Font;
import snap.text.TextAgent;
import snap.text.TextModel;
import snap.view.TextArea;
import snap.view.View;
import snap.view.ViewEvent;
import snap.viewx.TextPane;
import snap.web.WebFile;
import snapcode.apptools.RunTool;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JMDPage extends WebPage {

    // The TextPane
    private TextPane _textPane;

    /**
     * Constructor.
     */
    public JMDPage()
    {
        super();
        _textPane = new TextPane();
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
     * Returns the TextArea.
     */
    public TextArea getTextArea()  { return _textPane.getTextArea(); }

    /**
     * Creates UI panel.
     */
    @Override
    protected View createUI()  { return _textPane.getUI(); }

    /**
     * Init UI.
     */
    @Override
    protected void initUI()
    {
        // Get text agent for file and configure text model
        WebFile jmdFile = getFile();
        TextAgent textAgent = TextAgent.getAgentForFile(jmdFile);
        textAgent.setTextModelSupplier(() -> {
            TextModel textModel = new TextModel();
            textModel.setDefaultFont(Font.getCodeFontForSize(14));
            return textModel;
        });

        // Set text pane text file
        _textPane.setTextFile(jmdFile);

        // Set TextArea.TextModel and FirstFocus
        TextArea textArea = getTextArea();
        textArea.setPadding(5,5, 5,5);
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
            RunTool runTool = workspacePane.getRunTool();
            runTool.runAppForSelFile(false);
        }
    }
}
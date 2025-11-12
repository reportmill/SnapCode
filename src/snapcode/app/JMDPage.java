package snapcode.app;
import snap.gfx.Font;
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
     * Returns the TextArea.
     */
    public TextArea getTextArea()  { return _javaTextPane.getTextArea(); }

    /**
     * Creates UI panel.
     */
    protected View createUI()  { return _javaTextPane.getUI(); }

    /**
     * Init UI.
     */
    protected void initUI()
    {
        // Create text
        TextModel javaTextModel = new TextModel();
        javaTextModel.setDefaultFont(Font.getCodeFontForSize(14));

        // Read text from file
        WebFile jmdFile = getFile();
        javaTextModel.syncTextModelToSourceFile(jmdFile);

        // Set TextArea.TextModel and FirstFocus
        TextArea textArea = getTextArea();
        textArea.setTextModel(javaTextModel);
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

    /**
     * A TextPane subclass to edit Java Markdown.
     */
    private static class JMDPane extends TextPane {

        /**
         * Constructor.
         */
        public JMDPane()
        {
            super();
        }

        @Override
        protected TextArea createTextArea()
        {
            TextArea textArea = super.createTextArea();
            textArea.setPadding(5,5, 5,5);
            textArea.setSyncTextFont(false);
            return textArea;
        }
    }
}
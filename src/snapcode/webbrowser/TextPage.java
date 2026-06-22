/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Font;
import snap.text.TextAgent;
import snap.text.TextModel;
import snap.view.TextArea;
import snap.view.View;
import snap.view.ViewTheme;
import snap.viewx.TextPane;
import snap.web.WebFile;

/**
 * A WebPage subclass for a TextFile (or any WebFile treated as text).
 */
public class TextPage extends WebPage {

    // The text pane
    private TextPane _textPane;

    /**
     * Constructor.
     */
    public TextPage()
    {
        super();
        TextArea textArea = new TextArea(false);
        textArea.setFill(ViewTheme.get().getContentColor());
        textArea.getTextAdapter().setEditable(true);
        _textPane = new TextPane(textArea);
    }

    /**
     * Returns the TextArea.
     */
    public TextArea getTextArea()  { return _textPane.getTextArea(); }

    /**
     * Loads the text.
     */
    protected void loadTextAreaText()
    {
        // Configure text agent to supply text model with code font
        WebFile textFile = getFile();
        TextAgent textAgent = TextAgent.getAgentForFile(textFile);
        textAgent.setTextModelSupplier(() -> {
            TextModel textModel = TextModel.createDefaultTextModel();
            textModel.setDefaultFont(Font.getCodeFont());
            return textModel;
        });

        // Set text pane text file
        _textPane.setTextFile(textFile);
    }

    /**
     * Create UI panel.
     */
    @Override
    protected View createUI()  { return _textPane.getUI(); }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Configure TextArea
        setFirstFocus(getTextArea());

        // Load TextArea text
        loadTextAreaText();
    }

    /**
     * Override to detach text adapter when removed from browser.
     */
    @Override
    protected void setBrowser(WebBrowser aBrowser)
    {
        if (aBrowser == getBrowser()) return;
        super.setBrowser(aBrowser);
        if (aBrowser == null)
            getTextArea().getTextAdapter().detachAdapterFromTextModel();
    }
}
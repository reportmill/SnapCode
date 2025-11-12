/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.text.TextModel;
import snap.util.Convert;
import snap.view.TextArea;
import snap.view.View;
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
        _textPane = new TextPageTextPane();
    }

    /**
     * Returns the TextArea.
     */
    public TextArea getTextArea()
    {
        getUI();
        return _textPane.getTextArea();
    }

    /**
     * Loads the text.
     */
    protected void loadTextAreaText()
    {
        TextArea textArea = getTextArea();

        // Load text from web page file
        TextModel textModel = textArea.getTextModel();
        WebFile file = getFile();
        textModel.syncTextModelToSourceFile(file);
    }

    /**
     * Create UI panel.
     */
    protected View createUI()
    {
        return _textPane.getUI();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Configure TextArea
        TextArea textArea = getTextArea();
        textArea.getTextModel().setDefaultFont(Font.getCodeFont());
        setFirstFocus(getTextArea());

        // Load TextArea text
        loadTextAreaText();

        // Bind TextModel.TextModified to JavaPage.TextModified
        TextModel textModel = textArea.getTextModel();
        textModel.addPropChangeListener(this::handleTextModelTextModifiedChange, TextModel.TextModified_Prop);
    }

    /**
     * Called when TextArea.TextModel.TextModified property changes.
     */
    private void handleTextModelTextModifiedChange(PropChange propChange)
    {
        WebFile file = getFile();
        if (file != null) {
            WebFile.Updater updater = f -> updateFile();
            boolean isTextModified = Convert.booleanValue(propChange.getNewValue());
            file.setUpdater(isTextModified ? updater : null);
        }
    }

    /**
     * Called to update page file before save.
     */
    private void updateFile()
    {
        WebFile file = getFile();
        TextArea textArea = getTextArea();
        String textAreaText = textArea.getText();
        file.setText(textAreaText);
    }

    /**
     * A TextPane subclass.
     */
    private class TextPageTextPane extends TextPane {

        /**
         * Creates the TextArea.
         */
        protected TextArea createTextArea()  { return new TextArea(false); }

        /**
         * Override to save to page file.
         */
        @Override
        public void saveTextToFile()
        {
            try { getFile().save(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}
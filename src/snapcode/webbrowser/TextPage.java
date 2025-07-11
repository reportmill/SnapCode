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
    private TextPane _textPane = new TFTextPane();

    // The text
    private String _text;

    /**
     * Returns the text.
     */
    public String getText()
    {
        if (_text != null) return _text;
        return _text = getDefaultText();
    }

    /**
     * Sets the text.
     */
    public void setText(String aString)
    {
        _text = aString;
        if (isUISet())
            getTextArea().setText(aString);
    }

    /**
     * Returns the default text.
     */
    protected String getDefaultText()
    {
        return getFile().getText();
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
        textArea.setText(getText());
        setFirstFocus(getTextArea());

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
    private class TFTextPane extends TextPane {

        /**
         * Save file.
         */
        @Override
        protected void saveChangesImpl()
        {
            try { getFile().save(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}
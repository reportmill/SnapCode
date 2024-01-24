/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.*;
import snap.web.WebFile;
import snapcode.util.MarkDownText;

/**
 * A page for mark down.
 */
public class MarkDownPage extends WebPage {

    // The TextArea
    private TextArea _textArea;

    /**
     * Constructor.
     */
    public MarkDownPage()
    {
        super();
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        TextView textView = new TextView();
        _textArea = textView.getTextArea();
        _textArea.setPadding(20, 20, 20, 20);

        // Return
        return textView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        WebFile file = getFile();
        String fileText = file.getText();

        MarkDownText markDownText = new MarkDownText();
        markDownText.setMarkDown(fileText);
        _textArea.setSourceText(markDownText);
    }
}
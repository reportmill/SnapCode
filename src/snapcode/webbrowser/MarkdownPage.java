/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.*;
import snap.web.WebFile;
import snap.viewx.MarkdownView;

/**
 * A page for mark down.
 */
public class MarkdownPage extends WebPage {

    // The MarkdownView
    private MarkdownView _markdownView;

    /**
     * Constructor.
     */
    public MarkdownPage()
    {
        super();
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        _markdownView = new MarkdownView();
        _markdownView.setGrowWidth(true);
        _markdownView.setGrowHeight(true);

        // Wrap in scrollview
        ScrollView scrollView = new ScrollView(_markdownView);
        scrollView.setFillWidth(true);

        // Return
        return scrollView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        WebFile file = getFile();
        String fileText = file.getText();
        _markdownView.setMarkdown(fileText);
    }
}
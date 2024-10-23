/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.*;
import snap.web.WebFile;
import snap.viewx.MarkDownView;

/**
 * A page for mark down.
 */
public class MarkDownPage extends WebPage {

    // The MarkDownView
    private MarkDownView _markdownView;

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
        _markdownView = new MarkDownView();
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
        _markdownView.setMarkDown(fileText);
    }
}
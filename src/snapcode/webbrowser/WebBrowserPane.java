/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.geom.Polygon;
import snap.gfx.Color;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebURL;

/**
 * A class to manage a WebBrowser with a standard toolbar.
 */
public class WebBrowserPane extends ViewOwner {

    // The browser
    protected WebBrowser _browser;

    /**
     * Constructor.
     */
    public WebBrowserPane()
    {
        super();
        _browser = createBrowser();
    }

    /**
     * Returns the WebBrowser.
     */
    public WebBrowser getBrowser()  { return _browser; }

    /**
     * Creates the browser.
     */
    protected WebBrowser createBrowser()
    {
        WebBrowser browser = new WebBrowser();
        browser.setGrowWidth(true);
        browser.setGrowHeight(true);
        browser.setPrefSize(500, 450);
        return browser;
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _browser.getSelFile(); }

    /**
     * Sets the selected file.
     */
    public void setSelFile(WebFile aFile)  { _browser.setSelFile(aFile); }

    /**
     * Sets the browser URL.
     */
    public void setSelURL(WebURL aURL)  { _browser.setSelUrl(aURL); }

    /**
     * Returns the selected page.
     */
    public WebPage getSelPage()  { return _browser.getSelPage(); }

    /**
     * Sets the selected page.
     */
    public void setSelPage(WebPage aPage)  { _browser.setSelPage(aPage); }

    /**
     * Sets the browser URL.
     */
    public void setPageForURL(WebURL aURL, WebPage aPage)  { _browser.setPageForURL(aURL, aPage); }

    /**
     * Override to init UI.
     */
    @Override
    protected void initUI()
    {
        // Get top level ColView and add Browser
        ColView topColView = getUI(ColView.class);
        topColView.addChild(_browser, 2);

        // Register for updates
        _browser.addPropChangeListener(pc -> resetLater());

        // Set left arrow in BackButton
        ShapeView s2 = new ShapeView(new Polygon(13, 3, 5, 10, 13, 17));
        s2.setFill(Color.BLACK);
        getView("BackButton", Button.class).setGraphic(s2);

        // Set right arrow in NextButton
        ShapeView s1 = new ShapeView(new Polygon(6, 3, 14, 10, 6, 17));
        s1.setFill(Color.BLACK);
        getView("NextButton", Button.class).setGraphic(s1);

        // Add key binding to select address bar
        addKeyActionHandler("AddressTextAction", "meta O");
    }

    /**
     * Reset the UI.
     */
    @Override
    protected void resetUI()
    {
        // Get URL
        WebBrowser browser = getBrowser();
        WebURL url = browser.getLoader().getURL();
        if (url == null) url = browser.getSelUrl();

        // Set address text
        setViewText("AddressText", url != null ? url.getString() : "");

        // Set ActivityText, StatusText
        setViewText("ActivityText", browser.getActivity());
        setViewText("StatusText", browser.getStatus());

        // Update BackButton, NextButton
        setViewEnabled("BackButton", browser.getLastURL() != null);
        setViewEnabled("NextButton", browser.getNextURL() != null);

        // Update ProgressBar
        ProgressBar pb = getView("ProgressBar", ProgressBar.class);
        boolean loading = browser.isLoading();
        if (loading && !pb.isVisible()) {
            pb.setVisible(true);
            pb.setProgress(-1);
        }
        else if (!loading && pb.isVisible()) {
            pb.setProgress(0);
            pb.setVisible(false);
        }
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle BackButton, NextButton, ReloadButton
            case "BackButton" -> getBrowser().trackBack();
            case "NextButton" -> getBrowser().trackForward();
            case "ReloadButton" -> getBrowser().reloadPage();


            // Handle AddressText
            case "AddressText" -> getBrowser().setSelUrlForUrlAddress(anEvent.getStringValue());


            // Handle AddressTextAction
            case "AddressTextAction" -> {
                requestFocus("AddressText");
                getView("AddressText", TextField.class).selectAll();
            }

            // Handle InfoButton
            case "InfoButton" -> {
                String msg = "Snap Browser\nA browser for viewing Snap files, pages and applications\n" +
                        "Build Date: " + SnapUtils.getBuildInfo() + "\nJVM: " + System.getProperty("java.version");
                DialogBox dbox = new DialogBox("Browser Info");
                dbox.setMessage(msg);
                dbox.showMessageDialog(getUI());
            }
        }
    }
}
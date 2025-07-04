/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.ViewEvent;
import snap.view.ViewOwner;
import snap.web.*;
import java.util.Objects;

/**
 * A visual representation of a WebFile used in WebBrowser.
 */
public class WebPage extends ViewOwner {

    // The URL
    private WebURL  _url;

    // The response
    private WebResponse  _response;

    // The file
    private WebFile  _file;

    // The browser that owns this page
    private WebBrowser _browser;

    /**
     * Constructor.
     */
    public WebPage()
    {
        super();
    }

    /**
     * Returns the WebBrowser for this WebPage.
     */
    public WebBrowser getBrowser()  { return _browser; }

    /**
     * Returns the WebBrowser for this WebPage.
     */
    public void setBrowser(WebBrowser aBrowser)
    {
        _browser = aBrowser;
    }

    /**
     * Returns the Page URL.
     */
    public WebURL getURL()
    {
        if (_url != null) return _url;
        return _url = getURLImpl();
    }

    /**
     * Returns the Page URL.
     */
    protected WebURL getURLImpl()
    {
        // If file, return from that
        if (_file != null)
            return _file.getUrl();

        // If Response, return from that
        if (_response != null)
            return _response.getURL();

        // If subclass of WebPage, use Class file URL
        Class<? extends WebPage> pageClass = getClass();
        if (pageClass != WebPage.class)
            return WebURL.getUrl(pageClass);

        // Return null
        System.err.println("WebPage.getURL: No page URL");
        return null;
    }

    /**
     * Sets the Page URL.
     */
    public void setURL(WebURL aURL)
    {
        // If already set, just return
        if (Objects.equals(aURL, _url))
            return;

        // Set URL and Response
        _url = aURL;
        WebRequest req = new WebRequest(aURL);
        WebResponse resp = new WebResponse(req);
        resp.setCode(WebResponse.OK);
        setResponse(resp);
    }

    /**
     * Returns the WebFile for this WebPage.
     */
    public WebFile getFile()
    {
        // if already set, just return
        if (_file != null) return _file;

        // Get file from URL
        WebURL url = getURL();
        WebFile file = url != null ? url.getFile() : null;

        // Set/return
        return _file = file;
    }

    /**
     * Sets the WebFile for this WebPage.
     */
    public void setFile(WebFile aFile)
    {
        _file = aFile;
    }

    /**
     * Returns the response that generated this page.
     */
    public WebResponse getResponse()
    {
        if (_response != null) return _response;

        // Create response from URL
        WebURL url = getURL();
        if (url == null)
            return null;
        WebRequest req = new WebRequest(url);
        WebResponse resp = new WebResponse(req);
        resp.setCode(WebResponse.OK);

        // Set/return
        return _response = resp;
    }

    /**
     * Sets the response that generated this page.
     */
    public void setResponse(WebResponse aResp)
    {
        _response = aResp;
    }

    /**
     * Returns the WebSite for this WebPage.
     */
    public WebSite getSite()
    {
        WebURL url = getURL();
        return url.getSite();
    }

    /**
     * Returns the page title.
     */
    public String getTitle()
    {
        // Title is at least the file name
        WebURL pageURL = getURL();
        String filename = pageURL.getFilename();
        String title = filename;

        // Get file name and path
        String filePath = pageURL.getPath();
        filePath = filePath.substring(0, filePath.length() - filename.length() - 1);
        if (filePath.length() > 0)
            title += " - " + filePath;

        // Get Site.Name
        WebSite site = getSite();
        if (!(site instanceof FileSite)) {
            String siteName = site.getName();
            title += " - " + siteName;
        }

        // Return
        return title;
    }

    /**
     * Notification that WebPage was installed in WebBrowser.
     */
    public void notifyPageAdded(WebBrowser aBrowser)  { }

    /**
     * Notification that WebPage was removed from WebBrowser.
     */
    public void notifyPageRemoved(WebBrowser aBrowser)  { }

    /**
     * Override to show exception page.
     */
    @Override
    protected void invokeResetUI()
    {
        try { super.invokeResetUI(); }
        catch (Throwable t) {
            WebBrowser browser = getBrowser();
            browser.showException(getURL(), t);
        }
    }

    /**
     * Override to show exception page.
     */
    @Override
    protected void invokeRespondUI(ViewEvent anEvent)
    {
        try { super.invokeRespondUI(anEvent); }
        catch (Throwable t) {
            WebBrowser browser = getBrowser();
            if (browser != null)
                browser.showException(getURL(), t);
            else throw new RuntimeException(t);
        }
    }

    /**
     * Reloads a given page.
     */
    public void reload()
    {
        // Reload page
        WebURL url = getURL();
        WebFile file = url.getFile();
        if (file != null)
            file.resetAndVerify();

        WebBrowser browser = getBrowser();
        if (browser != null && browser.getSelPage() == this)
            browser.getLoader().setURL(url);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + getURL().getString();
    }
}
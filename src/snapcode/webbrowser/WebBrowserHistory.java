/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.util.ListUtils;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class to manage history for a WebBrowser.
 */
public class WebBrowserHistory {

    // The browser
    private WebBrowser _browser;

    // The current browser URL
    private WebURL _url;

    // Whether history is currently tracking browser
    private boolean _enabled = true;

    // A list of back-tracking URLs
    private List<WebURL> _lastUrls = new ArrayList<>();

    // A list of forward-tracking URLs
    private List<WebURL> _nextUrls = new ArrayList<>();

    /**
     * Constructor.
     */
    protected WebBrowserHistory(WebBrowser aBrowser)
    {
        _browser = aBrowser;
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _browser; }

    /**
     * Returns whether history is currently tracking browser.
     */
    public boolean isEnabled()  { return _enabled; }

    /**
     * Returns whether history is currently tracking browser.
     */
    public void setEnabled(boolean aFlag)
    {
        _enabled = aFlag;
    }

    /**
     * Returns the current browser URL.
     */
    public WebURL getURL()  { return _url; }

    /**
     * Sets the file.
     */
    protected void setURL(WebURL aURL)
    {
        // If URL hasn't changed, just return
        if (!isEnabled() || Objects.equals(aURL, _url)) return;

        // If first URL to be added, just set and return
        if (_url == null && _lastUrls.isEmpty() && _nextUrls.isEmpty()) {
            _url = aURL;
            return;
        }

        // Add to LastURLs, clear NextURLs and set URL
        if (!Objects.equals(_url, getLastUrl()))
            addLastUrl(_url);
        _nextUrls.clear();

        // Set URL
        _url = aURL;
    }

    /**
     * Sets the browser URL to the last URL (backtracking).
     */
    public void trackBack()
    {
        WebURL lastURL = removeLastUrl();
        if (lastURL == null)
            return;

        getBrowser().setTransition(WebBrowser.MoveUp);
        setEnabled(false);
        getBrowser().setSelUrl(lastURL);
        setEnabled(true);
        addNextUrl(_url);
        _url = lastURL;
    }

    /**
     * Sets the browser URL to the next URL (forward tracking).
     */
    public void trackForward()
    {
        WebURL nextURL = removeNextUrl();
        if (nextURL == null)
            return;

        setEnabled(false);
        getBrowser().setSelUrl(nextURL);
        setEnabled(true);
        addLastUrl(_url);
        _url = nextURL;
    }

    /**
     * Returns the last URL (backtracking).
     */
    public WebURL getLastUrl()  { return !_lastUrls.isEmpty() ? _lastUrls.get(_lastUrls.size() - 1) : null; }

    /**
     * Adds a last URL.
     */
    public void addLastUrl(WebURL aURL)
    {
        _lastUrls.add(aURL);
    }

    /**
     * Removes the last URL.
     */
    public WebURL removeLastUrl()
    {
        int index = _lastUrls.size() - 1;
        return index >= 0 ? _lastUrls.remove(index) : null;
    }

    /**
     * Returns the list of last URLs.
     */
    public List<WebURL> getLastUrls()  { return ListUtils.getReverse(_lastUrls); }

    /**
     * Returns the next URL (forward tracking).
     */
    public WebURL getNextUrl()  { return !_nextUrls.isEmpty() ? _nextUrls.get(_nextUrls.size() - 1) : null; }

    /**
     * Adds a next URL.
     */
    public void addNextUrl(WebURL aURL)
    {
        _nextUrls.add(aURL);
    }

    /**
     * Removes the next URL.
     */
    public WebURL removeNextUrl()
    {
        int index = _nextUrls.size() - 1;
        return index >= 0 ? _nextUrls.remove(index) : null;
    }

    /**
     * Returns the list of next URLs.
     */
    public List<WebURL> getNextUrls()  { return ListUtils.getReverse(_nextUrls); }

    /**
     * Removes a URL from history.
     */
    public void removeUrl(WebURL aURL)
    {
        _lastUrls.remove(aURL);
        _nextUrls.remove(aURL);
        if (getBrowser().getSelUrl() == aURL)
            getBrowser().setSelUrl(null);
        getBrowser().setPageForURL(aURL, null);
    }

    /**
     * Clears the browser history.
     */
    public void clearHistory()
    {
        getBrowser().setSelUrl(null);
        getBrowser()._allPages.clear();
        _lastUrls.clear();
        _nextUrls.clear();
    }
}
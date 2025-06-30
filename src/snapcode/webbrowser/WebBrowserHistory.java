/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.util.ListUtils;
import snap.web.WebFile;
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
    private WebURL  _url;

    // Whether history is currently tracking browser
    private boolean  _enabled = true;

    // A list of back-tracking URLs
    private List<WebURL>  _lastURLs = new ArrayList<>();

    // A list of forward-tracking URLs
    private List<WebURL>  _nextURLs = new ArrayList<>();

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
        if (_url == null && _lastURLs.isEmpty() && _nextURLs.isEmpty()) {
            _url = aURL;
            return;
        }

        // Add to LastURLs, clear NextURLs and set URL
        if (!Objects.equals(_url, getLastURL())) addLastURL(_url);
        _nextURLs.clear();

        // Set URL
        _url = aURL;
    }

    /**
     * Sets the browser URL to the last URL (backtracking).
     */
    public void trackBack()
    {
        WebURL lastURL = removeLastURL();
        if (lastURL == null) return;
        getBrowser().setTransition(WebBrowser.MoveUp);
        setEnabled(false);
        getBrowser().setSelUrl(lastURL);
        setEnabled(true);
        addNextURL(_url);
        _url = lastURL;
    }

    /**
     * Sets the browser URL to the next URL (forward tracking).
     */
    public void trackForward()
    {
        WebURL nextURL = removeNextURL();
        if (nextURL == null) return;
        setEnabled(false);
        getBrowser().setSelUrl(nextURL);
        setEnabled(true);
        addLastURL(_url);
        _url = nextURL;
    }

    /**
     * Returns the last URL (backtracking).
     */
    public WebURL getLastURL()
    {
        return !_lastURLs.isEmpty() ? _lastURLs.get(_lastURLs.size() - 1) : null;
    }

    /**
     * Adds a last URL.
     */
    public void addLastURL(WebURL aURL)
    {
        _lastURLs.add(aURL);
    }

    /**
     * Removes the last URL.
     */
    public WebURL removeLastURL()
    {
        int index = _lastURLs.size() - 1;
        return index >= 0 ? _lastURLs.remove(index) : null;
    }

    /**
     * Returns the list of last URLs.
     */
    public List<WebURL> getLastUrls()  { return ListUtils.getReverse(_lastURLs); }

    /**
     * Returns the next URL (forward tracking).
     */
    public WebURL getNextURL()
    {
        return !_nextURLs.isEmpty() ? _nextURLs.get(_nextURLs.size() - 1) : null;
    }

    /**
     * Adds a next URL.
     */
    public void addNextURL(WebURL aURL)
    {
        _nextURLs.add(aURL);
    }

    /**
     * Removes the next URL.
     */
    public WebURL removeNextURL()
    {
        int index = _nextURLs.size() - 1;
        return index >= 0 ? _nextURLs.remove(index) : null;
    }

    /**
     * Returns the list of next URLs.
     */
    public List<WebURL> getNextUrls()  { return ListUtils.getReverse(_nextURLs); }

    /**
     * Removes a URL from history.
     */
    public void removeURL(WebURL aURL)
    {
        _lastURLs.remove(aURL);
        _nextURLs.remove(aURL);
        if (getBrowser().getSelUrl() == aURL) getBrowser().setSelUrl(null);
        getBrowser().setPageForURL(aURL, null);
    }

    /**
     * Removes a file from history.
     */
    public void removeFile(WebFile aFile)
    {
        WebURL[] lastURLs = _lastURLs.toArray(new WebURL[0]);
        WebURL[] nextURLs = _nextURLs.toArray(new WebURL[0]);
        for (WebURL url : lastURLs) if (url.getFile() == aFile) _lastURLs.remove(url);
        for (WebURL url : nextURLs) if (url.getFile() == aFile) _lastURLs.remove(url);
        if (getBrowser().getSelFile() == aFile) getBrowser().setSelUrl(null);
        getBrowser().setPageForURL(aFile.getURL(), null);
    }

    /**
     * Clears the browser history.
     */
    public void clearHistory()
    {
        getBrowser().setSelUrl(null);
        getBrowser()._allPages.clear();
        _lastURLs.clear();
        _nextURLs.clear();
    }
}
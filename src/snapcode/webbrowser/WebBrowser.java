/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.util.ClassUtils;
import snap.view.View;
import snap.viewx.TransitionPane;
import snap.web.WebFile;
import snap.web.WebRequest;
import snap.web.WebResponse;
import snap.web.WebURL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A browser to show content for WebURLs.
 */
public class WebBrowser extends TransitionPane {

    // The current WebPage
    private WebPage _selPage;

    // A cache of WebPages for WebURLs
    protected Map<WebURL, WebPage> _allPages = new HashMap<>();

    // The file loader
    private WebBrowserLoader _loader = new WebBrowserLoader(this);

    // The file history
    private WebBrowserHistory _history = new WebBrowserHistory(this);

    // Whether browser is loading a page
    private boolean  _loading;

    // A string property that indicates what the browser is showing
    private String  _status;

    // A string property that indicates what the browser is doing
    private String  _activity;

    // Constants for properties
    public static final String SelPage_Prop = "SelPage";
    public static final String Loading_Prop = "Loading";
    public static final String Status_Prop = "Status";
    public static final String Activity_Prop = "Activity";

    /**
     * Constructor.
     */
    public WebBrowser()
    {
        super();
    }

    /**
     * Returns the current page URL.
     */
    public WebURL getSelUrl()
    {
        WebPage page = getSelPage();
        return page != null ? page.getURL() : null;
    }

    /**
     * Sets the browser to page for given URL.
     */
    public void setSelUrl(WebURL aURL)
    {
        if (Objects.equals(aURL, getSelUrl())) return;

        // If url is null, clear page
        if (aURL == null) {
            setSelPage(null);
            return;
        }

        // Get page - if not found, have loader load it
        WebPage page = getPageForURL(aURL); // Get page
        if (page == null) {
            getLoader().setURL(aURL);
            return;
        }

        // Update page display URL, set page and update history
        page.setURL(aURL);
        setSelPage(page);
    }

    /**
     * Sets the browser URL from given relative URL address string to the current page.
     */
    public void setSelUrlForUrlAddress(String urlAddress)
    {
        WebFile selFile = getSelFile();
        WebURL nextUrl = selFile != null ? selFile.getUrlForPath(urlAddress) : WebURL.getUrl(urlAddress);
        setSelUrl(nextUrl);
    }

    /**
     * Returns the WebFile for the current page.
     */
    public WebFile getSelFile()
    {
        WebPage selPage = getSelPage();
        return selPage != null ? selPage.getFile() : null;
    }

    /**
     * Sets the browser to page for given file.
     */
    public void setSelFile(WebFile aFile)
    {
        if (aFile == getSelFile()) return;

        // If file not verified, forward to setSelUrl()
        WebURL url = aFile != null ? aFile.getUrl() : null;
        if (aFile == null || !aFile.isVerified()) {
            setSelUrl(url);
            return;
        }

        // Get page for URL - create if missing
        WebPage page = getPageForURL(url);
        if (page == null)
            page = createPageForURL(url);

        // Set page
        setSelPage(page);
    }

    /**
     * Returns the current WebPage.
     */
    public WebPage getSelPage()  { return _selPage; }

    /**
     * Sets the current WebPage.
     */
    public void setSelPage(WebPage aPage)
    {
        // If already set, just return
        if (aPage == getSelPage()) return;

        // Notify current page of imminent removal
        if (_selPage != null)
            _selPage.notifyPageRemoved(this);

        // Set page
        _selPage = aPage;

        // Get page UI - if already set, just return
        View pageUI = _selPage != null ? _selPage.getUI() : null;
        if (pageUI == getContent())
            return;

        // Set component
        setContent(pageUI);

        // Focus default component
        if (_selPage != null) {
            _selPage.setBrowser(this);
            _selPage.notifyPageAdded(this);                 // Notify PageAdded
            if (_selPage.getFirstFocus() != null)              // Set Page Focus
                _selPage.requestFocus(_selPage.getFirstFocus());
            getHistory().setURL(_selPage.getURL());  // Update History
        }

        // Notify change
        firePropChange(SelPage_Prop, null, _selPage);

        // Flush WebSite for the heck of it?
        WebFile file = getSelFile();
        if (file != null) {
            try { file.getSite().flush(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    /**
     * Sets the response in the browser.
     */
    protected void setSelPageForResponse(WebResponse aResp)
    {
        // Create page for URL and set
        WebPage page = createPageForResponse(aResp);
        setSelPage(page);

        // If response has file, cache page
        if (aResp.getFile() != null)
            setPageForURL(page.getURL(), page);
    }

    /**
     * Returns the WebPage for given WebURL.
     */
    public WebPage getPageForURL(WebURL aURL)
    {
        WebURL baseUrl = aURL.getQueryUrl();
        return _allPages.get(baseUrl);
    }

    /**
     * Sets the WebPage for a given WebFile.
     */
    public void setPageForURL(WebURL aURL, WebPage aPage)
    {
        WebURL baseUrl = aURL.getQueryUrl();
        if (aPage != null) {
            _allPages.put(baseUrl, aPage);
            aPage.setBrowser(this);
        }
        else _allPages.remove(baseUrl);
    }

    /**
     * Creates a WebPage for given WebURL.
     */
    public WebPage createPageForURL(WebURL aURL)
    {
        // Create bogus response for URL
        WebRequest req = new WebRequest(aURL);
        WebResponse resp = new WebResponse(req);
        resp.setCode(WebResponse.OK);

        // Create page
        WebPage pageForUrl = createPageForResponse(resp);
        setPageForURL(aURL, pageForUrl);

        // Return
        return pageForUrl;
    }

    /**
     * Creates a WebPage for given WebFile.
     */
    protected WebPage createPageForResponse(WebResponse aResp)
    {
        // If UNAUTHORIZED response, create LoginPage
        WebPage page;
        if (aResp.getCode() == WebResponse.UNAUTHORIZED)
            page = new LoginPage();

        // If not OK, create FileNotFoundPage
        else if (aResp.getCode() != WebResponse.OK)
            page = new FileNotFoundPage();

        // Create WebPage in handler
        else {
            try {
                Class<? extends WebPage> cls = getPageClassForResponse(aResp);
                page = ClassUtils.newInstance(cls);
            }

            // Return Exception WebPage
            catch (Throwable t) {
                aResp.setException(t);
                return createExceptionPage(aResp);
            }
        }

        // Set response and browser and return
        page.setResponse(aResp);
        page.setBrowser(this);
        return page;
    }

    /**
     * Returns the specified WebPage subclass for given WebResponse.
     */
    protected Class<? extends WebPage> getPageClassForResponse(WebResponse aResp)
    {
        // Handle some common types
        switch (aResp.getFileType()) {

            // Handle image
            case "jpg": case "jpeg": case "gif": case "png": return ImagePage.class;

            // Handle text
            case "txt": return TextPage.class;

            // Handle SnapKit UI file
            case "snp": return SnapPage.class;

            // Handle sound
            case "wav": case "snd": case "mp3": case "m4a": return SoundPage.class;

            // Handle Jar
            case "jar": return ZipPage.class;

            // Handle Java
            case "java": return TextPage.class; //return getPageClass("snapcode.app.JavaPage", TextPage.class);

            // Handle ReportMill
            case "rpt": return getPageClass("com.reportmill.app.ReportPage", TextPage.class);
        }

        // Handle directory
        WebFile file = aResp.getFile();
        if (file != null && file.isDir())
            return DirFilePage.class;

        // Handle Text
        if (file != null && file.isText())
            return TextPage.class;

        // Return Unknown
        return UnknownPage.class;
    }

    /**
     * Returns the browser loader.
     */
    public WebBrowserLoader getLoader()  { return _loader; }

    /**
     * Returns the history.
     */
    public WebBrowserHistory getHistory()  { return _history; }

    /**
     * Returns the previous URL.
     */
    public WebURL getLastURL()  { return _history.getLastUrl(); }

    /**
     * Returns the next URL (if browser has backtracked).
     */
    public WebURL getNextURL()  { return _history.getNextUrl(); }

    /**
     * Sets the browser URL to the last URL (backtracking).
     */
    public void trackBack()  { _history.trackBack(); }

    /**
     * Sets the browser URL to the next URL (forward tracking).
     */
    public void trackForward()  { _history.trackForward(); }

    /**
     * Reloads the current page.
     */
    public void reloadPage()
    {
        WebPage page = getSelPage();
        if (page != null)
            page.reload();
    }

    /**
     * Shows an exception for given URL.
     */
    public void showException(WebURL aURL, Throwable aThrowable)
    {
        // If not EventDispatchThread, re-invoke in that thread
        if (!getEnv().isEventThread()) {
            getEnv().runLater(() -> showException(aURL, aThrowable));
            return;
        }

        // Print stack trace to console
        aThrowable.printStackTrace();

        // Create request/response
        WebRequest req = new WebRequest(aURL);
        WebResponse resp = new WebResponse(req);
        resp.setException(aThrowable);

        // Create page and set
        TextPage exceptionPage = createExceptionPage(resp);
        setSelPage(exceptionPage);
    }

    /**
     * Creates an exception page for exception.
     */
    protected ExceptionPage createExceptionPage(WebResponse aResp)
    {
        ExceptionPage exceptionPage = new ExceptionPage(aResp);
        exceptionPage.setBrowser(this);
        return exceptionPage;
    }

    /**
     * Returns whether browser is currently loading a page.
     */
    public boolean isLoading()  { return _loading; }

    /**
     * Sets whether browser is currently loading a page.
     */
    public void setLoading(boolean aValue)
    {
        if (aValue == _loading) return;
        firePropChange(Loading_Prop, _loading, _loading = aValue);
    }

    /**
     * Returns the status text.
     */
    public String getStatus()  { return _status; }

    /**
     * Sets the status text.
     */
    public void setStatus(String aString)
    {
        if (Objects.equals(aString, _status)) return;
        firePropChange(Status_Prop, _status, _status = aString);
    }

    /**
     * Returns the activity text.
     */
    public String getActivity()  { return _activity; }

    /**
     * Sets the activity text.
     */
    public void setActivity(String aString)
    {
        if (Objects.equals(aString, _activity)) return;
        firePropChange(Activity_Prop, _activity, _activity = aString);
    }

    /**
     * Handles a URL click.
     */
    public boolean performURLClick(String aURL)
    {
        setSelUrlForUrlAddress(aURL);
        return true;
    }

    /**
     * Returns the specified WebPage subclass for given WebFile.
     */
    protected static Class<? extends WebPage> getPageClass(String aClassName, Class<? extends WebPage> aDefault)
    {
        try { return (Class<? extends WebPage>) Class.forName(aClassName); }
        catch (Exception e) { System.err.println("WebBrowser: Page class not found: " + e); }
        return aDefault != null ? aDefault : UnknownPage.class;
    }
}
package snapcode.webbrowser;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.view.TextArea;
import snap.web.WebResponse;
import snap.web.WebURL;

/**
 * This web page subclass shows an exception.
 */
public class ExceptionPage extends TextPage {

    // The exception string
    private String _exceptionString;

    /**
     * Constructor.
     */
    public ExceptionPage(WebResponse aResp)
    {
        super();

        // Get URL and exception
        WebURL url = aResp.getURL();
        Throwable t = aResp.getException();

        // Get default console text
        _exceptionString = String.format("WebBrowser Exception Console\nBrowser Build Date: %s, Version 1.0, JVM %s, User %s\n",
                SnapUtils.getBuildInfo(), System.getProperty("java.version"), System.getProperty("user.name"));
        _exceptionString += t.toString() + '\n';
        _exceptionString += "Caused by: " + StringUtils.getStackTraceString(t);

        // Handle FileNotFound
        if (aResp.getCode() == WebResponse.NOT_FOUND)
            _exceptionString = "\nNot Found\n\nThe requested URL was not found on server.\n\n" + url.getString();

        setResponse(aResp);
    }

    /**
     * Override to set exception string.
     */
    @Override
    protected void loadTextAreaText()
    {
        TextArea textArea = new TextArea();
        textArea.setText(_exceptionString);
    }
}

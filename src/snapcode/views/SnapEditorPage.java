package snapcode.views;
import snapcode.javatext.JavaTextPane;
import snap.view.View;
import snap.view.ViewEvent;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebURL;
import snapcode.app.JavaPage;

/**
 * A WebPage to wrap around SnapEditorPane.
 */
public class SnapEditorPage extends WebPage {

    // The JavaPage
    JavaPage _javaPage;

    // The SnapEditorPane
    SnapEditorPane _editorPane;

    /**
     * Creates a SnapEditorPage.
     */
    public SnapEditorPage()
    {
        _javaPage = new JavaPage();
        _editorPane = new PageSnapEditorPane(_javaPage.getTextPane());
    }

    /**
     * Creates a SnapEditorPage.
     */
    public SnapEditorPage(JavaPage aJavaPage)
    {
        _javaPage = aJavaPage;
        _editorPane = new PageSnapEditorPane(aJavaPage.getTextPane());
    }

    /**
     * Return the JavaPage.
     */
    public JavaPage getJavaPage()  { return _javaPage; }

    /**
     * Override to forward to JavaPage.
     */
    public void setBrowser(WebBrowser aBrowser)
    {
        super.setBrowser(aBrowser);
        _javaPage.setBrowser(aBrowser);
    }

    /**
     * Override to forward to JavaPage.
     */
    public void setURL(WebURL aURL)
    {
        super.setURL(aURL);
        _javaPage.setURL(aURL);
    }

    /**
     * Override to forward to JavaPage.
     */
    public void setFile(WebFile aFile)
    {
        super.setFile(aFile);
        _javaPage.setFile(aFile);
    }

    /**
     * Override to forward to JavaPage.
     */
    public void setResponse(WebResponse aResp)
    {
        super.setResponse(aResp);
        _javaPage.setResponse(aResp);
    }

    /**
     * Override to forward to JavaPage.
     */
    public void reload()
    {
        super.reload();
        _javaPage.reload();
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        return _editorPane.getUI();
    }

    /**
     * Reopen this page as SnapCodePage.
     */
    public void openAsJavaText()
    {
        WebURL url = getURL();
        WebBrowser browser = getBrowser();
        browser.setPageForURL(url, _javaPage);
        browser.setSelUrl(url);
    }

    /**
     * Returns whether given file wants to be SanpEditorPage.
     */
    public static boolean isSnapEditSet(WebFile aFile)
    {
        //JavaData jdata = JavaData.get(aFile); Class cls = jdata.getJFile().getEvalClass();
        //for(Class c=cls;c!=null;c=c.getSuperclass()) if(c.getSimpleName().equals("GameActor")) return true;

        // Return true if 'SnapEdit=true' is found in the first comment
        String str = aFile.getText();
        if (str == null) return false;
        str = str.substring(0, str.indexOf("*/") + 1);
        return str.contains("SnapEdit=true");
    }

    /**
     * Override to Handle JavaButton.
     */
    private class PageSnapEditorPane extends SnapEditorPane {

        /**
         * Creates a new SnapEditorPane for given JavaTextPane.
         */
        public PageSnapEditorPane(JavaTextPane aJTP)
        {
            super(aJTP);
        }

        /**
         * Respond to UI changes.
         */
        @Override
        protected void respondUI(ViewEvent anEvent)
        {
            // Handle JavaButton
            if (anEvent.equals("JavaButton")) openAsJavaText();

                // Do normal version
            else super.respondUI(anEvent);
        }
    }
}
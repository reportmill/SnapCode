package snapcode.app;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.AccountTool;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;

/**
 * This page helps manage default cloud storage.
 */
public class SnapCloudPage extends WebPage {

    // The WebBrowser for remote files
    private WebBrowser _remoteBrowser;

    /**
     * Constructor.
     */
    public SnapCloudPage(WorkspacePane workspacePane)
    {
        super();
    }

    /**
     * Connect to snap cloud user site.
     */
    public void connectToSnapCloudUserSite()
    {
        // Get remote site
        WebSite snapCloudUserSite = getSnapCloudUserSite();
        if (snapCloudUserSite == null) {
            _remoteBrowser.setSelFile(null);
            return;
        }

        // Reset remote site root dir
        WebFile rootDir = snapCloudUserSite.getRootDir();
        rootDir.resetAndVerify();

        // Set root dir in remote browser
        _remoteBrowser.setSelFile(rootDir);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        getUI().setFill(ViewTheme.get().getContentColor());

        // Configure label
        Label snapCloudLabel = getView("SnapCloudLabel", Label.class);
        WebURL snapCloudImageUrl = WebURL.getURL("https://reportmill.com/SnapCode/images/SnapCloud.png");
        Image snapCloudImage = Image.getImageForUrl(snapCloudImageUrl);
        snapCloudLabel.setImage(snapCloudImage);

        // Get WebBrowser for remote files
        _remoteBrowser = new WebBrowser();
        _remoteBrowser.addPropChangeListener(e -> resetLater(), WebBrowser.Loading_Prop);

        // Add to RemoteBrowserBox
        BoxView remoteBrowserBox = getView("RemoteBrowserBox", BoxView.class);
        remoteBrowserBox.setContent(_remoteBrowser);
    }

    /**
     * Initialize showing.
     */
    @Override
    protected void initShowing()
    {
        runDelayed(this::connectToSnapCloudUserSite, 1000);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // If user email hasn't been set, show email box
        String userEmail = AccountTool.getUserEmail();
        setViewVisible("EmailBox", userEmail == null || userEmail.isEmpty());

        // Update ProgressBar
        ProgressBar progressBar = getView("ProgressBar", ProgressBar.class);
        boolean loading = _remoteBrowser.isLoading();
        System.out.println("Remote browser loaded: " + loading);
        progressBar.setVisible(loading);
        progressBar.setProgress(loading ? -1 : 0);

        // Update RemoteBrowserToolsBox, BoxRemoteBrowserBox
        setViewVisible("RemoteBrowserToolsBox", userEmail != null && !userEmail.isEmpty());
        setViewVisible("RemoteBrowserBox", userEmail != null && !userEmail.isEmpty());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle EmailText
            case "EmailText":
                AccountTool.setUserEmail(anEvent.getStringValue());
                runLater(this::connectToSnapCloudUserSite);
                break;
        }
    }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebSite getSnapCloudUserSite()
    {
        WebURL snapCloudUserUrl = getSnapCloudUserUrl();
        if (snapCloudUserUrl == null)
            return null;
        return snapCloudUserUrl.getAsSite();
    }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebURL getSnapCloudUserUrl()
    {
        String userEmail = AccountTool.getUserEmail();
        if (userEmail == null || userEmail.isEmpty())
            return null;

        int sepIndex = userEmail.indexOf('@');
        if (sepIndex <= 0)
            return null;

        // Get username and domain
        String userName = userEmail.substring(0, sepIndex);
        String domain = userEmail.substring(sepIndex + 1);

        // Return
        String snapCloudUrlAddress  = "dbox://dbox.com/" + domain + "/" + userName;
        return WebURL.getURL(snapCloudUrlAddress);
    }
}

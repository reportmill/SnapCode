package snapcode.app;
import snap.gfx.Image;
import snap.util.TaskMonitor;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.AccountTool;
import snapcode.webbrowser.DirFilePage;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;

/**
 * This page helps manage default cloud storage.
 */
public class SnapCloudPage extends WebPage {

    // The WorkspacePane
    private WorkspacePane _workspacePane;

    // The WebBrowser for remote files
    private WebBrowser _remoteBrowser;

    /**
     * Constructor.
     */
    public SnapCloudPage(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
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
        _remoteBrowser.addEventFilter(e -> resetLater(), MouseRelease);
    }

    /**
     * Returns the selected file.
     */
    private WebFile getSelFile()
    {
        DirFilePage dirFilePage = (DirFilePage) _remoteBrowser.getSelPage();
        return dirFilePage != null ? dirFilePage.getSelFile() : null;
    }

    /**
     * Sets the selected file.
     */
    private void setSelFile(WebFile aFile)
    {
        DirFilePage dirFilePage = (DirFilePage) _remoteBrowser.getSelPage();
        if (dirFilePage != null)
            dirFilePage.setSelFile(aFile);
    }

    /**
     * Opens project for selected file.
     */
    private void openProjectForSelFile()
    {
        WebURL snapCloudUserUrl = getSelectedProjectUrl();
        if (snapCloudUserUrl == null)
            return;

        WorkspacePaneUtils.openProjectForRepoUrl(_workspacePane, snapCloudUserUrl);
    }

    /**
     * Deletes remote file.
     */
    private void deleteSelFile()
    {
        // Ask user if they really want to do this
        WebFile selFile = getSelFile(); if (selFile == null) return;
        String title = "Delete file: " + selFile.getName();
        String msg = "Are you sure you want to delete SnapCloud file: " + selFile.getName() + "?";
        if (!DialogBox.showConfirmDialog(getUI(), title, msg))
            return;

        // Create task for delete and start
        TaskManagerTask<?> deleteSelFileTask = _workspacePane.getTaskManager().createTask();
        deleteSelFileTask.setTaskFunction(() -> { deleteSelFileImpl(deleteSelFileTask.getTaskMonitor()); return null; });
        deleteSelFileTask.start();
    }

    /**
     * Deletes remote file.
     */
    private void deleteSelFileImpl(TaskMonitor taskMonitor)
    {
        WebFile selFile = getSelFile(); if (selFile == null) return;
        WebFile parent = selFile.getParent();

        taskMonitor.startForTaskCount(1);
        taskMonitor.beginTask("Delete file: " + selFile.getName(), -1);
        selFile.delete();
        parent.resetAndVerify();
        setSelFile(parent);
        taskMonitor.endTask();
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
        runLater(this::connectToSnapCloudUserSite);
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
        progressBar.setVisible(loading);
        progressBar.setProgress(loading ? -1 : 0);

        // Update RemoteBrowserToolsBox, BoxRemoteBrowserBox
        setViewVisible("RemoteBrowserToolsBox", userEmail != null && !userEmail.isEmpty());
        setViewVisible("RemoteBrowserBox", userEmail != null && !userEmail.isEmpty());

        // Update OpenProjectButton
        WebURL selProjectUrl = getSelectedProjectUrl();
        setViewVisible("OpenProjectButton", selProjectUrl != null);
        if (selProjectUrl != null)
            setViewText("OpenProjectButton", "Open " + selProjectUrl.getFilename());

        // Update DeleteFileButton
        setViewVisible("DeleteFileButton", selProjectUrl != null);
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

            // Handle OpenProjectButton
            case "OpenProjectButton": openProjectForSelFile(); break;

            // Handle DeleteFileButton
            case "DeleteFileButton": deleteSelFile(); break;
        }
    }

    /**
     * Returns the selected project name.
     */
    private WebURL getSelectedProjectUrl()
    {
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.isRoot())
            return null;

        while (selFile.getParent() != null && !selFile.getParent().isRoot())
            selFile = selFile.getParent();

        String urlAddr = selFile.getUrlAddress();
        return WebURL.getURL(urlAddr);
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

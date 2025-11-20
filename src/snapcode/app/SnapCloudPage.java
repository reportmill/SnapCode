package snapcode.app;
import snap.gfx.Image;
import snap.util.ActivityMonitor;
import snap.util.TaskRunner;
import snap.util.UserInfo;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.VersionControlTool;
import snapcode.project.Project;
import snapcode.project.VersionControlSnapCloud;
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

    // Initialize SnapCloud
    static { SnapCloudInit.initSnapCloud(); }

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
        rootDir.reset();

        // Set root dir in remote browser
        _remoteBrowser.setSelFile(rootDir);
        _remoteBrowser.addEventFilter(e -> resetLater(), MouseRelease);
    }

    /**
     * Returns the selected file.
     */
    private WebFile getSelFile()
    {
        WebPage selPage = _remoteBrowser.getSelPage();
        if (selPage instanceof DirFilePage dirFilePage)
            return dirFilePage.getSelFile();
        return null;
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
     * Creates a new project.
     */
    private void createNewSnapCodeProject()
    {
        NewFileTool newFileTool = _workspacePane.getNewFileTool();
        ViewUtils.runLater(() -> {
            Project newProject = newFileTool.showNewProjectPanel();
            if (newProject != null) {
                ViewUtils.runLater(() -> {
                    ProjectPane projectPane = _workspacePane.getProjectPaneForProject(newProject);
                    VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                    versionControlTool.saveToSnapCloud();
                    WorkspacePaneUtils.selectGoodDefaultFile(_workspacePane, newProject);
                });
            }
        });
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
     * Shows a share/copy link panel for selected project.
     */
    private void shareProjectForSelFile()
    {
        WebURL snapCloudUserUrl = getSelectedProjectUrl();
        if (snapCloudUserUrl == null)
            return;

        // Get share link
        String shareLink = "https://reportmill.com/SnapCode/app/#" + snapCloudUserUrl.getString();

        // Define share link panel UI
        String SHARE_PROJECT_PANEL_UI = """
            <TextView Name="TextArea" Font="Arial 14" Margin="20" Padding="8" Border="#88" BorderRadius="4" />
            """;

        // Create share link panel UI
        TextArea shareLinkTextArea = (TextArea) UILoader.loadViewForString(SHARE_PROJECT_PANEL_UI);
        shareLinkTextArea.setEditable(true);
        shareLinkTextArea.setText(shareLink);
        shareLinkTextArea.selectAll();

        // Show share link panel
        DialogBox dialogBox = new DialogBox("Share Link Panel");
        dialogBox.setMessage("Copy link below");
        dialogBox.setContent(shareLinkTextArea);
        dialogBox.showMessageDialog(_workspacePane.getUI());

        // Copy link to pasteboard
        Clipboard clipboard = Clipboard.get();
        clipboard.addData(shareLink);
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
        TaskRunner<?> deleteSelFileTask = _workspacePane.getTaskManager().createTask();
        deleteSelFileTask.setTaskFunction(() -> { deleteSelFileImpl(deleteSelFileTask.getMonitor()); return null; });
        deleteSelFileTask.start();
    }

    /**
     * Deletes remote file.
     */
    private void deleteSelFileImpl(ActivityMonitor activityMonitor)
    {
        WebFile selFile = getSelFile(); if (selFile == null) return;
        WebFile parent = selFile.getParent();

        // Init activity monitor - add bogus work unit to add progress
        activityMonitor.startForTaskCount(1);
        activityMonitor.beginTask("Delete file: " + selFile.getName(), 2);
        activityMonitor.updateTask(1);

        // Delete file, reset and select parent
        selFile.delete();
        setSelFile(parent);
        activityMonitor.endTask();
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
        WebURL snapCloudImageUrl = WebURL.getUrl("https://reportmill.com/SnapCode/images/SnapCloud.png");
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
        String userEmail = UserInfo.getUserEmail();
        setViewVisible("EmailBox", userEmail == null || userEmail.isBlank());
        setViewVisible("NewSnapCloudProjectButton", userEmail != null && !userEmail.isBlank());

        // Update ProgressBar
        setViewVisible("ProgressBar", _remoteBrowser.isLoading());

        // Update RemoteBrowserToolsBox, BoxRemoteBrowserBox
        setViewVisible("RemoteBrowserToolsBox", userEmail != null && !userEmail.isBlank());
        setViewVisible("RemoteBrowserBox", userEmail != null && !userEmail.isBlank());

        // Update OpenProjectButton
        WebURL selProjectUrl = getSelectedProjectUrl();
        setViewVisible("OpenProjectButton", selProjectUrl != null);
        setViewVisible("ShareProjectButton", selProjectUrl != null);
        if (selProjectUrl != null) {
            setViewText("OpenProjectButton", "Open " + selProjectUrl.getFilename());
            setViewText("ShareProjectButton", "Share " + selProjectUrl.getFilename() + "...");
        }

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

            // Handle NewSnapCloudProjectButton
            case "NewSnapCloudProjectButton" -> createNewSnapCodeProject();

            // Handle EmailText
            case "EmailText" -> {
                UserInfo.setUserEmail(anEvent.getStringValue());
                runLater(this::connectToSnapCloudUserSite);
            }

            // Handle OpenProjectButton, ShareProjectButton
            case "OpenProjectButton" -> openProjectForSelFile();
            case "ShareProjectButton" -> shareProjectForSelFile();

            // Handle DeleteFileButton
            case "DeleteFileButton" -> deleteSelFile();
        }
    }

    /**
     * Returns the selected project name.
     */
    private WebURL getSelectedProjectUrl()
    {
        // If no selected directory, just return
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.isRoot())
            return null;

        // Get project directory
        while (!selFile.getParent().isRoot())
            selFile = selFile.getParent();

        // Return as flat url
        return selFile.getUrl().getFlatUrl();
    }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebSite getSnapCloudUserSite()  { return VersionControlSnapCloud.getSnapCloudUserSite(); }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebURL getSnapCloudUserUrl()  { return VersionControlSnapCloud.getSnapCloudUserUrl(); }
}

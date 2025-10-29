package snapcode.apptools;
import snap.props.PropChangeListener;
import snap.view.BoxView;
import snap.web.WebURL;
import snapcode.app.SnapCloudPage;
import snapcode.app.WorkspaceTools;
import snapcode.project.TaskManagerTask;
import snapcode.project.VersionControlUtils;
import snapcode.project.WorkspaceBuilder;
import snap.props.PropChange;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;
import snapcode.project.VersionControl;
import snap.util.ActivityMonitor;
import snap.util.TaskRunner;
import snap.view.ProgressBar;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snapcode.webbrowser.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * This ProjectTool subclass manages version control for project.
 */
public class VersionControlTool extends ProjectTool {

    // The VersionControl
    private VersionControl _versionControl;

    // The WebBrowser for remote files
    private WebBrowser _remoteBrowser;

    // The VersionControl prop change listener
    private PropChangeListener _versionControlPropChangeLsnr = this::handleVersionControlFileStatusChange;

    /**
     * Constructor.
     */
    public VersionControlTool(ProjectPane projectPane)
    {
        super(projectPane);

        // Get VersionControl for project site and set listener
        WebSite projectSite = getProjectSite();
        _versionControl = VersionControl.getVersionControlForProjectSite(projectSite);
        _versionControl.addPropChangeListener(_versionControlPropChangeLsnr);
    }

    /**
     * Returns the VersionControl.
     */
    public VersionControl getVC()  { return _versionControl; }

    /**
     * Returns the remote URL.
     */
    public String getRemoteUrlAddress()  { return _versionControl.getRemoteSiteUrlAddress(); }

    /**
     * Sets the Remote URL.
     */
    private void setRemoteUrlAddress(String urlAddress)
    {
        if (Objects.equals(urlAddress, getRemoteUrlAddress())) return;

        // Deactivate Version control and re-open site
        deactivate();
        WebSite projectSite = getProjectSite();
        VersionControlUtils.setRemoteSiteUrlAddress(projectSite, urlAddress);

        // Get VersionControl for project site and set listener
        _versionControl = VersionControl.getVersionControlForProjectSite(projectSite);
        _versionControl.addPropChangeListener(_versionControlPropChangeLsnr);
    }

    /**
     * Returns the remote site.
     */
    private WebSite getRemoteSite()  { return _versionControl.getRemoteSite(); }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get WebBrowser for remote files
        _remoteBrowser = new WebBrowser();
        _remoteBrowser.addPropChangeListener(e -> resetLater(), WebBrowser.Loading_Prop);

        // Add to RemoteBrowserBox
        BoxView remoteBrowserBox = getView("RemoteBrowserBox", BoxView.class);
        remoteBrowserBox.setContent(_remoteBrowser);
    }

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Update RemoteURLText
        String remoteUrlAddress = getRemoteUrlAddress();
        setViewValue("RemoteURLText", remoteUrlAddress);

        // Get remote exists in non-blocking way
        WebSite remoteSite = getRemoteSite();
        WebFile rootDir = remoteSite != null ? remoteSite.getRootDir() : null;
        boolean isRemoteExists = rootDir != null && rootDir.isVerified() && rootDir.getExists();
        if (rootDir != null && !rootDir.isVerified())
            CompletableFuture.runAsync(() -> { rootDir.getExists(); resetLater(); });

        // Update CreateRemoteButton, CreateCloneButton, ConnectButton
        boolean isCloneExists = _versionControl.isCloneExists();
        setViewVisible("CreateRemoteButton", !isRemoteExists && _versionControl.canCreateRemote());
        setViewVisible("CreateCloneButton", !isCloneExists && _versionControl.canCreateRemote());
        setViewVisible("ConnectButton", isRemoteExists);

        // Update CheckoutButton, UpdateFilesButton, ReplaceFilesButton, CommitFilesButton
        boolean isCheckedOut = _versionControl.isCheckedOut();
        setViewVisible("CheckoutButton", !isCheckedOut && isRemoteExists);
        setViewVisible("UpdateFilesButton", isCheckedOut);
        setViewVisible("ReplaceFilesButton", isCheckedOut);
        setViewVisible("CommitFilesButton", isCheckedOut);

        // Update DisconnectButton
        setViewVisible("DisconnectButton", isCloneExists);

        // Update SnapCloudButton
        setViewVisible("SnapCloudButton", (remoteUrlAddress == null || remoteUrlAddress.isEmpty()) &&
                SnapCloudPage.getSnapCloudUserUrl() != null);

        // Update ProgressBar
        ProgressBar progressBar = getView("ProgressBar", ProgressBar.class);
        boolean loading = _remoteBrowser.isLoading();
        progressBar.setVisible(loading);
        progressBar.setProgress(loading ? -1 : 0);
    }

    /**
     * Responds to changes to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle RemoteURLText
            case "RemoteURLText": setRemoteUrlAddress(anEvent.getStringValue()); break;

            // Handle ConnectButton, CreateCloneButton, CheckoutButton
            case "CreateRemoteButton": createRemoteSite(); break;
            case "CreateCloneButton": createCloneSite(); break;
            case "ConnectButton": connectToRemoteSite(); break;
            case "CheckoutButton": checkout(); break;

            // Handle UpdateFilesButton, ReplaceFilesButton, CommitFilesButton
            case "UpdateFilesButton": checkForUpdates(false); break;
            case "ReplaceFilesButton": replaceFiles(getSiteRootDirAsList()); break;
            case "CommitFilesButton": commitFiles(getSiteRootDirAsList(), true); break;

            // Handle DisconnectButton
            case "DisconnectButton": setRemoteUrlAddress(null); break;

            // Handle SnapCloudButton
            case "SnapCloudButton": saveToSnapCloud(); break;
        }
    }

    /**
     * Create remote site.
     */
    private void createRemoteSite()
    {
        // Create ActivityMonitor for create remote site
        String title = "Create remote site " + _versionControl.getRemoteSiteUrlAddress();
        ActivityMonitor activityMonitor = new ActivityMonitor(title);
        TaskRunner<Boolean> createRemoteSiteRunner = new TaskRunner<>(() -> _versionControl.createRemoteSite(activityMonitor));
        createRemoteSiteRunner.setMonitor(activityMonitor);

        // Configure callbacks and start
        createRemoteSiteRunner.setOnSuccess(obj -> handleCreateRemoteSuccess());
        createRemoteSiteRunner.setOnFailure(this::handleCreateRemoteFailed);
        createRemoteSiteRunner.start();
    }

    /**
     * Called when create remote succeeds.
     */
    private void handleCreateRemoteSuccess()
    {
        connectToRemoteSite();
        resetLater();
    }

    /**
     * Called when create remote fails.
     */
    private void handleCreateRemoteFailed(Exception exception)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Create remote failed", exception);
    }

    /**
     * Create clone site.
     */
    private void createCloneSite()
    {
        // Create ActivityMonitor for create clone site
        String title = "Create clone site " + _versionControl.getRemoteSiteUrlAddress();
        ActivityMonitor activityMonitor = new ActivityMonitor(title);
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> _versionControl.createCloneSite(activityMonitor));
        checkoutRunner.setMonitor(activityMonitor);

        // Configure callbacks and start
        checkoutRunner.setOnSuccess(obj -> handleCreateRemoteSuccess());
        checkoutRunner.setOnFailure(this::handleCreateRemoteFailed);
        checkoutRunner.start();
    }

    /**
     * Connect to remote site.
     */
    public void connectToRemoteSite()
    {
        // Get remote site
        WebSite remoteSite = getRemoteSite();
        if (remoteSite == null) {
            _remoteBrowser.setSelFile(null);
            return;
        }

        // Reset remote site root dir
        WebFile rootDir = remoteSite.getRootDir();
        rootDir.reset();

        // Set root dir in remote browser
        _remoteBrowser.setSelFile(rootDir);
    }

    /**
     * Called to end VCS association with this project.
     */
    public void deactivate()
    {
        try {
            ActivityMonitor activityMonitor = new ActivityMonitor(System.out);
            _versionControl.disconnect(activityMonitor);
        }

        catch (Exception e) {
            DialogBox dialogBox = new DialogBox("Disconnect Error");
            dialogBox.setErrorMessage(e.toString());
            dialogBox.showMessageDialog(_workspacePane.getUI());
        }
    }

    /**
     * Load all remote files into project directory.
     */
    public void checkout()
    {
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(false);

        // Create ActivityMonitor for checkout
        String title = "Checkout from " + _versionControl.getRemoteSiteUrlAddress();
        ActivityMonitor activityMonitor = new ActivityMonitor(title);
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> _versionControl.checkout(activityMonitor));
        checkoutRunner.setMonitor(activityMonitor);

        // Configure callbacks and start
        checkoutRunner.setOnSuccess(obj -> handleCheckoutSuccess());
        checkoutRunner.setOnFailure(exception -> handleCheckoutFailed(exception));
        checkoutRunner.start();

        // Show progress dialog
        activityMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when checkout succeeds.
     */
    private void handleCheckoutSuccess()
    {
        // Reload files
        WebSite projectSite = getProjectSite();
        projectSite.getRootDir().resetAndVerify();

        // Reset UI
        _workspacePane.resetLater();
        resetLater();

        // Reset AutoBuildEnabled and trigger auto build
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        builder.addAllFilesToBuild();
        builder.buildWorkspaceLater();
    }

    /**
     * Called when checkout fails.
     */
    private void handleCheckoutFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Checkout failed", anException);
    }

    /**
     * Checks for updates to project files and shows update panel if update files found.
     */
    public void checkForUpdates(boolean checkPassively)
    {
        checkForUpdatesForFiles(List.of(_proj.getRootDir()), checkPassively);
    }

    /**
     * Checks for updates to given files and shows update panel if update files found.
     */
    public void checkForUpdatesForFiles(List<WebFile> localFiles, boolean checkPassively)
    {
        // Sanity check
        if (!_versionControl.isCheckedOut()) return;

        // Create task find update files and forward to update
        TaskManagerTask<List<WebFile>> updateTask = (TaskManagerTask<List<WebFile>>) _workspacePane.getTaskManager().createTask();
        updateTask.setTaskFunction(() -> _versionControl.getUpdateFilesForLocalFiles(localFiles, updateTask.getActivityMonitor()));
        updateTask.setOnSuccess(updateFiles -> handleCheckForUpdatesSuccess(updateFiles, checkPassively));
        updateTask.setOnFailure(e -> e.printStackTrace());
        updateTask.start();
    }

    /**
     * Called when checkForUpdatesForFiles succeeds with list of update files and whether to always show transfer pane.
     */
    private void handleCheckForUpdatesSuccess(List<WebFile> updateFiles, boolean checkPassively)
    {
        // If no update files and update check is passive, just return
        if (updateFiles.isEmpty() && checkPassively)
            return;

        // Run VcsTransferPane for files and op to confirm
        if (!new VcsTransferPane().showPanel(this, updateFiles, VcsTransferPane.Op.Update))
            return;

        // Update files
        updateFiles(updateFiles);
    }

    /**
     * Called to update files.
     */
    private void updateFiles(List<WebFile> updateFiles)
    {
        // Disable workspace AutoBuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Call real update files method and configure callbacks
        ActivityMonitor activityMonitor = new ActivityMonitor("Update files from remote site");
        TaskRunner<Boolean> updateRunner = new TaskRunner<>(() -> _versionControl.updateFiles(updateFiles, activityMonitor));
        updateRunner.setMonitor(activityMonitor);
        updateRunner.setOnSuccess(completed -> handleUpdateFilesSuccess(updateFiles));
        updateRunner.setOnFinished(() -> handleUpdateFilesFinished());
        updateRunner.setOnFailure(exception -> handleUpdateFilesFailed(exception));
        updateRunner.start();

        // Show progress dialog
        activityMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when update files succeeds.
     */
    private void handleUpdateFilesSuccess(List<WebFile> theFiles)
    {
        resetAndReloadFiles(theFiles);
    }

    /**
     * Called when update files finishes.
     */
    private void handleUpdateFilesFinished()
    {
        // Reset AutoBuildEnabled and build Project
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        builder.buildWorkspaceLater();

        // Connect to remote site
        if (isUISet())
            connectToRemoteSite();
    }

    /**
     * Called when update files fails.
     */
    private void handleUpdateFilesFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Update files failed", anException);
    }

    /**
     * Called to replace files.
     */
    public void replaceFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.isCheckedOut()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.isEmpty())
            rootFiles = getSiteRootDirAsList();

        // Get replace files for root files
        List<WebFile> replaceFiles = _versionControl.getModifiedFilesForLocalFiles(rootFiles);

        // Run VcsTransferPane for files and op
        if (!new VcsTransferPane().showPanel(this, replaceFiles, VcsTransferPane.Op.Replace))
            return;

        // Disable workspace AutoBuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Call real replace method and configure callbacks
        ActivityMonitor activityMonitor = new ActivityMonitor("Replace files from remote site");
        TaskRunner<Boolean> replaceRunner = new TaskRunner<>(() -> _versionControl.replaceFiles(replaceFiles, activityMonitor));
        replaceRunner.setMonitor(activityMonitor);
        replaceRunner.setOnSuccess(obj -> handleReplaceFilesSuccess(replaceFiles));
        replaceRunner.setOnFinished(() -> handleReplaceFilesFinished());
        replaceRunner.setOnFailure(exception -> handleReplaceFilesFailed(exception));
        replaceRunner.start();

        // Show progress dialog
        activityMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when replace files succeeds.
     */
    private void handleReplaceFilesSuccess(List<WebFile> theFiles)
    {
        resetAndReloadFiles(theFiles);
    }

    /**
     * Called when replace files finishes.
     */
    private void handleReplaceFilesFinished()
    {
        // Reset AutoBuildEnabled and build Project
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        builder.buildWorkspaceLater();

        // Connect to remote site
        if (isUISet())
            connectToRemoteSite();
    }

    /**
     * Called when replace files fails.
     */
    private void handleReplaceFilesFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Replace files failed", anException);
    }

    /**
     * Called to commit files.
     */
    public void commitFiles(List<WebFile> theFiles, boolean showPanel)
    {
        // Sanity check
        if (!_versionControl.isCheckedOut()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.isEmpty())
            rootFiles = getSiteRootDirAsList();

        // Get commit files for root files
        List<WebFile> commitFiles = _versionControl.getModifiedFilesForLocalFiles(rootFiles);

        // Run VersionControlFilesPane for files and op
        VcsTransferPane transferPane = new VcsTransferPane();
        if (showPanel && !transferPane.showPanel(this, commitFiles, VcsTransferPane.Op.Commit))
            return;

        // Do real commit
        String commitMessage = transferPane.getCommitMessage();
        ActivityMonitor activityMonitor = new ActivityMonitor("Commit files to remote site");
        TaskRunner<Boolean> commitRunner = new TaskRunner<>(() -> _versionControl.commitFiles(commitFiles, commitMessage, activityMonitor));
        commitRunner.setMonitor(activityMonitor);
        commitRunner.setOnFailure(exception -> handleCommitFilesFailed(exception));
        commitRunner.start();

        // Show progress dialog
        activityMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when commit files fails.
     */
    private void handleCommitFilesFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Commit files failed", anException);
    }

    /**
     * Called when VersionControl changes a file status.
     */
    private void handleVersionControlFileStatusChange(PropChange aPC)
    {
        WebFile file = (WebFile) aPC.getSource();
        ProjectFilesTool projectFilesTool = _workspacePane.getProjectFilesTool();
        projectFilesTool.handleFileChange(file);
    }

    /**
     * Save files to Snap cloud in background.
     */
    private void saveToSnapCloud()
    {
        // Reset RemoteUrl to snap cloud
        WebURL snapCloudUserUrl = SnapCloudPage.getSnapCloudUserUrl();
        assert snapCloudUserUrl != null;
        WebURL snapCloudProjectUrl = snapCloudUserUrl.getChildUrlForPath(_proj.getName());
        setRemoteUrlAddress(snapCloudProjectUrl.getString());

        // Create ActivityMonitor for save to snap cloud
        String title = "Save to Snap Cloud " + _versionControl.getRemoteSiteUrlAddress();
        ActivityMonitor activityMonitor = new ActivityMonitor(title);
        TaskRunner<Boolean> saveToSnapCloudRunner = new TaskRunner<>(() -> saveToSnapCloudImpl(activityMonitor));
        saveToSnapCloudRunner.setMonitor(activityMonitor);

        // Configure callbacks and start
        saveToSnapCloudRunner.setOnSuccess(obj -> resetLater());
        saveToSnapCloudRunner.setOnFailure(this::handleCommitFilesFailed);
        saveToSnapCloudRunner.start();

        // Show progress dialog
        activityMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Save files to Snap cloud in background.
     */
    private boolean saveToSnapCloudImpl(ActivityMonitor activityMonitor) throws Exception
    {
        _versionControl.createRemoteSite(activityMonitor);
        List<WebFile> commitFiles = _versionControl.getModifiedFilesForLocalFiles(getSiteRootDirAsList());
        return _versionControl.commitFiles(commitFiles, "", activityMonitor);
    }

    /**
     * Resets the given files and reloads.
     */
    private void resetAndReloadFiles(List<WebFile> theFiles)
    {
        // Revert files
        WorkspaceTools workspaceTools = getWorkspaceTools();
        FilesTool filesTool = workspaceTools.getFilesTool();
        theFiles.forEach(filesTool::revertFile);

        // Reset WorkspacePane
        _workspacePane.resetLater();
    }

    /**
     * Returns the Site.RootDir as list.
     */
    private List<WebFile> getSiteRootDirAsList()  { return List.of(_proj.getRootDir()); }
}
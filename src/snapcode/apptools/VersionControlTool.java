package snapcode.apptools;
import snap.geom.HPos;
import snap.props.PropChangeListener;
import snap.view.*;
import snap.web.WebURL;
import snapcode.app.SnapCloudPage;
import snapcode.app.WorkspaceTools;
import snapcode.project.VersionControlSnapCloud;
import snapcode.project.VersionControlUtils;
import snapcode.project.WorkspaceBuilder;
import snap.props.PropChange;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;
import snapcode.project.VersionControl;
import snap.util.ActivityMonitor;
import snap.util.TaskRunner;
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
        resetLater();
    }

    /**
     * Clears the remote URL address.
     */
    private void clearRemoteUrlAddress()
    {
        setRemoteUrlAddress(null);
        connectToRemoteSite();
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

        // Add clear button to RemoteURLText
        CloseBox clearRemoteUrlButton = new CloseBox();
        clearRemoteUrlButton.setMargin(0, 4, 0, 4);
        clearRemoteUrlButton.setLeanX(HPos.RIGHT);
        clearRemoteUrlButton.addEventHandler(e -> clearRemoteUrlAddress(), View.Action);
        TextField remoteUrlText = getView("RemoteURLText", TextField.class);
        remoteUrlText.getLabel().setGraphicAfter(clearRemoteUrlButton);
        remoteUrlText.getLabel().setPickable(true);
    }

    /**
     * Override to connect to remote.
     */
    @Override
    protected void initShowing()
    {
        connectToRemoteSite();
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

        // Update CreateRemoteButton
        setViewVisible("CreateRemoteButton", !isRemoteExists && _versionControl.canCreateRemote());

        // Update CheckoutButton, UpdateFilesButton, ReplaceFilesButton, CommitFilesButton
        boolean isCheckedOut = _versionControl.isCheckedOut();
        setViewVisible("CheckoutButton", !isCheckedOut && isRemoteExists);
        setViewVisible("UpdateFilesButton", isCheckedOut);
        setViewVisible("ReplaceFilesButton", isCheckedOut);
        setViewVisible("CommitFilesButton", isCheckedOut);

        // Update ReplaceFilesButton, CommitFilesButton Enabled based on whether there are modified files
        boolean isProjectModified = isCheckedOut && _versionControl.isFileModified(_versionControl.getLocalSite().getRootDir());
        setViewEnabled("ReplaceFilesButton", isProjectModified);
        setViewEnabled("CommitFilesButton", isProjectModified);

        // Update SnapCloudButton, SnapCloudAutosaveCheckBox
        setViewVisible("SnapCloudButton", (remoteUrlAddress == null || remoteUrlAddress.isEmpty()) &&
                SnapCloudPage.getSnapCloudUserUrl() != null);
        setViewVisible("SnapCloudAutosaveCheckBox", _versionControl instanceof VersionControlSnapCloud);
        if (_versionControl instanceof VersionControlSnapCloud snapCloudVC)
            setViewValue("SnapCloudAutosaveCheckBox", snapCloudVC.isAutoSave());

        // Update ProgressBar
        setViewVisible("ProgressBar", _remoteBrowser.isLoading());
    }

    /**
     * Responds to changes to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle RemoteURLText
            case "RemoteURLText" -> setRemoteUrlAddress(anEvent.getStringValue());

            // Handle CreateRemoteButton, CheckoutButton
            case "CreateRemoteButton" -> createRemoteSite();
            case "CheckoutButton" -> checkout();

            // Handle UpdateFilesButton, ReplaceFilesButton, CommitFilesButton
            case "UpdateFilesButton" -> checkForUpdates(false);
            case "ReplaceFilesButton" -> replaceFiles(getSiteRootDirAsList());
            case "CommitFilesButton" -> commitFiles(getSiteRootDirAsList(), true);

            // Handle SnapCloudButton, SnapCloudAutosaveCheckBox
            case "SnapCloudButton" -> saveToSnapCloud();
            case "SnapCloudAutosaveCheckBox" -> ((VersionControlSnapCloud) _versionControl).setAutoSave(anEvent.getBoolValue());
        }
    }

    /**
     * Create remote site.
     */
    private void createRemoteSite()
    {
        // Create task runner for create remote site, configure callbacks and start
        TaskRunner<Boolean> createRemoteSiteRunner = new TaskRunner<>("Create remote site " + _versionControl.getRemoteSiteUrlAddress());
        createRemoteSiteRunner.setTaskFunction(() -> _versionControl.createRemoteSite(createRemoteSiteRunner.getMonitor()));
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

        // Reset remote files
        remoteSite.resetFiles();

        // Set root dir in remote browser
        WebFile rootDir = remoteSite.getRootDir();
        _remoteBrowser.setSelFile(rootDir);
    }

    /**
     * Called to end VCS association with this project.
     */
    public void deactivate()
    {
        try {
            ActivityMonitor activityMonitor = ActivityMonitor.getSystemOutActivityMonitor();
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

        // Create task runner for checkout, configure callbacks and start
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>("Checkout from " + _versionControl.getRemoteSiteUrlAddress());
        checkoutRunner.setTaskFunction(() -> _versionControl.checkout(checkoutRunner.getMonitor()));
        checkoutRunner.setOnSuccess(obj -> handleCheckoutSuccess());
        checkoutRunner.setOnFailure(exception -> handleCheckoutFailed(exception));
        checkoutRunner.start();

        // Show progress dialog
        checkoutRunner.getMonitor().showProgressPanel(_workspacePane.getUI());
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

        // Reset remote files
        getRemoteSite().resetFiles();

        // Create task find update files and forward to update
        TaskRunner<List<WebFile>> updateTask = (TaskRunner<List<WebFile>>) _workspacePane.getTaskManager().createTaskForName("Check for updates");
        updateTask.setTaskFunction(() -> _versionControl.getUpdateFilesForLocalFiles(localFiles, updateTask.getMonitor()));
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

        // Create task runner for update, configure callbacks and start
        TaskRunner<Boolean> updateRunner = new TaskRunner<>("Update files from remote site");
        updateRunner.setTaskFunction(() -> _versionControl.updateFiles(updateFiles, updateRunner.getMonitor()));
        updateRunner.setOnSuccess(completed -> handleUpdateFilesSuccess(updateFiles));
        updateRunner.setOnFinished(() -> handleUpdateFilesFinished());
        updateRunner.setOnFailure(exception -> handleUpdateFilesFailed(exception));
        updateRunner.start();

        // Show progress dialog
        updateRunner.getMonitor().showProgressPanel(_workspacePane.getUI());
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

        // Create task runner for replace files, configure callbacks and start
        TaskRunner<Boolean> replaceRunner = new TaskRunner<>("Replace files from remote site");
        replaceRunner.setTaskFunction(() -> _versionControl.replaceFiles(replaceFiles, replaceRunner.getMonitor()));
        replaceRunner.setOnSuccess(obj -> handleReplaceFilesSuccess(replaceFiles));
        replaceRunner.setOnFinished(() -> handleReplaceFilesFinished());
        replaceRunner.setOnFailure(exception -> handleReplaceFilesFailed(exception));
        replaceRunner.start();

        // Show progress dialog
        replaceRunner.getMonitor().showProgressPanel(_workspacePane.getUI());
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

        // Create task runner for commit, configure callbacks and start
        String commitMessage = transferPane.getCommitMessage();
        TaskRunner<Boolean> commitRunner = new TaskRunner<>("Commit files to remote site");
        commitRunner.setTaskFunction(() -> _versionControl.commitFiles(commitFiles, commitMessage, commitRunner.getMonitor()));
        commitRunner.setOnFailure(exception -> handleCommitFilesFailed(exception));
        commitRunner.start();

        // Show progress dialog
        commitRunner.getMonitor().showProgressPanel(_workspacePane.getUI());
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

        // Reset UI to update some buttons enabled (commit/replace)
        resetLater();
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

        // Create task runner for save to snap cloud, configure callbacks and start
        TaskRunner<Boolean> saveToSnapCloudRunner = (TaskRunner<Boolean>) _workspacePane.getTaskManager().createTaskForName("Save to SnapCloud");
        saveToSnapCloudRunner.setTaskFunction(() -> saveToSnapCloudImpl(saveToSnapCloudRunner.getMonitor()));
        saveToSnapCloudRunner.setOnFinished(this::handleSaveToSnapCloudFinished);
        saveToSnapCloudRunner.setOnFailure(this::handleCommitFilesFailed);
        saveToSnapCloudRunner.start();
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
     * Called when saveToSnapCloud() finishes.
     */
    private void handleSaveToSnapCloudFinished()
    {
        if (isShowing()) {
            connectToRemoteSite();
            resetLater();
        }
        _autosaveFilesRun = null;
    }

    /**
     * Called to handle SnapCloud autosave.
     */
    public void autosaveFilesToSnapCloud()
    {
        if (_autosaveFilesRun == null)
            runDelayed(_autosaveFilesRun = this::saveToSnapCloud, 500);
    }
    private Runnable _autosaveFilesRun;

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
package snapcode.apptools;
import snap.props.PropChangeListener;
import snap.util.ListUtils;
import snap.view.BoxView;
import snap.web.WebURL;
import snapcode.app.SnapCloudPage;
import snapcode.project.WorkspaceBuilder;
import snap.props.PropChange;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;
import snapcode.project.VersionControl;
import snap.util.TaskMonitor;
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

        _projPane.setRemoteUrlAddress(urlAddress);

        //if (_versionControl != null)
        //    _versionControl.removePropChangeLsnr(_versionControlPropChangeLsnr);

        // Get VersionControl for project site and set listener
        WebSite projectSite = getProjectSite();
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
        WebFile rootDir = remoteSite.getRootDir();
        boolean isRemoteExists = rootDir.isVerified() && rootDir.getExists();
        if (!rootDir.isVerified())
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
            case "UpdateFilesButton": updateFiles(getSiteRootDirAsList()); break;
            case "ReplaceFilesButton": replaceFiles(getSiteRootDirAsList()); break;
            case "CommitFilesButton": commitFiles(getSiteRootDirAsList()); break;

            // Handle SnapCloudButton
            case "SnapCloudButton": handleSnapCloudButton(); break;
        }
    }

    /**
     * Create remote site.
     */
    private void createRemoteSite()
    {
        // Create TaskMonitor for create remote site
        String title = "Create remote site " + _versionControl.getRemoteSiteUrlAddress();
        TaskMonitor taskMonitor = new TaskMonitor(title);
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> _versionControl.createRemoteSite(taskMonitor));
        checkoutRunner.setMonitor(taskMonitor);

        // Configure callbacks and start
        checkoutRunner.setOnSuccess(obj -> handleCreateRemoteSuccess());
        checkoutRunner.setOnFailure(this::handleCreateRemoteFailed);
        checkoutRunner.start();
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
        // Create TaskMonitor for create clone site
        String title = "Create clone site " + _versionControl.getRemoteSiteUrlAddress();
        TaskMonitor taskMonitor = new TaskMonitor(title);
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> _versionControl.createCloneSite(taskMonitor));
        checkoutRunner.setMonitor(taskMonitor);

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
        rootDir.resetAndVerify();

        // Set root dir in remote browser
        _remoteBrowser.setSelFile(rootDir);
    }

    /**
     * Called to end VCS association with this project.
     */
    public void deactivate()
    {
        try {
            TaskMonitor taskMonitor = new TaskMonitor(System.out);
            _versionControl.disconnect(taskMonitor);
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

        // Create TaskMonitor for checkout
        String title = "Checkout from " + _versionControl.getRemoteSiteUrlAddress();
        TaskMonitor taskMonitor = new TaskMonitor(title);
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> _versionControl.checkout(taskMonitor));
        checkoutRunner.setMonitor(taskMonitor);

        // Configure callbacks and start
        checkoutRunner.setOnSuccess(obj -> checkoutSuccess());
        checkoutRunner.setOnFailure(exception -> checkoutFailed(exception));
        checkoutRunner.start();

        // Show progress dialog
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when checkout succeeds.
     */
    private void checkoutSuccess()
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
    private void checkoutFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Checkout failed", anException);
    }

    /**
     * Called to update files.
     */
    public void updateFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.isCheckedOut()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.isEmpty())
            rootFiles = getSiteRootDirAsList();

        // Get update files for root files
        List<WebFile> updateFiles = _versionControl.getUpdateFilesForLocalFiles(rootFiles);

        // Run VcsTransferPane for files and op to confirm
        if (!new VcsTransferPane().showPanel(this, updateFiles, VcsTransferPane.Op.Update))
            return;

        // Disable workspace AutoBuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Call real update files method and configure callbacks
        TaskMonitor taskMonitor = new TaskMonitor("Update files from remote site");
        TaskRunner<Boolean> updateRunner = new TaskRunner<>(() -> _versionControl.updateFiles(updateFiles, taskMonitor));
        updateRunner.setMonitor(taskMonitor);
        updateRunner.setOnSuccess(completed -> updateFilesSuccess(updateFiles));
        updateRunner.setOnFinished(() -> updateFilesFinished());
        updateRunner.setOnFailure(exception -> updateFilesFailed(exception));
        updateRunner.start();

        // Show progress dialog
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when update files succeeds.
     */
    private void updateFilesSuccess(List<WebFile> theFiles)
    {
        resetAndReloadFiles(theFiles);
    }

    /**
     * Called when update files finishes.
     */
    private void updateFilesFinished()
    {
        // Reset AutoBuildEnabled and build Project
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        if (builder.isAutoBuild())
            builder.buildWorkspaceLater();

        // Connect to remote site
        if (isUISet())
            connectToRemoteSite();
    }

    /**
     * Called when update files fails.
     */
    private void updateFilesFailed(Exception anException)
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
        TaskMonitor taskMonitor = new TaskMonitor("Replace files from remote site");
        TaskRunner<Boolean> replaceRunner = new TaskRunner<>(() -> _versionControl.replaceFiles(replaceFiles, taskMonitor));
        replaceRunner.setMonitor(taskMonitor);
        replaceRunner.setOnSuccess(obj -> replaceFilesSuccess(replaceFiles));
        replaceRunner.setOnFinished(() -> replaceFilesFinished());
        replaceRunner.setOnFailure(exception -> replaceFilesFailed(exception));
        replaceRunner.start();

        // Show progress dialog
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when replace files succeeds.
     */
    private void replaceFilesSuccess(List<WebFile> theFiles)
    {
        resetAndReloadFiles(theFiles);
    }

    /**
     * Called when replace files finishes.
     */
    private void replaceFilesFinished()
    {
        // Reset AutoBuildEnabled and build Project
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        if (builder.isAutoBuild())
            builder.buildWorkspaceLater();

        // Connect to remote site
        if (isUISet())
            connectToRemoteSite();
    }

    /**
     * Called when replace files fails.
     */
    private void replaceFilesFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Replace files failed", anException);
    }

    /**
     * Called to commit files.
     */
    public void commitFiles(List<WebFile> theFiles)
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
        if (!transferPane.showPanel(this, commitFiles, VcsTransferPane.Op.Commit))
            return;

        // Do real commit
        String commitMessage = transferPane.getCommitMessage();
        TaskMonitor taskMonitor = new TaskMonitor("Commit files to remote site");
        TaskRunner<Boolean> commitRunner = new TaskRunner<>(() -> _versionControl.commitFiles(commitFiles, commitMessage, taskMonitor));
        commitRunner.setMonitor(taskMonitor);
        commitRunner.setOnFailure(exception -> commitFilesFailed(exception));
        commitRunner.start();

        // Show progress dialog
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when commit files fails.
     */
    private void commitFilesFailed(Exception anException)
    {
        DialogBox.showExceptionDialog(_workspacePane.getUI(), "Commit files failed", anException);
    }

    /**
     * Called when file added to project.
     */
    public void handleProjectFileAdded(WebFile aFile)
    {
        _versionControl.handleProjectFileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    public void handleProjectFileRemoved(WebFile aFile)
    {
        _versionControl.handleProjectFileRemoved(aFile);
    }

    /**
     * Called when project file saved.
     */
    public void handleProjectFileSaved(WebFile aFile)
    {
        _versionControl.handleProjectFileSaved(aFile);
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
     * Called when SnapCloudButton is pressed.
     */
    private void handleSnapCloudButton()
    {
        WebURL snapCloudUserUrl = SnapCloudPage.getSnapCloudUserUrl();
        assert snapCloudUserUrl != null;
        WebURL snapCloudProjectUrl = snapCloudUserUrl.getChild(_proj.getName());
        setRemoteUrlAddress(snapCloudProjectUrl.getString());
    }

    /**
     * Resets the given files and reloads.
     */
    private void resetAndReloadFiles(List<WebFile> theFiles)
    {
        for (WebFile file : theFiles)
            file.resetAndVerify();
        for (WebFile file : theFiles)
            getBrowser().reloadFile(file);
        _workspacePane.resetLater();
    }

    /**
     * Returns the Site.RootDir as list.
     */
    private List<WebFile> getSiteRootDirAsList()
    {
        WebFile rootDir = getProjectSite().getRootDir();
        return ListUtils.of(rootDir);
    }
}
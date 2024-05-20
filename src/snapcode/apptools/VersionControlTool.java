package snapcode.apptools;
import snapcode.project.WorkspaceBuilder;
import snap.props.PropChange;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;
import snapcode.app.WorkspaceTools;
import snapcode.project.VersionControl;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.ProgressBar;
import snap.view.SpringView;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snapcode.webbrowser.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Collections;
import java.util.List;

/**
 * This ProjectTool subclass manages version control for project.
 */
public class VersionControlTool extends ProjectTool {

    // The VersionControl
    private VersionControl _versionControl;

    // The WebBrowser for remote files
    private WebBrowser _remoteBrowser;

    /**
     * Constructor.
     */
    public VersionControlTool(ProjectPane projectPane)
    {
        super(projectPane);

        // Get VersionControl for project site
        WebSite projectSite = getProjectSite();
        _versionControl = VersionControl.getVersionControlForProjectSite(projectSite);

        // Add listener to update FilesPane.FilesTree when file status changed
        _versionControl.addPropChangeListener(pc -> versionControlFileStatusChanged(pc));
    }

    /**
     * Returns the VersionControl.
     */
    public VersionControl getVC()  { return _versionControl; }

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
        SpringView pane = getView("RemoteBrowserPane", SpringView.class);
        _remoteBrowser = new WebBrowser();
        _remoteBrowser.setGrowWidth(true);
        _remoteBrowser.setGrowHeight(true);
        _remoteBrowser.setBounds(2, 2, pane.getWidth() - 4, pane.getHeight() - 4);
        pane.addChild(_remoteBrowser);

        // Makes changes to RemoteBrowser update VerConPane
        _remoteBrowser.addPropChangeListener(e -> resetLater());
    }

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Update RemoteURLText
        setViewValue("RemoteURLText", _versionControl.getRemoteSiteUrlAddress());

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
        // Handle RemoteURLText
        if (anEvent.equals("RemoteURLText"))
            _projPane.setRemoteUrlAddress(anEvent.getStringValue());

        // Handle ConnectButton
        if (anEvent.equals("ConnectButton")) {
            WebSite remoteSite = getRemoteSite();
            if (remoteSite != null)
                remoteSite.getRootDir().resetAndVerify();
            connectToRemoteSite();
        }

        // Handle UpdateFilesButton
        if (anEvent.equals("UpdateFilesButton"))
            updateFiles(getSiteRootDirAsList());

        // Handle ReplaceFilesButton
        if (anEvent.equals("ReplaceFilesButton"))
            replaceFiles(getSiteRootDirAsList());

        // Handle CommitFilesButton
        if (anEvent.equals("CommitFilesButton"))
            commitFiles(getSiteRootDirAsList());
    }

    /**
     * Connect to remote site.
     */
    public void connectToRemoteSite()
    {
        WebSite remoteSite = getRemoteSite();
        if (remoteSite != null)
            _remoteBrowser.setSelUrlForUrlString(remoteSite.getURLString() + "!/");
        else _remoteBrowser.setSelFile(null);
    }

    /**
     * Called when project is opened to activate version control for project.
     */
    public void projectDidOpen()
    {
        if (_versionControl.isAvailable())
            return;
        if (_versionControl.getRemoteSiteUrl() == null)
            return;

        String msg = "Do you want to load remote files into project directory?";
        DialogBox dialogBox = new DialogBox("Checkout Project Files");
        dialogBox.setMessage(msg);
        if (!dialogBox.showConfirmDialog(_workspacePane.getUI()))
            return;

        // Perform checkout
        checkout();
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

        // Get checkout task runner and configure
        TaskRunner<?> checkoutRunner = _versionControl.checkout();
        checkoutRunner.setOnSuccess(obj -> checkoutSuccess());

        // Show progress dialog
        TaskMonitor taskMonitor = checkoutRunner.getMonitor();
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

        // Reset AutoBuildEnabled and trigger auto build
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(true);
        builder.addAllFilesToBuild();
        builder.buildWorkspaceLater();
    }

    /**
     * Called to update files.
     */
    public void updateFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.isAvailable()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get update files for root files
        List<WebFile> updateFiles = _versionControl.getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Update);

        // Run VcsTransferPane for files and op to confirm
        if (!new VcsTransferPane().showPanel(this, updateFiles, VersionControl.Op.Update))
            return;

        // Disable workspace AutoBuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Call real update files method and configure callbacks
        TaskRunner<Boolean> updateRunner = _versionControl.updateFiles(updateFiles);
        updateRunner.setOnSuccess(completed -> updateFilesSuccess(updateFiles));
        updateRunner.setOnFinished(() -> updateFilesFinished());

        // Show progress dialog
        TaskMonitor taskMonitor = updateRunner.getMonitor();
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when update files succeeds.
     */
    private void updateFilesSuccess(List<WebFile> theFiles)
    {
        for (WebFile file : theFiles)
            file.resetAndVerify();
        for (WebFile file : theFiles)
            getBrowser().reloadFile(file);
        _workspacePane.resetLater();
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
     * Called to replace files.
     */
    public void replaceFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.isAvailable()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get replace files for root files
        List<WebFile> replaceFiles = _versionControl.getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Replace);

        // Run VcsTransferPane for files and op
        if (!new VcsTransferPane().showPanel(this, replaceFiles, VersionControl.Op.Replace))
            return;

        // Disable workspace AutoBuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Call real replace method and configure callbacks
        TaskRunner<Boolean> replaceRunner = _versionControl.replaceFiles(replaceFiles);
        replaceRunner.setOnSuccess(obj -> updateFilesSuccess(replaceFiles));
        replaceRunner.setOnFinished(() -> updateFilesFinished());

        // Show progress dialog
        TaskMonitor taskMonitor = replaceRunner.getMonitor();
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called to commit files.
     */
    public void commitFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.isAvailable()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get commit files for root files
        List<WebFile> commitFiles = _versionControl.getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Commit);

        // Run VersionControlFilesPane for files and op
        VcsTransferPane transferPane = new VcsTransferPane();
        if (!transferPane.showPanel(this, commitFiles, VersionControl.Op.Commit))
            return;

        // Do real commit
        String commitMessage = transferPane.getCommitMessage();
        TaskRunner<Boolean> commitRunner =_versionControl.commitFiles(theFiles, commitMessage);

        // Show progress dialog
        TaskMonitor taskMonitor = commitRunner.getMonitor();
        taskMonitor.showProgressPanel(_workspacePane.getUI());
    }

    /**
     * Called when file added to project.
     */
    public void fileAdded(WebFile aFile)
    {
        _versionControl.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    public void fileRemoved(WebFile aFile)
    {
        _versionControl.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    public void fileSaved(WebFile aFile)
    {
        _versionControl.fileSaved(aFile);
    }

    /**
     * Called when VersionControl changes a file status.
     */
    private void versionControlFileStatusChanged(PropChange aPC)
    {
        WebFile file = (WebFile) aPC.getSource();
        WorkspaceTools workspaceTools = getWorkspaceTools();
        FileTreeTool fileTreeTool = workspaceTools.getFileTreeTool();
        fileTreeTool.updateChangedFile(file);
    }

    /**
     * Returns the Site.RootDir as list.
     */
    private List<WebFile> getSiteRootDirAsList()
    {
        WebFile rootDir = getProjectSite().getRootDir();
        return Collections.singletonList(rootDir);
    }
}
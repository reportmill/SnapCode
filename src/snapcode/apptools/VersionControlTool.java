package snapcode.apptools;
import snap.util.TaskMonitorPanel;
import snapcode.project.WorkspaceBuilder;
import snap.props.PropChange;
import snap.view.View;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;
import snapcode.app.WorkspaceTools;
import snapcode.project.VersionControl;
import snapcode.webbrowser.ClientUtils;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.ProgressBar;
import snap.view.SpringView;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snapcode.webbrowser.WebBrowser;
import snap.web.AccessException;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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
     * Returns the repository site.
     */
    private WebSite getRepoSite()  { return _versionControl.getRepoSite(); }

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
        setViewValue("RemoteURLText", _versionControl.getRemoteURLString());

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
            _projPane.setRemoteURLString(anEvent.getStringValue());

        // Handle ConnectButton
        if (anEvent.equals("ConnectButton")) {
            if (getRepoSite() != null) getRepoSite().getRootDir().reload();
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
        if (getRepoSite() != null)
            _remoteBrowser.setURLString(getRepoSite().getURLString() + "!/");
        else _remoteBrowser.setFile(null);
    }

    /**
     * Called when project is opened to activate version control for project.
     */
    public void projectDidOpen()
    {
        if (_versionControl.getExists())
            return;
        if (_versionControl.getRemoteURL() == null)
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

        View view = _workspacePane.getUI();
        TaskRunner<?> checkoutRunner = _versionControl.checkout(view);
        checkoutRunner.setOnSuccess(obj -> checkoutSuccess());
    }

    /**
     * Called when checkout succeeds.
     */
    private void checkoutSuccess()
    {
        // Reload files
        WebSite projectSite = getProjectSite();
        projectSite.getRootDir().reload();

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
        if (!_versionControl.getExists()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get update files for root files
        List<WebFile> updateFiles = getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Update);

        // Run VcsTransferPane for files and op to confirm
        if (!new VcsTransferPane().showPanel(this, updateFiles, VersionControl.Op.Update))
            return;

        // Call real update method
        updateFilesImpl(updateFiles);
    }

    /**
     * Update files.
     */
    protected void updateFilesImpl(List<WebFile> theFiles)
    {
        // Disable workspace Autobuild
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Create and configure task runner for update and start
        View view = _workspacePane.getUI();
        String title = "Update files from remote site";
        TaskMonitor taskMonitor = new TaskMonitorPanel(view, title);
        Supplier<?> updateFunc = () -> { _versionControl.updateFiles(theFiles, taskMonitor); return null; };
        TaskRunner<?> updateRunner = new TaskRunner<>(updateFunc);
        updateRunner.setMonitor(taskMonitor);
        updateRunner.setOnSuccess(obj -> updateFilesSuccess(theFiles));
        updateRunner.setOnFinished(() -> updateFilesFinished());
        updateRunner.start();
    }

    /**
     * Called when update files succeeds.
     */
    private void updateFilesSuccess(List<WebFile> theFiles)
    {
        for (WebFile file : theFiles)
            file.reload();
        for (WebFile file : theFiles)
            getBrowser().reloadFile(file); // Refresh replaced files
        _workspacePane.resetLater(); // Reset UI
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
        if (!_versionControl.getExists()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get replace files for root files
        List<WebFile> replaceFiles = getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Replace);

        // Run VcsTransferPane for files and op
        if (!new VcsTransferPane().showPanel(this, replaceFiles, VersionControl.Op.Replace))
            return;

        // Call real replace method
        replaceFilesImpl(replaceFiles);
    }

    /**
     * Replace files.
     */
    protected void replaceFilesImpl(List<WebFile> theFiles)
    {
        // Create TaskRunner and start
        _workspace.getBuilder().setAutoBuildEnabled(false);

        // Create and configure task runner for replace and start
        View view = _workspacePane.getUI();
        String title = "Replace files from remote site";
        TaskMonitor taskMonitor = new TaskMonitorPanel(view, title);
        Supplier<?> replaceFunc = () -> { _versionControl.replaceFiles(theFiles, taskMonitor); return null; };
        TaskRunner<?> replaceRunner = new TaskRunner<>(replaceFunc);
        replaceRunner.setMonitor(taskMonitor);
        replaceRunner.setOnSuccess(obj -> updateFilesSuccess(theFiles));
        replaceRunner.setOnFinished(() -> updateFilesFinished());
        replaceRunner.start();
    }

    /**
     * Called to commit files.
     */
    public void commitFiles(List<WebFile> theFiles)
    {
        // Sanity check
        if (!_versionControl.getExists()) { beep(); return; }

        // Get root files
        List<WebFile> rootFiles = theFiles != null ? theFiles : getSelFiles();
        if (rootFiles.size() == 0)
            rootFiles = getSiteRootDirAsList();

        // Get commit files for root files
        List<WebFile> commitFiles = getChangedFilesForRootFiles(rootFiles, VersionControl.Op.Commit);

        // Run VersionControlFilesPane for files and op
        VcsTransferPane transferPane = new VcsTransferPane();
        if (!transferPane.showPanel(this, commitFiles, VersionControl.Op.Commit))
            return;

        // Do real commit
        commitFilesImpl(commitFiles, transferPane.getCommitMessage());
    }

    /**
     * Commit files.
     */
    protected void commitFilesImpl(List<WebFile> theFiles, String aMessage)
    {
        // Create TaskRunner and start
        View view = _workspacePane.getUI();
        String title = "Commit files to remote site";
        TaskMonitor taskMonitor = new TaskMonitorPanel(view, title);
        Supplier<?> commitFunc = () -> { _versionControl.commitFiles(theFiles, aMessage, taskMonitor); return null; };
        TaskRunner<?> commitRunner = new TaskRunner<>(commitFunc);
        commitRunner.setMonitor(taskMonitor);
        commitRunner.start();
    }

    /**
     * Returns the changed files for given root files and version control operation.
     */
    protected List<WebFile> getChangedFilesForRootFiles(List<WebFile> rootFiles, VersionControl.Op operation)
    {
        List<WebFile> changedFiles = new ArrayList<>();

        try {
            for (WebFile file : rootFiles) {
                switch (operation) {
                    case Update: _versionControl.findUpdateFiles(file, changedFiles); break;
                    case Replace: _versionControl.findReplaceFiles(file, changedFiles); break;
                    case Commit: _versionControl.findCommitFiles(file, changedFiles); break;
                }
            }
        }

        // Handle AccessException:
        catch (AccessException e) {
            if (ClientUtils.setAccess(e.getSite()))
                return getChangedFilesForRootFiles(rootFiles, operation);
            throw e;
        }

        // Handle Exception
        catch (Exception e) {
            DialogBox dialogBox = new DialogBox("Disconnect Error");
            dialogBox.setErrorMessage(e.toString());
            dialogBox.showMessageDialog(_workspacePane.getUI());
        }

        // Sort and return
        Collections.sort(changedFiles);
        return changedFiles;
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
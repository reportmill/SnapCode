package snapcode.apptools;
import snapcode.app.AppPane;
import snapcode.app.ProjectTool;
import snapcode.project.ProjectX;
import snapcode.project.VersionControl;
import snap.util.ClientUtils;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.ProgressBar;
import snap.view.SpringView;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snap.viewx.LoginPage;
import snap.viewx.TaskRunnerPanel;
import snap.viewx.WebBrowser;
import snap.web.AccessException;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Manages VersionControl operations in application.
 */
public class VcsPane extends ProjectTool {

    // The AppPane
    private VcsTools  _vscTools;

    // The site
    WebSite _site;

    // The VersionControl
    VersionControl _vc;

    // The WebBrowser for remote files
    WebBrowser _remoteBrowser;

    /**
     * Creates new VersionControl.
     */
    public VcsPane(VcsTools vcsTools)
    {
        super(vcsTools.getProjectPane());
        _vscTools = vcsTools;
        _site = getRootSite();
        _site.setProp(VcsPane.class.getName(), this);
        _vc = VersionControl.get(_site);

        // Add listener to update FilesPane.FilesTree when file status changed
        _vc.addPropChangeListener(pc -> {
            WebFile file = (WebFile) pc.getSource();
            FileTreeTool fileTreeTool = _projTools.getFileTreeTool();
            fileTreeTool.updateFile(file);
        });
    }

    /**
     * Sets the anAppPane.
     */
    public void setAppPane(AppPane anAP)
    {
        _projPane = anAP;
    }

    /**
     * Returns the project site.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Returns the VersionControl.
     */
    public VersionControl getVC()  { return _vc; }

    /**
     * Returns the remote site.
     */
    private WebSite getRemoteSite()  { return _vc.getRemoteSite(); }

    /**
     * Returns the repository site.
     */
    private WebSite getRepoSite()  { return _vc.getRepoSite(); }

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

        // Layout
        //pane.layoutChildren();
    }

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Update RemoteURLText
        setViewValue("RemoteURLText", _vc.getRemoteURLString());

        // Update ProgressBar
        ProgressBar pb = getView("ProgressBar", ProgressBar.class);
        boolean loading = _remoteBrowser.isLoading();
        if (loading && !pb.isVisible()) {
            pb.setVisible(true);
            pb.setProgress(-1);
        } else if (!loading && pb.isVisible()) {
            pb.setProgress(0);
            pb.setVisible(false);
        }
    }

    /**
     * Responds to changes to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle RemoteURLText
        if (anEvent.equals("RemoteURLText"))
            _vscTools.setRemoteURLString(anEvent.getStringValue());

        // Handle ConnectButton
        if (anEvent.equals("ConnectButton")) {
            if (getRepoSite() != null) getRepoSite().getRootDir().reload();
            connectToRemoteSite();
        }

        // Handle UpdateFilesButton
        if (anEvent.equals("UpdateFilesButton"))
            updateFiles(Arrays.asList(getSite().getRootDir()));

        // Handle ReplaceFilesButton
        if (anEvent.equals("ReplaceFilesButton"))
            replaceFiles(Arrays.asList(getSite().getRootDir()));

        // Handle CommitFilesButton
        if (anEvent.equals("CommitFilesButton"))
            commitFiles(Arrays.asList(getSite().getRootDir()));
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
     * Called to activate version control for project.
     */
    public void openSite()
    {
        if (_vc.getExists()) return;
        if (_vc.getRemoteURL() == null) return;

        String msg = "Do you want to load remote files into project directory?";
        DialogBox dialogBox = new DialogBox("Checkout Project Files");
        dialogBox.setMessage(msg);
        if (!dialogBox.showConfirmDialog(_projPane.getUI()))
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
            _vc.disconnect(new TaskMonitor.Text(System.out));
        }

        catch (Exception e) {
            DialogBox dialogBox = new DialogBox("Disconnect Error");
            dialogBox.setErrorMessage(e.toString());
            dialogBox.showMessageDialog(_projPane.getUI());
        }
    }

    /**
     * Load all remote files into project directory.
     */
    public void checkout()
    {
        TaskRunner<?> runner = new TaskRunnerPanel(_projPane.getUI(), "Checkout from " + _vc.getRemoteURLString()) {
            boolean _oldAutoBuildEnabled;

            public Object run() throws Exception
            {
                _oldAutoBuildEnabled = _vscTools.setAutoBuildEnabled(false);
                if (!getSite().getRootDir().getExists()) getSite().getRootDir().save(); // So refresh will work later
                _vc.checkout(this);
                return null;
            }

            public void success(Object aRes)
            {
                checkoutSuccess(_oldAutoBuildEnabled);
            }

            public void failure(Exception e)
            {
                _vscTools.setAutoBuildEnabled(_oldAutoBuildEnabled);
                if (ClientUtils.setAccess(getRemoteSite())) checkout();
                else if (new LoginPage().showPanel(_projPane.getUI(), getRemoteSite())) checkout();
                else super.failure(e);
            }
        };
        runner.start();
    }

    /**
     * Called when checkout succeeds.
     */
    protected void checkoutSuccess(boolean oldAutoBuildEnabled)
    {
        getSite().getRootDir().reload();
        ProjectX proj = ProjectX.getProjectForSite(getSite());
        if (proj != null)
            proj.readSettings();

        // Reset UI
        _projPane.resetLater();

        //
        _vscTools.setAutoBuildEnabled(oldAutoBuildEnabled);

        //
        if (oldAutoBuildEnabled)
            _vscTools.buildSite(true);
    }

    /**
     * Called to commit files.
     */
    public void commitFiles(List<WebFile> theFiles)
    {
        if (!_vc.getExists()) {
            beep();
            return;
        }

        // Get base files and all files to transfer
        List<WebFile> selFiles = theFiles != null ? theFiles : getSelFiles();
        List<WebFile> sfiles = getCommitFiles(selFiles);

        // Run VersionControlFilesPane for files and op
        VcsTransferPane transferPane = new VcsTransferPane();
        if (!transferPane.showPanel(this, sfiles, VersionControl.Op.Commit))
            return;
        commitFilesImpl(sfiles, transferPane.getCommitMessage());
    }

    /**
     * Returns the Commit files.
     */
    protected List<WebFile> getCommitFiles(List<WebFile> theFromFiles)
    {
        // Create list
        List<WebFile> fromFiles = theFromFiles != null ? theFromFiles : Arrays.asList(getSite().getRootDir());
        List<WebFile> commitFiles = new ArrayList<>();

        try {
            for (WebFile file : fromFiles)
                _vc.getCommitFiles(file, commitFiles);
        }

        catch (AccessException e) {
            if (ClientUtils.setAccess(e.getSite()))
                return getCommitFiles(theFromFiles);
            throw e;
        }

        // Sort files and return
        Collections.sort(commitFiles);
        return commitFiles;
    }

    /**
     * Commit files.
     */
    protected void commitFilesImpl(final List<WebFile> theFiles, final String aMessage)
    {
        // Create TaskRunner and start
        new TaskRunnerPanel(_projPane.getUI(), "Commit files to remote site") {
            public Object run() throws Exception
            {
                _vc.commitFiles(theFiles, aMessage, this);
                return null;
            }

            public void success(Object anObj)  { }
            public void failure(Exception e)  { super.failure(e); }
            public void finished() { }
        }.start();
    }

    /**
     * Called to update files.
     */
    public void updateFiles(List<WebFile> theFiles)
    {
        if (!_vc.getExists()) {
            beep();
            return;
        }

        // Get base files and all files to transfer
        List<WebFile> bfiles = theFiles != null ? theFiles : getSelFiles();
        List<WebFile> sfiles = getUpdateFiles(bfiles);

        // Run VersionControlFilesPane for files and op
        if (!new VcsTransferPane().showPanel(this, sfiles, VersionControl.Op.Update))
            return;
        updateFilesImpl(sfiles);
    }

    /**
     * Returns the Update files.
     */
    protected List<WebFile> getUpdateFiles(List<WebFile> theFromFiles)
    {
        // Create list
        List<WebFile> fromFiles = theFromFiles != null ? theFromFiles : Arrays.asList(getSite().getRootDir());
        List<WebFile> xfiles = new ArrayList();

        try {
            for (WebFile file : fromFiles) _vc.getUpdateFiles(file, xfiles);
        } catch (AccessException e) {
            if (ClientUtils.setAccess(e.getSite())) return getUpdateFiles(theFromFiles);
            throw e;
        } catch (Exception e) {
            DialogBox db = new DialogBox("Disconnect Error");
            db.setErrorMessage(e.toString());
            db.showMessageDialog(_projPane.getUI());
        }

        // Sort files and return
        Collections.sort(xfiles);
        return xfiles;
    }

    /**
     * Update files.
     */
    protected void updateFilesImpl(final List<WebFile> theFiles)
    {
        // Get old Autobuild
        final boolean oldAutoBuild = _vscTools.setAutoBuildEnabled(false);

        // Create TaskRunner and start
        new TaskRunnerPanel(_projPane.getUI(), "Update files from remote site") {
            public Object run() throws Exception
            {
                _vc.updateFiles(theFiles, this);
                return null;
            }

            public void success(Object anObj)
            {
                for (WebFile file : theFiles)
                    file.reload();
                for (WebFile file : theFiles)
                    getBrowser().reloadFile(file); // Refresh replaced files
                _projPane.resetLater(); // Reset UI
            }

            public void finished()
            {
                // Reset AutoBuildEnabled and build Project
                _vscTools.setAutoBuildEnabled(oldAutoBuild);
                if (oldAutoBuild)
                    _vscTools.buildSite(false);

                // Connect to remote site
                if (isUISet()) connectToRemoteSite();
            }
        }.start();
    }

    /**
     * Called to replace files.
     */
    public void replaceFiles(List<WebFile> theFiles)
    {
        if (!_vc.getExists()) {
            beep();
            return;
        }

        // Get base files and all files to transfer
        List<WebFile> bfiles = theFiles != null ? theFiles : getSelFiles();
        List<WebFile> sfiles = getReplaceFiles(bfiles);

        // Run VersionControlFilesPane for files and op
        if (!new VcsTransferPane().showPanel(this, sfiles, VersionControl.Op.Replace)) return;
        replaceFilesImpl(sfiles);
    }

    /**
     * Returns the Replace files.
     */
    protected List<WebFile> getReplaceFiles(List<WebFile> theFromFiles)
    {
        // Create list
        List<WebFile> fromFiles = theFromFiles != null ? theFromFiles : Arrays.asList(getSite().getRootDir());
        List<WebFile> replaceFiles = new ArrayList<>();

        try {
            for (WebFile file : fromFiles)
                _vc.getReplaceFiles(file, replaceFiles);
        }

        catch (AccessException e) {
            if (ClientUtils.setAccess(e.getSite()))
                return getReplaceFiles(theFromFiles);
            throw e;
        }

        // Sort files and return
        Collections.sort(replaceFiles);
        return replaceFiles;
    }

    /**
     * Replace files.
     */
    protected void replaceFilesImpl(final List<WebFile> theFiles)
    {
        // Create TaskRunner and start
        final boolean oldAutoBuild = _vscTools.setAutoBuildEnabled(false);

        new TaskRunnerPanel(_projPane.getUI(), "Replace files from remote site") {
            public Object run() throws Exception
            {
                _vc.replaceFiles(theFiles, this);
                return null;
            }

            public void success(Object anObj)
            {
                for (WebFile file : theFiles)
                    file.reload();
                for (WebFile file : theFiles)
                    getBrowser().reloadFile(file);
                _projPane.resetLater();
            }

            public void finished()
            {
                // Reset AutoBuildEnabled and build Project
                _vscTools.setAutoBuildEnabled(oldAutoBuild);
                if (oldAutoBuild)
                    _vscTools.buildSite(false);

                // Connect to remote site
                if (isUISet()) connectToRemoteSite();
            }
        }.start();
    }

    /**
     * Called when file added to project.
     */
    public void fileAdded(WebFile aFile)
    {
        _vc.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    public void fileRemoved(WebFile aFile)
    {
        _vc.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    public void fileSaved(WebFile aFile)
    {
        _vc.fileSaved(aFile);
    }

    /**
     * Returns the VersionControlPane for given project.
     */
    public synchronized static VcsPane get(WebSite aSite)
    {
        return (VcsPane) aSite.getProp(VcsPane.class.getName());
    }
}
package snapcode.apptools;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.project.VersionControl;
//import snapcode.project.VersionControlGit;

/**
 * This is project tool to manage all project VcsTool instances.
 */
public class VcsTools extends WorkspaceTool {

    // The VcsTool for main project
    private VcsPane _vcp;

    // A PropChangeListener for Site file changes
    private PropChangeListener _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    public VcsTools(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    @Override
    protected void initUI()
    {
        // Set VersionControlPane
        String urls = getRemoteURLString();
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _vcp = new VcsPane(this);

        // Should be called for all projects/sites, not just root
        WebSite rootSite = _workspacePane.getRootSite();
        rootSite.addFileChangeListener(_siteFileLsnr);
    }

    /**
     * Opens the Site. Was called when WorkspacePane.Show
     */
    public void openSite()
    {
        // Activate VersionControlPane
        if (_vcp != null)
            _vcp.openSite();
    }

    /**
     * Returns the RemoteURL string.
     */
    public String getRemoteURLString()
    {
        WebSite site = getRootSite();
        return VersionControl.getRemoteURLString(site);
    }

    /**
     * Sets the RemoteURL string.
     */
    public void setRemoteURLString(String urls)
    {
        // Deactivate Version control pane and re-open site
        _vcp.deactivate();
        WebSite site = getRootSite();
        VersionControl.setRemoteURLString(site, urls);

        // Recreate VC and set in tab
        //_vcp = VersionControl.get(_site) instanceof VersionControlGit ? new VcsPaneGit(this) : new VcsPane(this);
        _vcp = new VcsPane(this);

        // Reopen site
        _vcp.openSite();

        // Reset UI
        //_tabView.setSelIndex(-1);
        //_tabView.setSelIndex(vcIndex);
    }

    /**
     * Called when a site file changes.
     */
    private void siteFileChanged(PropChange aPC)
    {
        // Get source and property name
        WebFile file = (WebFile) aPC.getSource();
        String propName = aPC.getPropName();

        // Handle Saved property: Call fileAdded or fileSaved
        if (propName == WebFile.Saved_Prop) {
            if ((Boolean) aPC.getNewValue())
                fileAdded(file);
            else fileRemoved(file);
        }

        // Handle ModifedTime property: Call file saved
        if (propName == WebFile.ModTime_Prop && file.getExists())
            fileSaved(file);
    }

    /**
     * Called when file added to project.
     */
    private void fileAdded(WebFile aFile)
    {
        if (_vcp != null)
            _vcp.fileAdded(aFile);
    }

    /**
     * Called when file removed from project.
     */
    private void fileRemoved(WebFile aFile)
    {
        if (_vcp != null)
            _vcp.fileRemoved(aFile);
    }

    /**
     * Called when file saved in project.
     */
    private void fileSaved(WebFile aFile)
    {
        if (_vcp != null)
            _vcp.fileSaved(aFile);
    }
}

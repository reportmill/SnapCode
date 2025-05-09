package snapcode.apptools;
import snap.geom.Pos;
import snap.gfx.Image;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.project.VersionControl;
import snapcode.util.FileIcons;
import java.util.List;

/**
 * Runs panel and transfers files for a Project WebSite.
 */
public class VcsTransferPane extends ViewOwner {

    // The VersionControlTool
    private VersionControlTool _versionControlTool;

    // The list of transfer files
    private List<WebFile>  _files;

    // The VersionControl operation
    private Op  _op;

    // The commit message (if commit)
    private String  _commitMsg;

    // Constants for Synchronization operations
    public enum Op { Update, Replace, Commit }

    // Images
    private static Image AddedLocalBadge = Image.getImageForClassResource(VcsTransferPane.class, "AddedLocalBadge.png");
    private static Image RemovedLocalBadge = Image.getImageForClassResource(VcsTransferPane.class, "RemovedLocalBadge.png");
    private static Image UpdatedLocalBadge = Image.getImageForClassResource(VcsTransferPane.class, "UpdatedLocalBadge.png");
    private static Image AddedRemoteBadge = Image.getImageForClassResource(VcsTransferPane.class, "AddedRemoteBadge.png");
    private static Image RemovedRemoteBadge = Image.getImageForClassResource(VcsTransferPane.class, "RemovedRemoteBadge.png");
    private static Image UpdatedRemoteBadge = Image.getImageForClassResource(VcsTransferPane.class, "UpdatedRemoteBadge.png");

    /**
     * Constructor.
     */
    public VcsTransferPane()
    {
        super();
    }

    /**
     * Show panel.
     */
    public boolean showPanel(VersionControlTool versionControlTool, List<WebFile> theFiles, Op anOp)
    {
        _versionControlTool = versionControlTool;
        _files = theFiles;
        _op = anOp;

        // Get view to show panel in
        View parentView = versionControlTool.getWorkspacePane().getUI();

        // If no transfer files, just tell user and return
        if (_files.isEmpty()) {
            String msg = "No " + getOp() + " files to transfer.", title = "Synchronize Files";
            DialogBox dialogBox = new DialogBox(title);
            dialogBox.setWarningMessage(msg);
            dialogBox.showMessageDialog(parentView);
            return false;
        }

        // Show confirmation dialog with files to transfer
        String mode = getOp().toString();
        String[] options = new String[]{mode, "Cancel"};
        DialogBox dialogBox = new DialogBox(mode + " Files Panel");
        dialogBox.setContent(getUI());
        dialogBox.setOptions(options);
        if (dialogBox.showOptionDialog(parentView, mode) != 0)
            return false;

        // If commit, get message
        if (anOp == Op.Commit && _versionControlTool.getVC().supportsCommitMessages()) {
            _commitMsg = getViewText("CommentText");
            if (_commitMsg != null)
                _commitMsg = _commitMsg.trim();
            if (_commitMsg == null || _commitMsg.isEmpty()) {
                DialogBox dbox = new DialogBox("Commit Files Message Panel");
                dbox.setMessage("Enter Commit Message");
                dbox.showMessageDialog(parentView);
                return showPanel(versionControlTool, theFiles, anOp);
            }
        }

        // Return
        return true;
    }

    /**
     * Returns the transfer files.
     */
    public List<WebFile> getFiles()  { return _files; }

    /**
     * Returns the TransferMode.
     */
    public Op getOp()  { return _op; }

    /**
     * Returns the commit message.
     */
    public String getCommitMessage()  { return _commitMsg; }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Set FilesList CellConfigure
        ListView<WebFile> filesList = getView("FilesList", ListView.class);
        filesList.setRowHeight(22);
        filesList.setCellConfigure(this::configureFilesListCell);
        if (getOp() != Op.Commit || !_versionControlTool.getVC().supportsCommitMessages()) {
            TextView commentText = getView("CommentText", TextView.class);
            getView("SplitView", SplitView.class).removeItem(commentText);
        }
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update FilesList
        List<WebFile> files = getFiles();
        setViewItems("FilesList", files);
    }

    /**
     * Called to configure a FilesList cell.
     */
    private void configureFilesListCell(ListCell<WebFile> aCell)
    {
        WebFile file = aCell.getItem();
        if (file == null)
            return;

        aCell.setText(file.getPath());
        aCell.setGraphic(getFileGraphicView(file));
    }

    /**
     * Returns the file graphic view to use for file icon.
     */
    protected View getFileGraphicView(WebFile aFile)
    {
        VersionControl versionControl = _versionControlTool.getVC();
        WebSite remoteSite = versionControl.getRemoteSite();
        WebFile remoteFile = remoteSite.createFileForPath(aFile.getPath(), aFile.isDir());

        // Create file image view
        Image fileImage = FileIcons.getFileIconImage(aFile);
        ImageView fileImageView = new ImageView(fileImage);

        // Create badge image view
        Image badge = getFileBadge(aFile, remoteFile);
        ImageView badgeImageView = new ImageView(badge);
        badgeImageView.setLean(Pos.CENTER_RIGHT);

        // Composite file and badge view
        StackView fileGraphicView = new StackView();
        fileGraphicView.setAlign(Pos.TOP_LEFT);
        fileGraphicView.setChildren(fileImageView, badgeImageView);
        fileGraphicView.setPrefSize(16 + 6, 16);

        // Return
        return fileGraphicView;
    }

    /**
     * Returns the icon to use list item.
     */
    protected Image getFileBadge(WebFile aFile, WebFile remoteFile)
    {
        boolean isCommit = getOp() == Op.Commit;
        if (!aFile.getExists())
            return isCommit ? RemovedLocalBadge : AddedRemoteBadge;
        if (!remoteFile.getExists())
            return isCommit ? AddedLocalBadge : RemovedRemoteBadge;
        return isCommit ? UpdatedLocalBadge : UpdatedRemoteBadge;
    }
}
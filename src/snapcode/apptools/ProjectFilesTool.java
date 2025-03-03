package snapcode.apptools;
import snap.geom.HPos;
import snap.util.ListUtils;
import snap.util.SnapUtils;
import snap.gfx.Image;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.*;
import snapcode.project.Project;
import snapcode.util.DiffPage;
import snapcode.util.FileIcons;
import snapcode.webbrowser.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle visual management of project files.
 */
public class ProjectFilesTool extends WorkspaceTool {

    // The FilesTool
    private FilesTool _filesTool;

    // The file tree
    private TreeView<FileTreeFile>  _filesTree;

    // The file list
    private ListView<FileTreeFile>  _filesList;

    // The root AppFiles (for TreeView)
    protected FileTreeFile[] _rootFiles;

    // Images for files tree/list
    private static Image FILES_TREE_ICON = Image.getImageForClassResource(ProjectFilesTool.class, "FilesTree.png");
    private static Image FILES_LIST_ICON = Image.getImageForClassResource(ProjectFilesTool.class, "FilesList.png");

    /**
     * Constructor.
     */
    public ProjectFilesTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
        _filesTool = _workspaceTools.getFilesTool();
    }

    /**
     * Returns the root files.
     */
    public FileTreeFile[] getRootFiles()
    {
        // If already set, just return
        if (_rootFiles != null) return _rootFiles;

        // Create RootFiles for Workspace.Sites
        WebSite[] workspaceSites = _workspace.getSites();
        List<FileTreeFile> rootFiles = new ArrayList<>(workspaceSites.length);
        for (WebSite site : workspaceSites) {
            FileTreeFile fileTreeFile = new FileTreeFile(null, site.getRootDir());
            rootFiles.add(fileTreeFile);
        }

        // Set array and Return
        return _rootFiles = rootFiles.toArray(new FileTreeFile[0]);
    }

    /**
     * Resets root files.
     */
    public void resetRootFiles()
    {
        _rootFiles = null;
        resetLater();

        if (_filesTree != null && _filesTree.getItemsList().isEmpty())
            ViewUtils.runLater(this::showRootProject);
    }

    /**
     * Shows the root project.
     */
    private void showRootProject()
    {
        FileTreeFile[] rootFiles = getRootFiles();
        if (rootFiles.length > 0)
            _filesTree.expandItem(rootFiles[0]);
        List<FileTreeFile> filesTreeFiles = _filesTree.getItemsList();
        if (filesTreeFiles.size() > 1)
            _filesTree.expandItem(filesTreeFiles.get(1));
    }

    /**
     * Returns an AppFile for given WebFile.
     */
    private FileTreeFile getTreeFile(WebFile aFile)
    {
        // Handle null
        if (aFile == null) return null;

        // If root, search for file in RootFiles
        if (aFile.isRoot()) {
            FileTreeFile[] rootFiles = getRootFiles();
            return ArrayUtils.findMatch(rootFiles, treeFile -> treeFile.getFile() == aFile);
        }

        // Otherwise, getTreeFile for sucessive parents and search them for this file
        for (WebFile parentFile = aFile.getParent(); parentFile != null; parentFile = parentFile.getParent()) {
            FileTreeFile parentTreeFile = getTreeFile(parentFile);
            if (parentTreeFile != null) {
                for (FileTreeFile treeFile : parentTreeFile.getChildren())
                    if (aFile == treeFile.getFile())
                        return treeFile;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns the array of FileTreeFiles for a given file and its parents.
     */
    private FileTreeFile[] getTreeFileAndParentsForFile(WebFile aFile)
    {
        // Create list for changed file and parents
        List<FileTreeFile> treeFiles = new ArrayList<>();

        // Iterate up parent files and add FileTreeFile for each
        for (WebFile parentFile = aFile; parentFile != null; parentFile = parentFile.getParent()) {
            FileTreeFile parentTreeFile = getTreeFile(parentFile);
            if (parentTreeFile != null) {
                treeFiles.add(parentTreeFile);
                if (parentFile == aFile) // Clear children in case they have changed
                    parentTreeFile._children = null;
            }
        }

        // Return array
        return treeFiles.toArray(new FileTreeFile[0]);
    }

    /**
     * Called to update a file when it has changed (Modified, Exists, ModTime, BuildIssues, child Files (dir)).
     */
    public void updateChangedFile(WebFile aFile)
    {
        // If UI not set, just return
        if (_filesTree == null) return;

        // Update items in FilesTree/FilesList
        FileTreeFile[] fileTreeFiles = getTreeFileAndParentsForFile(aFile);
        _filesTree.updateItems(fileTreeFiles);
        _filesList.updateItems(fileTreeFiles);

        // Reset UI
        if (aFile.isDir())
            resetLater();
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get and configure FilesTree
        _filesTree = getView("FilesTree", TreeView.class);
        _filesTree.setResolver(new FileTreeFile.AppFileTreeResolver());
        _filesTree.setRowHeight(20);
        _filesTree.addEventFilter(this::handleTreeViewMouseEvent, MousePress, MouseRelease);
        _filesTree.addEventFilter(this::handleTreeViewDragEvent, DragEvents);
        _filesTree.addEventFilter(this::handleDragGestureEvent, DragGesture);

        // Get FilesList
        _filesList = getView("FilesList", ListView.class);
        _filesList.setRowHeight(24);
        _filesList.setCellConfigure(this::configureFilesListCell);
        _filesList.addEventFilter(this::handleTreeViewMouseEvent, MousePress, MouseRelease);
        _filesList.addEventFilter(this::handleTreeViewDragEvent, DragEvents);

        // Set TreeView items
        FileTreeFile[] rootFiles = getRootFiles();
        _filesTree.setItems(rootFiles);

        // Register for copy/paste
        addKeyActionHandler("CopyAction", "Shortcut+C");
        addKeyActionHandler("PasteAction", "Shortcut+V");

        // Register for PagePane.SelFile change to reset UI
        _pagePane.addPropChangeListener(pc -> handlePagePaneSelFileChange(), PagePane.SelFile_Prop);

        // Register for Window.Focused change
        _workspacePane.getWindow().addPropChangeListener(pc -> windowFocusChanged(), View.Focused_Prop);
    }

    /**
     * Initialize UI for showing.
     */
    @Override
    protected void initShowing()
    {
        runLater(() -> showRootProject());
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Repaint tree
        WebFile selFile = getSelFile();
        FileTreeFile selTreeFile = getTreeFile(selFile);
        FileTreeFile[] rootTreeFiles = getRootFiles();
        _filesTree.setItems(rootTreeFiles);
        _filesTree.setSelItem(selTreeFile);

        // Update FilesList
        List<WebFile> openFiles = _pagePane.getOpenFiles();
        List<FileTreeFile> treeFiles = ListUtils.map(openFiles, openFile -> getTreeFile(openFile));
        _filesList.setItemsList(treeFiles);
        _filesList.setSelItem(selTreeFile);
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle FilesTree
            case "FilesTree": case "FilesList": {
                FileTreeFile item = (FileTreeFile) anEvent.getSelItem();
                WebFile file = item != null ? item.getFile() : null;
                _workspacePane.openFile(file);
                break;
            }

            // Handle AllFilesButton
            case "AllFilesButton":
                boolean flip = !_filesTree.isVisible(), flop = !flip;
                _filesTree.setVisible(flip); _filesTree.setPickable(flip);
                _filesList.setVisible(flop); _filesList.setPickable(flop);
                getView("AllFilesButton", ButtonBase.class).setImage(flip ? FILES_TREE_ICON : FILES_LIST_ICON);
                break;

            // Handle NewFileMenuItem, NewFileButton
            case "NewFileMenuItem": case "NewFileButton": _workspaceTools.getNewFileTool().showNewFilePanel(); break;

            // Handle DownloadFileButton
            case "DownloadFileButton": _filesTool.downloadFile(); break;

            // Handle RemoveFileMenuItem, RenameFileMenuItem
            case "RemoveFileMenuItem": _filesTool.showRemoveFilePanel(); break;
            case "RenameFileMenuItem": _filesTool.renameSelFile(); break;

            // Handle DuplicateFileMenuItem, RefreshFileMenuItem
            case "DuplicateFileMenuItem": _filesTool.duplicateSelFile(); break;
            case "RefreshFileMenuItem": _filesTool.revertSelFiles(); break;

            // Handle OpenInTextEditorMenuItem
            case "OpenInTextEditorMenuItem": {
                WebFile selFile = getSelFile();
                _pagePane.showFileInTextEditor(selFile);
                break;
            }

            // Handle OpenInBrowserMenuItem
            case "OpenInBrowserMenuItem": {
                WebFile selFile = getSelFile();
                WebBrowserPane browserPane = new WebBrowserPane();
                browserPane.getBrowser().setSelUrl(selFile.getURL());
                browserPane.getWindow().setVisible(true);
                break;
            }

            // Handle ShowFileMenuItem
            case "ShowFileMenuItem": {
                WebFile selFile = getSelFile();
                _pagePane.showFileInFinder(selFile);
                break;
            }

            // Handle RunFileMenuItem, DebugFileMenuItem
            case "RunFileMenuItem": runAppForSelFile(false); break;
            case "DebugFileMenuItem": runAppForSelFile(true); break;

            // Handle UpdateFilesMenuItem
            case "UpdateFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.updateFiles(null);
                break;
            }

            // Handle ReplaceFileMenuItem
            case "ReplaceFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.replaceFiles(null);
                break;
            }

            // Handle CommitFileMenuItem
            case "CommitFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.commitFiles(null);
                break;
            }

            // Handle DiffFilesMenuItem
            case "DiffFilesMenuItem": {
                WebFile selFile = getSelFile();
                DiffPage diffPage = new DiffPage(selFile);
                _pagePane.setPageForURL(diffPage.getURL(), diffPage);
                _pagePane.setBrowserURL(diffPage.getURL());
                break;
            }

            // Handle ShowClassInfoMenuItem
            case "ShowClassInfoMenuItem": {
                WebFile selFile = getSelFile();
                Project selProj = getSelProject();
                WebFile classFile = selProj.getProjectFiles().getClassFileForJavaFile(selFile);
                if (classFile != null)
                    _pagePane.setBrowserFile(classFile);
                break;
            }

            // Handle CopyAction, PasteAction
            case "CopyAction": _filesTool.copySelFiles(); break;
            case "PasteAction": _filesTool.pasteFiles(); break;
        }
    }

    /**
     * Called when PagePane changes SelFile.
     */
    private void handlePagePaneSelFileChange()
    {
        if (_filesTree == null) return;

        // If directory was selected, show all tree items for it
        WebFile selFile = getSelFile();
        if (selFile != null && selFile.isDir()) {
            FileTreeFile treeFile = getTreeFile(selFile);
            if (treeFile != null)
                _filesTree.expandItem(treeFile);
        }

        resetLater();
    }

    /**
     * Called when TreeView or ListView gets mouse press or release.
     */
    private void handleTreeViewMouseEvent(ViewEvent anEvent)
    {
        // Handle PopupTrigger
        if (anEvent.isPopupTrigger()) {
            MenuButton menuButton = getView("MenuButton", MenuButton.class);
            MenuItem[] menuItems = menuButton.getMenuItems();
            ViewArchiver viewArchiver = new ViewArchiver();
            MenuItem[] menuItemsCopy = ArrayUtils.map(menuItems, item -> viewArchiver.copy(item), MenuItem.class);
            Menu menu = new Menu();
            menu.setMenuItems(menuItemsCopy);
            menu.setOwner(this);
            menu.showMenuAtXY(anEvent.getView(), anEvent.getX(), anEvent.getY());
        }

        // Handle MouseClick (double-click): RunSelectedFile
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
            if (getSelFile().isFile())
                runAppForSelFile(false);
        }
    }

    /**
     * Called when TreeView or ListView gets drag event.
     */
    private void handleTreeViewDragEvent(ViewEvent anEvent)
    {
        // If from this app, just return
        Clipboard clipboard = anEvent.getClipboard();
        if (clipboard.getDragSourceView() != null)
            return;

        // Accept drag
        anEvent.acceptDrag();

        // Add files
        if (anEvent.isDragDropEvent() && clipboard.hasFiles()) {

            List<ClipboardData> clipboardFiles = clipboard.getFiles();
            for (ClipboardData clipboardData : clipboardFiles)
                handleTreeViewDropFile(clipboardData);

            // Complete drop
            anEvent.dropComplete();
        }
    }

    /**
     * Called when TreeView or ListView gets drop file.
     */
    private void handleTreeViewDropFile(ClipboardData dropFile)
    {
        // If file not loaded, come back later
        if (!dropFile.isLoaded()) {
            dropFile.addLoadListener(this::handleTreeViewDropFile); return; }

        // If no projects, forward to WorkspacePaneDnD
        if (getSelSiteOrFirst() == null) {
            _workspacePane.getWorkspacePaneDnD().dropFile(dropFile);
            return;
        }

        // Get file name and bytes
        String fileName = dropFile.getName();
        byte[] fileBytes = dropFile.getBytes();

        // Add files
        _filesTool.addFileForNameAndBytes(fileName, fileBytes);
    }

    /**
     * Called to handle DragGesture event.
     */
    private void handleDragGestureEvent(ViewEvent anEvent)
    {
        // If too close to edge, reject it
        if (anEvent.getView() != _filesTree)
            return;
        if (anEvent.getX() > _filesTree.getWidth() - 6)
            return;

        // Get row index of event Y - just return if out of bounds
        int rowIndex = _filesTree.getRowIndexForY(anEvent.getY());
        if (rowIndex >= _filesTree.getRowCount())
            return;

        // Add SelFile to clipboard and start drag
        WebFile selFile = getSelFile();
        if (selFile != null && !anEvent.isConsumed()) {
            Clipboard clipboard = anEvent.getClipboard();
            clipboard.addData(selFile.getJavaFile());
            Image dragImage = FileIcons.getFileIconImage(selFile);
            clipboard.setDragImage(dragImage);
            clipboard.startDrag();
            anEvent.consume();
        }
    }
    /**
     * Called when window focus changes to check if files have been externally modified.
     */
    protected void windowFocusChanged()
    {
        boolean isWindowFocused = _workspacePane.getWindow().isFocused();

        // If window focus gained, check for external file mods
        if (isWindowFocused) {
            if (!SnapUtils.isWebVM) { // No reason to do this for WebVM ?
                for (FileTreeFile file : getRootFiles())
                    checkForExternalMods(file.getFile());
            }
        }

        // If window focus lost, save all files
        //else _workspace.saveAllFiles();
    }

    /**
     * Checks file for external updates.
     */
    protected void checkForExternalMods(WebFile aFile)
    {
        // If file has been changed since last load, reload
        if (aFile.isModifiedExternally()) {
            aFile.resetAndVerify();
            _pagePane.reloadFile(aFile);
        }

        // If file is directory, recurse
        if (aFile.isDir()) {
            String name = aFile.getName();
            if (name.equals(".git") || name.equals("bin")) return;
            for (WebFile file : aFile.getFiles())
                checkForExternalMods(file);
        }
    }

    /**
     * Called to configure FilesList cell.
     */
    private void configureFilesListCell(ListCell<FileTreeFile> aCell)
    {
        // Get item
        FileTreeFile item = aCell.getItem();
        if (item == null)
            return;

        // Configure cell
        aCell.setPadding(2, 6, 2, 4);
        aCell.setGraphic(item.getGraphic());
        aCell.setGrowWidth(true);

        CloseBox closeBox = new CloseBox();
        closeBox.setLeanX(HPos.RIGHT);
        closeBox.addEventHandler(e -> _workspacePane.closeFile(item.getFile()), View.Action);

        aCell.setGraphicAfter(closeBox);
    }

    /**
     * Runs the given file.
     */
    private void runAppForSelFile(boolean isDebug)
    {
        RunTool runTool = _workspaceTools.getRunTool();
        runTool.runAppForSelFile(isDebug);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Project"; }
}
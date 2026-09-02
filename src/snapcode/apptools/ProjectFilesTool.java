package snapcode.apptools;
import snap.geom.HPos;
import snap.util.ListUtils;
import snap.util.SnapEnv;
import snap.gfx.Image;
import snap.util.StringUtils;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.*;
import snapcode.util.DiffPage;
import snapcode.util.FileIcons;
import snapcode.webbrowser.*;
import java.util.*;

/**
 * A class to handle visual management of project files.
 */
public class ProjectFilesTool extends WorkspaceTool {

    // The display mode
    private DisplayMode _displayMode;

    // The project file system
    private ProjectFileSystem _fileSystem;

    // The FilesTool
    private FilesTool _filesTool;

    // The file tree
    private TreeView<ProjectFile> _filesTree;

    // The file list
    private ListView<ProjectFile> _filesList;

    // The root project files (for TreeView)
    protected List<ProjectFile> _rootFiles;

    // A runnable to reset file tree after delay
    private Runnable _resetFilesTreeRunnable;

    // The files that have changed since last reset files tree
    private final Set<WebFile> _changedFiles = Collections.synchronizedSet(new HashSet<>());

    // Images for files tree/list
    private static Image FILES_TREE_ICON = Image.getImageForClassResource(ProjectFilesTool.class, "FilesTree.png");
    private static Image FILES_LIST_ICON = Image.getImageForClassResource(ProjectFilesTool.class, "FilesList.png");
    private static Image FILES_HISTORY_ICON = Image.getImageForClassResource(ProjectFilesTool.class, "FilesHistory.png");

    // Constants for display type
    public enum DisplayMode { FilesTree, FilesList, History}

    // Constants for properties
    public static final String DisplayMode_Prop = "DisplayMode";

    /**
     * Constructor.
     */
    public ProjectFilesTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
        _displayMode = DisplayMode.FilesTree;
        _fileSystem = ProjectFileSystem.getDefaultProjectFileSystem();
        _filesTool = getFilesTool();
    }

    /**
     * Returns the display mode.
     */
    public Object getDisplayMode()  { return _displayMode; }

    /**
     * Sets the display mode.
     */
    public void setDisplayMode(DisplayMode displayMode)
    {
        if (displayMode == getDisplayMode()) return;

        // Set new mode
        firePropChange(DisplayMode_Prop, _displayMode, _displayMode = displayMode);

        // Update UI
        boolean historyMode = displayMode == DisplayMode.History;
        boolean treeMode = displayMode == DisplayMode.FilesTree || historyMode;
        boolean listMode = displayMode == DisplayMode.FilesList;

        // Set FilesTree, visible for display mode
        _filesTree.setVisible(treeMode);
        if (_filesTree.getParent().getParent() instanceof ScrollView)
            _filesTree.getParent().getParent().setVisible(treeMode);
        _fileSystem = historyMode ? LastModTimeFileSystem.getShared() : ProjectFileSystem.getDefaultProjectFileSystem();
        _filesTree.setItems(Collections.emptyList());
        resetRootFiles();

        // Set FilesList visible for display mode
        _filesList.setVisible(listMode);
        if (_filesList.getParent().getParent() instanceof ScrollView)
            _filesList.getParent().getParent().setVisible(listMode);

        // Reset DisplayModeButton image
        ButtonBase displayModeButton = getView("DisplayModeButton", ButtonBase.class);
        displayModeButton.setImage(getDisplayModeImage(displayMode));
        displayModeButton.setToolTip("File display mode: " + StringUtils.fromCamelCase(displayMode.toString()));
    }

    /**
     * Returns the next display mode.
     */
    private DisplayMode getNextDisplayMode()
    {
        return switch (_displayMode) {
            case FilesTree -> DisplayMode.FilesList;
            case FilesList -> DisplayMode.History;
            case History -> DisplayMode.FilesTree;
        };
    }

    /**
     * Returns the root files.
     */
    public List<ProjectFile> getRootFiles()
    {
        if (_rootFiles != null) return _rootFiles;
        List<WebSite> workspaceSites = _workspace.getProjectSites();
        return _rootFiles = ListUtils.map(workspaceSites, site -> _fileSystem.getProjectFileForRootFile(site.getRootDir()));
    }

    /**
     * Resets root files.
     */
    public void resetRootFiles()
    {
        _rootFiles = null;
        _fileSystem.resetRootFiles();
        if (_filesTree != null)
            resetFilesTreeDelayed();
    }

    /**
     * Returns a project file for given WebFile.
     */
    private ProjectFile getProjectFileForFile(WebFile aFile)
    {
        if (aFile == null || !_workspacePane.getProjectSites().contains(aFile.getSite()))
            return null;
        return _fileSystem.getProjectFileForFile(aFile);
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get and configure FilesTree
        _filesTree = getView("FilesTree", TreeView.class);
        _filesTree.setResolver(new ProjectFile.ProjectFileTreeResolver());
        _filesTree.setCellConfigure(this::configureFilesTreeCell);
        _filesTree.setRowHeight(24);
        _filesTree.addEventFilter(this::handleTreeViewMouseEvent, MousePress, MouseRelease);
        _filesTree.addEventFilter(this::handleTreeViewDragEvent, DragEvents);
        //_filesTree.addEventFilter(this::handleDragGestureEvent, DragGesture);

        // Get FilesList
        _filesList = getView("FilesList", ListView.class);
        _filesList.setRowHeight(24);
        _filesList.setCellConfigure(this::configureFilesListCell);
        _filesList.addEventFilter(this::handleTreeViewMouseEvent, MousePress, MouseRelease);
        _filesList.addEventFilter(this::handleTreeViewDragEvent, DragEvents);

        // Register for copy/paste
        addKeyActionHandler("CopyAction", "Shortcut+C");
        addKeyActionHandler("PasteAction", "Shortcut+V");

        // Register for WorkspacePane.SelFileTool.SelFile change to reset UI
        _workspacePane.getSelFileTool().addPropChangeListener(pc -> handleSelFileChange(), SelFileTool.SelFile_Prop);

        // Register for Window.Focused change
        if (!SnapEnv.isWebVM) // No reason to do this for WebVM ?
            _workspacePane.getWindow().addPropChangeListener(pc -> handleWindowFocusChange(), View.Focused_Prop);
    }

    /**
     * Initialize UI for showing.
     */
    @Override
    protected void initShowing()
    {
        resetFilesTreeDelayed();
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Reset FilesList items
        List<WebFile> openFiles = _pagePane.getOpenFiles();
        List<ProjectFile> openProjectFiles = ListUtils.map(openFiles, this::getProjectFileForFile);
        _filesList.setItems(openProjectFiles);

        // Reset selected file
        WebFile selFile = getSelFile();
        ProjectFile selProjectFile = getProjectFileForFile(selFile);
        _filesTree.setSelItem(selProjectFile);
        _filesList.setSelItem(selProjectFile);
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle FilesTree
            case "FilesTree": case "FilesList": {
                ProjectFile item = (ProjectFile) anEvent.getSelItem();
                WebFile file = item != null ? item.getFile() : null;
                _workspacePane.openFile(file);
                break;
            }

            // Handle DisplayModeButton
            case "DisplayModeButton": setDisplayMode(getNextDisplayMode()); break;

            // Handle NewFileMenuItem, NewFileButton
            case "NewFileMenuItem": case "NewFileButton": _workspacePane.getNewFileTool().showNewFilePanel(); break;

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
                browserPane.getBrowser().setSelUrl(selFile.getUrl());
                browserPane.getWindow().setVisible(true);
                break;
            }

            // Handle ShowFileMenuItem
            case "ShowFileMenuItem": {
                WebFile selFile = getSelFile();
                _pagePane.showFileInFinder(selFile);
                break;
            }

            // Handle CommitFileMenuItem
            case "CommitFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.commitFiles(null, true);
                break;
            }

            // Handle RollbackFilesMenuItem
            case "RollbackFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.replaceFiles(null);
                break;
            }

            // Handle UpdateFilesMenuItem
            case "UpdateFilesMenuItem": {
                ProjectPane projectPane = getSelProjectPane();
                VersionControlTool versionControlTool = projectPane.getVersionControlTool();
                versionControlTool.checkForUpdatesForFiles(getSelFiles(), false);
                break;
            }

            // Handle ShowDiffMenuItem
            case "ShowDiffMenuItem": {
                WebFile selFile = getSelFile();
                DiffPage diffPage = new DiffPage(selFile);
                _pagePane.setPageForURL(diffPage.getURL(), diffPage);
                _pagePane.setSelPage(diffPage);
                break;
            }

            // Handle RunFileMenuItem
            case "RunFileMenuItem": runAppForSelFile(); break;

            // Handle CopyAction, PasteAction
            case "CopyAction": _filesTool.copySelFiles(); break;
            case "PasteAction": _filesTool.pasteFiles(); break;
        }
    }

    /**
     * Resets the FilesTree when root files have changed.
     */
    private void resetFilesTree()
    {
        _resetFilesTreeRunnable = null;

        // Get changed project files and clear their children
        List<ProjectFile> changedProjectFiles = Collections.emptyList();
        if (!_changedFiles.isEmpty()) {
            synchronized (_changedFiles) {
                changedProjectFiles = ListUtils.mapNonNull(_changedFiles, this::getProjectFileForFile);
                _changedFiles.clear();
            }
            changedProjectFiles.stream().filter(ProjectFile::isDir).forEach(projFile -> projFile._childFiles = null);
        }

        // Get whether files tree has yet to load
        boolean firstTreeLoad = _filesTree.getItems().isEmpty();

        // Reset FilesTree items
        List<ProjectFile> rootFiles = getRootFiles();
        _filesTree.setItems(rootFiles);

        // If LastModTime file system, show all
        if (_fileSystem instanceof LastModTimeFileSystem)
            _filesTree.expandAll();

        // Show first project root dir
        else if (firstTreeLoad) {
            if (!rootFiles.isEmpty())
                _filesTree.expandItem(rootFiles.get(0));
            List<ProjectFile> filesTreeFiles = _filesTree.getItems();
            if (filesTreeFiles.size() > 1)
                _filesTree.expandItem(filesTreeFiles.get(1));
        }

        // Make files tree update project files
        if (!changedProjectFiles.isEmpty()) {
            if (_filesTree.isShowing())
                changedProjectFiles.forEach(_filesTree::updateItem);
            else if (_filesList.isShowing())
                changedProjectFiles.forEach(_filesList::updateItem);
        }

        // Reset
        resetLater();
    }

    /**
     * Resets the file tree after delay.
     */
    private void resetFilesTreeDelayed()
    {
        if (_resetFilesTreeRunnable == null)
            runDelayed(_resetFilesTreeRunnable = this::resetFilesTree, 200);
    }

    /**
     * Called when WorkspacePane.SelFileTool.SelFile changes.
     */
    private void handleSelFileChange()
    {
        if (_filesTree == null) return;

        // If directory was selected, show all tree items for it
        WebFile selFile = getSelFile();
        if (selFile != null && selFile.isDir()) {
            ProjectFile projectFile = getProjectFileForFile(selFile);
            if (projectFile != null)
                _filesTree.expandItem(projectFile);
        }

        resetLater();
    }

    /**
     * Called when workspace file has changed to update files tree.
     * Changes include: Modified, Exists, ModTime, BuildIssues, child Files (dir), version control status.
     */
    public void handleFileChange(WebFile aFile)
    {
        // If UI not set, just return
        if (_filesTree == null) return;

        // If file already registered, just return
        if (_changedFiles.contains(aFile))
            return;

        // Add file and file parent and call reset files tree delayed
        _changedFiles.add(aFile);
        WebFile parentFile = aFile.getParent();
        if (parentFile != null)
            handleFileChange(parentFile);
        resetFilesTreeDelayed();
    }

    /**
     * Called when TreeView or ListView gets mouse press or release.
     */
    private void handleTreeViewMouseEvent(ViewEvent anEvent)
    {
        // Handle PopupTrigger
        if (anEvent.isPopupTrigger()) {
            MenuButton menuButton = getView("MenuButton", MenuButton.class);
            List<MenuItem> menuItems = menuButton.getMenuItems();
            ViewArchiver viewArchiver = new ViewArchiver();
            List<MenuItem> menuItemsCopy = ListUtils.map(menuItems, viewArchiver::copyPropObject);
            Menu menu = new Menu();
            menu.setMenuItems(menuItemsCopy);
            menu.setController(this);
            menu.showMenuAtXY(anEvent.getView(), anEvent.getX(), anEvent.getY());
        }

        // Handle MouseClick (double-click): RunSelectedFile
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
            WebFile selFile = getSelFile();
            if (selFile != null && selFile.isFile())
                runAppForSelFile();
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
    private void handleWindowFocusChange()
    {
        if (_workspacePane.getWindow().isFocused())
            getRootFiles().stream().map(ProjectFile::getFile).forEach(this::checkForExternalMods);
    }

    /**
     * Checks file for external updates.
     */
    private void checkForExternalMods(WebFile aFile)
    {
        // If file has been changed since last load, reload
        if (aFile.isModifiedExternally())
            _filesTool.revertFile(aFile);

        // If file is directory, recurse
        if (aFile.isDir()) {
            String name = aFile.getName();
            if (name.equals(".git") || name.equals("bin")) return;
            for (WebFile file : aFile.getFiles())
                checkForExternalMods(file);
        }
    }

    /**
     * Called to configure FilesTree cell.
     */
    private void configureFilesTreeCell(ListCell<ProjectFile> aCell)
    {
        // Get item
        ProjectFile item = aCell.getItem();
        if (item == null)
            return;

        // Set cell text/graphic
        aCell.setText(item.getText());
        aCell.setGraphic(item.getGraphic());
    }

    /**
     * Called to configure FilesList cell.
     */
    private void configureFilesListCell(ListCell<ProjectFile> aCell)
    {
        // Get item
        ProjectFile item = aCell.getItem();
        if (item == null)
            return;

        // Set cell text/graphic
        aCell.setText(item.getText());
        aCell.setGraphic(item.getGraphic());
        aCell.setPadding(2, 6, 2, 4);
        aCell.setGrowWidth(true);

        // Add close box
        CloseBox closeBox = new CloseBox();
        closeBox.setLeanX(HPos.RIGHT);
        closeBox.addEventHandler(e -> _workspacePane.closeFile(item.getFile()), View.Action);
        aCell.setTailGraphic(closeBox);
    }

    /**
     * Runs the given file.
     */
    private void runAppForSelFile()  { _workspaceTools.getRunTool().runAppForSelFile(false); }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Project"; }

    /**
     * Returns an image for given display mode.
     */
    private static Image getDisplayModeImage(DisplayMode displayMode)
    {
        return switch (displayMode) {
            case FilesTree -> FILES_TREE_ICON;
            case FilesList -> FILES_LIST_ICON;
            default -> FILES_HISTORY_ICON;
        };
    }
}
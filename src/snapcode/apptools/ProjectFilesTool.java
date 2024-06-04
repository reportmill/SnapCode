package snapcode.apptools;
import snap.gfx.GFXEnv;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.*;
import snapcode.project.Project;
import snapcode.util.DiffPage;
import snapcode.util.FileIcons;
import snapcode.webbrowser.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle visual management of project files.
 */
public class ProjectFilesTool extends WorkspaceTool {

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
     * Shows the given file.
     */
    public void showFile(WebFile aFile)
    {
        if (_filesTree != null)
            showDir(aFile.getParent());
        setSelFile(aFile);
    }

    /**
     * Shows the given directory (but doesn't select it).
     */
    public void showDir(WebFile aFile)
    {
        if (_filesTree == null) return;
        FileTreeFile treeFile = getTreeFile(aFile);
        if (treeFile != null)
            _filesTree.expandItem(treeFile);
    }

    /**
     * Resets root files.
     */
    public void resetRootFiles()
    {
        _rootFiles = null;

        if (_filesTree != null && _filesTree.getItemsList().size() == 0) {
            resetLater();
            ViewUtils.runLater(this::showRootProject);
        }
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
     * Called to update a file when it has changed (Modified, Exists, ModTime, BuildIssues, child Files (dir)).
     */
    public void updateChangedFile(WebFile aFile)
    {
        // Create list for changed file and parents
        List<FileTreeFile> treeFiles = new ArrayList<>();

        // Iterate up parent files and clear parent TreeFile.Children
        for (WebFile parentFile = aFile; parentFile != null; parentFile = parentFile.getParent()) {

            // Get FileTreeFile for parent file and add to list
            FileTreeFile parentTreeFile = getTreeFile(parentFile);
            if (parentTreeFile != null) {
                treeFiles.add(parentTreeFile);
                if (parentFile == aFile)
                    parentTreeFile._children = null;
            }
        }

        // Update items in FilesTree/FilesList
        if (_filesTree != null) {
            FileTreeFile[] fileTreeFiles = treeFiles.toArray(new FileTreeFile[0]);
            _filesTree.updateItems(fileTreeFiles);
            _filesList.updateItems(fileTreeFiles);
        }

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
        _filesList.setAltPaint(Color.WHITE);
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
        _pagePane.addPropChangeListener(pc -> resetLater(), PagePane.SelFile_Prop);

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
        WebFile[] openFiles = _pagePane.getOpenFiles();
        FileTreeFile[] treeFiles = ArrayUtils.map(openFiles, openFile -> getTreeFile(openFile), FileTreeFile.class);
        _filesList.setItems(treeFiles);
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
                setSelFile(file);
                break;
            }

            // Handle AllFilesButton
            case "AllFilesButton":
                boolean flip = !_filesTree.isVisible(), flop = !flip;
                _filesTree.setVisible(flip);
                _filesTree.setPickable(flip);
                _filesList.setVisible(flop);
                _filesList.setPickable(flop);
                getView("AllFilesButton", ButtonBase.class).setImage(flip ? FILES_TREE_ICON : FILES_LIST_ICON);
                break;

            // Handle NewFileMenuItem, NewFileButton
            case "NewFileMenuItem": case "NewFileButton": {
                NewFileTool newFileTool = _workspaceTools.getNewFileTool();
                newFileTool.showNewFilePanel();
                break;
            }

            // Handle DownloadFileButton
            case "DownloadFileButton": {
                downloadFile();
                break;
            }

            // Handle RemoveFileMenuItem
            case "RemoveFileMenuItem": {
                FilesTool filesTool = _workspaceTools.getFilesTool();
                filesTool.showRemoveFilePanel();
                break;
            }

            // Handle RenameFileMenuItem
            case "RenameFileMenuItem": renameFile(); break;

            // Handle DuplicateFileMenuItem
            case "DuplicateFileMenuItem":
                copy();
                paste();
                break;

            // Handle RefreshFileMenuItem
            case "RefreshFileMenuItem":
                for (WebFile file : getSelFiles())
                    file.resetAndVerify();
                break;

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
            case "CopyAction": copy(); break;
            case "PasteAction": paste(); break;
        }
    }

    /**
     * Called when TreeView or ListView gets mouse press or release.
     */
    private void handleTreeViewMouseEvent(ViewEvent anEvent)
    {
        // Handle PopupTrigger
        if (anEvent.isPopupTrigger()) {
            MenuButton menuButton = getView("MenuButton", MenuButton.class);
            List<MenuItem> menuItems = menuButton.getItems();
            ViewArchiver viewArchiver = new ViewArchiver();
            Menu menu = new Menu();
            for (MenuItem mi : menuItems) {
                MenuItem menuItemCopy = viewArchiver.copy(mi);
                menu.addItem(menuItemCopy);
            }
            menu.setOwner(this);
            menu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
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

        // Get file name and bytes
        String fileName = dropFile.getName();
        byte[] fileBytes = dropFile.getBytes();

        // Add files
        FilesTool filesTool = _workspaceTools.getFilesTool();
        filesTool.addFileForNameAndBytes(fileName, fileBytes);
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
     * Renames currently selected file.
     */
    public void renameFile()
    {
        WebFile selFile = getSelFile();
        if (selFile == null || !ArrayUtils.containsId(_workspacePane.getSites(), selFile.getSite()))
            return;

        DialogBox dbox = new DialogBox("Rename File");
        dbox.setMessage("Enter new name for " + selFile.getName());
        String newName = dbox.showInputDialog(_workspacePane.getUI(), selFile.getName());
        if (newName != null) {
            FilesTool filesTool = _workspaceTools.getFilesTool();
            filesTool.renameFile(selFile, newName);
        }
    }

    /**
     * Handle Copy.
     */
    public void copy()
    {
        List<WebFile> selFiles = getSelFiles();
        List<File> javaFiles = new ArrayList<>();
        for (WebFile selFile : selFiles) {
            if (selFile.getJavaFile() != null)
                javaFiles.add(selFile.getJavaFile());
        }

        Clipboard clipboard = Clipboard.getCleared();
        clipboard.addData(javaFiles);
    }

    /**
     * Handle Paste.
     */
    public void paste()
    {
        Clipboard clipboard = Clipboard.get();
        if (clipboard.hasFiles()) {
            FilesTool filesTool = _workspaceTools.getFilesTool();
            List<File> files = clipboard.getJavaFiles();
            filesTool.addFiles(files);
        }
    }

    /**
     * Handle download file.
     */
    public void downloadFile()
    {
        // Get selected file - if file, just forward to download file
        WebFile selFile = getSelFile();
        if (selFile.isFile()) {
            File selFileJava = selFile.getJavaFile();
            if (selFileJava != null)
                downloadFile(selFile.getJavaFile());
            return;
        }

        // Get filename
        String filename = selFile.getName();
        if (selFile.isRoot())
            filename = selFile.getSite().getName();

        // Create zip file
        File zipDir = selFile.getJavaFile();
        File zipFile = FileUtils.getTempFile(filename + ".zip");
        try { FilesTool.zipDirectory(zipDir, zipFile); }
        catch (Exception e) { System.err.println(e.getMessage()); }

        // Download file
        downloadFile(zipFile);
        runDelayed(() -> zipFile.delete(), 1000);
    }

    /**
     * Handle download file.
     */
    public void downloadFile(File fileToDownload)
    {
        WebFile webFile = WebFile.getFileForJavaFile(fileToDownload);
        GFXEnv.getEnv().downloadFile(webFile);
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
        aCell.getStringView().setGrowWidth(true);

        CloseBox closeBox = new CloseBox();
        closeBox.addEventHandler(e -> closeFile(item.getFile()), View.Action);

        aCell.setGraphicAfter(closeBox);
    }

    /**
     * Closes the given file.
     */
    private void closeFile(WebFile buttonFile)
    {
        _pagePane.removeOpenFile(buttonFile);
        resetLater();
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
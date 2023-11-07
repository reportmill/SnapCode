package snapcode.apptools;
import snapcode.project.WorkspaceBuilder;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.*;
import snapcode.util.DiffPage;
import snapcode.webbrowser.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle visual management of project files.
 */
public class FileTreeTool extends WorkspaceTool {

    // The file tree
    private TreeView<FileTreeFile>  _filesTree;

    // The file list
    private ListView<FileTreeFile>  _filesList;

    // The root AppFiles (for TreeView)
    protected List<FileTreeFile>  _rootFiles;

    // Images for files tree/list
    private static Image FILES_TREE_ICON = Image.getImageForClassResource(FileTreeTool.class, "FilesTree.png");
    private static Image FILES_LIST_ICON = Image.getImageForClassResource(FileTreeTool.class, "FilesList.png");

    /**
     * Constructor.
     */
    public FileTreeTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the root files.
     */
    public List<FileTreeFile> getRootFiles()
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

        // Set, Return
        return _rootFiles = rootFiles;
    }

    /**
     * Resets root files.
     */
    public void resetRootFiles()  { _rootFiles = null; }

    /**
     * Returns an AppFile for given WebFile.
     */
    public FileTreeFile getTreeFile(WebFile aFile)
    {
        // Handle null
        if (aFile == null) return null;

        // If root, search for file in RootFiles
        if (aFile.isRoot()) {
            List<FileTreeFile> rootFiles = getRootFiles();
            return ListUtils.findMatch(rootFiles, treeFile -> treeFile.getFile() == aFile);
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

        // Create RootFiles for TreeView (one for each open project)
        List<FileTreeFile> rootFiles = getRootFiles();
        _filesTree.setItemsList(rootFiles);
        _filesTree.expandItem(rootFiles.get(0));
        if (_filesTree.getItemsList().size() > 1)
            _filesTree.expandItem(_filesTree.getItemsList().get(1));

        // Register for copy/paste
        addKeyActionHandler("CopyAction", "Shortcut+C");
        addKeyActionHandler("PasteAction", "Shortcut+V");

        // Register for Window.Focused change
        _workspacePane.getWindow().addPropChangeListener(pc -> windowFocusChanged(), View.Focused_Prop);
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Repaint tree
        WebFile selFile = getSelFile();
        FileTreeFile selTreeFile = getTreeFile(selFile);
        List<FileTreeFile> rootTreeFiles = getRootFiles();
        _filesTree.setItemsList(rootTreeFiles);
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
        View eventView = anEvent.getView();

        // Handle FilesTree
        if (eventView == _filesTree || eventView == _filesList) {
            FileTreeFile item = (FileTreeFile) anEvent.getSelItem();
            WebFile file = item != null ? item.getFile() : null;
            setSelFile(file);
        }

        // Handle AllFilesButton
        if (anEvent.equals("AllFilesButton")) {
            boolean flip = !_filesTree.isVisible(), flop = !flip;
            _filesTree.setVisible(flip);
            _filesTree.setPickable(flip);
            _filesList.setVisible(flop);
            _filesList.setPickable(flop);
            getView("AllFilesButton", ButtonBase.class).setImage(flip ? FILES_TREE_ICON : FILES_LIST_ICON);
        }

        // Handle NewFileMenuItem, NewFileButton
        if (anEvent.equals("NewFileMenuItem") || anEvent.equals("NewFileButton")) {
            FilesTool filesTool = _workspaceTools.getFilesTool();
            filesTool.showNewFilePanel();
        }

        // Handle RemoveFileMenuItem
        if (anEvent.equals("RemoveFileMenuItem")) {
            FilesTool filesTool = _workspaceTools.getFilesTool();
            filesTool.showRemoveFilePanel();
        }

        // Handle RenameFileMenuItem
        if (anEvent.equals("RenameFileMenuItem"))
            renameFile();

        // Handle DuplicateFileMenuItem
        if (anEvent.equals("DuplicateFileMenuItem")) {
            copy();
            paste();
        }

        // Handle RefreshFileMenuItem
        if (anEvent.equals("RefreshFileMenuItem")) {
            for (WebFile file : getSelFiles())
                file.reload();
        }

        // Handle OpenInTextEditorMenuItem
        if (anEvent.equals("OpenInTextEditorMenuItem")) {
            WebFile file = getSelFile();
            _pagePane.showFileInTextEditor(file);
        }

        // Handle OpenInBrowserMenuItem
        if (anEvent.equals("OpenInBrowserMenuItem")) {
            WebFile file = getSelFile();
            WebBrowserPane browserPane = new WebBrowserPane();
            browserPane.getBrowser().setURL(file.getURL());
            browserPane.getWindow().setVisible(true);
        }

        // Handle ShowFileMenuItem
        if (anEvent.equals("ShowFileMenuItem")) {
            WebFile file = getSelFile();
            _pagePane.showFileInFinder(file);
        }

        // Handle RunFileMenuItem, DebugFileMenuItem
        if (anEvent.equals("RunFileMenuItem"))
            runFile(getSelFile(), false);
        if (anEvent.equals("DebugFileMenuItem"))
            runFile(getSelFile(), true);

        // Handle UpdateFilesMenuItem
        if (anEvent.equals("UpdateFilesMenuItem")) {
            ProjectPane projectPane = getSelProjectPane();
            VersionControlTool versionControlTool = projectPane.getVersionControlTool();
            versionControlTool.updateFiles(null);
        }

        // Handle ReplaceFileMenuItem
        if (anEvent.equals("ReplaceFilesMenuItem")) {
            ProjectPane projectPane = getSelProjectPane();
            VersionControlTool versionControlTool = projectPane.getVersionControlTool();
            versionControlTool.replaceFiles(null);
        }

        // Handle CommitFileMenuItem
        if (anEvent.equals("CommitFilesMenuItem")) {
            ProjectPane projectPane = getSelProjectPane();
            VersionControlTool versionControlTool = projectPane.getVersionControlTool();
            versionControlTool.commitFiles(null);
        }

        // Handle DiffFilesMenuItem
        if (anEvent.equals("DiffFilesMenuItem")) {
            WebFile file = getSelFile();
            DiffPage diffPage = new DiffPage(file);
            _pagePane.setPageForURL(diffPage.getURL(), diffPage);
            _pagePane.setBrowserURL(diffPage.getURL());
        }

        // Handle CleanProjectMenuItem
        if (anEvent.equals("CleanProjectMenuItem")) {
            WorkspaceBuilder builder = _workspace.getBuilder();
            builder.cleanWorkspace();
        }

        // Handle BuildProjectMenuItem
        if (anEvent.equals("BuildProjectMenuItem")) {
            WorkspaceBuilder builder = _workspace.getBuilder();
            builder.buildWorkspaceLater(false);
        }

        // Handle ShowClassInfoMenuItem
        if (anEvent.equals("ShowClassInfoMenuItem")) {
            WebFile javaFile = getSelFile();
            String classFilePath = javaFile.getPath().replace("/src/", "/bin/").replace(".java", ".class");
            WebFile classFile = javaFile.getSite().getFileForPath(classFilePath);
            if (classFile != null)
                _pagePane.setBrowserFile(classFile);
        }

        // Handle CopyAction, PasteAction
        if (anEvent.equals("CopyAction")) copy();
        if (anEvent.equals("PasteAction")) paste();
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
                runFile(getSelFile(), false);
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

        // Accept drag and add files
        anEvent.acceptDrag();
        if (anEvent.isDragDropEvent() && clipboard.hasFiles()) {
            List<File> files = clipboard.getJavaFiles();
            if (files == null || files.size() == 0)
                return;
            FilesTool filesTool = _workspaceTools.getFilesTool();
            filesTool.addFiles(files);
            anEvent.dropComplete();
        }
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
     * Called when window focus changes to check if files have been externally modified.
     */
    protected void windowFocusChanged()
    {
        if (_workspacePane.getWindow().isFocused()) {
            for (FileTreeFile file : getRootFiles())
                checkForExternalMods(file.getFile());
        }
    }

    /**
     * Checks file for external updates.
     */
    protected void checkForExternalMods(WebFile aFile)
    {
        // If file has been changed since last load, reload
        if (aFile.isModifiedExternally()) {
            aFile.reload();
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
    private void runFile(WebFile aFile, boolean isDebug)
    {
        _workspaceTools.runFile(aFile, isDebug);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Project"; }
}
package snapcode.app;
import javakit.project.Project;
import snap.geom.Polygon;
import snap.gfx.Border;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snap.web.WebUtils;
import snapcode.apptools.DebugTool;
import snapcode.apptools.VcsPane;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle visual management of project files.
 */
public class FilesPane extends ProjectTool {

    // The AppPane
    private AppPane  _appPane;

    // The file tree
    private TreeView<AppFile>  _filesTree;

    // The file list
    private ListView<AppFile>  _filesList;

    // The root AppFiles (for TreeView)
    protected List<AppFile>  _rootFiles;

    // Images for files tree/list
    private static Image FILES_TREE_ICON = Image.get(FilesPane.class, "FilesTree.png");
    private static Image FILES_LIST_ICON = Image.get(FilesPane.class, "FilesList.png");

    /**
     * Creates a new AppPaneFilesPane.
     */
    public FilesPane(AppPane projPane)
    {
        super(projPane);
        _appPane = projPane;
    }

    /**
     * Returns the root files.
     */
    public List<AppFile> getRootFiles()
    {
        // If already set, just return
        if (_rootFiles != null) return _rootFiles;

        // Create RootFiles
        List<AppFile> rootFiles = new ArrayList<>(_projPane.getSiteCount());
        for (int i = 0, iMax = _projPane.getSiteCount(); i < iMax; i++) {
            WebSite site = _projPane.getSite(i);
            rootFiles.add(new AppFile(null, site.getRootDir()));
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
    public AppFile getAppFile(WebFile aFile)
    {
        // Handle null
        if (aFile == null) return null;

        // If root, search for file in RootFiles
        if (aFile.isRoot()) {
            for (AppFile af : getRootFiles())
                if (aFile == af.getFile())
                    return af;
            return null;
        }

        // Otherwise, getAppFile for sucessive parents and search them for this file
        for (WebFile par = aFile.getParent(); par != null; par = par.getParent()) {
            AppFile apar = getAppFile(par);
            if (apar != null) // && _filesTree.isExpanded(apar))
                for (AppFile af : apar.getChildren())
                    if (aFile == af.getFile())
                        return af;
        }

        // Return not found
        return null;
    }

    /**
     * Called to update a file in FilesPane.FilesTree.
     */
    public void updateFile(WebFile aFile)
    {
        List<AppFile> appFiles = new ArrayList<>();
        for (WebFile file = aFile; file != null; file = file.getParent()) {
            AppFile afile = getAppFile(file);
            if (afile != null) {
                appFiles.add(afile);
                if (file == aFile)
                    afile._children = null;
            }
        }

        if (_filesTree != null) {
            _filesTree.updateItems(appFiles.toArray(new AppFile[0]));
            _filesList.updateItems(appFiles.toArray(new AppFile[0]));
        }
        if (aFile.isDir())
            resetLater();
    }

    /**
     * Shows the given file in tree.
     */
    public void showInTree(WebFile aFile)
    {
        // Get AppFile and return if already visible
        AppFile afile = getAppFile(aFile);
        if (_filesTree.getItems().contains(afile))
            return;

        // Make sure parent is showing and expand item for parent
        showInTree(aFile.getParent());
        _filesTree.expandItem(afile.getParent());

        // If file is SelectedFile, make FilesTree select it
        if (aFile == getSelFile())
            _filesTree.setSelItem(afile);
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Configure RowView
        RowView rowView = getView("RowView", RowView.class);
        rowView.setBorder(Color.GRAY7, 1);

        // Get the FilesTree
        _filesTree = getView("FilesTree", TreeView.class);
        _filesTree.setResolver(new AppFile.AppFileTreeResolver());
        _filesTree.setRowHeight(20);

        // Get FilesList
        _filesList = getView("FilesList", ListView.class);
        _filesList.setRowHeight(24);
        _filesList.setAltPaint(Color.WHITE);
        _filesList.setCellConfigure(this::configureFilesListCell);
        enableEvents(_filesList, MousePress, MouseRelease);
        enableEvents(_filesList, DragEvents);

        // Create RootFiles for TreeView (one for each open project)
        _filesTree.setItems(getRootFiles());
        _filesTree.expandItem(getRootFiles().get(0));
        if (_filesTree.getItems().size() > 1) _filesTree.expandItem(_filesTree.getItems().get(1));

        // Enable events to get MouseUp on TreeView
        enableEvents(_filesTree, MousePress, MouseRelease, DragGesture);
        enableEvents(_filesTree, DragEvents);

        // Register for copy/paste
        addKeyActionHandler("CopyAction", "Shortcut+C");
        addKeyActionHandler("PasteAction", "Shortcut+V");

        // Register for Window.Focused change
        _projPane.getWindow().addPropChangeListener(pc -> windowFocusChanged(), View.Focused_Prop);
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Repaint tree
        WebFile file = getSelFile();
        AppFile afile = getAppFile(file);
        _filesTree.setItems(getRootFiles());
        _filesTree.setSelItem(afile);

        // Update FilesList
        WebFile[] openFiles = _pagePane.getOpenFiles();
        AppFile[] appFiles = ArrayUtils.map(openFiles, openFile -> getAppFile(openFile), AppFile.class);
        _filesList.setItems(appFiles);
        _filesList.setSelItem(afile);
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle OpenMenuItem
        if (anEvent.equals("OpenMenuItem"))
            _appPane.getToolBar().selectSearchText();

        // Handle QuitMenuItem
        if (anEvent.equals("QuitMenuItem"))
            WelcomePanel.getShared().quitApp();

        // Handle FilesTree
        if (anEvent.equals(_filesTree) || anEvent.equals(_filesList)) {

            // Handle PopupTrigger
            if (anEvent.isPopupTrigger()) {
                List<MenuItem> mitems = getView("MenuButton", MenuButton.class).getItems();
                List<MenuItem> mitems2 = ViewUtils.copyMenuItems(mitems);
                Menu menu = new Menu();
                for (MenuItem mi : mitems2) menu.addItem(mi);
                menu.setOwner(this);
                menu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
            }

            // Handle MouseClick (double-click): RunSelectedFile
            if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
                if (getSelFile().isFile()) {
                    DebugTool debugTool = _projTools.getDebugTool();
                    debugTool.runConfigOrFile(null, getSelFile(), false);
                }
            }

            // Handle DragEvent
            else if (anEvent.isDragEvent()) {

                // If from this app, just return
                if (anEvent.getClipboard().getDragSourceView() != null) return;

                // Accept drag and add files
                anEvent.acceptDrag();
                if (anEvent.isDragDropEvent() && anEvent.getClipboard().hasFiles()) {
                    List<File> files = anEvent.getClipboard().getJavaFiles();
                    if (files == null || files.size() == 0) return;
                    addFiles(files);
                    anEvent.dropComplete();
                }
            }

            // Handle DragGesture
            else if (anEvent.isDragGesture() && getSelFile() != null && !anEvent.isConsumed()) {
                Clipboard cb = anEvent.getClipboard();
                cb.addData(getSelFile().getJavaFile());
                cb.startDrag();
            }

            // Handle Selection event: Select file for tree selection
            else if (anEvent.isActionEvent()) { //if(anEvent.isSelectionEvent()) {
                AppFile item = (AppFile) anEvent.getSelItem();
                WebFile file = item != null ? item.getFile() : null;
                setSelFile(file);
            }
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
        if (anEvent.equals("NewFileMenuItem") || anEvent.equals("NewFileButton"))
            _appPane.showNewFilePanel();

        // Handle RemoveFileMenuItem
        if (anEvent.equals("RemoveFileMenuItem"))
            showRemoveFilePanel();

        // Handle RenameFileMenuItem
        if (anEvent.equals("RenameFileMenuItem"))
            renameFile();

        // Handle DuplicateFileMenuItem
        if (anEvent.equals("DuplicateFileMenuItem")) {
            copy();
            paste();
        }

        // Handle RefreshFileMenuItem
        if (anEvent.equals("RefreshFileMenuItem"))
            for (WebFile file : getSelFiles())
                file.reload();

        // Handle OpenInTextEditorMenuItem
        if (anEvent.equals("OpenInTextEditorMenuItem")) {
            WebFile file = getSelFile();
            WebURL url = file.getURL();
            WebPage page = new TextPage();
            page.setURL(url);
            _pagePane.setPageForURL(page.getURL(), page);
            _pagePane.setBrowserURL(url);
        }

        // Handle OpenInBrowserMenuItem
        if (anEvent.equals("OpenInBrowserMenuItem")) {
            WebFile file = getSelFile();
            WebBrowserPane bpane = new WebBrowserPane();
            bpane.getBrowser().setURL(file.getURL());
            bpane.getWindow().setVisible(true);
        }

        // Handle ShowFileMenuItem
        if (anEvent.equals("ShowFileMenuItem")) {
            WebFile file = getSelFile();
            if (!file.isDir()) file = file.getParent();
            File file2 = file.getJavaFile();
            FileUtils.openFile(file2);
        }

        // Handle RunFileMenuItem, DebugFileMenuItem
        if (anEvent.equals("RunFileMenuItem")) {
            DebugTool debugTool = _projTools.getDebugTool();
            debugTool.runConfigOrFile(null, getSelFile(), false);
        }
        if (anEvent.equals("DebugFileMenuItem")) {
            DebugTool debugTool = _projTools.getDebugTool();
            debugTool.runConfigOrFile(null, getSelFile(), true);
        }

        // Handle UpdateFilesMenuItem
        if (anEvent.equals("UpdateFilesMenuItem"))
            VcsPane.get(getSelSite()).updateFiles(null);

        // Handle ReplaceFileMenuItem
        if (anEvent.equals("ReplaceFilesMenuItem"))
            VcsPane.get(getSelSite()).replaceFiles(null);

        // Handle CommitFileMenuItem
        if (anEvent.equals("CommitFilesMenuItem"))
            VcsPane.get(getSelSite()).commitFiles(null);

        // Handle DiffFilesMenuItem
        if (anEvent.equals("DiffFilesMenuItem")) {
            WebFile file = getSelFile();
            DiffPage diffPage = new DiffPage(file);
            _pagePane.setPageForURL(diffPage.getURL(), diffPage);
            _pagePane.setBrowserURL(diffPage.getURL());
        }

        // Handle CleanProjectMenuItem
        if (anEvent.equals("CleanProjectMenuItem"))
            SitePane.get(getRootSite()).cleanSite();

        // Handle BuildProjectMenuItem
        if (anEvent.equals("BuildProjectMenuItem"))
            SitePane.get(getRootSite()).buildSite(false);

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
     * Adds a list of files.
     */
    boolean addFiles(List<File> theFiles)
    {
        // Get target (selected) directory
        WebSite site = getSelSite();
        WebFile selFile = getSelFile();
        if (selFile.getSite() != site)
            selFile = site.getRootDir();
        WebFile selDir = selFile.isDir() ? selFile : selFile.getParent();

        // Get SitePane and disable AutoBuild
        SitePane sitePane = SitePane.get(site);
        sitePane.setAutoBuildEnabled(false);

        // Add files (disable site build)
        boolean success = true;
        for (File file : theFiles) {
            if (!addFile(selDir, file)) {
                success = false;
                break;
            }
        }

        // Enable auto build and build
        sitePane.setAutoBuildEnabled(true);
        sitePane.buildSite(false);

        // Return files
        return success && theFiles.size() > 0;
    }

    /**
     * Adds a file.
     */
    boolean addFile(WebFile aDirectory, File aFile)
    {
        // Get site
        WebSite site = aDirectory.getSite();

        // Handle directory
        if (aFile.isDirectory()) {

            // Create new directory
            WebFile directory = site.createFileForPath(aDirectory.getDirPath() + aFile.getName(), true);
            File[] dirFiles = aFile.listFiles();
            for (File file : dirFiles)
                addFile(directory, file);
        }

        // Handle plain file
        else {

            // Get name and file
            String name = aFile.getName();
            WebFile siteFile = site.getFileForPath(aDirectory.getDirPath() + name);

            // See if IsDuplicating (there is a local file and it is the same as given file)
            File siteLocalFile = siteFile != null ? siteFile.getJavaFile() : null;
            boolean isDuplicating = SnapUtils.equals(aFile, siteLocalFile);

            // If file exists, run option panel for replace
            if (siteFile != null) {

                // If not duplicating, ask user if they want to Replace, Rename, Cancel
                String[] options = new String[]{"Replace", "Rename", "Cancel"};
                String defaultOption = "Replace";
                int option = 1;
                if (!isDuplicating) {
                    String msg = "A file named " + name + " already exists in this location.\n Do you want to proceed?";
                    DialogBox dbox = new DialogBox("Add File");
                    dbox.setWarningMessage(msg);
                    dbox.setOptions(options);
                    option = dbox.showOptionDialog(_projPane.getUI(), defaultOption);
                    if (option < 0 || options[option].equals("Cancel")) return false;
                }

                // If user wants to Rename, ask for new name
                if (options[option].equals("Rename")) {
                    if (isDuplicating) name = "Duplicate " + name;
                    DialogBox dbox = new DialogBox("Rename File");
                    dbox.setQuestionMessage("Enter new file name:");
                    name = dbox.showInputDialog(_projPane.getUI(), name);
                    if (name == null) return false;
                    name = name.replace(" ", "");
                    if (!StringUtils.endsWithIC(name, '.' + siteFile.getType())) name = name + '.' + siteFile.getType();
                    if (name.equals(aFile.getName()))
                        return addFile(aDirectory, aFile);
                }
            }

            // Get file (force this time), set bytes, save and select file
            siteFile = site.createFileForPath(aDirectory.getDirPath() + name, false);
            siteFile.setBytes(FileUtils.getBytes(aFile));
            try { siteFile.save(); }
            catch (Exception e) { throw new RuntimeException(e); }
            setSelFile(siteFile);
        }

        // Return true
        return true;
    }

    /**
     * Removes a list of files.
     */
    public void removeFiles(List<WebFile> theFiles)
    {
        // Get SitePane and disable AutoBuild
        WebFile file0 = theFiles.size() > 0 ? theFiles.get(0) : null;
        if (file0 == null) {
            beep();
            return;
        }
        SitePane sitePane = SitePane.get(file0.getSite());
        sitePane.setAutoBuildEnabled(false);

        // Add files (disable site build)
        for (WebFile file : theFiles) {
            try {
                if (file.getExists())
                    removeFile(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                beep();
            }
        }

        // Enable auto build and build
        sitePane.setAutoBuildEnabled(true);
        sitePane.buildSite(false);
    }

    /**
     * Deletes a file.
     */
    public void removeFile(WebFile aFile)
    {
        try { aFile.delete(); }
        catch (Exception e) { throw new RuntimeException(e); }
        _pagePane.removeOpenFile(aFile);
    }

    /**
     * Saves all unsaved files.
     */
    public void saveAllFiles()
    {
        saveFiles(getRootSite().getRootDir(), true);
    }

    /**
     * Saves any unsaved files in given directory.
     */
    public int saveFiles(WebFile aFile, boolean doSaveAll)
    {
        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _projPane.getBuildDir()) return doSaveAll ? 1 : 0;
            for (WebFile file : aFile.getFiles()) {
                int choice = saveFiles(file, doSaveAll);
                if (choice < 0 || choice == 2)
                    return -1;
                if (choice == 1) doSaveAll = true;
            }
        }

        // Handle file
        else if (aFile.isUpdateSet()) {
            DialogBox dbox = new DialogBox("Save Modified File");
            dbox.setMessage("File has been modified:\n" + aFile.getPath());
            dbox.setOptions("Save File", "Save All Files", "Cancel");
            int choice = doSaveAll ? 1 : dbox.showOptionDialog(_projPane.getUI(), "Save File");
            if (choice == 0 || choice == 1)
                try {
                    aFile.save();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            return choice;
        }

        return doSaveAll ? 1 : 0;
    }

    /**
     * Renames currently selected file.
     */
    public void renameFile()
    {
        WebFile selFile = getSelFile();
        if (selFile == null || !ArrayUtils.containsId(_projPane.getSites(), selFile.getSite()))
            return;

        DialogBox dbox = new DialogBox("Rename File");
        dbox.setMessage("Enter new name for " + selFile.getName());
        String newName = dbox.showInputDialog(_projPane.getUI(), selFile.getName());
        if (newName != null)
            renameFile(selFile, newName);
    }

    /**
     * Renames a file.
     */
    public boolean renameFile(WebFile aFile, String aName)
    {
        // TODO - this is totally bogus
        if (aFile.isDir() && aFile.getFileCount() > 0) {
            //File file = getLocalFile(aFile), file2 = new File(file.getParentFile(), aName); file.renameTo(file2);
            DialogBox dbox = new DialogBox("Can't rename non-empty directory");
            dbox.setErrorMessage("I know this is bogus, but app can't yet rename non-empty directory");
            dbox.showMessageDialog(_projPane.getUI()); //getAppPaneUI()
            return false;
        }

        // Get file name (if no extension provided, default to file extension) and path
        String name = aName;
        if (name.indexOf('.') < 0 && aFile.getType().length() > 0) name += "." + aFile.getType();
        String path = aFile.getParent().getDirPath() + name;

        // If file for NewPath already exists, complain
        if (aFile.getSite().getFileForPath(path) != null) {
            beep();
            return false;
        }

        // Set bytes and save
        WebFile newFile = aFile.getSite().createFileForPath(path, aFile.isDir());
        newFile.setBytes(aFile.getBytes());
        try { newFile.save(); }
        catch (Exception e) { throw new RuntimeException(e); }

        // Remove old file
        removeFile(aFile);

        // Select new file pane
        setSelFile(newFile);

        // Return true
        return true;
    }

    /**
     * Runs a panel for a new file (Java, JFX, Swing, table, etc.).
     */
    public void showNewFilePanel()
    {
        // Get new FormBuilder and configure
        FormBuilder form = new FormBuilder();
        form.setPadding(20, 5, 15, 5);
        form.addLabel("Select file type:           ").setFont(new snap.gfx.Font("Arial", 24));
        form.setSpacing(15);

        // Define options
        String[][] options = {
                {"Java Programming File", ".java"},
                {"Graphics + UI File", ".snp"},
                {"Sound File", ".wav"},
                {"Directory", ".dir"},
                {"ReportMill\u2122 Report Template", ".rpt"}};

        // Add and configure radio buttons
        for (int i = 0; i < options.length; i++) {
            String option = options[i][0];
            form.addRadioButton("EntryType", option, i == 0);
        }

        // Run dialog panel (just return if null), select type and extension
        if (!form.showPanel(_projPane.getUI(), "New Project File", DialogBox.infoImage)) return;
        String desc = form.getStringValue("EntryType");
        int index = 0;
        for (int i = 0; i < options.length; i++) if (desc.equals(options[i][0])) index = i;
        String extension = options[index][1];
        boolean isDir = extension.equals(".dir");
        if (isDir) extension = "";

        // Get source dir
        WebSite selSite = getSelSite();
        WebFile selFile = getSelFile();
        if (selFile.getSite() != selSite)
            selFile = selSite.getRootDir();
        WebFile selDir = selFile.isDir() ? selFile : selFile.getParent();
        if (extension.equals(".java") && selDir == selSite.getRootDir())
            selDir = Project.getProjectForSite(selSite).getSourceDir();

        // Get suggested "Untitled.xxx" path for AppPane.SelectedFile and extension
        String path = selDir.getDirPath() + "Untitled" + extension;

        // Create suggested file and page
        WebFile file = selSite.createFileForPath(path, isDir);
        WebPage page = _pagePane.createPageForURL(file.getURL());

        // ShowNewFilePanel and save returned file
        file = page.showNewFilePanel(_projPane.getUI(), file);
        if (file == null) return;
        try {
            file.save();
        }
        catch (Exception e) {
            _pagePane.showException(file.getURL(), e);
            return;
        }

        // Select file and show in tree
        setSelFile(file);
        showInTree(file);
    }

    /**
     * Runs the remove file panel.
     */
    public void showRemoveFilePanel()
    {
        // Get selected files - if any are root, beep and return
        List<WebFile> files = getSelFiles();
        for (WebFile file : files)
            if (file.isRoot()) {
                beep();
                return;
            }

        // Give the user one last chance to bail
        DialogBox dialogBox = new DialogBox("Remove File(s)");
        dialogBox.setQuestionMessage("Are you sure you want to remove the currently selected File(s)?");
        if (!dialogBox.showConfirmDialog(_projPane.getUI()))
            return;

        // Get top parent
        WebFile parent = files.size() > 0 ? files.get(0).getParent() : null;
        for (WebFile file : files)
            parent = WebUtils.getCommonAncestor(parent, file);

        // Remove files (check File.Exists in case previous file was a parent directory)
        removeFiles(files);

        // Update tree again
        setSelFile(parent);
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
        if (clipboard.hasFiles())
            addFiles(clipboard.getJavaFiles());
    }

    /**
     * Called when window focus changes to check if files have been externally modified.
     */
    protected void windowFocusChanged()
    {
        if (_projPane.getWindow().isFocused()) {
            for (AppFile file : getRootFiles())
                checkForExternalMods(file.getFile());
        }
    }

    /**
     * Checks file for external updates.
     */
    protected void checkForExternalMods(WebFile aFile)
    {
        // If file has been changed since last load, reload
        if (aFile.getLastModTime() < aFile.getURL().getLastModTime()) {
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
    private void configureFilesListCell(ListCell<AppFile> aCell)
    {
        AppFile item = aCell.getItem();
        if (item == null) return;
        aCell.setPadding(2, 6, 2, 4);
        aCell.setGraphic(item.getGraphic());
        aCell.setGrowWidth(true);
        aCell.getStringView().setGrowWidth(true);
        Polygon poly = new Polygon(0, 2, 2, 0, 5, 3, 8, 0, 10, 2, 7, 5, 10, 8, 8, 10, 5, 7, 2, 10, 0, 8, 3, 5);

        ShapeView sview = new ShapeView(poly);
        sview.setPrefSize(11, 11);
        sview.setFillSize(true);
        sview.setFill(Color.WHITE);
        sview.setBorder(CLOSE_BOX_BORDER1);
        sview.setProp("File", item.getFile());
        sview.addEventFilter(e -> handleBookmarkEvent(e), MouseEnter, MouseExit, MouseRelease);
        aCell.setGraphicAfter(sview);
    }

    /**
     * Called for events on bookmark close button.
     */
    private void handleBookmarkEvent(ViewEvent anEvent)
    {
        View cbox = anEvent.getView();
        if (anEvent.isMouseEnter()) {
            cbox.setFill(Color.CRIMSON);
            cbox.setBorder(CLOSE_BOX_BORDER2);
        }
        else if (anEvent.isMouseExit()) {
            cbox.setFill(Color.WHITE);
            cbox.setBorder(CLOSE_BOX_BORDER1);
        }
        else if (anEvent.isMouseRelease())
            _pagePane.removeOpenFile((WebFile) cbox.getProp("File"));
        anEvent.consume();
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Project"; }

    // Constants
    private static Border CLOSE_BOX_BORDER1 = Border.createLineBorder(Color.LIGHTGRAY, .5);
    private static Border CLOSE_BOX_BORDER2 = Border.createLineBorder(Color.BLACK, 1);
}
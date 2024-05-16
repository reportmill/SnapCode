/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.util.*;
import snapcode.apptools.NewFileTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.project.Workspace;
import snap.props.PropChange;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.*;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // The FilePanel
    private FilePanel  _filePanel;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    public static final String JAVA_FILE_EXT = "java";
    public static final String JEPL_FILE_EXT = "jepl";
    public static final String[] FILE_TYPES = { JAVA_FILE_EXT, JEPL_FILE_EXT };

    /**
     * Constructor.
     */
    protected WelcomePanel()
    {
        super();
        _shared = this;
    }

    /**
     * Returns the shared instance.
     */
    public static WelcomePanel getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new WelcomePanel();
    }

    /**
     * Shows the welcome panel.
     */
    public void showPanel()
    {
        getWindow().setVisible(true);
        resetLater();
    }

    /**
     * Hides the welcome panel.
     */
    public void hide()
    {
        // Hide window and flush prefs
        getWindow().setVisible(false);
        Prefs.getDefaultPrefs().flush();
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Add WelcomePaneAnim view
        View anim = getTopGraphic();
        getUI(ChildView.class).addChild(anim, 0);
        anim.playAnimDeep();

        // Create FilePanel
        _filePanel = createFilePanel();
        View filePanelUI = _filePanel.getUI();
        filePanelUI.setGrowHeight(true);

        // Add FilePanel.UI to ColView
        ColView topColView = (ColView) getUI();
        ColView colView2 = (ColView) topColView.getChild(1);
        colView2.addChild(filePanelUI, 1);

        // Hide ProgressBar
        getView("ProgressBar").setVisible(false);

        // Configure Window: Add WindowListener to indicate app should exit when close button clicked
        WindowView win = getWindow();
        win.setTitle("Welcome");
        enableEvents(win, WinClose);
        getView("OpenButton", Button.class).setDefaultButton(true);

        // Add drag listener to content view
        topColView.addEventHandler(e -> handleDragEvent(e), DragEvents);
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            openWorkspaceForShowSamples();

        // Handle NewButton, NewJavaReplMenu
        if (anEvent.equals("NewButton") || anEvent.equals("NewJavaReplMenu"))
            openWorkspaceForNewFileOfType("jepl");

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFileAndAddToRecentFiles();
            openWorkspaceForFile(selFile);
        }

        // Handle NewJavaClassMenu
        if (anEvent.equals("NewJavaClassMenu"))
            openWorkspaceForNewFileOfType("java");

        // Handle OpenGreenfootMenu
        if (anEvent.equals("OpenGreenfootMenu"))
            GreenImport.showGreenfootPanel(null);

        // Handle QuitButton
        if (anEvent.equals("QuitButton"))
            App.getShared().quitApp();

        // Handle WinClosing
        if (anEvent.isWinClose())
            hide();
    }

    /**
     * Opens empty workspace and triggers show samples.
     */
    protected void openWorkspaceForShowSamples()
    {
        //openWorkspaceForNewFileOfType("jepl", true);

        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();

        // Show samples
        runDelayed(() -> workspacePane.getWorkspaceTools().showSamples(), 300);
    }

    /**
     * Opens empty workspace and opens given sample name.
     */
    protected void openWorkspaceForSample(String sampleName)
    {
        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();

        // Get sample url
        String SAMPLES_ROOT = "https://reportmill.com/SnapCode/Samples/";
        String samplePath = FilePathUtils.getFilenameSimple(sampleName) + '/' + sampleName;
        WebURL sampleURL = WebURL.getURL(SAMPLES_ROOT + samplePath);
        assert (sampleURL != null);

        // Open sample URL
        WorkspacePaneUtils.openSamplesUrl(workspacePane, sampleURL);
    }

    /**
     * Creates a new file.
     */
    protected void openWorkspaceForNewFileOfType(String fileType)
    {
        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();

        // Create Java file
        NewFileTool newFileTool = workspacePane.getWorkspaceTools().getNewFileTool();
        runLater(() -> newFileTool.createFileForType(fileType));

        // Start sample button anim
        runLater(() -> workspacePane.getWorkspaceTools().startSamplesButtonAnim());
    }

    /**
     * Opens a Workspace for given Java/Jepl file or project dir/file.
     */
    protected void openWorkspaceForFile(WebFile aFile)
    {
        // If file is gfar file, open repo
        if (aFile.getType().equals("gfar")) {
            WorkspacePane workspacePane = openEmptyWorkspace();
            GreenImport.openGreenfootForArchiveFilePath(workspacePane, aFile.getPath());
            return;
        }

        // Open empty workspace pane
        WorkspacePane workspacePane = openEmptyWorkspace();
        WorkspacePaneUtils.openFile(workspacePane, aFile);
    }

    /**
     * Opens a Java string file.
     */
    protected void openJavaString(String javaString, boolean isJepl)
    {
        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();
        Workspace workspace = workspacePane.getWorkspace();
        Project tempProj = ProjectUtils.getTempProject(workspace);

        // If new source file has word 'chart', add SnapCharts runtime to tempProj
        if (isJepl && javaString.contains("chart"))
            tempProj.getBuildFile().setIncludeSnapChartsRuntime(true);

        // Show new project panel
        runLater(() -> {
            String fileType = isJepl ? "jepl" : "java";
            NewFileTool newFileTool = workspacePane.getWorkspaceTools().getNewFileTool();
            newFileTool.newJavaOrJeplFileForNameAndTypeAndString("JavaFiddle", fileType, javaString);
        });
    }

    /**
     * Opens an empty workspace pane.
     */
    protected WorkspacePane openEmptyWorkspace()
    {
        // Create workspace and workspace pane
        Workspace workspace = new Workspace();
        WorkspacePane workspacePane = new WorkspacePane(workspace);

        // Show workspace, hide WelcomePanel
        workspacePane.show();
        hide();

        // Return
        return workspacePane;
    }

    /**
     * Creates the FilePanel to be added to WelcomePanel.
     */
    private FilePanel createFilePanel()
    {
        // Add recent files
        WebSite recentFilesSite = RecentFilesSite.getShared();
        FilePanel.addDefaultSite(recentFilesSite);

        // Add DropBox
        String dropBoxEmail = DropBoxSite.getDefaultEmail();
        WebSite dropBoxSite = DropBoxSite.getSiteForEmail(dropBoxEmail);
        FilePanel.addDefaultSite(dropBoxSite);

        // Create/config FilePanel
        FilePanel filePanel = new FilePanel();
        filePanel.setFileValidator(file -> isValidOpenFile(file)); //filePanel.setTypes(EXTENSIONS);
        filePanel.setSelSite(recentFilesSite);
        filePanel.setActionHandler(e -> WelcomePanel.this.fireActionEventForObject("OpenButton", e));

        // Add PropChangeListener
        filePanel.addPropChangeListener(pc -> filePanelDidPropChange(pc));

        // Return
        return filePanel;
    }

    /**
     * Called when FilePanel does prop change.
     */
    private void filePanelDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle SelSite change:
        if (propName.equals(FilePanel.SelSite_Prop)) {
            WebSite selSite = _filePanel.getSelSite();;
            boolean minimize = !(selSite instanceof RecentFilesSite);
            setTopGraphicMinimized(minimize);
        }

        // Handle SelFile change: Update OpenButton.Enabled
        else if (propName.equals(FilePanel.SelFile_Prop)) {
            boolean isOpenFileSet = _filePanel.getSelFile() != null;
            getView("OpenButton").setEnabled(isOpenFileSet);
        }
    }

    /**
     * Called when content gets drag event.
     */
    private void handleDragEvent(ViewEvent anEvent)
    {
        // Handle drag over: Accept
        if (anEvent.isDragOver()) {
            if (isSupportedDragEvent(anEvent))
                anEvent.acceptDrag();
            return;
        }

        // Handle drop
        if (anEvent.isDragDrop()) {
            if (!isSupportedDragEvent(anEvent))
                return;
            anEvent.acceptDrag();
            Clipboard clipboard = anEvent.getClipboard();
            ClipboardData clipboardData = clipboard.getFiles().get(0);
            dropFile(clipboardData);
            anEvent.dropComplete();
        }
    }

    /**
     * Returns whether event is supported drag event.
     */
    private boolean isSupportedDragEvent(ViewEvent anEvent)
    {
        Clipboard clipboard = anEvent.getClipboard();
        if (!clipboard.hasFiles())
            return false;
        return true;
    }

    /**
     * Called to handle a file drop on top graphic.
     */
    private void dropFile(ClipboardData clipboardData)
    {
        // If clipboard data not loaded, come back when it is
        if (!clipboardData.isLoaded()) {
            clipboardData.addLoadListener(f -> dropFile(clipboardData));
            return;
        }

        // Get path and extension (set to empty string if null)
        String ext = clipboardData.getExtension();
        if (ext == null)
            return;
        ext = ext.toLowerCase();
        if (!ArrayUtils.contains(new String[] { "zip", "gfar", "java", "jepl" }, ext))
            return;

        // Handle file available: Open file
        WebURL sourceUrl = clipboardData.getSourceURL();
        if (sourceUrl != null) {
            WebFile sourceFile = sourceUrl.getFile();
            if (sourceFile != null)
                openWorkspaceForFile(sourceFile);
            return;
        }

        // Handle bytes: Copy to temp file and open
        byte[] bytes = clipboardData.getBytes();
        WebURL dropFileUrl = WebURL.getURL(FileUtils.getTempFile(clipboardData.getName()));
        assert (dropFileUrl != null);
        WebFile dropFile = dropFileUrl.createFile(false);
        dropFile.setBytes(bytes);
        dropFile.save();
        openWorkspaceForFile(dropFile);
    }

    /**
     * Load/configure top graphic WelcomePaneAnim.snp.
     */
    private View getTopGraphic()
    {
        // Unarchive WelcomePaneAnim.snp as DocView
        WebURL url = WebURL.getURL(WelcomePanel.class, "WelcomePanelAnim.snp");
        ChildView topGraphic = (ChildView) new ViewArchiver().getViewForSource(url);

        // Bake emboss into background image for performance on TopGraphicMinized
        //ImageView backImageView = (ImageView) topGraphic.getChild(0); backImageView.setEffect(null);
        //backImageView.getImage().emboss(10, 120, 90);

        // Get page and clear border/shadow
        ParentView page = (ParentView) topGraphic.getChild(2);
        page.setBorder(null);
        page.setFill(null);
        page.setEffect(null);

        // Set BuildText and JavaText
        View buildText = topGraphic.getChildForName("BuildText");
        View jvmText = topGraphic.getChildForName("JVMText");
        buildText.setText("Build: " + SnapUtils.getBuildInfo());
        jvmText.setText("JVM: " + System.getProperty("java.runtime.version"));

        // Configure TopGraphic to call setTopGraphicMinimized() on click
        topGraphic.addEventHandler(e -> setTopGraphicMinimized(!isTopGraphicMinimized()), View.MouseRelease);

        // Return
        return topGraphic;
    }

    /**
     * Returns whether top graphic is minimized.
     */
    private boolean isTopGraphicMinimized()
    {
        ChildView mainView = getUI(ChildView.class);
        View topGraphic = mainView.getChild(0);
        return topGraphic.getHeight() < 200;
    }

    /**
     * Toggles the top graphic.
     */
    private void setTopGraphicMinimized(boolean aValue)
    {
        // Just return if already set
        if (aValue == isTopGraphicMinimized()) return;

        // Get TopGraphic
        ChildView mainView = getUI(ChildView.class);
        ChildView topGraphic = (ChildView) mainView.getChild(0);

        // Show/hide views below the minimize size
        topGraphic.getChild(2).setVisible(!aValue);
        ColView topGraphicColView = (ColView) topGraphic.getChild(1);
        for (int i = 2; i < topGraphicColView.getChildCount(); i++)
            topGraphicColView.getChild(i).setVisible(!aValue);

        // Handle Minimize: Size PrefHeight down
        if (aValue)
            topGraphic.getAnimCleared(600).setPrefHeight(140);

        // Handle normal: Size PrefHeight up
        else {
            topGraphic.setClipToBounds(true);
            topGraphic.getAnimCleared(600).setPrefHeight(240);
        }

        // Start anim
        topGraphic.playAnimDeep();
    }

    /**
     * Returns whether given file can be opened by app (java, jepl, project).
     */
    protected static boolean isValidOpenFile(WebFile aFile)
    {
        if (isSourceFile(aFile))
            return true;
        if (aFile.getType().equals("zip"))
            return true;
        if (aFile.getType().equals("gfar"))
            return true;
        return isProjectFile(aFile);
    }

    /**
     * Returns whether given file is Java/Jepl.
     */
    private static boolean isSourceFile(WebFile aFile)
    {
        String fileType = aFile.getType();
        return ArrayUtils.contains(FILE_TYPES, fileType);
    }

    /**
     * Returns whether given file is Java/Jepl.
     */
    private static boolean isProjectFile(WebFile aFile)
    {
        // If BuildFile, return true
        String BUILD_FILE_NAME = "build.snapcode";
        if (aFile.getName().equals(BUILD_FILE_NAME))
            return true;

        // If is dir with BuildFile or source dir or source file, return true
        if (aFile.isDir()) {

            // If file doesn't exist, return false
            if (!aFile.getExists())
                return false;

            // If dir contains build file, src dir, or src file, return true
            WebFile[] dirFiles = aFile.getFiles();
            String[] projectFileNames = { "build.snapcode", "src" };
            for (WebFile file : dirFiles) {
                if (ArrayUtils.contains(projectFileNames, file.getName()) || isSourceFile(file))
                    return true;
            }
        }

        // Return not project file
        return false;
    }
}
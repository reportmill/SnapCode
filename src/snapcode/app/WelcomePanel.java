/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.util.*;
import snapcode.apptools.FileTreeTool;
import snapcode.apptools.FilesTool;
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

        // Java Desktop seems to take a ~second to do first URL fetch
        primeNetworkConnection();
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

        // Handle NewProjectButton
        if (anEvent.equals("NewProjectButton"))
            openWorkspaceForNewProject();

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
        runDelayed(300, () -> workspacePane.getWorkspaceTools().showSamples());
    }

    /**
     * Creates a new file.
     */
    protected void openWorkspaceForNewFileOfType(String fileType)
    {
        // Handle alt down
        if (ViewUtils.isAltDown()) {
            WebURL repoURL = WebURL.getURL("https://reportmill.com/SnapCode/Samples/Samples.zip");
            assert (repoURL != null);
            openWorkspaceForFile(repoURL.createFile(false));
            return;
        }

        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();
        Workspace workspace = workspacePane.getWorkspace();
        ProjectUtils.getTempProject(workspace);

        // Open new Jepl file
        runLater(() -> {
            FilesTool filesTool = workspacePane.getWorkspaceTools().getFilesTool();
            if (fileType.equals("jepl"))
                filesTool.newJeplFileForNameAndString("JavaFiddle", "");
            else filesTool.newJavaFileForName("JavaFiddle");
        });

        // Start sample button anim
        runLater(() -> workspacePane.getWorkspaceTools().startSamplesButtonAnim());
    }

    /**
     * Opens a Workspace for given Java/Jepl file or project dir/file.
     */
    protected void openWorkspaceForFile(WebFile aFile)
    {
        // Open empty workspace pane
        WorkspacePane workspacePane = openEmptyWorkspace();

        // If file is just a source file, open external source file
        boolean isSourceFile = ArrayUtils.contains(FILE_TYPES, aFile.getType());
        if (isSourceFile) {
            ViewUtils.runLater(() -> workspacePane.openExternalSourceFile(aFile));
            return;
        }

        // If file is zip file, open repo
        if (aFile.getType().equals("zip")) {
            WorkspacePaneUtils.addProjectForRepoURL(workspacePane, aFile.getURL());
            return;
        }

        // Open project: Get project site and add to workspace
        WebFile projectDir = aFile.isDir() ? aFile : aFile.getParent();
        WebSite projectSite = projectDir.getURL().getAsSite();
        Workspace workspace = workspacePane.getWorkspace();
        workspace.addProjectForSite(projectSite);
    }

    /**
     * Opens a new workspace for a new project.
     */
    private void openWorkspaceForNewProject()
    {
        // Open empty workspace pane
        WorkspacePane workspacePane = openEmptyWorkspace();
        workspacePane.showProjectTool();

        // Show FileTreeTool
        workspacePane.getWorkspaceTools().getLeftTray().setSelToolForClass(FileTreeTool.class);

        // Show new project panel
        runLater(() -> {
            FilesTool filesTool = workspacePane.getWorkspaceTools().getFilesTool();
            filesTool.showNewProjectPanel(getUI());
        });
    }

    /**
     * Opens a Java string file.
     */
    protected void openJavaString(String javaString, boolean isJepl)
    {
        // Open empty workspace pane with temp project
        WorkspacePane workspacePane = openEmptyWorkspace();
        Workspace workspace = workspacePane.getWorkspace();
        ProjectUtils.getTempProject(workspace);

        // Show new project panel
        runLater(() -> {
            FilesTool filesTool = workspacePane.getWorkspaceTools().getFilesTool();
            if (isJepl)
                filesTool.newJeplFileForNameAndString("JavaFiddle", javaString);
            else filesTool.newJavaFileForString(javaString);
        });
    }

    /**
     * Opens an empty workspace pane.
     */
    private WorkspacePane openEmptyWorkspace()
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
    private static boolean isValidOpenFile(WebFile aFile)
    {
        if (isSourceFile(aFile))
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
            String[] projectFileNames = { "build.snapcode", "src" };
            WebFile[] dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles) {
                if (ArrayUtils.contains(projectFileNames, file.getName()))
                    return true;
                if (isSourceFile(file))
                    return true;
            }
        }

        // Return not project file
        return false;
    }

    /**
     * Prime network connections: Java desktop seems to take a second+ to do first URL fetch.
     */
    private static void primeNetworkConnection()
    {
        if (SnapUtils.isWebVM) return;
        Thread primeThread = new Thread(() -> WebURL.getURL("https://reportmill.com/index.html").getHead());
        ViewUtils.runDelayed(() -> primeThread.start(), 1000);
    }
}
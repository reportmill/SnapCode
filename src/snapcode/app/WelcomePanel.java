/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.util.*;
import snapcode.apptools.FilesTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.project.Workspace;
import snap.props.PropChange;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.*;
import snapcode.project.WorkspaceBuilder;
import java.io.File;

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
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            newFile(true);

        // Handle NewButton, NewJavaReplMenu
        if (anEvent.equals("NewButton") || anEvent.equals("NewJavaReplMenu"))
            newFile(false);

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFileAndAddToRecentFiles();
            openWorkspaceForFile(selFile);
        }

        // Handle NewJavaClassMenu
        if (anEvent.equals("NewJavaClassMenu")) {
            String javaContents = JavaPage.getJavaContentStringForPackageAndClassName(null, "JavaFiddle");
            WebFile javaFile = ProjectUtils.getTempJavaFile("JavaFiddle", javaContents, false);
            openWorkspaceForFile(javaFile);
        }

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
     * Creates a new file.
     */
    protected void newFile(boolean showSamples)
    {
        // Handle alt down
        if (ViewUtils.isAltDown()) {
            WebURL repoURL = WebURL.getURL("https://reportmill.com/SnapCode/Samples/Samples.zip");
            openWorkspaceForRepoURL(repoURL);
            return;
        }

        // Open workspace for new Java repl file
        WebFile javaReplFile = ProjectUtils.getTempJavaFile("JavaFiddle", "", true);
        WorkspacePane workspacePane = openWorkspaceForFile(javaReplFile);

        if (showSamples)
            runDelayed(300, () -> workspacePane.getWorkspaceTools().showSamples());
        else runLater(() -> workspacePane.getWorkspaceTools().startSamplesButtonAnim());
    }

    /**
     * Opens a Workspace for given Java/Jepl file or project dir/file.
     */
    protected WorkspacePane openWorkspaceForFile(WebFile aFile)
    {
        // Get whether file is just a source file
        boolean isSourceFile = ArrayUtils.contains(FILE_TYPES, aFile.getType());

        // Create Workspace
        Workspace workspace = new Workspace();
        if (!isSourceFile)
            workspace.setUseRealCompiler(true);

        // Create WorkspacePane, set source, show
        WorkspacePane workspacePane = new WorkspacePane(workspace);

        // Handle source file
        if (isSourceFile) {
            workspacePane.openWorkspaceForSource(aFile);
        }

        // Handle Project file: Get project site and add to workspace
        else {
            WebFile projectDir = aFile.isDir() ? aFile : aFile.getParent();
            WebSite projectSite = projectDir.getURL().getAsSite();
            workspace.addProjectForSite(projectSite);
        }

        // Show workspace, hide WelcomePanel
        workspacePane.show();
        hide();

        // Return
        return workspacePane;
    }

    /**
     * Opens a workspace for repo URL.
     */
    private void openWorkspaceForRepoURL(WebURL repoURL)
    {
        // Create workspace
        Workspace workspace = new Workspace();
        workspace.setUseRealCompiler(true);

        // Add project
        TaskRunner<Boolean> checkoutRunner = workspace.addProjectForRepoURL(repoURL);

        // Create and show workspace pane
        WorkspacePane workspacePane = new WorkspacePane(workspace);
        workspacePane.show();
        hide();

        // After add project, trigger build and show files
        checkoutRunner.setOnSuccess(val -> openWorkspaceForRepoUrlFinished(workspacePane));

        // Show check progress panel
        checkoutRunner.getMonitor().showProgressPanel(workspacePane.getUI());
    }

    /**
     * Called when openWorkspaceForRepoURL finished.
     */
    private void openWorkspaceForRepoUrlFinished(WorkspacePane workspacePane)
    {
        // Build all files
        WorkspaceBuilder builder = workspacePane.getWorkspace().getBuilder();
        builder.addAllFilesToBuild();
        builder.buildWorkspaceLater();
    }

    /**
     * Opens a new workspace for a new project.
     */
    private void openWorkspaceForNewProject()
    {
        // Create workspace
        Workspace workspace = new Workspace();
        workspace.setUseRealCompiler(true);

        // Create workspace pane
        WorkspacePane workspacePane = new WorkspacePane(workspace);

        // Show new project panel (if cancelled, just return)
        FilesTool filesTool = workspacePane.getWorkspaceTools().getFilesTool();
        Project newProject = filesTool.showNewProjectPanel(getUI());
        if (newProject == null)
            return;

        // Show workspace pane
        workspacePane.show();
        hide();
    }

    /**
     * Opens a Java string file.
     */
    protected void openJavaString(String javaString, boolean isJepl)
    {
        // Create temp file dir
        File tempDir = FileUtils.getTempFile("JavaFiddleProj");
        tempDir.deleteOnExit();
        WebURL javaFiddleProjURL = WebURL.getURL(tempDir);
        WebFile javaFiddleProjDir = javaFiddleProjURL.createFile(true);
        javaFiddleProjDir.save();

        // Write to JavaFiddle file
        String javaFiddlePath = javaFiddleProjDir.getPath() + "/JavaFiddle.java";
        if (isJepl)
            javaFiddlePath = javaFiddlePath.replace(".java", ".jepl");
        WebURL javaFiddleURL = WebURL.getURL(javaFiddlePath);
        WebFile javaFiddleFile = javaFiddleURL.createFile(false);
        javaFiddleFile.setText(javaString);
        javaFiddleFile.save();

        // Open file
        openWorkspaceForFile(javaFiddleFile);
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
}
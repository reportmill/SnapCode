/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snapcode.project.Workspace;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ClassUtils;
import snap.util.Prefs;
import snap.util.SnapUtils;
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
        // Set as Shared (there should only be one instance)
        _shared = this;

        //
        JeplUtils.initJavaKitForThisApp();
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

        // Handle NewButton
        if (anEvent.equals("NewButton"))
            newFile(false);

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFileAndAddToRecentFiles();
            openWorkspaceForFile(selFile);
        }

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
        // Show jepl
        WorkspacePane workspacePane = openWorkspaceForFile(null);

        if (showSamples)
            runLaterDelayed(300, () -> workspacePane.getWorkspaceTools().showSamples());
        else runLater(() -> workspacePane.getWorkspaceTools().startSamplesButtonAnim());
    }

    /**
     * Opens a Workspace for given Java/Jepl file or project dir/file.
     */
    protected WorkspacePane openWorkspaceForFile(WebFile aFile)
    {
        // Create WorkspacePane, set source, show
        WorkspacePane workspacePane;

        // Handle source file
        boolean isSourceFile = aFile == null || ArrayUtils.contains(FILE_TYPES, aFile.getType());
        if (isSourceFile) {
            workspacePane = new WorkspacePane();
            workspacePane.setWorkspaceForJeplFileSource(aFile);
        }

        // Handle Project file
        else {

            // If desktop, swap in real WorkspacePaneX
            WorkspacePane workspacePaneX = createWorkspacePaneX();
            if (workspacePaneX != null)
                workspacePane = workspacePaneX;
            else workspacePane = new WorkspacePane();

            // Get project site and add to workspace
            WebFile projectDir = aFile.isDir() ? aFile : aFile.getParent();
            WebSite projectSite = projectDir.getURL().getAsSite();
            Workspace workspace = workspacePane.getWorkspace();
            workspace.addProjectForSite(projectSite);
        }

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

        // Get page and clear border/shadow
        ParentView page = (ParentView) topGraphic.getChild(2);
        page.setBorder(null);
        page.setFill(null);
        page.setEffect(null);

        // Set BuildText and JavaText
        View buildText = topGraphic.getChildForName("BuildText");
        View jvmText = topGraphic.getChildForName("JVMText");
        buildText.setText("Build: " + SnapUtils.getBuildInfo());
        jvmText.setText("JVM: " + (SnapUtils.isTeaVM ? "TeaVM" : System.getProperty("java.runtime.version")));

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
     * Creates a WorkspacePaneX if available.
     */
    private static WorkspacePane createWorkspacePaneX()
    {
        // Get WorkspacePaneX class name
        String workspaceXClassName = SnapUtils.isTeaVM ? null : "snapcode.app.WorkspacePaneX";
        if (workspaceXClassName == null)
            return null;

        // Create/return
        try {
            Class<? extends WorkspacePane> workspacePaneClass = (Class<? extends WorkspacePane>) Class.forName(workspaceXClassName);
            return ClassUtils.newInstance(workspacePaneClass);
        }
        catch (Exception e) {
            System.err.println("WelcomePanel.openWorkspace: Couldn't create WorkspaceX");
            return null;
        }
    }
}
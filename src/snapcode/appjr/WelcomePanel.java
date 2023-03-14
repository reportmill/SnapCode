/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import snap.util.Prefs;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.*;
import snapcode.app.AppBase;
import snapcode.app.WorkspacePane;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // The FilePanel
    private FilePanel  _filePanel;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    public static final String JAVA_FILE_EXT = "jepl";

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
        DocView anim = getAnimView();
        getUI(ChildView.class).addChild(anim, 0);
        anim.playAnimDeep();

        // Size main UI view
        getUI().setPrefHeight(580);

        // Create OpenPanel
        _filePanel = createOpenPanel();
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
        win.setResizable(false);
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

        // Handle OpenPanelButton
        //if (anEvent.equals("OpenPanelButton")) showOpenPanel();

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFile();
            openWorkspaceForJeplFileSource(selFile);
        }

        // Handle QuitButton
        if (anEvent.equals("QuitButton"))
            AppBase.getShared().quitApp();

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
        WorkspacePane workspacePane = openWorkspaceForJeplFileSource(null);

        if (showSamples)
            runLaterDelayed(300, () -> workspacePane.getWorkspaceTools().showSamples());
        else runLater(() -> workspacePane.getWorkspaceTools().startSamplesButtonAnim());
    }

    /**
     * Runs the open panel.
     */
    public void showOpenPanel()
    {
        // Get path from open panel for supported file extensions
        String[] extensions = { JAVA_FILE_EXT };
        WebFile selFile = FilePanel.showOpenFilePanel(getUI(), "Snap Java File", extensions);
        if (selFile == null)
            return;

        // Show jepl
        openWorkspaceForJeplFileSource(selFile);
    }

    /**
     * Creates the OpenPanel to be added to WelcomePanel.
     */
    private FilePanel createOpenPanel()
    {
        // Add recent files
        RecentFiles.setPrefsKey("RecentJeplDocs");
        WebSite recentFilesSite = RecentFilesSite.getShared();
        FilePanel.addDefaultSite(recentFilesSite);

        // Add DropBox
        String dropBoxEmail = DropBoxSite.getDefaultEmail();
        WebSite dropBoxSite = DropBoxSite.getSiteForEmail(dropBoxEmail);
        FilePanel.addDefaultSite(dropBoxSite);

        // Get path from open panel for supported file extensions
        String[] extensions = { JAVA_FILE_EXT };
        FilePanel filePanel = new FilePanel() {
            @Override
            protected void fireActionEvent(ViewEvent anEvent)
            {
                WelcomePanel.this.fireActionEventForObject("OpenButton", anEvent);
            }
        };

        // Config
        filePanel.setTypes(extensions);
        filePanel.setSelSite(recentFilesSite);

        // Return
        return filePanel;
    }

    /**
     * Opens a Jepl Workspace.
     */
    protected WorkspacePane openWorkspaceForJeplFileSource(Object aSource)
    {
        // Create WorkspacePane, set Jepl source, show
        WorkspacePane workspacePane = new WorkspacePane();
        workspacePane.setWorkspaceForJeplFileSource(aSource);
        workspacePane.show();

        // Hide WelcomePanel
        hide();

        // Return
        return workspacePane;
    }

    /**
     * Loads the WelcomePaneAnim.snp DocView.
     */
    private DocView getAnimView()
    {
        // Unarchive WelcomePaneAnim.snp as DocView
        WebURL url = WebURL.getURL(WelcomePanel.class, "WelcomePanelAnim.snp");
        DocView doc = (DocView) new ViewArchiver().getViewForSource(url);

        // Get page and clear border/shadow
        PageView page = doc.getPage();
        page.setBorder(null);
        page.setEffect(null);

        // Set BuildText and JavaText
        View buildText = page.getChildForName("BuildText");
        View jvmText = page.getChildForName("JVMText");
        buildText.setText("Build: " + SnapUtils.getBuildInfo());
        jvmText.setText("JVM: " + System.getProperty("java.runtime.version"));

        // Return
        return doc;
    }
}
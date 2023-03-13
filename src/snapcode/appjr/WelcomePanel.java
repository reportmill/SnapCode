/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import snap.geom.Insets;
import snap.gfx.Color;
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

    // The selected file
    private WebFile  _selFile;

    // The RecentFiles
    private WebFile[]  _recentFiles;

    // The SitesTable
    private TableView<WebFile>  _sitesTable;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    private static final String USER_EMAIL = "UserEmail";
    public static final String JAVA_FILE_EXT = "jepl";
    public static final String RECENT_FILES_ID = "RecentJeplDocs";

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
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _selFile; }

    /**
     * Sets the selected file.
     */
    public void setSelFile(WebFile aFile)
    {
        _selFile = aFile;
    }

    /**
     * Shows the welcome panel.
     */
    public void showPanel()
    {
        _recentFiles = null;
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

        // Configure SitesTable
        _sitesTable = getView("SitesTable", TableView.class);
        //_sitesTable.setRowHeight(24);
        _sitesTable.getScrollView().setFillWidth(false);
        _sitesTable.getScrollView().setBarSize(14);

        // Configure SitesTable columns
        TableCol<WebFile> nameCol = _sitesTable.getCol(0);
        nameCol.setCellPadding(new Insets(4, 8, 4, 5));
        nameCol.setCellConfigure(cell -> configureSitesTableNameColCell(cell));
        TableCol<WebFile> pathCol = _sitesTable.getCol(1);
        pathCol.setCellPadding(new Insets(4, 5, 4, 5));
        pathCol.setCellConfigure(cell -> configureSitesTablePathColCell(cell));

        // Enable SitesTable MouseReleased
        WebFile[] recentFiles = getRecentFiles();
        if (recentFiles.length > 0)
            _selFile = recentFiles[0];
        enableEvents(_sitesTable, MouseRelease);

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
     * Resets UI.
     */
    public void resetUI()
    {
        setViewEnabled("OpenButton", getSelFile() != null);
        _sitesTable.setItems(getRecentFiles());
        _sitesTable.setSelItem(getSelFile());
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            newFile(true);

        // Handle SitesTable
        if (anEvent.equals("SitesTable"))
            setSelFile((WebFile) anEvent.getSelItem());

        // Handle NewButton
        if (anEvent.equals("NewButton"))
            newFile(false);

        // Handle OpenPanelButton
        if (anEvent.equals("OpenPanelButton"))
            showOpenPanel();

        // Handle OpenButton or SitesTable double-click
        if (anEvent.equals("OpenButton") || anEvent.equals("SitesTable") && anEvent.getClickCount() > 1) {
            WebFile file = (WebFile) getViewSelItem("SitesTable");
            openWorkspaceForJeplFileSource(file);
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
        // Add recent files
        WebSite recentFilesSite = RecentFilesSite.getRecentFilesSiteForId(RECENT_FILES_ID);
        FilePanel.addDefaultSite(recentFilesSite);

        // Add DropBox
        String dropBoxEmail = DropBoxSite.getDefaultEmail();
        WebSite dropBoxSite = DropBoxSite.getSiteForEmail(dropBoxEmail);
        FilePanel.addDefaultSite(dropBoxSite);

        // Get path from open panel for supported file extensions
        String[] extensions = { JAVA_FILE_EXT };
        WebFile selFile = FilePanel.showOpenFilePanel(getUI(), "Snap Java File", extensions);
        if (selFile == null)
            return;

        // Show jepl
        openWorkspaceForJeplFileSource(selFile);
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
     * Returns the list of the recent documents as a list of strings.
     */
    public WebFile[] getRecentFiles()
    {
        // If already set, just return
        if (_recentFiles != null) return _recentFiles;

        WebFile[] recentFiles = RecentFiles.getFiles(RECENT_FILES_ID);
        return _recentFiles = recentFiles;
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

    /**
     * Called to configure a SitesTable.ListCell for Name Column.
     */
    private void configureSitesTableNameColCell(ListCell<WebFile> aCell)
    {
        WebFile file = aCell.getItem();
        if (file == null) return;
        String dirPath = file.getName();
        aCell.setText(dirPath);
    }

    /**
     * Called to configure a SitesTable.ListCell for Path Column.
     */
    private void configureSitesTablePathColCell(ListCell<WebFile> aCell)
    {
        WebFile file = aCell.getItem();
        if (file == null) return;
        String dirPath = file.getParent().getPath();
        aCell.setText(dirPath);
        aCell.setTextFill(Color.DARKGRAY);

        // Add button to clear item from recent files
        CloseBox closeBox = new CloseBox();
        closeBox.setMargin(0, 4, 0, 4);
        closeBox.addEventHandler(e -> handleCloseBoxClicked(closeBox), View.Action);
        aCell.setGraphic(closeBox);
    }

    /**
     * Called when SitesTable.ListCell close box is clicked.
     */
    private void handleCloseBoxClicked(View aView)
    {
        // Get SitesTable ListCell holding given view
        ListCell<?> listCell = aView.getParent(ListCell.class);
        if (listCell == null)
            return;

        // Get recent file for ListCell
        WebFile file = (WebFile) listCell.getItem();
        if (file == null)
            return;

        // Clear RecentFile
        String filePath = file.getURL().getString();
        RecentFiles.removePath(RECENT_FILES_ID, filePath);

        // Clear RecentFiles, SelFile and trigger reset
        _recentFiles = null;
        if (getSelFile() == file)
            setSelFile(null);
        resetLater();
    }
}
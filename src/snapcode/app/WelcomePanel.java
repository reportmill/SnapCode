package snapcode.app;
import snap.util.Prefs;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.FilePanel;
import snap.viewx.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcharts.app.DropBox;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // Whether file system is cloud
    private boolean  _isCloud;

    // The cloud email account
    private String  _email;

    // The selected file
    private WebFile  _selFile;

    // Whether welcome panel should exit on hide
    private boolean  _exit;

    // The Runnable to be called when app quits
    private Runnable  _onQuit;

    // The RecentFiles
    private WebFile[]  _recentFiles;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    private static final String FILE_SYSTEM = "FileSystem";
    private static final String FILE_SYSTEM_LOCAL = "FileSystemLocal";
    private static final String FILE_SYSTEM_CLOUD = "FileSystemCloud";
    private static final String USER_EMAIL = "UserEmail";

    /**
     * Constructor.
     */
    protected WelcomePanel()
    {
        // Get FileSystem
        String fileSys = Prefs.get().getString(FILE_SYSTEM);
        _isCloud = fileSys != null && fileSys.equals(FILE_SYSTEM_CLOUD);

        // Get Email
        _email = Prefs.get().getString(USER_EMAIL);

        // Set as Shared (there should only be one instance)
        _shared = this;
    }

    /**
     * Returns wether file system is cloud.
     */
    public boolean isCloud()  { return _isCloud; }

    /**
     * Sets whether file system is cloud.
     */
    public void setCloud(boolean aValue)
    {
        if (aValue == isCloud()) return;
        _isCloud = aValue;
        _recentFiles = null;

        // Update Prefs
        String fileSys = aValue ? FILE_SYSTEM_CLOUD : FILE_SYSTEM_LOCAL;
        Prefs.get().setValue(FILE_SYSTEM, fileSys);
    }

    /**
     * Returns the cloud email.
     */
    public String getCloudEmail()
    {
        return _email;
    }

    /**
     * Sets the cloud email.
     */
    public void setCloudEmail(String aString)
    {
        // If already set, just return
        if (Objects.equals(aString, getCloudEmail())) return;

        // Set and clear RecentFiles
        _email = aString;
        _recentFiles = null;

        // Update Prefs
        Prefs.get().setValue(USER_EMAIL, _email);
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
        getUI(); // This is bogus - if this isn't called, Window node get reset
        _recentFiles = null;
        getWindow().setVisible(true); //getTimeline().play();
        resetLater();
    }

    /**
     * Hides the welcome panel.
     */
    public void hide()
    {
        // Hide window and stop animation
        getWindow().setVisible(false); //getTimeline().stop();

        // Write current list of sites, flush prefs and mayb exit
        //writeSites();         // Write data file for open/selected sites
        Prefs.get().flush();    // Flush preferences
        if (_exit) quitApp(); // If exit requested, quit app
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()
    {
        return _selFile;
    }

    /**
     * Sets the selected file.
     */
    public void setSelFile(WebFile aFile)
    {
        _selFile = aFile;
    }

    /**
     * Returns the Runnable to be called to quit app.
     */
    public Runnable getOnQuit()
    {
        return _onQuit;
    }

    /**
     * Sets the Runnable to be called to quit app.
     */
    public void setOnQuit(Runnable aRunnable)
    {
        _onQuit = aRunnable;
    }

    /**
     * Called to quit app.
     */
    public void quitApp()
    {
        _onQuit.run();
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

        // Hide FileSystemTitleView for now
        getUI().setPrefHeight(580);

        // Configure SitesTable
        TableView<WebFile> sitesTable = getView("SitesTable", TableView.class);
        sitesTable.setRowHeight(24);
        sitesTable.getCol(0).setItemTextFunction(i -> i.getName());

        // Enable SitesTable MouseReleased
        WebFile[] recentFiles = getRecentFiles();
        if (recentFiles.length > 0)
            _selFile = recentFiles[0];
        enableEvents(sitesTable, MouseRelease);

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
        setViewItems("SitesTable", getRecentFiles());
        setViewSelItem("SitesTable", getSelFile());

        // Update file system buttons/text: LocalButton, CloudButton, EmailText
        setViewValue("LocalButton", !isCloud());
        setViewValue("CloudButton", isCloud());
        setViewValue("EmailText", getCloudEmail());
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle LocalButton, CloudButton
        if (anEvent.equals("LocalButton"))
            setCloud(false);
        if (anEvent.equals("CloudButton"))
            handleCloudButton();

        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            newFile(true);

        // Handle EmailText
        if (anEvent.equals("EmailText")) {
            String email = anEvent.getStringValue().trim().toLowerCase();
            if (email.equals("jeff@reportmill.com")) return;
            if (email.equals("jeff")) email = "jeff@reportmill.com";
            if (!email.contains("@")) return;
            setCloudEmail(email);
            setCloud(true);
        }

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
            openFile(file);
        }

        // Handle QuitButton
        if (anEvent.equals("QuitButton")) {
            _exit = true;
            hide();
        }

        // Handle WinClosing
        if (anEvent.isWinClose()) {
            _exit = true;
            hide();
        }
    }

    /**
     * Called when CloudButton selected.
     */
    private void handleCloudButton()
    {
        if (getCloudEmail() == null || getCloudEmail().length() == 0) {
            String msg = "The cloud file system needs an email to provide a unique folder for user files.\n";
            msg += "This information is not used for any other purposes. Though feel free to email\n";
            msg += "me at jeff@reportmill.com";
            String email = DialogBox.showInputDialog(getUI(), "Set Cloud Email", msg, "guest@guest");
            if (email == null || !email.contains("@")) return;
            email = email.trim().toLowerCase();
            if (email.equalsIgnoreCase("jeff@reportmill.com")) {
                DialogBox.showErrorDialog(getUI(), "Joker Alert", "Nice try.");
                return;
            }
            setCloudEmail(email);
        }
        setCloud(true);
    }

    /**
     * Creates a new file.
     */
    protected void newFile(boolean showSamples)
    {
        DocPaneX docPane = newDocPane();
        docPane.setWindowVisible(true);
        hide();

        if (showSamples)
            runLaterDelayed(300, () -> docPane.showSamples());
        else runLater(() -> docPane.startSamplesButtonAnim());
    }

    /**
     * Runs the open panel.
     */
    public void showOpenPanel()
    {
        // Have DocPane run open panel (if no doc opened, just return)
        DocPane docPane = newDocPane();
        docPane = docPane.showOpenPanel(getUI());
        if (docPane == null)
            return;

        // Make editor window visible and hide welcome panel
        docPane.setWindowVisible(true);
        hide();
    }

    /**
     * Opens selected file.
     */
    public void openFile(Object aSource)
    {
        // Have DocPane run open panel (if no doc opened, just return)
        DocPane docPane = newDocPane();
        docPane = docPane.openDocFromSource(aSource);
        if (docPane == null)
            return;

        // Make editor window visible and hide welcome panel
        docPane.setWindowVisible(true);
        hide();
    }

    /**
     * Creates the NotebookPane (as a hook, so it can be overriden).
     */
    protected DocPaneX newDocPane()
    {
        return new DocPaneX();
    }

    /**
     * Returns the list of the recent documents as a list of strings.
     */
    public WebFile[] getRecentFiles()
    {
        // If already set, just return
        if (_recentFiles != null) return _recentFiles;

        // Get DropBox
        DropBox dropBox = getDropBox();
        if (isCloud())
            FilePanel.setSiteDefault(dropBox);

        // Handle Local
        if (!isCloud()) {
            WebFile[] recentFiles = RecentFiles.getFiles(DocPane.RECENT_FILES_ID);
            return _recentFiles = recentFiles;
        }

        // Turn on progress bar
        ProgressBar progressBar = getView("ProgressBar", ProgressBar.class);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);

        // Set loading files
        new Thread(() -> setRecentFilesInBackground()).start();

        // Handle Cloud
        return _recentFiles = new WebFile[0];
    }

    /**
     * Loads recent files in background.
     */
    private void setRecentFilesInBackground()
    {
        // Get chart files
        DropBox dropBox = getDropBox();
        WebFile[] dropBoxfiles = dropBox.getRootDir().getFiles();
        Stream<WebFile> dropBoxfilesStream = Stream.of(dropBoxfiles);
        Stream<WebFile> jeplFilesStream = dropBoxfilesStream.filter(f -> DocPane.JAVA_FILE_EXT.equals(f.getType()));
        WebFile[] jeplFiles = jeplFilesStream.toArray(size -> new WebFile[size]);

        // Set files and trigger reload
        _recentFiles = jeplFiles;
        runLater(() -> recentFilesLoaded());
    }

    /**
     * Gets the DropBox.
     */
    private DropBox getDropBox()
    {
        // Get email
        String email = getCloudEmail();
        if (email == null || email.length() == 0)
            email = "guest@guest";

        // Get chart files
        return DropBox.getSiteForEmail(email);
    }

    /**
     * Called when cloud files finish loading.
     */
    private void recentFilesLoaded()
    {
        // Turn on progress bar
        ProgressBar pbar = getView("ProgressBar", ProgressBar.class);
        pbar.setIndeterminate(false);
        pbar.setVisible(false);
        resetLater();
    }

    /**
     * Clears recent documents from preferences.
     */
    public void clearRecentFiles()
    {
        Prefs.get().getChild("RecentDocuments").clear();
    }

    /**
     * Loads the WelcomePaneAnim.snp DocView.
     */
    DocView getAnimView()
    {
        // Unarchive WelcomePaneAnim.snp as DocView
        WebURL url = WebURL.getURL(WelcomePanel.class, "WelcomePanelAnim.snp");
        DocView doc = (DocView) new ViewArchiver().getView(url);

        // Get page and clear border/shadow
        PageView page = doc.getPage();
        page.setBorder(null);
        page.setEffect(null);

        // Set BuildText and JavaText
        View bt = page.getChildForName("BuildText");
        View jt = page.getChildForName("JVMText");
        bt.setText("Build: " + SnapUtils.getBuildInfo());
        jt.setText("JVM: " + System.getProperty("java.runtime.version"));

        // Return doc
        return doc;
    }
}
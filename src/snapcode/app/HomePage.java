package snapcode.app;
import snap.util.SnapUtils;
import snap.view.*;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.NewFileTool;
import snapcode.webbrowser.WebPage;
import java.io.File;

/**
 * This page class is a useful hub for common project functions.
 */
public class HomePage extends WebPage {

    // The WorkspacePane
    protected WorkspacePane _workspacePane;

    // The HomePageView
    private HomePageView _homePageView;

    /**
     * Constructor.
     */
    public HomePage(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;

        WebURL homePageUrl = WebURL.getURL(getClass(), "HomePage.md"); assert (homePageUrl != null);
        setURL(homePageUrl);
    }

    /**
     * Creates a new file.
     */
    private void createNewFileForType(String fileType)
    {
        NewFileTool newFileTool = _workspacePane.getWorkspaceTools().getNewFileTool();
        runLater(() -> newFileTool.createFileForType(fileType));
    }

    /**
     * Clears the workspace.
     */
    private void clearWorkspace()
    {
        _workspacePane.clearWorkspace();
        setViewVisible("ClearWorkspaceButton", false);
        setViewVisible("RestoreWorkspaceButton", true);
    }

    /**
     * Restores the workspace.
     */
    private void restoreWorkspace()
    {
        _workspacePane.restoreWorkspace();
        setViewVisible("ClearWorkspaceButton", true);
        setViewVisible("RestoreWorkspaceButton", false);
    }

    /**
     * Creates a new project.
     */
    private void createNewProject()
    {
        // Removes image temp files
        removeImageTempFiles();

        // Show new project panel
        NewFileTool newFileTool = _workspacePane.getWorkspaceTools().getNewFileTool();
        runLater(newFileTool::showNewProjectPanel);
    }

    /**
     * Removes recent file for given url.
     */
    public void removeRecentFileUrl(WebURL recentFileUrl)
    {
        RecentFiles.removeURL(recentFileUrl);
        _workspacePane.getPagePane().showHomePage();
    }

    /**
     * Shows the samples page.
     */
    public void showSamplesPage()
    {
        SamplesPage samplesPage = new SamplesPage(_workspacePane);
        _workspacePane.getPagePane().setPageForURL(samplesPage.getURL(), samplesPage);
        _workspacePane.getPagePane().setSelPage(samplesPage);
    }

    /**
     * Shows the release notes.
     */
    public void showReleaseNotes()
    {
        //WebURL releaseNotesURL = WebURL.getURL("/Users/jeff/Markdown/ReleaseNotes.md");
        WebURL releaseNotesURL = WebURL.getURL("https://reportmill.com/SnapCode/ReleaseNotes.md");
        assert releaseNotesURL != null;
        WebFile releaseNotesFile = releaseNotesURL.getFile();
        _workspacePane.openFile(releaseNotesFile);
    }

    /**
     * Called to resolve links.
     */
    protected void handleLinkClick(String urlAddr)
    {
        // Handle any link with "OpenRecent:..."
        if (urlAddr.startsWith("OpenRecent:")) {
            String recentFileUrlAddr = urlAddr.substring("OpenRecent:".length());
            WebURL recentFileUrl = WebURL.getURL(recentFileUrlAddr);
            boolean didOpen = recentFileUrl != null && WorkspacePaneUtils.openFileUrl(_workspacePane, recentFileUrl);
            if (!didOpen)
                removeRecentFileUrl(recentFileUrl);
            return;
        }

        // Handle any link with "Sample:..."
        if (urlAddr.startsWith("Sample:")) {

            // Get URL address
            String sampleUrlAddr = urlAddr.substring("Sample:".length());

            // This would let us have github project with icon at reportmill (not currently used)
            if (sampleUrlAddr.endsWith(".git") && sampleUrlAddr.contains("https://reportmill.com/SnapCode/Samples")) {
                WebURL sampleUrl = WebURL.getURL(sampleUrlAddr); assert (sampleUrl != null);
                sampleUrlAddr = "https://github.com/reportmill/" + sampleUrl.getFilename();
            }

            // Open URL
            WebURL sampleUrl = WebURL.getURL(sampleUrlAddr);
            WorkspacePaneUtils.openSamplesUrl(_workspacePane, sampleUrl);
            return;
        }

        switch (urlAddr) {

            // Handle NewJavaClassButton, NewJavaReplButton
            case "NewJavaClassButton": createNewFileForType("java"); break;
            case "NewJavaReplButton": createNewFileForType("jepl"); break;

            // Handle NewProjectButton, NewBlockProjectButton
            case "NewProjectButton":
            case "NewBlockProjectButton": createNewProject(); break;
        }
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        _homePageView = new HomePageView(this);
        ScrollView scrollView = new ScrollView(_homePageView);
        scrollView.setBorder(null);
        return scrollView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        WebURL homePageUrl = getURL();
        String homePageText = homePageUrl.getText();
        _homePageView.setMarkDown(homePageText);
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle ClearWorkspaceButton, RestoreWorkspaceButton
            case "ClearWorkspaceButton": clearWorkspace(); break;
            case "RestoreWorkspaceButton": restoreWorkspace(); break;

            // Handle OpenButton, OpenDesktopFileButton
            case "OpenButton": _workspacePane.getWorkspaceTools().getFilesTool().showOpenFilePanel(); break;
            case "OpenDesktopFileButton": _workspacePane.getWorkspaceTools().getFilesTool().showOpenDesktopFilePanel(); break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
    }

    /**
     * Override so we don't get plain markdown page.
     */
    @Override
    public void reload()
    {
        runLater(_workspacePane._pagePane::showHomePage);
    }

    /**
     * Removes CJ "imageio23452345.tmp" files.
     */
    private static void removeImageTempFiles()
    {
        if (!SnapUtils.isWebVM) return;
        try {
            File rootFile = new File("/files");
            File[] rootFiles = rootFile.listFiles(); assert (rootFiles != null);
            for (File file : rootFiles) {
                if (file.getName().startsWith("imageio"))
                    if (!file.delete())
                        System.out.println("HomePage.removeImageTempFiles: Failed to delete: " + file);
            }
        }
        catch (Exception ignore) { }
    }
}

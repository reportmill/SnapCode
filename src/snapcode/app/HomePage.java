package snapcode.app;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.NewFileTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.webbrowser.WebPage;

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

        WebURL homePageUrl = WebURL.getResourceUrl(getClass(), "HomePage.md"); assert (homePageUrl != null);
        setURL(homePageUrl);
    }

    /**
     * Creates a new file.
     */
    private void createNewFileForType(String fileType)
    {
        NewFileTool newFileTool = _workspacePane.getNewFileTool();
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
        NewFileTool newFileTool = _workspacePane.getNewFileTool();
        runLater(newFileTool::showNewProjectPanel);
    }

    /**
     * Removes recent project for given url.
     */
    public void removeRecentProjectUrl(WebURL recentProjectUrl)
    {
        // Remove URL
        RecentFiles.removeURL(recentProjectUrl);

        // If project open, close it
        String projectName = recentProjectUrl.getFilenameSimple();
        Project project = _workspacePane.getWorkspace().getProjectForName(projectName);
        if (project != null)
            _workspacePane.getWorkspace().closeProject(project);

        // Get project site
        WebSite projectSite = SnapCodeUtils.getSnapCodeProjectSiteForName(projectName);

        // If project files exist, ask to remove files
        if (projectSite.getExists()) {

            // Ask to remove project
            boolean removeProjectFiles = DialogBox.showConfirmDialog(getUI(), "Remove Recent File",
                    "Remove project files for project: " + projectName + "?");
            if (removeProjectFiles) {
                try { ProjectUtils.deleteProjectFilesForSite(projectSite); }
                catch (Exception e) { DialogBox.showExceptionDialog(getUI(), "Delete failed", e); }
            }
        }

        // Reshow home page
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
     * Shows the SnapCloud page.
     */
    public void showSnapCloudPage()
    {
        SnapCloudPage samplesPage = new SnapCloudPage(_workspacePane);
        _workspacePane.getPagePane().setPageForURL(samplesPage.getURL(), samplesPage);
        _workspacePane.getPagePane().setSelPage(samplesPage);
    }

    /**
     * Shows the release notes.
     */
    public void showReleaseNotes()
    {
        //WebURL releaseNotesURL = WebURL.getURL("/Users/jeff/Markdown/ReleaseNotes.md");
        WebURL releaseNotesURL = WebURL.getUrl("https://reportmill.com/SnapCode/ReleaseNotes.md");
        assert releaseNotesURL != null;
        WebFile releaseNotesFile = releaseNotesURL.getFile();
        _workspacePane.openFile(releaseNotesFile);
    }

    /**
     * Opens project for given recent project URL address.
     */
    private void openProjectForRecentProjectUrlAddress(String urlAddr)
    {
        // Get recent project URL
        WebURL recentProjectUrl = WebURL.getUrl(urlAddr);
        if (recentProjectUrl == null)
            return;

        // Open project URL
        if (!WorkspacePaneUtils.openProjectUrl(_workspacePane, recentProjectUrl))
            removeRecentProjectUrl(recentProjectUrl);
    }

    /**
     * Opens project for given sample project URL address.
     */
    private void openProjectForSampleUrlAddress(String sampleUrlAddr)
    {
        // This would let us have github project with icon at reportmill (not currently used)
        if (sampleUrlAddr.endsWith(".git") && sampleUrlAddr.contains("https://reportmill.com/SnapCode/Samples")) {
            WebURL sampleUrl = WebURL.getUrl(sampleUrlAddr); assert (sampleUrl != null);
            sampleUrlAddr = "https://github.com/reportmill/" + sampleUrl.getFilename();
        }

        // Open URL
        WebURL sampleUrl = WebURL.getUrl(sampleUrlAddr);
        WorkspacePaneUtils.openSamplesUrl(_workspacePane, sampleUrl);
    }

    /**
     * Called to resolve links.
     */
    protected void handleLinkClick(String urlAddr)
    {
        // Handle any link with "OpenRecent:..."
        if (urlAddr.startsWith("OpenRecent:")) {
            openProjectForRecentProjectUrlAddress(urlAddr.substring("OpenRecent:".length()));
            return;
        }

        // Handle any link with "Sample:..."
        if (urlAddr.startsWith("Sample:")) {
            openProjectForSampleUrlAddress(urlAddr.substring("Sample:".length()));
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
     * Resets the UI.
     */
    @Override
    protected void resetUI()
    {
        // Update OpenScratchPadButton
        setViewVisible("OpenScratchPadButton", _workspacePane.getWorkspace().getProjectForName(WorkspacePaneUtils.SCRATCH_PAD) == null);
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle CloudButton
            case "CloudButton": showSnapCloudPage(); break;

            // Handle ClearWorkspaceButton, RestoreWorkspaceButton
            case "ClearWorkspaceButton": clearWorkspace(); break;
            case "RestoreWorkspaceButton": restoreWorkspace(); break;

            // Handle OpenButton, OpenDesktopFileButton, OpenScratchPadButton
            case "OpenButton": _workspacePane.getFilesTool().showOpenFilePanel(); break;
            case "OpenDesktopFileButton": _workspacePane.getFilesTool().showOpenDesktopFilePanel(); break;
            case "OpenScratchPadButton": WorkspacePaneUtils.openScratchPad(_workspacePane); break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
    }
}

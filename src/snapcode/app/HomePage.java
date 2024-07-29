package snapcode.app;
import snap.util.SnapUtils;
import snap.view.*;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.NewFileTool;
import snapcode.project.Project;
import snapcode.project.Workspace;
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
     * Creates a new project.
     */
    private void createNewProject()
    {
        // Removes image temp files
        removeImageTempFiles();

        // Hidden Greenfoot opener
        if (ViewUtils.isAltDown()) {
            GreenImport.showGreenfootPanel(_workspacePane);
            return;
        }

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
            String sampleUrlAddr = urlAddr.substring("Sample:".length());
            if (ViewUtils.isAltDown()) {
                sampleUrlAddr = "https://github.com/reportmill/SnapDemos.git";
                WebURL snapCodeDirURL = SnapCodeUtils.getSnapCodeDirURL();
                WebURL projDirURL = snapCodeDirURL.getChild("SnapDemos");
                WebFile projDir = projDirURL.getFile();
                if (projDir != null)
                    projDir.delete();
            }
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

            // Handle ClearWorkspaceButton
            case "ClearWorkspaceButton": clearWorkspace(); break;

            // Handle OpenButton, OpenDesktopFileButton
            case "OpenButton": _workspacePane.getWorkspaceTools().getFilesTool().showOpenFilePanel(); break;
            case "OpenDesktopFileButton": _workspacePane.getWorkspaceTools().getFilesTool().showOpenDesktopFilePanel(); break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
    }

    /**
     * Removes all projects from workspace.
     */
    private void clearWorkspace()
    {
        Workspace workspace = _workspacePane.getWorkspace();
        Project[] projects = workspace.getProjects();
        for (Project project : projects)
            workspace.removeProject(project);
        setViewVisible("ClearWorkspaceButton", false);
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

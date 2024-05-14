package snapcode.app;
import snap.view.*;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.FilesTool;
import snapcode.project.ProjectUtils;
import snapcode.project.Workspace;
import snapcode.webbrowser.WebPage;

/**
 * This page class is a useful hub for common project functions.
 */
public class HomePage extends WebPage {

    // The WorkspacePane
    private WorkspacePane _workspacePane;

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
    private void createNewJavaFile(String fileType)
    {
        Workspace workspace = _workspacePane.getWorkspace();
        ProjectUtils.getTempProject(workspace);

        // Open new Jepl file
        runLater(() -> {
            FilesTool filesTool = _workspacePane.getWorkspaceTools().getFilesTool();
            if (fileType.equals("jepl"))
                filesTool.newJeplFileForNameAndString("JavaFiddle", "");
            else filesTool.newJavaFileForName("JavaFiddle");
        });
    }

    /**
     * Creates a new project.
     */
    private void createNewProject()
    {
        // Open empty workspace pane
        _workspacePane.showProjectTool();

        // Show new project panel
        runLater(() -> {
            FilesTool filesTool = _workspacePane.getWorkspaceTools().getFilesTool();
            filesTool.showNewProjectPanel(getUI());
        });
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
            WebFile recentFile = recentFileUrl != null ? recentFileUrl.getFile() : null;
            if (recentFile != null)
                WorkspacePaneUtils.openFile(_workspacePane, recentFile);
            else removeRecentFileUrl(recentFileUrl);
            return;
        }

        // Handle any link with "Sample:..."
        if (urlAddr.startsWith("Sample:")) {
            String sampleUrlAddr = urlAddr.substring("Sample:".length());
            WebURL sampleUrl = WebURL.getURL(sampleUrlAddr);
            WorkspacePaneUtils.openSamplesUrl(_workspacePane, sampleUrl);
            return;
        }

        switch (urlAddr) {

            // Handle NewJavaClassButton, NewJavaReplButton
            case "NewJavaClassButton": createNewJavaFile("java"); break;
            case "NewJavaReplButton": createNewJavaFile("jepl"); break;

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
}

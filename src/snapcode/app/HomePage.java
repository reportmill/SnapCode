package snapcode.app;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.NewFileTool;
import snapcode.project.ProjectUtils;
import snapcode.webbrowser.WebPage;
import java.io.File;

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

        // Show new project panel
        NewFileTool newFileTool = _workspacePane.getWorkspaceTools().getNewFileTool();
        runLater(newFileTool::showNewProjectPanel);
    }

    /**
     * Shows the open panel.
     */
    private void showOpenPanel()
    {
        FilePanel filePanel = new FilePanel();
        filePanel.setFileValidator(file -> ProjectUtils.isValidOpenFile(file));
        WebFile openFile = filePanel.showFilePanel(getUI());
        if (openFile == null)
            return;

        // Open file
        WorkspacePaneUtils.openFile(_workspacePane, openFile);
    }

    /**
     * Shows the open desktop file panel.
     */
    private void showOpenDesktopFilePanel()
    {
        String[] fileTypes = { "*" };
        getEnv().showFilePicker(fileTypes, pickedFile -> {
            System.out.println("OpenFile picked: " + pickedFile);
            WorkspacePaneUtils.openFile(_workspacePane, pickedFile);
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

            // Handle OpenButton, Bogus
            case "OpenButton": showOpenPanel(); break;
            case "OpenDesktopFileButton": showOpenDesktopFilePanel(); break;
            case "Bogus": break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
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

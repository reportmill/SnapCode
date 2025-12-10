package snapcode.app;
import snap.util.*;
import snap.view.TabView;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.*;
import snapcode.apptools.HelpTool;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.ProjectFilesTool;
import snapcode.apptools.RunTool;
import snapcode.project.*;
import java.util.List;

/**
 * Utilities for WorkspacePane.
 */
public class WorkspacePaneUtils {

    // Constant for Scratch pad project name
    public static final String SCRATCH_PAD = "ScratchPad";

    /**
     * Opens default workspace and triggers home page.
     */
    public static void openDefaultWorkspace()
    {
        // Create workspace pane
        WorkspacePane workspacePane = new WorkspacePane();
        workspacePane.show();

        // Show home page (was: ViewUtils.runDelayed(workspacePane.getPagePane()::showHomePage, 500))
        workspacePane.getPagePane().showHomePageNoAnimation();
    }

    /**
     * Opens any file.
     */
    public static void openFile(WorkspacePane workspacePane, WebFile aFile)
    {
        WebURL fileUrl = aFile.getUrl();
        openFileUrl(workspacePane, fileUrl);
    }

    /**
     * Opens any file URL.
     */
    public static void openFileUrl(WorkspacePane workspacePane, WebURL fileUrl)
    {
        switch (fileUrl.getFileType()) {

            // Handle java/jepl
            case "java": case "jepl": openExternalSourceFile(workspacePane, fileUrl); return;

            // Handle lesson
            case "md": openLesson(workspacePane, fileUrl); return;

            // Handle anything else: Try to open as project (zip, git, gfar, project dir)
            default: openProjectUrl(workspacePane, fileUrl);
        }
    }

    /**
     * Opens the given samples URL.
     */
    public static void openSamplesUrl(WorkspacePane workspacePane, WebURL sampleURL)
    {
        // This isn't great, but remove any non-TempProj projects
        Workspace workspace = workspacePane.getWorkspace();
        List<Project> projects = workspace.getProjects();
        for (Project project : projects) {
            if (!project.getName().equals("TempProj"))
                workspace.closeProject(project);
        }

        // Open sample URL
        openFileUrl(workspacePane, sampleURL);
    }

    /**
     * Opens known sample for given name.
     */
    public static void openSampleForName(WorkspacePane workspacePane, String sampleName)
    {
        // Get sample url
        String SAMPLES_ROOT = "https://reportmill.com/SnapCode/Samples/";
        String samplePath = FilePathUtils.getFilenameSimple(sampleName) + '/' + sampleName;
        WebURL sampleURL = WebURL.getUrl(SAMPLES_ROOT + samplePath);
        assert (sampleURL != null);

        // Open sample URL
        openSamplesUrl(workspacePane, sampleURL);
    }

    /**
     * Opens a lesson.
     */
    private static void openLesson(WorkspacePane workspacePane, WebURL lessonURL)
    {
        HelpTool.setDefaultHelpFileUrl(lessonURL);
        workspacePane.getWorkspaceTools().getHelpTool().showTool();
    }

    /**
     * Opens a source file.
     */
    private static void openExternalSourceFile(WorkspacePane workspacePane, WebURL sourceFileURL)
    {
        // Get source file - complain and return if not found
        WebFile sourceFile = sourceFileURL.getFile();
        if (sourceFile == null) {
            String msg = "Couldn't find sample for name: " + sourceFileURL.getFilename();
            DialogBox.showErrorDialog(workspacePane.getUI(), "Unknown Sample", msg);
            return;
        }

        // Make sure workspace has temp project
        Workspace workspace = workspacePane.getWorkspace();
        Project tempProj = ProjectUtils.getTempProject(workspace);

        // If new source file has word 'chart', add SnapCharts runtime to tempProj
        if (sourceFile.getText().contains("chart"))
            tempProj.getBuildFile().setIncludeSnapChartsRuntime(true);

        // Create new source file for given external source file
        NewFileTool newFileTool = workspacePane.getNewFileTool();
        WebFile newSourceFile = newFileTool.newSourceFileForExternalSourceFile(sourceFile);
        if (newSourceFile == null) {
            System.out.println("WorkspacePane.openSourceFile: Couldn't open source file: " + sourceFile);
            return;
        }

        // Show source file
        ViewUtils.runLater(() -> workspacePane.openFile(newSourceFile));
    }

    /**
     * Opens a project URL (zip, git, gfar or project dir).
     */
    public static boolean openProjectUrl(WorkspacePane workspacePane, WebURL projectUrl)
    {
        // If project for URL name already exists, open that
        WebFile localProjectDir = SnapCodeUtils.getSnapCodeProjectDirForName(projectUrl.getFilenameSimple());
        if (ProjectUtils.isProjectDir(localProjectDir)) {
            openProjectForProjectFile(workspacePane, localProjectDir);
            if (projectUrl.getRef() != null)
                openRepoUrlFileReference(workspacePane, projectUrl);
            return true;
        }

        // Handle SnapCloud URL
        if (VersionControlSnapCloud.isSnapCloudUrl(projectUrl)) {
            openProjectForRepoUrl(workspacePane, projectUrl);
            return true;
        }

        // If GitHub repo with missing file type, add '.git' extension
        if (projectUrl.getFileType().isEmpty() && projectUrl.getString().startsWith("https://github.com/"))
            projectUrl = WebURL.createUrl(projectUrl.getString() + ".git");

        switch (projectUrl.getFileType()) {

            // Handle zip, git: Open project for repo URL
            case "zip": //return openProjectForZipUrl(workspacePane, projectUrl);
            case "git": openProjectForRepoUrl(workspacePane, projectUrl); return true;

            // Handle gfar
            case "gfar": GreenImport.openProjectForGreenfootArchiveUrl(workspacePane, projectUrl); return true;

            // Handle anything else: Open project for given file. Maybe only do this for existing SnapCode projects?
            default:
                WebFile file = projectUrl.getFile();
                if (file == null)
                    return false;
                openProjectForProjectFile(workspacePane, file);
                return true;
        }
    }

    /**
     * Opens a project for given project file.
     */
    public static void openProjectForProjectFile(WorkspacePane workspacePane, WebFile projectFile)
    {
        // If ScratchPad, set ProjectFilesTool.DisplayMode to history (shouldn't need this twice)
        if (projectFile.getName().equals("ScratchPad"))
            workspacePane.getProjectFilesTool().setDisplayMode(ProjectFilesTool.DisplayMode.History);

        // Get project dir and project site
        WebFile projectDir = projectFile.isDir() ? projectFile : projectFile.getParent();
        WebSite projectSite = projectDir.getUrl().getAsSite();

        // Open project: Get project site and open in workspace
        Workspace workspace = workspacePane.getWorkspace();
        workspace.openProjectForSite(projectSite);
    }

    /**
     * Adds a project to given workspace pane for repo URL.
     */
    public static void openProjectForRepoUrl(WorkspacePane workspacePane, WebURL repoURL)
    {
        Workspace workspace = workspacePane.getWorkspace();

        // Open project for repo URL
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>("Checkout " + repoURL.getString());
        checkoutRunner.setTaskFunction(() -> workspace.openProjectForRepoUrl(repoURL, checkoutRunner.getMonitor()));
        checkoutRunner.setOnSuccess(e -> openRepoUrlFileReference(workspacePane, repoURL));
        checkoutRunner.setOnFailure(e -> handleOpenProjectForRepoUrlFailed(workspacePane, repoURL, e));
        checkoutRunner.setOnCancelled(() -> handleOpenProjectForRepoUrlCancelled(repoURL));
        checkoutRunner.start();

        // Show check progress panel
        checkoutRunner.getMonitor().showProgressPanel(workspacePane.getUI());
    }

    /**
     * Called if openProjectForRepoURL fails.
     */
    private static void handleOpenProjectForRepoUrlFailed(WorkspacePane workspacePane, WebURL repoURL, Exception anException)
    {
        DialogBox dialogBox = new DialogBox("Checkout failed");
        dialogBox.setErrorMessage("Failed checkout: " + repoURL.getString() + '\n' + "Error: " + anException.getMessage());
        dialogBox.showMessageDialog(workspacePane.getUI());

        handleOpenProjectForRepoUrlCancelled(repoURL);
    }

    /**
     * Called if openProjectForRepoURL is cancelled to delete any partially checked out files.
     */
    private static void handleOpenProjectForRepoUrlCancelled(WebURL repoURL)
    {
        System.out.println("WorkspacePane: open project repo interrupted - local project files removed");
        WebSite projectSite = SnapCodeUtils.getSnapCodeProjectSiteForName(repoURL.getFilenameSimple());
        ProjectUtils.deleteProjectFilesForSite(projectSite);
    }

    /**
     * Opens a file reference for given URL.
     */
    private static void openRepoUrlFileReference(WorkspacePane workspacePane, WebURL repoURL)
    {
        // If no file ref, just return
        String fileRef = repoURL.getRef();
        if (fileRef == null || fileRef.isBlank())
            return;
        if (!fileRef.startsWith("/"))
            fileRef = '/' + fileRef;

        // Get project for repo URL
        Project project = workspacePane.getWorkspace().getProjectForName(repoURL.getFilenameSimple());
        if (project == null)
            return;

        // If file ref resolves to project file, open it
        WebFile file = project.getSourceFileForPath(fileRef);
        if (file == null) file = project.getFileForPath(fileRef);
        if (file != null) { WebFile finalFile = file;
            ViewUtils.runLater(() -> workspacePane.openFile(finalFile)); }
    }

    /**
     * Opens the scratch pad project.
     */
    public static void openScratchPad(WorkspacePane workspacePane)
    {
        WebFile projectDir = SnapCodeUtils.getSnapCodeProjectDirForName(SCRATCH_PAD);
        boolean projectDirExists = projectDir.getExists();

        // Change ProjectFilesTool display mode to history
        workspacePane.getProjectFilesTool().setDisplayMode(ProjectFilesTool.DisplayMode.History);

        // Open project
        openProjectForProjectFile(workspacePane, projectDir);

        // If project didn't exist, configure project
        if (!projectDirExists) {

            // Add SnapKit dependency
            Project scratchProj = workspacePane.getWorkspace().getProjectForName(SCRATCH_PAD);
            scratchProj.getBuildFile().setIncludeSnapKitRuntime(true);
            scratchProj.getBuildFile().writeFile();

            // Add starter file
            NewFileTool newFileTool = workspacePane.getNewFileTool();
            ViewUtils.runLater(() -> newFileTool.newJeplFileForNameAndString("ScratchPad1", "jepl", ""));
        }
    }

    /**
     * Opens a temp blocks project.
     */
    public static void openTempBlocksProject(WorkspacePane workspacePane)
    {
        Project tempBlocksProject = ProjectUtils.getTempBlocksProject(workspacePane.getWorkspace());
        openProjectForProjectFile(workspacePane, tempBlocksProject.getSite().getURL().getFile());
    }

    /**
     * Opens a Workspace in embed mode.
     */
    public static void openEmbedWorkspace(String javaStr, String fileType)
    {
        // Increase default font size
        if (JavaTextUtils.getDefaultJavaFontSize() < 15)
            JavaTextUtils.setDefaultJavaFontSize(15);

        // Open Java/Jepl string
        WorkspacePane._embedMode = true;
        WorkspacePane workspacePane = new WorkspacePane();
        workspacePane.getUI();
        WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
        workspaceTools.setShowLeftTray(false);
        workspaceTools.setShowBottomTray(false);
        workspaceTools.getToolButtonForClass(RunTool.class).setMargin(0, 0, 0, 32);
        workspacePane.getWindow().getRootView().setBorderRadius(8);
        workspacePane.getWindow().getRootView().setClipToBounds(true);
        workspacePane.show();

        // If string provided, hide right tray (don't need help)
        if (javaStr != null)
            workspacePane.getWorkspaceTools().getRightTray().getUI(TabView.class).getTabBar().setVisible(false);

        // If no java string provided, use sample
        if (javaStr == null) {
            javaStr = SAMPLE_EMBED_CODE;
            fileType = "jepl";
        }

        // Create and open new Jepl file
        NewFileTool newFileTool = workspacePane.getNewFileTool();
        newFileTool.newJavaFileForStringAndType(javaStr, fileType);
    }

    /**
     * Selects a good default file.
     */
    public static void selectGoodDefaultFile(WorkspacePane workspacePane, Project project)
    {
        // If project was given, show source dir
        if (project != null) {
            WebFile sourceDir = project.getSourceDir();
            workspacePane.openFile(sourceDir);
        }

        // Get good default file and open it
        Workspace workspace = workspacePane.getWorkspace();
        WebFile defaultFile = project != null ? ProjectUtils.getGoodDefaultFile(project) :
                WorkspaceUtils.getGoodDefaultFile(workspace);
        if (defaultFile != null)
            workspacePane.openFile(defaultFile);
    }

    /**
     * Adds a recent project to recent files.
     */
    protected static void addRecentProject(Project aProject)
    {
        // Get project site URL
        WebURL projectURL = aProject.getSite().getURL();

        // If URL to TempProj or Temp dir, just return
        String filePath = projectURL.getPath();
        if (filePath.startsWith(SnapUtils.getTempDir()) || filePath.startsWith("/tmp"))
            return;

        // Add URL
        RecentFiles.addURL(projectURL);
    }

    // A Sample embed string
    private static final String SAMPLE_EMBED_CODE = """
        var button = new Button("Hello World");
        button.setPropsString("Font: Arial Bold 24; Margin:40; Padding:10; Effect:Shadow;");
        button.setAnimString("time: 1000; scale: 2; time: 2000; scale: 1; time: 2000; rotate: 360");
        button.getAnim(0).setLoopCount(3).play();
        button.addEventHandler(e -> show(new Label("Stop that")), View.Action);
        show(button);
        """;
}

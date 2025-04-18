package snapcode.app;
import snap.util.*;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.web.*;
import snapcode.apptools.HelpTool;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.ProjectFilesTool;
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

        // Show home page
        ViewUtils.runDelayed(() -> workspacePane.getPagePane().showHomePage(), 500);
    }

    /**
     * Opens any file.
     */
    public static void openFile(WorkspacePane workspacePane, WebFile aFile)
    {
        WebURL fileUrl = aFile.getURL();
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
        WebURL sampleURL = WebURL.getURL(SAMPLES_ROOT + samplePath);
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
        switch (projectUrl.getFileType()) {

            // Handle zip, git: Open project for repo URL
            case "zip": //return openProjectForZipUrl(workspacePane, projectUrl);
            case "git": openProjectForRepoUrl(workspacePane, projectUrl); return true;

            // Handle gfar
            case "gfar": GreenImport.openProjectForGreenfootArchiveUrl(workspacePane, projectUrl); return true;

            // Handle anything else: Open project for given file
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
        // Get project dir and project site
        WebFile projectDir = projectFile.isDir() ? projectFile : projectFile.getParent();
        WebSite projectSite = projectDir.getURL().getAsSite();

        // Open project: Get project site and open in workspace
        Workspace workspace = workspacePane.getWorkspace();
        workspace.openProjectForSite(projectSite);
    }

    /**
     * Adds a project to given workspace pane for Zip URL.
     */
    public static boolean openProjectForZipUrl(WorkspacePane workspacePane, WebURL zipURL)
    {
        // Get zipped directory file for zip file
        WebFile zipDirFile = getZipFileMainFile(zipURL);
        if (zipDirFile == null) {
            String msg = "Can't find archived directory in zip file: " + zipURL.getString();
            DialogBox.showErrorDialog(workspacePane.getUI(), "Error Opening Zip Url", msg);
            return false;
        }

        // If project already exists, ask to replace it
        Workspace workspace = workspacePane.getWorkspace();
        String projectName = zipDirFile.getName();
        Project existingProj = workspace.getProjectForName(projectName);
        if (existingProj != null) {

            // Ask to replace project - just return if denied
            DialogBox dialogBox = new DialogBox("Open Zip Url");
            String msg = "A project with the same name already exists.\nDo you want to replace it?";
            dialogBox.setQuestionMessage(msg);
            int answer = dialogBox.showOptionDialog(workspacePane.getUI(), null);
            if (answer == DialogBox.NO_OPTION)
                return true;

            // Otherwise, delete files
            try { existingProj.deleteProject(new TaskMonitor("Delete Project")); }
            catch (Exception e) {
                DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Project", e);
                return true;
            }
        }

        // Get project dir
        WebFile projectDir = SnapCodeUtils.getSnapCodeProjectDirForName(zipDirFile.getName());

        // If project directory already exists, ask to replace it
        if (projectDir.getExists()) {

            // Ask to replace directory - just open directory project if denied
            DialogBox dialogBox = new DialogBox("Open Zip Url");
            String msg = "A project directory with the same name already exists.\nDo you want to replace it?";
            dialogBox.setQuestionMessage(msg);
            int answer = dialogBox.showOptionDialog(workspacePane.getUI(), null);
            if (answer == DialogBox.CANCEL_OPTION)
                return false;

            // If replace, delete project dir
            if (answer == DialogBox.YES_OPTION) {
                try { projectDir.delete(); }
                catch (Exception e) {
                    DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Directory", e);
                    return false;
                }
            }
        }

        // Copy to SnapCode dir
        if (!projectDir.getExists())
            WebUtils.copyFile(zipDirFile, SnapCodeUtils.getSnapCodeDir());

        // Set remote Zip address
        WebSite projectSite = projectDir.getURL().getAsSite();
        VersionControlUtils.setRemoteSiteUrl(projectSite, zipURL);

        // Select good default file
        openProjectForProjectFile(workspacePane, projectDir);
        return true;
    }

    /**
     * Adds a project to given workspace pane for repo URL.
     */
    public static void openProjectForRepoUrl(WorkspacePane workspacePane, WebURL repoURL)
    {
        Workspace workspace = workspacePane.getWorkspace();

        // Open project for repo URL
        TaskMonitor taskMonitor = new TaskMonitor("Checkout " + repoURL.getString());
        TaskRunner<Boolean> checkoutRunner = new TaskRunner<>(() -> workspace.openProjectForRepoUrl(repoURL, taskMonitor));
        checkoutRunner.setMonitor(taskMonitor);
        checkoutRunner.start();

        // After add project, trigger build and show files
        //checkoutRunner.setOnSuccess(val -> openProjectForRepoUrlFinished(workspacePane, repoURL));
        checkoutRunner.setOnFailure(e -> openProjectForRepoUrlFailed(workspacePane, repoURL, e));

        // Show check progress panel
        checkoutRunner.getMonitor().showProgressPanel(workspacePane.getUI());
    }

    /**
     * Called if openProjectForRepoURL fails.
     */
    private static void openProjectForRepoUrlFailed(WorkspacePane workspacePane, WebURL repoURL, Exception anException)
    {
        DialogBox dialogBox = new DialogBox("Checkout failed");
        dialogBox.setErrorMessage("Failed checkout: " + repoURL.getString() + '\n' + "Error: " + anException.getMessage());
        dialogBox.showMessageDialog(workspacePane.getUI());
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
     * Opens a Workspace in embed mode.
     */
    public static void openEmbedWorkspace()
    {
        // Increase default font size
        if (JavaTextDocUtils.getDefaultJavaFontSize() < 15)
            JavaTextDocUtils.setDefaultJavaFontSize(15);

        // Open Java/Jepl string
        WorkspacePane._embedMode = true;
        WorkspacePane workspacePane = new WorkspacePane();
        workspacePane.getUI();
        WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
        workspaceTools.setShowLeftTray(false);
        workspaceTools.setShowBottomTray(false);
        workspacePane.getWindow().setType(WindowView.TYPE_PLAIN);
        workspacePane.show();

        // Open Jepl
        String javaStr = "var button = new Button(\"Hello World\");\n" +
                "button.setPropsString(\"Font: Arial Bold 24; Margin:40; Padding:10; Effect:Shadow;\");\n" +
                "button.setAnimString(\"time: 1000; scale: 2; time: 2000; scale: 1; time: 2000; rotate: 360\");\n" +
                "button.getAnim(0).setLoopCount(3).play();\n" +
                "button.addEventHandler(e -> show(new Label(\"Stop that\")), View.Action);\n" +
                "show(button);\n";

        // Create and open new Jepl file
        NewFileTool newFileTool = workspacePane.getNewFileTool();
        newFileTool.newJavaFileForStringAndType(javaStr, "jepl");
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
        WebURL projectURL = aProject.getSourceURL();

        // If URL to TempProj or Temp dir, just return
        String filePath = projectURL.getPath();
        if (filePath.contains("TempProj") || filePath.startsWith(SnapUtils.getTempDir()))
            return;

        // Add URL
        RecentFiles.addURL(projectURL);
    }

    /**
     * Returns the Zip file zipped directory file.
     */
    private static WebFile getZipFileMainFile(WebURL zipUrl)
    {
        WebSite zipSite = zipUrl.getAsSite();
        WebFile zipSiteRootDir = zipSite.getRootDir();
        List<WebFile> zipSiteRootFiles = zipSiteRootDir.getFiles();
        return ListUtils.findMatch(zipSiteRootFiles, file -> ProjectUtils.isProjectDir(file));
    }
}

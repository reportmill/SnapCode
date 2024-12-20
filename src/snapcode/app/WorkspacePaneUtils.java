package snapcode.app;
import snap.util.*;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.web.*;
import snapcode.apptools.HelpTool;
import snapcode.apptools.ProjectFilesTool;
import snapcode.apptools.NewFileTool;
import snapcode.project.*;

/**
 * Utilities for WorkspacePane.
 */
public class WorkspacePaneUtils {

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
    public static boolean openFileUrl(WorkspacePane workspacePane, WebURL fileUrl)
    {
        switch (fileUrl.getFileType()) {

            // Handle java/jepl
            case "java": case "jepl": return openExternalSourceFile(workspacePane, fileUrl);

            // Handle zip
            case "zip": return openProjectForZipUrl(workspacePane, fileUrl);

            // Handle git
            case "git": openProjectForRepoURL(workspacePane, fileUrl); return true;

            // Handle gfar
            case "gfar": GreenImport.openGreenfootForArchiveFileUrl(workspacePane, fileUrl); return true;

            // Handle lesson
            case "md": openLesson(workspacePane, fileUrl); return true;

            // Handle anything else: Open project for given file
            default:
                WebFile file = fileUrl.getFile();
                if (file == null)
                    return false;
                openProjectForProjectFile(workspacePane, file);
                return true;
        }
    }

    /**
     * Creates a new file.
     */
    public static void openNewFileOfType(WorkspacePane workspacePane, String fileType)
    {
        NewFileTool newFileTool = workspacePane.getWorkspaceTools().getNewFileTool();
        ViewUtils.runLater(() -> newFileTool.createFileForType(fileType));
    }

    /**
     * Opens a Java string file.
     */
    public static void openJavaString(WorkspacePane workspacePane, String javaString, String fileType)
    {
        // Open empty workspace pane with temp project
        Workspace workspace = workspacePane.getWorkspace();
        Project tempProj = ProjectUtils.getTempProject(workspace);

        // If new source file has word 'chart', add SnapCharts runtime to tempProj
        boolean isJepl = fileType.equals("jepl") || fileType.equals("jmd");
        if (isJepl && javaString.contains("chart"))
            tempProj.getBuildFile().setIncludeSnapChartsRuntime(true);

        // Show new project panel
        ViewUtils.runLater(() -> {
            NewFileTool newFileTool = workspacePane.getWorkspaceTools().getNewFileTool();
            newFileTool.newJavaOrJeplFileForNameAndTypeAndString("JavaFiddle", fileType, javaString);
        });
    }

    /**
     * Opens empty workspace and opens given sample name.
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
     * Opens a samples URL.
     */
    public static void openSamplesUrl(WorkspacePane workspacePane, WebURL sampleURL)
    {
        // This isn't great, but remove any non-TempProj projects
        Workspace workspace = workspacePane.getWorkspace();
        Project[] projects = workspace.getProjects();
        for (Project project : projects) {
            if (!project.getName().equals("TempProj"))
                workspace.removeProject(project);
        }

        // Open sample URL
        openFileUrl(workspacePane, sampleURL);
    }

    /**
     * Opens a lesson.
     */
    public static void openLesson(WorkspacePane workspacePane, WebURL lessonURL)
    {
        HelpTool.setDefaultHelpFileUrl(lessonURL);
        workspacePane.getWorkspaceTools().getHelpTool().showTool();
    }

    /**
     * Opens a source file.
     */
    public static boolean openExternalSourceFile(WorkspacePane workspacePane, WebURL sourceFileURL)
    {
        // Get source file - complain and return if not found
        WebFile sourceFile = sourceFileURL.getFile();
        if (sourceFile == null) {
            String msg = "Couldn't find sample for name: " + sourceFileURL.getFilename();
            DialogBox.showErrorDialog(workspacePane.getUI(), "Unknown Sample", msg);
            return false;
        }

        // Make sure workspace has temp project
        Workspace workspace = workspacePane.getWorkspace();
        Project tempProj = ProjectUtils.getTempProject(workspace);

        // If new source file has word 'chart', add SnapCharts runtime to tempProj
        if (sourceFile.getText().contains("chart"))
            tempProj.getBuildFile().setIncludeSnapChartsRuntime(true);

        // Create new source file for given external source file
        NewFileTool newFileTool = workspacePane.getWorkspaceTools().getNewFileTool();
        WebFile newSourceFile = newFileTool.newSourceFileForExternalSourceFile(sourceFile);
        if (newSourceFile == null) {
            System.out.println("WorkspacePane.openSourceFile: Couldn't open source file: " + sourceFile);
            return false;
        }

        // Add to RecentFiles
        addRecentFileUrl(sourceFile.getURL());

        // Show source file
        ViewUtils.runLater(() -> showFile(workspacePane, newSourceFile));
        return true;
    }

    /**
     * Opens a project for given project file.
     */
    public static void openProjectForProjectFile(WorkspacePane workspacePane, WebFile projectFile)
    {
        // Get project dir and project site
        WebFile projectDir = projectFile.isDir() ? projectFile : projectFile.getParent();
        WebSite projectSite = projectDir.getURL().getAsSite();

        // Open project: Get project site and add to workspace
        Workspace workspace = workspacePane.getWorkspace();
        Project project = workspace.addProjectForSite(projectSite);

        // Select good file
        ViewUtils.runDelayed(() -> selectGoodDefaultFile(workspacePane, project), 400);

        // Add recent file
        addRecentFileUrl(projectDir.getURL());
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
            if (answer == 1)
                return true;

            // Otherwise, delete files
            try { existingProj.deleteProject(new TaskMonitor("Delete Project")); }
            catch (Exception e) {
                DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Project", e);
                return true;
            }
        }

        // If project directory already exists, ask to replace it
        WebFile snapCodeDir = SnapCodeUtils.getSnapCodeDir();
        WebFile projectDir = snapCodeDir.getFileForName(zipDirFile.getName());
        if (projectDir != null) {

            // Ask to replace directory - just open directory project if denied
            DialogBox dialogBox = new DialogBox("Open Zip Url");
            String msg = "A project directory with the same name already exists.\nDo you want to replace it?";
            dialogBox.setQuestionMessage(msg);
            int answer = dialogBox.showOptionDialog(workspacePane.getUI(), null);
            if (answer == 1) {
                openProjectForProjectFile(workspacePane, projectDir);
                return true;
            }

            // Otherwise, delete files
            try { projectDir.delete(); }
            catch (Exception e) {
                DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Directory", e);
                return true;
            }
        }

        // Copy to SnapCode dir
        WebUtils.copyFile(zipDirFile, snapCodeDir);
        projectDir = snapCodeDir.getFileForName(zipDirFile.getName());

        // Select good default file
        openProjectForProjectFile(workspacePane, projectDir);
        return true;
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
        WorkspacePaneUtils.openJavaString(workspacePane, javaStr, "jepl");
    }

    /**
     * Adds a project to given workspace pane for repo URL.
     */
    public static void openProjectForRepoURL(WorkspacePane workspacePane, WebURL repoURL)
    {
        // Add project for repo URL
        Workspace workspace = workspacePane.getWorkspace();
        TaskRunner<Boolean> checkoutRunner = workspace.addProjectForRepoURL(repoURL);

        // After add project, trigger build and show files
        checkoutRunner.setOnSuccess(val -> openWorkspaceForRepoUrlFinished(workspacePane, repoURL));
        checkoutRunner.setOnFailure(e -> checkoutFailed(workspacePane, repoURL, e));

        // Show check progress panel
        checkoutRunner.getMonitor().showProgressPanel(workspacePane.getUI());
    }

    /**
     * Called if checkout fails.
     */
    private static void checkoutFailed(WorkspacePane workspacePane, WebURL repoURL, Exception anException)
    {
        DialogBox dialogBox = new DialogBox("Checkout failed");
        dialogBox.setErrorMessage("Failed checkout: " + repoURL.getString() + '\n' + "Error: " + anException.getMessage());
        dialogBox.showMessageDialog(workspacePane.getUI());
    }

    /**
     * Called when openWorkspaceForRepoURL finished.
     */
    private static void openWorkspaceForRepoUrlFinished(WorkspacePane workspacePane, WebURL repoURL)
    {
        // Select good default file
        String projName = repoURL.getFilenameSimple();
        Workspace workspace = workspacePane.getWorkspace();
        Project project = workspace.getProjectForName(projName);
        ViewUtils.runDelayed(() -> selectGoodDefaultFile(workspacePane, project), 400);

        // Add repoURL to recent files
        if (!addRecentFileUrl(repoURL)) {

            // If repo URL wasn't added, add project URL
            String filePath = repoURL.getPath();
            if (!filePath.contains("/SnapCode/Samples"))
                addRecentFileUrl(project.getSite().getURL());
        }
    }

    /**
     * Selects a good default file.
     */
    public static void selectGoodDefaultFile(WorkspacePane workspacePane, Project project)
    {
        // If project, expand source dir
        if (project != null) {
            ProjectFilesTool projectFilesTool = workspacePane.getWorkspaceTools().getProjectFilesTool();
            WebFile sourceDir = project.getSourceDir();
            projectFilesTool.showDir(sourceDir);
        }

        // Get good default file
        Workspace workspace = workspacePane.getWorkspace();
        WebFile defaultFile = project != null ? ProjectUtils.getGoodDefaultFile(project) :
                WorkspaceUtils.getGoodDefaultFile(workspace);
        if (defaultFile == null)
            return;

        // Select it
        showFile(workspacePane, defaultFile);
    }

    /**
     * Removes all projects from workspace.
     */
    public static void clearWorkspace(WorkspacePane workspacePane)
    {
        Workspace workspace = workspacePane.getWorkspace();
        Project[] projects = workspace.getProjects();
        for (Project project : projects)
            workspace.removeProject(project);
    }

    /**
     * Adds a recent file URL.
     */
    private static boolean addRecentFileUrl(WebURL fileUrl)
    {
        String filePath = fileUrl.getPath();
        if (filePath.contains("TempProj") || filePath.contains("/SnapCode/Samples/"))
            return false;

        // If URL is from temp dir, just return
        if (filePath.startsWith(SnapUtils.getTempDir()))
            return false;

        // Add URL
        RecentFiles.addURL(fileUrl);
        return true;
    }

    /**
     * Show file.
     */
    private static void showFile(WorkspacePane workspacePane, WebFile aFile)
    {
        ProjectFilesTool projectFilesTool = workspacePane.getWorkspaceTools().getProjectFilesTool();
        projectFilesTool.showFile(aFile);
    }

    /**
     * Returns the Zip file zipped directory file.
     */
    private static WebFile getZipFileMainFile(WebURL zipUrl)
    {
        WebSite zipSite = zipUrl.getAsSite();
        WebFile zipSiteRootDir = zipSite.getRootDir();
        WebFile[] zipSiteRootFiles = zipSiteRootDir.getFiles();
        return ArrayUtils.findMatch(zipSiteRootFiles, file -> ProjectUtils.isProjectDir(file));
    }
}

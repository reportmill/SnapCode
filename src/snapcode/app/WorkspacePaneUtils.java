package snapcode.app;
import snap.util.*;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.*;
import snapcode.apptools.ProjectFilesTool;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.RunTool;
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
        String fileType = fileUrl.getFileType();

        // If file type is zip, open Zip file
        if (fileType.equals("zip")) {
            openProjectForZipUrl(workspacePane, fileUrl);
            return true;
        }

        // If file type is git, open repo
        if (fileType.equals("git")) {
            openProjectForRepoURL(workspacePane, fileUrl);
            return true;
        }

        // Get file (just return if file doesn't exist)
        WebFile file = fileUrl.getFile();
        if (file == null)
            return false;

        // If file is gfar file, open repo
        if (fileType.equals("gfar")) {
            GreenImport.openGreenfootForArchiveFilePath(workspacePane, fileUrl.getPath());
            return true;
        }

        // If file is just a source file, open external source file
        boolean isSourceFile = ArrayUtils.contains(ProjectUtils.FILE_TYPES, fileType);
        if (isSourceFile) {
            ViewUtils.runLater(() -> openExternalSourceFile(workspacePane, file));
            return true;
        }

        // Open project for given file
        openProjectForProjectFile(workspacePane, file);
        return true;
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
    public static void openJavaString(WorkspacePane workspacePane, String javaString, boolean isJepl)
    {
        // Open empty workspace pane with temp project
        Workspace workspace = workspacePane.getWorkspace();
        Project tempProj = ProjectUtils.getTempProject(workspace);

        // If new source file has word 'chart', add SnapCharts runtime to tempProj
        if (isJepl && javaString.contains("chart"))
            tempProj.getBuildFile().setIncludeSnapChartsRuntime(true);

        // Show new project panel
        ViewUtils.runLater(() -> {
            String fileType = isJepl ? "jepl" : "java";
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

        switch (sampleURL.getFileType()) {

            // Handle java/jepl
            case "java": case "jepl":

                // Get sample file - complain and return if not found
                WebFile sampleFile = sampleURL.getFile();
                if (sampleFile == null) {
                    String msg = "Couldn't find sample for name: " + sampleURL.getFilename();
                    DialogBox.showErrorDialog(workspacePane.getUI(), "Unknown Sample", msg);
                    return;
                }

                // Open
                openExternalSourceFile(workspacePane, sampleFile);

                // Kick off run
                RunTool runTool = workspacePane.getWorkspaceTools().getRunTool();
                ViewUtils.runLater(() -> runTool.runAppForSelFile(false));
                break;


            // Handle zip
            case "zip": openProjectForZipUrl(workspacePane, sampleURL); break;


            // Handle git
            case "git": openProjectForRepoURL(workspacePane, sampleURL); break;
        }
    }

    /**
     * Opens a source file.
     */
    public static void openExternalSourceFile(WorkspacePane workspacePane, WebFile sourceFile)
    {
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
            return;
        }

        // Add to RecentFiles
        addRecentFileUrl(sourceFile.getURL());

        // Show source file
        ViewUtils.runLater(() -> showFile(workspacePane, newSourceFile));
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
    public static void openProjectForZipUrl(WorkspacePane workspacePane, WebURL zipURL)
    {
        // Get zipped directory file for zip file
        WebFile zipDirFile = getZipFileMainFile(zipURL);
        if (zipDirFile == null) {
            String msg = "Can't find archived directory in zip file: " + zipURL.getString();
            DialogBox.showErrorDialog(workspacePane.getUI(), "Error Opening Zip Url", msg);
            return;
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
                return;

            // Otherwise, delete files
            try { existingProj.deleteProject(new TaskMonitor("Delete Project")); }
            catch (Exception e) {
                DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Project", e);
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
                return;
            }

            // Otherwise, delete files
            try { projectDir.delete(); }
            catch (Exception e) {
                DialogBox.showExceptionDialog(workspacePane.getUI(), "Error Deleting Directory", e);
            }
        }

        // Copy to SnapCode dir
        WebUtils.copyFile(zipDirFile, snapCodeDir);
        projectDir = snapCodeDir.getFileForName(zipDirFile.getName());

        // Select good default file
        openProjectForProjectFile(workspacePane, projectDir);
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
    private static void selectGoodDefaultFile(WorkspacePane workspacePane, Project project)
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
        return ArrayUtils.findMatch(zipSiteRootFiles, file -> ProjectUtils.isProjectFile(file));
    }
}

package snapcode.app;
import snap.util.ArrayUtils;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.FileTreeTool;
import snapcode.apptools.FilesTool;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.RunTool;
import snapcode.project.*;

/**
 * Utilities for WorkspacePane.
 */
public class WorkspacePaneUtils {

    /**
     * Opens any file.
     */
    public static void openFile(WorkspacePane workspacePane, WebFile aFile)
    {
        // If file is just a source file, open external source file
        boolean isSourceFile = ArrayUtils.contains(WelcomePanel.FILE_TYPES, aFile.getType());
        if (isSourceFile) {
            ViewUtils.runLater(() -> openExternalSourceFile(workspacePane, aFile));
            return;
        }

        // If file is zip file, open repo
        if (aFile.getType().equals("zip")) {
            openProjectForRepoURL(workspacePane, aFile.getURL());
            return;
        }

        // Open project for given file
        openProjectForProjectFile(workspacePane, aFile);
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

        // Get sample file - complain and return if not found
        WebFile sampleFile = sampleURL.getFile();
        if (sampleFile == null) {
            String msg = "Couldn't find sample for name: " + sampleURL.getFilename();
            DialogBox.showErrorDialog(workspacePane.getUI(), "Unknown Sample", msg);
            return;
        }

        // If java/jepl file, open and run
        String fileType = sampleFile.getType();
        if (fileType.equals("java") || fileType.equals("jepl")) {

            // Open
            openExternalSourceFile(workspacePane, sampleFile);

            // Kick off run
            RunTool runTool = workspacePane.getWorkspaceTools().getRunTool();
            ViewUtils.runLater(() -> runTool.runAppForSelFile(false));
            return;
        }

        // If zip file, open project
        if (fileType.equals("zip"))
            openProjectForRepoURL(workspacePane, sampleFile.getURL());
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

        // If not "TempProj" file, add to RecentFiles
        WebURL sourceURL = sourceFile.getURL();
        if (!sourceURL.getString().contains("TempProj"))
            RecentFiles.addURL(sourceURL);

        // Show source file
        ViewUtils.runLater(() -> showFile(workspacePane, newSourceFile));
    }

    /**
     * Opens a project for given project file.
     */
    public static void openProjectForProjectFile(WorkspacePane workspacePane, WebFile projectFile)
    {
        // Show project tool
        workspacePane.showProjectTool();

        // Get project dir and project site
        WebFile projectDir = projectFile.isDir() ? projectFile : projectFile.getParent();
        WebSite projectSite = projectDir.getURL().getAsSite();

        // Open project: Get project site and add to workspace
        Workspace workspace = workspacePane.getWorkspace();
        Project project = workspace.addProjectForSite(projectSite);

        // Select good file
        ViewUtils.runDelayed(() -> selectGoodDefaultFile(workspacePane, project), 400);

        // Add recent file
        RecentFiles.addURL(projectDir.getURL());
    }

    /**
     * Adds a project to given workspace pane for repo URL.
     */
    public static void openProjectForRepoURL(WorkspacePane workspacePane, WebURL repoURL)
    {
        // Add repoURL to recent files
        RecentFiles.addURL(repoURL);

        // Open empty workspace pane
        workspacePane.showProjectTool();

        // Add project for repo URL
        Workspace workspace = workspacePane.getWorkspace();
        TaskRunner<Boolean> checkoutRunner = workspace.addProjectForRepoURL(repoURL);

        // After add project, trigger build and show files
        checkoutRunner.setOnSuccess(val -> openWorkspaceForRepoUrlFinished(workspacePane, repoURL));

        // Show check progress panel
        checkoutRunner.getMonitor().showProgressPanel(workspacePane.getUI());
    }

    /**
     * Called when openWorkspaceForRepoURL finished.
     */
    private static void openWorkspaceForRepoUrlFinished(WorkspacePane workspacePane, WebURL repoURL)
    {
        // Show project tool
        workspacePane.showProjectTool();

        // Select good default file
        String projName = repoURL.getFilenameSimple();
        Workspace workspace = workspacePane.getWorkspace();
        Project project = workspace.getProjectForName(projName);
        ViewUtils.runDelayed(() -> selectGoodDefaultFile(workspacePane, project), 400);
    }

    /**
     * Selects a good default file.
     */
    private static void selectGoodDefaultFile(WorkspacePane workspacePane, Project project)
    {
        // If project, expand source dir
        if (project != null) {
            FileTreeTool fileTreeTool = workspacePane.getWorkspaceTools().getFileTreeTool();
            WebFile sourceDir = project.getSourceDir();
            fileTreeTool.showDir(sourceDir);
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
     * Show file.
     */
    private static void showFile(WorkspacePane workspacePane, WebFile aFile)
    {
        FileTreeTool fileTreeTool = workspacePane.getWorkspaceTools().getFileTreeTool();
        fileTreeTool.showFile(aFile);
    }
}

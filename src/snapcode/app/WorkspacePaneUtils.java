package snapcode.app;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.FileTreeTool;
import snapcode.apptools.FilesTool;
import snapcode.apptools.RunTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.project.Workspace;
import snapcode.project.WorkspaceBuilder;

/**
 * Utilities for WorkspacePane.
 */
public class WorkspacePaneUtils {

    /**
     * Opens a samples URL.
     */
    public static void openSamplesUrl(WorkspacePane workspacePane, WebURL sampleURL)
    {
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
            RunTool runTool = workspacePane.getWorkspaceTools().getToolForClass(RunTool.class);
            if (runTool != null)
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
        ProjectUtils.getTempProject(workspace);

        // Create new source file for given external source file
        FilesTool filesTool = workspacePane.getWorkspaceTools().getFilesTool();
        WebFile newSourceFile = filesTool.newSourceFileForExternalSourceFile(sourceFile);
        if (newSourceFile == null) {
            System.out.println("WorkspacePane.openSourceFile: Couldn't open source file: " + sourceFile);
            return;
        }

        // If not "TempProj" file, add to RecentFiles
        WebURL sourceURL = sourceFile.getURL();
        if (!sourceURL.getString().contains("TempProj"))
            RecentFiles.addURL(sourceURL);

        // Show RunTool
        workspacePane.showRunTool();

        // Show source file
        ViewUtils.runLater(() -> {
            PagePane pagePane = workspacePane.getPagePane();
            pagePane.setSelFile(newSourceFile);
        });
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
        workspace.addProjectForSite(projectSite);

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

        // Select first file in project
        String projName = repoURL.getFilenameSimple();
        Workspace workspace = workspacePane.getWorkspace();
        Project project = workspace.getProjectForName(projName);
        if (project != null) {
            WebFile srcDir = project.getSourceDir();
            if (srcDir.getFileCount() > 0) {
                WebFile srcFile = srcDir.getFiles()[0];
                FileTreeTool fileTreeTool = workspacePane.getWorkspaceTools().getFileTreeTool();
                fileTreeTool.showFile(srcFile);
            }
        }

        // Build all files
        WorkspaceBuilder builder = workspacePane.getWorkspace().getBuilder();
        builder.addAllFilesToBuild();
        builder.buildWorkspaceLater();
    }
}

package snapcode.app;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.FileTreeTool;
import snapcode.apptools.RunTool;
import snapcode.project.Project;
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
        WebFile sampleFile = sampleURL.createFile(false);
        String fileType = sampleFile.getType();

        // If java/jepl file, open and run
        if (fileType.equals("java") || fileType.equals("jepl")) {

            // Open
            workspacePane.openExternalSourceFile(sampleFile);

            // Kick off run
            RunTool runTool = workspacePane.getWorkspaceTools().getToolForClass(RunTool.class);
            if (runTool != null)
                ViewUtils.runLater(() -> runTool.runAppForSelFile(false));
            return;
        }

        // If zip file, open project
        if (fileType.equals("zip"))
            addProjectForRepoURL(workspacePane, sampleFile.getURL());
    }

    /**
     * Adds a project to given workspace pane for repo URL.
     */
    public static void addProjectForRepoURL(WorkspacePane workspacePane, WebURL repoURL)
    {
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

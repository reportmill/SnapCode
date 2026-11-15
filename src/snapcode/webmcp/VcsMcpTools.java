package snapcode.webmcp;
import snap.util.ActivityMonitor;
import snap.util.TaskRunner;
import snap.web.WebFile;
import snapcode.app.ProjectPane;
import snapcode.app.SnapCloudPage;
import snapcode.app.WorkspacePane;
import snapcode.apptools.FilesTool;
import snapcode.apptools.VersionControlTool;
import snapcode.project.Project;
import snapcode.project.VersionControl;
import snapcode.project.WorkspaceBuilder;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * WebMCP tools for version control (GitHub / SnapCloud / zip remotes) of a project.
 * Signing in to SnapCloud or GitHub is left to the user.
 */
public class VcsMcpTools {

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("vcs_status",
                "Returns the version control status of a project: remote URL, whether checked out, and local files " +
                "that are modified/added/removed relative to the remote.",
                new Schema().str("project", McpUtils.PROJECT_ARG_DESC, false),
                VcsMcpTools::vcsStatus)).readOnly();

        reg.addTool(new McpTool("vcs_commit",
                "Commits (uploads) locally modified files of a project to its remote (SnapCloud or a GitHub repo the " +
                "user is signed in to). Saves all files first.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("message", "Commit message (used by remotes that support messages).", false)
                        .strArray("paths", "Only commit these files/directories (default: whole project).", false),
                VcsMcpTools::vcsCommit)).consequential();

        reg.addTool(new McpTool("vcs_update",
                "Updates (downloads) files that changed on the project's remote into the local project.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .strArray("paths", "Only update these files/directories (default: whole project).", false),
                VcsMcpTools::vcsUpdate)).consequential();

        reg.addTool(new McpTool("vcs_discard_changes",
                "Replaces local files with the remote versions, discarding local changes (cannot be undone).",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .strArray("paths", "Files/directories to replace.", true),
                VcsMcpTools::vcsDiscardChanges)).consequential();

        reg.addTool(new McpTool("save_to_snapcloud",
                "Saves the project to the signed-in user's SnapCloud storage (same as the SnapCloud save button). " +
                "Requires the user to be signed in to SnapCloud; ask them to sign in if not.",
                new Schema().str("project", McpUtils.PROJECT_ARG_DESC, false),
                VcsMcpTools::saveToSnapCloud)).consequential();
    }

    /**
     * Returns the version control for project (throws if none).
     */
    private static VersionControl getVersionControl(Project proj, boolean requireCheckedOut)
    {
        VersionControl vc = proj.getVersionControl();
        if (vc == null || vc.getRemoteSiteUrl() == null)
            throw new McpException("Project '" + proj.getName() + "' has no remote. Use save_to_snapcloud, or open the project from a GitHub URL.");
        if (requireCheckedOut && !vc.isCheckedOut())
            throw new McpException("Project '" + proj.getName() + "' is not checked out from its remote");
        return vc;
    }

    /**
     * Returns the files for 'paths' arg (or project root).
     */
    private static List<WebFile> getFiles(McpArgs args, Project proj)
    {
        List<String> paths = args.getStringList("paths");
        if (paths == null || paths.isEmpty())
            return List.of(proj.getRootDir());
        List<WebFile> files = new ArrayList<>();
        for (String path : paths)
            files.add(McpUtils.getFile(proj.getName(), path));
        return files;
    }

    /**
     * vcs_status
     */
    private static void vcsStatus(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        VersionControl vc = proj.getVersionControl();
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("project", proj.getName());
        map.put("remote", vc != null ? vc.getRemoteSiteUrlAddress() : null);
        map.put("snapcloud_signed_in", SnapCloudPage.getSnapCloudUserUrl() != null);
        if (vc != null && vc.getRemoteSiteUrl() != null) {
            map.put("checked_out", vc.isCheckedOut());
            List<Map<String,Object>> modified = new ArrayList<>();
            if (vc.isCheckedOut()) {
                for (WebFile file : vc.getModifiedFilesForLocalFiles(List.of(proj.getRootDir()))) {
                    if (file.isDir()) continue;
                    modified.add(Map.of("path", file.getPath(), "status", String.valueOf(vc.getFileStatus(file))));
                }
            }
            map.put("modified", modified);
        }
        result.json(map);
    }

    /**
     * Runs a VCS operation in the background and completes the result.
     */
    private static void runTask(String aTitle, Callable<Object> aTask, Runnable onSuccess, McpResult result, Map<String,Object> successInfo)
    {
        TaskRunner<Object> runner = new TaskRunner<>(aTitle);
        runner.setTaskFunction(aTask);
        runner.setOnSuccess(obj -> McpUtils.guard(result, () -> {
            if (onSuccess != null)
                onSuccess.run();
            result.json(successInfo);
        }).run());
        runner.setOnFailure(e -> result.error(aTitle + " failed: " + e.getMessage()));
        runner.setOnCancelled(() -> result.error(aTitle + " was cancelled"));
        runner.start();
    }

    /**
     * vcs_commit
     */
    private static void vcsCommit(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        VersionControl vc = getVersionControl(proj, true);
        if (!vc.isRemoteWritable())
            throw new McpException("The project's remote is read-only");
        McpUtils.getWorkspace().saveAllFiles();
        List<WebFile> commitFiles = vc.getModifiedFilesForLocalFiles(getFiles(args, proj));
        if (commitFiles.isEmpty()) {
            result.json(Map.of("committed", 0, "note", "No modified files"));
            return;
        }
        String message = args.getString("message", "Update from SnapCode");
        List<String> paths = commitFiles.stream().filter(f -> !f.isDir()).map(WebFile::getPath).toList();
        runTask("Commit files", () -> vc.commitFiles(commitFiles, message, new ActivityMonitor()), null, result,
                Map.of("committed", paths.size(), "files", paths));
    }

    /**
     * vcs_update
     */
    private static void vcsUpdate(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        VersionControl vc = getVersionControl(proj, true);
        List<WebFile> rootFiles = getFiles(args, proj);
        List<WebFile> updateFiles = new ArrayList<>();
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();

        TaskRunner<Object> runner = new TaskRunner<>("Update files");
        runner.setTaskFunction(() -> {
            ActivityMonitor monitor = new ActivityMonitor();
            updateFiles.addAll(vc.getUpdateFilesForLocalFiles(rootFiles, monitor));
            if (!updateFiles.isEmpty())
                vc.updateFiles(updateFiles, monitor);
            return true;
        });
        runner.setOnSuccess(obj -> McpUtils.guard(result, () -> {
            reloadFiles(workspacePane, updateFiles);
            List<String> paths = updateFiles.stream().filter(f -> !f.isDir()).map(WebFile::getPath).toList();
            result.json(Map.of("updated", paths.size(), "files", paths));
        }).run());
        runner.setOnFailure(e -> result.error("Update failed: " + e.getMessage()));
        runner.start();
    }

    /**
     * vcs_discard_changes
     */
    private static void vcsDiscardChanges(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        VersionControl vc = getVersionControl(proj, true);
        List<String> pathArgs = args.getStringList("paths");
        if (pathArgs == null || pathArgs.isEmpty())
            throw new McpException("paths is required");
        List<WebFile> replaceFiles = vc.getModifiedFilesForLocalFiles(getFiles(args, proj));
        if (replaceFiles.isEmpty()) {
            result.json(Map.of("replaced", 0, "note", "No modified files"));
            return;
        }
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        List<String> paths = replaceFiles.stream().filter(f -> !f.isDir()).map(WebFile::getPath).toList();
        runTask("Replace files", () -> vc.replaceFiles(replaceFiles, new ActivityMonitor()),
                () -> reloadFiles(workspacePane, replaceFiles), result, Map.of("replaced", paths.size(), "files", paths));
    }

    /**
     * Reloads files after VCS changed them and triggers build.
     */
    private static void reloadFiles(WorkspacePane workspacePane, List<WebFile> theFiles)
    {
        FilesTool filesTool = workspacePane.getFilesTool();
        for (WebFile file : theFiles)
            if (!file.isDir())
                filesTool.revertFile(file);
        WorkspaceBuilder builder = workspacePane.getWorkspace().getBuilder();
        builder.addAllFilesToBuild();
        builder.buildWorkspaceLater();
        workspacePane.resetLater();
    }

    /**
     * save_to_snapcloud
     */
    private static void saveToSnapCloud(McpArgs args, McpResult result)
    {
        if (SnapCloudPage.getSnapCloudUserUrl() == null)
            throw new McpException("The user is not signed in to SnapCloud. Ask the user to sign in (Account button in the toolbar), then retry.");
        Project proj = McpUtils.getProject(args.getString("project"));
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        ProjectPane projectPane = workspacePane.getProjectPaneForProject(proj);
        if (projectPane == null)
            throw new McpException("No project pane for " + proj.getName());
        workspacePane.getWorkspace().saveAllFiles();
        VersionControlTool vcTool = projectPane.getVersionControlTool();
        vcTool.saveToSnapCloud();

        // Wait for save task to finish
        McpUtils.waitUntil(result, () -> {
                    VersionControl vc = proj.getVersionControl();
                    return vc != null && vc.isIdle() && vc.getRemoteSiteUrl() != null &&
                            vc.getModifiedFilesForLocalFiles(List.of(proj.getRootDir())).isEmpty();
                }, 60000,
                () -> result.json(Map.of("saved", proj.getName(), "remote", String.valueOf(proj.getVersionControl().getRemoteSiteUrlAddress()))),
                () -> result.text("Save to SnapCloud started; it may still be uploading. Check vcs_status."));
    }
}

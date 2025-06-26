package snapcode.project;
import snap.props.PropChange;
import snap.util.*;
import snap.view.ViewUtils;
import java.util.Date;
import java.util.List;

/**
 * This class handles building a Workspace.
 */
public class WorkspaceBuilder {

    // The Workspace
    private Workspace _workspace;

    // Whether to auto build project when files change
    private boolean _autoBuild = true;

    // Whether to auto build project feature is enabled
    private boolean _autoBuildEnabled = true;

    // Whether to add all files to next build
    private boolean _addAllFilesToBuild;

    // Whether to build again after current build
    private boolean _buildAgain;

    // The current build workspace task
    private TaskManagerTask<Boolean> _buildWorkspaceTask;

    // The last build log
    private StringBuffer _buildLogBuffer;

    /**
     * Constructor.
     */
    public WorkspaceBuilder(Workspace workspace)
    {
        super();
        _workspace = workspace;
        _buildLogBuffer = new StringBuffer();
    }

    /**
     * Returns whether to automatically build files when changes are detected.
     */
    public boolean isAutoBuild()  { return _autoBuild; }

    /**
     * Sets whether to automatically build files when changes are detected.
     */
    public void setAutoBuild(boolean aValue)  { _autoBuild = aValue; }

    /**
     * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean isAutoBuildEnabled()  { return isAutoBuild() && _autoBuildEnabled; }

    /**
     * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean setAutoBuildEnabled(boolean aFlag)
    {
        boolean o = _autoBuildEnabled;
        _autoBuildEnabled = aFlag;
        return o;
    }

    /**
     * Returns whether workspace needs to build.
     */
    public boolean isNeedsBuild()
    {
        if (_addAllFilesToBuild)
            return true;
        List<Project> projects = _workspace.getProjects();
        return ListUtils.hasMatch(projects, proj -> proj.getBuilder().isNeedsBuild());
    }

    /**
     * Returns whether workspace is building.
     */
    public boolean isBuilding()  { return _buildWorkspaceTask != null; }

    /**
     * Build workspace.
     */
    public TaskManagerTask<Boolean> buildWorkspace()
    {
        // If already building, register for build again and return build runner
        TaskManagerTask<Boolean> lastRunner = _buildWorkspaceTask;
        if (lastRunner != null) {
            _buildAgain = true;
            return lastRunner;
        }

        // Create/configure build task and start
        _buildWorkspaceTask = (TaskManagerTask<Boolean>) _workspace.getTaskManager().createTask();
        _buildWorkspaceTask.setTaskFunction(() -> buildWorkspaceImpl(_buildWorkspaceTask.getTaskMonitor()));
        _buildWorkspaceTask.setOnFailure(e -> e.printStackTrace());
        _buildWorkspaceTask.setOnFinished(() -> handleBuildWorkspaceFinished(_buildWorkspaceTask));
        _buildWorkspaceTask.getTaskMonitor().addPropChangeListener(this::handleBuildTaskTitleChange, TaskMonitor.TaskTitle_Prop);
        _buildWorkspaceTask.start();

        // Register building
        _workspace.setBuilding(true);

        // Return
        return _buildWorkspaceTask;
    }

    /**
     * Build workspace after delay.
     */
    public void buildWorkspaceLater()
    {
        buildWorkspaceAfterDelay(0);
    }

    /**
     * Build workspace after given delay.
     */
    public void buildWorkspaceAfterDelay(int aDelay)
    {
        ViewUtils.runDelayedCancelPrevious(_buildWorkspaceRunnable, aDelay);
    }

    // Runnable for above to make sure there is only one
    private Runnable _buildWorkspaceRunnable = this::buildWorkspace;

    /**
     * Stops any build in progress.
     */
    public void stopBuild()
    {
        TaskManagerTask<?> buildRunner = _buildWorkspaceTask;
        if (buildRunner != null)
            buildRunner.getTaskRunner().cancel();
        _buildAgain = false;
    }

    /**
     * Removes build files from workspace.
     */
    public void cleanWorkspace()
    {
        boolean old = setAutoBuildEnabled(false);
        List<Project> projects = _workspace.getProjects();
        for (Project project : projects)
            project.getBuilder().cleanProject();
        setAutoBuildEnabled(old);
        _buildLogBuffer.setLength(0);
        _buildLogBuffer.append("Clean workspace - all build files removed");
    }

    /**
     * Build workspace real.
     */
    private boolean buildWorkspaceImpl(TaskMonitor taskMonitor)
    {
        // Reset BuildAgain
        _buildAgain = false;

        // Save all files
        _workspace.saveAllFiles();

        // Handle AddFiles
        if (_addAllFilesToBuild) {
            addAllFilesToBuildImpl();
            _addAllFilesToBuild = false;
        }

        // If no file to build, just return
        if (!isNeedsBuild())
            return _workspace.getBuildIssues().getErrorCount() == 0;

        // Start building
        _buildLogBuffer.setLength(0);
        String dateString = FormatUtils.formatDate(new Date(), "MMM dd, HH:mm:ss");
        _buildLogBuffer.append("Build Started - ").append(dateString).append('\n');
        long buildStartTime = System.currentTimeMillis();

        // Get all projects and declare var for buildSuccess
        List<Project> projects = _workspace.getProjects();
        boolean buildSuccess = true;

        // Build child projects - need to recurse!!!
        for (Project childProject : projects) {

            // Build project
            ProjectBuilder projectBuilder = childProject.getBuilder();
            boolean projBuildSuccess = projectBuilder.buildProject(taskMonitor);
            if (!projBuildSuccess) {
                buildSuccess = false;
                break;
            }
        }

        // Log finished
        String elapsedTimeString = FormatUtils.formatNum((System.currentTimeMillis() - buildStartTime) / 1000d);
        if (buildSuccess)
            _buildLogBuffer.append("Build Completed (").append(elapsedTimeString).append(" seconds)");
        else {
            int errorCount = _workspace.getBuildIssues().getErrorCount();
            _buildLogBuffer.append("Build Failed - " + errorCount + " error(s)");
        }

        // Handle BuildAgain
        if (_buildAgain)
            return buildWorkspaceImpl(taskMonitor);

        // Return
        return buildSuccess;
    }

    /**
     * Called when build is finished.
     */
    protected void handleBuildWorkspaceFinished(TaskManagerTask<Boolean> buildWorkspaceRunner)
    {
        // If this runner is retired, just return
        if (_buildWorkspaceTask != buildWorkspaceRunner)
            return;

        // Clear runner
        _buildWorkspaceTask = null;

        // Set workspace building done
        _workspace.setBuilding(false);
    }

    /**
     * Adds all workspace source files to next build.
     */
    public void addAllFilesToBuild()  { _addAllFilesToBuild = true; }

    /**
     * Returns the build log.
     */
    public StringBuffer getBuildLogBuffer()  { return _buildLogBuffer; }

    /**
     * Adds a build file.
     */
    private void addAllFilesToBuildImpl()
    {
        // Make RootProject.Projects addBuildFiles
        List<Project> projects = _workspace.getProjects();
        for (Project proj : projects) {
            ProjectBuilder projBuilder = proj.getBuilder();
            projBuilder.addBuildFilesAll();
        }
    }

    /**
     * Called when build task title changes.
     */
    private void handleBuildTaskTitleChange(PropChange propChange)
    {
        TaskMonitor taskMonitor = (TaskMonitor) propChange.getSource();
        String taskTitle = taskMonitor.getTaskTitle();
        TaskManagerTask<?> taskRunner = _buildWorkspaceTask;
        if (taskRunner != null && taskMonitor == taskRunner.getTaskMonitor())
            _buildLogBuffer.append(taskTitle).append('\n');
    }
}

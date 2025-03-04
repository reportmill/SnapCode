package snapcode.project;
import snap.util.*;
import snap.view.ViewUtils;
import java.util.Date;
import java.util.List;

/**
 * This class handles building a Workspace.
 */
public class WorkspaceBuilder {

    // The Workspace
    private Workspace  _workspace;

    // Whether to auto build project when files change
    private boolean  _autoBuild = true;

    // Whether to auto build project feature is enabled
    private boolean  _autoBuildEnabled = true;

    // Whether to add all files to next build
    private boolean _addAllFilesToBuild;

    // Whether to build again after current build
    private boolean _buildAgain;

    // A runnable to build file after delay
    private Runnable _buildWorkspaceRun = () -> buildWorkspace();

    // The runner to build workspace
    private TaskRunner<Boolean> _buildWorkspaceRunner;

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
    public boolean isBuilding()
    {
        TaskRunner<Boolean> buildRunner = _buildWorkspaceRunner;
        return buildRunner != null && buildRunner.isActive();
    }

    /**
     * Build workspace.
     */
    public TaskRunner<Boolean> buildWorkspace()
    {
        // If already building, register for build again and return build runner
        TaskRunner<Boolean> lastRunner = _buildWorkspaceRunner;
        if (lastRunner != null) {
            _buildAgain = true;
            return lastRunner;
        }

        // Create task monitor
        TaskMonitor taskMonitor = new TaskMonitor();
        taskMonitor.addPropChangeListener(pc -> taskMonitorTaskTitleChanged(taskMonitor), TaskMonitor.TaskTitle_Prop);

        // Create configure task runner and start
        _buildWorkspaceRunner = new TaskRunner<>(() -> buildWorkspaceImpl(taskMonitor));
        _buildWorkspaceRunner.setOnFailure(e -> e.printStackTrace());
        _buildWorkspaceRunner.setOnFinished(() -> buildWorkspaceFinished(_buildWorkspaceRunner));
        _buildWorkspaceRunner.setMonitor(taskMonitor);
        _buildWorkspaceRunner.start();

        // Return
        return _buildWorkspaceRunner;
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
        ViewUtils.runDelayedCancelPrevious(_buildWorkspaceRun, aDelay);
    }

    /**
     * Stops any build in progress.
     */
    public void stopBuild()
    {
        TaskRunner<?> buildRunner = _buildWorkspaceRunner;
        if (buildRunner != null)
            buildRunner.cancel();
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
        _workspace.setBuilding(true);
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

        // Stop building
        _workspace.setActivity(null);
        _workspace.setBuilding(false);

        // Handle BuildAgain
        if (_buildAgain)
            buildSuccess = buildWorkspaceImpl(taskMonitor);

        // Return
        return buildSuccess;
    }

    /**
     * Called when build is finished.
     */
    protected void buildWorkspaceFinished(TaskRunner<Boolean> buildWorkspaceRunner)
    {
        // If this runner is retired, just return
        if (_buildWorkspaceRunner != buildWorkspaceRunner)
            return;

        // Clear runner
        _buildWorkspaceRunner = null;
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
     * Called when task monitor task changes.
     */
    private void taskMonitorTaskTitleChanged(TaskMonitor taskMonitor)
    {
        String taskTitle = taskMonitor.getTaskTitle();
        TaskRunner<?> taskRunner = _buildWorkspaceRunner;
        if (taskRunner != null && taskMonitor == taskRunner.getMonitor()) {
            _workspace.setActivity(taskTitle);
            _buildLogBuffer.append(taskTitle).append('\n');
        }
    }
}

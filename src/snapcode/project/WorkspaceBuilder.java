package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import java.util.Date;

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
    private BuildWorkspaceRunner _buildWorkspaceRunner;

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
        Project rootProj = _workspace.getRootProject();
        if (rootProj.getBuilder().isNeedsBuild())
            return true;
        Project[] childProjects = rootProj.getProjects();
        return ArrayUtils.hasMatch(childProjects, proj -> proj.getBuilder().isNeedsBuild());
    }

    /**
     * Returns whether workspace is building.
     */
    public boolean isBuilding()
    {
        BuildWorkspaceRunner buildRunner = _buildWorkspaceRunner;
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
        _buildWorkspaceRunner = new BuildWorkspaceRunner();
        _buildWorkspaceRunner.setTaskFunction(() -> buildWorkspaceImpl(taskMonitor));
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
        BuildWorkspaceRunner buildRunner = _buildWorkspaceRunner;
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
        Project rootProj = _workspace.getRootProject();
        ProjectBuilder rootProjBuilder = rootProj.getBuilder();
        rootProjBuilder.cleanProject();
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

        // Get all projects
        Project[] projects = _workspace.getProjects();

        // Start building
        _buildLogBuffer.setLength(0);
        _buildLogBuffer.append("Build Started - " + new Date() + '\n');
        _workspace.setBuilding(true);

        // Track buildSuccess
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
        Project[] projects = _workspace.getProjects();
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

    /**
     * This TaskRunner subclass builds workspace in background thread.
     */
    private class BuildWorkspaceRunner extends TaskRunner<Boolean> {

        /**
         * Constructor.
         */
        public BuildWorkspaceRunner()  { super(); }

        /**
         * Override to reset Workspace Activity/Building properties and clear runner.
         */
        @Override
        protected void finished()
        {
            // If this runner is retired, just return
            if (_buildWorkspaceRunner != this)
                return;

            // Clear runner
            _buildWorkspaceRunner = null;

            // Update Workspace Activity/Building
            _workspace.setActivity("Build Completed");
            _workspace.setBuilding(false);
        }

        /**
         * Override to print exception.
         */
        @Override
        protected void failure(final Exception e)
        {
            e.printStackTrace();
        }
    }
}

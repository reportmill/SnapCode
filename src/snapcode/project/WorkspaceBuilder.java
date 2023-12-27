package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;

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

    // A runnable to build file after delay
    private Runnable _buildWorkspaceRun = () -> buildWorkspace();

    // The runner to build workspace
    private BuildWorkspaceRunner _buildWorkspaceRunner;

    /**
     * Constructor.
     */
    public WorkspaceBuilder(Workspace workspace)
    {
        super();
        _workspace = workspace;
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
        // If already building: Configure new build and interrupt
        if (_buildWorkspaceRunner != null) {

            // Update BuildFilesRunner.[ AddFiles, RunAgain ]
            if (_addAllFilesToBuild)
                _buildWorkspaceRunner._addAllFiles = true;
            _buildWorkspaceRunner._runAgain = true;

            // Stop active build
            Project rootProj = _workspace.getRootProject();
            ProjectBuilder rootProjBuilder = rootProj.getBuilder();
            rootProjBuilder.interruptBuild();
        }

        // If not building: Create BuildFilesRunner and start
        else {
            _buildWorkspaceRunner = new BuildWorkspaceRunner();
            _buildWorkspaceRunner.start();
        }

        _addAllFilesToBuild = false;
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
    private boolean buildWorkspaceImpl(boolean addAllFilesToBuild)
    {
        // Handle AddFiles
        if (addAllFilesToBuild)
            addAllFilesToBuildImpl();

        // Get RootProj and child projects
        Project rootProj = _workspace.getRootProject();
        Project[] childProjects = rootProj.getProjects();
        TaskMonitor taskMonitor = new BuildWorkspaceMonitor();

        // Build child projects
        for (Project childProject : childProjects) {

            // Build project
            ProjectBuilder projectBuilder = childProject.getBuilder();
            boolean projBuildSuccess = projectBuilder.buildProject(taskMonitor);
            if (!projBuildSuccess)
                return false;
        }

        // Build root project
        ProjectBuilder rootBuilder = rootProj.getBuilder();
        return rootBuilder.buildProject(taskMonitor);
    }

    /**
     * Adds all workspace source files to next build.
     */
    public void addAllFilesToBuild()  { _addAllFilesToBuild = true; }

    /**
     * Adds a build file.
     */
    private void addAllFilesToBuildImpl()
    {
        // Make RootProject addBuildFiles
        Project rootProj = _workspace.getRootProject();
        ProjectBuilder rootProjBuilder = rootProj.getBuilder();
        rootProjBuilder.addBuildFilesAll();

        // Make RootProject.Projects addBuildFiles
        Project[] projects = rootProj.getProjects();
        for (Project proj : projects) {
            ProjectBuilder projBuilder = proj.getBuilder();
            projBuilder.addBuildFilesAll();
        }
    }

    /**
     * A TaskMonitor implementation to update workspace Activity and Building properties.
     */
    private class BuildWorkspaceMonitor implements TaskMonitor {

        @Override
        public void beginTask(String aTitle, int theTotalWork)
        {
            _workspace.setActivity(aTitle);
            _workspace.setBuilding(true);
        }
    }

    /**
     * This TaskRunner subclass builds workspace in background thread.
     */
    private class BuildWorkspaceRunner extends TaskRunner<Boolean> {

        // Whether to add all files to build
        private boolean _addAllFiles;

        // Whether to run again
        private boolean _runAgain;

        /**
         * Constructor.
         */
        public BuildWorkspaceRunner()
        {
            super();
            _addAllFiles = _addAllFilesToBuild;
        }

        /**
         * Called to start runner.
         */
        public Boolean run()
        {
            return buildWorkspaceImpl(_addAllFiles);
        }

        /**
         * Called when runner finishes all tasks.
         */
        public void finished()
        {
            // Handle RunAgain request
            if (_runAgain) {
                _runAgain = false;
                start();
            }

            else _buildWorkspaceRunner = null;

            // Update Workspace Activity/Building
            _workspace.setActivity("Build Completed");
            _workspace.setBuilding(false);
        }

        /**
         * Called when runner hits exception.
         */
        public void failure(final Exception e)
        {
            e.printStackTrace();
            ViewUtils.runLater(() -> DialogBox.showExceptionDialog(null, "Build Failed", e));
            _runAgain = false;
        }
    }
}

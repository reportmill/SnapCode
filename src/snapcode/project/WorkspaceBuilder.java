package snapcode.project;
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

    // The runner to build files
    private BuildFilesRunner  _buildFilesRunner;

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
     * Build workspace after delay (with option to add build files).
     */
    public void buildWorkspaceLater(boolean doAddFiles)
    {
        _addAllFilesToBuild |= doAddFiles;
        buildWorkspaceAfterDelay(0);
    }

    /**
     * Build workspace after delay.
     */
    public void buildWorkspaceAfterDelay(int aDelay)
    {
        ViewUtils.runDelayedCancelPrevious(_buildWorkspaceRun, aDelay);
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
     * Build workspace.
     */
    private void buildWorkspace()
    {
        getBuildFilesRunner();
        _addAllFilesToBuild = false;
    }

    /**
     * Build workspace real.
     */
    private void buildWorkspaceImpl(boolean addAllFilesToBuild, TaskMonitor aTM)
    {
        // Handle AddFiles
        if (addAllFilesToBuild)
            addAllFilesToBuild();

        // Get RootProj and child projects
        Project rootProj = _workspace.getRootProject();
        Project[] projects = rootProj.getProjects();
        boolean success = true;

        // Build child projects
        for (Project proj : projects) {

            // Build project
            ProjectBuilder projectBuilder = proj.getBuilder();
            boolean projBuildSuccess = projectBuilder.buildProject(aTM);
            if (!projBuildSuccess) {
                success = false;
                break;
            }
        }

        // Build project
        if (success) {
            ProjectBuilder rootBuilder = rootProj.getBuilder();
            rootBuilder.buildProject(aTM);
        }
    }

    /**
     * Adds a build file.
     */
    private void addAllFilesToBuild()
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
     * Returns the build files runner.
     */
    private synchronized void getBuildFilesRunner()
    {
        // If already building: Configure new build and interrupt
        if (_buildFilesRunner != null) {

            // Update BuildFilesRunner.[ AddFiles, RunAgain ]
            if (_addAllFilesToBuild)
                _buildFilesRunner._addAllFiles = true;
            _buildFilesRunner._runAgain = true;

            // Stop active build
            Project rootProj = _workspace.getRootProject();
            ProjectBuilder rootProjBuilder = rootProj.getBuilder();
            rootProjBuilder.interruptBuild();
        }

        // If not building: Create BuildFilesRunner and start
        else {
            _buildFilesRunner = new BuildFilesRunner();
            _buildFilesRunner.start();
        }
    }

    /**
     * This TaskRunner subclass builds workspace in background thread.
     */
    private class BuildFilesRunner extends TaskRunner<Object> {

        // Whether to add all files to build
        private boolean _addAllFiles;

        // Whether to run again
        private boolean _runAgain;

        /**
         * Constructor.
         */
        public BuildFilesRunner()
        {
            super();
            _addAllFiles = _addAllFilesToBuild;
        }

        /**
         * Called to start runner.
         */
        public Object run()
        {
            buildWorkspaceImpl(_addAllFiles, this);
            return true;
        }

        /**
         * Called when runner starts a new task.
         */
        public void beginTask(final String aTitle, int theTotalWork)
        {
            _workspace.setActivity(aTitle);
            _workspace.setBuilding(true);
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

            else _buildFilesRunner = null;

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

package snapcode.app;
import javakit.project.Project;
import javakit.project.ProjectSet;
import javakit.project.Workspace;
import snap.util.TaskRunner;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snapcode.apptools.ProblemsTool;

/**
 * This class handles building a Workspace.
 */
public class WorkspaceBuilder {

    // The WorkspacePane
    private WorkspacePane  _workspacePane;

    // The Workspace
    private Workspace  _workspace;

    // Whether to auto build project when files change
    private boolean  _autoBuild = true;

    // Whether to auto build project feature is enabled
    private boolean  _autoBuildEnabled = true;

    // The runner to build files
    private BuildFilesRunner  _buildFilesRunner;

    // Runnable for build later
    private Runnable  _buildLaterRun;

    /**
     * Constructor.
     */
    public WorkspaceBuilder(WorkspacePane workspacePane)
    {
        _workspacePane = workspacePane;
        _workspace = workspacePane.getWorkspace();
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
     * Build project.
     */
    public void buildProjectLater(boolean doAddFiles)
    {
        // If not already set, register for buildLater run
        if (_buildLaterRun == null)
            ViewUtils.runLater(_buildLaterRun = () -> buildProject(doAddFiles));
    }

    /**
     * Build project.
     */
    public void buildProject(boolean doAddFiles)
    {
        getBuildFilesRunner(doAddFiles);
        _buildLaterRun = null;
    }

    /**
     * Returns the build files runner.
     */
    private synchronized void getBuildFilesRunner(boolean addBuildFiles)
    {
        if (_buildFilesRunner != null) {
            if (addBuildFiles)
                _buildFilesRunner._addFiles = addBuildFiles;
            _buildFilesRunner._runAgain = true;

            Project rootProj = _workspace.getRootProject();
            rootProj.interruptBuild();
        }

        else {
            _buildFilesRunner = new BuildFilesRunner();
            _buildFilesRunner._addFiles = addBuildFiles;
            _buildFilesRunner.start();
        }
    }

    /**
     * An Runner subclass to build project files in the background.
     */
    public class BuildFilesRunner extends TaskRunner<Object> {

        // Whether to add files and run again
        boolean _addFiles, _runAgain;

        /**
         * BuildFiles.
         */
        public Object run()
        {
            Project rootProj = _workspace.getRootProject();
            ProjectSet projectSet = rootProj.getProjectSet();
            if (_addFiles) {
                _addFiles = false;
                projectSet.addBuildFilesAll();
            }
            projectSet.buildProjects(this);
            return true;
        }

        public void beginTask(final String aTitle, int theTotalWork)
        {
            setActivity(aTitle);
        }

        public void finished()
        {
            boolean runAgain = _runAgain;
            _runAgain = false;
            if (runAgain) start();
            else _buildFilesRunner = null;
            setActivity("Build Completed");
            ViewUtils.runLater(() -> handleBuildCompleted());
        }

        void setActivity(String aStr)
        {
            if (_workspacePane != null)
                _workspacePane.getBrowser().setActivity(aStr);
        }

        public void failure(final Exception e)
        {
            e.printStackTrace();
            ViewUtils.runLater(() -> DialogBox.showExceptionDialog(null, "Build Failed", e));
            _runAgain = false;
        }
    }

    /**
     * Removes build files from the project.
     */
    public void cleanProject()
    {
        boolean old = setAutoBuildEnabled(false);
        Project rootProj = _workspace.getRootProject();
        rootProj.cleanProject();
        setAutoBuildEnabled(old);
    }

    /**
     * Called when a build is completed.
     */
    protected void handleBuildCompleted()
    {
        // If final error count non-zero, show problems pane
        int errorCount = _workspace.getBuildIssues().getErrorCount();
        if (errorCount > 0) {
            SupportTray supportTray = _workspacePane.getSupportTray();
            if (supportTray.getSelTool() instanceof ProblemsTool)
                supportTray.showProblemsTool();
        }

        // If error count zero and SupportTray showing problems, close
        if (errorCount == 0) {
            SupportTray supportTray = _workspacePane.getSupportTray();
            if (supportTray.getSelTool() instanceof ProblemsTool)
                supportTray.hideTools();
        }
    }
}

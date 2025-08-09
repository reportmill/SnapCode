package snapcode.apptools;
import snap.props.PropChange;
import snap.util.ListUtils;
import snap.util.SnapEnv;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.view.*;
import snapcode.app.JavaPage;
import snapcode.project.TaskManagerTask;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.debug.*;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the run/eval UI.
 */
public class RunTool extends WorkspaceTool implements AppListener {

    // The current run config
    private RunConfig _runConfig;

    // The list of recently run apps
    private List<RunApp> _apps = new ArrayList<>();

    // The selected app
    private RunApp _selApp;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    // Whether to run apps in snapcode process
    private static boolean _runInSnapCodeProcess;

    /**
     * Constructor.
     */
    public RunTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the current RunConfig.
     */
    public RunConfig getRunConfig()
    {
        if (_runConfig != null) return _runConfig;
        return _runConfig = findRunConfig();
    }

    /**
     * Finds a run config.
     */
    private RunConfig findRunConfig()
    {
        // Search all projects for first that defines a main class name
        List<Project> projects = _workspace.getProjects();
        for (Project project : projects) {
            String mainClassName = project.getBuildFile().getMainClassName();
            if (mainClassName != null) {
                RunConfig runConfig = RunConfig.createRunConfigForWorkspaceAndClassName(_workspace, mainClassName);
                WebFile mainJavaFile = runConfig.getMainJavaFile();
                if (mainJavaFile != null && RunToolUtils.isJavaFileWithMain(runConfig.getMainJavaFile()))
                    return runConfig;
            }
        }

        // See if selected file is Java file
        WebFile selFile = getSelFile();
        if (selFile != null && RunToolUtils.isJavaFileWithMain(selFile))
            return RunConfig.createRunConfigForJavaFile(selFile);

        // Return not found
        return null;
    }

    /**
     * Returns the list of apps.
     */
    public List<RunApp> getApps()  { return _apps; }

    /**
     * Returns the number of apps.
     */
    public int getAppCount()  { return _apps.size(); }

    /**
     * Returns the app at given index.
     */
    public RunApp getApp(int anIndex)  { return _apps.get(anIndex); }

    /**
     * Adds a new app.
     */
    public void addApp(RunApp runApp)
    {
        // Remove procs that are terminated and procs beyond limit
        for (RunApp p : _apps.toArray(new RunApp[0]))
            if (p.isTerminated())
                removeApp(p);

        if (getAppCount() + 1 > _procLimit) {
            RunApp proc = getApp(0);
            proc.terminate();
            removeApp(proc);
        }

        // Add new proc
        _apps.add(runApp);
        runApp.addListener(this);
        resetLater();
        _workspacePane.getToolBar().resetLater();
    }

    /**
     * Removes the app at given index.
     */
    public void removeApp(int anIndex)
    {
        RunApp runApp = _apps.remove(anIndex);
        runApp.removeListener(this);
    }

    /**
     * Removes the given app.
     */
    public void removeApp(RunApp aProcess)
    {
        int index = ListUtils.indexOfId(_apps, aProcess);
        if (index >= 0)
            removeApp(index);
    }

    /**
     * Returns the selected app.
     */
    public RunApp getSelApp()  { return _selApp; }

    /**
     * Sets the selected app.
     */
    public void setSelApp(RunApp aProc)
    {
        if (aProc == _selApp) return;
        _selApp = aProc;

        resetLater();
        runLater(() -> resetConsoleView());
        DebugTool debugTool = getDebugTool();
        debugTool.resetLater();
        _workspacePane.getToolBar().resetLater();
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getSelDebugApp()  { return _selApp instanceof DebugApp ? (DebugApp) _selApp : null; }

    /**
     * Returns the DebugTool.
     */
    public DebugTool getDebugTool()  { return _workspaceTools.getToolForClass(DebugTool.class); }

    /**
     * Runs app for selected file.
     */
    public void runAppForSelFile(boolean isDebug)
    {
        // Get selected file (just return if not runnable)
        WebFile selFile = getSelFile();
        if (!RunToolUtils.isJavaFileWithMain(selFile))
            return;

        // Create default run config
        _runConfig = RunConfig.createRunConfigForJavaFile(selFile);

        // Run app
        runApp(isDebug);
    }

    /**
     * Runs app.
     */
    public void runApp(boolean isDebug)
    {
        RunConfig runConfig = getRunConfig();
        runAppForConfig(runConfig, isDebug);
    }

    /**
     * Runs app for given RunConfig.
     */
    public void runAppForConfig(RunConfig runConfig, boolean isDebug)
    {
        // Automatically save all files - this is getting done twice for build, maybe this should call build tool
        _workspace.saveAllFiles();

        // If isDebug in browser, complain and run normal
        if (isDebug && SnapEnv.isWebVM) {
            String msg = "Debug only currently available on desktop.\nBrowser support coming soon.\nExecuting normal run instead";
            DialogBox.showWarningDialog(_workspacePane.getUI(), "Debug Support Coming Soon", msg);
            isDebug = false;
        }

        // Create RunApp and run
        RunApp runApp = RunToolUtils.createRunAppForConfig(this, runConfig, isDebug);
        runApp(runApp);
    }

    /**
     * Runs the given app.
     */
    private void runApp(RunApp runApp)
    {
        // If no app, just return
        if (runApp == null)
            return;

        // Clear display
        clearConsole();

        // If workspace needs build, trigger build
        WorkspaceBuilder workspaceBuilder = _workspace.getBuilder();
        if (workspaceBuilder.isNeedsBuild() || workspaceBuilder.isBuilding()) {
            TaskManagerTask<Boolean> buildRunner = workspaceBuilder.buildWorkspace();
            buildRunner.setOnSuccess(success -> runAppBuildFinished(runApp, success));
        }

        // Otherwise, just launch
        else runAppImpl(runApp);
    }

    /**
     * Called after build finished.
     */
    private void runAppBuildFinished(RunApp runApp, boolean noErrors)
    {
        // If no errors, just run app
        if (noErrors)
            runAppImpl(runApp);

        // Otherwise, show build tool
        else runLater(_workspaceTools.getBuildTool()::showToolAutomatically);
    }

    /**
     * Runs app for selected file.
     */
    private void runAppImpl(RunApp runApp)
    {
        // Add app to list and select
        addApp(runApp);
        setSelApp(runApp);

        // Start app
        runApp.exec();

        // Reset UI
        resetLater();
        _workspacePane.getToolBar().resetLater();

        // Auto-show tool
        WorkspaceTool appTool = runApp instanceof DebugApp ? getDebugTool() : this;
        appTool.showToolAutomatically();
    }

    /**
     * Reset console view.
     */
    protected void resetConsoleView()
    {
        RunApp selApp = getSelApp();
        if (selApp == null)
            return;

        // Set selApp.OutputText in ConsoleText
        View consoleView = selApp.getConsoleView();
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        if (scrollView.getContent() != consoleView)
            scrollView.setContent(consoleView);
        scrollView.setFillWidth(consoleView != null && consoleView.isGrowWidth());

        resetLater();
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    public void handleBreakpointsChange(PropChange pc)
    {
        if (pc.getPropName() != Breakpoints.ITEMS_PROP) return;

        // Get processes
        List<RunApp> processes = getApps();

        // Handle Breakpoint added
        Breakpoint addedBreakpoint = (Breakpoint) pc.getNewValue();
        if (addedBreakpoint != null) {

            // Tell active processes about breakpoint change
            for (RunApp rp : processes)
                rp.addBreakpoint(addedBreakpoint);
        }

        // Handle Breakpoint removed
        else {

            Breakpoint removedBreakpoint = (Breakpoint) pc.getOldValue();

            // Make current JavaPage.TextArea resetLater
            WebPage page = getBrowser().getPageForURL(removedBreakpoint.getFile().getUrl());
            if (page instanceof JavaPage)
                ((JavaPage) page).getTextPane().handleBuildIssueOrBreakPointMarkerChange();

            // Tell active processes about breakpoint change
            for (RunApp rp : processes)
                rp.removeBreakpoint(removedBreakpoint);
        }
    }

    /**
     * RunProc.Listener method - called when process starts.
     */
    public void appStarted(RunApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appStarted(aProc);
    }

    /**
     * RunProc.Listener method - called when process is paused.
     */
    public void appPaused(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appPaused(aProc);
    }

    /**
     * RunProc.Listener method - called when process is continued.
     */
    public void appResumed(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appResumed(aProc);
    }

    /**
     * RunProc.Listener method - called when process ends.
     */
    public void appExited(RunApp runApp)
    {
        if (runApp instanceof DebugApp) {
            DebugTool debugTool = getDebugTool();
            debugTool.appExited(runApp);
        }

        // Reset UI
        resetLater();
        _workspacePane.getToolBar().resetLater();

        // Auto-hide tool
        WorkspaceTool appTool = runApp instanceof DebugApp ? getDebugTool() : this;
        appTool.hideToolAutomaticallyAfterDelay(7000);
    }

    /**
     * Called when run app console view changes.
     */
    public void handleConsoleViewChange(RunApp runApp)
    {
        runLater(this::resetConsoleView);
    }

    /**
     * Called when DebugApp gets notice of things like VM start/death, thread start/death, breakpoints, etc.
     */
    public void processDebugEvent(DebugApp aProc, DebugEvent anEvent)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.processDebugEvent(aProc, anEvent);
    }

    /**
     * RunProc.Listener method - called when stack frame changes.
     */
    public void frameChanged(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.frameChanged(aProc);
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()
    {
        RunApp selApp = getSelApp();
        return selApp != null && selApp.isRunning();
    }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        // Auto-hide tool (if was shown automatically)
        RunApp selApp = getSelApp();
        WorkspaceTool appTool = selApp instanceof DebugApp ? getDebugTool() : this;
        appTool.hideToolAutomatically();

        // Terminate app
        if (selApp != null)
            selApp.terminate();
    }

    /**
     * Clear eval values.
     */
    public void clearConsole()
    {
        // Tell selected app to clear console
        RunApp selApp = getSelApp();
        if (selApp != null)
            selApp.clearConsole();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Get ScrollView and config
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        scrollView.setBorder(null);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update TerminateButton
        //setViewEnabled("TerminateButton", isRunning());

        // Update SwapConsoleButton
        RunApp selApp = getSelApp();
        boolean hasAltConsole = selApp != null && selApp.getAltConsoleView() != null;
        setViewVisible("SwapConsoleButton", hasAltConsole);
        if (hasAltConsole) {
            boolean isAltConsole = selApp.getConsoleView() == selApp.getAltConsoleView();
            String swapConsoleButtonTitle = isAltConsole ? "Show System Console" : "Show App Console";
            setViewText("SwapConsoleButton", swapConsoleButtonTitle);
        }

        // Update RunInSnapCodeProcessMenuItem
        MenuButton menuButton = getView("MenuButton", MenuButton.class);
        CheckBoxMenuItem runInSnapCodeProcessMenuItem = (CheckBoxMenuItem) menuButton.getMenuItemForName("RunInSnapCodeProcessMenuItem");
        runInSnapCodeProcessMenuItem.setSelected(isRunInSnapCodeProcess());
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle RunButton, TerminateButton
            case "RunButton" -> runApp(false);
            case "TerminateButton" -> cancelRun();

            // Handle SwapConsoleButton
            case "SwapConsoleButton" -> {
                RunApp selApp = getSelApp();
                boolean isAltConsole = selApp.getConsoleView() == selApp.getAltConsoleView();
                View swapConsoleView = isAltConsole ? selApp.getConsoleTextView() : selApp.getAltConsoleView();
                selApp.setConsoleView(swapConsoleView);
            }

            // Handle ClearButton
            case "ClearButton", "ClearConsoleMenuItem" -> clearConsole();

            // Handle InputTextField: Show input string, add to runner input and clear text
            case "InputTextField" -> {

                // Get InputTextField string and send to current process
                String inputString = anEvent.getStringValue();
                RunApp selApp = getSelApp();
                if (selApp != null)
                    selApp.sendInput(inputString + '\n');

                // Clear InputTextField
                setViewValue("InputTextField", null);
            }

            // Handle RunInSnapCodeProcessMenuItem
            case "RunInSnapCodeProcessMenuItem" -> setRunInSnapCodeProcess(!isRunInSnapCodeProcess());

            // Do normal version
            default -> super.respondUI(anEvent);
        }
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Console"; }

    /**
     * Override to terminate app if running.
     */
    @Override
    protected boolean workspaceIsClosing()
    {
        if (isRunning())
            cancelRun();
        return true;
    }

    /**
     * Override to clear RunConfig if matching project.
     */
    @Override
    protected void projectIsClosing(Project aProject)
    {
        if (_runConfig != null) {
            Project runConfigProject = _runConfig.getProject();
            if (runConfigProject == null || runConfigProject == aProject)
                _runConfig = null;
        }
    }

    /**
     * Returns whether to run processes in SnapCode.
     */
    public static boolean isRunInSnapCodeProcess()  { return _runInSnapCodeProcess; }

    /**
     * Sets whether to run processes in SnapCode.
     */
    public static void setRunInSnapCodeProcess(boolean aValue)
    {
        _runInSnapCodeProcess = aValue;
        //Prefs.getDefaultPrefs().setValue(RUN_IN_SNAPCODE_PROCESS_KEY, _runInSnapCodeProcess);
    }
}

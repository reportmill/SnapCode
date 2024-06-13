package snapcode.apptools;
import snap.props.PropChange;
import snap.util.ListUtils;
import snap.util.TaskRunner;
import snap.web.WebFile;
import snap.view.*;
import snapcode.app.JavaPage;
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

    // The list of recently run apps
    private List<RunApp> _apps = new ArrayList<>();

    // The selected app
    private RunApp _selApp;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    /**
     * Constructor.
     */
    public RunTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
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
        // Get SelFile - if not main class file, try to replace with defined main class file
        WebFile selFile = getSelFile();

        // If selFile not main class file, try to replace with defined main class file
        if (selFile == null || !RunToolUtils.isJavaFileWithMain(selFile)) {

            // Get default or last main class file - if found, select and run again
            WebFile mainJavaFile = RunToolUtils.getMainJavaFile(this);
            if (mainJavaFile != null && RunToolUtils.isJavaFileWithMain(mainJavaFile)) {
                setSelFile(mainJavaFile);
                runDelayed(() -> runAppForSelFile(isDebug), 800);
                return;
            }
        }

        runConfigOrFile(null, selFile, isDebug);
    }

    /**
     * Runs app for given RunConfig or file.
     */
    public void runConfigOrFile(RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Automatically save all files - this is getting done twice for build, maybe this should call build tool
        _workspace.saveAllFiles();

        // Create RunApp and run
        RunApp runApp = RunToolUtils.createRunAppForConfigOrFile(this, aConfig, aFile, isDebug);
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

        // Show tool
        boolean isDebug = runApp instanceof DebugApp;
        _workspaceTools.showToolForClass(isDebug ? DebugTool.class : RunTool.class);

        // Clear display
        clearConsole();

        // If workspace needs build, trigger build
        WorkspaceBuilder workspaceBuilder = _workspace.getBuilder();
        if (workspaceBuilder.isNeedsBuild() || workspaceBuilder.isBuilding()) {
            TaskRunner<Boolean> buildRunner = workspaceBuilder.buildWorkspace();
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
        else _workspaceTools.showToolForClass(BuildTool.class);
    }

    /**
     * Runs app for selected file.
     */
    private void runAppImpl(RunApp runApp)
    {
        execProc(runApp);
        resetLater();
        _workspacePane.getToolBar().resetLater();
    }

    /**
     * Executes a proc and adds it to list.
     */
    public void execProc(RunApp runApp)
    {
        addApp(runApp);
        setSelApp(runApp);
        runApp.exec();
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

        resetLater();
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    public void breakpointsDidChange(PropChange pc)
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
            WebPage page = getBrowser().getPageForURL(removedBreakpoint.getFile().getURL());
            if (page instanceof JavaPage)
                ((JavaPage) page).getTextPane().buildIssueOrBreakPointMarkerChanged();

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
    public void appExited(RunApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appExited(aProc);

        // Reset UI
        resetLater();
        _workspacePane.getToolBar().resetLater();
    }

    /**
     * Called when run app console view changes.
     */
    public void consoleViewDidChange(RunApp runApp)
    {
        runLater(() -> resetConsoleView());
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
        RunApp selApp = getSelApp();
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
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunButton, TerminateButton
        if (anEvent.equals("RunButton"))
            runAppForSelFile(false);
        else if (anEvent.equals("TerminateButton"))
            cancelRun();

        // Handle SwapConsoleButton
        else if (anEvent.equals("SwapConsoleButton")) {
            RunApp selApp = getSelApp();
            boolean isAltConsole = selApp.getConsoleView() == selApp.getAltConsoleView();
            View swapConsoleView = isAltConsole ? selApp.getConsoleTextView() : selApp.getAltConsoleView();
            selApp.setConsoleView(swapConsoleView);
        }

        // Handle ClearButton
        else if (anEvent.equals("ClearButton"))
            clearConsole();

        // Handle InputTextField: Show input string, add to runner input and clear text
        else if (anEvent.equals("InputTextField")) {

            // Get InputTextField string and send to current process
            String inputString = anEvent.getStringValue();
            RunApp selApp = getSelApp();
            if (selApp != null)
                selApp.sendInput(inputString + '\n');

            // Clear InputTextField
            setViewValue("InputTextField", null);
        }

        // Do normal version
        else super.respondUI(anEvent);
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
}

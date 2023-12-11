package snapcode.apptools;
import snap.props.PropChange;
import snap.util.FilePathUtils;
import snap.util.ListUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snap.view.*;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.debug.*;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import java.io.File;
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

    // The view that shows when there is an extended run
    private View  _extendedRunView;

    // The view that shows when there is cancelled run
    private View  _cancelledRunView;

    // The last executed file
    private static WebFile _lastRunFile;

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
    }

    /**
     * Removes the app at given index.
     */
    public void removeApp(int anIndex)
    {
        _apps.remove(anIndex);
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
     * Run application.
     */
    public void runDefaultConfig(boolean withDebug)
    {
        WebSite site = getRootSite();
        RunConfig config = RunConfigs.get(site).getRunConfig();
        runConfigOrFile(config, null, withDebug);
    }

    /**
     * Run application.
     */
    public void runConfigForName(String configName, boolean withDebug)
    {
        RunConfigs runConfigs = RunConfigs.get(getRootSite());
        RunConfig runConfig = runConfigs.getRunConfig(configName);
        if (runConfig != null) {
            runConfigs.getRunConfigs().remove(runConfig);
            runConfigs.getRunConfigs().add(0, runConfig);
            runConfigs.writeFile();
            runConfigOrFile(runConfig, null, withDebug);
        }
    }

    /**
     * Runs a given RunConfig or file as a separate process.
     */
    public void runConfigOrFile(RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Automatically save all files
        FilesTool filesTool = _workspaceTools.getFilesTool();
        filesTool.saveAllFiles();

        // Get site and RunConfig (if available)
        WebSite site = getRootSite();
        RunConfig config = aConfig != null || aFile != null ? aConfig : RunConfigs.get(site).getRunConfig();

        // Get file
        WebFile runFile = aFile;
        if (runFile == null && config != null)
            runFile = site.createFileForPath(config.getMainFilePath(), false);
        if (runFile == null)
            runFile = _lastRunFile;
        if (runFile == null)
            runFile = getSelFile();

        // Try to replace file with project file
        Project proj = Project.getProjectForFile(runFile);
        if (proj == null) {
            System.err.println("DebugTool: not project file: " + runFile);
            return;
        }

        // Get class file for given file (should be JavaFile)
        ProjectFiles projectFiles = proj.getProjectFiles();
        WebFile classFile;
        if (runFile.getType().equals("java"))
            classFile = projectFiles.getClassFileForJavaFile(runFile);

            // Try generic way to get class file
        else classFile = projectFiles.getBuildFile(runFile.getPath(), false, runFile.isDir());

        // If ClassFile found, set run file
        if (classFile != null)
            runFile = classFile;

        // Set last run file
        _lastRunFile = runFile;

        // Run/debug file
        String[] runArgs = getRunArgs(proj, config, runFile, isDebug);
        WebURL url = runFile.getURL();
        runAppForArgs(runArgs, url, isDebug);
    }

    /**
     * Runs the provided file as straight app.
     */
    public void runAppForArgs(String[] args, WebURL aURL, boolean isDebug)
    {
        // Print run command to console
        String commandLineStr = String.join(" ", args);
        if (isDebug)
            commandLineStr = "debug " + commandLineStr;
        System.err.println(commandLineStr);

        // Get process
        RunApp proc;
        if (!isDebug)
            proc = new RunAppBin(this, aURL, args);

            // Handle isDebug: Create DebugApp with Workspace.Breakpoints
        else {
            proc = new DebugApp(this, aURL, args);
            Breakpoints breakpointsHpr = _workspace.getBreakpoints();
            Breakpoint[] breakpoints = breakpointsHpr.getArray();
            for (Breakpoint breakpoint : breakpoints)
                proc.addBreakpoint(breakpoint);
        }

        // Create RunApp and exec
        execProc(proc);
    }

    /**
     * Executes a proc and adds it to list.
     */
    public void execProc(RunApp runApp)
    {
        setSelApp(null);
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

        // Clear extended run UI if needed
        ViewUtils.runLater(() -> setShowExtendedRunUI(false));
    }

    /**
     * Called when run app console view changes.
     */
    public void consoleViewDidChange(RunApp runApp)
    {
        resetConsoleView();
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
     * Returns a source URL for path.
     */
    public String getSourceURL(String aPath)
    {
        if (aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
            return "https://reportmill.com/jars/8u05/src.zip!" + aPath;
        if (aPath.startsWith("/javafx/"))
            return "https://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;

        // Get root project
        WebSite rootSite = getRootSite();
        Project project = Project.getProjectForSite(rootSite);
        if (project == null)
            return aPath;

        // Look in project
        WebFile file = project.getSourceFile(aPath, false, false);
        if (file != null)
            return file.getUrlString();

        // Look in child projects
        Project[] projects = project.getProjects();
        for (Project proj : projects) {
            file = proj.getSourceFile(aPath, false, false);
            if (file != null)
                return file.getUrlString();
        }

        // Return not found
        return aPath;
    }

    /**
     * Returns an array of args for given config and file.
     */
    private static String[] getRunArgs(Project aProj, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            commands.add(javaCmdPath);
        }

        // Get Class path and add to list
        String[] classPaths = aProj.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = aProj.getClassNameForFile(aFile);
        commands.add(className);

        // Add App Args
        String appArgs = aConfig != null ? aConfig.getAppArgs() : null;
        if (appArgs != null && appArgs.length() > 0)
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
    }

    /**
     * Runs Java code.
     */
    public void runApp()
    {
        // Clear display
        clearConsole();

        // Build app
        BuildTool buildTool = _workspaceTools.getToolForClass(BuildTool.class);
        boolean success = buildTool.buildApp();
        if (!success)
            return;

        // Run app
        WebFile selFile = getSelFile();
        if (selFile == null)
            return;

        // Get args
        Project proj = getProject();
        String className = proj.getClassNameForFile(selFile);
        String[] args = { className };

        // Create app and run
        RunApp runApp = new RunAppSrc(this, selFile.getURL(), args);
        execProc(runApp);

        // Check back in half a second to see if we need to show progress bar
        ViewUtils.runDelayed(() -> handleExtendedRun(), 500);

        // Reset UI
        resetLater();
    }

    /**
     * Called when a run is taking a long time.
     */
    protected void handleExtendedRun()
    {
        RunApp selApp = getSelApp();
        if (selApp == null || !selApp.isRunning())
            return;

        // Show extended run UI
        setShowExtendedRunUI(true);
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

        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void clearConsole()
    {
        // Remove extended run or cancelled run UI if set
        setShowExtendedRunUI(false);
        setShowCancelledRunUI(false);

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

        // Get ExtendedRunView
        _extendedRunView = getView("ExtendedRunView");
        _extendedRunView.setVisible(false);
        getView("ProgressBar", ProgressBar.class).setIndeterminate(true);

        // Get CancelledRunView
        _cancelledRunView = getView("CancelledRunView");
        _cancelledRunView.setVisible(false);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update RunButton, TerminateButton
        setViewEnabled("RunButton", !isRunning());
        setViewEnabled("TerminateButton", isRunning());

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
            runApp();
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
     * Sets whether ExtendedRunView is showing.
     */
    protected void setShowExtendedRunUI(boolean aValue)
    {
        if (_extendedRunView != null)
            _extendedRunView.setVisible(aValue);
    }

    /**
     * Sets whether CancelledRunView is showing.
     */
    protected void setShowCancelledRunUI(boolean aValue)
    {
        if (_cancelledRunView != null)
            _cancelledRunView.setVisible(aValue);
        if (aValue) {
            String text = "Last run cancelled";
            setViewText("CancelledRunLabel", text);
        }
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Console"; }
}

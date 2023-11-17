package snapcode.apptools;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.util.FilePathUtils;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.debug.*;
import snapcode.javatext.JavaTextArea;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.app.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This project tool class handles running/debugging a process.
 */
public class DebugTool extends WorkspaceTool implements RunApp.AppListener {

    // The list of recently run apps
    private List<RunApp>  _apps = new ArrayList<>();

    // The selected app
    private RunApp _selApp;

    // Whether Console needs to be reset
    private boolean _resetConsole;

    // The DebugFramesPane
    private DebugFramesPane _debugFramesPane;

    // The DebugVarsPane
    private DebugVarsPane _debugVarsPane;

    // The DebugExprsPane
    private DebugExprsPane _debugExprsPane;

    // The file that currently has the ProgramCounter
    private WebFile  _progCounterFile;

    // The current ProgramCounter line
    private int  _progCounterLine;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    // The last executed file
    private static WebFile  _lastRunFile;

    /**
     * Constructor.
     */
    public DebugTool(WorkspacePane workspacePane)
    {
        super(workspacePane);

        // Create parts
        _debugFramesPane = new DebugFramesPane(this);
        _debugVarsPane = new DebugVarsPane(this);
        _debugExprsPane = new DebugExprsPane(this);
    }

    /**
     * Returns the list of processes.
     */
    public List<RunApp> getProcs()  { return _apps; }

    /**
     * Returns the number of processes.
     */
    public int getProcCount()  { return _apps.size(); }

    /**
     * Returns the process at given index.
     */
    public RunApp getProc(int anIndex)  { return _apps.get(anIndex); }

    /**
     * Adds a new process.
     */
    public void addProc(RunApp aProc)
    {
        // Remove procs that are terminated and procs beyond limit
        for (RunApp p : _apps.toArray(new RunApp[0]))
            if (p.isTerminated())
                removeProc(p);

        if (getProcCount() + 1 > _procLimit) {
            RunApp proc = getProc(0);
            proc.terminate();
            removeProc(proc);
        }

        // Add new proc
        _apps.add(aProc);
        aProc.setListener(this);
        resetLater();
    }

    /**
     * Removes a process.
     */
    public void removeProc(int anIndex)
    {
        _apps.remove(anIndex);
    }

    /**
     * Removes a process.
     */
    public void removeProc(RunApp aProcess)
    {
        int index = ListUtils.indexOfId(_apps, aProcess);
        if (index >= 0)
            removeProc(index);
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
        _resetConsole = true;
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getSelDebugApp()  { return _selApp instanceof DebugApp ? (DebugApp) _selApp : null; }

    /**
     * Returns the program counter for given file.
     */
    public int getProgramCounter(WebFile aFile)
    {
        return aFile == _progCounterFile ? _progCounterLine : -1;
    }

    /**
     * Sets the program counter file, line.
     */
    public void setProgramCounter(WebFile aFile, int aLine)
    {
        // Store old value, set new value
        WebFile oldPCF = _progCounterFile;
        _progCounterFile = aFile;
        _progCounterLine = aLine;

        // Reset JavaPage.TextArea for old/new files
        WebPage page = oldPCF != null ? getBrowser().getPageForURL(oldPCF.getURL()) : null;
        if (page instanceof JavaPage)
            ((JavaPage) page).getTextArea().repaint();
        page = _progCounterFile != null ? getBrowser().getPageForURL(_progCounterFile.getURL()) : null;
        if (page instanceof JavaPage)
            ((JavaPage) page).getTextArea().repaint();
    }

    /**
     * Returns whether selected process is terminated.
     */
    public boolean isTerminated()
    {
        RunApp app = getSelApp();
        return app == null || app.isTerminated();
    }

    /**
     * Returns whether selected process can be paused.
     */
    public boolean isPausable()
    {
        DebugApp app = getSelDebugApp();
        return app != null && app.isRunning();
    }

    /**
     * Returns whether selected process is paused.
     */
    public boolean isPaused()
    {
        DebugApp app = getSelDebugApp();
        return app != null && app.isPaused();
    }

    /**
     * Returns the processes pane.
     */
    public DebugFramesPane getProcPane()  { return _debugFramesPane; }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()  { return _debugVarsPane; }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()  { return _debugExprsPane; }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _workspaceTools.getToolForClass(RunConsole.class); }

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
            proc = new RunApp(aURL, args);

        // Handle isDebug: Create DebugApp with Workspace.Breakpoints
        else {
            proc = new DebugApp(aURL, args);
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
    public void execProc(RunApp aProc)
    {
        setSelApp(null);
        addProc(aProc);
        setSelApp(aProc);
        ToolTray bottomTray = _workspaceTools.getBottomTray();
        bottomTray.setSelToolForClass(RunConsole.class);
        aProc.exec();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Get main col view
        ColView mainColView = getUI(ColView.class);

        // Config RowView
        RowView rowView = getView("RowView", RowView.class);
        rowView.setBorder(Color.GRAY8, 1);

        // Add ProcPane
        View procPaneUI = _debugFramesPane.getUI();
        CollapseView processToolUI = new CollapseView("Frames", procPaneUI);
        mainColView.addChild(processToolUI);

        // Add DebugVars
        View debugVarsUI = _debugVarsPane.getUI();
        CollapseView debugVarsUIBox = new CollapseView("Variables", debugVarsUI);
        debugVarsUIBox.setCollapsed(true);
        mainColView.addChild(debugVarsUIBox);

        // Add DebugExprs
        View debugExprsUI = _debugExprsPane.getUI();
        CollapseView debugExprsUIBox = new CollapseView("Expressions", debugExprsUI);
        debugExprsUIBox.setCollapsed(true);
        mainColView.addChild(debugExprsUIBox);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        boolean paused = isPaused();
        boolean pausable = isPausable();
        setViewEnabled("ResumeButton", paused);
        setViewEnabled("SuspendButton", pausable);
        setViewEnabled("TerminateButton", !isTerminated());
        setViewEnabled("StepIntoButton", paused);
        setViewEnabled("StepOverButton", paused);
        setViewEnabled("StepReturnButton", paused);
        setViewEnabled("RunToLineButton", paused);

        _debugFramesPane.resetLater();
        _debugVarsPane.resetLater();
        _debugExprsPane.resetLater();

        // Reset Console
        if (_resetConsole) {
            _resetConsole = false;
            getRunConsole().clear();
            RunApp proc = getSelApp();
            if (proc != null) {
                for (RunApp.Output out : proc.getOutput()) {
                    if (out.isErr())
                        appendErr(proc, out.getString());
                    else appendOut(proc, out.getString());
                }
            }
        }
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // The Selected App, DebugApp
        RunApp selApp = getSelApp();
        DebugApp debugApp = getSelDebugApp();

        // Handle DebugButton
        if (anEvent.equals("DebugButton"))
            runDefaultConfig(true);

        // Handle ResumeButton, SuspendButton, TerminateButton
        else if (anEvent.equals("ResumeButton"))
            debugApp.resume();
        else if (anEvent.equals("SuspendButton"))
            debugApp.pause();
        else if (anEvent.equals("TerminateButton"))
            selApp.terminate();

        // Handle StepIntoButton, StepOverButton, StepReturnButton, RunToLineButton
        else if (anEvent.equals("StepIntoButton"))
            debugApp.stepIntoLine();
        else if (anEvent.equals("StepOverButton"))
            debugApp.stepOverLine();
        else if (anEvent.equals("StepReturnButton"))
            debugApp.stepOut();
        else if (anEvent.equals("RunToLineButton")) {
            WebPage page = getBrowser().getPage();
            JavaPage jpage = page instanceof JavaPage ? (JavaPage) page : null;
            if (jpage == null) return;
            JavaTextArea tarea = jpage.getTextArea();
            WebFile file = jpage.getFile();
            int line = tarea.getSel().getStartLine().getIndex();
            debugApp.runToLine(file, line);
        }

        // Do normal version
        else super.respondUI(anEvent);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Run / Debug"; }

    /**
     * Called when Workspace.BreakPoints change.
     */
    public void breakpointsDidChange(PropChange pc)
    {
        if (pc.getPropName() != Breakpoints.ITEMS_PROP) return;

        // Get processes
        List<RunApp> processes = getProcs();

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
        resetLater();
        _debugFramesPane.updateProcTreeLater();
    }

    /**
     * RunProc.Listener method - called when process is paused.
     */
    public void appPaused(DebugApp aProc)
    {
        resetLater();
        _debugFramesPane.updateProcTreeLater();
    }

    /**
     * RunProc.Listener method - called when process is continued.
     */
    public void appResumed(DebugApp aProc)
    {
        resetLater();
        _debugFramesPane.updateProcTreeLater();
        setProgramCounter(null, -1);
    }

    /**
     * RunProc.Listener method - called when process ends.
     */
    public void appExited(RunApp aProc)
    {
        // Only run on event thread
        if (!isEventThread()) {
            runLater(() -> appExited(aProc));
            return;
        }

        // Update proc items and reset UI
        resetLater();
        _workspacePane.resetLater();

        // If debug app, reset Debug vars, expressions and current line counter
        if (aProc instanceof DebugApp) {
            getDebugVarsPane().resetVarTable();
            getDebugExprsPane().resetVarTable();
            setProgramCounter(null, 1);
        }
    }

    /**
     * Called when DebugApp gets notice of things like VM start/death, thread start/death, breakpoints, etc.
     */
    public void processDebugEvent(DebugApp aProc, DebugEvent anEvent)
    {
        switch (anEvent.getType()) {

            // Handle ThreadStart, ThreadDeatch
            case ThreadStart:
            case ThreadDeath: _debugFramesPane.updateProcTreeLater(); break;

            // Handle LocationTrigger
            case LocationTrigger: {
                runLater(() -> handleLocationTrigger());
                break;
            }
        }
    }

    /**
     * Called when Debug LocationTrigger is encountered.
     */
    protected void handleLocationTrigger()
    {
        getEnv().activateApp(getUI());
        getDebugVarsPane().resetVarTable();
        getDebugExprsPane().resetVarTable();
    }

    /**
     * RunProc.Listener method - called when stack frame changes.
     */
    public void frameChanged(DebugApp aProc)
    {
        // Only run on event thread
        if (!isEventThread()) {
            runLater(() -> frameChanged(aProc));
            return;
        }

        // Make DebugVarsPane visible and updateVarTable
        _workspaceTools.showToolForClass(DebugTool.class);

        DebugFrame frame = aProc.getFrame();
        if (frame == null) return;
        getDebugVarsPane().resetVarTable(); // This used to be before short-circuit to clear trees
        getDebugExprsPane().resetVarTable();
        String path = frame.getSourcePath();
        if (path == null) return;
        int lineNum = frame.getLineNumber();
        if (lineNum < 0) lineNum = 0;
        path = getRunConsole().getSourceURL(path);
        path += "#SelLine=" + lineNum;

        // Set ProgramCounter file and line
        WebURL url = WebURL.getURL(path);
        WebFile file = url.getFile();
        setProgramCounter(file, lineNum - 1);
        getBrowser().setURL(url);
    }

    /**
     * DebugListener breakpoint methods.
     */
    public void requestSet(BreakpointReq e)  { }
    public void requestDeferred(BreakpointReq e)  { }
    public void requestDeleted(BreakpointReq e)  { }
    public void requestError(BreakpointReq e)  { }

    /**
     * RunProc.Listener method - called when output is available.
     */
    public void appendOut(final RunApp aProc, final String aStr)
    {
        if (getSelApp() == aProc)
            getRunConsole().appendOut(aStr);
    }

    /**
     * RunProc.Listener method - called when error output is available.
     */
    public void appendErr(final RunApp aProc, final String aStr)
    {
        if (getSelApp() == aProc)
            getRunConsole().appendErr(aStr);
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
}

package snapcode.apptools;
import snap.view.*;
import snapcode.debug.*;
import snapcode.javatext.JavaTextArea;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.*;
import java.util.List;

/**
 * This project tool class handles running/debugging a process.
 */
public class DebugTool extends WorkspaceTool {

    // The RunTool
    private RunTool _runTool;

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

    /**
     * Constructor.
     */
    public DebugTool(WorkspacePane workspacePane, RunTool aRunTool)
    {
        super(workspacePane);
        _runTool = aRunTool;

        // Create parts
        _debugFramesPane = new DebugFramesPane(this);
        _debugVarsPane = new DebugVarsPane(this);
        _debugExprsPane = new DebugExprsPane(this);
    }

    /**
     * Returns the list of processes.
     */
    public List<RunApp> getProcs()  { return _runTool.getApps(); }

    /**
     * Returns the number of processes.
     */
    public int getProcCount()  { return _runTool.getAppCount(); }

    /**
     * Returns the selected app.
     */
    public RunApp getSelApp()  { return _runTool.getSelApp(); }

    /**
     * Returns the debug app.
     */
    public DebugApp getSelDebugApp()  { return _runTool.getSelDebugApp(); }

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
     * Returns the DebugFramesPane pane.
     */
    public DebugFramesPane getDebugFramesPane()  { return _debugFramesPane; }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()  { return _debugVarsPane; }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()  { return _debugExprsPane; }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Get main col view
        ColView mainColView = getUI(ColView.class);

        // Config AppsComboBox, ThreadsComboBox
        ComboBox<RunApp> appsComboBox = getView("AppsComboBox", ComboBox.class);
        appsComboBox.setItemTextFunction(RunApp::getName);
        ComboBox<DebugThread> threadsComboBox = getView("ThreadsComboBox", ComboBox.class);
        threadsComboBox.setItemTextFunction(DebugThread::getName);

        // Add ProcPane
        View debugFramesUI = _debugFramesPane.getUI();
        CollapseView debugFramesUIBox = new CollapseView("Frames", debugFramesUI);
        mainColView.addChild(debugFramesUIBox);

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
        RunApp selApp = getSelApp();
        DebugApp debugApp = getSelDebugApp();

        // Update ResumeButton, SuspendButton, TerminateButton
        boolean paused = isPaused();
        boolean pausable = isPausable();
        setViewEnabled("ResumeButton", paused);
        setViewEnabled("SuspendButton", pausable);
        setViewEnabled("TerminateButton", !isTerminated());

        // Update StepIntoButton, StepOverButton, StepReturnButton, RunToLineButton
        setViewEnabled("StepIntoButton", paused);
        setViewEnabled("StepOverButton", paused);
        setViewEnabled("StepReturnButton", paused);
        setViewEnabled("RunToLineButton", paused);

        // Update AppsBox, AppsComboBox
        setViewVisible("AppsBox", getProcCount() > 0);
        setViewItems("AppsComboBox", getProcs());
        setViewSelItem("AppsComboBox", selApp);

        // Update ThreadsBox, ThreadsComboBox
        setViewVisible("ThreadsBox", paused);
        if (paused) {
            DebugFrame debugFrame = debugApp.getFrame();
            setViewItems("ThreadsComboBox", debugApp.getThreads());
            if (debugFrame != null)
                setViewSelItem("ThreadsComboBox", debugFrame.getThread());
        }

        // Update frames, vars, expressions
        _debugFramesPane.resetLater();
        _debugVarsPane.resetLater();
        _debugExprsPane.resetLater();
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

        // Handle DebugButton: Run default config
        if (anEvent.equals("DebugButton"))
            _runTool.runConfigOrFile(null, null,true);

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
            WebPage page = getBrowser().getSelPage();
            JavaPage jpage = page instanceof JavaPage ? (JavaPage) page : null;
            if (jpage == null) return;
            JavaTextArea tarea = jpage.getTextArea();
            WebFile file = jpage.getFile();
            int line = tarea.getSel().getStartLine().getIndex();
            debugApp.runToLine(file, line);
        }

        // Handle ThreadsComboBox
//        else if (anEvent.equals("ThreadsComboBox")) {
//            DebugThread debugThread = (DebugThread) getViewSelItem("ThreadsComboBox");
//            debugThread.select();
//        }

        // Do normal version
        else super.respondUI(anEvent);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Debug"; }

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

        // Get current frame (just return if null)
        DebugFrame frame = aProc.getFrame();
        if (frame == null)
            return;

        // Reset vars/exprs UI (This used to be before short-circuit to clear trees)
        getDebugVarsPane().resetVarTable();
        getDebugExprsPane().resetVarTable();

        // Get frame class file path
        String classFilePath = frame.getSourcePath();
        if (classFilePath == null)
            return;

        // Get full path to class in project (or url to system source)
        Project rootProject = getProject();
        String sourceCodeFilePath = ProjectUtils.getSourceCodeUrlForClassPath(rootProject, classFilePath);

        // Add line number
        int lineNum = frame.getLineNumber();
        if (lineNum >= 0)
            sourceCodeFilePath += "#SelLine=" + lineNum;

        // Set ProgramCounter file and line
        WebURL url = WebURL.getURL(sourceCodeFilePath);
        WebFile file = url.getFile();
        setProgramCounter(file, lineNum - 1);
        getBrowser().setSelUrl(url);
    }
}

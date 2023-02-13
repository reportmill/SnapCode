package snapcode.apptools;
import javakit.ide.JavaTextArea;
import snap.gfx.Image;
import snap.util.ListUtils;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.*;
import snapcode.debug.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The ProcPane manages run/debug processes.
 */
public class ProcPane extends PodTool implements RunApp.AppListener {

    // The DebugTool
    private DebugTool _debugTool;

    // The list of recently run apps
    private List<RunApp>  _apps = new ArrayList<>();

    // The selected app
    private RunApp  _selApp;

    // The Process TreeView
    private TreeView  _procTree;

    // Whether Console needs to be reset
    private boolean  _resetConsole;

    // The file that currently has the ProgramCounter
    private WebFile  _progCounterFile;

    // The current ProgramCounter line
    private int  _progCounterLine;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    // Images
    public static Image ProcImage = Image.get(ProcPane.class, "Process.png");
    public static Image ThreadImage = Image.get(ProcPane.class, "Thread.png");
    public static Image StackFrameImage = Image.get(ProcPane.class, "StackFrame.png");

    /**
     * Creates a new ProcPane.
     */
    public ProcPane(DebugTool aDebugTool)
    {
        super(aDebugTool.getPodPane());
        _debugTool = aDebugTool;
    }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _podTools.getRunConsole(); }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()  { return _debugTool.getDebugVarsPane(); }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()  { return _debugTool.getDebugExprsPane(); }

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
    public RunApp removeProc(int anIndex)
    {
        RunApp proc = _apps.remove(anIndex);
        return proc;
    }

    /**
     * Removes a process.
     */
    public int removeProc(RunApp aProcess)
    {
        int index = ListUtils.indexOfId(_apps, aProcess);
        if (index >= 0) removeProc(index);
        return index;
    }

    /**
     * Executes a proc and adds it to list.
     */
    public void execProc(RunApp aProc)
    {
        setSelApp(null);
        addProc(aProc);
        setSelApp(aProc);
        SupportTray supportTray = _podPane.getSupportTray();
        supportTray.showRunTool();
        aProc.exec();
    }

    /**
     * Returns the selected app.
     */
    public RunApp getSelApp()
    {
        return _selApp;
    }

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
     * Returns the debug process.
     */
    public DebugApp getSelDebugApp()
    {
        return _selApp instanceof DebugApp ? (DebugApp) _selApp : null;
    }

    /**
     * Sets the selected stack frame.
     */
    public DebugFrame getSelFrame()
    {
        DebugApp dp = getSelDebugApp();
        return dp != null ? dp.getFrame() : null;
    }

    /**
     * Returns whether selected process is running.
     */
    public boolean isRunning()
    {
        RunApp app = getSelApp();
        return app != null && app.isRunning();
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
     * RunProc.Listener method - called when process starts.
     */
    public void appStarted(RunApp aProc)
    {
        resetLater();
        updateProcTreeLater();
    }

    /**
     * RunProc.Listener method - called when process is paused.
     */
    public void appPaused(DebugApp aProc)
    {
        resetLater();
        updateProcTreeLater();
    }

    /**
     * RunProc.Listener method - called when process is continued.
     */
    public void appResumed(DebugApp aProc)
    {
        resetLater();
        updateProcTreeLater();
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
        _procTree.updateItems(aProc);
        resetLater();
        _podPane.resetLater();

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

            // Handle ThreadStart
            case ThreadStart:
                updateProcTreeLater();
                break;

            // Handle ThreadDeatch
            case ThreadDeath:
                updateProcTreeLater();
                break;

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
        _podPane.getSupportTray().showDebugTool();

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
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get and configure ProcTree
        _procTree = getView("ProcTree", TreeView.class);
        _procTree.setResolver(new ProcTreeResolver());
        _procTree.setRowHeight(20);
    }

    /**
     * ResetUI controls.
     */
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

        // Reset items, auto expand threads
        List<RunApp> apps = getProcs();
        _procTree.setItems(apps);
        for (RunApp app : apps)
            _procTree.expandItem(app);
        if (apps.size() > 0)
            _procTree.updateItems();

        // If current proc is Debug with suspended thread, select current frame
        RunApp proc = getSelApp();
        DebugFrame frame = getSelFrame();
        if (frame != null) {
            _procTree.expandItem(frame.getThread());
            _procTree.setSelItem(frame);
        }

        // Reset Console
        if (_resetConsole) {
            _resetConsole = false;
            getRunConsole().clear();
            if (proc != null) for (RunApp.Output out : proc.getOutput())
                if (out.isErr()) appendErr(proc, out.getString());
                else appendOut(proc, out.getString());
        }
    }

    /**
     * Respond to UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // The Selected App, DebugApp
        RunApp selApp = getSelApp();
        DebugApp debugApp = getSelDebugApp();

        // Handle ResumeButton
        if (anEvent.equals("ResumeButton"))
            debugApp.resume();

        // Handle SuspendButton
        else if (anEvent.equals("SuspendButton"))
            debugApp.pause();

        // Handle TerminateButton
        else if (anEvent.equals("TerminateButton"))
            selApp.terminate();

        // Handle StepIntoButton
        else if (anEvent.equals("StepIntoButton"))
            debugApp.stepIntoLine();

        // Handle StepOverButton
        else if (anEvent.equals("StepOverButton"))
            debugApp.stepOverLine();

        // Handle StepReturnButton
        else if (anEvent.equals("StepReturnButton"))
            debugApp.stepOut();

        // Handle RunToLineButton
        else if (anEvent.equals("RunToLineButton")) {
            WebPage page = getBrowser().getPage();
            JavaPage jpage = page instanceof JavaPage ? (JavaPage) page : null;
            if (jpage == null) return;
            JavaTextArea tarea = jpage.getTextArea();
            WebFile file = jpage.getFile();
            int line = tarea.getSel().getStartLine().getIndex();
            debugApp.runToLine(file, line);
        }

        // Handle ProcTree
        else if (anEvent.equals("ProcTree")) {
            Object item = anEvent.getSelItem();
            if (item instanceof RunApp)
                setSelApp((RunApp) item);
            else if (item instanceof DebugFrame) {
                DebugFrame frame = (DebugFrame) item;
                frame.select();
                _podPane.getSupportTray().showDebugTool();
            }
        }
    }

    /**
     * A TreeResolver for ProcTree.
     */
    private static class ProcTreeResolver extends TreeResolver<Object> {

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(Object anItem)
        {
            if (anItem instanceof DebugApp) return !((DebugApp) anItem).isTerminated();
            if (anItem instanceof DebugThread) {
                DebugThread dthread = (DebugThread) anItem;
                DebugApp dapp = dthread.getApp();
                return dapp.isPaused() && dthread.isSuspended();
            }
            return false;
        }

        /**
         * Returns the children.
         */
        public Object[] getChildren(Object aParent)
        {
            if (aParent instanceof DebugApp) return ((DebugApp) aParent).getThreads();
            if (aParent instanceof DebugThread) return ((DebugThread) aParent).getFrames();
            throw new RuntimeException("ProcPane.ProcTreeResolver: Invalid parent: " + aParent);
        }

        /**
         * Returns the parent of an item.
         */
        public Object getParent(Object anItem)
        {
            Object par = null;
            if (anItem instanceof DebugThread) par = ((DebugThread) anItem).getApp();
            if (anItem instanceof DebugFrame) par = ((DebugFrame) anItem).getThread();
            return par;
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(Object anItem)
        {
            if (anItem instanceof RunApp) return ((RunApp) anItem).getName();
            if (anItem instanceof DebugThread) return ((DebugThread) anItem).getName();
            if (anItem instanceof DebugFrame) return ((DebugFrame) anItem).getDescription();
            return "ProcPane.ProcTreeResolver: Invalid Item: " + anItem;
        }

        /**
         * Return the image to be used for given item.
         */
        public Image getImage(Object anItem)
        {
            if (anItem instanceof RunApp) return ProcImage;
            if (anItem instanceof DebugThread) return ThreadImage;
            return StackFrameImage;
        }
    }

    /**
     * Update RuntimeTree later.
     */
    synchronized void updateProcTreeLater()
    {
        if (_procTreeUpdater != null) return; // If already set, just return
        runLaterDelayed(250, _procTreeUpdater = _procTreeUpdaterImpl);
    }

    Runnable _procTreeUpdater, _procTreeUpdaterImpl = () -> {
        resetLater();
        _procTreeUpdater = null;
    };

}
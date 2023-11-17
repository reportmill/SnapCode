package snapcode.apptools;
import snap.gfx.Image;
import snap.util.ListUtils;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.*;
import snapcode.debug.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class displays stack frame for DebugTool.SelApp thread.
 */
public class DebugFramesPane extends WorkspaceTool implements RunApp.AppListener {

    // The DebugTool
    private DebugTool _debugTool;

    // The list of recently run apps
    private List<RunApp>  _apps = new ArrayList<>();

    // The Process TreeView
    private TreeView<Object>  _procTree;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    // Runnable to defer/coalesce ProcTree update
    private Runnable _procTreeUpdater, _procTreeUpdaterImpl = () -> { resetLater(); _procTreeUpdater = null; };

    // Images
    public static Image ProcImage = Image.getImageForClassResource(DebugFramesPane.class, "Process.png");
    public static Image ThreadImage = Image.getImageForClassResource(DebugFramesPane.class, "Thread.png");
    public static Image StackFrameImage = Image.getImageForClassResource(DebugFramesPane.class, "StackFrame.png");

    /**
     * Constructor.
     */
    public DebugFramesPane(DebugTool aDebugTool)
    {
        super(aDebugTool.getWorkspacePane());
        _debugTool = aDebugTool;
    }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _debugTool.getRunConsole(); }

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
     * Sets the selected stack frame.
     */
    public DebugFrame getSelFrame()
    {
        DebugApp dp = _debugTool.getSelDebugApp();
        return dp != null ? dp.getFrame() : null;
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
        _procTree.updateItem(aProc);
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

            // Handle ThreadStart
            case ThreadStart: updateProcTreeLater(); break;

            // Handle ThreadDeatch
            case ThreadDeath: updateProcTreeLater(); break;

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
        _debugTool.setProgramCounter(file, lineNum - 1);
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
    public void appendOut(RunApp aProc, final String aStr)  { _debugTool.appendOut(aProc, aStr); }

    /**
     * RunProc.Listener method - called when error output is available.
     */
    public void appendErr(RunApp aProc, final String aStr)  { _debugTool.appendErr(aProc, aStr); }

    /**
     * Sets the program counter file, line.
     */
    public void setProgramCounter(WebFile aFile, int aLine)  { _debugTool.setProgramCounter(aFile, aLine); }

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
        // Reset items, auto expand threads
        List<RunApp> apps = getProcs();
        _procTree.setItemsList((List<Object>) (List<?>) apps);
        for (RunApp app : apps)
            _procTree.expandItem(app);
        if (apps.size() > 0)
            _procTree.updateItems();

        // If current proc is Debug with suspended thread, select current frame
        DebugFrame frame = getSelFrame();
        if (frame != null) {
            _procTree.expandItem(frame.getThread());
            _procTree.setSelItem(frame);
        }
    }

    /**
     * Respond to UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ProcTree
        if (anEvent.equals("ProcTree")) {
            Object item = anEvent.getSelItem();
            if (item instanceof RunApp)
                _debugTool.setSelApp((RunApp) item);
            else if (item instanceof DebugFrame) {
                DebugFrame frame = (DebugFrame) item;
                frame.select();
                _workspaceTools.showToolForClass(DebugTool.class);
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
        if (_procTreeUpdater == null)
            runDelayed(250, _procTreeUpdater = _procTreeUpdaterImpl);
    }
}
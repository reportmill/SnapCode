package snapcode.apptools;
import snap.gfx.Image;
import snap.view.*;
import snapcode.app.*;
import snapcode.debug.*;
import java.util.List;

/**
 * This class displays stack frame for DebugTool.SelApp thread.
 */
public class DebugFramesPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool _debugTool;

    // The Process TreeView
    private TreeView<Object>  _procTree;

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
     * Sets the selected stack frame.
     */
    public DebugFrame getSelFrame()
    {
        DebugApp dp = _debugTool.getSelDebugApp();
        return dp != null ? dp.getFrame() : null;
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
        // Reset items, auto expand threads
        List<RunApp> apps = _debugTool.getProcs();
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
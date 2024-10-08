package snapcode.apptools;
import snap.geom.Insets;
import snap.view.*;
import snapcode.app.*;
import snapcode.debug.*;

/**
 * This class displays stack frame for DebugTool.SelApp thread.
 */
public class DebugFramesPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool _debugTool;

    // The frames ListView
    private ListView<DebugFrame> _framesListView;

    // Runnable to defer/coalesce ProcTree update
    private Runnable _procTreeUpdater, _procTreeUpdaterImpl = () -> { resetLater(); _procTreeUpdater = null; };

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
        DebugApp debugApp = _debugTool.getSelDebugApp();
        return debugApp != null ? debugApp.getFrame() : null;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get and configure FramesListView
        _framesListView = getView("FramesListView", ListView.class);
        _framesListView.setCellPadding(new Insets(5, 5, 5, 8));
        _framesListView.setItemTextFunction(DebugFrame::getDescription);
    }

    /**
     * ResetUI controls.
     */
    protected void resetUI()
    {
        // Reset items, auto expand threads
        DebugFrame selFrame = getSelFrame();
        DebugThread debugThread = selFrame != null ? selFrame.getThread() : null;
        DebugFrame[] debugFrames = debugThread != null ? debugThread.getFrames() : null;
        _framesListView.setItems(debugFrames);

        // If current proc is Debug with suspended thread, select current frame
        _framesListView.setSelItem(selFrame);
    }

    /**
     * Respond to UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle FramesListView
        if (anEvent.equals("FramesListView")) {
            DebugFrame frame = (DebugFrame) anEvent.getSelItem();
            if (frame != null)
                frame.select();
        }
    }

    /**
     * Update RuntimeTree later.
     */
    synchronized void updateProcTreeLater()
    {
        if (_procTreeUpdater == null)
            runDelayed(_procTreeUpdater = _procTreeUpdaterImpl, 250);
    }
}
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

    // The frames ListArea
    private ListArea<DebugFrame>  _framesListArea;

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
        // Get and configure FramesListArea
        ListView<DebugFrame> frameListView = getView("FramesListView", ListView.class);
        _framesListArea = frameListView.getListArea();
        _framesListArea.setCellPadding(new Insets(5, 5, 5, 8));
        _framesListArea.setItemTextFunction(DebugFrame::getDescription);
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
        _framesListArea.setItems(debugFrames);

        // If current proc is Debug with suspended thread, select current frame
        _framesListArea.setSelItem(selFrame);
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
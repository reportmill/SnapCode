package snapcode.apptools;
import javakit.project.Breakpoint;
import javakit.project.Breakpoints;
import snap.view.ListView;
import snap.view.View;
import snap.view.ViewEvent;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * This class displays and edits project breakpoints.
 */
public class BreakpointsTool extends WorkspaceTool {

    // The breakpoints list
    private ListView<Breakpoint>  _breakpointsList;

    /**
     * Constructor.
     */
    public BreakpointsTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the list of Breakpoints.
     */
    public Breakpoints getBreakpoints()  { return _workspace.getBreakpoints(); }

    /**
     * Returns the selected Breakpoint.
     */
    public Breakpoint getSelBreakpoint()
    {
        return _breakpointsList.getSelItem();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        _breakpointsList = getView("BreakpointList", ListView.class);
        _breakpointsList.setRowHeight(24);
        _breakpointsList.addEventFilter(e -> breakpointsListDidMouseRelease(e), View.MouseRelease);
        getBreakpoints().addPropChangeListener(pce -> resetLater());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        setViewEnabled("DeleteButton", getSelBreakpoint() != null);

        setViewItems("BreakpointList", getBreakpoints().getArray());
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            getBreakpoints().remove(getSelBreakpoint());

        // Handle DeleteAllButton
        if (anEvent.equals("DeleteAllButton"))
            getBreakpoints().clear();
    }

    /**
     * Called when BreakpointsList gets MouseRelease.
     */
    private void breakpointsListDidMouseRelease(ViewEvent anEvent)
    {
        if (anEvent.getClickCount() == 2) {
            Breakpoint breakpoint = getSelBreakpoint();
            String urlStr = breakpoint.getFile().getUrlString();
            String urlStrForLine = urlStr + "#LineNumber=" + (breakpoint.getLine() + 1);
            getBrowser().setURLString(urlStrForLine);
        }
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Breakpoints"; }
}
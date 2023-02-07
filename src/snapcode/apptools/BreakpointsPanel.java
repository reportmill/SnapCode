package snapcode.apptools;
import javakit.project.Breakpoint;
import javakit.project.Breakpoints;
import snap.view.ListView;
import snap.view.ViewEvent;
import snapcode.app.ProjectPane;
import snapcode.app.ProjectTool;

/**
 * This class displays and edits project breakpoints.
 */
public class BreakpointsPanel extends ProjectTool {

    // The breakpoints list
    private ListView<Breakpoint>  _breakpointsList;

    /**
     * Creates a new BreakpointsPanel.
     */
    public BreakpointsPanel(ProjectPane projPane)
    {
        super(projPane);
    }

    /**
     * Returns the list of Breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        return getProject().getBreakpoints();
    }

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
    protected void initUI()
    {
        _breakpointsList = getView("BreakpointList", ListView.class);
        enableEvents(_breakpointsList, MouseRelease);
        _breakpointsList.setRowHeight(24);
        getBreakpoints().addPropChangeListener(pce -> resetLater());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        setViewEnabled("DeleteButton", getSelBreakpoint() != null);

        setViewItems("BreakpointList", getBreakpoints());
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            getBreakpoints().remove(getSelBreakpoint());

        // Handle DeleteAllButton
        if (anEvent.equals("DeleteAllButton"))
            getBreakpoints().clear();

        // Handle BreakpointList
        if (anEvent.equals("BreakpointList") && anEvent.getClickCount() == 2) {
            Breakpoint bp = getSelBreakpoint();
            String urls = bp.getFile().getURL().getString() + "#LineNumber=" + (bp.getLine() + 1);
            getBrowser().setURLString(urls);
        }
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Breakpoints"; }
}
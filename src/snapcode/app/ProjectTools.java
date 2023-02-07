package snapcode.app;
import snapcode.apptools.*;

/**
 * This class manages all of the ProjectTools for a ProjectPane.
 */
public class ProjectTools {

    // The Problems pane
    private ProblemsPane _problemsPane;

    // The RunConsole
    private RunConsole _runConsole;

    // The BreakpointsPanel
    private BreakpointsPanel _breakpointsPanel;

    // The ProjectPane
    private ProjectPane  _projPane;

    // The DebugTool
    private DebugTool  _debugTool;

    // The SearchTool
    private SearchPane  _searchTool;

    /**
     * Constructor.
     */
    public ProjectTools(ProjectPane projectPane)
    {
        super();
        _projPane = projectPane;
    }

    /**
     * Create tools.
     */
    protected void createTools()
    {
        _problemsPane = new ProblemsPane(_projPane);
        _runConsole = new RunConsole(_projPane);
        _breakpointsPanel = new BreakpointsPanel(_projPane);
        _debugTool = new DebugTool(_projPane);
        _searchTool = new SearchPane(_projPane);
    }

    /**
     * Returns the problems pane.
     */
    public ProblemsPane getProblemsPane()  { return _problemsPane; }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _runConsole; }

    /**
     * Returns the BreakpointsPanel.
     */
    public BreakpointsPanel getBreakpointsPanel()  { return _breakpointsPanel; }

    /**
     * Returns the debug tool.
     */
    public DebugTool getDebugTool()  { return _debugTool; }

    /**
     * Returns the search tool.
     */
    public SearchPane getSearchTool()  { return _searchTool; }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends ProjectTool> aClass)
    {
        SupportTray supportTray = ((AppPane) _projPane).getSupportTray();
        supportTray.setSelToolForClass(aClass);
    }
}

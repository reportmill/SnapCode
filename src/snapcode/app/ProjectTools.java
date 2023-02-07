package snapcode.app;
import snap.geom.Side;
import snapcode.apptools.*;

/**
 * This class manages all of the ProjectTools for a ProjectPane.
 */
public class ProjectTools {

    // The FilesPane
    protected FilesPane  _filesPane;

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

    // The SupportTray
    private SupportTray  _supportTray;

    // The SupportTray
    private SupportTray  _sideBar;

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
        _filesPane = new FilesPane((AppPane) _projPane);
        _problemsPane = new ProblemsPane(_projPane);
        _runConsole = new RunConsole(_projPane);
        _breakpointsPanel = new BreakpointsPanel(_projPane);
        _debugTool = new DebugTool(_projPane);
        _searchTool = new SearchPane(_projPane);

        // Set tools
        ProjectTool[] bottomTools = { _problemsPane, _debugTool, _runConsole, _breakpointsPanel, _searchTool };
        _supportTray = new SupportTray(_projPane, Side.BOTTOM, bottomTools);


        ProjectTool[] sideTools = { _filesPane };
        _sideBar = new SupportTray(_projPane, Side.LEFT, sideTools);
    }

    /**
     * Returns the files pane.
     */
    public FilesPane getFilesPane()  { return _filesPane; }

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
     * Returns the support tray.
     */
    public SupportTray getSupportTray()  { return _supportTray; }

    /**
     * Returns the sidebar.
     */
    public SupportTray getSideBar()  { return _sideBar; }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends ProjectTool> aClass)
    {
        _supportTray.setSelToolForClass(aClass);
    }

    /**
     * Reset visible tools.
     */
    public void resetLater()
    {
        _supportTray.resetLater();
        _sideBar.resetLater();
    }
}

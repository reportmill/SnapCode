package snapcode.app;
import snap.geom.Side;
import snapcode.apptools.*;

/**
 * This class manages all of the ProjectTools for a ProjectPane.
 */
public class ProjectTools {

    // The FilesPane
    protected FilesTool  _filesTool;

    // The FilesPane
    protected FileTreeTool _fileTreeTool;

    // The Problems pane
    private ProblemsTool _problemsTool;

    // The RunConsole
    private RunConsole _runConsole;

    // The BreakpointsTool
    private BreakpointsTool _breakpointsTool;

    // The ProjectPane
    private ProjectPane  _projPane;

    // The DebugTool
    private DebugTool  _debugTool;

    // The SearchTool
    private SearchPane  _searchTool;

    // The RunConfigs tool
    private RunConfigsTool  _runConfigsTool;

    // The HttpServerTool
    private HttpServerTool  _httpServerTool;

    // The SupportTray
    private SupportTray  _supportTray;

    // The SideBar
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
        _filesTool = new FilesTool(_projPane);
        _fileTreeTool = new FileTreeTool((AppPane) _projPane);
        _problemsTool = new ProblemsTool(_projPane);
        _runConsole = new RunConsole(_projPane);
        _breakpointsTool = new BreakpointsTool(_projPane);
        _debugTool = new DebugTool(_projPane);
        _searchTool = new SearchPane(_projPane);
        _runConfigsTool = new RunConfigsTool(_projPane);
        _httpServerTool = new HttpServerTool(_projPane);

        // Set tools
        ProjectTool[] bottomTools = {_problemsTool, _debugTool, _runConsole, _breakpointsTool, _searchTool, _runConfigsTool, _httpServerTool };
        _supportTray = new SupportTray(_projPane, Side.BOTTOM, bottomTools);


        ProjectTool[] sideTools = {_fileTreeTool};
        _sideBar = new SupportTray(_projPane, Side.LEFT, sideTools);
    }

    /**
     * Returns the files tool.
     */
    public FilesTool getFilesTool()  { return _filesTool; }

    /**
     * Returns the FileTreeTool.
     */
    public FileTreeTool getFileTreeTool()  { return _fileTreeTool; }

    /**
     * Returns the problems tool.
     */
    public ProblemsTool getProblemsTool()  { return _problemsTool; }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _runConsole; }

    /**
     * Returns the BreakpointsTool.
     */
    public BreakpointsTool getBreakpointsTool()  { return _breakpointsTool; }

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

    /**
     * Closes the project.
     */
    public void closeProject()
    {
        _httpServerTool.stopServer();
    }
}

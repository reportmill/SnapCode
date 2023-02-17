package snapcode.app;
import javakit.ide.JavaTextPane;
import javakit.project.Breakpoints;
import javakit.project.BuildIssue;
import javakit.project.BuildIssues;
import javakit.project.Workspace;
import snap.geom.Side;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snapcode.apptools.*;

/**
 * This class manages all the WorkspaceTool instances for a WorkspacePane.
 */
public class WorkspaceTools {

    // The WorkspacePane
    private WorkspacePane  _workspacePane;

    // The Workspace
    private Workspace  _workspace;

    // Array of all tools
    protected WorkspaceTool[]  _tools;

    // The FilesPane
    protected FilesTool  _filesTool;

    // The FilesPane
    protected FileTreeTool  _fileTreeTool;

    // The Problems pane
    private ProblemsTool  _problemsTool;

    // The RunConsole
    private RunConsole  _runConsole;

    // The BreakpointsTool
    private BreakpointsTool  _breakpointsTool;

    // The DebugTool
    private DebugTool  _debugTool;

    // The SearchTool
    private SearchTool _searchTool;

    // The RunConfigs tool
    private RunConfigsTool  _runConfigsTool;

    // The HttpServerTool
    private HttpServerTool  _httpServerTool;

    // The VcsTools
    private VcsTools  _vcsTools;

    // The bottom side ToolTray
    private ToolTray _bottomTray;

    // The left side ToolTray
    private ToolTray _leftTray;

    // The right side ToolTray
    private ToolTray _rightTray;

    // PropChangeListener for breakpoints
    private PropChangeListener  _breakpointsLsnr = pc -> breakpointsDidChange(pc);

    // PropChangeListener for BuildIssues
    private PropChangeListener  _buildIssueLsnr = pc -> buildIssuesDidChange(pc);

    /**
     * Constructor.
     */
    public WorkspaceTools(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
    }

    /**
     * Create tools.
     */
    protected void createTools()
    {
        _filesTool = new FilesTool(_workspacePane);
        _fileTreeTool = new FileTreeTool(_workspacePane);
        _problemsTool = new ProblemsTool(_workspacePane);
        _runConsole = new RunConsole(_workspacePane);
        _breakpointsTool = new BreakpointsTool(_workspacePane);
        _debugTool = new DebugTool(_workspacePane);
        _searchTool = new SearchTool(_workspacePane);
        _runConfigsTool = new RunConfigsTool(_workspacePane);
        _httpServerTool = new HttpServerTool(_workspacePane);
        _vcsTools = new VcsTools(_workspacePane);

        // Create BottomTray
        WorkspaceTool[] bottomTools = { _problemsTool, _debugTool, _runConsole, _breakpointsTool, _runConfigsTool, _httpServerTool };
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);

        // Create LeftTray
        WorkspaceTool[] leftTools = { _fileTreeTool };
        _leftTray = new ToolTray(Side.LEFT, leftTools);

        // Create RightTray
        EvalTool evalTool = new EvalTool(_workspacePane);
        WorkspaceTool[] rightTools = { evalTool, _searchTool };
        _rightTray = new ToolTray(Side.RIGHT, rightTools);

        _tools = ArrayUtils.addAll(_tools, _bottomTray._trayTools);
        _tools = ArrayUtils.addAll(_tools, _leftTray._trayTools);
        _tools = ArrayUtils.addAll(_tools, _rightTray._trayTools);

        // Start listening to Breakpoints helper
        Workspace workspace = _workspacePane.getWorkspace();
        Breakpoints breakpoints = workspace.getBreakpoints();
        breakpoints.addPropChangeListener(_breakpointsLsnr);

        // Start listening to BuildIssues helper
        BuildIssues buildIssues = workspace.getBuildIssues();
        buildIssues.addPropChangeListener(_buildIssueLsnr);
    }

    /**
     * Sets the workspace.
     */
    protected void setWorkspace(Workspace aWorkspace)
    {
        if (aWorkspace == _workspace) return;

        // Remove
        if (_workspace != null) {

            // Remove listeners
            Breakpoints breakpoints = _workspace.getBreakpoints();
            breakpoints.removePropChangeListener(_breakpointsLsnr);
            BuildIssues buildIssues = _workspace.getBuildIssues();
            buildIssues.removePropChangeListener(_buildIssueLsnr);
        }

        // Set workspace
        _workspace = aWorkspace;

        // Add
        if (_workspace != null) {

            // Add listeners
            Breakpoints breakpoints = _workspace.getBreakpoints();
            breakpoints.addPropChangeListener(_breakpointsLsnr);
            BuildIssues buildIssues = _workspace.getBuildIssues();
            buildIssues.addPropChangeListener(_buildIssueLsnr);

            // Update tools
            for (WorkspaceTool tool : _tools)
                tool._workspace = _workspace;
        }
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
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _runConsole; }

    /**
     * Returns the debug tool.
     */
    public DebugTool getDebugTool()  { return _debugTool; }

    /**
     * Returns the search tool.
     */
    public SearchTool getSearchTool()  { return _searchTool; }

    /**
     * Returns the support tray.
     */
    public ToolTray getBottomTray()  { return _bottomTray; }

    /**
     * Returns the left side tray.
     */
    public ToolTray getLeftTray()  { return _leftTray; }

    /**
     * Returns the right side tray.
     */
    public ToolTray getRightTray()  { return _rightTray; }

    /**
     * Returns the tool for given class.
     */
    public <T extends WorkspaceTool> T getToolForClass(Class<T> aToolClass)
    {
        return _bottomTray.getToolForClass(aToolClass);
    }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends WorkspaceTool> aClass)
    {
        _bottomTray.setSelToolForClass(aClass);
    }

    /**
     * Reset visible tools.
     */
    public void resetLater()
    {
        _bottomTray.resetLater();
        _leftTray.resetLater();
        _rightTray.resetLater();
    }

    /**
     * Closes the project.
     */
    public void closeProject()
    {
        _httpServerTool.stopServer();
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    protected void breakpointsDidChange(PropChange pc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.breakpointsDidChange(pc);
    }

    /**
     * Called when Project.BuildIssues change.
     */
    protected void buildIssuesDidChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Get issue added or removed
        BuildIssue issue = (BuildIssue) pc.getNewValue();
        if (issue == null)
            issue = (BuildIssue) pc.getOldValue();

        // Make current JavaPage.TextArea resetLater
        WebFile issueFile = issue.getFile();
        WebPage page = _workspacePane.getBrowser().getPageForURL(issueFile.getURL());
        if (page instanceof JavaPage) {
            JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
            javaTextPane.buildIssueOrBreakPointMarkerChanged();
        }

        // Update FilesPane.FilesTree
        FileTreeTool fileTreeTool = getFileTreeTool();
        fileTreeTool.updateFile(issueFile);
    }
}
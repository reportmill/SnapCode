package snapcode.app;
import snap.geom.Side;
import snapcode.apptools.*;

/**
 * This WorkspaceTools subclass enables real JRE dev features.
 */
public class WorkspaceToolsX extends WorkspaceTools {

    /**
     * Constructor.
     */
    public WorkspaceToolsX(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Create tools.
     */
    @Override
    protected void createTools()
    {
        // Create tools
        FilesTool filesTool = new FilesTool(_workspacePane);
        FileTreeTool fileTreeTool = new FileTreeTool(_workspacePane);
        ProblemsTool problemsTool = new ProblemsTool(_workspacePane);
        BreakpointsTool breakpointsTool = new BreakpointsTool(_workspacePane);
        SearchTool searchTool = new SearchTool(_workspacePane);
        DebugTool debugTool = new DebugTool(_workspacePane);
        RunConsole runConsole = new RunConsole(_workspacePane);
        RunConfigsTool runConfigsTool = new RunConfigsTool(_workspacePane);
        HttpServerTool httpServerTool = new HttpServerTool(_workspacePane);
        VcsTools vcsTools = new VcsTools(_workspacePane);

        // Create tools
        _tools = new WorkspaceTool[] {
                filesTool, fileTreeTool,
                problemsTool, breakpointsTool,
                searchTool,
                debugTool, runConsole, runConfigsTool,
                httpServerTool, vcsTools
        };

        // Create LeftTray
        WorkspaceTool[] leftTools = { fileTreeTool };
        _leftTray = new ToolTray(Side.LEFT, leftTools);

        // Create RightTray
        WorkspaceTool[] rightTools = { searchTool };
        _rightTray = new ToolTray(Side.RIGHT, rightTools);

        // Create BottomTray
        WorkspaceTool[] bottomTools = { problemsTool, debugTool, runConsole, breakpointsTool, runConfigsTool, httpServerTool };
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);
    }

    /**
     * Closes the project.
     */
    @Override
    public void closeProject()
    {
        HttpServerTool httpServerTool = getToolForClass(HttpServerTool.class);
        if (httpServerTool != null)
            httpServerTool.stopServer();
    }
}
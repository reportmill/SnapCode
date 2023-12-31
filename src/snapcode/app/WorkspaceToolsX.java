package snapcode.app;
import snap.geom.Side;
import snap.props.PropChange;
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
        // Main tools
        FilesTool filesTool = new FilesTool(_workspacePane);
        FileTreeTool fileTreeTool = new FileTreeTool(_workspacePane);
        RunTool runTool = new RunTool(_workspacePane);
        DebugTool debugTool = new DebugTool(_workspacePane, runTool);
        BuildTool buildTool = new BuildTool(_workspacePane);

        // Support tools
        BreakpointsTool breakpointsTool = new BreakpointsTool(_workspacePane);
        SearchTool searchTool = new SearchTool(_workspacePane);
        CompleterTool completerTool = new CompleterTool(_workspacePane);
        RunConfigsTool runConfigsTool = new RunConfigsTool(_workspacePane);
        HttpServerTool httpServerTool = new HttpServerTool(_workspacePane);

        // Create tools array
        _tools = new WorkspaceTool[] {
                filesTool, fileTreeTool, runTool, debugTool, buildTool, breakpointsTool,
                searchTool, completerTool,
                runConfigsTool, httpServerTool
        };

        // Create LeftTray
        WorkspaceTool[] leftTools = { fileTreeTool };
        _leftTray = new ToolTray(Side.LEFT, leftTools);

        // Create RightTray
        WorkspaceTool[] rightTools = { runTool, debugTool, buildTool, searchTool, completerTool };
        _rightTray = new ToolTray(Side.RIGHT, rightTools);

        // Create BottomTray
        WorkspaceTool[] bottomTools = { breakpointsTool, runConfigsTool, httpServerTool };
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    @Override
    protected void breakpointsDidChange(PropChange pc)
    {
        RunTool runTool = getToolForClass(RunTool.class);
        runTool.breakpointsDidChange(pc);
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
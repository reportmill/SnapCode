package snapcode.app;
import snap.geom.Side;
import snap.props.PropChange;
import snap.web.WebFile;
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
     * Runs the given file.
     */
    @Override
    public void runFile(WebFile aFile, boolean isDebug)
    {
        DebugTool debugTool = getToolForClass(DebugTool.class);
        showTool(debugTool);
        debugTool.runConfigOrFile(null, aFile, false);
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
        EvalTool evalTool = new EvalTool(_workspacePane);
        CompleterTool completerTool = new CompleterTool(_workspacePane);
        DebugTool debugTool = new DebugTool(_workspacePane);
        RunConsole runConsole = new RunConsole(_workspacePane);
        RunConfigsTool runConfigsTool = new RunConfigsTool(_workspacePane);
        HttpServerTool httpServerTool = new HttpServerTool(_workspacePane);

        // Create tools array
        _tools = new WorkspaceTool[] {
                filesTool, fileTreeTool,
                problemsTool, breakpointsTool,
                searchTool, evalTool, completerTool,
                debugTool, debugTool.getProcPane(),
                runConsole, runConfigsTool,
                httpServerTool
        };

        // Create LeftTray
        WorkspaceTool[] leftTools = { fileTreeTool };
        _leftTray = new ToolTray(Side.LEFT, leftTools);

        // Create RightTray
        WorkspaceTool[] rightTools = { evalTool, debugTool, searchTool, completerTool };
        _rightTray = new ToolTray(Side.RIGHT, rightTools);

        // Create BottomTray
        WorkspaceTool[] bottomTools = { problemsTool, runConsole, breakpointsTool, runConfigsTool, httpServerTool };
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    @Override
    protected void breakpointsDidChange(PropChange pc)
    {
        DebugTool debugTool = getToolForClass(DebugTool.class);
        debugTool.breakpointsDidChange(pc);
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
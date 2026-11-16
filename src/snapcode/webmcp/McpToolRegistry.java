package snapcode.webmcp;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snapcode.app.WorkspacePane;
import java.util.*;
import java.util.function.Consumer;

/**
 * Holds all SnapCode WebMCP tools and dispatches calls to them.
 */
public class McpToolRegistry {

    // The tools by name
    private final Map<String,McpTool> _tools = new LinkedHashMap<>();

    // The shared registry
    private static McpToolRegistry _shared;

    /**
     * Constructor: registers all tool groups.
     */
    public McpToolRegistry()
    {
        GuideMcpTools.addTools(this);
        WorkspaceMcpTools.addTools(this);
        FileMcpTools.addTools(this);
        BuildRunMcpTools.addTools(this);
        BuilderMcpTools.addTools(this);
        ProjectSettingsMcpTools.addTools(this);
        VcsMcpTools.addTools(this);
        HelpUiMcpTools.addTools(this);
    }

    /**
     * Returns the shared registry.
     */
    public static McpToolRegistry getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new McpToolRegistry();
    }

    /**
     * Adds a tool.
     */
    public McpTool addTool(McpTool aTool)
    {
        if (_tools.containsKey(aTool.getName()))
            throw new IllegalStateException("Duplicate WebMCP tool name: " + aTool.getName());
        _tools.put(aTool.getName(), aTool);
        return aTool;
    }

    /**
     * Returns the tools.
     */
    public List<McpTool> getTools()  { return new ArrayList<>(_tools.values()); }

    /**
     * Returns the tool for given name.
     */
    public McpTool getTool(String aName)  { return _tools.get(aName); }

    /**
     * Calls the named tool with given args. Must be called on the event thread.
     * The consumer receives the MCP CallToolResult map (possibly later, for long-running tools).
     */
    public void callTool(String aName, Map<String,Object> theArgs, Consumer<Map<String,Object>> onComplete)
    {
        McpResult result = new McpResult(onComplete);
        McpTool tool = getTool(aName);
        if (tool == null) {
            result.error("Unknown tool: " + aName);
            return;
        }

        try { tool.getHandler().handle(new McpArgs(theArgs), result); }
        catch (McpException e) { result.error(e.getMessage()); }
        catch (Throwable t) {
            t.printStackTrace();
            result.error("Tool '" + aName + "' failed: " + t);
        }
    }

    /**
     * Calls the named tool from any thread, running it on the event thread.
     */
    public void callToolLater(String aName, Map<String,Object> theArgs, Consumer<Map<String,Object>> onComplete)
    {
        ViewUtils.runLater(() -> callTool(aName, theArgs, onComplete));
    }

    /**
     * Returns the open WorkspacePane (throws McpException if none).
     */
    public static WorkspacePane getWorkspacePane()
    {
        WorkspacePane workspacePane = WindowView.getOpenWindowController(WorkspacePane.class);
        if (workspacePane == null)
            throw new McpException("No SnapCode workspace window is open");
        return workspacePane;
    }
}

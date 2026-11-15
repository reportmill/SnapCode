package snapcode.webmcp;
import snap.view.ViewUtils;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.app.WorkspaceTools;
import snapcode.apptools.*;
import snapcode.util.HelpFile;
import snapcode.util.HelpSection;
import snapcode.views.BlocksConsole;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;

/**
 * WebMCP tools for the help system and for showing IDE panels.
 */
public class HelpUiMcpTools {

    // Panel names to tool classes
    private static final Map<String,Class<? extends WorkspaceTool>> PANELS = new LinkedHashMap<>();
    static {
        PANELS.put("project", ProjectFilesTool.class);
        PANELS.put("help", HelpTool.class);
        PANELS.put("blocks", BlocksConsole.class);
        PANELS.put("run", RunTool.class);
        PANELS.put("build", BuildTool.class);
        PANELS.put("search", SearchTool.class);
        PANELS.put("classes", ClassesTool.class);
        PANELS.put("run_configs", RunConfigsTool.class);
        PANELS.put("breakpoints", BreakpointsTool.class);
        PANELS.put("http_server", HttpServerTool.class);
    }

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("help_list_sections",
                "Lists the topics of SnapCode's built-in help (Java and SnapKit how-tos with example code: printing, " +
                "loops, UI, drawing, charts, animation, etc.).",
                new Schema(), HelpUiMcpTools::helpListSections)).readOnly();

        reg.addTool(new McpTool("help_get_section",
                "Returns the markdown content (explanation and example code) of a help topic.",
                new Schema().str("section", "Topic header as listed by help_list_sections.", true),
                HelpUiMcpTools::helpGetSection)).readOnly();

        reg.addTool(new McpTool("help_insert_example",
                "Inserts a help topic's example code into the selected Java/Jepl editor (like the help panel's 'Add " +
                "Code' button) and runs it. A .jepl REPL file is created if none is open.",
                new Schema().str("section", "Topic header as listed by help_list_sections.", true),
                HelpUiMcpTools::helpInsertExample));

        reg.addTool(new McpTool("show_panel",
                "Shows an IDE panel: " + String.join(", ", PANELS.keySet()) + ". Or shows/hides the left, right " +
                "and bottom trays, or the home page.",
                new Schema()
                        .enumStr("panel", "Panel to show.", false, PANELS.keySet().toArray(new String[0]))
                        .bool("left_tray", "Show (true) or hide (false) the left tray.", false)
                        .bool("right_tray", "Show (true) or hide (false) the right tray.", false)
                        .bool("bottom_tray", "Show (true) or hide (false) the bottom tray.", false)
                        .bool("home_page", "Show the SnapCode home page.", false),
                HelpUiMcpTools::showPanel));
    }

    /**
     * Calls handler with loaded help file.
     */
    private static void withHelpFile(McpResult result, java.util.function.Consumer<HelpFile> aHandler)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        HelpTool helpTool = workspacePane.getWorkspaceTools().getHelpTool();
        if (!helpTool.getHelpFile().getSections().isEmpty()) {
            aHandler.accept(helpTool.getHelpFile());
            return;
        }

        // Showing help tool loads the help file
        helpTool.getUI();
        workspacePane.getWorkspaceTools().showTool(helpTool);
        McpUtils.waitUntil(result, () -> !helpTool.getHelpFile().getSections().isEmpty(), 20000,
                () -> {
                    try { aHandler.accept(helpTool.getHelpFile()); }
                    catch (McpException e) { result.error(e.getMessage()); }
                },
                () -> result.error("Help file did not load"));
    }

    /**
     * Returns section for name.
     */
    private static HelpSection getSection(HelpFile helpFile, String aName)
    {
        HelpSection section = helpFile.getSectionForHeaderId(aName.trim());
        if (section == null)
            throw new McpException("No help section '" + aName + "'. See help_list_sections.");
        return section;
    }

    /**
     * help_list_sections
     */
    private static void helpListSections(McpArgs args, McpResult result)
    {
        withHelpFile(result, helpFile -> result.json(helpFile.getSections().stream().map(HelpSection::getHeader).toList()));
    }

    /**
     * help_get_section
     */
    private static void helpGetSection(McpArgs args, McpResult result)
    {
        String name = args.requireString("section");
        withHelpFile(result, helpFile -> {
            HelpSection section = getSection(helpFile, name);
            result.text("# " + section.getHeader() + "\n" + section.getContent());
        });
    }

    /**
     * help_insert_example
     */
    private static void helpInsertExample(McpArgs args, McpResult result)
    {
        String name = args.requireString("section");
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        withHelpFile(result, helpFile -> {
            HelpSection section = getSection(helpFile, name);
            HelpTool helpTool = workspacePane.getWorkspaceTools().getHelpTool();
            helpTool.addHelpCodeToDocForSectionId(section.getHeader());
            ViewUtils.runDelayed(McpUtils.guard(result, () -> {
                boolean isJava = workspacePane.getPagePane().getSelPage() instanceof JavaPage;
                Map<String,Object> map = new LinkedHashMap<>();
                map.put("inserted", section.getHeader());
                map.put("file", workspacePane.getSelFile() != null ? WorkspaceMcpTools.getFileRef(workspacePane.getSelFile()) : null);
                if (!isJava)
                    map.put("note", "No Java editor was selected; open a .java/.jepl file and retry if nothing was inserted.");
                result.json(map);
            }), 500);
        });
    }

    /**
     * show_panel
     */
    private static void showPanel(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        WorkspaceTools tools = workspacePane.getWorkspaceTools();
        List<String> done = new ArrayList<>();

        String panel = args.getString("panel");
        if (panel != null && !panel.isEmpty()) {
            Class<? extends WorkspaceTool> toolClass = PANELS.get(panel);
            if (toolClass == null)
                throw new McpException("Unknown panel: " + panel + ". Panels: " + PANELS.keySet());
            if (tools.getToolTrayForToolClass(toolClass) == null)
                throw new McpException("The " + panel + " panel is not available in this version of SnapCode");
            tools.showToolForClass(toolClass);
            done.add("panel:" + panel);
        }
        if (args.has("left_tray")) { tools.setShowLeftTray(args.getBool("left_tray", true)); done.add("left_tray"); }
        if (args.has("right_tray")) { tools.setShowRightTray(args.getBool("right_tray", true)); done.add("right_tray"); }
        if (args.has("bottom_tray")) { tools.setShowBottomTray(args.getBool("bottom_tray", true)); done.add("bottom_tray"); }
        if (args.getBool("home_page", false)) { workspacePane.getPagePane().showHomePage(); done.add("home_page"); }
        if (done.isEmpty())
            throw new McpException("Nothing to show: give panel, a tray flag or home_page");
        result.json(Map.of("shown", done));
    }
}

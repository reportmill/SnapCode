package snapcode.webmcp;
import java.util.*;

/**
 * Describes a single WebMCP tool: name, description, input schema and handler.
 */
public class McpTool {

    // The tool name
    private final String _name;

    // The tool description
    private final String _description;

    // The input schema (JSON schema as nested maps)
    private final Map<String,Object> _inputSchema;

    // A human-readable title (optional)
    private String _title;

    // Whether tool only reads state
    private boolean _readOnly;

    // Whether tool is consequential: irreversible, or acts outside SnapCode (e.g. deletes files, commits to a remote)
    private boolean _consequential;

    // Whether the tool's result is instructions for the agent (a WebMCP "skill"; read-only tools only)
    private boolean _skill;

    // The handler
    private final Handler _handler;

    /**
     * The handler interface. Handlers are always invoked on the SnapKit event thread and must call
     * one of the result methods exactly once (possibly later, e.g. after a build finishes).
     */
    public interface Handler {
        void handle(McpArgs args, McpResult result) throws Exception;
    }

    /**
     * Constructor.
     */
    public McpTool(String aName, String aDescription, Schema aSchema, Handler aHandler)
    {
        _name = aName;
        _description = aDescription;
        _inputSchema = aSchema != null ? aSchema.toMap() : new Schema().toMap();
        _handler = aHandler;
    }

    public String getName()  { return _name; }

    public String getDescription()  { return _description; }

    public Map<String,Object> getInputSchema()  { return _inputSchema; }

    public boolean isReadOnly()  { return _readOnly; }

    /**
     * Marks this tool as read-only (only queries state). Returns this tool for chaining.
     */
    public McpTool readOnly()
    {
        _readOnly = true;
        return this;
    }

    public boolean isConsequential()  { return _consequential; }

    /**
     * Marks this tool as consequential (irreversible or outward-facing), so agents/browsers confirm it with the
     * user. Returns this tool for chaining.
     */
    public McpTool consequential()
    {
        _consequential = true;
        return this;
    }

    public String getTitle()  { return _title; }

    /**
     * Sets a human-readable title (the MCP 'title' annotation). Returns this tool for chaining.
     */
    public McpTool title(String aTitle)
    {
        _title = aTitle;
        return this;
    }

    public boolean isSkill()  { return _skill; }

    /**
     * Marks this read-only tool as a skill: its result is instructions for the agent. Agent hosts that support
     * page skills (e.g. the agentic app framework) offer it to the user to approve as a skill. Returns this tool.
     */
    public McpTool skill()
    {
        _skill = true;
        return this;
    }

    public Handler getHandler()  { return _handler; }

    /**
     * Returns the tool description as a map (for registration).
     */
    public Map<String,Object> toMap()
    {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("name", _name);
        map.put("description", _description);
        map.put("inputSchema", _inputSchema);
        Map<String,Object> annotations = new LinkedHashMap<>();
        if (_title != null)
            annotations.put("title", _title);
        annotations.put("readOnlyHint", _readOnly);
        // Declare both the WebMCP draft name and MCP's older name: under the older spec an undeclared
        // destructiveHint defaults to true, which would make every editing tool look destructive.
        annotations.put("consequentialHint", _consequential);
        annotations.put("destructiveHint", _consequential);
        if (_skill && _readOnly)
            annotations.put("skill", true);
        map.put("annotations", annotations);
        return map;
    }

    /**
     * A small builder for JSON object schemas.
     */
    public static class Schema {

        private final Map<String,Object> _props = new LinkedHashMap<>();
        private final List<String> _required = new ArrayList<>();

        public Schema str(String aName, String aDesc, boolean isRequired)  { return prop(aName, "string", aDesc, isRequired); }

        public Schema integer(String aName, String aDesc, boolean isRequired)  { return prop(aName, "integer", aDesc, isRequired); }

        public Schema number(String aName, String aDesc, boolean isRequired)  { return prop(aName, "number", aDesc, isRequired); }

        public Schema bool(String aName, String aDesc, boolean isRequired)  { return prop(aName, "boolean", aDesc, isRequired); }

        public Schema object(String aName, String aDesc, boolean isRequired)  { return prop(aName, "object", aDesc, isRequired); }

        public Schema enumStr(String aName, String aDesc, boolean isRequired, String... theValues)
        {
            prop(aName, "string", aDesc, isRequired);
            ((Map<String,Object>) _props.get(aName)).put("enum", List.of(theValues));
            return this;
        }

        public Schema strArray(String aName, String aDesc, boolean isRequired)
        {
            prop(aName, "array", aDesc, isRequired);
            ((Map<String,Object>) _props.get(aName)).put("items", Map.of("type", "string"));
            return this;
        }

        private Schema prop(String aName, String aType, String aDesc, boolean isRequired)
        {
            Map<String,Object> prop = new LinkedHashMap<>();
            prop.put("type", aType);
            prop.put("description", aDesc);
            _props.put(aName, prop);
            if (isRequired)
                _required.add(aName);
            return this;
        }

        public Map<String,Object> toMap()
        {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("type", "object");
            map.put("properties", _props);
            if (!_required.isEmpty())
                map.put("required", _required);
            return map;
        }
    }
}

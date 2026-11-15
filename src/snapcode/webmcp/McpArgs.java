package snapcode.webmcp;
import java.util.*;

/**
 * Typed access to the arguments of a WebMCP tool call.
 */
public class McpArgs {

    // The raw args
    private final Map<String,Object> _map;

    /**
     * Constructor.
     */
    public McpArgs(Map<String,Object> aMap)
    {
        _map = aMap != null ? aMap : new LinkedHashMap<>();
    }

    public Map<String,Object> getMap()  { return _map; }

    public boolean has(String aKey)  { return _map.get(aKey) != null; }

    public Object get(String aKey)  { return _map.get(aKey); }

    /**
     * Returns a string value, or null if missing.
     */
    public String getString(String aKey)
    {
        Object value = _map.get(aKey);
        return value != null ? value.toString() : null;
    }

    /**
     * Returns a string value, or the default if missing or blank.
     */
    public String getString(String aKey, String aDefault)
    {
        String value = getString(aKey);
        return value != null && !value.isEmpty() ? value : aDefault;
    }

    /**
     * Returns a required string value (throws McpException if missing).
     */
    public String requireString(String aKey)
    {
        String value = getString(aKey);
        if (value == null)
            throw new McpException("Missing required argument: " + aKey);
        return value;
    }

    public int getInt(String aKey, int aDefault)
    {
        Object value = _map.get(aKey);
        if (value instanceof Number num)
            return num.intValue();
        if (value instanceof String str && !str.isBlank()) {
            try { return (int) Double.parseDouble(str.trim()); }
            catch (NumberFormatException e) { throw new McpException("Argument '" + aKey + "' is not a number: " + str); }
        }
        return aDefault;
    }

    public boolean getBool(String aKey, boolean aDefault)
    {
        Object value = _map.get(aKey);
        if (value instanceof Boolean bool)
            return bool;
        if (value instanceof String str)
            return str.equalsIgnoreCase("true");
        return aDefault;
    }

    @SuppressWarnings("unchecked")
    public Map<String,Object> getObject(String aKey)
    {
        Object value = _map.get(aKey);
        if (value == null)
            return null;
        if (value instanceof Map)
            return (Map<String,Object>) value;
        if (value instanceof String str)
            return Json.parseObject(str);
        throw new McpException("Argument '" + aKey + "' must be an object");
    }

    public List<String> getStringList(String aKey)
    {
        Object value = _map.get(aKey);
        if (value == null)
            return null;
        if (value instanceof Collection<?> coll) {
            List<String> list = new ArrayList<>();
            for (Object item : coll)
                list.add(String.valueOf(item));
            return list;
        }
        return List.of(value.toString());
    }
}

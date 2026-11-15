package snapcode.webmcp;
import java.util.*;

/**
 * A minimal JSON reader/writer for the WebMCP bridge.
 *
 * Values map to: JSON object -> LinkedHashMap, array -> ArrayList, string -> String, number -> Double/Long,
 * true/false -> Boolean, null -> null.
 */
public class Json {

    // The text being parsed and the parse index
    private final String _text;
    private int _index;

    /**
     * Constructor.
     */
    private Json(String aText)
    {
        _text = aText;
    }

    /**
     * Parses the given JSON string.
     */
    public static Object parse(String aString)
    {
        if (aString == null || aString.isBlank())
            return null;
        Json json = new Json(aString);
        json.skipWhitespace();
        Object value = json.readValue();
        json.skipWhitespace();
        if (json._index < json._text.length())
            throw json.error("Unexpected trailing characters");
        return value;
    }

    /**
     * Parses the given JSON string as a JSON object (returns empty map for null/blank).
     */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> parseObject(String aString)
    {
        Object value = parse(aString);
        if (value == null)
            return new LinkedHashMap<>();
        if (!(value instanceof Map))
            throw new IllegalArgumentException("JSON value is not an object");
        return (Map<String,Object>) value;
    }

    /**
     * Reads a value.
     */
    private Object readValue()
    {
        if (_index >= _text.length())
            throw error("Unexpected end of JSON");
        char c = _text.charAt(_index);
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': expectWord("true"); return Boolean.TRUE;
            case 'f': expectWord("false"); return Boolean.FALSE;
            case 'n': expectWord("null"); return null;
            default:
                if (c == '-' || Character.isDigit(c))
                    return readNumber();
                throw error("Unexpected character '" + c + "'");
        }
    }

    private Map<String,Object> readObject()
    {
        Map<String,Object> map = new LinkedHashMap<>();
        _index++; // '{'
        skipWhitespace();
        if (peek() == '}') { _index++; return map; }
        while (true) {
            skipWhitespace();
            if (peek() != '"')
                throw error("Expected string key");
            String key = readString();
            skipWhitespace();
            expectChar(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') return map;
            if (c != ',') throw error("Expected ',' or '}'");
        }
    }

    private List<Object> readArray()
    {
        List<Object> list = new ArrayList<>();
        _index++; // '['
        skipWhitespace();
        if (peek() == ']') { _index++; return list; }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') return list;
            if (c != ',') throw error("Expected ',' or ']'");
        }
    }

    private String readString()
    {
        _index++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (_index >= _text.length())
                throw error("Unterminated string");
            char c = _text.charAt(_index++);
            if (c == '"')
                return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char esc = next();
            switch (esc) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (_index + 4 > _text.length())
                        throw error("Bad unicode escape");
                    sb.append((char) Integer.parseInt(_text.substring(_index, _index + 4), 16));
                    _index += 4;
                    break;
                default: throw error("Bad escape '\\" + esc + "'");
            }
        }
    }

    private Object readNumber()
    {
        int start = _index;
        if (peek() == '-') _index++;
        while (_index < _text.length() && "0123456789.eE+-".indexOf(_text.charAt(_index)) >= 0)
            _index++;
        String numStr = _text.substring(start, _index);
        try {
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E"))
                return Double.parseDouble(numStr);
            return Long.parseLong(numStr);
        }
        catch (NumberFormatException e) { throw error("Bad number: " + numStr); }
    }

    private void expectWord(String aWord)
    {
        if (!_text.startsWith(aWord, _index))
            throw error("Expected '" + aWord + "'");
        _index += aWord.length();
    }

    private void expectChar(char aChar)
    {
        if (next() != aChar)
            throw error("Expected '" + aChar + "'");
    }

    private char peek()  { return _index < _text.length() ? _text.charAt(_index) : 0; }

    private char next()
    {
        if (_index >= _text.length())
            throw error("Unexpected end of JSON");
        return _text.charAt(_index++);
    }

    private void skipWhitespace()
    {
        while (_index < _text.length() && Character.isWhitespace(_text.charAt(_index)))
            _index++;
    }

    private IllegalArgumentException error(String aMsg)
    {
        return new IllegalArgumentException("JSON parse error at " + _index + ": " + aMsg);
    }

    /**
     * Writes the given value (Map, Collection, array, String, Number, Boolean, null) as JSON.
     */
    public static String write(Object aValue)
    {
        StringBuilder sb = new StringBuilder();
        writeValue(aValue, sb);
        return sb.toString();
    }

    private static void writeValue(Object aValue, StringBuilder sb)
    {
        if (aValue == null)
            sb.append("null");
        else if (aValue instanceof String str)
            writeString(str, sb);
        else if (aValue instanceof Boolean)
            sb.append(aValue);
        else if (aValue instanceof Double || aValue instanceof Float) {
            double d = ((Number) aValue).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d))
                sb.append("null");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15)
                sb.append((long) d);
            else sb.append(d);
        }
        else if (aValue instanceof Number)
            sb.append(aValue);
        else if (aValue instanceof Map<?,?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?,?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                writeValue(entry.getValue(), sb);
            }
            sb.append('}');
        }
        else if (aValue instanceof Collection<?> coll) {
            sb.append('[');
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(',');
                first = false;
                writeValue(item, sb);
            }
            sb.append(']');
        }
        else if (aValue instanceof Object[] array)
            writeValue(Arrays.asList(array), sb);
        else writeString(aValue.toString(), sb);
    }

    private static void writeString(String aStr, StringBuilder sb)
    {
        sb.append('"');
        for (int i = 0, iMax = aStr.length(); i < iMax; i++) {
            char c = aStr.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20 || c == 0x2028 || c == 0x2029)
                        sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}

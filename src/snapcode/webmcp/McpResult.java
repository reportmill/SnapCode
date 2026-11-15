package snapcode.webmcp;
import java.util.*;
import java.util.function.Consumer;

/**
 * Collects the result of a WebMCP tool call in MCP "CallToolResult" form:
 *     { content: [ { type: 'text', text }, { type: 'image', data, mimeType } ], isError }
 *
 * A result is completed exactly once, by calling text(), json(), error() or done().
 */
public class McpResult {

    // The content items
    private final List<Map<String,Object>> _content = new ArrayList<>();

    // Whether completed
    private boolean _completed;

    // Whether the browser bridge should replace the content with a capture of the app frame (if available)
    private boolean _captureRunFrame;

    // The consumer to call with the finished result
    private final Consumer<Map<String,Object>> _onComplete;

    /**
     * Constructor.
     */
    public McpResult(Consumer<Map<String,Object>> onComplete)
    {
        _onComplete = onComplete;
    }

    /**
     * Returns whether this result has been completed.
     */
    public boolean isCompleted()  { return _completed; }

    /**
     * Adds a text item (without completing).
     */
    public McpResult addText(String aText)
    {
        Map<String,Object> item = new LinkedHashMap<>();
        item.put("type", "text");
        item.put("text", aText);
        _content.add(item);
        return this;
    }

    /**
     * Adds an image item (without completing).
     */
    public McpResult addImage(byte[] pngBytes)
    {
        Map<String,Object> item = new LinkedHashMap<>();
        item.put("type", "image");
        item.put("data", Base64.getEncoder().encodeToString(pngBytes));
        item.put("mimeType", "image/png");
        _content.add(item);
        return this;
    }

    /**
     * Asks the browser bridge to replace the content with a screenshot of the app frame launched by SnapCode, if
     * one can be captured (apps run in a separate browser frame aren't visible to Java).
     */
    public McpResult setCaptureRunFrame()
    {
        _captureRunFrame = true;
        return this;
    }

    /**
     * Completes with given text.
     */
    public void text(String aText)
    {
        addText(aText);
        done();
    }

    /**
     * Completes with given value written as JSON text.
     */
    public void json(Object aValue)
    {
        addText(Json.write(aValue));
        done();
    }

    /**
     * Completes with an error message.
     */
    public void error(String aMessage)
    {
        if (_completed) return;
        _content.clear();
        addText(aMessage);
        complete(true);
    }

    /**
     * Completes with the content added so far.
     */
    public void done()
    {
        complete(false);
    }

    /**
     * Completes.
     */
    private void complete(boolean isError)
    {
        if (_completed) return;
        _completed = true;
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("content", _content);
        if (isError)
            result.put("isError", true);
        if (_captureRunFrame && !isError)
            result.put("_snapcodeCaptureRunFrame", true);
        _onComplete.accept(result);
    }
}

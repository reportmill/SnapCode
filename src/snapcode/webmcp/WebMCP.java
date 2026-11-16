package snapcode.webmcp;
import snap.util.SnapEnv;
import snap.webapi.*;
import java.util.*;

/**
 * Exposes SnapCode features to AI agents in the browser through WebMCP (document.modelContext, or the older
 * navigator.modelContext).
 *
 * How it works:
 *   - install() adds a small inline script to the page that creates window.__snapcodeWebMCP. That object
 *     registers each tool with document.modelContext or navigator.modelContext (when available).
 *   - When an agent calls a tool, the script dispatches a 'change' CustomEvent (detail = JSON call record)
 *     on a private EventTarget. SnapKit's EventQueue delivers it to Java on the event thread.
 *   - Java runs the tool and calls __snapcodeWebMCP.resolve(id, resultJson), which resolves the agent's promise.
 *
 * The bridge also exposes window.__snapcodeWebMCP.callTool(name, args) and .listTools() so the tools can be
 * exercised from the browser console or a test harness without a WebMCP-capable browser.
 */
public class WebMCP {

    // The JS bridge object
    private static Object _bridgeJS;

    // Whether installed
    private static boolean _installed;

    /**
     * Installs the WebMCP bridge (browser only, once).
     */
    public static void install()
    {
        if (_installed || !SnapEnv.isWebVM)
            return;
        _installed = true;

        // Not when SnapCode is embedded in another site's page (snapcode.js): that is a code widget, not the IDE
        if (snapcode.app.WorkspacePane.isEmbedMode())
            return;

        try { installImpl(); }
        catch (Throwable t) {
            System.err.println("WebMCP: Failed to install: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Installs the bridge.
     */
    private static void installImpl()
    {
        // Add bridge script to document
        HTMLDocument doc = HTMLDocument.getDocument();
        HTMLScriptElement script = (HTMLScriptElement) doc.createElement("script");
        script.setText(BRIDGE_SCRIPT);
        doc.getBody().appendChild(script);

        // Get bridge object and its event target
        WebEnv<Object> webEnv = WebEnv.get();
        _bridgeJS = webEnv.getMember(Window.get().getJS(), "__snapcodeWebMCP");
        if (_bridgeJS == null)
            throw new RuntimeException("Bridge script did not run");
        Object targetJS = webEnv.getMember(_bridgeJS, "target");

        // Listen for tool calls
        BridgeTarget target = new BridgeTarget(targetJS);
        snap.webapi.EventListener<Event> callLsnr = WebMCP::handleToolCallEvent;
        target.addEventListener("change", callLsnr);

        // Register tools
        List<Map<String,Object>> toolMaps = new ArrayList<>();
        for (McpTool tool : McpToolRegistry.getShared().getTools())
            toolMaps.add(tool.toMap());
        webEnv.call(_bridgeJS, "registerTools", Json.write(toolMaps));
        System.out.println("WebMCP: Installed " + toolMaps.size() + " tools");
    }

    /**
     * Called when the JS bridge dispatches a tool call.
     */
    private static void handleToolCallEvent(Event anEvent)
    {
        String callJson = anEvent.getMemberString("detail");
        String callId = null;
        try {
            Map<String,Object> call = Json.parseObject(callJson);
            callId = String.valueOf(call.get("id"));
            String toolName = String.valueOf(call.get("name"));
            Object argsObj = call.get("args");
            @SuppressWarnings("unchecked")
            Map<String,Object> args = argsObj instanceof Map ? (Map<String,Object>) argsObj : new LinkedHashMap<>();
            String finalCallId = callId;
            McpToolRegistry.getShared().callTool(toolName, args, result -> sendResult(finalCallId, result));
        }
        catch (Throwable t) {
            t.printStackTrace();
            if (callId != null) {
                String errCallId = callId;
                McpResult result = new McpResult(res -> sendResult(errCallId, res));
                result.error("WebMCP bridge error: " + t);
            }
        }
    }

    /**
     * Sends a tool result back to JavaScript.
     */
    private static void sendResult(String callId, Map<String,Object> aResult)
    {
        WebEnv.get().call(_bridgeJS, "resolve", callId, Json.write(aResult));
    }

    /**
     * A JSProxy for the bridge EventTarget.
     */
    private static class BridgeTarget extends JSProxy implements EventTarget {
        BridgeTarget(Object targetJS)  { super(targetJS); }
    }

    /**
     * The bridge script.
     */
    private static final String BRIDGE_SCRIPT = """
        (function() {
            if (window.__snapcodeWebMCP) return;
            const target = new EventTarget();
            const pending = new Map();
            let nextId = 1;
            let tools = [];
            let registeredWith = null;

            // Captures the largest canvas of the newest app frame launched by SnapCode (separate-process runs)
            function captureRunFrame() {
                const frames = Array.from(document.querySelectorAll('iframe'))
                    .filter(f => (f.getAttribute('src') || '').includes('launcher.html'));
                for (let i = frames.length - 1; i >= 0; i--) {
                    let doc = null;
                    try { doc = frames[i].contentDocument; } catch (e) { }
                    if (!doc) continue;
                    const canvases = Array.from(doc.querySelectorAll('canvas'));
                    canvases.sort((a, b) => (b.width * b.height) - (a.width * a.height));
                    for (const canvas of canvases) {
                        try {
                            const url = canvas.toDataURL('image/png');
                            const comma = url.indexOf(',');
                            if (comma > 0) return url.substring(comma + 1);
                        } catch (e) { }
                    }
                }
                return null;
            }

            function finishResult(result) {
                if (result && result._snapcodeCaptureRunFrame) {
                    delete result._snapcodeCaptureRunFrame;
                    const data = captureRunFrame();
                    if (data) {
                        result.content = [ { type: 'image', data: data, mimeType: 'image/png' } ];
                        delete result.isError;
                    }
                }
                return result;
            }

            // A call that gets no answer within this time resolves with an error, so an agent is never left waiting
            const CALL_TIMEOUT_MS = 15 * 60 * 1000;

            function callTool(name, args) {
                return new Promise(resolve => {
                    const id = String(nextId++);
                    const timer = setTimeout(() => {
                        if (!pending.has(id)) return;
                        pending.delete(id);
                        resolve({ content: [ { type: 'text', text: 'SnapCode did not answer the ' + name + ' call in time.' } ], isError: true });
                    }, CALL_TIMEOUT_MS);
                    pending.set(id, result => { clearTimeout(timer); resolve(result); });
                    const detail = JSON.stringify({ id: id, name: name, args: args || {} });
                    target.dispatchEvent(new CustomEvent('change', { detail: detail }));
                });
            }

            function toWebMCPTool(tool) {
                return {
                    name: tool.name,
                    description: tool.description,
                    inputSchema: tool.inputSchema,
                    annotations: tool.annotations,
                    execute: (input) => callTool(tool.name, input)
                };
            }

            // Names of tools registered so far, and the registry they were registered with
            const registeredNames = new Set();

            // Registers the tools with the page's model context (once per registry). Each tool is registered on its
            // own, so one rejected tool doesn't stop the others or cause the rest to be registered twice.
            // Returns whether a registry was found.
            function registerWithModelContext() {
                // Newer browsers put the registry on document; earlier drafts used navigator
                const mc = document.modelContext || navigator.modelContext;
                if (!mc || tools.length === 0) return false;
                if (registeredWith !== mc) { registeredWith = mc; registeredNames.clear(); }
                if (typeof mc.registerTool === 'function') {
                    let failed = 0;
                    for (const tool of tools) {
                        if (registeredNames.has(tool.name)) continue;
                        try { mc.registerTool(toWebMCPTool(tool)); registeredNames.add(tool.name); }
                        catch (e) { failed++; console.error('SnapCode WebMCP: could not register ' + tool.name, e); }
                    }
                    console.log('SnapCode WebMCP: registered ' + registeredNames.size + ' tools' + (failed ? ', ' + failed + ' failed' : ''));
                }
                else if (typeof mc.provideContext === 'function') {
                    try { mc.provideContext({ tools: tools.map(toWebMCPTool) }); tools.forEach(t => registeredNames.add(t.name)); }
                    catch (e) { console.error('SnapCode WebMCP: registration failed', e); }
                }
                return true;
            }

            window.__snapcodeWebMCP = {
                target: target,
                resolve: function(id, json) {
                    const res = pending.get(String(id));
                    if (!res) return;
                    pending.delete(String(id));
                    res(finishResult(JSON.parse(json)));
                },
                registerTools: function(json) {
                    tools = JSON.parse(json);
                    if (registerWithModelContext()) return;
                    // Retry briefly only in case modelContext is installed late (e.g. by an extension polyfill)
                    let tries = 0;
                    const timer = setInterval(() => {
                        if (registerWithModelContext() || ++tries > 20) clearInterval(timer);
                    }, 500);
                },
                registerNow: registerWithModelContext,
                listTools: function() { return tools; },
                callTool: callTool
            };
        })();
        """;
}

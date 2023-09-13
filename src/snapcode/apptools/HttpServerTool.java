package snapcode.apptools;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.RichText;
import snap.text.TextBlock;
import snap.text.TextLineStyle;
import snap.text.TextStyle;
import snap.util.FilePathUtils;
import snap.util.ListUtils;
import snap.util.StringUtils;
import snap.view.TextArea;
import snap.view.TextView;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snap.web.*;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * HttpServerTool provides UI for managing an HTTP-Server for the project.
 */
public class HttpServerTool extends WorkspaceTool {

    // The WebSite path
    private String  _sitePath;

    // The HTTPServer
    private HttpServer  _server;

    // The port
    private int  _port = 8080;

    // Cache-control: max-age=20
    private String  _cacheControl = "max-age=20";

    // Whether server is running
    private boolean  _running;

    // The TextView
    private TextView  _textView;

    // The last response code
    private int  _respCode;

    // DateFormat for GMT time
    private static DateFormat _fmt;

    // Colors
    static Color OK_COLOR = Color.LIGHTBLUE;
    static Color ERR_COLOR = Color.RED;

    /**
     * Constructor.
     */
    public HttpServerTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the Site path.
     */
    public String getSitePath()
    {
        if (_sitePath != null) return _sitePath;

        WebSite site = getRootSite();
        File file = site.getRootDir().getJavaFile();
        String sitePath = FilePathUtils.getStandardizedPath(file.getAbsolutePath());
        sitePath = FilePathUtils.getChild(sitePath, "/bin/");

        // Set, return
        return _sitePath = sitePath;
    }

    /**
     * Returns a URL for given path.
     */
    public WebURL getURL(String aPath)
    {
        String sitePath = getSitePath();
        String path = FilePathUtils.getChild(sitePath, aPath);
        return WebURL.getURL(path);
    }

    /**
     * Init UI.
     */
    protected void initUI()
    {
        // Get TextView and configure
        _textView = getView("LogText", TextView.class);
        _textView.setWrapLines(true);

        // Make font bigger and increase space between lines
        TextBlock richText = _textView.getTextBox();
        richText.setDefaultStyle(richText.getDefaultStyle().copyFor(Font.Arial12));
        TextLineStyle lstyle = richText.getDefaultLineStyle();
        TextLineStyle lstyle2 = lstyle.copyFor(TextLineStyle.SPACING_KEY, 2);
        richText.setDefaultLineStyle(lstyle2);
    }

    /**
     * Update the UI.
     */
    protected void resetUI()
    {
        setViewText("StartButton", isRunning() ? "Stop Server" : "Start Server");
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle StartButton
        if (anEvent.equals("StartButton")) {
            if (!isRunning()) startServer();
            else stopServer();
        }

        // Handle ClearButton
        if (anEvent.equals("ClearButton")) {
            TextArea textArea = _textView.getTextArea();
            textArea.clear();
        }
    }

    /**
     * Returns the server.
     */
    public HttpServer getServer()
    {
        if (_server != null) return _server;
        try { _server = createServer(); }
        catch (Exception e) { throw new RuntimeException(e); }
        return _server;
    }

    /**
     * Creates the server.
     */
    protected HttpServer createServer() throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(_port), 0);
        server.createContext("/", new SimpleHttpHandler());
        return server;
    }

    /**
     * Returns whether server is running.
     */
    public boolean isRunning()
    {
        return _server != null && _running;
    }

    /**
     * Starts the server.
     */
    public void startServer()
    {
        if (_running) return;
        try { getServer().start(); }
        catch (Exception e) {
            DialogBox.showConfirmDialog(getUI(), "Server Error", e.toString());
            return;
        }
        _running = true;
        getUI();
        append("Started Server\n");
    }

    /**
     * Stops the server.
     */
    public void stopServer()
    {
        if (!_running) return;
        getServer().stop(0);
        _server = null;
        _running = false;
    }

    /**
     * Prints exchange to server.
     */
    void printExchange(HttpExchange anExch)
    {
        // Append Date
        append("[");
        append(new Date().toString());
        append("] ");

        // Append method and path
        Color color = _respCode == WebResponse.OK ? OK_COLOR : ERR_COLOR;
        String meth = anExch.getRequestMethod();
        String path = anExch.getRequestURI().getPath();
        append("\"");
        append(meth, color);
        append(" ", color);
        append(path, color);
        append("\" ");

        // If error print error
        if (_respCode != WebResponse.OK) {
            append("Error (");
            append(String.valueOf(_respCode), color);
            append("): \"");
            append(WebResponse.getCodeString(_respCode), color);
            append("\"");
        }

        // Otherwise append User-Agent
        else {
            Headers hdrs = anExch.getRequestHeaders();
            List<String> userAgents = hdrs.get("User-agent");
            if (userAgents != null)
                append(StringUtils.getStringQuoted(ListUtils.joinStrings(userAgents, ",")));
        }

        //for(String hdr : hdrs.keySet()) append(hdr + " = " + ListUtils.joinStrings(hdrs.get(hdr),",") + '\n');
        append("\n");
    }

    /**
     * Appends to the text view.
     */
    void append(String aStr)
    {
        append(aStr, Color.BLACK);
    }

    /**
     * Appends to the text view.
     */
    void append(String aStr, Color aColor)
    {
        // If not main thread, return on main thread
        if (!getEnv().isEventThread()) {
            runLater(() -> append(aStr, aColor));
            return;
        }

        // Append text
        TextArea textArea = _textView.getTextArea();
        int textLength = textArea.length();
        TextStyle textStyle = textArea.getStyleForCharIndex(textLength).copyFor(aColor);
        textArea.replaceChars(aStr, textStyle, textLength, textLength, false);
    }

    /**
     * Returns a GMT date string.
     */
    private static String getGMT(Date aDate)
    {
        if (_fmt == null) {
            _fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            _fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        return _fmt.format(aDate);
    }

    /**
     * A simple HttpHandler.
     */
    private class SimpleHttpHandler implements HttpHandler {

        /**
         * Handle.
         */
        public void handle(HttpExchange anExch) throws IOException
        {
            // Get method
            String meth = anExch.getRequestMethod();
            String path = anExch.getRequestURI().getPath();

            // Add ResponseHeaders: Server, Keep-alive
            Headers hdrs = anExch.getResponseHeaders();
            hdrs.add("server", "SnapCode 1.0");
            hdrs.add("Connection", "keep-alive");

            // Handle method
            if (meth.equals("HEAD")) handleHead(anExch);
            else if (meth.equals("GET")) handleGet(anExch);

            printExchange(anExch);
        }

        /**
         * Handle HEAD.
         */
        public void handleHead(HttpExchange anExch) throws IOException
        {
            // Get path and URL
            String path = anExch.getRequestURI().getPath();
            WebURL url = getURL(path);
            WebResponse resp = url.getHead();

            // If response not OK, return error code
            _respCode = resp.getCode();
            if (resp.getCode() != WebResponse.OK) {
                anExch.sendResponseHeaders(resp.getCode(), -1);
                return;
            }

            // Get length and LastModified
            FileHeader fhdr = resp.getFileHeader();
            long len = fhdr.getSize();
            Date lastMod = new Date(fhdr.getModTime());
            String ext = FilePathUtils.getExtension(url.getPath());

            // Add ResponseHeaders: last-modified, cache-control, content-length, content-type
            Headers hdrs = anExch.getResponseHeaders();
            hdrs.add("last-modified", getGMT(lastMod));
            hdrs.add("cache-control", _cacheControl);
            hdrs.add("content-length", String.valueOf(len));
            String mtype = MIMEType.getType(ext);
            if (mtype != null) hdrs.add("content-type", mtype);

            // Get bytes and append
            anExch.sendResponseHeaders(HTTPResponse.OK, -1);
        }

        /**
         * Handle GET.
         */
        public void handleGet(HttpExchange anExch) throws IOException
        {
            // Get path and URL
            String path = anExch.getRequestURI().getPath();
            WebURL url = getURL(path);
            WebResponse resp = url.getResponse();

            // If response not OK, return error code
            _respCode = resp.getCode();
            if (resp.getCode() != WebResponse.OK) {
                anExch.sendResponseHeaders(resp.getCode(), -1);
                return;
            }

            // Get length and LastModified
            FileHeader fhdr = resp.getFileHeader();
            long len = fhdr.getSize();
            Date lastMod = new Date(fhdr.getModTime());
            String ext = FilePathUtils.getExtension(url.getPath());
            byte[] bytes = resp.getBytes();

            // Add ResponseHeaders: last-modified, content-length, content-type
            Headers hdrs = anExch.getResponseHeaders();
            hdrs.add("last-modified", getGMT(lastMod));
            hdrs.add("cache-control", _cacheControl);
            hdrs.add("content-length", String.valueOf(len));
            String mtype = MIMEType.getType(ext);
            if (mtype != null) hdrs.add("content-type", mtype);

            // Append bytes
            anExch.sendResponseHeaders(200, bytes.length);
            OutputStream os = anExch.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    @Override
    public String getTitle()  { return "HTTP Server"; }
}
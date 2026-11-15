package snapcode.webmcp;
import snap.gfx.Image;
import snap.util.SnapEnv;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.apptools.RunTool;
import snapcode.apptools.RunToolUtils;
import snapcode.debug.RunApp;
import snapcode.debug.RunAppLocal;
import snapcode.project.*;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;
import java.util.regex.Pattern;

/**
 * WebMCP tools to build projects and run apps.
 */
public class BuildRunMcpTools {

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("build",
                "Saves all files and builds (compiles) the workspace, waiting for the build to finish. Returns the " +
                "error/warning counts and the issues (file, line, message), plus browser_warnings for code known to break " +
                "in the browser (fix those too). SnapCode also builds automatically after " +
                "saves; use get_build_issues to just read the current issues.",
                new Schema()
                        .bool("clean", "Clean (remove build output) before building (default false).", false)
                        .integer("timeout_seconds", "Max seconds to wait (default 120).", false),
                BuildRunMcpTools::build));

        reg.addTool(new McpTool("get_build_issues",
                "Returns the current compile errors and warnings (file, line, column, message), waiting for any build " +
                "in progress to finish first.",
                new Schema()
                        .str("path", "Only return issues for this file (optional).", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .bool("errors_only", "Only return errors (default false).", false),
                BuildRunMcpTools::getBuildIssues)).readOnly();

        reg.addTool(new McpTool("run_app",
                "Runs a program, like the Run button: builds if needed, then runs the given file's main method " +
                "(or for '.jepl'/'.jmd' scripts, runs the script). With no path, runs the project's configured main " +
                "class or the selected file. Waits 'wait_seconds' and returns the console output so far. If the build " +
                "fails, returns the build errors instead. UI apps (snap.view.ViewOwner with setWindowVisible(true)) " +
                "open in an app window; use screenshot_app to see it. Includes browser_warnings for code known to break " +
                "in the browser.",
                new Schema()
                        .str("path", "File with a main method or a .jepl/.jmd script (optional).", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("main_class", "Fully qualified main class name to run instead of a path (optional).", false)
                        .str("app_args", "Program arguments (optional).", false)
                        .integer("wait_seconds", "Seconds to wait for output before returning (default 3).", false),
                BuildRunMcpTools::runApp));

        reg.addTool(new McpTool("get_console_output",
                "Returns the console output (stdout/stderr, and for REPL scripts the evaluated values) of the selected " +
                "running or finished app, and whether it is still running.",
                new Schema()
                        .integer("max_chars", "Return at most this many characters from the end (default 20000).", false),
                BuildRunMcpTools::getConsoleOutput)).readOnly();

        reg.addTool(new McpTool("send_console_input",
                "Sends a line of text to the running app's standard input (for programs that read System.in / IO.readln).",
                new Schema().str("text", "Text to send (a newline is appended if missing).", true),
                BuildRunMcpTools::sendConsoleInput));

        reg.addTool(new McpTool("stop_app",
                "Stops (terminates) the selected running app, like the Stop button.",
                new Schema(), BuildRunMcpTools::stopApp));

        reg.addTool(new McpTool("screenshot_app",
                "Returns a PNG screenshot of the running app: its app window if it opened one, otherwise the " +
                "Run/Console panel (which shows REPL output such as charts and views).",
                new Schema(), BuildRunMcpTools::screenshotApp)).readOnly();
    }

    /**
     * build
     */
    private static void build(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();
        if (workspace.getProjects().isEmpty())
            throw new McpException("No project is open");
        workspace.saveAllFiles();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (args.getBool("clean", false))
            builder.cleanWorkspace();
        builder.addAllFilesToBuild();
        builder.buildWorkspace();

        int timeout = args.getInt("timeout_seconds", 120) * 1000;
        McpUtils.waitUntil(result, () -> !workspace.isBuilding() && !builder.isBuilding(), timeout,
                () -> result.json(getBuildIssuesMap(workspace, null, false)),
                () -> result.error("Build did not finish within the timeout; call get_build_issues later."));
    }

    /**
     * get_build_issues
     */
    private static void getBuildIssues(McpArgs args, McpResult result)
    {
        Workspace workspace = McpUtils.getWorkspace();
        String path = args.getString("path");
        WebFile file = path != null && !path.isEmpty() ? McpUtils.getFile(args.getString("project"), path) : null;
        boolean errorsOnly = args.getBool("errors_only", false);
        McpUtils.waitUntil(result, () -> !workspace.isBuilding(), 60000,
                () -> result.json(getBuildIssuesMap(workspace, file, errorsOnly)),
                () -> result.json(getBuildIssuesMap(workspace, file, errorsOnly)));
    }

    /**
     * Returns a map describing build issues.
     */
    static Map<String,Object> getBuildIssuesMap(Workspace workspace, WebFile aFile, boolean errorsOnly)
    {
        BuildIssues buildIssues = workspace.getBuildIssues();
        List<BuildIssue> issues = aFile != null ? buildIssues.getIssuesForFile(aFile) : buildIssues.getBuildIssues();

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("errors", buildIssues.getErrorCount());
        map.put("warnings", buildIssues.getWarningCount());
        List<Map<String,Object>> list = new ArrayList<>();
        for (BuildIssue issue : issues) {
            if (errorsOnly && !issue.isError())
                continue;
            Map<String,Object> issueMap = new LinkedHashMap<>();
            issueMap.put("kind", issue.getKind().toString().toLowerCase());
            issueMap.put("file", issue.getFile() != null ? WorkspaceMcpTools.getFileRef(issue.getFile()) : null);
            issueMap.put("line", issue.getLineNumber());
            issueMap.put("column", issue.getColumn() + 1);
            issueMap.put("message", issue.getText());
            list.add(issueMap);
            if (list.size() >= 200) break;
        }
        map.put("issues", list);
        List<Map<String,Object>> browserWarnings = getBrowserWarnings(workspace);
        if (!browserWarnings.isEmpty())
            map.put("browser_warnings", browserWarnings);
        return map;
    }

    /**
     * Code patterns known to break apps when SnapCode runs them in the browser (each was reproduced there).
     * Pattern, then the advice shown to the agent.
     */
    private static final String[][] BROWSER_HAZARDS = {
        { "\\b(ScheduledExecutorService|ScheduledThreadPoolExecutor|newScheduledThreadPool|newSingleThreadScheduledExecutor)\\b",
          "ScheduledExecutorService: stopping it (shutdown/shutdownNow) freezes the whole browser tab in SnapCode web. " +
          "Use snap.view.ViewTimer for periodic work (it runs on the UI thread): new ViewTimer(this::tick, 20) " +
          "with start(), pause() and stop()." },
    };

    // Compiled hazard patterns
    private static final Pattern[] BROWSER_HAZARD_PATTERNS = Arrays.stream(BROWSER_HAZARDS)
            .map(h -> Pattern.compile(h[0])).toArray(Pattern[]::new);

    /**
     * Returns warnings for source lines that use patterns known to break apps in the browser.
     */
    static List<Map<String,Object>> getBrowserWarnings(Workspace workspace)
    {
        List<Map<String,Object>> warnings = new ArrayList<>();
        for (Project proj : workspace.getProjects()) {
            WebFile srcDir = proj.getSourceDir();
            if (srcDir != null)
                findBrowserWarnings(srcDir, proj, warnings);
        }
        return warnings;
    }

    /**
     * Scans a source file or directory for browser hazards.
     */
    private static void findBrowserWarnings(WebFile aFile, Project aProj, List<Map<String,Object>> warnings)
    {
        if (warnings.size() >= 20)
            return;
        if (aFile.isDir()) {
            for (WebFile file : aFile.getFiles())
                findBrowserWarnings(file, aProj, warnings);
            return;
        }
        String type = aFile.getFileType();
        if (!type.equals("java") && !type.equals("jepl") && !type.equals("jmd"))
            return;
        String[] lines = McpUtils.getFileText(aFile).split("\n", -1);
        for (int h = 0; h < BROWSER_HAZARDS.length; h++) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("import ") || line.startsWith("//") || line.startsWith("*"))
                    continue;
                if (BROWSER_HAZARD_PATTERNS[h].matcher(lines[i]).find()) {
                    Map<String,Object> warning = new LinkedHashMap<>();
                    warning.put("file", WorkspaceMcpTools.getFileRef(aFile));
                    warning.put("line", i + 1);
                    warning.put("message", BROWSER_HAZARDS[h][1]);
                    warnings.add(warning);
                    break;   // one warning per hazard per file
                }
            }
        }
    }

    /**
     * run_app
     */
    private static void runApp(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();
        RunTool runTool = workspacePane.getRunTool();

        // Get run config
        RunConfig runConfig;
        String mainClass = args.getString("main_class");
        String path = args.getString("path");
        if (mainClass != null && !mainClass.isEmpty()) {
            runConfig = RunConfig.createRunConfigForWorkspaceAndClassName(workspace, mainClass);
            if (runConfig.getMainJavaFile() == null)
                throw new McpException("No source file found for main class: " + mainClass);
        }
        else if (path != null && !path.isEmpty()) {
            WebFile file = McpUtils.getFile(args.getString("project"), path);
            if (!RunToolUtils.isJavaFileWithMain(file))
                throw new McpException(file.getPath() + " is not runnable (needs a main method, or be a .jepl/.jmd script)");
            runConfig = RunConfig.createRunConfigForJavaFile(file);
        }
        else {
            runConfig = runTool.getRunConfig();
            if (runConfig == null)
                throw new McpException("Nothing to run: give 'path' to a file with a main method or a .jepl script");
        }

        String appArgs = args.getString("app_args");
        if (appArgs != null)
            runConfig.setAppArgs(appArgs);

        // Run
        RunApp prevApp = runTool.getSelApp();
        runTool.runAppForConfig(runConfig, false);

        // Wait for app to start or build to fail
        int waitMillis = Math.max(0, args.getInt("wait_seconds", 3)) * 1000;
        long[] startTime = new long[1];
        McpUtils.waitUntil(result, () -> {
                    if (runTool.getSelApp() != prevApp && runTool.getSelApp() != null)
                        return true;
                    return !workspace.isBuilding() && workspace.getBuildIssues().getErrorCount() > 0;
                }, 180000,
                () -> {
                    // Handle build failure
                    if (runTool.getSelApp() == prevApp || runTool.getSelApp() == null) {
                        Map<String,Object> map = getBuildIssuesMap(workspace, null, true);
                        map.put("ran", false);
                        map.put("reason", "Build failed");
                        result.json(map);
                        return;
                    }

                    // Wait for output, then return it
                    RunApp app = runTool.getSelApp();
                    startTime[0] = System.currentTimeMillis();
                    McpUtils.waitUntil(result, () -> !app.isRunning() && System.currentTimeMillis() - startTime[0] > 300 ||
                                    System.currentTimeMillis() - startTime[0] > waitMillis, waitMillis + 1000,
                            () -> result.json(withBrowserWarnings(getConsoleMap(app, 20000), workspace)),
                            () -> result.json(withBrowserWarnings(getConsoleMap(app, 20000), workspace)));
                },
                () -> result.error("Timed out waiting for the app to start"));
    }

    /**
     * Adds browser warnings (if any) to a result map.
     */
    private static Map<String,Object> withBrowserWarnings(Map<String,Object> aMap, Workspace workspace)
    {
        List<Map<String,Object>> warnings = getBrowserWarnings(workspace);
        if (!warnings.isEmpty())
            aMap.put("browser_warnings", warnings);
        return aMap;
    }

    /**
     * get_console_output
     */
    private static void getConsoleOutput(McpArgs args, McpResult result)
    {
        RunApp app = McpUtils.getWorkspacePane().getRunTool().getSelApp();
        if (app == null)
            throw new McpException("No app has been run yet");
        result.json(getConsoleMap(app, args.getInt("max_chars", 20000)));
    }

    /**
     * Returns a map describing an app and its console output.
     */
    private static Map<String,Object> getConsoleMap(RunApp app, int maxChars)
    {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("app", app.getName());
        map.put("running", app.isRunning());

        // Get console text
        StringBuilder sb = new StringBuilder();
        View altConsole = app.getAltConsoleView();
        if (altConsole != null)
            appendViewText(altConsole, sb);
        else sb.append(app.getConsoleTextView().getText());
        String text = sb.toString();
        if (text.length() > maxChars)
            text = "...[truncated]...\n" + text.substring(text.length() - maxChars);
        map.put("output", text);
        return map;
    }

    /**
     * Appends the visible text of a console view hierarchy (REPL consoles show output as views).
     */
    private static void appendViewText(View aView, StringBuilder sb)
    {
        if (aView instanceof TextArea textArea) {
            appendLine(sb, textArea.getText());
            return;
        }
        if (aView instanceof Label || aView instanceof ButtonBase || aView instanceof TextField) {
            appendLine(sb, aView.getText());
            return;
        }
        if (aView instanceof ParentView parentView) {
            int childCount = parentView.getChildCount();
            for (int i = 0; i < childCount; i++)
                appendViewText(parentView.getChild(i), sb);
            if (childCount == 0 && !(aView instanceof BoxView))
                appendLine(sb, "[" + aView.getClass().getSimpleName() + "]");
        }
        else if (!(aView instanceof Separator))
            appendLine(sb, "[" + aView.getClass().getSimpleName() + "]");
    }

    private static void appendLine(StringBuilder sb, String aStr)
    {
        if (aStr == null || aStr.isEmpty()) return;
        sb.append(aStr);
        if (!aStr.endsWith("\n")) sb.append('\n');
    }

    /**
     * send_console_input
     */
    private static void sendConsoleInput(McpArgs args, McpResult result)
    {
        RunApp app = McpUtils.getWorkspacePane().getRunTool().getSelApp();
        if (app == null || !app.isRunning())
            throw new McpException("No app is running");
        if (SnapEnv.isWebVM && !(app instanceof RunAppLocal))
            throw new McpException("This app runs in a separate browser process, which does not support console " +
                    "input yet. Programs that call IO.readln() run in-process and do support it.");
        String text = args.requireString("text");
        if (!text.endsWith("\n"))
            text += '\n';
        app.sendInput(text);
        result.json(Map.of("sent", text));
    }

    /**
     * stop_app
     */
    private static void stopApp(McpArgs args, McpResult result)
    {
        RunTool runTool = McpUtils.getWorkspacePane().getRunTool();
        RunApp app = runTool.getSelApp();
        if (app == null || !app.isRunning()) {
            result.json(Map.of("stopped", false, "reason", "No app is running"));
            return;
        }
        runTool.cancelRun();
        result.json(Map.of("stopped", true, "app", app.getName()));
    }

    /**
     * screenshot_app
     */
    private static void screenshotApp(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        RunTool runTool = workspacePane.getRunTool();
        RunApp app = runTool.getSelApp();
        if (app == null)
            throw new McpException("No app has been run yet");

        // In browser, apps not run in-process are shown in a separate frame: capture it in JavaScript
        boolean captureFrame = SnapEnv.isWebVM && !(app instanceof RunAppLocal) && app.isRunning();

        // Get image of the visible Run/Console panel (fallback for frame capture)
        if (!runTool.isShowing())
            runTool.showTool();
        View runToolUI = runTool.getUI();
        if (runToolUI.getWidth() > 0 && runToolUI.getHeight() > 0) {
            Image image = ViewUtils.getImageForDpiScale(runToolUI, 1);
            result.addImage(image.getBytesPNG());
        }
        else result.addText("The Run/Console panel is not visible yet; try again.");

        if (captureFrame)
            result.setCaptureRunFrame();
        result.done();
    }
}

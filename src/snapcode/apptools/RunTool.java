package snapcode.apptools;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.Convert;
import snap.util.FilePathUtils;
import snap.util.ListUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcharts.repl.Console;
import snapcharts.repl.ReplObject;
import snap.view.*;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.debug.*;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the run/eval UI.
 */
public class RunTool extends WorkspaceTool implements RunApp.AppListener {

    // The list of recently run apps
    private List<RunApp> _apps = new ArrayList<>();

    // The selected app
    private RunApp _selApp;

    // The limit to the number of running processes
    private int  _procLimit = 1;

    // The last executed file
    private static WebFile _lastRunFile;

    // EvalRunner
    protected EvalToolRunner  _evalRunner;

    // The Console
    protected Console _console;

    // The view that shows when there is an extended run
    private View  _extendedRunView;

    // The view that shows when there is cancelled run
    private View  _cancelledRunView;

    // Constants
    private static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Constructor.
     */
    public RunTool(WorkspacePane workspacePane)
    {
        super(workspacePane);

        // Create console
        _console = new EvalToolConsole(this);

        // Create runner
        _evalRunner = new EvalToolRunner(this);
    }

    /**
     * Returns the list of apps.
     */
    public List<RunApp> getApps()  { return _apps; }

    /**
     * Returns the number of apps.
     */
    public int getAppCount()  { return _apps.size(); }

    /**
     * Returns the app at given index.
     */
    public RunApp getApp(int anIndex)  { return _apps.get(anIndex); }

    /**
     * Adds a new app.
     */
    public void addApp(RunApp runApp)
    {
        // Remove procs that are terminated and procs beyond limit
        for (RunApp p : _apps.toArray(new RunApp[0]))
            if (p.isTerminated())
                removeApp(p);

        if (getAppCount() + 1 > _procLimit) {
            RunApp proc = getApp(0);
            proc.terminate();
            removeApp(proc);
        }

        // Add new proc
        _apps.add(runApp);
        runApp.addListener(this);
        resetLater();
    }

    /**
     * Removes the app at given index.
     */
    public void removeApp(int anIndex)
    {
        _apps.remove(anIndex);
    }

    /**
     * Removes the given app.
     */
    public void removeApp(RunApp aProcess)
    {
        int index = ListUtils.indexOfId(_apps, aProcess);
        if (index >= 0)
            removeApp(index);
    }

    /**
     * Returns the selected app.
     */
    public RunApp getSelApp()  { return _selApp; }

    /**
     * Sets the selected app.
     */
    public void setSelApp(RunApp aProc)
    {
        if (aProc == _selApp) return;
        _selApp = aProc;

        resetLater();
        runLater(() -> resetConsole());
        DebugTool debugTool = getDebugTool();
        debugTool.resetLater();
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getSelDebugApp()  { return _selApp instanceof DebugApp ? (DebugApp) _selApp : null; }

    /**
     * Returns the DebugTool.
     */
    public DebugTool getDebugTool()  { return _workspaceTools.getToolForClass(DebugTool.class); }

    /**
     * Run application.
     */
    public void runDefaultConfig(boolean withDebug)
    {
        WebSite site = getRootSite();
        RunConfig config = RunConfigs.get(site).getRunConfig();
        runConfigOrFile(config, null, withDebug);
    }

    /**
     * Run application.
     */
    public void runConfigForName(String configName, boolean withDebug)
    {
        RunConfigs runConfigs = RunConfigs.get(getRootSite());
        RunConfig runConfig = runConfigs.getRunConfig(configName);
        if (runConfig != null) {
            runConfigs.getRunConfigs().remove(runConfig);
            runConfigs.getRunConfigs().add(0, runConfig);
            runConfigs.writeFile();
            runConfigOrFile(runConfig, null, withDebug);
        }
    }

    /**
     * Runs a given RunConfig or file as a separate process.
     */
    public void runConfigOrFile(RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Automatically save all files
        FilesTool filesTool = _workspaceTools.getFilesTool();
        filesTool.saveAllFiles();

        // Get site and RunConfig (if available)
        WebSite site = getRootSite();
        RunConfig config = aConfig != null || aFile != null ? aConfig : RunConfigs.get(site).getRunConfig();

        // Get file
        WebFile runFile = aFile;
        if (runFile == null && config != null)
            runFile = site.createFileForPath(config.getMainFilePath(), false);
        if (runFile == null)
            runFile = _lastRunFile;
        if (runFile == null)
            runFile = getSelFile();

        // Try to replace file with project file
        Project proj = Project.getProjectForFile(runFile);
        if (proj == null) {
            System.err.println("DebugTool: not project file: " + runFile);
            return;
        }

        // Get class file for given file (should be JavaFile)
        ProjectFiles projectFiles = proj.getProjectFiles();
        WebFile classFile;
        if (runFile.getType().equals("java"))
            classFile = projectFiles.getClassFileForJavaFile(runFile);

            // Try generic way to get class file
        else classFile = projectFiles.getBuildFile(runFile.getPath(), false, runFile.isDir());

        // If ClassFile found, set run file
        if (classFile != null)
            runFile = classFile;

        // Set last run file
        _lastRunFile = runFile;

        // Run/debug file
        String[] runArgs = getRunArgs(proj, config, runFile, isDebug);
        WebURL url = runFile.getURL();
        runAppForArgs(runArgs, url, isDebug);
    }

    /**
     * Runs the provided file as straight app.
     */
    public void runAppForArgs(String[] args, WebURL aURL, boolean isDebug)
    {
        // Print run command to console
        String commandLineStr = String.join(" ", args);
        if (isDebug)
            commandLineStr = "debug " + commandLineStr;
        System.err.println(commandLineStr);

        // Get process
        RunApp proc;
        if (!isDebug)
            proc = new RunAppBin(aURL, args);

            // Handle isDebug: Create DebugApp with Workspace.Breakpoints
        else {
            proc = new DebugApp(aURL, args);
            Breakpoints breakpointsHpr = _workspace.getBreakpoints();
            Breakpoint[] breakpoints = breakpointsHpr.getArray();
            for (Breakpoint breakpoint : breakpoints)
                proc.addBreakpoint(breakpoint);
        }

        // Create RunApp and exec
        execProc(proc);
    }

    /**
     * Executes a proc and adds it to list.
     */
    public void execProc(RunApp aProc)
    {
        setSelApp(null);
        addApp(aProc);
        setSelApp(aProc);
        aProc.exec();
    }

    /**
     * Reset console.
     */
    protected void resetConsole()
    {
        resetDisplay();

        RunApp selApp = getSelApp();
        if (selApp == null)
            return;

        // Reset Console
        for (RunApp.Output out : selApp.getOutput()) {
            if (out.isErr())
                appendErr(selApp, out.getString());
            else appendOut(selApp, out.getString());
        }
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    public void breakpointsDidChange(PropChange pc)
    {
        if (pc.getPropName() != Breakpoints.ITEMS_PROP) return;

        // Get processes
        List<RunApp> processes = getApps();

        // Handle Breakpoint added
        Breakpoint addedBreakpoint = (Breakpoint) pc.getNewValue();
        if (addedBreakpoint != null) {

            // Tell active processes about breakpoint change
            for (RunApp rp : processes)
                rp.addBreakpoint(addedBreakpoint);
        }

        // Handle Breakpoint removed
        else {

            Breakpoint removedBreakpoint = (Breakpoint) pc.getOldValue();

            // Make current JavaPage.TextArea resetLater
            WebPage page = getBrowser().getPageForURL(removedBreakpoint.getFile().getURL());
            if (page instanceof JavaPage)
                ((JavaPage) page).getTextPane().buildIssueOrBreakPointMarkerChanged();

            // Tell active processes about breakpoint change
            for (RunApp rp : processes)
                rp.removeBreakpoint(removedBreakpoint);
        }
    }

    /**
     * RunProc.Listener method - called when process starts.
     */
    public void appStarted(RunApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appStarted(aProc);
    }

    /**
     * RunProc.Listener method - called when process is paused.
     */
    public void appPaused(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appPaused(aProc);
    }

    /**
     * RunProc.Listener method - called when process is continued.
     */
    public void appResumed(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appResumed(aProc);
    }

    /**
     * RunProc.Listener method - called when process ends.
     */
    public void appExited(RunApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.appExited(aProc);
    }

    /**
     * Called when DebugApp gets notice of things like VM start/death, thread start/death, breakpoints, etc.
     */
    public void processDebugEvent(DebugApp aProc, DebugEvent anEvent)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.processDebugEvent(aProc, anEvent);
    }

    /**
     * RunProc.Listener method - called when stack frame changes.
     */
    public void frameChanged(DebugApp aProc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.frameChanged(aProc);
    }

    /**
     * DebugListener breakpoint methods.
     */
    public void requestSet(BreakpointReq e)  { }
    public void requestDeferred(BreakpointReq e)  { }
    public void requestDeleted(BreakpointReq e)  { }
    public void requestError(BreakpointReq e)  { }

    /**
     * RunProc.Listener method - called when output is available.
     */
    public void appendOut(RunApp aProc, String aStr)
    {
        if (getSelApp() == aProc)
            appendOut(aStr);
    }

    /**
     * RunProc.Listener method - called when error output is available.
     */
    public void appendErr(RunApp aProc, String aStr)
    {
        if (getSelApp() == aProc)
            appendErr(aStr);
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        // Make sure we're in app event thread
        if (!isEventThread()) {
            runLater(() -> appendOut(aStr)); return; }

        // Append string in black
        appendString(aStr, Color.BLACK);
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        // Make sure we're in app event thread
        if (!isEventThread()) {
            runLater(() -> appendErr(aStr)); return; }

        // Append string in red
        appendString(aStr, ERROR_COLOR);
    }

    /**
     * Appends text with given color.
     */
    void appendString(String aStr, Color aColor)
    {
        TextArea _textView = new TextArea();

        // Get default style modified for color
        TextStyle style = _textView.getStyleForCharIndex(_textView.length());
        if (_textView.length() > 100000) return;
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int parenStartIndex = aStr.lastIndexOf("(", i);
            int parenEndIndex = aStr.indexOf(")", i);
            if (parenStartIndex < start || parenEndIndex < 0) {
                _textView.addChars(aStr.substring(start, start = i + 6), style);
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, parenStartIndex + 1);
            _textView.addChars(prefix, style);

            // Get link text, link address, TextLink
            String linkText = aStr.substring(parenStartIndex + 1, parenEndIndex);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle lstyle = style.copyFor(textLink);
            _textView.addChars(linkText, lstyle);

            // Update start to end of link text and continue
            start = parenEndIndex;
        }

        // Add remainder normally
        _textView.addChars(aStr.substring(start), style);

        _console.show(_textView);
    }

    /**
     * Returns a link for a StackString.
     */
    protected String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0)
            return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start)
            end = aPrefix.lastIndexOf('.');
        if (end < start)
            end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = Convert.intValue(lineStr);
        if (line > 0)
            path += "#LineNumber=" + line;
        return path;
    }

    /**
     * Returns a source URL for path.
     */
    protected String getSourceURL(String aPath)
    {
        if (aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
            return "https://reportmill.com/jars/8u05/src.zip!" + aPath;
        if (aPath.startsWith("/javafx/"))
            return "https://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;

        Project project = Project.getProjectForSite(getRootSite());
        if (project == null)
            return aPath;

        // Look in project
        WebFile file = project.getSourceFile(aPath, false, false);
        if (file != null)
            return file.getUrlString();

        // Look in child projects
        Project[] projects = project.getProjects();
        for (Project proj : projects) {
            file = proj.getSourceFile(aPath, false, false);
            if (file != null)
                return file.getUrlString();
        }

        // Return not found
        return aPath;
    }

    /**
     * Returns an array of args for given config and file.
     */
    private static String[] getRunArgs(Project aProj, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            commands.add(javaCmdPath);
        }

        // Get Class path and add to list
        String[] classPaths = aProj.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = aProj.getClassNameForFile(aFile);
        commands.add(className);

        // Add App Args
        String appArgs = aConfig != null ? aConfig.getAppArgs() : null;
        if (appArgs != null && appArgs.length() > 0)
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
    }

    /**
     * Runs Java code.
     */
    public void runApp()
    {
        _evalRunner.runApp();
        resetLater();
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()  { return _evalRunner.isRunning(); }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        _evalRunner.cancelRun();
        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void resetDisplay()
    {
        setShowExtendedRunUI(false);
        setShowCancelledRunUI(false);
        _console.resetConsole();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Create Repl Console
        View consoleView = _console.getConsoleView();
        consoleView.setGrowHeight(true);

        // Get ScrollView and config
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        scrollView.setBorder(null);
        scrollView.setContent(consoleView);

        // Get ExtendedRunView
        _extendedRunView = getView("ExtendedRunView");
        _extendedRunView.setVisible(false);
        getView("ProgressBar", ProgressBar.class).setIndeterminate(true);

        // Get CancelledRunView
        _cancelledRunView = getView("CancelledRunView");
        _cancelledRunView.setVisible(false);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update RunButton, TerminateButton
        setViewEnabled("RunButton", !isRunning());
        setViewEnabled("TerminateButton", isRunning());
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunButton, TerminateButton
        if (anEvent.equals("RunButton"))
            runApp();
        else if (anEvent.equals("TerminateButton"))
            cancelRun();

        // Handle ClearButton
        else if (anEvent.equals("ClearButton"))
            resetDisplay();

        // Handle InputTextField: Show input string, add to runner input and clear text
        else if (anEvent.equals("InputTextField")) {
            String inputString = anEvent.getStringValue();
            ReplObject.show(inputString);
            _evalRunner.addSystemInputString(inputString + '\n');
            setViewValue("InputTextField", null);
        }

        // Do normal version
        else super.respondUI(anEvent);
    }

    /**
     * Sets whether ExtendedRunView is showing.
     */
    protected void setShowExtendedRunUI(boolean aValue)
    {
        if (_extendedRunView != null)
            _extendedRunView.setVisible(aValue);
    }

    /**
     * Sets whether CancelledRunView is showing.
     */
    protected void setShowCancelledRunUI(boolean aValue)
    {
        if (_cancelledRunView != null)
            _cancelledRunView.setVisible(aValue);
        if (aValue) {
            String text = "Last run cancelled";
            if (_console.getItemCount() > EvalToolConsole.MAX_OUTPUT_COUNT)
                text += " - Too much output";
            setViewText("CancelledRunLabel", text);
        }
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Console"; }
}

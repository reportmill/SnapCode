package snapcode.apptools;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.util.FilePathUtils;
import snap.view.*;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.app.*;
import snapcode.debug.DebugApp;
import snapcode.debug.RunApp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This project tool class handles running/debugging a process.
 */
public class DebugTool extends WorkspaceTool {

    // The ProcPane manages run/debug processes
    private ProcPane  _procPane;

    // The SplitView
    private SplitView  _splitView;

    // The DebugVarsPane
    private DebugVarsPane _debugVarsPane;

    // The DebugExprsPane
    private DebugExprsPane _debugExprsPane;

    // The last executed file
    private static WebFile  _lastRunFile;

    /**
     * Constructor.
     */
    public DebugTool(WorkspacePane workspacePane)
    {
        super(workspacePane);

        // Create parts
        _procPane = new ProcPane(this);
        _debugVarsPane = new DebugVarsPane(this);
        _debugExprsPane = new DebugExprsPane(this);
    }

    /**
     * Returns the processes pane.
     */
    public ProcPane getProcPane()  { return _procPane; }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()  { return _debugVarsPane; }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()  { return _debugExprsPane; }

    /**
     * Returns the debug app.
     */
    public DebugApp getSelDebugApp()
    {
        return _procPane.getSelDebugApp();
    }

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
            proc = new RunApp(aURL, args);

        // Handle isDebug: Create DebugApp with Workspace.Breakpoints
        else {
            proc = new DebugApp(aURL, args);
            Breakpoints breakpointsHpr = _workspace.getBreakpoints();
            Breakpoint[] breakpoints = breakpointsHpr.getArray();
            for (Breakpoint breakpoint : breakpoints)
                proc.addBreakpoint(breakpoint);
        }

        // Create RunApp and exec
        ProcPane procPane = getProcPane();
        procPane.execProc(proc);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Config RowView
        RowView rowView = getView("RowView", RowView.class);
        rowView.setBorder(Color.GRAY8, 1);

        // Get/config SplitView
        _splitView = getView("SplitView", SplitView.class);
        _splitView.setBorder(null);
        int DIVIDER_SPAN = 2;
        Color DIVIDER_FILL = new Color(.87);
        _splitView.setDividerSpan(DIVIDER_SPAN);
        Divider divider = _splitView.getDivider();
        divider.setFill(DIVIDER_FILL);
        divider.setBorder(null);

        // Add ProcPane
        View procPaneUI = _procPane.getUI();
        _splitView.addItem(procPaneUI);

        // Add DebugVars
        View debugVarsUI = _debugVarsPane.getUI();
        _splitView.addItem(debugVarsUI);

        // Add DebugExprs
        View debugExprsUI = _debugExprsPane.getUI();
        _splitView.addItem(debugExprsUI);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        _procPane.resetLater();
        _debugVarsPane.resetLater();
        _debugExprsPane.resetLater();
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle DebugButton
        if (anEvent.equals("DebugButton"))
            runDefaultConfig(true);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Debug"; }

    /**
     * Called when Workspace.BreakPoints change.
     */
    public void breakpointsDidChange(PropChange pc)
    {
        if (pc.getPropName() != Breakpoints.ITEMS_PROP) return;

        // Get processes
        ProcPane procPane = getProcPane();
        List<RunApp> processes = procPane.getProcs();

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
}

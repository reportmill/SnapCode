package snapcode.app;
import snap.gfx.Image;
import snap.util.SnapEnv;
import snap.viewx.DevPane;
import snapcode.javatext.JavaTextPane;
import snapcode.project.*;
import snap.geom.Side;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snapcode.apptools.*;

/**
 * This class manages all the WorkspaceTool instances for a WorkspacePane.
 */
public class WorkspaceTools {

    // The WorkspacePane
    protected WorkspacePane  _workspacePane;

    // The Workspace
    protected Workspace  _workspace;

    // The FilesTool
    private FilesTool _filesTool;

    // Array of all tools
    protected WorkspaceTool[]  _tools;

    // The left side ToolTray
    protected ToolTray  _leftTray;

    // The right side ToolTray
    protected ToolTray  _rightTray;

    // The bottom side ToolTray
    protected ToolTray  _bottomTray;

    // PropChangeListener for breakpoints
    private PropChangeListener  _breakpointsLsnr = this::handleBreakpointsChange;

    // PropChangeListener for BuildIssues
    private PropChangeListener  _buildIssueLsnr = this::handleBuildIssuesChange;

    /**
     * Constructor.
     */
    public WorkspaceTools(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
        workspacePane._workspaceTools = this;

        // Create tools
        createTools();

        // Set workspace
        setWorkspace(_workspacePane.getWorkspace());
    }

    /**
     * Create tools.
     */
    protected void createTools()
    {
        // Main tools
        _filesTool = new FilesTool(_workspacePane);
        ProjectFilesTool projectFilesTool = new ProjectFilesTool(_workspacePane);
        NewFileTool newFileTool = new NewFileTool(_workspacePane);
        RunTool runTool = new RunTool(_workspacePane);
        DebugTool debugTool = new DebugTool(_workspacePane, runTool);
        BuildTool buildTool = new BuildTool(_workspacePane);

        // Support tools
        HelpTool helpTool = new HelpTool(_workspacePane);
        BlocksTool blocksTool = new BlocksTool(_workspacePane);

        // Support tools
        SearchTool searchTool = new SearchTool(_workspacePane);
        RunConfigsTool runConfigsTool = new RunConfigsTool(_workspacePane);
        BreakpointsTool breakpointsTool = new BreakpointsTool(_workspacePane);
        HttpServerTool httpServerTool = new HttpServerTool(_workspacePane);
        ClassesTool classesTool = new ClassesTool(_workspacePane);

        // Create tools array
        _tools = new WorkspaceTool[] {
                _filesTool, projectFilesTool, newFileTool,
                runTool, debugTool, buildTool,
                searchTool, helpTool, blocksTool, classesTool,
                runConfigsTool, breakpointsTool,
                httpServerTool
        };

        // Configure tray tool lists
        WorkspaceTool[] leftTools = { helpTool, projectFilesTool };
        WorkspaceTool[] rightTools = { runTool, debugTool, buildTool, searchTool, classesTool, blocksTool };
        WorkspaceTool[] bottomTools = { runConfigsTool, breakpointsTool };
        if (WorkspacePane._embedMode) {
            leftTools = new WorkspaceTool[] { projectFilesTool };
            rightTools = new WorkspaceTool[] { runTool, helpTool };
        }
        if (SnapEnv.isWebVM) {
            rightTools = ArrayUtils.remove(rightTools, debugTool);
            bottomTools = new WorkspaceTool[0];
        }
        if (helpTool.isLesson())
            rightTools = ArrayUtils.moveToFront(rightTools, helpTool);

        // Create LeftTray, RightTray, BottomTray
        _leftTray = new ToolTray(Side.LEFT, leftTools);
        _rightTray = new ToolTray(Side.RIGHT, rightTools);
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);
        if (SnapEnv.isWebVM)
            _bottomTray.getUI(TabView.class).getTabBar().setMinHeight(30);

        // Add DevToolsButton
        addDevToolsButton();
    }

    /**
     * Sets the workspace.
     */
    protected void setWorkspace(Workspace aWorkspace)
    {
        if (aWorkspace == _workspace) return;

        // Remove
        if (_workspace != null) {

            // Remove listeners
            Breakpoints breakpoints = _workspace.getBreakpoints();
            breakpoints.removePropChangeListener(_breakpointsLsnr);
            BuildIssues buildIssues = _workspace.getBuildIssues();
            buildIssues.removePropChangeListener(_buildIssueLsnr);
        }

        // Set workspace
        _workspace = aWorkspace;

        // Add
        if (_workspace != null) {

            // Add listeners
            Breakpoints breakpoints = _workspace.getBreakpoints();
            breakpoints.addPropChangeListener(_breakpointsLsnr);
            BuildIssues buildIssues = _workspace.getBuildIssues();
            buildIssues.addPropChangeListener(_buildIssueLsnr);

            // Update tools
            for (WorkspaceTool tool : _tools)
                tool._workspace = _workspace;
        }
    }

    /**
     * Returns the files tool.
     */
    public FilesTool getFilesTool()  { return _filesTool; }

    /**
     * Returns the files tool.
     */
    public NewFileTool getNewFileTool()  { return getToolForClass(NewFileTool.class); }

    /**
     * Returns the ProjectFilesTool.
     */
    public ProjectFilesTool getProjectFilesTool()  { return getToolForClass(ProjectFilesTool.class); }

    /**
     * Returns the RunTool.
     */
    public RunTool getRunTool()  { return getToolForClass(RunTool.class); }

    /**
     * Returns the BuildTool.
     */
    public BuildTool getBuildTool()  { return getToolForClass(BuildTool.class); }

    /**
     * Returns the HelpTool.
     */
    public HelpTool getHelpTool()  { return getToolForClass(HelpTool.class); }

    /**
     * Returns the left side tray.
     */
    public ToolTray getLeftTray()  { return _leftTray; }

    /**
     * Returns the right side tray.
     */
    public ToolTray getRightTray()  { return _rightTray; }

    /**
     * Returns the support tray.
     */
    public ToolTray getBottomTray()  { return _bottomTray; }

    /**
     * Returns whether left tray should be showing.
     */
    public boolean isShowLeftTray()  { return _leftTray.getUI().isVisible(); }

    /**
     * Sets whether left tray should be showing.
     */
    public void setShowLeftTray(boolean aValue)  { setTrayVisible(_leftTray, aValue); }

    /**
     * Returns whether right tray should be showing.
     */
    public boolean isShowRightTray()  { return _rightTray.getUI().isVisible(); }

    /**
     * Sets whether right tray should be showing.
     */
    public void setShowRightTray(boolean aValue)  { setTrayVisible(_rightTray, aValue); }

    /**
     * Returns whether bottom tray should be showing.
     */
    public boolean isShowBottomTray()  { return _bottomTray.getUI().isVisible(); }

    /**
     * Sets whether bottom tray should be showing.
     */
    public void setShowBottomTray(boolean aValue)  { setTrayVisible(_bottomTray, aValue); }

    /**
     * Sets given tray UI visible - using anim if parent onscreen.
     */
    private void setTrayVisible(ViewOwner aTray, boolean aValue)
    {
        View trayUI = aTray.getUI();
        View trayParent = trayUI.getParent();
        if (trayParent.isShowing())
            ViewAnimUtils.setVisible(trayUI, aValue, true, true);
        else trayUI.setVisible(aValue);
    }

    /**
     * Returns the tool for given class.
     */
    public <T extends WorkspaceTool> T getToolForClass(Class<T> aToolClass)
    {
        return (T) ArrayUtils.findMatch(_tools, tool -> aToolClass.isInstance(tool));
    }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends WorkspaceTool> aClass)
    {
        WorkspaceTool workspaceTool = getToolForClass(aClass);
        showTool(workspaceTool);
    }

    /**
     * Sets the selected index for given class.
     */
    public void showTool(WorkspaceTool workspaceTool)
    {
        ToolTray toolTray = getToolTrayForTool(workspaceTool);
        if (toolTray != null)
            toolTray.setSelTool(workspaceTool);
    }

    /**
     * Returns the ToolTray for given tool.
     */
    public ToolTray getToolTrayForTool(WorkspaceTool workspaceTool)
    {
        if (ArrayUtils.containsId(_leftTray._trayTools, workspaceTool))
            return _leftTray;
        if (ArrayUtils.containsId(_rightTray._trayTools, workspaceTool))
            return _rightTray;
        if (ArrayUtils.containsId(_bottomTray._trayTools, workspaceTool))
            return _bottomTray;
        return null;
    }

    /**
     * Reset visible tools.
     */
    public void resetLater()
    {
        _leftTray.resetLater();
        _rightTray.resetLater();
        _bottomTray.resetLater();
    }

    /**
     * Notification that workspace is closing.
     */
    protected void workspaceIsClosing()
    {
        // Notify all tools of close
        for (WorkspaceTool tool : _tools) {
            if (!tool.workspaceIsClosing())
                break;
        }
    }

    /**
     * Notification that project is closing.
     */
    protected void projectIsClosing(Project aProject)
    {
        // Notify all tools of close
        for (WorkspaceTool tool : _tools)
            tool.projectIsClosing(aProject);
    }

    /**
     * Called when Workspace.BreakPoints change.
     */
    protected void handleBreakpointsChange(PropChange pc)
    {
        RunTool runTool = getRunTool();
        runTool.handleBreakpointsChange(pc);
    }

    /**
     * Called when Project.BuildIssues change.
     */
    protected void handleBuildIssuesChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Get issue added or removed
        BuildIssue issue = (BuildIssue) pc.getNewValue();
        if (issue == null)
            issue = (BuildIssue) pc.getOldValue();

        // Make current JavaPage.TextArea resetLater
        WebFile issueFile = issue.getFile();
        WebPage page = _workspacePane.getBrowser().getPageForURL(issueFile.getURL());
        if (page instanceof JavaPage) {
            JavaTextPane javaTextPane = ((JavaPage) page).getTextPane();
            javaTextPane.handleBuildIssueOrBreakPointMarkerChange();
        }

        // Update ProjectFilesTool
        getProjectFilesTool().handleFileChange(issueFile);

        // Update BuildTool
        getBuildTool().resetLater();
    }

    /**
     * Adds DevToolsButton.
     */
    private void addDevToolsButton()
    {
        // Add DevToolsButton
        Button devToolsButton = new Button();
        devToolsButton.setImage(Image.getImageForClassResource(getClass(), "pkg.images/DevTools.png"));
        devToolsButton.setPropsString("Margin: 0,8,0,0; Padding: 3; ShowArea: false; Managed: false; LeanX: RIGHT; LeanY: CENTER;");
        devToolsButton.addEventHandler(e -> DevPane.toggleDevPaneShowing(_workspacePane.getUI()), View.Action);
        devToolsButton.setPrefSize(24, 24);
        devToolsButton.setSize(24, 24);
        devToolsButton.setPadding(0, 0, 0, 0);
        TabView tabView = (TabView) _bottomTray.getUI();
        ViewUtils.addChild(tabView.getTabBar(), devToolsButton);
    }
}

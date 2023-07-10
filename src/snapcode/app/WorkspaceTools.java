package snapcode.app;
import snapcode.javatext.JavaTextPane;
import snapcode.project.Breakpoints;
import snapcode.project.BuildIssue;
import snapcode.project.BuildIssues;
import snapcode.project.Workspace;
import snap.geom.Side;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.*;
import snapcode.util.SamplesPane;

/**
 * This class manages all the WorkspaceTool instances for a WorkspacePane.
 */
public class WorkspaceTools {

    // The WorkspacePane
    protected WorkspacePane  _workspacePane;

    // The Workspace
    protected Workspace  _workspace;

    // Array of all tools
    protected WorkspaceTool[]  _tools;

    // The left side ToolTray
    protected ToolTray  _leftTray;

    // The right side ToolTray
    protected ToolTray  _rightTray;

    // The bottom side ToolTray
    protected ToolTray  _bottomTray;

    // PropChangeListener for breakpoints
    private PropChangeListener  _breakpointsLsnr = pc -> breakpointsDidChange(pc);

    // PropChangeListener for BuildIssues
    private PropChangeListener  _buildIssueLsnr = pc -> buildIssuesDidChange(pc);

    // Samples button
    private Button  _samplesButton;

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
        // Create tools
        FilesTool filesTool = new FilesTool(_workspacePane);
        FileTreeTool fileTreeTool = new FileTreeTool(_workspacePane);
        EvalTool evalTool = new EvalTool(_workspacePane);
        HelpTool helpTool = new HelpTool(_workspacePane);
        CompleterTool completerTool = new CompleterTool(_workspacePane);
        ProblemsTool problemsTool = new ProblemsTool(_workspacePane);

        // Create tools array
        _tools = new WorkspaceTool[] { filesTool, fileTreeTool, evalTool, helpTool, completerTool, problemsTool };

        // Create LeftTray
        WorkspaceTool[] leftTools = { fileTreeTool };
        _leftTray = new ToolTray(Side.LEFT, leftTools);

        // Create RightTray
        WorkspaceTool[] rightTools = { evalTool, helpTool, completerTool };
        _rightTray = new ToolTray(Side.RIGHT, rightTools);

        // Create BottomTray
        WorkspaceTool[] bottomTools = { problemsTool };
        _bottomTray = new ToolTray(Side.BOTTOM, bottomTools);

        // Add SamplesButton to RightTray
        addSamplesButton();
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
    public FilesTool getFilesTool()  { return getToolForClass(FilesTool.class); }

    /**
     * Returns the FileTreeTool.
     */
    public FileTreeTool getFileTreeTool()  { return getToolForClass(FileTreeTool.class); }

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
        if (ArrayUtils.containsId(_leftTray._trayTools, workspaceTool))
            _leftTray.setSelTool(workspaceTool);
        else if (ArrayUtils.containsId(_rightTray._trayTools, workspaceTool))
            _rightTray.setSelTool(workspaceTool);
        else if (ArrayUtils.containsId(_bottomTray._trayTools, workspaceTool))
            _bottomTray.setSelTool(workspaceTool);
    }

    /**
     * Runs the given file.
     */
    public void runFile(WebFile aFile, boolean isDebug)  { }

    /**
     * Reset visible tools.
     */
    public void resetLater()
    {
        _bottomTray.resetLater();
        _leftTray.resetLater();
        _rightTray.resetLater();
    }

    /**
     * Closes the project.
     */
    public void closeProject()  { }

    /**
     * Called when Workspace.BreakPoints change.
     */
    protected void breakpointsDidChange(PropChange pc)  { }

    /**
     * Called when Project.BuildIssues change.
     */
    protected void buildIssuesDidChange(PropChange pc)
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
            JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
            javaTextPane.buildIssueOrBreakPointMarkerChanged();
        }

        // Update FilesPane.FilesTree
        FileTreeTool fileTreeTool = getFileTreeTool();
        fileTreeTool.updateChangedFile(issueFile);

        // Update ProblemsTool
        ProblemsTool problemsTool = getToolForClass(ProblemsTool.class);
        if (problemsTool != null && problemsTool.isShowing())
            problemsTool.resetLater();
    }

    /**
     * Shows samples.
     */
    public void showSamples()
    {
        stopSamplesButtonAnim();
        new SamplesPane().showSamples(_workspacePane, url -> showSamplesDidReturnURL(url));
    }

    /**
     * Called when SamplesPane returns a URL.
     */
    private void showSamplesDidReturnURL(WebURL aURL)
    {
        _workspacePane.openWorkspaceForSource(aURL);

        // Kick off run
        EvalTool evalTool = getToolForClass(EvalTool.class);
        if (evalTool != null && !evalTool.isAutoRun())
            evalTool.runApp(false);
    }

    /**
     * Animate SampleButton.
     */
    public void startSamplesButtonAnim()
    {
        View samplesButton = getSamplesButton();
        samplesButton.setVisible(true);
        SamplesPane.startSamplesButtonAnim(samplesButton);
    }

    /**
     * Stops SampleButton animation.
     */
    private void stopSamplesButtonAnim()
    {
        View samplesButton = getSamplesButton();
        SamplesPane.stopSamplesButtonAnim(samplesButton);
    }

    /**
     * Adds the SamplesButton to RightTray.
     */
    private void addSamplesButton()
    {
        // Get SamplesButton
        Button samplesButton = getSamplesButton();

        // Add to RightTray.UI.TabBar.TabsBox
        TabView tabView = _rightTray.getUI(TabView.class);
        TabBar tabBar = tabView.getTabBar();
        ParentView tabsBox = tabBar.getTabsBox();
        ViewUtils.addChild(tabsBox, samplesButton);
        tabsBox.setSpacing(6);
    }

    /**
     * Adds the SamplesButton to RightTray.
     */
    private Button getSamplesButton()
    {
        // If already set, just return
        if (_samplesButton != null) return _samplesButton;

        // Create/config button
        Button samplesButton = new ViewBuilder<>(Button.class).name("SamplesButton").text("Samples").build();
        samplesButton.setPrefWidth(80);
        samplesButton.setMargin(2, 0, 2, 0);
        samplesButton.setPadding(3, 7, 3, 7);
        samplesButton.addEventHandler(e -> showSamples(), View.Action);

        // Set/return
        return _samplesButton = samplesButton;
    }
}

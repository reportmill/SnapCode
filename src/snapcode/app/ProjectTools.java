package snapcode.app;
import javakit.ide.JavaTextPane;
import javakit.project.Breakpoints;
import javakit.project.BuildIssue;
import snap.geom.Side;
import snap.props.PropChange;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snapcode.apptools.*;

/**
 * This class manages all of the ProjectTools for a PodPane.
 */
public class ProjectTools {

    // The FilesPane
    protected FilesTool  _filesTool;

    // The FilesPane
    protected FileTreeTool _fileTreeTool;

    // The Problems pane
    private ProblemsTool _problemsTool;

    // The RunConsole
    private RunConsole _runConsole;

    // The BreakpointsTool
    private BreakpointsTool _breakpointsTool;

    // The PodPane
    private PodPane  _podPane;

    // The DebugTool
    private DebugTool  _debugTool;

    // The SearchTool
    private SearchPane  _searchTool;

    // The RunConfigs tool
    private RunConfigsTool  _runConfigsTool;

    // The HttpServerTool
    private HttpServerTool  _httpServerTool;

    // The VcsTools
    private VcsTools  _vcsTools;

    // The SupportTray
    private SupportTray  _supportTray;

    // The SideBar
    private SupportTray  _sideBar;

    /**
     * Constructor.
     */
    public ProjectTools(PodPane podPane)
    {
        super();
        _podPane = podPane;
    }

    /**
     * Create tools.
     */
    protected void createTools()
    {
        _filesTool = new FilesTool(_podPane);
        _fileTreeTool = new FileTreeTool(_podPane);
        _problemsTool = new ProblemsTool(_podPane);
        _runConsole = new RunConsole(_podPane);
        _breakpointsTool = new BreakpointsTool(_podPane);
        _debugTool = new DebugTool(_podPane);
        _searchTool = new SearchPane(_podPane);
        _runConfigsTool = new RunConfigsTool(_podPane);
        _httpServerTool = new HttpServerTool(_podPane);
        _vcsTools = new VcsTools(_podPane);

        // Set tools
        ProjectTool[] bottomTools = {_problemsTool, _debugTool, _runConsole, _breakpointsTool, _searchTool, _runConfigsTool, _httpServerTool };
        _supportTray = new SupportTray(_podPane, Side.BOTTOM, bottomTools);


        ProjectTool[] sideTools = {_fileTreeTool};
        _sideBar = new SupportTray(_podPane, Side.LEFT, sideTools);
    }

    /**
     * Returns the files tool.
     */
    public FilesTool getFilesTool()  { return _filesTool; }

    /**
     * Returns the FileTreeTool.
     */
    public FileTreeTool getFileTreeTool()  { return _fileTreeTool; }

    /**
     * Returns the problems tool.
     */
    public ProblemsTool getProblemsTool()  { return _problemsTool; }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _runConsole; }

    /**
     * Returns the BreakpointsTool.
     */
    public BreakpointsTool getBreakpointsTool()  { return _breakpointsTool; }

    /**
     * Returns the debug tool.
     */
    public DebugTool getDebugTool()  { return _debugTool; }

    /**
     * Returns the search tool.
     */
    public SearchPane getSearchTool()  { return _searchTool; }

    /**
     * Returns the support tray.
     */
    public SupportTray getSupportTray()  { return _supportTray; }

    /**
     * Returns the sidebar.
     */
    public SupportTray getSideBar()  { return _sideBar; }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends ProjectTool> aClass)
    {
        _supportTray.setSelToolForClass(aClass);
    }

    /**
     * Reset visible tools.
     */
    public void resetLater()
    {
        _supportTray.resetLater();
        _sideBar.resetLater();
    }

    /**
     * Closes the project.
     */
    public void closeProject()
    {
        _httpServerTool.stopServer();
    }

    /**
     * Called when Project.BreakPoints change.
     */
    protected void projBreakpointsDidChange(PropChange pc)
    {
        DebugTool debugTool = getDebugTool();
        debugTool.projBreakpointsDidChange(pc);
    }

    /**
     * Called when Project.BuildIssues change.
     */
    protected void projBuildIssuesDidChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Get issue added or removed
        BuildIssue issue = (BuildIssue) pc.getNewValue();
        if (issue == null)
            issue = (BuildIssue) pc.getOldValue();

        // Make current JavaPage.TextArea resetLater
        WebFile issueFile = issue.getFile();
        WebPage page = _podPane.getBrowser().getPageForURL(issueFile.getURL());
        if (page instanceof JavaPage) {
            JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
            javaTextPane.buildIssueOrBreakPointMarkerChanged();
        }

        // Update FilesPane.FilesTree
        FileTreeTool fileTreeTool = getFileTreeTool();
        fileTreeTool.updateFile(issueFile);
    }
}

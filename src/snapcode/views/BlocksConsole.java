package snapcode.views;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspacePaneUtils;
import snapcode.app.WorkspaceTool;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextPane;
import snapcode.project.JavaAgent;
import snapcode.project.JavaTextModel;
import snapcode.project.Project;
import snapcode.webbrowser.WebPage;

/**
 * This class manages a blocks project.
 */
public class BlocksConsole extends WorkspaceTool {

    // The Stage Pane
    private StagePane _stagePane;

    // The Code pane
    private JavaTextPane _javaTextPane;

    // The Code pane
    private SnapEditorPane _snapEditorPane;

    // The BlocksTool
    private BlocksTool _blocksTool;

    /**
     * Constructor.
     */
    public BlocksConsole(WorkspacePane workspacePane)
    {
        super(workspacePane);
        addPropChangeListener(pc -> handleShowingChange(), Showing_Prop);
        _blocksTool = new BlocksTool(workspacePane);
    }

    /**
     * Sets current actor file.
     */
    protected void setCodeEditorForClassName(String className)
    {
        Project rootProject = _workspacePane.getRootProject();
        WebFile javaFile = rootProject != null ? rootProject.getJavaFileForClassName("Actor1") : null;
        if (javaFile == null)
            return;

        // Get WebPage
        WebPage webPage = _pagePane.getBrowser().getPageForURL(javaFile.getUrl());
        if (webPage == null)
            webPage = _pagePane.getBrowser().createPageForURL(javaFile.getUrl());

        // Add CodePane
        _javaTextPane = webPage instanceof JavaPage javaPage ? javaPage.getTextPane() : new JavaTextPane();
        _javaTextPane.getUI();

        // Get java text model for java file
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
        JavaTextModel javaTextModel = javaAgent.getJavaTextModel();

        // Set java text and FirstFocus
        JavaTextArea javaTextArea = _javaTextPane.getTextArea();
        javaTextArea.setTextModel(javaTextModel);
        setFirstFocus(javaTextArea);

        //
        _snapEditorPane = new SnapEditorPane(_javaTextPane);
        View snapEditorPaneUI = _snapEditorPane.getUI();

        // Add
        BoxView codeColView = getView("CodeBoxView", BoxView.class);
        codeColView.setContent(snapEditorPaneUI);

        // Select file
        _workspacePane.openFile(javaFile);
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        RowView mainUI = (RowView) UILoader.loadViewForString(BLOCKS_CONSOLE_UI);

        // Add StagePane
        _stagePane = new StagePane(_workspacePane);
        mainUI.addChild(_stagePane.getUI(), 0);

        // Add BlocksTool
        View blocksToolUI = _blocksTool.getUI();
        BoxView blocksBoxView = (BoxView) mainUI.getChildForName("BlocksBoxView");
        blocksBoxView.setContent(blocksToolUI);

        // Return
        return mainUI;
    }

    /**
     * Initialize when showing.
     */
    @Override
    protected void initShowing()
    {
        if (_workspacePane.getRootProject() == null) {
            WorkspacePaneUtils.openTempBlocksProject(_workspacePane);
            runLater(() -> setCodeEditorForClassName("Actor1"));
        }

        else setCodeEditorForClassName("Actor1");
    }

    /**
     * Called when showing changes.
     */
    private void handleShowingChange()
    {
        SplitView pagePaneSplitView = _workspacePane.getView("PagePaneSplitView", SplitView.class);
        if (isShowing()) {
            pagePaneSplitView.removeItem(_pagePane.getUI());
            _workspaceTools.getLeftTray().getUI().setGrowWidth(true);
        }
        else {
            pagePaneSplitView.addItem(_pagePane.getUI(), 1);
            _workspaceTools.getLeftTray().getUI().setGrowWidth(false);
        }
    }

    @Override
    public String getTitle()  { return "Blocks"; }

    // The UI
    private static final String BLOCKS_CONSOLE_UI = """
            <RowView GrowWidth="true" FillHeight="true">
              <BoxView Name="CodeBoxView" Margin="5" Padding="5" PrefWidth="400" GrowWidth="true" FillWidth="true" FillHeight="true" Fill="#FF" Border="#C0" BorderRadius="4" />
              <BoxView Name="BlocksBoxView" Margin="5" Padding="5" PrefWidth="310" FillWidth="true" FillHeight="true" Fill="#FF" Border="#C0" BorderRadius="3" />
            </RowView>
            """;
}

package snapcode.views;
import snap.games.GameView;
import snap.view.BoxView;
import snap.view.UILoader;
import snap.view.View;
import snap.view.ViewOwner;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.project.Project;

/**
 * Stage Pane
 */
public class StagePane extends ViewOwner {

    // The workspace pane
    private WorkspacePane _workspacePane;

    // The stage file
    private WebFile _stageFile;

    // The stage box
    private BoxView _stageBox;

    /**
     * Constructor.
     */
    public StagePane(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
    }

    /**
     * Opens the stage.
     */
    public void openStageFile()
    {
        WebFile stageFile = getStageFile();

        GameView gameView = (GameView) UILoader.loadViewForUrl(stageFile.getUrl());
        _stageBox.setContent(gameView);
    }

    /**
     * Returns the stage file.
     */
    public WebFile getStageFile()
    {
        if (_stageFile != null) return _stageFile;
        Project project = _workspacePane.getRootProject();
        return _stageFile = project.getFileForPath("/src/Stage1.snp");
    }

    @Override
    protected View createUI()
    {
        return UILoader.loadViewForString(STAGE_PANE_UI);
    }

    @Override
    protected void initUI()
    {
        _stageBox = getView("StageBox", BoxView.class);
    }

    @Override
    protected void initShowing()
    {
        openStageFile();
    }

    // The UI
    private static final String STAGE_PANE_UI = """
        <ColView Name="MainColView" PrefWidth="500" FillWidth="true">
          <BoxView Name="StageBox" Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" FillWidth="true" FillHeight="true" />
          <BoxView Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" PrefHeight="300" GrowHeight="true" />
        </ColView>
        """;
}

package snapcode.views;
import snap.games.Actor;
import snap.games.GameView;
import snap.view.*;
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

    // The current game view
    private GameView _gameView;

    // The selected actor
    private Actor _selActor;

    // The stage box
    private StagePaneBoxView _stageBox;

    // Constants for properties
    public static final String SelActor_Prop = "SelActor";

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
        setGameView(gameView);
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

    /**
     * Returns the current game view.
     */
    public GameView getGameView()  { return _gameView; }

    /**
     * Sets the current game view.
     */
    public void setGameView(GameView gameView)
    {
        if (gameView == _gameView) return;
        _gameView = gameView;
        _gameView.addEventHandler(_stageBox::handleGameViewEvent, MousePress, MouseDrag, MouseRelease);
        _stageBox.setContent(_gameView);

        // Select first actor
        Actor firstActor = !_gameView.getActors().isEmpty() ? _gameView.getActors().get(0) : null;
        setSelActor(firstActor);
    }

    /**
     * Returns the selected actor.
     */
    public Actor getSelActor()  { return _selActor; }

    /**
     * Sets the selected actor.
     */
    public void setSelActor(Actor selActor)
    {
        if (selActor == _selActor) return;
        firePropChange(SelActor_Prop, _selActor, _selActor = selActor);
        _stageBox.repaint();
    }

    /**
     * Called when stage changes.
     */
    protected void handleStageChanged()
    {
        WebFile stageFile = getStageFile();
        if (stageFile.getUpdater() == null)
            stageFile.setUpdater(file -> updateStageFile());
    }

    /**
     * Updates stage file.
     */
    private void updateStageFile()
    {
        String stageFileText = new ViewArchiver().toXML(getGameView()).getString();
        WebFile stageFile = getStageFile();
        stageFile.setText(stageFileText);
    }

    @Override
    protected View createUI()
    {
        return UILoader.loadViewForString(STAGE_PANE_UI);
    }

    @Override
    protected void initUI()
    {
        _stageBox = getView("StageBox", StagePaneBoxView.class);
        _stageBox.initWithStagePane(this);
    }

    @Override
    protected void initShowing()
    {
        openStageFile();
    }

    // The UI
    private static final String STAGE_PANE_UI = """
        <ColView Name="MainColView" PrefWidth="500" FillWidth="true">
          <BoxView Name="StageBox" Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" FillWidth="true" FillHeight="true" Class="snapcode.views.StagePaneBoxView" />
          <BoxView Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" PrefHeight="300" GrowHeight="true" />
        </ColView>
        """;
}

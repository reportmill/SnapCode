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
    private BoxView _stageBox;

    // The last mouse actor
    private Actor _mouseActor;

    // The last mouse X/Y
    private double _lastMouseX, _lastMouseY;

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
        _gameView.addEventHandler(this::handleGameViewEvent, MousePress, MouseDrag, MouseRelease);
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
        _selActor = selActor;
    }

    /**
     * Called when game view gets event.
     */
    private void handleGameViewEvent(ViewEvent anEvent)
    {
        switch (anEvent.getType()) {
            case MousePress -> handleGameViewMousePress(anEvent);
            case MouseDrag -> handleGameViewMouseDrag(anEvent);
            case MouseRelease -> handleGameViewMouseRelease(anEvent);
        }
    }

    /**
     * Called when game view gets mouse press event.
     */
    private void handleGameViewMousePress(ViewEvent anEvent)
    {
        View mouseView = ViewUtils.getDeepestViewAt(_gameView, anEvent.getX(), anEvent.getY());
        if (mouseView instanceof Actor mouseActor) {
            _mouseActor = mouseActor;
            setSelActor(mouseActor);
            _lastMouseX = anEvent.getX();
            _lastMouseY = anEvent.getY();
        }
    }

    /**
     * Called when game view gets mouse drag event.
     */
    private void handleGameViewMouseDrag(ViewEvent anEvent)
    {
        if (_mouseActor == null) return;
        Actor selActor = getSelActor();
        double dx = anEvent.getX() - _lastMouseX;
        double dy = anEvent.getY() - _lastMouseY;
        selActor.setX(selActor.getX() + dx);
        selActor.setY(selActor.getY() + dy);
        _lastMouseX = anEvent.getX();
        _lastMouseY = anEvent.getY();
    }

    /**
     * Called when game view gets mouse release event.
     */
    private void handleGameViewMouseRelease(ViewEvent anEvent)
    {
        _mouseActor = null;
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

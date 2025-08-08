package snapcode.views;
import snap.games.Actor;
import snap.games.GameView;
import snap.geom.Point;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.project.Project;

import java.util.List;

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
        resetLater();
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
    protected void initUI()
    {
        _stageBox = getView("StageBox", StagePaneBoxView.class);
        _stageBox.initWithStagePane(this);

        setViewItems("ImageComboBox", List.of("Car", "Cat", "Dog", "Duke", "Robot"));
    }

    @Override
    protected void initShowing()
    {
        openStageFile();
    }

    @Override
    protected void resetUI()
    {
        Actor selActor = getSelActor(); if (selActor == null) return;

        // Update NameText
        setViewValue("NameText", selActor.getName());

        // Update XText, XThumbWheel, YText, YThumbWheel
        Point centerX = selActor.localToParent(selActor.getWidth() / 2, selActor.getHeight() / 2);
        setViewValue("XText", centerX.x);
        setViewValue("XThumbWheel", centerX.x);
        setViewValue("YText", centerX.y);
        setViewValue("YThumbWheel", centerX.y);

        // Update WidthText, WidthThumbWheel, HeightText, HeightThumbWheel
        setViewValue("WidthText", selActor.getWidth());
        setViewValue("WidthThumbWheel", selActor.getWidth());
        setViewValue("HeightText", selActor.getHeight());
        setViewValue("HeightThumbWheel", selActor.getHeight());

        // Update RotateText, RotateThumbWheel
        setViewValue("RotateText", selActor.getRotate());
        setViewValue("RotateThumbWheel", selActor.getRotate());

        // Update ImageComboBox
        setViewSelItem("ImageComboBox", selActor.getImageName());
    }

    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        Actor selActor = getSelActor(); if (selActor == null) return;

        switch (anEvent.getName()) {

            // Handle NameText
            case "NameText" -> selActor.setName(anEvent.getStringValue());

            // Handle XText, XThumbWheel, YText, YThumbWheel
            case "XText", "XThumbWheel" -> setActorX(selActor, anEvent.getFloatValue());
            case "YText", "YThumbWheel" -> setActorY(selActor, anEvent.getFloatValue());

            // Handle WidthText, WidthThumbWheel, HeightText, HeightThumbWheel
            case "WidthText", "WidthThumbWheel" -> setActorWidth(selActor, anEvent.getFloatValue());
            case "HeightText", "HeightThumbWheel" -> setActorHeight(selActor, anEvent.getFloatValue());

            // Handle RotateText, RotateThumbWheel
            case "RotateText", "RotateThumbWheel" -> selActor.setRotate(anEvent.getIntValue());

            // Handle ImageComboBox
            case "ImageComboBox" -> selActor.setImageForName(anEvent.getStringValue());
        }

        _stageBox.repaint();
        _stageBox._actorCopy = null;
        handleStageChanged();
    }

    /**
     * Sets the given actor new X.
     */
    private void setActorX(Actor actor, float aX)
    {
        double oldX = actor.localToParent(actor.getWidth() / 2, actor.getHeight() / 2).x;
        double dx = aX - oldX;
        actor.setX(actor.getX() + dx);
    }

    /**
     * Sets the given actor new Y.
     */
    private void setActorY(Actor actor, float aY)
    {
        double oldY = actor.localToParent(actor.getWidth() / 2, actor.getHeight() / 2).y;
        double dy = aY - oldY;
        actor.setY(actor.getY() + dy);
    }

    /**
     * Sets the given actor new width.
     */
    private void setActorWidth(Actor actor, float aWidth)
    {
        double dx = (aWidth - actor.getWidth()) / 2;
        actor.setWidth(aWidth);
        actor.setPrefWidth(aWidth);
        actor.setX(actor.getX() + dx);
    }

    /**
     * Sets the given actor new height.
     */
    private void setActorHeight(Actor actor, float aHeight)
    {
        double dy = (aHeight - actor.getHeight()) / 2;
        actor.setHeight(aHeight);
        actor.setPrefHeight(aHeight);
        actor.setY(actor.getY() + dy);
    }
}

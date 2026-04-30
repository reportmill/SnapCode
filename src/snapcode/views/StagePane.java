package snapcode.views;
import snap.games.ActorView;
import snap.games.StageView;
import snap.geom.Point;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.project.Project;
import java.util.List;

/**
 * Stage Pane
 */
public class StagePane extends ViewController {

    // The workspace pane
    private WorkspacePane _workspacePane;

    // The stage file
    private WebFile _stageFile;

    // The current stage view
    private StageView _stageView;

    // The selected actor
    private ActorView _selActor;

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

        StageView stageView = (StageView) UILoader.loadViewForUrl(stageFile.getUrl());
        setStageView(stageView);
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
     * Returns the current stage view.
     */
    public StageView getStageView()  { return _stageView; }

    /**
     * Sets the current stage view.
     */
    public void setStageView(StageView stageView)
    {
        if (stageView == _stageView) return;
        _stageView = stageView;
        _stageView.addEventHandler(_stageBox::handleStageViewEvent, MousePress, MouseDrag, MouseRelease);
        _stageBox.setContent(_stageView);

        // Select first actor
        ActorView firstActor = !_stageView.getActors().isEmpty() ? _stageView.getActors().get(0).getActorView() : null;
        setSelActor(firstActor);
    }

    /**
     * Returns the selected actor.
     */
    public ActorView getSelActor()  { return _selActor; }

    /**
     * Sets the selected actor.
     */
    public void setSelActor(ActorView selActor)
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
        String stageFileText = new ViewArchiver().toXML(getStageView()).getString();
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
        ActorView selActor = getSelActor(); if (selActor == null) return;

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
        ActorView selActor = getSelActor(); if (selActor == null) return;

        switch (anEvent.getName()) {

            // Handle NameText
            case "NameText" -> selActor.setName(anEvent.getStringValue());

            // Handle XText, XThumbWheel, YText, YThumbWheel
            case "XText", "XThumbWheel" -> setActorX(selActor, anEvent.getIntValue());
            case "YText", "YThumbWheel" -> setActorY(selActor, anEvent.getIntValue());

            // Handle WidthText, WidthThumbWheel, HeightText, HeightThumbWheel
            case "WidthText", "WidthThumbWheel" -> setActorWidth(selActor, anEvent.getIntValue());
            case "HeightText", "HeightThumbWheel" -> setActorHeight(selActor, anEvent.getIntValue());

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
    private void setActorX(ActorView actor, float aX)  { actor.setX(aX - actor.getWidth() / 2); }

    /**
     * Sets the given actor new Y.
     */
    private void setActorY(ActorView actor, float aY)  { actor.setY(aY - actor.getHeight() / 2); }

    /**
     * Sets the given actor new width.
     */
    private void setActorWidth(ActorView actorView, float aWidth)
    {
        actorView.setX(actorView.getX() + (aWidth - actorView.getWidth()) / 2);
        actorView.setWidth(aWidth);
        actorView.setPrefWidth(aWidth);
    }

    /**
     * Sets the given actor new height.
     */
    private void setActorHeight(ActorView actorView, float aHeight)
    {
        actorView.setY(actorView.getY() + (aHeight - actorView.getHeight()) / 2);
        actorView.setHeight(aHeight);
        actorView.setPrefHeight(aHeight);
    }
}

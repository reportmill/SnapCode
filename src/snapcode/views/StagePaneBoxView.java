package snapcode.views;
import snap.games.Actor;
import snap.gfx.Color;
import snap.gfx.Effect;
import snap.gfx.Painter;
import snap.gfx.ShadowEffect;
import snap.util.ClassUtils;
import snap.view.BoxView;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

/**
 * Custom box class to paint selected actor.
 */
public class StagePaneBoxView extends BoxView {

    // The Stage Pane
    protected StagePane _stagePane;

    // The copy of actor
    private Actor _actorCopy;

    // The last mouse actor
    private Actor _mouseActor;

    // The last mouse X/Y
    private double _lastMouseX, _lastMouseY;

    // Constants for selected actor effect
    private static Color FOCUSED_COLOR = Color.get("#039ED3");
    private static final Effect SEL_ACTOR_EFFECT = new ShadowEffect(10, FOCUSED_COLOR, 0, 0);

    /**
     * Constructor.
     */
    public StagePaneBoxView()
    {
        super();
    }

    /**
     * Called to set stage pane.
     */
    protected void initWithStagePane(StagePane stagePane)
    {
        _stagePane = stagePane;
        _stagePane.addPropChangeListener(pc -> _actorCopy = null, StagePane.SelActor_Prop);
    }

    /**
     * Returns the selected actor copy.
     */
    public Actor getSelActorCopy()
    {
        if (_actorCopy != null) return _actorCopy;

        // Get selected actor
        Actor selActor = _stagePane.getSelActor();
        if (selActor == null)
            return null;

        // Create/configure copy
        _actorCopy = ClassUtils.newInstance(selActor.getClass());
        _actorCopy.setImage(selActor.getImage());
        _actorCopy.setSize(selActor.getWidth(), selActor.getHeight());
        _actorCopy.setRotate(selActor.getRotate());
        _actorCopy.setScaleX(selActor.getScaleX());
        _actorCopy.setScaleY(selActor.getScaleY());
        _actorCopy.setEffect(SEL_ACTOR_EFFECT);

        // Return
        return _actorCopy;
    }

    /**
     * Override to paint selected actor.
     */
    @Override
    protected void paintAbove(Painter aPntr)
    {
        // Get selected actor copy
        Actor selActor = _stagePane.getSelActor();
        Actor selActorCopy = getSelActorCopy();
        if (selActorCopy == null) return;

        // Repaint SelView so selection is behind
        aPntr.save();
        aPntr.transform(selActor.getLocalToParent());
        ViewUtils.paintView(selActorCopy, aPntr);
        aPntr.restore();
    }

    /**
     * Called when game view gets event.
     */
    protected void handleGameViewEvent(ViewEvent anEvent)
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
        View mouseView = ViewUtils.getDeepestViewAt(getContent(), anEvent.getX(), anEvent.getY());
        if (mouseView instanceof Actor mouseActor) {
            _mouseActor = mouseActor;
            _stagePane.setSelActor(mouseActor);
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
        Actor selActor = _stagePane.getSelActor();

        double dx = anEvent.getX() - _lastMouseX;
        double dy = anEvent.getY() - _lastMouseY;

        // If alt key down, rotate actor
        if (anEvent.isAltDown()) {
            double deltaRotation = Math.abs(dx) > Math.abs(dy) ? dx : dy;
            selActor.setRotate(selActor.getRotate() + deltaRotation);
        }

        // Otherwise move actor
        else {
            selActor.setX(selActor.getX() + dx);
            selActor.setY(selActor.getY() + dy);
        }

        // Update last mouse point and repaint
        _lastMouseX = anEvent.getX();
        _lastMouseY = anEvent.getY();
        repaint();
        _stagePane.handleStageChanged();
    }

    /**
     * Called when game view gets mouse release event.
     */
    private void handleGameViewMouseRelease(ViewEvent anEvent)
    {
        _mouseActor = null;
    }
}

package snapcode.app;
import javakit.parse.JeplTextDoc;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.view.*;
import snap.viewx.TextPane;

/**
 * This class manages the run/eval UI.
 */
public class EvalPane extends ViewOwner {

    // The DocPane
    private DocPane  _docPane;

    // Whether to auto run code
    private boolean  _autoRun = true;

    // EvalView
    protected EvalView  _evalView;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRun;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRunReal = () -> resetEvalValuesNow();

    /**
     * Constructor.
     */
    public EvalPane(DocPane aDocPane)
    {
        super();
        _docPane = aDocPane;
    }

    /**
     * Returns the DocPane.
     */
    public DocPane getDocPane()  { return _docPane; }

    /**
     * Returns the JeplDoc.
     */
    public JeplTextDoc getJeplDoc()  { return _docPane.getJeplDoc(); }

    /**
     * Returns whether to automatically run when enter key is pressed in edit pane.
     */
    public boolean isAutoRun()  { return _autoRun; }

    /**
     * Sets whether to automatically run when enter key is pressed in edit pane.
     */
    public void setAutoRun(boolean aValue)  { _autoRun = aValue; }

    /**
     * Reset Repl values.
     */
    public void resetEvalValues()
    {
        if (_resetEvalValuesRun == null)
            runLater(_resetEvalValuesRun = _resetEvalValuesRunReal);
        resetLater();
    }

    /**
     * Reset Repl values.
     */
    protected void resetEvalValuesNow()
    {
        try { _evalView.resetView(); }
        finally { _resetEvalValuesRun = null; }
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()  { return _evalView.isRunning(); }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        _evalView.cancelRun();
        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void clearEvalValues()
    {
        _evalView.removeChildren();
    }

    /**
     * Override to add EvalView.
     */
    @Override
    protected View createUI()
    {
        // Do normal version
        ColView colView = (ColView) super.createUI();

        // Create/config EvalView
        _evalView = new EvalView(this);
        _evalView.setGrowHeight(true);

        // Create/config ScrollView
        ScrollView scrollView = new ScrollView(_evalView);
        scrollView.setFillWidth(true);
        scrollView.setGrowHeight(true);
        colView.addChild(scrollView);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Set DeleteButton image
        getView("RunButton", ButtonBase.class).setImageAfter(getImage("pkg.images/Run.png"));
        getView("RunButton", ButtonBase.class).getLabel().setTextFill(Color.GRAY);
        getView("DeleteButton", ButtonBase.class).setImage(Image.get(TextPane.class, "pkg.images/Edit_Delete.png"));
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update RunButton, AutoRunCheckBox
        setViewText("RunButton", !_evalView.isRunning() ? "Run" : "Cancel");
        setViewValue("AutoRunCheckBox", isAutoRun());
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunButton
        if (anEvent.equals("RunButton")) {
            if (!isRunning())
                resetEvalValues();
            else cancelRun();
        }

        // Handle AutoRunCheckBox
        if (anEvent.equals("AutoRunCheckBox"))
            setAutoRun(!isAutoRun());

        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            _evalView.removeChildren();
    }
}

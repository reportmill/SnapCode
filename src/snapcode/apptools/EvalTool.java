package snapcode.apptools;
import javakit.ide.JavaTextArea;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import snap.geom.HPos;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.text.TextBoxLine;
import snap.view.*;
import snap.viewx.TextPane;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * This class manages the run/eval UI.
 */
public class EvalTool extends WorkspaceTool {

    // Whether to auto run code
    private boolean  _autoRun;

    // Whether auto run was requested
    protected boolean  _autoRunRequested;

    // EvalRunner
    protected EvalToolRunner  _evalRunner;

    // EvalView
    protected EvalView  _evalView;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRun;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRunReal = () -> runAppNow();

    // The view that shows when there is an extended run
    private View  _extendedRunView;

    // The view that shows when there is cancelled run
    private View  _cancelledRunView;

    /**
     * Constructor.
     */
    public EvalTool(WorkspacePane workspacePane)
    {
        super(workspacePane);

        // Create runner
        _evalRunner = new EvalToolRunner(this);
    }

    /**
     * Returns whether to automatically run when enter key is pressed in edit pane.
     */
    public boolean isAutoRun()  { return _autoRun; }

    /**
     * Sets whether to automatically run when enter key is pressed in edit pane.
     */
    public void setAutoRun(boolean aValue)  { _autoRun = aValue; }

    /**
     * Runs Java code.
     */
    public void runApp(boolean autoRunRequested)
    {
        _autoRunRequested = autoRunRequested;
        if (_resetEvalValuesRun == null)
            runLater(_resetEvalValuesRun = _resetEvalValuesRunReal);
        resetLater();
    }

    /**
     * Reset Repl values.
     */
    protected void runAppNow()
    {
        try { _evalRunner.runApp(); }
        finally {
            _resetEvalValuesRun = null;
            _autoRunRequested = false;
        }
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()  { return _evalRunner.isRunning(); }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        _evalRunner.cancelRun();
        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void resetDisplay()
    {
        setShowExtendedRunUI(false);
        setShowCancelledRunUI(false);
        _evalView.resetDisplay();
    }

    /**
     * Triggers auto run if it makes sense. Called when newline is entered.
     */
    public void autoRunIfDesirable(JavaTextArea textArea)
    {
        // If AutoRun not set, just return
        if (!isAutoRun())
            return;

        // If inside method decl, just return
        JNode selNode = textArea.getSelNode();
        if (selNode != null && selNode.getParent(JMethodDecl.class) != null)
            return;

        // If previous line is empty whitespace, just return
        TextBoxLine textLine = textArea.getSel().getStartLine();
        TextBoxLine prevLine = textLine.getPrevious();
        if (prevLine.isWhiteSpace())
            return;

        // Trigger auto run
        runApp(true);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Create EvalView
        _evalView = new EvalView(this);
        _evalView.setGrowHeight(true);

        // Get ScrollView and config
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        scrollView.setBorder(null);
        scrollView.setContent(_evalView);

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
        setViewText("RunButton", !_evalRunner.isRunning() ? "Run" : "Cancel");
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
                runApp(false);
            else cancelRun();
        }

        // Handle AutoRunCheckBox
        if (anEvent.equals("AutoRunCheckBox")) {
            setAutoRun(!isAutoRun());
            if (isAutoRun())
                runApp(true);
        }

        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            resetDisplay();
    }

    /**
     * Sets whether ExtendedRunView is showing.
     */
    protected void setShowExtendedRunUI(boolean aValue)
    {
        ColView topColView = (ColView) getUI();

        // Handle Show: Get/add ExtendedRunView
        if (aValue) {
            View extendedRunView = getExtendedRunView();
            if (!extendedRunView.isShowing())
                topColView.addChild(extendedRunView, 1);
        }

        // Handle Hide: Remove ExtendedRunView
        else {
            if (_extendedRunView != null && _extendedRunView.isShowing())
                topColView.removeChild(_extendedRunView);
        }
    }

    /**
     * Sets whether CancelledRunView is showing.
     */
    protected void setShowCancelledRunUI(boolean aValue)
    {
        ColView topColView = (ColView) getUI();

        // Handle Show: Get/add CancelledRunView
        if (aValue) {
            View cancelledRunView = getCancelledRunView();
            if (!cancelledRunView.isShowing())
                topColView.addChild(cancelledRunView, 1);
        }

        // Handle Hide: Remove CancelledRunView
        else {
            if (_cancelledRunView != null && _cancelledRunView.isShowing())
                topColView.removeChild(_cancelledRunView);
            _cancelledRunView = null;
        }
    }

    /**
     * Returns the view to show when there is an extended run.
     */
    private View getExtendedRunView()
    {
        // If already set, just return
        if (_extendedRunView != null) return _extendedRunView;

        // Add separator
        RectView separator = new RectView();
        separator.setPrefSize(1, 1);
        separator.setGrowWidth(true);
        separator.setFill(Color.GRAY8);

        // Create label
        Label label = new Label("Running...");
        label.setLeanX(HPos.CENTER);

        // Create ProgressBar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefSize(400, 25);
        progressBar.setLeanX(HPos.CENTER);
        progressBar.setIndeterminate(true);

        // Create Cancel button
        Button button = new Button("Cancel");
        button.setPrefSize(80, 25);
        button.setLeanX(HPos.CENTER);
        button.addEventHandler(e -> cancelRun(), Button.Action);
        button.setMargin(8, 8, 8, 8);

        // Create ColView to hold
        ColView colView = new ColView();
        colView.setSpacing(8);
        colView.setGrowWidth(true);
        colView.setChildren(separator, label, progressBar, button);

        // Set, return
        return _extendedRunView = colView;
    }

    /**
     * Returns the view to show when there is a cancelled run.
     */
    protected View getCancelledRunView()
    {
        // If already set, just return
        if (_cancelledRunView != null) return _cancelledRunView;

        // Add separator
        RectView separator = new RectView();
        separator.setPrefSize(1, 1);
        separator.setGrowWidth(true);
        separator.setFill(Color.GRAY8);

        // Create label
        Label label = new Label("Last run cancelled");
        label.setLeanX(HPos.CENTER);
        label.setMargin(8, 8, 8, 8);

        // Handle AutoRun timeout
        if (_autoRunRequested)
            label.setText(label.getText() + " - exceeded AutoRun timeout");
        if (_evalView.getChildCount() > EvalView.MAX_OUTPUT_COUNT)
            label.setText(label.getText() + " - Too much output");

        // Create ColView to hold
        ColView colView = new ColView();
        colView.setGrowWidth(true);
        colView.setChildren(separator, label);

        // Return
        return _cancelledRunView = colView;
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Debug"; }
}

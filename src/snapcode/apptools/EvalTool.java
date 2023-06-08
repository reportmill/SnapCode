package snapcode.apptools;
import snapcharts.repl.Console;
import snapcode.javatext.JavaTextArea;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
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

    // The Console
    protected Console _console;

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

        // Create console
        _console = new EvalToolConsole(this);

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
        _console.resetConsole();
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
        // Create Repl Console
        View consoleView = _console.getConsoleView();
        consoleView.setGrowHeight(true);

        // Get ScrollView and config
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        scrollView.setBorder(null);
        scrollView.setContent(consoleView);

        // Set RunButton, DeleteButton image
        getView("RunButton", ButtonBase.class).setImageAfter(getImage("pkg.images/Run.png"));
        getView("RunButton", ButtonBase.class).getLabel().setTextFill(Color.GRAY);
        getView("DeleteButton", ButtonBase.class).setImage(Image.getImageForClassResource(TextPane.class, "pkg.images/Edit_Delete.png"));

        // Get ExtendedRunView
        _extendedRunView = getView("ExtendedRunView");
        _extendedRunView.setVisible(false);
        getView("ProgressBar", ProgressBar.class).setIndeterminate(true);

        // Get CancelledRunView
        _cancelledRunView = getView("CancelledRunView");
        _cancelledRunView.setVisible(false);
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
        else if (anEvent.equals("AutoRunCheckBox")) {
            setAutoRun(!isAutoRun());
            if (isAutoRun())
                runApp(true);
        }

        // Handle DeleteButton
        else if (anEvent.equals("DeleteButton"))
            resetDisplay();

        // Handle CancelExtendedRunButton
        else if (anEvent.equals("CancelExtendedRunButton"))
            cancelRun();
    }

    /**
     * Sets whether ExtendedRunView is showing.
     */
    protected void setShowExtendedRunUI(boolean aValue)
    {
        if (_extendedRunView != null)
            _extendedRunView.setVisible(aValue);
    }

    /**
     * Sets whether CancelledRunView is showing.
     */
    protected void setShowCancelledRunUI(boolean aValue)
    {
        if (_cancelledRunView != null)
            _cancelledRunView.setVisible(aValue);
        if (aValue) {
            String text = "Last run cancelled";
            if (_autoRunRequested)
                text += " - exceeded AutoRun timeout";
            if (_console.getItemCount() > EvalToolConsole.MAX_OUTPUT_COUNT)
                text += " - Too much output";
            setViewText("CancelledRunLabel", text);
        }
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Debug"; }
}

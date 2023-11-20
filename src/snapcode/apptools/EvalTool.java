package snapcode.apptools;
import snapcharts.repl.Console;
import snapcharts.repl.ReplObject;
import snap.view.*;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * This class manages the run/eval UI.
 */
public class EvalTool extends WorkspaceTool {

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
     * Runs Java code.
     */
    public void runApp()
    {
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
        // Update RunButton, TerminateButton
        setViewEnabled("RunButton", !isRunning());
        setViewEnabled("TerminateButton", isRunning());
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunButton, TerminateButton
        if (anEvent.equals("RunButton"))
            runApp();
        else if (anEvent.equals("TerminateButton"))
            cancelRun();

        // Handle ClearButton
        else if (anEvent.equals("ClearButton"))
            resetDisplay();

        // Handle InputTextField: Show input string, add to runner input and clear text
        else if (anEvent.equals("InputTextField")) {
            String inputString = anEvent.getStringValue();
            ReplObject.show(inputString);
            _evalRunner.addSystemInputString(inputString + '\n');
            setViewValue("InputTextField", null);
        }

        // Do normal version
        else super.respondUI(anEvent);
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
            if (_console.getItemCount() > EvalToolConsole.MAX_OUTPUT_COUNT)
                text += " - Too much output";
            setViewText("CancelledRunLabel", text);
        }
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Console"; }
}

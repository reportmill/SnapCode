/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.parse.JStmt;
import javakit.parse.JeplTextDoc;
import javakit.runner.JavaShell;
import snap.geom.HPos;
import snap.gfx.Color;
import snap.view.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A TextArea subclass to show code evaluation.
 */
class EvalView extends ColView implements JavaShell.ShellClient {

    // The EvalPane
    private EvalPane  _evalPane;

    // The JavaShell
    protected JavaShell  _javaShell;

    // A cache of views for output values
    private Map<Object,View>  _replViewsCache = new HashMap<>();

    // The thread to reset views
    private Thread  _resetViewsThread;

    // Whether last run was pre-empted
    private boolean  _preemptedForNewRun;

    // The view that shows when there is an extended run
    private View  _extendedRunView;

    // Constants
    private static final int MAX_OUTPUT_COUNT = 1000;

    /**
     * Constructor.
     */
    public EvalView(EvalPane anEvalPane)
    {
        _evalPane = anEvalPane;
        setSpacing(6);
        setFill(new Color(.98));

        // Set Padding to match TextArea
        setPadding(5, 5, 5, 5);

        // Create JavaShell
        _javaShell = new JavaShell();
        _javaShell.setClient(this);
    }

    public JeplTextDoc getJeplDoc()  { return _evalPane.getJeplDoc(); }

    /**
     * Called to update when textView changes.
     */
    public void resetView()
    {
        // Remove children
        removeChildren();

        // Reset actual values
        resetEvalValues();
    }

    /**
     * Updates the notebook.
     */
    protected void resetEvalValues()
    {
        // If previous thread is running, kill it
        if (_resetViewsThread != null) {
            _preemptedForNewRun = true;
            _javaShell.interrupt();
            return;
        }

        // Run
        _resetViewsThread = new Thread(() -> runJavaCode());
        _resetViewsThread.start();

        // Check back in half a second to see if we need to show progress bar
        ViewUtils.runDelayed(() -> handleExtendedRun(), 500, true);

        // Reset EvalPane
        _evalPane.resetLater();
    }

    /**
     * Runs Java Code.
     */
    protected void runJavaCode()
    {
        // Clear value/views cache
        _replViewsCache.clear();

        // Get code and run
        JeplTextDoc jeplDoc = getJeplDoc();
        _javaShell.runJavaCode(jeplDoc);

        // Remove ExtendedRunView
        ViewUtils.runLater(() -> removeExtendedRunView());

        // If Preempted, kick off another run
        if (_preemptedForNewRun) {
            _preemptedForNewRun = false;
            ViewUtils.runLater(() -> resetEvalValues());
        }

        // If otherwise interrupted, add last run cancelled UI
        else if (_javaShell.isInterrupted())
            ViewUtils.runLater(() -> addLastRunCancelledUI());

        // Reset thread
        _resetViewsThread = null;

        // Reset EvalPane
        _evalPane.resetLater();
    }

    /**
     * Called by shell when there is output.
     */
    public void processOutput(JStmt aStmt, Object aValue)
    {
        // synchronized (_outputList) {
        //     if (_outputList.size() + getChildCount() > MAX_OUTPUT_COUNT)
        //         cancelRun();
        //     _outputList.add(aValue);
        //     if (_outputList.size() == 1)
        //         ViewUtils.runLater(() -> processOutputInEventThread());
        // }

        // Add output
        ViewUtils.runLater(() -> processOutputInEventThread(aValue));

        // Yield to show output
        Thread.yield();
    }

    /**
     * Called by shell when there is output.
     */
    private void processOutputInEventThread()
    {
        // synchronized (_outputList) {
        //     for (Object out : _outputList)
        //         processOutputInEventThread(out);
        //     _outputList.clear();
        // }
    }

    /**
     * Called by shell when there is output.
     */
    private void processOutputInEventThread(Object anObj)
    {
        // If too much output, bail
        if (getChildCount() > MAX_OUTPUT_COUNT) {
            if (_resetViewsThread == null)
                return;
            cancelRun();
            return;
        }

        // Get view for output object and add
        View replView = getViewForReplValue(anObj);
        if (!replView.isShowing())
            addChild(replView);
    }

    /**
     * Returns whether evaluation is running.
     */
    public boolean isRunning()  { return _resetViewsThread != null; }

    /**
     * Creates a view for given Repl value.
     */
    protected View getViewForReplValue(Object aValue)
    {
        // Handle simple value: just create/return new view
        if (isSimpleValue(aValue))
            return EvalViewUtils.createBoxViewForValue(aValue);

        // Handle other values: Get cached view and create if not yet cached
        View view = _replViewsCache.get(aValue);
        if (view == null) {
            view = EvalViewUtils.createBoxViewForValue(aValue);
            _replViewsCache.put(aValue, view);
        }

        // Return
        return view;
    }

    /**
     * Returns whether given value is simple (String, Number, Boolean, Character, Date).
     */
    protected boolean isSimpleValue(Object anObj)
    {
        return anObj instanceof Boolean ||
                anObj instanceof Number ||
                anObj instanceof String ||
                anObj instanceof Date;
    }

    /**
     * Called when a run is taking a long time.
     */
    protected void handleExtendedRun()
    {
        if (_resetViewsThread == null)
            return;

        // Get/add ExtendedRunView
        View extendedRunView = getExtendedRunView();
        addChild(extendedRunView, 0);
    }

    /**
     * Called when a run is cancelled.
     */
    protected void cancelRun()
    {
        // If already cancelled, just return
        if (_resetViewsThread == null) return;

        // Interrupt and clear
        _javaShell.interrupt();
        _resetViewsThread = null;
    }

    /**
     * Adds a last run cancelled UI.
     */
    private void addLastRunCancelledUI()
    {
        Label label = new Label("Last run cancelled");
        label.setLeanX(HPos.CENTER);
        addChild(label, 0);

        if (getChildCount() > MAX_OUTPUT_COUNT)
            label.setText(label.getText() + " - Too much output");
    }

    /**
     * Returns the view to show when there is an extended run.
     */
    private View getExtendedRunView()
    {
        // If already set, just return
        if (_extendedRunView != null) return _extendedRunView;

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

        // Create ColView to hold
        ColView colView = new ColView();
        colView.setSpacing(8);
        colView.setGrowWidth(true);
        colView.setChildren(label, progressBar, button);

        // Set, return
        return _extendedRunView = colView;
    }

    /**
     * Removes the extended run view.
     */
    private void removeExtendedRunView()
    {
        if (_extendedRunView != null && _extendedRunView.isShowing())
            removeChild(_extendedRunView);
    }
}

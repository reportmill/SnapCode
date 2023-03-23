/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
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
public class EvalView extends ColView implements JavaShell.ShellClient {

    // The EvalTool
    private EvalTool  _evalTool;

    // The JavaShell
    protected JavaShell  _javaShell;

    // A cache of views for output values
    private Map<Object,View>  _replViewsCache = new HashMap<>();

    // The view that shows when there is an extended run
    private View  _extendedRunView;

    // Constants
    private static final int MAX_OUTPUT_COUNT = 1000;

    /**
     * Constructor.
     */
    public EvalView(EvalTool evalTool)
    {
        _evalTool = evalTool;
        setSpacing(6);
        setFill(new Color(.99));

        // Set Padding to match TextArea
        setPadding(5, 5, 5, 5);

        // Create JavaShell
        _javaShell = new JavaShell();
        _javaShell.setClient(this);
    }

    /**
     * Resets the display.
     */
    public void resetDisplay()
    {
        removeChildren();
        _replViewsCache.clear();
    }

    /**
     * Called by shell when there is output.
     */
    public void processOutput(Object aValue)
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
            boolean isRunning = _evalTool.isRunning();
            if (!isRunning)
                return;
            _evalTool.cancelRun();
            return;
        }

        // Get view for output object and add
        View replView = getViewForReplValue(anObj);
        if (!replView.isShowing())
            addChild(replView);
    }

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
     * Adds a last run cancelled UI.
     */
    public void addLastRunCancelledUI()
    {
        Label label = new Label("Last run cancelled");
        label.setLeanX(HPos.CENTER);
        addChild(label, 0);

        // Handle AutoRun timeout
        if (_evalTool._autoRunRequested)
            label.setText(label.getText() + " - exceeded AutoRun timeout");

        if (getChildCount() > MAX_OUTPUT_COUNT)
            label.setText(label.getText() + " - Too much output");
    }

    /**
     * Triggers an extended run.
     */
    public void triggerExtendedRun()
    {
        // Get/add ExtendedRunView
        View extendedRunView = getExtendedRunView();
        addChild(extendedRunView, 0);
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
        button.addEventHandler(e -> _evalTool.cancelRun(), Button.Action);

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
    public void removeExtendedRunView()
    {
        if (_extendedRunView != null && _extendedRunView.isShowing())
            removeChild(_extendedRunView);
    }
}

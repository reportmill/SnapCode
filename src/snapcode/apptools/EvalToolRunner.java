/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.ide.JavaTextPane;
import javakit.parse.JeplTextDoc;
import javakit.project.BuildIssue;
import javakit.project.JeplAgent;
import javakit.runner.JavaShell;
import snap.view.*;
import snap.viewx.WebPage;
import snapcode.app.JavaPage;
import snapcode.app.PagePane;
import snapcode.app.WorkspacePane;

/**
 * A TextArea subclass to show code evaluation.
 */
public class EvalToolRunner implements JavaShell.ShellClient {

    // The EvalTool
    private EvalTool _evalTool;

    // The EvalView
    private EvalView _evalView;

    // The JavaShell
    protected JavaShell _javaShell;

    // The thread to reset views
    private Thread _runAppThread;

    // Whether last run was pre-empted
    private boolean _preemptedForNewRun;

    /**
     * Constructor.
     */
    public EvalToolRunner(EvalTool evalTool)
    {
        _evalTool = evalTool;

        // Create JavaShell
        _javaShell = new JavaShell();
        _javaShell.setClient(this);
    }

    public JavaTextPane<?> getJavaTextPane()
    {
        WorkspacePane workspacePane = _evalTool.getWorkspacePane();
        PagePane pagePane = workspacePane.getPagePane();
        WebPage selPage = pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        return javaPage != null ? javaPage.getTextPane() : null;
    }

    public JeplTextDoc getJeplDoc()
    {
        JavaTextPane<?> javaTextPane = getJavaTextPane();
        return (JeplTextDoc) javaTextPane.getTextDoc();
    }

    /**
     * Called to update when textView changes.
     */
    public void runApp()
    {
        // Reset display
        _evalView = _evalTool._evalView;
        _evalView.resetDisplay();

        // If previous thread is running, kill it
        if (_runAppThread != null) {
            _preemptedForNewRun = true;
            _javaShell.interrupt();
            return;
        }

        // Run
        _runAppThread = new Thread(() -> runAppImpl());
        _runAppThread.start();

        // Check back in half a second to see if we need to show progress bar
        ViewUtils.runDelayed(() -> handleExtendedRun(), 500, true);

        // Reset EvalPane
        _evalTool.resetLater();
    }

    /**
     * Runs Java Code.
     */
    protected void runAppImpl()
    {
        // Get JeplTextDoc, JeplAgent
        JeplTextDoc jeplDoc = getJeplDoc();
        JeplAgent jeplAgent = jeplDoc.getAgent();

        // Build file
        boolean success = jeplAgent.buildFile();

        // Notify EditPane of possible BuildIssue changes
        JavaTextPane<?> javaTextPane = getJavaTextPane();
        javaTextPane.buildIssueOrBreakPointMarkerChanged();

        // If build failed, report errors
        if (!success) {
            BuildIssue[] buildIssues = jeplAgent.getBuildIssues();
            processOutput(buildIssues);
        }

        // If no errors, run
        else _javaShell.runJavaCode(jeplDoc);

        // Remove ExtendedRunView
        ViewUtils.runLater(() -> _evalView.removeExtendedRunView());

        // If Preempted, kick off another run
        if (_preemptedForNewRun) {
            _preemptedForNewRun = false;
            ViewUtils.runLater(() -> runApp());
        }

        // If otherwise interrupted, add last run cancelled UI
        else if (_javaShell.isInterrupted())
            ViewUtils.runLater(() -> _evalView.addLastRunCancelledUI());

        // Reset thread
        _runAppThread = null;

        // Reset EvalPane
        _evalTool.resetLater();
    }

    /**
     * Called by shell when there is output.
     */
    public void processOutput(Object aValue)
    {
        _evalView.processOutput(aValue);
    }

    /**
     * Returns whether evaluation is running.
     */
    public boolean isRunning()  { return _runAppThread != null; }

    /**
     * Called when a run is taking a long time.
     */
    protected void handleExtendedRun()
    {
        if (_runAppThread == null)
            return;

        // If AutoRunRequested, just cancel run
        if (_evalTool._autoRunRequested) {
            cancelRun();
            return;
        }

        // Trigger extended run
        _evalView.triggerExtendedRun();
    }

    /**
     * Called when a run is cancelled.
     */
    public void cancelRun()
    {
        // If already cancelled, just return
        if (_runAppThread == null) return;

        // Interrupt and clear
        _javaShell.interrupt();
        _runAppThread = null;
    }
}

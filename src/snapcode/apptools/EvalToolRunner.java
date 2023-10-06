/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JStmt;
import snapcharts.repl.Console;
import snapcharts.repl.ScanPane;
import snapcode.javatext.JavaTextPane;
import snapcode.project.JavaTextDoc;
import snapcode.project.BuildIssue;
import snapcode.project.JavaAgent;
import javakit.runner.JavaShell;
import snap.view.*;
import snapcode.webbrowser.WebPage;
import snapcode.app.JavaPage;
import snapcode.app.PagePane;
import snapcode.app.WorkspacePane;
import java.io.InputStream;

/**
 * A TextArea subclass to show code evaluation.
 */
public class EvalToolRunner {

    // The EvalTool
    private EvalTool _evalTool;

    // The JavaShell
    protected JavaShell _javaShell;

    // The thread to reset views
    private Thread _runAppThread;

    // Whether last run was pre-empted
    private boolean _preemptedForNewRun;

    // An input stream for standard in
    protected ScanPane.BytesInputStream _inputStream;

    /**
     * Constructor.
     */
    public EvalToolRunner(EvalTool evalTool)
    {
        _evalTool = evalTool;

        // Create JavaShell
        _javaShell = new JavaShell();
    }

    /**
     * Returns the current JavaTextPane.
     */
    public JavaTextPane getJavaTextPane()
    {
        WorkspacePane workspacePane = _evalTool.getWorkspacePane();
        PagePane pagePane = workspacePane.getPagePane();
        WebPage selPage = pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        return javaPage != null ? javaPage.getTextPane() : null;
    }

    /**
     * Called to update when textView changes.
     */
    public void runApp()
    {
        // Reset display
        _evalTool.resetDisplay();

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
        ViewUtils.runDelayed(() -> handleExtendedRun(), 500);

        // Reset EvalPane
        _evalTool.resetLater();
    }

    /**
     * Runs Java Code.
     */
    protected void runAppImpl()
    {
        // Get JavaTextDoc, JavaAgent
        JavaTextPane javaTextPane = getJavaTextPane();
        JavaTextDoc javaTextDoc = javaTextPane.getJavaTextDoc();
        JavaAgent javaAgent = javaTextDoc.getAgent();

        // Build file
        boolean success = javaAgent.buildFile();

        // Notify EditPane of possible BuildIssue changes
        javaTextPane.buildIssueOrBreakPointMarkerChanged();

        // If build failed, report errors
        if (!success) {
            BuildIssue[] buildIssues = javaAgent.getBuildIssues();
            _evalTool._console.show(buildIssues);
        }

        // If no errors, run
        else {
            Console.setShared(_evalTool._console);
            JFile jfile = javaAgent.getJFile();
            JStmt[] javaStmts = javaAgent.getJFileStatements();

            // Replace System.in with our own input stream to allow input
            InputStream stdIn = System.in;
            System.setIn(_inputStream = new ScanPane.BytesInputStream(null));

            // Run code
            _javaShell.runJavaCode(jfile, javaStmts);

            // Restore System.in
            System.setIn(stdIn);
        }

        // Remove extended run ui
        ViewUtils.runLater(() -> _evalTool.setShowExtendedRunUI(false));

        // If Preempted, kick off another run
        if (_preemptedForNewRun) {
            _preemptedForNewRun = false;
            ViewUtils.runLater(() -> runApp());
        }

        // If otherwise interrupted, add last run cancelled UI
        else if (_javaShell.isInterrupted())
            ViewUtils.runLater(() -> _evalTool.setShowCancelledRunUI(true));

        // Reset thread
        _runAppThread = null;

        // Reset EvalPane
        _evalTool.resetLater();
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

        // Show extended run UI
        _evalTool.setShowExtendedRunUI(true);
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

    /**
     * Adds an input string.
     */
    public void addSystemInputString(String aString)
    {
        _inputStream.add(aString);
    }
}

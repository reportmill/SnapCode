/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JStmt;
import snap.web.WebFile;
import snapcharts.repl.Console;
import snapcharts.repl.ScanPane;
import snapcode.project.JavaAgent;
import javakit.runner.JavaShell;
import snap.view.*;
import java.io.InputStream;

/**
 * A TextArea subclass to show code evaluation.
 */
public class EvalToolRunner {

    // The RunTool
    private RunTool _runTool;

    // The JavaShell
    protected JavaShell _javaShell;

    // The thread to reset views
    private Thread _runAppThread;

    // An input stream for standard in
    protected ScanPane.BytesInputStream _inputStream;

    /**
     * Constructor.
     */
    public EvalToolRunner(RunTool runTool)
    {
        _runTool = runTool;

        // Create JavaShell
        _javaShell = new JavaShell();
    }

    /**
     * Called to update when textView changes.
     */
    public void runApp()
    {
        // If previous thread is running, kill it
        if (_runAppThread != null)
            cancelRun();

        // Run
        _runAppThread = new Thread(() -> runAppImpl());
        _runAppThread.start();

        // Check back in half a second to see if we need to show progress bar
        ViewUtils.runDelayed(() -> handleExtendedRun(), 500);
    }

    /**
     * Runs Java Code.
     */
    protected void runAppImpl()
    {
        // Get JavaAgent
        WebFile selFile = _runTool.getSelFile();
        JavaAgent javaAgent = selFile != null ? JavaAgent.getAgentForFile(selFile) : null;
        if (javaAgent == null)
            return;

        // Get statements
        Console.setShared(_runTool._console);
        JFile jfile = javaAgent.getJFile();
        JStmt[] javaStmts = javaAgent.getJFileStatements();

        // Replace System.in with our own input stream to allow input
        InputStream stdIn = System.in;
        System.setIn(_inputStream = new ScanPane.BytesInputStream(null));

        // Run code
        _javaShell.runJavaCode(jfile, javaStmts);

        // Restore System.in
        System.setIn(stdIn);

        // Remove extended run ui
        ViewUtils.runLater(() -> _runTool.setShowExtendedRunUI(false));

        // If thread was interrupted, add last run cancelled UI
        if (_javaShell.isInterrupted())
            ViewUtils.runLater(() -> _runTool.setShowCancelledRunUI(true));

        // Reset thread
        _runAppThread = null;

        // Reset EvalPane
        _runTool.resetLater();
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

        // Show extended run UI
        _runTool.setShowExtendedRunUI(true);
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
        ViewUtils.runDelayed(() -> cancelRunExtreme(_runAppThread), 600);
    }

    /**
     * Called to really cancel run with thread interrupt, if in system code.
     */
    private void cancelRunExtreme(Thread runAppThread)
    {
        if (runAppThread != null)
            runAppThread.interrupt();
    }

    /**
     * Adds an input string.
     */
    public void addSystemInputString(String aString)
    {
        _inputStream.add(aString);
    }
}

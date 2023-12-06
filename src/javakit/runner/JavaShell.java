/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import snap.util.StringUtils;
import snapcharts.repl.ReplObject;
import snapcode.debug.RunApp;
import snapcode.util.ExceptionUtil;
import java.io.PrintStream;

/**
 * A class to evaluate JavaShell code.
 */
public class JavaShell {

    // A Statement evaluator
    private JSStmtEval _stmtEval;

    // An object to act as "this"
    private Object  _thisObject = new Object();

    // Whether error was hit
    private boolean  _errorWasHit;

    // The RunApp
    protected RunApp _runApp;

    // The public out and err PrintStreams
    private PrintStream  _stdOut = System.out;
    private PrintStream  _stdErr = System.err;

    // Proxy standard out/err to capture console
    private PrintStream  _shellOut = new JavaShellUtils.ProxyPrintStream(this, _stdOut);
    private PrintStream  _shellErr = new JavaShellUtils.ProxyPrintStream(this,_stdErr);

    /**
     * Constructor.
     */
    public JavaShell(RunApp runApp)
    {
        _runApp = runApp;

        // Create Statement eval
        _stmtEval = new JSStmtEval();
    }

    /**
     * Evaluate string.
     */
    public void runJavaCode(JFile jfile, JStmt[] javaStmts)
    {
        // Reset VarStack
        _stmtEval._exprEval._varStack.reset();

        // Set var stack indexes in AST
        Simpiler.setVarStackIndexForJFile(jfile);

        // Get parsed statements
        if (javaStmts == null) {
            System.err.println("JavaShell.runJavaCode: No main method");
            return;
        }

        // Set System out/err to catch console output
        System.setOut(_shellOut);
        System.setErr(_shellErr);

        // Clear StopRun
        _stmtEval._stopRun = _errorWasHit = false;

        // Iterate over lines and eval each
        for (JStmt stmt : javaStmts) {

            // Get Statement (if null, just set empty string value and continue)
            if (stmt == null)
                continue;

            // Evaluate statement
            evalStatement(stmt);

            // If StopRun hit, break
            if (_stmtEval._stopRun || _errorWasHit)
                break;
        }

        // Restore System out/err
        System.setOut(_stdOut);
        System.setErr(_stdErr);
    }

    /**
     * Called to stop current run.
     */
    public void interrupt()
    {
        _stmtEval._stopRun = true;
    }

    /**
     * Appends console output string with option for whether is error.
     */
    protected void appendConsoleOutput(String aString, boolean isError)
    {
        _runApp.appendConsoleOutput(aString, isError);
    }

    /**
     * Evaluate JStmt.
     */
    protected void evalStatement(JStmt aStmt)
    {
        // Handle statement with errors
        if (aStmt.getErrors() != NodeError.NO_ERRORS) {
            _errorWasHit = true;
            return;
        }

        // Eval statement
        try { _stmtEval.evalExecutable(_thisObject, aStmt); }

        // Handle statement eval exception: Try expression
        catch (Exception e) {

            // Ignore InterruptedExceptions - assume this is from controlling thread
            if (isInterruptedException(e))
                return;

            // Mark ErrorWasHit
            _errorWasHit = true;

            // Show exception
            String str = StringUtils.getStackTraceString(e);
            appendConsoleOutput(str, true);

            // Show exception
            Object exceptionText = ExceptionUtil.getTextBlockForException(e);
            ReplObject.show(exceptionText);
        }
    }

    /**
     * Returns whether given exception is InterruptedException.
     */
    private static boolean isInterruptedException(Exception anException)
    {
        for (Throwable e = anException; e != null; e = e.getCause())
            if (e instanceof InterruptedException)
                return true;
        return false;
    }
}
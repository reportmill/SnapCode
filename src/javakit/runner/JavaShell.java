/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import snap.util.CharSequenceUtils;
import snapcharts.repl.ReplObject;
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

    // Current console out object
    private ConsoleOutput  _consoleOut;

    // Current console err object
    private ConsoleOutput  _consoleErr;

    // The public out and err PrintStreams
    private PrintStream  _stdOut = System.out;
    private PrintStream  _stdErr = System.err;

    // Proxy standard out/err to capture console
    private PrintStream  _shellOut = new JavaShellUtils.ProxyPrintStream(this, _stdOut);
    private PrintStream  _shellErr = new JavaShellUtils.ProxyPrintStream(this,_stdErr);

    // Constants
    public static final String STANDARD_OUT = "Standard Out";
    public static final String STANDARD_ERR = "Standard Err";

    /**
     * Creates a new PGEvaluator.
     */
    public JavaShell()
    {
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
     * Returns whether shell was interrupted.
     */
    public boolean isInterrupted()
    {
        return _stmtEval._stopRun;
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
            e.printStackTrace(_stdErr);
            _errorWasHit = true;
        }
    }

    /**
     * Appends output.
     */
    protected void appendOut(String aString)
    {
        // Get/update ConsoleOut with string
        if (_consoleOut == null)
            _consoleOut = new ConsoleOutput(STANDARD_OUT, aString);
        else _consoleOut._string += aString;

        // If newline, process and clear
        if (CharSequenceUtils.isLastCharNewline(_consoleOut.getString())) {
            ReplObject.show(_consoleOut);
            _consoleOut = null;
        }
    }

    /**
     * Appends error.
     */
    protected void appendErr(String aString)
    {
        // Get/update ConsoleOut with string
        if (_consoleErr == null)
            _consoleErr = new ConsoleOutput(STANDARD_ERR, aString);
        else _consoleErr._string += aString;

        // If newline, process and clear
        if (CharSequenceUtils.isLastCharNewline(_consoleErr.getString())) {
            ReplObject.show(_consoleErr);
            _consoleErr = null;
        }
    }

    /**
     * A class to hold standard out/err output strings.
     */
    public static class ConsoleOutput {

        // Ivars
        private String  _name;
        protected String  _string;

        /**
         * Constructor.
         */
        public ConsoleOutput(String aName, String aString)
        {
            _name = aName;
            _string = aString;
        }

        /**
         * Returns the name.
         */
        public String getName()  { return _name; }

        /**
         * Returns the string.
         */
        public String getString()  { return _string; }

        /**
         * Returns whether is error.
         */
        public boolean isError()  { return _name == STANDARD_ERR; }

        /**
         * Standard toString.
         */
        public String toString()  { return _string.trim(); }
    }
}
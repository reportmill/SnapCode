/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import javakit.project.JavaAgent;
import snap.util.CharSequenceUtils;
import java.io.PrintStream;

/**
 * A class to evaluate JavaShell code.
 */
public class JavaShell {

    // A Statement evaluator
    private JSStmtEval _stmtEval;

    // An object to act as "this"
    private Object  _thisObject = new Object();

    // The client
    private static ShellClient  _client;

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
     * Returns the client.
     */
    public static ShellClient getClient()  { return _client; }

    /**
     * Sets the client.
     */
    public void setClient(ShellClient aClient)
    {
        _client = aClient;
    }

    /**
     * Evaluate string.
     */
    public void runJavaCode(JavaTextDoc javaTextDoc)
    {
        // Reset VarStack
        _stmtEval._exprEval._varStack.reset();

        // Set var stack indexes in AST
        JavaAgent javaAgent = javaTextDoc.getAgent();
        JFile jfile = javaAgent.getJFile();
        Simpiler.setVarStackIndexForJFile(jfile);

        // Get parsed statements
        JStmt[] javaStmts = javaAgent.getJFileStatements();
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
            Object lineVal = evalStatement(stmt);

            // Process output
            //if (_client != null && lineVal != null)
            //    _client.processOutput(lineVal);

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
    protected Object evalStatement(JStmt aStmt)
    {
        // Handle statement with errors
        if (aStmt.getErrors() != NodeError.NO_ERRORS) {
            _errorWasHit = true;
            return aStmt.getErrors();
        }

        // Eval statement
        Object val;
        try {
            val = _stmtEval.evalExecutable(_thisObject, aStmt);
        }

        // Handle statement eval exception: Try expression
        catch (Exception e) {
            e.printStackTrace(_stdErr);
            val = e;
            _errorWasHit = true;
        }

        // Return
        return val;
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
            _client.processOutput(_consoleOut);
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
            _client.processOutput(_consoleErr);
            _consoleErr = null;
        }
    }

    /**
     * An interface to process shell output.
     */
    public interface ShellClient {

        /**
         * Called when a statement is evaluated with console and result output.
         */
        void processOutput(Object anOutput);
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
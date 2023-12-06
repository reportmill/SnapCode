/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextBlock;
import snap.text.TextStyle;
import snap.util.CharSequenceUtils;
import snap.util.StringUtils;
import snapcharts.repl.ReplObject;
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
        flushConsole();
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

            // Ignore InterruptedExceptions - assume this is from controlling thread
            if (isInterruptedException(e))
                return;

            // Show exception and mark ErrorWasHit
            Object exceptionText = ExceptionUtil.getTextBlockForException(e);
            ReplObject.show(exceptionText);
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
            ReplObject.show(_consoleOut.getTextBlock());
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
            ReplObject.show(_consoleErr.getTextBlock());
            _consoleErr = null;
        }
    }

    /**
     * Flushes output.
     */
    protected void flushConsole()
    {
        if (_consoleOut != null)
            ReplObject.show(_consoleOut.getTextBlock());
        _consoleOut = null;
        if (_consoleErr != null)
            ReplObject.show(_consoleErr.getTextBlock());
        _consoleErr = null;
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

    /**
     * A class to hold standard out/err output strings.
     */
    public static class ConsoleOutput {

        // Ivars
        private String  _name;
        protected String  _string;

        private static Color ERROR_COLOR = Color.get("#CC0000");

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
         * Returns a text block.
         */
        public TextBlock getTextBlock()
        {
            // Create TextBlock and configure Style
            TextBlock textBlock = new TextBlock();
            TextStyle textStyle = textBlock.getDefaultStyle();
            TextStyle textStyle2 = textStyle.copyFor(Font.Arial14);
            if (_name.equals(STANDARD_ERR))
                textStyle2 = textStyle2.copyFor(ERROR_COLOR);
            textBlock.setDefaultStyle(textStyle2);

            // Set string
            String str = StringUtils.trimEnd(_string);
            textBlock.setString(str);

            // Return
            return textBlock;
        }

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
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import javakit.resolver.Resolver;
import snap.util.Convert;
import snap.util.StringUtils;
import snap.web.WebFile;
import snapcharts.repl.CallHandler;
import snapcharts.repl.Console;
import snapcharts.repl.ReplObject;
import snapcode.debug.RunAppSrc;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import snapcode.util.ExceptionUtil;

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
    protected RunAppSrc _runApp;

    /**
     * Constructor.
     */
    public JavaShell(RunAppSrc runApp)
    {
        _runApp = runApp;

        // Create Statement eval
        _stmtEval = new JSStmtEval();
    }

    /**
     * Run code in JavaFile for given JavaAgent.
     */
    public void runJavaCode(JavaAgent javaAgent)
    {
        // Set var stack indexes in AST
        JFile jfile = javaAgent.getJFile();
        Simpiler.setVarStackIndexForJFile(jfile);

        // Run as main method (RunApp.SrcHybrid) or main statements (legacy)
        if (_runApp.isSrcHybrid())
            runMainMethod(javaAgent);
        else runMainStatements(jfile);
    }

    /**
     * Runs the main method for given JavaAgent.
     */
    public void runMainMethod(JavaAgent javaAgent)
    {
        // Create/set CallHandler
        new CallHandler() {
            @Override
            public Object call(String className, String methodName, Object thisObject, Object[] args)
            {
                return JavaShell.this.call(className, methodName, thisObject, args);
            }
        };

        // Get main method
        Project project = javaAgent.getProject();
        Resolver resolver = project.getResolver();
        String mainClassName = _runApp.getMainClassName();
        JavaClass mainClass = resolver.getJavaClassForName(mainClassName);
        JavaClass stringArrayClass = resolver.getJavaClassForClass(String[].class);
        JavaMethod mainMethod = mainClass.getMethodForNameAndTypes("main", new JavaClass[] { stringArrayClass });

        // Invoke main method
        try { mainMethod.invoke(null, (Object) new String[0]); }

        // Catch exceptions and show in console
        catch (Exception e) {
            _errorWasHit = true;
            String str = StringUtils.getStackTraceString(e);
            appendConsoleOutput(str, true);
        }
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
     * Runs main statements in given JFile.
     */
    private void runMainStatements(JFile jfile)
    {
        JStmt[] mainStmts = JavaShellUtils.getMainStatements(jfile);
        runStatements(mainStmts);
    }

    /**
     * Runs given statements in given JFile.
     */
    private void runStatements(JStmt[] stmtsArray)
    {
        // Iterate over main statements and eval each
        for (JStmt stmt : stmtsArray) {

            // Evaluate statement
            evalStatement(stmt);

            // If StopRun hit, break
            if (_stmtEval._stopRun || _errorWasHit)
                break;
        }
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
            if (Console.getShared() != null) {
                Object exceptionText = ExceptionUtil.getTextBlockForException(e);
                ReplObject.show(exceptionText);
            }
        }
    }

    /**
     * This method is a dispatcher for Java source classes.
     */
    public Object call(String className, String methodName, Object thisObject, Object[] args)
    {
        // If hit error, just return
        if (_errorWasHit) return null;

        // Get source file for class name
        WebFile sourceFile = _runApp.getSourceFileForClassName(className);

        // Get ClassDecl for full class name
        JavaAgent javaAgent = JavaAgent.getAgentForFile(sourceFile);
        JFile jFile = javaAgent.getJFile();
        JClassDecl classDecl = jFile.getClassDecl();

        // Handle constructor
        if (methodName.equals("__init")) {
            JConstrDecl constrDecl = classDecl.getConstructorDeclForTypes( null);
            return null;
        }

        // Handle initializer
        if (methodName.startsWith("__initializer")) {
            int initializerIndex = Convert.intValue(methodName);
            JInitializerDecl[] initializerDecls = classDecl.getInitDecls();
            JInitializerDecl initializerDecl = initializerDecls[initializerIndex];
            JStmt[] stmts = initializerDecl.getBlockStatements();
            runStatements(stmts);
            return null;
        }

        // Get MethodDecl for method name and invoke
        JMethodDecl methodDecl = classDecl.getMethodDeclForNameAndTypes(methodName, null);
        try {
            return _stmtEval._exprEval.evalMethodCallExprForMethodDecl(thisObject, methodDecl, args);
        }

        // Handle exceptions: Add to console
        catch (Exception e) {
            _errorWasHit = true;
            String str = StringUtils.getStackTraceString(e);
            appendConsoleOutput(str, true);
            return null;
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
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import snap.util.Convert;
import snap.util.StringUtils;
import snap.web.WebFile;
import snapcharts.repl.CallHandler;
import snapcharts.repl.Console;
import snapcharts.repl.ReplObject;
import snapcode.debug.RunAppSrc;
import snapcode.project.JavaAgent;
import snapcode.util.ExceptionUtil;
import java.util.List;

/**
 * A class to evaluate JavaShell code.
 */
public class JavaShell {

    // An expression evaluator
    protected JSExprEval _exprEval;

    // A Statement evaluator
    private JSStmtEval _stmtEval;

    // A map of local variables
    protected JSVarStack _varStack;

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
        _varStack = new JSVarStack();
        _exprEval = new JSExprEval(this);
        _stmtEval = new JSStmtEval(this);
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

        // Otherwise just run main statements
        else {
            JStmt[] mainStmts = JavaShellUtils.getMainStatements(jfile);
            Class<?> mainClass = JavaShellUtils.getMainClass(this, javaAgent);
            evalStatements(mainStmts, mainClass);
        }
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
        JavaMethod mainMethod = JavaShellUtils.getMainJavaMethod(this, javaAgent);

        // Invoke main method
        try { mainMethod.invoke(null, (Object) new String[0]); }

        // Catch exceptions and show in console
        catch (Exception e) {
            _errorWasHit = true;
            String str = StringUtils.getStackTraceString(e);
            _runApp.appendConsoleOutput(str, true);
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
     * Runs given statements array.
     */
    public Object evalStatements(JStmt[] stmtsArray, Object thisObject)
    {
        // Eval statement
        try { return _stmtEval.evalStatements(stmtsArray, thisObject); }

        // Handle statement eval exception: Try expression
        catch (Exception e) {

            // Ignore InterruptedExceptions - assume this is from controlling thread
            if (JavaShellUtils.isInterruptedException(e))
                return null;

            // Mark ErrorWasHit
            _errorWasHit = true;

            // Show exception
            String str = StringUtils.getStackTraceString(e);
            _runApp.appendConsoleOutput(str, true);

            // Show exception
            if (Console.getShared() != null) {
                Object exceptionText = ExceptionUtil.getTextBlockForException(e);
                ReplObject.show(exceptionText);
            }

            // Return null
            return null;
        }
    }

    /**
     * Calls given method decl with target object and args.
     */
    public Object callMethodDecl(JMethodDecl aMethodDecl, Object thisObject, Object[] argValues)
    {
        // Create stack frame
        _varStack.pushStackFrame();

        // Install params
        List<JVarDecl> params = aMethodDecl.getParameters();
        for (int i = 0, iMax = params.size(); i < iMax; i++) {
            JVarDecl varDecl = params.get(i);
            JExprId varId = varDecl.getId();
            setExprIdValue(varId, argValues[i]);
        }

        // Get method body and run
        JStmt[] methodBody = aMethodDecl.getBlockStatements();
        Object returnVal = evalStatements(methodBody, thisObject);

        // Pop stack frame
        _varStack.popStackFrame();

        // Return
        return returnVal;
    }

    /**
     * Sets an assignment value for given identifier expression and value.
     */
    protected Object setExprIdValue(JExprId idExpr, Object aValue)
    {
        // Convert type
        JavaClass assignClass = idExpr.getEvalClass();
        Class<?> realClass = assignClass.getRealClass();
        Object assignValue = JSExprEvalUtils.castOrConvertValueToPrimitiveClass(aValue, realClass);

        if (!_varStack.setStackValueForNode(idExpr, assignValue))
            System.err.println("JSExprEval: Unknown id: " + idExpr);
        return assignValue;
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
            evalStatements(stmts, thisObject);
            return null;
        }

        // Get MethodDecl for method name and invoke
        JMethodDecl methodDecl = classDecl.getMethodDeclForNameAndTypes(methodName, null);
        try {
            return callMethodDecl(methodDecl, thisObject, args);
        }

        // Handle exceptions: Add to console
        catch (Exception e) {
            _errorWasHit = true;
            String str = StringUtils.getStackTraceString(e);
            _runApp.appendConsoleOutput(str, true);
            return null;
        }
    }
}
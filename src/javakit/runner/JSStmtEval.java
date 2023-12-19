/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import java.lang.reflect.Array;
import java.util.*;
import javakit.parse.*;
import snap.util.Convert;

/**
 * A class to evaluate Java statements.
 */
public class JSStmtEval {

    // The JavaShell
    private JavaShell _javaShell;

    // Whether we hit a break statement
    private boolean  _breakWasHit;

    // Whether we hit a continue statement
    private boolean  _continueWasHit;

    // Holds a return value if return was hit
    protected Object  _returnValueHit;

    // Whether to stop current run
    protected boolean  _stopRun;

    // Constant representing a returned null value
    private Object NULL_RETURN_VALUE = new Object();

    /**
     * Constructor.
     */
    public JSStmtEval(JavaShell javaShell)
    {
        super();
        _javaShell = javaShell;
    }

    /**
     * Executes an array of statements.
     */
    public Object evalStatements(JStmt[] stmtsArray, Object thisObject) throws Exception
    {
        // Eval statement and return
        try {
            _returnValueHit = null;
            Object returnVal = evalBlockStatements(Arrays.asList(stmtsArray), thisObject);
            if (returnVal == NULL_RETURN_VALUE)
                returnVal = null;
            return returnVal;
        }

        // Reset ReturnValueHit
        finally {
            _returnValueHit = null;
        }
    }

    /**
     * Evaluate JStmt.
     */
    private Object evalStmt(JStmt aStmt, Object thisObject) throws Exception
    {
        // Handle Assert statement
        if (aStmt instanceof JStmtAssert) {
            System.out.println("JSStmtEval: Assert Statement not implemented");
        }

        // Handle block statement
        if (aStmt instanceof JStmtBlock)
            return evalBlockStmt((JStmtBlock) aStmt, thisObject);

        // Handle break statement
        if (aStmt instanceof JStmtBreak) {
            _breakWasHit = true;
            return null;
        }

        // Handle ClassDecl
        if (aStmt instanceof JStmtClassDecl)
            throw new RuntimeException("JSStmtEval: ClassDecl Statement not implemented");

        // Handle constructor call
        if (aStmt instanceof JStmtConstrCall)
            throw new RuntimeException("JSStmtEval: constructor Statement not implemented");

        // Handle continue statement
        if (aStmt instanceof JStmtContinue) {
            _continueWasHit = true;
            return null;
        }

        // Handle Do statement
        if (aStmt instanceof JStmtDo)
            return evalDoStmt((JStmtDo) aStmt, thisObject);

        // Empty statement
        if(aStmt instanceof JStmtEmpty)
            return null;

        // Expression statement
        if (aStmt instanceof JStmtExpr)
            return evalExprStmt((JStmtExpr) aStmt, thisObject);

        // For statement
        if (aStmt instanceof JStmtFor)
            return evalForStmt((JStmtFor) aStmt, thisObject);

        // Handle if statement
        if (aStmt instanceof JStmtIf)
            return evalIfStmt((JStmtIf) aStmt, thisObject);

        // Handle labeled statement
        if (aStmt instanceof JStmtLabeled) {
            JStmt stmt = ((JStmtLabeled) aStmt).getStatement();
            return evalStmt(stmt, thisObject);
        }

        // Handle return statement
        if (aStmt instanceof JStmtReturn)
            return evalReturnStmt((JStmtReturn) aStmt, thisObject);

        // Handle switch statement
        if (aStmt instanceof JStmtSwitch)
            return evalSwitchStmt((JStmtSwitch) aStmt, thisObject);

        // Handle sync statement
        if (aStmt instanceof JStmtSynchronized) {
            JStmtSynchronized syncStmt = (JStmtSynchronized) aStmt;
            return evalStmt(syncStmt.getBlock(), thisObject);
        }

        // Handle throw statement
        if (aStmt instanceof JStmtThrow)
            throw new RuntimeException("JSStmtEval: throw Statement not implemented");

        // Handle try statement
        if (aStmt instanceof JStmtTry)
            return evalTryStmt((JStmtTry) aStmt, thisObject);

        // Handle while statement
        if (aStmt instanceof JStmtWhile)
            return evalWhileStmt((JStmtWhile) aStmt, thisObject);

        // Complain
        throw new RuntimeException("EvalStmt.evalStmt: Unsupported statement " + aStmt.getClass());
    }

    /**
     * Evaluate JStmtBlock.
     */
    private Object evalBlockStmt(JStmtBlock aBlockStmt, Object thisObject) throws Exception
    {
        List<JStmt> statements = aBlockStmt.getStatements();
        return evalBlockStatements(statements, thisObject);
    }

    /**
     * Evaluate List of JStmts.
     */
    private Object evalBlockStatements(List<JStmt> statements, Object thisObject) throws Exception
    {
        // Get statements
        Object returnVal = null;

        // Iterate over statements and evaluate each
        for (JStmt stmt : statements) {
            Object rval = evalStmt(stmt, thisObject);
            if (stmt instanceof JStmtReturn)
                returnVal = rval;
            if (_breakWasHit || _continueWasHit || _stopRun || _returnValueHit != null)
                return _returnValueHit;
        }

        // Return
        return returnVal;
    }

    /**
     * Evaluate JStmtExpr.
     */
    private Object evalExprStmt(JStmtExpr aStmt, Object thisObject) throws Exception
    {
        JExpr expr = aStmt.getExpr();
        Object val = evalExpr(expr, thisObject);
        return val;
    }

    /**
     * Evaluate JStmtReturn.
     */
    private Object evalReturnStmt(JStmtReturn aReturnStmt, Object thisObject) throws Exception
    {
        // Get return value
        Object returnVal = NULL_RETURN_VALUE;

        // If return expression set, evaluate and set returnVal
        JExpr returnExpr = aReturnStmt.getExpr();
        if (returnExpr != null) {
            returnVal = evalExpr(returnExpr, thisObject);
            if (returnVal == null)
                returnVal = NULL_RETURN_VALUE;
        }

        // Set and return value
        return _returnValueHit = returnVal;
    }

    /**
     * Evaluate JStmtSwitch.
     */
    private Object evalSwitchStmt(JStmtSwitch aSwitchStmt, Object thisObject) throws Exception
    {
        // Get switch expression and eval
        JExpr switchExpr = aSwitchStmt.getExpr();
        Object switchValue = evalExpr(switchExpr, thisObject);

        // Iterate over cases
        List<JStmtSwitchCase> switchCases = aSwitchStmt.getSwitchCases();
        boolean hitTrueCase = false;

        // Iterate over switch cases
        for (JStmtSwitchCase switchCase : switchCases) {

            // Skip default
            if (switchCase.isDefault())
                continue;

            // If haven't found true case, eval case expr and check if this is it
            if (!hitTrueCase) {
                JExpr caseExpr = switchCase.getExpr();
                Object caseValue = evalExpr(caseExpr, thisObject);
                hitTrueCase = Objects.equals(switchValue, caseValue);
            }

            // If hit true case, execute statements and check for break/return
            if (hitTrueCase) {
                List<JStmt> statements = switchCase.getStatements();
                evalBlockStatements(statements, thisObject);
                if (handleBreakCheck())
                    return _returnValueHit;
            }
        }

        // Get default and run that
        JStmtSwitchCase defaultCase = aSwitchStmt.getDefaultCase();
        if (defaultCase != null) {
            List<JStmt> statements = defaultCase.getStatements();
            evalBlockStatements(statements, thisObject);
            if (handleBreakCheck())
                return _returnValueHit;
        }

        // Return null
        return null;
    }

    /**
     * Evaluate JStmtFor.
     */
    private Object evalIfStmt(JStmtIf anIfStmt, Object thisObject) throws Exception
    {
        // Get conditional
        JExpr condExpr = anIfStmt.getConditional();
        Object condValue = evalExpr(condExpr, thisObject);

        // Handle true: Get true statement and return eval
        if (Convert.booleanValue(condValue)) {
            JStmt trueStmt = anIfStmt.getStatement();
            return evalStmt(trueStmt, thisObject);
        }

        // If else statement set, forward to it
        JStmt elseStmt = anIfStmt.getElseStatement();
        if (elseStmt != null)
            return evalStmt(elseStmt, thisObject);

        // Return
        return null;
    }

    /**
     * Evaluate JStmtFor.
     */
    private Object evalForStmt(JStmtFor aForStmt, Object thisObject) throws Exception
    {
        // Handle ForEach
        if (aForStmt.isForEach())
            return evalForEachStmt(aForStmt, thisObject);

        // Get block statement
        JStmtBlock blockStmt = aForStmt.getBlock();

        // Get var decl and evaluate
        JExprVarDecl varDeclExpr = aForStmt.getVarDeclExpr();
        if (varDeclExpr != null)
            evalExpr(varDeclExpr, thisObject);

        // If init expressions instead, get and evaluate those
        else {
            JExpr[] initExprs = aForStmt.getInitExprs();
            for (JExpr initExpr : initExprs)
                evalExpr(initExpr, thisObject);
        }

        // Get conditional
        JExpr condExpr = aForStmt.getConditional();

        // Get update expressions
        JExpr[] updateExpressions = aForStmt.getUpdateExprs();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr, thisObject);
            if (!Convert.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(blockStmt, thisObject);

            // Execute update expressions
            for (JExpr updateExpr : updateExpressions) {

                // Eval statements
                evalExpr(updateExpr, thisObject);

                // If break was hit, break
                if (handleBreakCheck())
                    return _returnValueHit;
            }
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtFor.
     */
    private Object evalForEachStmt(JStmtFor aForStmt, Object thisObject) throws Exception
    {
        // Get block statement
        JStmtBlock blockStmt = aForStmt.getBlock();

        // Get variable name
        JExprVarDecl varDeclExpr = aForStmt.getVarDeclExpr();
        List<JVarDecl> varDecls = varDeclExpr.getVarDecls();
        JVarDecl varDecl = varDecls.get(0);
        JExprId varId = varDecl.getId();

        // Get list value
        JExpr iterableExpr = aForStmt.getIterableExpr();
        Object iterableObj = evalExpr(iterableExpr, thisObject);

        // Handle null
        if (iterableObj == null)
            throw new NullPointerException("JSStmtEval.evalForEachStmt: Iteration value is null");

        // If Object[], convert to list
        if (iterableObj.getClass().isArray()) {

            // Get array length
            int length = Array.getLength(iterableObj);

            // Iterate over objects
            for (int i = 0; i < length; i++) {

                // Get/set loop var
                Object obj = Array.get(iterableObj, i);
                _javaShell.setExprIdValue(varId, obj);

                // Eval statement
                evalStmt(blockStmt, thisObject);

                // If LoopLimit hit, throw exception
                if (handleBreakCheck())
                    return _returnValueHit;
            }
        }

        // Handle Iterable
        else if (iterableObj instanceof Iterable) {

            // Get iterable
            Iterable<?> iterable = (Iterable<?>) iterableObj;

            // Iterate over objects
            for (Object obj : iterable) {

                // Get/set loop var
                _javaShell.setExprIdValue(varId, obj);

                // Eval statement
                evalStmt(blockStmt, thisObject);

                // If LoopLimit hit, throw exception
                if (handleBreakCheck())
                    return _returnValueHit;
            }
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtWhile.
     */
    private Object evalWhileStmt(JStmtWhile aWhileStmt, Object thisObject) throws Exception
    {
        // Get conditional and block statement
        JExpr condExpr = aWhileStmt.getConditional();
        JStmt blockStmt = aWhileStmt.getStatement();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr, thisObject);
            if (!Convert.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(blockStmt, thisObject);

            // If break was hit, break
            if (handleBreakCheck())
                return _returnValueHit;
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtDo.
     */
    private Object evalDoStmt(JStmtDo aDoStmt, Object thisObject) throws Exception
    {
        // Get conditional and block statement
        JExpr condExpr = aDoStmt.getConditional();
        JStmt blockStmt = aDoStmt.getStatement();

        // Iterate while conditional is true
        while (true) {

            // Evaluate block statements
            evalStmt(blockStmt, thisObject);

            // If break was hit, break
            if (handleBreakCheck())
                return _returnValueHit;

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr, thisObject);
            if (!Convert.booleanValue(condValue))
                break;
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtTry.
     */
    private Object evalTryStmt(JStmtTry aTryStmt, Object thisObject) throws Exception
    {
        JExpr[] resourceExprs = aTryStmt.getResources();
        for (JExpr resourceExpr : resourceExprs)
            evalExpr(resourceExpr, thisObject);

        // Run try block
        JStmtBlock blockStmt = aTryStmt.getBlock();
        evalStmt(blockStmt, thisObject);

        // If finally statement availabe, run it
        JStmtBlock finallyStmt = aTryStmt.getFinallyBlock();
        if (finallyStmt != null)
            evalStmt(finallyStmt, thisObject);

        // Return
        return null;
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalExpr(JExpr anExpr, Object thisObject) throws Exception
    {
        return _javaShell._exprEval.evalExpr(anExpr, thisObject);
    }

    /**
     * This method checks and returns whether a break statement was hit in a loop.
     * For Browser, this checks whether a frame has passed and does a yield every 40 millis for progress bar.
     */
    private boolean handleBreakCheck()
    {
        // Check for BreakWasHit
        if (_breakWasHit || _stopRun) {
            _breakWasHit = false;
            return true;
        }

        // Check for return value hit
        if (_returnValueHit != null)
            return true;

        // Check for continueWasHit
        if (_continueWasHit)
            _continueWasHit = false;

        // Return no break
        return false;
    }
}
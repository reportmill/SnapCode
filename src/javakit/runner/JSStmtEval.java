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

    // The Expression evaluator
    protected JSExprEval _exprEval;

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
    public JSStmtEval()
    {
        super();

        // Create ExprEval
        _exprEval = new JSExprEval(this);
    }

    /**
     * Executes top level statements.
     */
    public Object evalExecutable(Object anOR, JStmt aStmt) throws Exception
    {
        // Eval statement and return
        try {
            _returnValueHit = null;
            Object returnVal = evalStmt(anOR, aStmt);
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
    public Object evalStmt(Object anOR, JStmt aStmt) throws Exception
    {
        // Dunno
        _exprEval._thisObj = anOR;

        // Handle Assert statement
        if (aStmt instanceof JStmtAssert)
            throw new RuntimeException("JSStmtEval: Assert Statement not implemented");

        // Handle block statement
        if (aStmt instanceof JStmtBlock)
            return evalBlockStmt(anOR, (JStmtBlock) aStmt);

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
            return evalDoStmt(anOR, (JStmtDo) aStmt);

        // Empty statement
        if(aStmt instanceof JStmtEmpty)
            return null;

        // Expression statement
        if (aStmt instanceof JStmtExpr)
            return evalExprStmt((JStmtExpr) aStmt);

        // For statement
        if (aStmt instanceof JStmtFor)
            return evalForStmt(anOR, (JStmtFor) aStmt);

        // Handle if statement
        if (aStmt instanceof JStmtIf)
            return evalIfStmt(anOR, (JStmtIf) aStmt);

        // Handle labeled statement
        if (aStmt instanceof JStmtLabeled)
            throw new RuntimeException("JSStmtEval: labeled Statement not implemented");

        // Handle return statement
        if (aStmt instanceof JStmtReturn)
            return evalReturnStmt(anOR, (JStmtReturn) aStmt);

        // Handle switch statement
        if (aStmt instanceof JStmtSwitch)
            throw new RuntimeException("JSStmtEval: switch Statement not implemented");

        // Handle sync statement
        if (aStmt instanceof JStmtSynchronized) {
            JStmtSynchronized syncStmt = (JStmtSynchronized) aStmt;
            return evalStmt(anOR, syncStmt.getBlock());
        }

        // Handle throw statement
        if (aStmt instanceof JStmtThrow)
            throw new RuntimeException("JSStmtEval: throw Statement not implemented");

        // Handle try statement
        if (aStmt instanceof JStmtTry)
            return evalTryStmt(anOR, (JStmtTry) aStmt);

        // Handle while statement
        if (aStmt instanceof JStmtWhile)
            return evalWhileStmt(anOR, (JStmtWhile) aStmt);

        // Complain
        throw new RuntimeException("EvalStmt.evalStmt: Unsupported statement " + aStmt.getClass());
    }

    /**
     * Evaluate JStmtBlock.
     */
    public Object evalBlockStmt(Object anOR, JStmtBlock aBlockStmt) throws Exception
    {
        // Get statements
        List<JStmt> statements = aBlockStmt.getStatements();
        Object returnVal = null;

        // Iterate over statements and evaluate each
        for (JStmt stmt : statements) {
            Object rval = evalStmt(anOR, stmt);
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
    public Object evalExprStmt(JStmtExpr aStmt) throws Exception
    {
        JExpr expr = aStmt.getExpr();
        Object val = evalExpr(expr);
        return val;
    }

    /**
     * Evaluate JStmtReturn.
     */
    public Object evalReturnStmt(Object anOR, JStmtReturn aReturnStmt) throws Exception
    {
        // Get return value
        Object returnVal = NULL_RETURN_VALUE;

        // If return expression set, evaluate and set returnVal
        JExpr returnExpr = aReturnStmt.getExpr();
        if (returnExpr != null) {
            returnVal = evalExpr(returnExpr);
            if (returnVal == null)
                returnVal = NULL_RETURN_VALUE;
        }

        // Set and return value
        return _returnValueHit = returnVal;
    }

    /**
     * Evaluate JStmtFor.
     */
    public Object evalIfStmt(Object anOR, JStmtIf anIfStmt) throws Exception
    {
        // Get conditional
        JExpr condExpr = anIfStmt.getConditional();
        Object condValue = evalExpr(condExpr);

        // Handle true: Get true statement and return eval
        if (Convert.booleanValue(condValue)) {
            JStmt trueStmt = anIfStmt.getStatement();
            return evalStmt(anOR, trueStmt);
        }

        // If else statement set, forward to it
        JStmt elseStmt = anIfStmt.getElseStatement();
        if (elseStmt != null)
            return evalStmt(anOR, elseStmt);

        // Return
        return null;
    }

    /**
     * Evaluate JStmtFor.
     */
    public Object evalForStmt(Object anOR, JStmtFor aForStmt) throws Exception
    {
        // Handle ForEach
        if (aForStmt.isForEach())
            return evalForEachStmt(anOR, aForStmt);

        // Get block statement
        JStmtBlock blockStmt = aForStmt.getBlock();

        // Get var decl and evaluate
        JExprVarDecl varDeclExpr = aForStmt.getVarDeclExpr();
        if (varDeclExpr != null)
            evalExpr(varDeclExpr);

        // If init expressions instead, get and evaluate those
        else {
            JExpr[] initExprs = aForStmt.getInitExprs();
            for (JExpr initExpr : initExprs)
                evalExpr(initExpr);
        }

        // Get conditional
        JExpr condExpr = aForStmt.getConditional();

        // Get update expressions
        JExpr[] updateExpressions = aForStmt.getUpdateExprs();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr);
            if (!Convert.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(anOR, blockStmt);

            // Execute update expressions
            for (JExpr updateExpr : updateExpressions) {

                // Eval statements
                evalExpr(updateExpr);

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
    public Object evalForEachStmt(Object anOR, JStmtFor aForStmt) throws Exception
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
        Object iterableObj = evalExpr(iterableExpr);

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
                _exprEval.setExprIdValue(varId, obj);

                // Eval statement
                evalStmt(anOR, blockStmt);

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
                _exprEval.setExprIdValue(varId, obj);

                // Eval statement
                evalStmt(anOR, blockStmt);

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
    public Object evalWhileStmt(Object anOR, JStmtWhile aWhileStmt) throws Exception
    {
        // Get conditional and block statement
        JExpr condExpr = aWhileStmt.getConditional();
        JStmt blockStmt = aWhileStmt.getStatement();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr);
            if (!Convert.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(anOR, blockStmt);

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
    public Object evalDoStmt(Object anOR, JStmtDo aDoStmt) throws Exception
    {
        // Get conditional and block statement
        JExpr condExpr = aDoStmt.getConditional();
        JStmt blockStmt = aDoStmt.getStatement();

        // Iterate while conditional is true
        while (true) {

            // Evaluate block statements
            evalStmt(anOR, blockStmt);

            // If break was hit, break
            if (handleBreakCheck())
                return _returnValueHit;

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr);
            if (!Convert.booleanValue(condValue))
                break;
        }

        // Return
        return null;
    }

    /**
     * Evaluate JStmtTry.
     */
    public Object evalTryStmt(Object anOR, JStmtTry aTryStmt) throws Exception
    {
        JExpr[] resourceExprs = aTryStmt.getResources();
        for (JExpr resourceExpr : resourceExprs)
            evalExpr(resourceExpr);

        // Run try block
        JStmtBlock blockStmt = aTryStmt.getBlock();
        evalStmt(anOR, blockStmt);

        // If finally statement availabe, run it
        JStmtBlock finallyStmt = aTryStmt.getFinallyBlock();
        if (finallyStmt != null)
            evalStmt(anOR, finallyStmt);

        // Return
        return null;
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalExpr(JExpr anExpr) throws Exception
    {
        return _exprEval.evalExpr(anExpr);
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
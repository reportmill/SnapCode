/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import java.util.*;
import javakit.parse.*;
import snap.util.Convert;
import snap.util.ListUtils;
import snap.util.SnapUtils;

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

    // A Yield count
    private long  _lastYield;

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
            throw new RuntimeException("JSStmtEval: try Statement not implemented");

        // Handle variable declaration statement
        if (aStmt instanceof JStmtVarDecl)
            return evalVarDeclStmt((JStmtVarDecl) aStmt);

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

        // Get main var name statement
        JStmtVarDecl initDeclStmt = aForStmt.getInitDecl();
        evalStmt(anOR, initDeclStmt);

        // Get conditional
        JExpr condExpr = aForStmt.getConditional();

        // Get update statements
        List<JStmtExpr> updateStmts = aForStmt.getUpdateStmts();

        // Iterate while conditional is true
        while (true) {

            // Evaluate conditional and break if false
            Object condValue = evalExpr(condExpr);
            if (!Convert.booleanValue(condValue))
                break;

            // Evaluate block statements
            evalStmt(anOR, blockStmt);

            // Execute update statements
            for (JStmtExpr updateStmt : updateStmts) {

                // Eval statements
                evalStmt(anOR, updateStmt);

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

        // Get main variable name
        JStmtVarDecl initDeclStmt = aForStmt.getInitDecl();
        List<JVarDecl> varDecls = initDeclStmt.getVarDecls();
        JVarDecl varDecl = varDecls.get(0);
        JExprId varId = varDecl.getId();

        // Get list value
        JExpr listExpr = aForStmt.getConditional();
        Object listValue = evalExpr(listExpr);

        // If Object[], convert to list
        if (listValue instanceof Object[])
            listValue = Arrays.asList((Object[]) listValue);

        // Handle Iterable
        if (listValue instanceof Iterable) {

            // Get iterable
            Iterable<?> iterable = (Iterable<?>) listValue;

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
     * Evaluate JStmtVarDecl.
     */
    public Object evalVarDeclStmt(JStmtVarDecl aStmt) throws Exception
    {
        // Get list
        List<JVarDecl> varDecls = aStmt.getVarDecls();
        List<Object> vals = new ArrayList<>();

        // Iterate over VarDecls
        for (JVarDecl varDecl : varDecls) {

            // If initializer expression, evaluate and set local var
            JExpr initExpr = varDecl.getInitializer();
            if (initExpr != null) {
                JExprId varId = varDecl.getId();
                Object val = evalExpr(initExpr);
                _exprEval.setExprIdValue(varId, val);
                vals.add(val);
            }
        }

        // If one value, just return it
        if (vals.size() == 1)
            return vals.get(0);

        // Otherwise, return joined string
        return ListUtils.joinStrings(vals, ", ");
    }

    /**
     * Evaluate JStmtExpr.
     */
    public Object evalExpr(JExpr anExpr) throws Exception
    {
        // Evaluate expr
        Object val = _exprEval.evalExpr(anExpr);

        // Return
        return val;
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

        // If TeaVM, check whether we need a yield
        if (SnapUtils.isTeaVM) {
            long time = System.currentTimeMillis();
            if (time - _lastYield > 40) {
                Thread.yield();
                _lastYield = time;
            }
        }

        // Return no break
        return false;
    }
}
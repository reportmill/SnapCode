/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for yield statement.
 */
public class JStmtYield extends JStmt {

    // The yield expression
    private JExpr _expr;

    /**
     * Constructor.
     */
    public JStmtYield()
    {
        super();
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Override to check errors.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If missing expression, return error
        if (_expr == null)
            return NodeError.newErrorArray(this, "Missing yield value");

        // If expression has errors, just return
        NodeError[] exprErrors = _expr.getErrors();
        if (exprErrors.length > 0)
            return exprErrors;

        // Return no errors
        return NodeError.NO_ERRORS;
    }
}
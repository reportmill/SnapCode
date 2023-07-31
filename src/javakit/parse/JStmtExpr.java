/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for statements that are just expressions (increment, decrement, assignment).
 */
public class JStmtExpr extends JStmt {

    // The expression
    protected JExpr  _expr;

    /**
     * Constructor.
     */
    public JStmtExpr()
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
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();

        // Handle missing statement
        if (_expr == null)
            errors = NodeError.addError(errors, this, "Missing or incomplete expression", 0);

        // Return
        return errors;
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import snap.util.ArrayUtils;

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
     * Tries to resolve the class name for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        if (_expr == null)
            return null;
        return _expr.getDecl();
    }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle missing statement
        if (_expr == null) {
            NodeError error = new NodeError(this, "Missing or incomplete expression");
            errors = ArrayUtils.add(errors, error);
        }

        // Otherwise init to expression errors
        else errors = _expr.getErrors();

        // Return
        return errors;
    }
}
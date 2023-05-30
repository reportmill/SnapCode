/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for AssertStatement.
 */
public class JStmtAssert extends JStmt {

    // The conditional expression
    protected JExpr  _cond;

    // The optional assertion value expression
    protected JExpr  _expr;

    /**
     * Returns the conditional.
     */
    public JExpr getConditional()  { return _cond; }

    /**
     * Sets the conditional.
     */
    public void setConditional(JExpr anExpr)
    {
        replaceChild(_cond, _cond = anExpr);
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the conditional.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

}
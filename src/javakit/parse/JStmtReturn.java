/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for ReturnStatement.
 */
public class JStmtReturn extends JStmt {
    // The return expression
    JExpr _expr;

    /**
     * Returns the expression.
     */
    public JExpr getExpr()
    {
        return _expr;
    }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

}
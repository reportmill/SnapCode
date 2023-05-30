/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for ThrowStatement.
 */
public class JStmtThrow extends JStmt {
    // The throw expression
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
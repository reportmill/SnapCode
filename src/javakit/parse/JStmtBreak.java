/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for Break statement.
 */
public class JStmtBreak extends JStmt {

    // The break label
    protected JExprId  _label;

    /**
     * Returns the label id.
     */
    public JExprId getLabel()  { return _label; }

    /**
     * Sets the label id.
     */
    public void setLabel(JExprId anExpr)
    {
        replaceChild(_label, _label = anExpr);
    }
}
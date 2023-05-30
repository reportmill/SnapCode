/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for ContinueStatement.
 */
public class JStmtContinue extends JStmt {
    // The continue label
    JExpr _label;

    /**
     * Returns the label.
     */
    public JExpr getLabel()
    {
        return _label;
    }

    /**
     * Sets the label.
     */
    public void setLabel(JExpr anExpr)
    {
        replaceChild(_label, _label = anExpr);
    }

}
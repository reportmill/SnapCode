/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JStatement for if() statements.
 */
public class JStmtIf extends JStmtConditional {

    // The else clause
    protected JStmt  _elseStmt;

    /**
     * Constructor.
     */
    public JStmtIf()
    {
        super();
    }

    /**
     * Returns the else statement.
     */
    public JStmt getElseStatement()  { return _elseStmt; }

    /**
     * Sets the else statement.
     */
    public void setElseStatement(JStmt aStmt)
    {
        replaceChild(_elseStmt, _elseStmt = aStmt);
    }
}
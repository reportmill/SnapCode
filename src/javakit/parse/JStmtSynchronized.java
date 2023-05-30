/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for SynchronizedStatement.
 */
public class JStmtSynchronized extends JStmt implements WithBlockStmt {

    // The synchronized expression
    protected JExpr  _expr;

    // The statement block
    protected JStmtBlock  _block;

    /**
     * Constructor.
     */
    public JStmtSynchronized()
    {
        super();
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpression()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpression(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }
}
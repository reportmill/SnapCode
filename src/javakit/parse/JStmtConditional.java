/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java statement for conditional/nested statements (while, do, if, for).
 */
public class JStmtConditional extends JStmt implements WithBodyStmt, WithBlockStmt, WithVarDecls {

    // The conditional expression
    protected JExpr  _cond;

    // The statement to perform while conditional is true
    protected JStmt  _stmt;

    /**
     * Returns the conditional.
     */
    public JExpr getConditional()  { return _cond; }

    /**
     * Sets the conditional.
     */
    public void setConditional(JExpr aCond)
    {
        replaceChild(_cond, _cond = aCond);
    }

    /**
     * Returns the statement.
     */
    public JStmt getStatement()  { return _stmt; }

    /**
     * Sets the statement.
     */
    public void setStatement(JStmt aStmt)
    {
        replaceChild(_stmt, _stmt = aStmt);
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()
    {
        // If already set, just return
        if (_stmt instanceof JStmtBlock || _stmt == null)
            return (JStmtBlock) _stmt;

        // Create StmtBlock, add statement and replace
        JStmtBlock stmtBlock = new JStmtBlock();
        stmtBlock.addStatement(_stmt);
        setStatement(stmtBlock);

        // Return
        return stmtBlock;
    }

    /**
     * Sets a block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        setStatement(aBlock);
    }

    /**
     * WithVarDecls method: Override to handle var decls in conditional (like instanceof pattern expression).
     */
    @Override
    public JVarDecl[] getVarDecls()  { return _cond != null ? _cond.getVarDecls() : new JVarDecl[0]; }

    /**
     * Override to provide errors for conditional statements.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();
        if (errors.length > 0)
            return errors;

        // Handle missing conditional
        if (_cond == null && !(this instanceof JStmtFor))
            return NodeError.newErrorArray(this, "Missing conditional");

        // Handle missing statement
        if (_stmt == null)
            return NodeError.newErrorArray(this, "Missing statement block");

        // Return
        return errors;
    }
}
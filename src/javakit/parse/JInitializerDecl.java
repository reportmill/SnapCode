/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JMemberDecl for Initializer declarations.
 */
public class JInitializerDecl extends JMemberDecl implements WithBlockStmt {

    // Whether initializer is static
    protected boolean  isStatic;

    // The Block statement of statements
    protected JStmtBlock  _block;

    /**
     * Returns whether is static.
     */
    public boolean isStatic()  { return isStatic; }

    /**
     * Sets whether is static.
     */
    public void setStatic(boolean aValue)  { isStatic = aValue; }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the statement block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * Override to return no errors for JFile.getDeclarationErrors().
     */
    @Override
    protected NodeError[] getErrorsImpl()  { return NodeError.NO_ERRORS; }
}
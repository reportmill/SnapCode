/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import java.util.List;

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
     * REPL hack - Override to check prior JInitDecls for VarDecl matching node name.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Do normal version - just return if successful
        JavaDecl decl = super.getDeclForChildExprIdNode(anExprId);
        if (decl != null)
            return decl;

        // Get enclosing class initDecls
        JClassDecl classDecl = getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Iterate over initDecls
        for (JInitializerDecl initDecl : initDecls) {

            // Stop when we hit this InitDecl
            if (initDecl == this)
                break;

            // Get InitDecl block statement and search
            JStmtBlock initDeclBlock = initDecl.getBlock();
            List<JStmt> initDeclStmts = initDeclBlock.getStatements();
            JVarDecl varDecl = JStmtBlock.getVarDeclForNameFromStatements(anExprId, initDeclStmts);
            if (varDecl != null)
                return varDecl.getDecl();
        }

        // Return not found
        return null;
    }
}
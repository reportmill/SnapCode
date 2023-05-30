package javakit.parse;
import javakit.resolver.JavaDecl;

import java.util.Objects;

/**
 * A JNode for a catch block
 */
public class JStmtTryCatch extends JNode implements WithBlockStmt {

    // The formal parameter
    protected JVarDecl  _param;

    // The catch block
    protected JStmtBlock  _block;

    /**
     * Constructor.
     */
    public JStmtTryCatch()
    {
        super();
    }

    /**
     * Returns the parameter.
     */
    public JVarDecl getParameter()  { return _param; }

    /**
     * Sets the parameter.
     */
    public void setParameter(JVarDecl aVD)
    {
        replaceChild(_param, _param = aVD);
    }

    /**
     * Returns the statement block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the statement block.
     */
    public void setBlock(JStmtBlock aStmtBlock)
    {
        replaceChild(_block, _block = aStmtBlock);
    }

    /**
     * Override to check param.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Check params
        String name = anExprId.getName();
        if (_param != null && Objects.equals(_param.getName(), name))
            return _param.getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }
}

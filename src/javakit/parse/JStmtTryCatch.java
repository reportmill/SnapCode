package javakit.parse;
import java.util.Collections;
import java.util.List;

/**
 * A JNode for a catch block
 */
public class JStmtTryCatch extends JNode implements WithVarDecls, WithBlockStmt {

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
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        return Collections.singletonList(_param);
    }
}

package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for expressions inside parenthesis.
 */
public class JExprParen extends JExpr {

    // The real expression for cast
    JExpr _expr;

    /**
     * Constructor.
     */
    public JExprParen()
    {
        super();
    }

    /**
     * Constructor with given expression.
     */
    public JExprParen(JExpr anExpr)
    {
        super();
        if (anExpr != null)
            setExpr(anExpr);
    }

    /**
     * Returns the expression being cast.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the cast expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "Paren"; }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()
    {
        if (_expr == null)
            return null;
        return _expr.getDecl();
    }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();

        // Handle missing statement
        if (_expr == null)
            errors = NodeError.addError(errors, this, "Missing or incomplete expression", 0);

        // Return
        return errors;
    }
}

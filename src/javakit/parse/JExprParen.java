package javakit.parse;
import javakit.resolver.JavaDecl;
import snap.util.ArrayUtils;

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
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle missing statement
        if (_expr == null) {
            NodeError error = new NodeError(this, "Missing or incomplete expression");
            errors = ArrayUtils.add(errors, error);
        }

        // Otherwise init to expression errors
        else errors = _expr.getErrors();

        // Return
        return errors;
    }
}

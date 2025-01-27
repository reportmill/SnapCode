/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for Cast expressions.
 */
public class JExprCast extends JExpr {

    // The cast type
    private JType _type;

    // The real expression for cast
    private JExpr _expr;

    /**
     * Constructor.
     */
    public JExprCast()
    {
        super();
    }

    /**
     * Returns the cast JType.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the cast JType.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
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
    public String getNodeString()  { return "Cast"; }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()
    {
        return _type != null ? _type.getDecl() : null;
    }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If Type or Expr have errors, just return them
        NodeError[] errors = super.getErrorsImpl();
        if (errors.length > 0)
            return errors;

        // Handle missing type or expression
        if (_type == null)
            return NodeError.newErrorArray(this, "Missing or incomplete cast type");
        if (_expr == null)
            return NodeError.newErrorArray(this, "Missing or incomplete cast expression");

        // Maybe add an "isCastable()" check? If common superclass is Object and expression class isn't Object, return false?

        // Return
        return errors;
    }
}

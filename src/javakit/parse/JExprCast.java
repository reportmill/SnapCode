/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import snap.util.ArrayUtils;

/**
 * A JExpr subclass for Cast expressions.
 */
public class JExprCast extends JExpr {

    // The cast type
    JType _type;

    // The real expression for cast
    JExpr _expr;

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
        if (_type == null)
            return null;
        return _type.getDecl();
    }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle missing type
        if (_type == null) {
            NodeError error = new NodeError(this, "Missing or incomplete type");
            errors = ArrayUtils.add(errors, error);
        }

        // Handle missing expression
        if (_expr == null) {
            NodeError error = new NodeError(this, "Missing or incomplete expression");
            errors = ArrayUtils.add(errors, error);
        }

        // Otherwise init to expression errors
        else {
            NodeError[] exprErrors = _expr.getErrors();
            if (exprErrors.length > 0)
                errors = ArrayUtils.addAll(errors, exprErrors);
        }

        // Return
        return errors;
    }
}

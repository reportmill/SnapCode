/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * This JExpr subclass represents a method reference: obj::method.
 */
public class JExprMethodRef extends JExpr {

    // The expression
    JExpr _expr;

    // The identifier
    JExprId _id;

    /**
     * Creates a new Method Reference expression for expression and id.
     */
    public JExprMethodRef(JExpr anExpr, JExprId anId)
    {
        setExpr(anExpr);
        setId(anId);
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()
    {
        return _expr;
    }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        if (_expr == null)
            addChild(_expr = anExpr, 0);
        else replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the identifier.
     */
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "MethodRef"; }
}
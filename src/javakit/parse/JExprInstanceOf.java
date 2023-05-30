/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for InstanceOf expressions.
 */
public class JExprInstanceOf extends JExpr {

    // The real expression for cast
    JExpr _expr;

    // The target type
    JType _type;

    /**
     * Returns the expression to be checked.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression to be checked.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the JType to be checked against.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the JType to be checked against.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "InstanceOf"; }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()
    {
        return getJavaClassForClass(boolean.class);
    }
}

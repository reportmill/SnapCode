/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * An expression subclass for a Type (JType), as in MyClass.class or MyClass.super.myMethod().
 */
public class JExprType extends JExpr {

    // The type
    JType  _type;

    /**
     * Creates a new JExprType for given JType.
     */
    public JExprType(JType aType)
    {
        setType(aType);
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
        if (_type != null) setName(_type.getName());
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "Type"; }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()
    {
        return _type.getDecl();
    }
}
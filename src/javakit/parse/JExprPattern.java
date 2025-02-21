/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JExpr subclass for pattern expressions.
 */
public class JExprPattern extends JExpr {

    // The modifiers
    protected JModifiers _modifiers;

    // The name identifier
    protected JExprId  _id;

    // The type/return-type
    protected JType  _type;

    /**
     * Constructor.
     */
    public JExprPattern()
    {
        super();
    }

    /**
     * Returns the modifiers.
     */
    public JModifiers getModifiers()
    {
        if (_modifiers == null)
            _modifiers = new JModifiers();
        return _modifiers;
    }

    /**
     * Sets the modifiers.
     */
    public void setModifiers(JModifiers aValue)
    {
        if (_modifiers == null)
            addChild(_modifiers = aValue, 0);
        else replaceChild(_modifiers, _modifiers = aValue);
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
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Returns the pattern type.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the pattern type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "PatternExpr"; }
}

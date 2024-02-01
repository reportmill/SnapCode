/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JNode for type members: fields, methods, constructors, inner classes.
 */
public class JMemberDecl extends JBodyDecl implements WithId {

    // The modifiers
    protected JModifiers _modifiers;

    // The name identifier
    protected JExprId  _id;

    /**
     * Constructor.
     */
    public JMemberDecl()
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
    @Override
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
}
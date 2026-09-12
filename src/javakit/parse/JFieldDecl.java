/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import snap.util.ArrayUtils;

/**
 * A JMember for Field declarations.
 */
public class JFieldDecl extends JMemberDecl implements WithVarDecls {

    // The type/return-type
    protected JType  _type;

    // List of variable declarations
    protected JVarDecl[] _vars = new JVarDecl[0];

    /**
     * Constructor.
     */
    public JFieldDecl()
    {
        super();
    }

    /**
     * Returns the field type.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the field type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the variable declarations.
     */
    public JVarDecl[] getVarDecls()  { return _vars; }

    /**
     * Adds a variable declarations.
     */
    public void addVarDecl(JVarDecl aVarDecl)
    {
        if (_type == null)
            _type = aVarDecl.getType();
        _vars = ArrayUtils.add(_vars, aVarDecl);
        addChild(aVarDecl);
    }

    /**
     * Override to return first var decl.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        JVarDecl varDecl = _vars.length > 0 ? _vars[0] : null;
        return varDecl != null ? varDecl.getDecl() : null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "FieldDecl";
    }
}
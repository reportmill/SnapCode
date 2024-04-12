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
     * Override to return errors for ReturnValue.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        JType returnType = getType();
        return returnType.getErrors();
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        return "FieldDecl";
    }
}
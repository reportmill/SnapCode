/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import java.util.*;

/**
 * A JMember for Field declarations.
 */
public class JFieldDecl extends JMemberDecl {

    // The type/return-type
    protected JType  _type;

    // List of variable declarations
    protected List<JVarDecl> _vars = new ArrayList<>();

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
    public List<JVarDecl> getVarDecls()  { return _vars; }

    /**
     * Adds a variable declarations.
     */
    public void addVarDecl(JVarDecl aVD)
    {
        _vars.add(aVD);
        addChild(aVD, -1);
    }

    /**
     * Override to return first var decl.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        JVarDecl varDecl = _vars.size() > 0 ? _vars.get(0) : null;
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
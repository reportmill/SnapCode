/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java expression for variable declarations.
 */
public class JExprVarDecl extends JExpr implements WithVarDecls {

    // The modifiers
    protected JModifiers  _mods;

    // The type/return-type
    protected JType  _type;

    // List of variable declarations
    protected List<JVarDecl>  _varDecls = new ArrayList<>();

    /**
     * Constructor.
     */
    public JExprVarDecl()
    {
        super();
    }

    /**
     * Returns the modifiers.
     */
    public JModifiers getMods()  { return _mods; }

    /**
     * Sets the modifiers.
     */
    public void setMods(JModifiers theMods)
    {
        if (_mods == null)
            addChild(_mods = theMods);
        else replaceChild(_mods, _mods = theMods);
    }

    /**
     * Returns the type.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the first var decl.
     */
    public JVarDecl getVarDecl()  { return _varDecls.size() > 0 ? _varDecls.get(0) : null; }

    /**
     * Returns the variable declarations.
     */
    @Override
    public List<JVarDecl> getVarDecls()  { return _varDecls; }

    /**
     * Adds a variable declaration.
     */
    public void addVarDecl(JVarDecl aVD)
    {
        _varDecls.add(aVD);
        addChild(aVD, -1);
    }

    /**
     * Override to return var decl.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        JVarDecl varDecl = getVarDecl();
        return varDecl != null ? varDecl.getDecl() : null;
    }

    /**
     * Override to just return first VarDecl error, if present.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Handle compound var
        if (_type.isVarType() && _varDecls.size() > 1)
            return NodeError.newErrorArray(_type, "'var' is not allowed in a compound declaration");

        // If any child VarDecls has errors, just return that
        for (JVarDecl varDecl : _varDecls) {
            NodeError[] varDeclErrors = varDecl.getErrors();
            if (varDeclErrors.length > 0)
                return varDeclErrors;
        }

        // Do normal version
        return super.getErrorsImpl();
    }
}
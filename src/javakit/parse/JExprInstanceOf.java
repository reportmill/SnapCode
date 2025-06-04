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
    private JExpr _expr;

    // The target type
    private JType _type;

    // The pattern
    private JExprPattern _pattern;

    /**
     * Constructor.
     */
    public JExprInstanceOf()
    {
        super();
    }

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
     * Returns the pattern.
     */
    public JExprPattern getPattern()  { return _pattern; }

    /**
     * Sets the pattern.
     */
    public void setPattern(JExprPattern aPattern)
    {
        replaceChild(_pattern, _pattern = aPattern);
        _type = _pattern.getType();
    }

    /**
     * Returns the pattern Var decl.
     */
    public JVarDecl getPatternVarDecl()  { return _pattern != null ? _pattern.getVarDecl() : null; }

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

    /**
     * Override to return pattern var decl, if set.
     */
    @Override
    protected JVarDecl[] getVarDeclsImpl()
    {
        JVarDecl patternVarDecl = getPatternVarDecl();
        return patternVarDecl != null ? new JVarDecl[] { patternVarDecl } : new JVarDecl[0];
    }
}

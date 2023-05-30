/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A Java part for package declaration.
 */
public class JPackageDecl extends JNode {

    // The package name identifier
    protected JExpr  _nameExpr;

    /**
     * Constructor.
     */
    public JPackageDecl()
    {
        super();
    }

    /**
     * Returns the name expression.
     */
    public JExpr getNameExpr()  { return _nameExpr; }

    /**
     * Sets the name expression.
     */
    public void setNameExpr(JExpr anExpr)
    {
        replaceChild(_nameExpr, _nameExpr = anExpr);
    }

    /**
     * Override to get name.
     */
    @Override
    protected String getNameImpl()
    {
        return _nameExpr != null ? _nameExpr.getName() : null;
    }
}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for local variable declarations.
 */
public class JStmtVarDecl extends JStmt implements WithVarDecls {

    // The var decl expression
    protected JExprVarDecl  _varDeclExpr;

    /**
     * Constructor.
     */
    public JStmtVarDecl()
    {
        super();
    }

    /**
     * Returns the VarDecl expression.
     */
    public JExprVarDecl getVarDeclExpr()  { return _varDeclExpr; }

    /**
     * Sets the VarDecl expression.
     */
    public void setVarDeclExpr(JExprVarDecl varDeclExpr)
    {
        replaceChild(_varDeclExpr, _varDeclExpr = varDeclExpr);
    }

    /**
     * Returns the modifiers.
     */
    public JModifiers getMods()  { return _varDeclExpr.getMods(); }

    /**
     * Returns the type.
     */
    public JType getType()  { return _varDeclExpr.getType(); }

    /**
     * Returns the variable declarations.
     */
    public List<JVarDecl> getVarDecls()  { return _varDeclExpr.getVarDecls(); }
}
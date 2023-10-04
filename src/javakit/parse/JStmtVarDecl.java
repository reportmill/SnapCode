/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.List;

/**
 * A Java statement for local variable declarations.
 */
public class JStmtVarDecl extends JStmtExpr implements WithVarDecls {

    /**
     * Constructor.
     */
    public JStmtVarDecl(JExprVarDecl varDeclExpr)
    {
        super();
        setExpr(varDeclExpr);
    }

    /**
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        JExprVarDecl varDeclExpr = (JExprVarDecl) _expr;
        return varDeclExpr.getVarDecls();
    }
}
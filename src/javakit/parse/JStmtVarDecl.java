/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.List;

/**
 * A Java statement for local variable declarations.
 */
public class JStmtVarDecl extends JStmtExpr {

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
    public List<JVarDecl> getVarDecls()
    {
        JExprVarDecl varDeclExpr = (JExprVarDecl) _expr;
        return varDeclExpr.getVarDecls();
    }
}
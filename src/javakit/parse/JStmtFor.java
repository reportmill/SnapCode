/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaDecl;

/**
 * A JStatement for for() statements.
 */
public class JStmtFor extends JStmtConditional implements WithVarDecls {

    // Whether this for statement is really ForEach
    protected boolean  _forEach = true;

    // The for-init declaration (if declaration)
    protected JStmtVarDecl  _initDecl;

    // The update
    protected List<JStmtExpr>  _updateStmts = new ArrayList<>();

    // The for-init List of StatementExpressions (if statement expressions)
    protected List<JStmtExpr>  _initStmts = new ArrayList<>();

    /**
     * Constructor.
     */
    public JStmtFor()
    {
        super();
    }

    /**
     * Returns whether for statement is ForEach.
     */
    public boolean isForEach()  { return _forEach; }

    /**
     * Returns the init declaration.
     */
    public JStmtVarDecl getInitDecl()  { return _initDecl; }

    /**
     * Sets the init declaration.
     */
    public void setInitDecl(JStmtVarDecl aVD)
    {
        replaceChild(_initDecl, _initDecl = aVD);
    }

    /**
     * Returns the update statements.
     */
    public List<JStmtExpr> getUpdateStmts()  { return _updateStmts; }

    /**
     * Add an update statements.
     */
    public void addUpdateStmt(JStmtExpr aStmtExpr)
    {
        _updateStmts.add(aStmtExpr);
        addChild(aStmtExpr, -1);
    }

    /**
     * Returns the init statements.
     */
    public List<JStmtExpr> getInitStmts()  { return _initStmts; }

    /**
     * Adds an init statements.
     */
    public void addInitStmt(JStmtExpr aStmtExpr)
    {
        _initStmts.add(aStmtExpr);
        addChild(aStmtExpr, -1);
    }

    /**
     * Returns the for statement InitDecl.VarDecls.
     */
    public List<JVarDecl> getVarDecls()
    {
        List<JVarDecl> varDecls = _initDecl != null ? _initDecl.getVarDecls() : Collections.EMPTY_LIST;
        return varDecls;
    }

    /**
     * Override to check init declaration.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // If any ForStmt.varDecls matches id expr name, return decl
        String name = anExprId.getName();
        JVarDecl varDecl = getVarDeclForName(name);
        if (varDecl != null)
            return varDecl.getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }
}
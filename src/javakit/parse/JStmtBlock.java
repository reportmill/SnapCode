/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for a block of statements.
 */
public class JStmtBlock extends JStmt implements WithStmts, WithVarDeclsX {

    // An array of VarDecls held by JStmtVarDecls
    private List<JVarDecl> _varDecls;

    /**
     * Constructor.
     */
    public JStmtBlock()
    {
        super();
    }

    /**
     * Returns the list of statements.
     */
    public List<JStmt> getStatements()
    {
        List<JStmt> stmts = (List<JStmt>) (List<?>) _children;
        return stmts;
    }

    /**
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt)
    {
        addChild(aStmt, -1);
    }

    /**
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt, int anIndex)
    {
        addChild(aStmt, anIndex);
    }

    /**
     * Removes a statement.
     */
    public int removeStatement(JStmt aStmt)
    {
        return removeChild(aStmt);
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        if (_varDecls != null) return _varDecls;
        List<JVarDecl> varDecls = WithStmts.getWithStmtsVarDecls(this);
        return _varDecls = varDecls;
    }
}
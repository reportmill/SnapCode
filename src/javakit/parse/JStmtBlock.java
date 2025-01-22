/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import java.util.*;

/**
 * A Java statement for a block of statements.
 */
public class JStmtBlock extends JStmt implements WithStmts, WithVarDeclsX {

    // An array of VarDecls held by JStmtVarDecls
    private JVarDecl[] _varDecls;

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
        addChild(aStmt);
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        if (_varDecls != null) return _varDecls;
        JVarDecl[] varDecls = WithStmts.getVarDecls(this);
        return _varDecls = varDecls;
    }

    /**
     * Override to try to resolve given id name from any preceding ClassDecl statements.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If any previous statements are class decl statements that declare type, return class
        JavaClass javaClass = WithStmts.getJavaClassForChildTypeOrId(this, anExprId);
        if (javaClass != null)
            return javaClass;

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Override to try to resolve given type name from any preceding ClassDecl statements.
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType aJType)
    {
        // If any previous statements are class decl statements that declare type, return class
        JavaClass javaClass = WithStmts.getJavaClassForChildTypeOrId(this, aJType);
        if (javaClass != null)
            return javaClass;

        // Do normal version
        return super.getJavaTypeForChildType(aJType);
    }
}
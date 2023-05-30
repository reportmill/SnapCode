/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.Objects;

/**
 * A Java statement for Labeled Statement.
 */
public class JStmtLabeled extends JStmt implements WithBodyStmt {

    // The label identifier
    protected JExprId  _labelId;

    // The actual statement
    protected JStmt  _stmt;

    /**
     * Returns the label.
     */
    public JExprId getLabel()  { return _labelId; }

    /**
     * Sets the label.
     */
    public void setLabel(JExprId anExpr)
    {
        replaceChild(_labelId, _labelId = anExpr);
        setName(_labelId.getName());
    }

    /**
     * Returns the statement.
     */
    public JStmt getStatement()  { return _stmt; }

    /**
     * Sets the statement.
     */
    public void setStatement(JStmt aStmt)
    {
        replaceChild(_stmt, _stmt = aStmt);
    }

    /**
     * Override to create LocalVarDecl.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // Create JavaLocalVar for label
        Resolver resolver = getResolver();
        String name = getName();
        JavaType evalType = getJavaClassForClass(void.class);
        String uniqueId = JVarDecl.getUniqueId(this, name, evalType);
        JavaLocalVar localVar = new JavaLocalVar(resolver, name, evalType, uniqueId);

        // Return
        return localVar;
    }

    /**
     * Override to handle label variable declaration.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Check label name
        String name = anExprId.getName();
        if (Objects.equals(name, getName()))
            return getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }
}
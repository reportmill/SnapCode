/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaParameterizedType;
import javakit.resolver.JavaType;

/**
 * A JStatement for for() statements.
 */
public class JStmtFor extends JStmtConditional implements WithVarDecls {

    // Whether this for statement is really ForEach
    protected boolean  _forEach = true;

    // The for-init declaration (if declaration)
    protected JExprVarDecl  _initDecl;

    // The init expressions
    private List<JExpr> _initExpressions = new ArrayList<>();

    // The update expressions
    private List<JExpr> _updateExpressions = new ArrayList<>();

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
    public JExprVarDecl getInitDecl()  { return _initDecl; }

    /**
     * Sets the init declaration.
     */
    public void setInitDecl(JExprVarDecl aVD)
    {
        replaceChild(_initDecl, _initDecl = aVD);
    }

    /**
     * Returns the init expression.
     */
    public List<JExpr> getInitExpressions()  { return _initExpressions; }

    /**
     * Adds an init expression.
     */
    public void addInitExpression(JExpr anExpr)
    {
        _initExpressions.add(anExpr);
        addChild(anExpr, -1);
    }

    /**
     * Returns the update expressions.
     */
    public List<JExpr> getUpdateExpressions()  { return _updateExpressions; }

    /**
     * Add an update expressions.
     */
    public void addUpdateExpression(JExpr anExpr)
    {
        _updateExpressions.add(anExpr);
        addChild(anExpr, -1);
    }

    /**
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        List<JVarDecl> varDecls = _initDecl != null ? _initDecl.getVarDecls() : Collections.EMPTY_LIST;
        return varDecls;
    }

    /**
     * Returns the ForEach iteration type.
     */
    public JavaType getForEachIterationType()
    {
        // Get ForEach initializer
        JExpr initExpr = getConditional();
        if (initExpr == null)
            return null;

        // Get iterable type (just return if null)
        JavaType iterableType = initExpr.getEvalType();
        if (iterableType == null)
            return null;

        // Handle iterable is array: Return component type
        if (iterableType.isArray())
            return iterableType.getComponentType();

        // Handle iterable is ParameterizedType: Return first
        if (iterableType instanceof JavaParameterizedType) {
            JavaParameterizedType parameterizedType = (JavaParameterizedType) iterableType;
            JavaType[] parameterTypes = parameterizedType.getParamTypes();
            if (parameterTypes.length > 0)
                return parameterTypes[0];
        }

        // Show some surprise
        System.err.println("JStmtFor.getForEachIterationType: Can't determine type for iterable: " + iterableType);
        return null;
    }
}
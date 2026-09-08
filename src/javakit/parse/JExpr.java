/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.*;

/**
 * The JNode base class for Java expressions.
 */
public abstract class JExpr extends JNode implements WithVarDecls {

    // Var decls cached
    private JVarDecl[] _varDecls;

    // Constant for empty expressions
    public static final JExpr[] EMPTY_EXPR_ARRAY = new JExpr[0];

    /**
     * Constructor.
     */
    public JExpr()
    {
        super();
    }

    /**
     * Returns the scope expression for this expression, if this expression has scope.
     */
    public JExpr getScopeExpr()
    {
        switch (getParent()) {

            // Handle dot expression: If this node is DotExpr.Expr, return scope expression
            case JExprDot dotExpr -> {
                if (dotExpr.getExpr() == this)
                    return dotExpr.getScopeExpr();
            }

            // Handle method call: If this node is method name, return scope expression
            case JExprMethodCall methodCallExpr -> {
                if (methodCallExpr.getId() == this)
                    return methodCallExpr.getScopeExpr();
            }

            // Handle method ref: If this node is method name, return scope expression
            case JExprMethodRef methodRef -> {
                if (methodRef.getMethodId() == this)
                    return methodRef.getScopeExpr();
            }

            case null, default -> { }
        }

        // Return not found
        return null;
    }

    /**
     * Returns whether expression is a class name literal.
     */
    public boolean isClassNameLiteral()
    {
        // Get id for expression if simple id or dot expression
        JExpr expr = this instanceof JExprDot dotExpr ? dotExpr.getExpr() : this;
        JExprId exprId = expr instanceof JExprId ? (JExprId) expr : null;
        if (exprId == null)
            return false;

        // Get expression decl (just return if not type)
        JavaDecl decl = getDecl();
        JavaType javaType = decl instanceof JavaType ? (JavaType) decl : null;
        if (javaType == null)
            return false;

        // Get class name for expression EvalClass
        JavaClass exprEvalClass = javaType.getEvalClass();
        String className = exprEvalClass.getSimpleName();

        String exprStr = exprId.getName();
        return exprStr.equals(className);
    }

    /**
     * WithVarDecl method: Override to handle nested expressions with VarDecls (JExprInstanceOf with pattern).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        if (_varDecls != null) return _varDecls;
        return _varDecls = getVarDeclsImpl();
    }

    /**
     * WithVarDecl method: Override to handle nested expressions with VarDecls (JExprInstanceOf with pattern).
     */
    protected JVarDecl[] getVarDeclsImpl()
    {
        List<JExprInstanceOf> instanceOfExprs = getChildrenForClassDeep(JExprInstanceOf.class);
        if (instanceOfExprs.isEmpty())
            return new JVarDecl[0];
        List<JVarDecl> varDecls = new ArrayList<>();
        instanceOfExprs.forEach(instanceOfExpr -> Collections.addAll(varDecls, instanceOfExpr.getVarDecls()));
        return varDecls.toArray(new JVarDecl[0]);
    }
}
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
     * Returns the prefix expression for this expression, if this expression is part of dot expression.
     */
    public JExpr getScopeExpr()
    {
        // If parent is JExprDot and this is DotExpr.Expr, return DotExpr.PrefixExpr
        JNode parent = getParent();
        if (parent instanceof JExprDot dotExpr) {
            if (dotExpr.getExpr() == this)
                return dotExpr.getPrefixExpr();
        }

        // If parent is method call and this node is name, get parent for method call
        if (parent instanceof JExprMethodCall methodCallExpr) {
            if (methodCallExpr.getId() == this)
                return methodCallExpr.getScopeExpr();
        }

        // If parent is method ref and this is MethodRef.Id, return MethodRef.PrefixExpr
        if (parent instanceof JExprMethodRef methodRef) {
            if (methodRef.getMethodId() == this)
                return methodRef.getPrefixExpr();
        }

        // Return not found
        return null;
    }

    /**
     * Returns the JavaType for the scope expression (if present) or enclosing class.
     */
    public JavaType getScopeEvalType()
    {
        // If scope expression exists, return its decl
        JExpr scopeExpr = getScopeExpr();
        if (scopeExpr != null)
            return scopeExpr.getEvalType();

        // Otherwise, return enclosing class
        JClassDecl classDecl = getEnclosingClassDecl();
        if (classDecl != null)
            return classDecl.getEvalType();

        // Return not found
        return null;
    }

    /**
     * Returns whether expression is a class name literal.
     */
    public boolean isClassNameLiteral()
    {
        // Get id for expression if simple id or dot expression
        JExpr expr = this instanceof JExprDot ? ((JExprDot) this).getExpr() : this;
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
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;

/**
 * The JNode base class for Java expressions.
 */
public abstract class JExpr extends JNode {

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
        if (parent instanceof JExprDot) {
            JExprDot dotExpr = (JExprDot) parent;
            if (dotExpr.getExpr() == this)
                return dotExpr.getPrefixExpr();
        }

        // If parent is method call and this node is name, get parent for method call
        if (parent instanceof JExprMethodCall) {
            JExprMethodCall methodCallExpr = (JExprMethodCall) parent;
            if (methodCallExpr.getId() == this)
                return methodCallExpr.getScopeExpr();
        }

        // If parent is method ref and this is MethodRef.Id, return MethodRef.PrefixExpr
        if (parent instanceof JExprMethodRef) {
            JExprMethodRef methodRef = (JExprMethodRef) parent;
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
     * Joins two expressions together and returns the result (for PrimaryExprHandler).
     */
    protected static JExpr joinPrimaryPrefixAndSuffixExpressions(JExpr prefixExpr, JExpr suffixExpr)
    {
        // Handle MethodCall: Set prefix expression
        if (suffixExpr instanceof JExprMethodCall) {

            // Get method call and check that id is null (should not be possible)
            JExprMethodCall methodCall = (JExprMethodCall) suffixExpr;
            if (methodCall.getId() != null)
                System.err.println("JExpr.joinExpression: Unexpected method call with id: " + methodCall.getId().getName());

            // If prefix is dot expression, replace dotExpr.Expr with method call
            if (prefixExpr instanceof JExprDot) {
                JExprDot dotExpr = (JExprDot) prefixExpr;
                JExpr dotExprExpr = dotExpr.getExpr();
                if (dotExprExpr instanceof JExprId) {
                    JExprId idExpr = (JExprId) dotExprExpr;
                    dotExpr.setExpr(methodCall);
                    methodCall.setId(idExpr);
                }
                return prefixExpr;
            }

            // Otherwise set id and return method call
            JExprId idExpr = (JExprId) prefixExpr;
            methodCall.setId(idExpr);
            return suffixExpr;
        }

        // Handle MethodRef: Set prefix expression
        if (suffixExpr instanceof JExprMethodRef) {
            JExprMethodRef methodRef = (JExprMethodRef) suffixExpr;
            methodRef.setPrefixExpr(prefixExpr);
            return methodRef;
        }

        // If ArrayIndex with missing ArrayExpr, set and return
        if (suffixExpr instanceof JExprArrayIndex) {
            JExprArrayIndex arrayIndexExpr = (JExprArrayIndex) suffixExpr;
            if (arrayIndexExpr.getArrayExpr() != null)
                System.err.println("JExpr.join: ArrayIndex.ArrayExpr not null");
            arrayIndexExpr.setArrayExpr(prefixExpr);
            return arrayIndexExpr;
        }

        // Handle two arbitrary expressions
        return new JExprDot(prefixExpr, suffixExpr);
    }
}
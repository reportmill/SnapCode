/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaType;

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
     * Joins two expressions together and returns the result.
     */
    public static JExpr joinExpressions(JExpr expr1, JExpr expr2)
    {
        // Handle MethodCall or MethodRef with missing prefix expression: Set prefix expression and return
        if (expr2 instanceof WithId && ((WithId) expr2).getId() == null) {

            // If prefix is dot expression, replace dotExpr.Expr with method call
            WithId methodCallOrRef = (WithId) expr2;
            if (expr1 instanceof JExprDot) {
                JExprDot dotExpr = (JExprDot) expr1;
                JExpr dotExprExpr = dotExpr.getExpr();
                if (dotExprExpr instanceof JExprId) {
                    JExprId idExpr = (JExprId) dotExprExpr;
                    dotExpr.setExpr(expr2);
                    methodCallOrRef.setId(idExpr);
                }
                return expr1;
            }

            // Otherwise set id and return method call
            JExprId idExpr = (JExprId) expr1;
            methodCallOrRef.setId(idExpr);
            return expr2;
        }

        // If ArrayIndex with missing ArrayExpr, set and return
        if (expr2 instanceof JExprArrayIndex) {
            JExprArrayIndex arrayIndexExpr = (JExprArrayIndex) expr2;
            if (arrayIndexExpr.getArrayExpr() != null)
                System.err.println("JExpr.join: ArrayIndex.ArrayExpr not null");
            arrayIndexExpr.setArrayExpr(expr1);
            return arrayIndexExpr;
        }

        // Handle two arbitrary expressions
        return new JExprDot(expr1, expr2);
    }
}
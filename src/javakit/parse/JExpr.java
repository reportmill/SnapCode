/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.util.ArrayUtils;
import java.util.Collections;
import java.util.List;

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
     * Returns the node the expression should be evaluated against.
     */
    public JNode getScopeNode()
    {
        // Get parent expression, and if found, return its type class
        JExpr parentExpr = getParentExpr();
        if (parentExpr != null)
            return parentExpr;

        // Otherwise, return enclosing class
        return getEnclosingClassDecl();
    }

    /**
     * Returns the expression prior to this expression, if parent is JExprChain and this expression isn't first.
     */
    public JExpr getParentExpr()
    {
        // If parent is JExprChain, iterate over expressions and return one before this expression
        JNode par = getParent();
        if (par instanceof JExprChain) {
            JExprChain exprChain = (JExprChain) par;
            List<JExpr> chainExprs = exprChain.getExpressions();
            for (int i = 0, iMax = chainExprs.size(); i < iMax; i++) {
                JExpr expr = exprChain.getExpr(i);
                if (expr == this)
                    return i > 0 ? exprChain.getExpr(i - 1) : null;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Joins two expressions together and returns the result.
     */
    public static JExpr joinExpressions(JExpr expr1, JExpr expr2)
    {
        // Handle null expression: Just return second expression
        if (expr1 == null)
            return expr2;

        // Handle MethodCall or MethodRef with missing prefix expression: Set prefix expression and return
        if (expr2 instanceof JExprMethodCall && ((JExprMethodCall) expr2).getId() == null)
            return setExpr(expr1, expr2);
        if (expr2 instanceof JExprMethodRef && ((JExprMethodRef) expr2).getExpr() == null)
            return setExpr(expr1, expr2);

        // If ArrayIndex with missing ArrayExpr, set and return
        if (expr2 instanceof JExprArrayIndex) {
            JExprArrayIndex arrayIndexExpr = (JExprArrayIndex) expr2;
            if (arrayIndexExpr.getArrayExpr() != null)
                System.err.println("JExpr.join: ArrayIndex.ArrayExpr not null");
            arrayIndexExpr.setArrayExpr(expr1);
            return arrayIndexExpr;
        }

        // Handle ExprChain
        if (expr1 instanceof JExprChain) {
            ((JExprChain) expr1).addExpr(expr2);
            return expr1;
        }

        // Handle two arbitrary expressions
        return new JExprChain(expr1, expr2);
    }

    /**
     * Sets a given expression in given MethodCall or MethodRef and returns the head expression.
     */
    private static JExpr setExpr(JExpr expr1, JExpr expr2)
    {
        // If given expression chain, pick off last expression, set it instead and return chain
        if (expr1 instanceof JExprChain) {
            JExprChain exprChain = (JExprChain) expr1;
            int exprCount = exprChain.getExprCount();
            JExpr lastExpr = (JExpr) exprChain._children.remove(exprCount - 1);
            setExpr(lastExpr, expr2);
            exprChain.addExpr(expr2);
            return expr1;
        }

        // Handle MethodCall
        if (expr2 instanceof JExprMethodCall) {
            JExprMethodCall methodCall = (JExprMethodCall) expr2;
            if (expr1 instanceof JExprId)
                methodCall.setId((JExprId) expr1);
        }

        // Handle MethodRef
        else if (expr2 instanceof JExprMethodRef) {
            JExprMethodRef methodRef = (JExprMethodRef) expr2;
            methodRef.setExpr(expr1);
        }

        // Return
        return expr2;
    }

    /**
     * Returns the node errors.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If no children, return no errors
        if (_children == Collections.EMPTY_LIST)
            return NodeError.NO_ERRORS;

        NodeError[] errors = NodeError.NO_ERRORS;

        // Iterate over children and add any errors for each
        for (JNode child : _children) {
            NodeError[] childErrors = child.getErrors();
            if (childErrors.length > 0)
                errors = ArrayUtils.addAll(errors, childErrors);
        }

        // Return
        return errors;
    }
}
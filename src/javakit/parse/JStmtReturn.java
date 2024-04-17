/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaType;

/**
 * A Java statement for return statement.
 */
public class JStmtReturn extends JStmt {

    // The return expression
    private JExpr _expr;

    /**
     * Constructor.
     */
    public JStmtReturn()
    {
        super();
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Override to check errors.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Handle Constructor
        JExecutableDecl executableDecl = getParent(JExecutableDecl.class);
        if (executableDecl instanceof JConstrDecl) {
            if (_expr != null)
                return NodeError.newErrorArray(_expr, "Unexpected return value");
            return NodeError.NO_ERRORS;
        }

        // Handle Method
        JType returnJType = ((JMethodDecl) executableDecl).getReturnType();
        JavaType returnType = returnJType != null ? returnJType.getJavaType() : null;
        JavaClass returnClass = returnType != null ? returnType.getEvalClass() : null;
        JavaType exprType = _expr != null ? _expr.getEvalType() : getJavaClassForName("void");
        JavaClass exprClass = exprType.getEvalClass();

        // If types don't match, return error
        if (returnClass == null || !returnClass.isAssignableFrom(exprClass)) {
            String returnClassName = returnClass != null ? returnClass.getClassName() : "null";
            String msg = "Incompatible types: " + exprType.getClassName() + " cannot be converted to " + returnClassName;
            return NodeError.newErrorArray(_expr != null ? _expr : this, msg);
        }

        // Return no errors
        return NodeError.NO_ERRORS;
    }
}
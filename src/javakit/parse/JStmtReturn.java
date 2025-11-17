/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
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
        // Handle Constructor: Return error if unexpected return type
        JExecutableDecl executableDecl = getParent(JExecutableDecl.class);
        if (executableDecl instanceof JConstrDecl || executableDecl == null) {
            if (_expr != null)
                return NodeError.newErrorArray(_expr, "Unexpected return value");
            return NodeError.NO_ERRORS;
        }

        // Handle Method: Get method return type and complain if not assignable
        JType returnJType = ((JMethodDecl) executableDecl).getReturnType();
        JavaType returnType = returnJType != null ? returnJType.getJavaType() : null;
        if (returnType == null) // If not found, just return and let method complain
            return NodeError.NO_ERRORS;

        // If return type is void and expression is set, return error
        if (returnType.getName().equals("void")) {
            if (_expr != null)
                return NodeError.newErrorArray(_expr, "Unexpected return value");
            return NodeError.NO_ERRORS;
        }

        // If missing expression, return error
        if (_expr == null)
            return NodeError.newErrorArray(this, "Missing return value");

        // If expression has errors, just return
        NodeError[] exprErrors = _expr.getErrors();
        if (exprErrors.length > 0)
            return exprErrors;

        // If expression type not compatible with return type, return error
        JavaType exprType = _expr.getEvalType();
        if (!returnType.isAssignableFrom(exprType))
            return NodeError.newErrorArray(_expr, "Incompatible types: " + exprType.getClassName() + " cannot be converted to " + returnType.getClassName());

        // Return no errors
        return NodeError.NO_ERRORS;
    }
}
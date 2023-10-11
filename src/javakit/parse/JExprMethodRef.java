/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaConstructor;
import javakit.resolver.JavaMethod;
import javakit.resolver.JavaType;
import snap.util.ListUtils;

import java.util.List;

/**
 * This JExpr subclass represents a method reference: obj::method.
 */
public class JExprMethodRef extends JExpr {

    // The expression
    JExpr _expr;

    // The identifier
    JExprId _id;

    // The type for this lambda
    private JavaType _lambdaType;

    // The actual interface method this lambda represents
    private JavaMethod _lambdaMethod;

    // A constant to define the types of method refs.
    // See https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html
    public enum Type { StaticMethod, InstanceMethod, HelperInstanceMethod, Constructor };

    /**
     * Creates a new Method Reference expression for expression and id.
     */
    public JExprMethodRef(JExpr anExpr, JExprId anId)
    {
        setExpr(anExpr);
        setId(anId);
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()
    {
        return _expr;
    }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        if (_expr == null)
            addChild(_expr = anExpr, 0);
        else replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the identifier.
     */
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
    }

    /**
     * Returns the specific method in the lambda class interface that is to be called.
     */
    public JavaMethod getLambdaMethod()
    {
        // If already set, just return
        if (_lambdaMethod != null) return _lambdaMethod;

        // Get lambda class and lambda method with correct arg count
        JavaClass lambdaClass = getLambdaClass();
        JavaMethod lambdaMethod = lambdaClass != null ? lambdaClass.getLambdaMethod() : null;

        // Set/return
        return _lambdaMethod = lambdaMethod;
    }

    /**
     * Returns the lambda type.
     */
    public JavaType getLambdaType()
    {
        if (_lambdaType != null) return _lambdaType;
        JavaType lambdaType = getLambdaTypeImpl();
        return _lambdaType = lambdaType;
    }

    /**
     * Returns the lambda type.
     */
    private JavaType getLambdaTypeImpl()
    {
        // Get Parent (just return if null)
        JNode parentNode = getParent();
        if (parentNode == null)
            return null;

        // Handle parent is method call: Get lambda interface from method call decl param
        if (parentNode instanceof JExprMethodCall) {

            // Get methodCall method
            JExprMethodCall methodCall = (JExprMethodCall) parentNode;
            JavaMethod method = methodCall.getDecl();
            if (method == null)
                return null;

            // Get arg index of this lambda expr
            List<JExpr> args = methodCall.getArgs();
            int argIndex = ListUtils.indexOfId(args, this);
            if (argIndex < 0)
                return null;

            // Get arg type at arg index
            return method.getParameterType(argIndex);
        }

        // Handle parent is alloc expression: Get lambda interface from alloc expression param
        if (parentNode instanceof JExprAlloc) {

            // Get alloc expr contructor
            JExprAlloc allocExpr = (JExprAlloc) parentNode;
            JavaConstructor constructor = (JavaConstructor) allocExpr.getDecl();
            if (constructor == null)
                return null;

            // Get arg index of this lambda expr
            List<JExpr> args = allocExpr.getArgs();
            int argIndex = ListUtils.indexOfId(args, this);
            if (argIndex < 0)
                return null;

            // Get arg type at arg index
            return constructor.getParameterType(argIndex);
        }

        // Handle parent anything else (JVarDecl, JStmtExpr): Return parent eval type
        JavaType lambdaType = parentNode.getEvalType();
        if (lambdaType != null)
            return lambdaType;

        // Return not found
        return null;
    }

    /**
     * Returns the lambda class.
     */
    public JavaClass getLambdaClass()
    {
        JavaType lambdaType = getLambdaType();
        return lambdaType != null ? lambdaType.getEvalClass() : null;
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "MethodRef"; }
}
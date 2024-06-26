/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.Objects;

/**
 * This JExpr subclass represents a method reference: obj::method.
 */
public class JExprMethodRef extends JExprLambdaBase {

    // The prefix expression
    private JExpr _prefixExpr;

    // The method name identifier
    private JExprId _methodId;

    // The type of method ref
    private Type _type;

    // The method
    private JavaMethod _method;

    // The constructor
    private JavaConstructor _constructor;

    // A constant to define the types of method refs.
    // See https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html
    public enum Type { InstanceMethod, StaticMethod, HelperMethod, Constructor, Unknown }

    /**
     * Creates a new Method Reference expression for expression and id.
     */
    public JExprMethodRef(JExpr prefixExpr, JExprId anId)
    {
        setPrefixExpr(prefixExpr);
        setMethodId(anId);
    }

    /**
     * Returns the prefix expression.
     */
    public JExpr getPrefixExpr()  { return _prefixExpr; }

    /**
     * Sets the prefix expression.
     */
    public void setPrefixExpr(JExpr anExpr)
    {
        if (_prefixExpr == null)
            addChild(_prefixExpr = anExpr, 0);
        else replaceChild(_prefixExpr, _prefixExpr = anExpr);
    }

    /**
     * Returns the method name identifier.
     */
    public JExprId getMethodId()  { return _methodId; }

    /**
     * Sets the method name identifier.
     */
    public void setMethodId(JExprId anId)
    {
        replaceChild(_methodId, _methodId = anId);
    }

    /**
     * Returns the type.
     */
    public Type getType()
    {
        if (_type != null) return _type;
        return _type = getTypeImpl();
    }

    /**
     * Returns the type.
     */
    private Type getTypeImpl()
    {
        if (Objects.equals(getMethodName(), "new"))
            return Type.Constructor;

        JavaMethod method = getMethod();
        if (method == null)
            return Type.Unknown;
        if (method.getParameterCount() == 0)
            return Type.InstanceMethod;
        if (_prefixExpr.isClassNameLiteral())
            return Type.StaticMethod;
        return Type.HelperMethod;
    }

    /**
     * Returns the method name.
     */
    public String getMethodName()  { return _methodId != null ? _methodId.getName() : null; }

    /**
     * Tries to resolve the method declaration for this node.
     */
    public JavaMethod getMethod()
    {
        if (_method != null) return _method;
        return _method = getMethodImpl();
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaMethod getMethodImpl()
    {
        // Get method name
        String methodName = getMethodName();
        if (methodName == null)
            return null;

        // Get prefix expr eval class
        JExpr prefixExpr = _prefixExpr;
        JavaType prefixEvalType = prefixExpr != null ? prefixExpr.getEvalType() : null;
        JavaClass prefixClass = prefixEvalType != null ? prefixEvalType.getEvalClass() : null;
        if (prefixClass == null)
            return null;

        // Get parameter types from lambda method and look for method
        JavaClass[] paramTypes = getLambdaMethodParameterTypesResolved();
        if (paramTypes == null)
            return null;

        // If one parameter with same class as prefix expression class, search for instance method with no args
        if (paramTypes.length == 1) {
            JavaClass paramClass = paramTypes[0];
            if (prefixClass.isAssignableFrom(paramClass)) {
                JavaMethod instanceMethod = JavaClassUtils.getCompatibleMethodAll(prefixClass, methodName, new JavaClass[0], false);
                if (instanceMethod != null && !instanceMethod.isStatic())
                    return instanceMethod;
            }
        }

        // Get lambda method - just return if null
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;

        // Get whether scope expression is class name literal
        boolean staticOnly = prefixExpr.isClassNameLiteral();

        // Search for static or helper method for name and arg types
        JavaMethod helperMethod = JavaClassUtils.getCompatibleMethodAll(prefixClass, methodName, paramTypes, staticOnly);
        if (helperMethod != null)
            return helperMethod;

        // Return not found
        return null;
    }

    /**
     * Tries to resolve the constructor declaration for this node (if type is constructor).
     */
    public JavaConstructor getConstructor()
    {
        if (_constructor != null) return _constructor;
        return _constructor = getConstructorImpl();
    }

    /**
     * Tries to resolve the constructor declaration for this node (if type is constructor).
     */
    protected JavaConstructor getConstructorImpl()
    {
        // Get method name
        String methodName = getMethodName();
        if (!Objects.equals(methodName, "new"))
            return null;

        // Get prefix expr eval class
        JExpr prefixExpr = _prefixExpr;
        JavaType prefixEvalType = prefixExpr != null ? prefixExpr.getEvalType() : null;
        JavaClass prefixClass = prefixEvalType != null ? prefixEvalType.getEvalClass() : null;
        if (prefixClass == null)
            return null;

        // Return default constructor
        return prefixClass.getDeclaredConstructorForClasses(new JavaClass[0]);
    }

    /**
     * Returns the executable, depending on whether method ref is method or constructor.
     */
    public JavaExecutable getExecutable()
    {
        if (getType() == Type.Constructor)
            return getConstructor();
        return getMethod();
    }

    /**
     * Returns the resolved lambda method return type.
     */
    @Override
    public JavaType getLambdaMethodReturnTypeResolved()
    {
        // If method is set, return its eval type
        JavaMethod method = getMethod();
        if (method != null)
            return method.getReturnType();

        // If Constructor is set, return its type
        JavaConstructor constructor = getConstructor();
        if (constructor != null)
            return constructor.getDeclaringClass();

        // Do normal version
        return super.getLambdaMethodReturnTypeResolved();
    }

    /**
     * Returns the JavaDecl most closely associated with given child JExprId node.
     */
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If given id is MethodId, return method
        if (anExprId == _methodId)
            return getExecutable();

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Override to customize for MethodRef.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If prefix expression has errors, return them
        NodeError[] prefixExprErrors = _prefixExpr.getErrors();
        if (prefixExprErrors.length > 0)
            return prefixExprErrors;

        // If no MethodId expression has errors, return them
        if (_methodId == null)
            return  NodeError.newErrorArray(this, "Method reference method name not specified");

        // If MethodId has errors, return them
        NodeError[] methodIdErrors = _methodId.getErrors();
        if (methodIdErrors.length > 0)
            return methodIdErrors;

        // Return no errors
        return super.getErrorsImpl();
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "MethodRef"; }
}
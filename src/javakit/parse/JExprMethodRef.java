/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ListUtils;
import java.util.List;

/**
 * This JExpr subclass represents a method reference: obj::method.
 */
public class JExprMethodRef extends JExpr {

    // The prefix expression
    private JExpr _prefixExpr;

    // The method name identifier
    private JExprId _methodId;

    // The type of method ref
    private Type _type;

    // The method
    private JavaMethod _method;

    // The type for this lambda
    private JavaType _lambdaType;

    // The actual interface method this lambda represents
    private JavaMethod _lambdaMethod;

    // A constant to define the types of method refs.
    // See https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html
    public enum Type { InstanceMethod, StaticMethod, HelperMethod, Constructor, Unknown };

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
     * Returns the resolved lambda method parameter types.
     */
    public JavaClass[] getLambdaMethodParameterTypesResolved()
    {
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;
        JavaType[] paramTypes = lambdaMethod.getParameterTypes();
        JavaClass[] paramClasses = new JavaClass[paramTypes.length];

        // Get node to resolve any unresolved types
        JNode resolveNode = getParent(JExprMethodCall.class);
        if (resolveNode == null)
            resolveNode = this;

        // Iterate over parameter types
        for (int i = 0; i < paramTypes.length; i++) {
            JavaType paramType = paramTypes[i];
            if (!paramType.isResolvedType())
                paramType = resolveNode.getResolvedTypeForType(paramType);
            paramClasses[i] = paramType != null ? paramType.getEvalClass() : null;
        }

        // Return
        return paramClasses;
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

        // Handle parent is JVarDecl, JExprCast, JExprAssign: Return parent eval type
        if (parentNode instanceof JVarDecl || parentNode instanceof JExprCast || parentNode instanceof JExprAssign) {
            JavaType lambdaType = parentNode.getEvalType();
            if (lambdaType != null)
                return lambdaType;
        }

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
     * Override to return Method.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        return getLambdaMethod();
    }

    /**
     * Override to get eval type from expression if possible.
     */
    @Override
    protected JavaType getEvalTypeImpl()
    {
        // If parent is variable declaration, return its type
        JNode parentNode = getParent();
        if (parentNode instanceof JVarDecl)
            return parentNode.getEvalType();

        // If method is found, return its eval type
        JavaMethod method = getMethod();
        if (method != null) {
            JavaType evalType = method.getEvalType();
            if (evalType != null && !evalType.isResolvedType()) {
                JavaType resolvedType = getResolvedTypeForType(evalType);
                evalType = resolvedType != null ? resolvedType : evalType.getEvalClass();
            }
            return evalType;
        }

        // Do normal version
        return super.getEvalTypeImpl();
    }

    /**
     * Returns the JavaDecl most closely associated with given child JExprId node.
     */
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If given id is MethodId, return method
        if (anExprId == _methodId)
            return getMethod();

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
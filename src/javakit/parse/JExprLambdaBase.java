/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * A JExpr to represent lambda expressions.
 */
public abstract class JExprLambdaBase extends JExpr {

    // The type for this lambda
    private JavaType _lambdaType;

    // The actual interface method this lambda represents
    private JavaMethod _lambdaMethod;

    /**
     * Constructor.
     */
    public JExprLambdaBase()
    {
        super();
    }

    /**
     * Override to try to resolve decl from parent.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // If parent is variable declaration, return its type
        JNode parentNode = getParent();
        if (parentNode instanceof JVarDecl)
            return parentNode.getEvalType();

        // Return lambda type
        return getLambdaType();
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
            JExprMethodCall methodCallExpr = (JExprMethodCall) parentNode;
            JavaMethod method = methodCallExpr.getMethod();
            if (method == null)
                return null;

            // Get arg index of this lambda expr
            JExpr[] args = methodCallExpr.getArgs();
            int argIndex = ArrayUtils.indexOfId(args, this);
            if (argIndex < 0) { System.err.println("JExprLambdaBase.getLambdaTypeImpl: Can't happen 1"); return null; }
            return method.getGenericParameterType(argIndex);
        }

        // Handle parent is alloc expression: Get lambda interface from alloc expression param
        if (parentNode instanceof JExprAlloc) {

            // Get alloc expr constructor
            JExprAlloc allocExpr = (JExprAlloc) parentNode;
            JavaConstructor constructor = allocExpr.getConstructor();
            if (constructor == null)
                return null;

            // Get arg index of this lambda expr
            JExpr[] args = allocExpr.getArgs();
            int argIndex = ArrayUtils.indexOfId(args, this);
            if (argIndex < 0) { System.err.println("JExprLambdaBase.getLambdaTypeImpl: Can't happen 2"); return null; }
            return constructor.getGenericParameterType(argIndex);
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
     * Returns the resolved lambda method parameter types.
     */
    public JavaClass[] getLambdaMethodParameterTypesResolved()
    {
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;
        JavaType[] paramTypes = lambdaMethod.getGenericParameterTypes();
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
     * Returns the resolved lambda method return type.
     */
    public JavaType getLambdaMethodReturnTypeResolved()
    {
        // Return lambda method eval type - this is a fallback and isn't any help if method return type is type var
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod != null)
            return lambdaMethod.getReturnType();

        // Return not found
        return null;
    }

    /**
     * Override to try to resolve with lambda type.
     */
    @Override
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        JavaMethod lambdaMethod = getLambdaMethod();
        JavaType lambdaMethodReturnType = lambdaMethod != null ? lambdaMethod.getGenericReturnType() : null;

        // If type var matches lambda method return type, return resolved version. This needs to be fixed to support param types
        if (lambdaMethodReturnType instanceof JavaTypeVariable) {
            JavaTypeVariable lambdaMethodTypeVar = (JavaTypeVariable) lambdaMethodReturnType;
            if (aTypeVar.getName().equals(lambdaMethodTypeVar.getName()))
                return getLambdaMethodReturnTypeResolved();
        }

        // Do normal version
        return super.getResolvedTypeForTypeVar(aTypeVar);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "LambdaExpr"; }

    /**
     * Returns the node errors.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();

        // Handle missing class
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            errors = NodeError.addError(errors, this, "Can't resolve lambda method", 0);

        // Return
        return errors;
    }
}
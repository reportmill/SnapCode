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
    public JavaType[] getLambdaMethodParameterTypes()
    {
        // Get lambda method - just return if not found
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;

        // Get parameter types
        JavaType[] paramTypes = lambdaMethod.getGenericParameterTypes();

        // If lambda method from subclass of lambda type, translate types to lambda type
        if (lambdaMethod.getDeclaringClass() != getLambdaClass()) {
            JavaClass lambdaMethodClass = lambdaMethod.getDeclaringClass();
            JavaType lambdaType = getLambdaType();
            paramTypes = JavaTypeUtils.translateParamTypesToSubclass(paramTypes, lambdaMethodClass, lambdaType);
        }

        // Get parameter types and return resolved types
        return ArrayUtils.map(paramTypes, type -> getLambdaMethodParameterType(type), JavaType.class);
    }

    /**
     * Returns the given parameter type resolved.
     */
    private JavaType getLambdaMethodParameterType(JavaType paramType)
    {
        // If already resolved, just return class
        if (paramType.isResolvedType())
            return paramType;

        // If lambda type not parameterized type, just return class
        if (!(getLambdaType() instanceof JavaParameterizedType))
            return paramType;

        // Get lambda type generic and resolved types
        JavaParameterizedType lambdaType = (JavaParameterizedType) getLambdaType();
        JavaClass lambdaClass = lambdaType.getRawType();
        JavaType[] genericTypes = lambdaClass.getTypeVars();
        JavaType[] resolvedTypes = lambdaType.getParamTypes();

        // Try to resolve type from lambda generic and resolved types
        JavaType resolvedType = JavaTypeUtils.getResolvedTypeForTypeArrays(paramType, genericTypes, resolvedTypes);
        return resolvedType;
    }

    /**
     * Returns the resolved lambda method parameter types.
     */
    protected JavaType[] getLambdaMethodParameterTypesResolved()
    {
        JavaType[] paramTypes = getLambdaMethodParameterTypes();
        return paramTypes != null ? ArrayUtils.map(paramTypes, type -> getResolvedTypeForType(type), JavaType.class) : null;
    }

    /**
     * Override to return lambda type.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        return getLambdaType();
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
        if (lambdaMethod == null) {
            if (!(this instanceof JExprMethodRef) || ((JExprMethodRef) this).getConstructor() == null)
                errors = NodeError.addError(errors, this, "Can't resolve lambda method", 0);
        }

        // Return
        return errors;
    }
}
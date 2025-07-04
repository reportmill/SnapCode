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
        // Get Parent
        JNode parentNode = getParent();

        // Handle parent is WithArgs (JExprMethodCall, JExprAlloc): Get lambda interface from executable call decl param
        if (parentNode instanceof WithArgs withArgsNode) {

            // Get WithArgs node and method/constructor
            JavaExecutable method = withArgsNode.getExecutable();
            if (method == null)
                return null;

            // Get arg index of this lambda expr
            JExpr[] args = withArgsNode.getArgs();
            int argIndex = ArrayUtils.indexOfId(args, this);
            if (argIndex < 0) { System.err.println("JExprLambdaBase.getLambdaTypeImpl: Can't happen"); return null; }
            return method.getGenericParameterType(argIndex);
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

        // Get parameter types and return as LambdaType types
        JavaType[] paramTypes = lambdaMethod.getGenericParameterTypes();
        return ArrayUtils.map(paramTypes, type -> translateLambdaMethodParameterTypeToLambdaType(type), JavaType.class);
    }

    /**
     * Returns the resolved lambda method return type.
     */
    public JavaType getLambdaMethodReturnType()
    {
        // Get lambda method - just return if not found
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;

        // Get return type and return as LambdaType type
        JavaType returnType = lambdaMethod.getGenericReturnType();
        return translateLambdaMethodParameterTypeToLambdaType(returnType);
    }

    /**
     * Returns the given lambda method parameter/return type translated to LambdaType.
     */
    private JavaType translateLambdaMethodParameterTypeToLambdaType(JavaType paramType)
    {
        // If already resolved, just return type
        if (paramType.isResolvedType())
            return paramType;

        // If lambda type not parameterized type, just return type
        if (!(getLambdaType() instanceof JavaParameterizedType lambdaType))
            return paramType;

        // Get types
        JavaClass lambdaClass = lambdaType.getRawType();
        JavaMethod lambdaMethod = getLambdaMethod();

        // If lambda method from subclass of lambda type, translate return type to lambda type
        if (lambdaMethod.getDeclaringClass() != lambdaClass) {
            JavaClass lambdaMethodClass = lambdaMethod.getDeclaringClass();
            paramType = JavaTypeUtils.translateParamTypeToSubclass(paramType, lambdaMethodClass, lambdaType);
            if (paramType.isResolvedType())
                return paramType;
        }

        // Get lambda type generic and resolved types
        JavaType[] genericTypes = lambdaClass.getTypeParameters();
        JavaType[] resolvedTypes = lambdaType.getParamTypes();

        // Try to resolve type from lambda generic and resolved types
        return JavaTypeUtils.getResolvedTypeForTypeArrays(paramType, genericTypes, resolvedTypes);
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
     * Returns the LambdaType return type by evaluating lambda expression or method ref method.
     */
    protected JavaType getLambdaReturnType()
    {
        // Handle Lambda
        if (this instanceof JExprLambda) {
            JExpr expr = ((JExprLambda) this).getExpr();
            return expr != null ? expr.getEvalType() : null;
        }

        return null;
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
     * Override to prevent infinite loop.
     */
    @Override
    public JavaType getEvalTypeImpl()
    {
        if (_resolvingEvalType)
            return null;

        // Do normal version without reentry for args
        _resolvingEvalType = true;
        JavaType evalType = super.getEvalTypeImpl();
        _resolvingEvalType = false;
        return evalType;
    }

    // Whether resolving eval type
    private boolean _resolvingEvalType;

    /**
     * Override to try to resolve return type.
     */
    @Override
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // If type var is return type only, try to resolve with lambda expression type or method ref method return type
        if (isTypeVarInReturnTypeOnly(aTypeVar)) {

            // Get generic return type and resolved return type and try to resolve
            JavaType lambdaMethodReturnType = getLambdaMethodReturnType();
            JavaType lambdaReturnType = getLambdaReturnType();
            if (lambdaReturnType != null) {
                JavaType resolvedType = JavaTypeUtils.getResolvedTypeVariableForTypes(aTypeVar, lambdaMethodReturnType, lambdaReturnType);
                if (resolvedType != aTypeVar)
                    return resolvedType;
            }

            // If resolve failed, return Object
            return aTypeVar.getEvalType(); //getJavaClassForName("java.lang.Object");
        }

        // Do normal version
        return super.getResolvedTypeForTypeVar(aTypeVar);
    }

    /**
     * Returns whether given type var shows up in lambda method return type but not in parameters.
     */
    private boolean isTypeVarInReturnTypeOnly(JavaTypeVariable aTypeVar)
    {
        // If not in return type, return false
        JavaType lambdaMethodReturnType = getLambdaMethodReturnType();
        if (lambdaMethodReturnType == null || !lambdaMethodReturnType.hasTypeVar(aTypeVar))
            return false;

        // If in parameter types, return false
        JavaType[] lambdaMethodParamTypes = getLambdaMethodParameterTypes();
        return !ArrayUtils.hasMatch(lambdaMethodParamTypes, type -> type.hasTypeVar(aTypeVar));
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "LambdaExpr"; }
}
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * Utility methods to resolve an id to declaration.
 */
class ResolveType {

    /**
     * Returns a resolved type for given type.
     */
    public static JavaType getResolvedTypeForType(JNode aNode, JavaType aType)
    {
        // Handle TypeVar
        if (aType instanceof JavaTypeVariable typeVar)
            return getResolvedTypeForTypeVar(aNode, typeVar);

            // Handle ParameterizedType
        else if (aType instanceof JavaParameterizedType parameterizedType) {

            // Get parameter types
            JavaType[] paramTypes = parameterizedType.getParamTypes();
            JavaType[] paramTypesResolved = paramTypes.clone();
            boolean didResolve = false;

            // Iterate over each and resolve if needed
            for (int i = 0; i < paramTypes.length; i++) {
                JavaType paramType = paramTypes[i];
                if (!paramType.isResolvedType()) {
                    JavaType paramTypeResolved = getResolvedTypeForType(aNode, paramType);
                    if (paramTypeResolved != paramType) {
                        paramTypesResolved[i] = paramTypeResolved;
                        didResolve = true;
                    }
                }
            }

            // If something was resolved, create new type with resolved parameter types
            if (didResolve) {
                JavaClass rawType = parameterizedType.getRawType();
                return rawType.getParameterizedTypeForTypes(paramTypesResolved);
            }
        }

        // Handle Generic array type
        else if (aType instanceof JavaGenericArrayType arrayType) {
            JavaType compType = arrayType.getComponentType();
            JavaType compTypeResolved = getResolvedTypeForType(aNode, compType);
            if (compTypeResolved != compType)
                return compTypeResolved.getArrayType();
        }

        // Return
        return aType;
    }

    /**
     * Returns a resolved type for given type.
     */
    private static JavaType getResolvedTypeForTypeVar(JNode aNode, JavaTypeVariable aTypeVar)
    {
        switch (aNode) {

            case JClassDecl classDecl -> {

                // If this class is subclass of parameterized type with given type var, return resolved type
                JavaClass javaClass = classDecl.getJavaClass();
                if (javaClass != null) {
                    JavaType resolvedType = javaClass.getResolvedTypeForTypeVariable(aTypeVar);
                    if (resolvedType != null)
                        return resolvedType;
                }
            }

            case JExprLambdaBase lambdaBase -> {

                // If type var is return type only, try to resolve with lambda expression type or method ref method return type
                if (isTypeVarInReturnTypeOnly(lambdaBase, aTypeVar)) {

                    // Get generic return type and resolved return type and try to resolve
                    JavaType lambdaMethodReturnType = lambdaBase.getLambdaMethodReturnType();
                    JavaType lambdaReturnType = lambdaBase.getLambdaReturnType();
                    if (lambdaReturnType != null) {
                        JavaType resolvedType = JavaTypeUtils.getResolvedTypeVariableForTypes(aTypeVar, lambdaMethodReturnType, lambdaReturnType);
                        if (resolvedType != aTypeVar)
                            return resolvedType;
                    }

                    // If resolve failed, return Object
                    return aTypeVar.getEvalType(); //getJavaClassForName("java.lang.Object");
                }
            }

            case JExprMethodCall methodCall -> {

                // Try to resolve from method types
                JavaType methodResolvedType = getResolvedTypeForTypeVarFromMethodTypes(methodCall, aTypeVar);
                if (methodResolvedType != aTypeVar) {
                    if (methodResolvedType.isResolvedType())
                        return methodResolvedType;
                    return getResolvedTypeForType(methodCall, methodResolvedType);
                }

                // Try to resolve from ScopeNode.Type
                JavaType scopeType = methodCall.getScopeEvalType();
                JavaType scopeResolvedType = scopeType != null ? scopeType.getResolvedTypeForTypeVariable(aTypeVar) : null;
                if (scopeResolvedType != null && scopeResolvedType.isResolvedType())
                    return scopeResolvedType;
            }

            case JVarDecl varDecl -> {

                // If VarDecl type is parameterized type, try to resolve given type var
                JavaType javaType = varDecl.getJavaType();
                if (javaType != null) {
                    JavaType resolvedType = javaType.getResolvedTypeForTypeVariable(aTypeVar);
                    if (resolvedType != null)
                        return resolvedType;
                }
            }

            default -> { }
        }

        // Get parent to resolve type (skip method calls - since args aren't defined in those terms)
        JNode parent = aNode.getParent();
        while (parent instanceof JExprMethodCall && !(aNode instanceof JExprLambdaBase))
            parent = parent.getParent();
        if (parent != null)
            return getResolvedTypeForTypeVar(parent, aTypeVar);

        // Since type var not resolved, return bounds type
        return aTypeVar.getEvalType();
    }


    /**
     * Returns whether given type var shows up in lambda method return type but not in parameters.
     */
    private static boolean isTypeVarInReturnTypeOnly(JExprLambdaBase lambdaBase, JavaTypeVariable aTypeVar)
    {
        // If not in return type, return false
        JavaType lambdaMethodReturnType = lambdaBase.getLambdaMethodReturnType();
        if (lambdaMethodReturnType == null || !lambdaMethodReturnType.hasTypeVar(aTypeVar))
            return false;

        // If in parameter types, return false
        JavaType[] lambdaMethodParamTypes = lambdaBase.getLambdaMethodParameterTypes();
        return !ArrayUtils.hasMatch(lambdaMethodParamTypes, type -> type.hasTypeVar(aTypeVar));
    }

    /**
     * Returns a resolved type for given type variable.
     */
    private static JavaType getResolvedTypeForTypeVarFromMethodTypes(JExprMethodCall methodCall, JavaTypeVariable aTypeVar)
    {
        // Get method (just return if not found or doesn't have type var
        JavaMethod method = methodCall.getMethod();
        if (method == null || method.getTypeParameterForName(aTypeVar.getName()) == null)
            return aTypeVar;

        // Get method parameter types and arg types
        JExpr[] methodArgs = methodCall.getArgs();
        JavaType[] argTypes = ArrayUtils.map(methodArgs, arg -> arg.getEvalType(), JavaType.class);

        // Forward to getResolvedTypeVariableForTypeArrays()
        return JavaTypeUtils.getResolvedTypeForTypeVarAndMethodAndArgTypes(aTypeVar, method, argTypes);
    }
}

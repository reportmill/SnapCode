/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class represents a method call in code.
 */
public class JExprMethodCall extends JExpr implements WithId {

    // The identifier
    private JExprId _id;

    // The args
    private JExpr[] _args = JExpr.EMPTY_EXPR_ARRAY;

    // The method
    private JavaMethod _method;

    /**
     * Constructor.
     */
    public JExprMethodCall()
    {
        super();
    }

    /**
     * Constructor for given identifier (method name) and arg list.
     */
    public JExprMethodCall(JExprId anId, JExpr[] theArgs)
    {
        setId(anId);
        setArgs(theArgs);
    }

    /**
     * Returns the identifier.
     */
    @Override
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        if (_id == null)
            addChild(_id = anId, 0);
        else replaceChild(_id, _id = anId);
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Returns the number of arguments.
     */
    public int getArgCount()  { return _args.length; }

    /**
     * Returns the individual argument at index.
     */
    public JExpr getArg(int anIndex)  { return _args[anIndex]; }

    /**
     * Returns the method arguments.
     */
    public JExpr[] getArgs()  { return _args; }

    /**
     * Sets the method arguments.
     */
    public void setArgs(JExpr[] theArgs)
    {
        _args = theArgs;
        Stream.of(_args).forEach(this::addChild);
    }

    /**
     * Returns the method.
     */
    public JavaMethod getMethod()
    {
        if (_method != null) return _method;
        return _method = getMethodImpl();
    }

    /**
     * Returns the method.
     */
    private JavaMethod getMethodImpl()
    {
        // Get compatible methods for name and arg types
        List<JavaMethod> compatibleMethods = getCompatibleMethods();
        if (compatibleMethods == null || compatibleMethods.isEmpty())
            return null;

        // Get arg index of lambda expression
        if (ArrayUtils.hasMatch(_args, arg -> arg instanceof JExprLambdaBase))
            return getMethodForLambdaArgs(compatibleMethods);

        // Return first method
        return compatibleMethods.get(0);
    }

    /**
     * Returns the method for method call with lambda arg(s).
     */
    private JavaMethod getMethodForLambdaArgs(List<JavaMethod> compatibleMethods)
    {
        // Get arg index of lambda expression
        int lambdaArgIndex = ArrayUtils.findMatchIndex(_args, arg -> arg instanceof JExprLambdaBase);

        // Iterate over compatible methods and return first that matches arg count
        for (JavaMethod compatibleMethod : compatibleMethods) {

            // Get parameter type and look for lambda method - return if found
            JavaType paramType = compatibleMethod.getGenericParameterType(lambdaArgIndex);
            JavaClass paramClass = paramType.getEvalClass();
            JavaMethod lambdaMethod = paramClass.getLambdaMethod();
            if (lambdaMethod != null)
                return compatibleMethod;
        }

        // Return first method
        return compatibleMethods.get(0);
    }

    /**
     * Returns the list of compatible methods for method call target and args.
     */
    private List<JavaMethod> getCompatibleMethods()
    {
        // Get scope class and search for compatible method for name and arg types
        JavaType scopeEvalType = getScopeEvalType();
        JavaClass scopeClass = scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get arg classes
        JavaClass[] argClasses = ArrayUtils.map(_args, arg -> arg instanceof JExprLambdaBase ? null : arg.getEvalClass(), JavaClass.class);

        // Find compatible methods for class and arg classes
        for (int i = 0; i < 4; i++) {

            // Find compatible methods for class and arg classes and return if found
            List<JavaMethod> compatibleMethods = getCompatibleMethodsForScopeClassAndArgClasses(scopeClass, argClasses);
            if (!compatibleMethods.isEmpty())
                return compatibleMethods;

            // Try adding a null arg (maybe user is missing or typing args)
            argClasses = ArrayUtils.add(argClasses, null);
        }

        // Return not found
        return null;
    }

    /**
     * Returns the method decl for the parent method call (assumes this lambda is an arg).
     */
    private List<JavaMethod> getCompatibleMethodsForScopeClassAndArgClasses(JavaClass scopeClass, JavaClass[] argClasses)
    {
        // Get method name and whether to only search static methods (scope expression is Class)
        String methodName = getName();
        JExpr scopeExpr = getScopeExpr();
        boolean staticOnly = scopeExpr != null && scopeExpr.isClassNameLiteral();

        // Get scope node class type and search for compatible method for name and arg types
        List<JavaMethod> compatibleMethods = JavaClassUtils.getCompatibleMethods(scopeClass, methodName, argClasses, staticOnly);
        if (!compatibleMethods.isEmpty())
            return compatibleMethods;

        // If scope expression is present, just return (can't be method from enclosing class or static import)
        if (scopeExpr != null)
            return Collections.EMPTY_LIST;

        // If scope node class type is member class and not static, go up parent classes
        while (scopeClass.isMemberClass() && !scopeClass.isStatic()) {
            scopeClass = scopeClass.getDeclaringClass();
            compatibleMethods = JavaClassUtils.getCompatibleMethods(scopeClass, methodName, argClasses, false);
            if (!compatibleMethods.isEmpty())
                return compatibleMethods;
        }

        // See if method is from static import -
        JFile jfile = getFile();
        JavaMember importClassMember = jfile.getStaticImportMemberForNameAndParamTypes(methodName, argClasses);
        if (importClassMember instanceof JavaMethod)
            return Collections.singletonList((JavaMethod) importClassMember);

        // Return
        return compatibleMethods;
    }

    /**
     * Override to return method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

    /**
     * Returns a resolved type for given type.
     */
    @Override
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // Try to resolve from method types
        JavaType methodResolvedType = getResolvedTypeForTypeVarFromMethodTypes(aTypeVar);
        if (methodResolvedType != null) {
            if (methodResolvedType.isResolvedType())
                return methodResolvedType;
            return getResolvedTypeForType(methodResolvedType);
        }

        // Try to resolve from ScopeNode.Type
        JavaType scopeType = getScopeEvalType();
        JavaType scopeResolvedType = scopeType != null ? scopeType.getResolvedTypeForTypeVariable(aTypeVar) : null;
        if (scopeResolvedType != null)
                return scopeResolvedType;

        // Do normal version
        return super.getResolvedTypeForTypeVar(aTypeVar);
    }

    /**
     * Returns a resolved type for given type.
     */
    private JavaType getResolvedTypeForTypeVarFromMethodTypes(JavaTypeVariable aTypeVar)
    {
        // Get method (just return if not found or doesn't have type var
        JavaMethod method = getMethod();
        if (method == null || method.getTypeVarForName(aTypeVar.getName()) == null)
            return null;

        // Get method parameter types and arg types
        JavaType[] paramTypes = method.getGenericParameterTypes();
        JavaType[] argTypes = ArrayUtils.map(_args, arg -> arg instanceof JExprLambdaBase ? null : arg.getEvalType(), JavaType.class);

        // Forward to getResolvedTypeVariableForTypeArrays()
        return getResolvedTypeVariableForTypeArrays(aTypeVar, paramTypes, argTypes);
    }

    /**
     * Returns the resolved type for given type variable and array of generic types and array of resolved types.
     */
    private static JavaType getResolvedTypeVariableForTypeArrays(JavaTypeVariable aTypeVar, JavaType[] paramTypes, JavaType[] argTypes)
    {
        // Iterate over method parameter types to see if any can resolve the type var name
        for (int i = 0; i < paramTypes.length; i++) {

            // If paramType doesn't reference type var, just continue
            JavaType paramType = paramTypes[i];
            if (!paramType.hasTypeVar(aTypeVar))
                continue;

            // Get arg type
            JavaType argType = argTypes[i];
            if (argType != null) {
                JavaType resolvedType = getResolvedTypeVariableForTypes(aTypeVar, paramType, argType);
                if (resolvedType != null)
                    return resolvedType;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns resolved type for given type variable, given a generic type and a resolved type.
     * Returns null if generic type doesn't reference type variable.
     */
    private static JavaType getResolvedTypeVariableForTypes(JavaTypeVariable aTypeVar, JavaType paramType, JavaType argType)
    {
        // Handle TypeVar: If name matches, return arg type
        if (paramType instanceof JavaTypeVariable) {
            JavaTypeVariable paramTypeVar = (JavaTypeVariable) paramType;
            if (paramTypeVar.getName().equals(aTypeVar.getName()))
                return argType;
            return null;
        }

        // Handle Parameterized type
        if (paramType instanceof JavaParameterizedType) {

            // Get types as JavaParameterized types
            JavaParameterizedType paramParamType = (JavaParameterizedType) paramType;
            JavaParameterizedType argParamType = argType instanceof JavaParameterizedType ? (JavaParameterizedType) argType : null;
            if (argParamType == null) {
                System.err.println("JExprMethodCall.getResolvedTypeVariableForTypes: arg type not parameterized type");
                return null;
            }

            // Get arrays of parameter types
            JavaType[] paramParamTypes = paramParamType.getParamTypes();
            JavaType[] argParamTypes = argParamType.getParamTypes();
            if (paramParamTypes.length != argParamTypes.length) {
                System.err.println("JExprMethodCall.getResolvedTypeVariableForTypes: param types length mismatch");
                return null;
            }

            // Forward to type arrays version
            return getResolvedTypeVariableForTypeArrays(aTypeVar, paramParamTypes, argParamTypes);
        }

        // Handle array type: Get component types and recurse
        if (paramType instanceof JavaGenericArrayType) {
            JavaType paramCompType = paramType.getComponentType();
            JavaType argCompType = argType.getComponentType();
            return getResolvedTypeVariableForTypes(aTypeVar, paramCompType, argCompType);
        }

        // Complain and return
        System.err.println("JExprMethodCall.getResolvedTypeVariableForTypes: Unsupported type: " + paramType);
        return null;
    }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If any arg errors, return them
        for (JExpr arg : _args) {
            NodeError[] argErrors = arg.getErrors();
            if (argErrors.length > 0)
                return argErrors;
        }

        // Handle can't resolve method
        JavaMethod method = getMethod();
        if (method == null) {

            // If no method exists for name, return can't resolve method name
            boolean hasAnyMethodForName = getMethodAny() != null;
            String methodString = getMethodString(hasAnyMethodForName);
            return NodeError.newErrorArray(this, "Can't find method: " + methodString);
        }

        // If missing args, complain
        int paramCount = method.getParameterCount();
        int argCount = getArgCount();
        if (paramCount > argCount && !method.isVarArgs())
            return NodeError.newErrorArray(this, "Missing args, " + paramCount + " expected, " + argCount + " provided");

        // Return
        return NodeError.NO_ERRORS;
    }

    /**
     * Looks to see if there is any method for given name.
     */
    protected JavaMethod getMethodAny()
    {
        // Get scope node class
        String name = getName();
        JavaType scopeEvalType = getScopeEvalType();
        JavaClass scopeClass = scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get whether to only search static methods (scope expression is Class)
        JExpr scopeExpr = getScopeExpr();
        boolean staticOnly = scopeExpr != null && scopeExpr.isClassNameLiteral();

        // Search for compatible method for name and arg types
        return JavaClassUtils.getCompatibleMethod(scopeClass, name, null, staticOnly);
    }

    /**
     * Returns a string for method.
     */
    private String getMethodString(boolean withArgs)
    {
        // Get method name and arg types
        String methodName = getName();
        if (methodName == null)
            return "No name found";
        if (!withArgs)
            return methodName + "()";
        String argTypesString = getArgTypesString();
        String methodString = methodName + argTypesString;

        // Get scope node class name
        JavaDecl scopeEvalType = getScopeEvalType();
        String scopeClassName = scopeEvalType != null ? scopeEvalType.getEvalClassName() : null;
        if (scopeClassName != null)
            methodString = scopeClassName + '.' + methodString;

        // Return
        return methodString;
    }

    /**
     * Returns the parameter string.
     */
    private String getArgTypesString()
    {
        JavaType[] argTypes = ArrayUtils.map(_args, expr -> expr != null ? expr.getEvalType() : null, JavaType.class);
        String argTypeString = ArrayUtils.mapToStringsAndJoin(argTypes, type -> type != null ? type.getSimpleName() : "null", ",");
        return '(' + argTypeString + ')';
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodCall"; }
}
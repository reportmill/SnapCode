/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.StringUtils;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a method call in code.
 */
public class JExprMethodCall extends JExpr implements WithId {

    // The identifier
    private JExprId _id;

    // The args
    private List<JExpr> _args;

    // The param types
    private JavaType[] _paramTypes;

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
    public JExprMethodCall(JExprId anId, List<JExpr> theArgs)
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
        if (_id == null) addChild(_id = anId, 0);
        else replaceChild(_id, _id = anId);
        if (_id != null) setName(_id.getName());
    }

    /**
     * Returns the number of arguments.
     */
    public int getArgCount()  { return _args.size(); }

    /**
     * Returns the individual argument at index.
     */
    public JExpr getArg(int anIndex)  { return _args.get(anIndex); }

    /**
     * Returns the method arguments.
     */
    public List<JExpr> getArgs()  { return _args; }

    /**
     * Sets the method arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        if (_args != null)
            for (JExpr arg : _args)
                removeChild(arg);

        _args = theArgs;

        if (_args != null)
            for (JExpr arg : _args)
                addChild(arg, -1);
    }

    /**
     * Returns the arg eval types.
     */
    public JavaType[] getArgEvalTypes()
    {
        // If already set, just return
        if (_paramTypes != null) return _paramTypes;

        // Get param expressions and create array for types
        List<JExpr> paramExprs = getArgs();
        JavaType[] paramTypes = new JavaType[paramExprs.size()];

        // Iterate over expressions and evaluate to type
        for (int i = 0, iMax = paramExprs.size(); i < iMax; i++) {
            JExpr arg = paramExprs.get(i);
            paramTypes[i] = arg != null ? arg.getEvalType() : null;
        }

        // Set/Return
        return _paramTypes = paramTypes;
    }

    /**
     * Override to return as JavaMethod.
     */
    @Override
    public JavaMethod getDecl()  { return (JavaMethod) super.getDecl(); }

    /**
     * Override to return method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

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
        // Get scope node class
        JavaType scopeEvalType = getScopeEvalType();
        JavaClass scopeClass = scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get method name and arg types
        String name = getName();
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaClass[] argClasses = new JavaClass[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            if (arg instanceof JExprLambda || arg instanceof JExprMethodRef)
                return getMethodForLambdaArgs();
            JavaClass argClass = arg != null ? arg.getEvalClass() : null;
            argClasses[i] = argClass;
        }

        // Get whether to only search static methods (scope expression is Class)
        JExpr scopeExpr = getScopeExpr();
        boolean staticOnly = scopeExpr != null && scopeExpr.isClassNameLiteral();

        // Search for compatible method for name and arg types
        JavaMethod method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argClasses, staticOnly);
        if (method != null)
            return method;

        // If scope expression is present, just return
        if (scopeExpr != null)
            return null;

        // Get enclosing class
        JClassDecl enclosingClass = getEnclosingClassDecl();

        // If scope node is class and not static, go up parent classes
        while (enclosingClass != null && !scopeClass.isStatic()) {

            // Get enclosing JClassDecl and JavaClass
            enclosingClass = enclosingClass.getEnclosingClassDecl();
            scopeClass = enclosingClass != null ? enclosingClass.getEvalClass() : null;
            if (scopeClass == null)
                break;

            // If method found, return it
            method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argClasses, false);
            if (method != null)
                return method;
        }

        // See if method is from static import
        JFile jfile = getFile();
        JavaMember importClassMember = jfile.getStaticImportMemberForNameAndParamTypes(name, argClasses);
        if (importClassMember instanceof JavaMethod)
            return (JavaMethod) importClassMember;

        // Return not found
        return null;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaMethod getMethodForLambdaArgs()
    {
        // Get matching methods
        List<JavaMethod> methods = getCompatibleMethods();
        if (methods == null || methods.size() == 0)
            return null;

        // Get arg index of lambda expression
        List<JExpr> args = getArgs();
        int argIndex = ListUtils.findMatchIndex(args, arg -> arg instanceof JExprLambda || arg instanceof JExprMethodRef);
        if (argIndex < 0)
            return null;

        // Iterate over methods and return first that matches arg count
        for (JavaMethod method : methods) {
            JavaType paramType = method.getParameterType(argIndex);
            JavaClass paramClass = paramType.getEvalClass();
            JavaMethod lambdaMethod = paramClass.getLambdaMethod();
            if (lambdaMethod != null)
                return method;
        }

        // Otherwise, let's just return first matching method (maybe TeaVM thing)
        if (methods.size() > 0)
            return methods.get(0);

        // Return not found
        return null;
    }

    /**
     * Returns the method decl for the parent method call (assumes this lambda is an arg).
     */
    protected List<JavaMethod> getCompatibleMethods()
    {
        // Get scope class and search for compatible method for name and arg types
        JavaType scopeEvalType = getScopeEvalType();
        JavaClass scopeClass = scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get method name and arg types
        String name = getName();
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaClass[] argClasses = new JavaClass[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            JavaClass argType = arg instanceof JExprLambda || arg instanceof JExprMethodRef ? null : arg.getEvalClass();
            argClasses[i] = argType;
        }

        // Get whether to only search static methods (scope expression is Class)
        JExpr scopeExpr = getScopeExpr();
        boolean staticOnly = scopeExpr != null && scopeExpr.isClassNameLiteral();

        // Get scope node class type and search for compatible method for name and arg types
        List<JavaMethod> compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argClasses, staticOnly);
        if (compatibleMethods.size() > 0)
            return compatibleMethods;

        // If scope node class type is member class and not static, go up parent classes
        while (scopeClass.isMemberClass() && !scopeClass.isStatic()) {
            scopeClass = scopeClass.getDeclaringClass();
            compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argClasses, staticOnly);
            if (compatibleMethods.size() > 0)
                return compatibleMethods;
        }

        // See if method is from static import -
        JFile jfile = getFile();
        JavaMember importClassMember = jfile.getStaticImportMemberForNameAndParamTypes(name, argClasses);
        if (importClassMember instanceof JavaMethod)
            return Collections.singletonList((JavaMethod) importClassMember);

        // Return
        return compatibleMethods;
    }

    /**
     * Returns a resolved type for given type.
     */
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // Get type
        JavaType resolvedType = aTypeVar;

        // Get name and method
        JavaMethod method = getDecl();

        // Get whether method has this type var
        String typeVarName = aTypeVar.getName();
        boolean methodHasTypeVar = method != null && method.getTypeVarForName(typeVarName) != null;

        // See if TypeVar can be resolved by method
        if (methodHasTypeVar) {
            JavaType resType = getResolvedTypeVarForMethod(aTypeVar, method);
            if (resType != null)
                resolvedType = resType;
        }

        // See if TypeVar can be resolved by ScopeNode.Type
        else {
            JavaType scopeType = getScopeEvalType();
            JavaType resolvedDecl = scopeType != null ? scopeType.getResolvedType(resolvedType) : null;
            if (resolvedDecl != null)
                resolvedType = resolvedDecl;
        }

        // If still not resolved, do normal version
        if (!resolvedType.isResolvedType())
            resolvedType = super.getResolvedTypeForTypeVar((JavaTypeVariable) resolvedType);

        // Return
        return resolvedType;
    }

    /**
     * Resolves a TypeVar for given method decl and arg types.
     */
    private JavaType getResolvedTypeVarForMethod(JavaTypeVariable aTypeVar, JavaMethod aMethod)
    {
        // Get parameter types
        JavaType[] methodParameterTypes = aMethod.getParameterTypes();

        // Iterate over method arg types to see if any can resolve the type var
        for (int i = 0, iMax = methodParameterTypes.length; i < iMax; i++) {
            JavaType methodParameterType = methodParameterTypes[i];
            JavaType resolvedType = getResolvedTypeVarForMethodParameterType(aTypeVar, methodParameterType, i);
            if (resolvedType != null)
                return resolvedType;
        }

        // Return not found
        return null;
    }

    /**
     * Resolves a TypeVar for given method decl and arg types.
     */
    private JavaType getResolvedTypeVarForMethodParameterType(JavaTypeVariable aTypeVar, JavaType methodParameterType, int parameterIndex)
    {
        // If no type var for given name, just return
        String typeVarName = aTypeVar.getName();

        // If method arg is TypeVar with same name, return arg expr eval type (if not null)
        if (methodParameterType instanceof JavaTypeVariable) {

            // If name matches, return arg expression eval type
             if (methodParameterType.getName().equals(typeVarName)) {
                 JExpr argExpr = getArg(parameterIndex);
                 JavaType argEvalType = argExpr != null ? argExpr.getEvalType() : null;
                 if (argEvalType != null)
                     return argEvalType;
             }
        }

        // If method arg is ParamType with matching param TypeVar,
        else if (methodParameterType instanceof JavaParameterizedType) {

            // Get parameterized type parameter types
            JavaParameterizedType argPT = (JavaParameterizedType) methodParameterType;
            JavaType[] paramTypes = argPT.getParamTypes();

            // Iterate over ParamType params
            for (int i = 0; i < paramTypes.length; i++) {

                JavaType paramType = paramTypes[i];

                // If name matches, return arg expression eval type
                if (paramType instanceof JavaTypeVariable && paramType.getName().equals(typeVarName)) {

                    // If arg type is parameterized type, get type
                    JExpr argExpr = getArg(parameterIndex);
                    JavaType argEvalType = argExpr != null ? argExpr.getEvalType() : null;
                    if (argEvalType instanceof JavaParameterizedType) {
                        JavaParameterizedType argEvalTypePT = (JavaParameterizedType) argEvalType;
                        JavaType[] argEvalTypePTParams = argEvalTypePT.getParamTypes();
                        if (i < argEvalTypePTParams.length)
                            return argEvalTypePTParams[i];
                        return null;
                    }

                    // Otherwise, return type
                    return argEvalType;
                }
            }
        }

        // Handle GenericArrayType: Need to handle var args
        else if (methodParameterType instanceof JavaGenericArrayType) {

            // Get array component type
            JavaGenericArrayType genericArrayType = (JavaGenericArrayType) methodParameterType;
            JavaType componentType = genericArrayType.getComponentType();

            // If name matches
            if (componentType.getName().equals(typeVarName)) {
                JExpr argExpr = getArg(parameterIndex);
                JavaClass argEvalClass = argExpr != null ? argExpr.getEvalClass() : null;
                if (argEvalClass != null) {
                    if (argEvalClass.isArray())
                        return argEvalClass.getComponentType();

                    // If VarArgs, need to get common ancestor class
                    return argEvalClass;
                }
            }
        }

        // Return not found
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
        JavaMethod method = getDecl();
        if (method == null) {

            // If no method exists for name, return can't resolve method name
            boolean hasAnyMethodForName = getMethodAny() != null;
            String methodString = getMethodString(hasAnyMethodForName);
            return NodeError.newErrorArray(this, "Can't find method: " + methodString);
        }

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
        return JavaClassUtils.getCompatibleMethodAll(scopeClass, name, null, staticOnly);
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
        // Get arg types
        JavaType[] argTypes = getArgEvalTypes();
        if (argTypes.length == 0)
            return "()";

        // Get arg type string and join by comma
        String[] argTypeStrings = ArrayUtils.map(argTypes, type -> type != null ? type.getSimpleName() : "null", String.class);
        String argTypeString = StringUtils.join(argTypeStrings, ",");

        // Return in parens
        return '(' + argTypeString + ')';
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodCall"; }
}
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
     * Tries to resolve the method declaration for this node.
     */
    @Override
    protected JavaMethod getDeclImpl()
    {
        // Get scope node class
        JavaDecl scopeDecl = getScopeDecl();
        JavaClass scopeClass = scopeDecl != null ? scopeDecl.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get method name and arg types
        String name = getName();
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaClass[] argClasses = new JavaClass[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            if (arg instanceof JExprLambda)
                return getMethodForLambdaArgs();
            JavaClass argClass = arg != null ? arg.getEvalClass() : null;
            argClasses[i] = argClass;
        }

        // Search for compatible method for name and arg types
        JavaMethod method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argClasses);
        if (method != null)
            return method;

        // If scope expression is present, just return
        if (getScopeExpr() != null)
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
            method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argClasses);
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
        int argIndex = ListUtils.findMatchIndex(args, arg -> arg instanceof JExprLambda);
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
        JavaDecl scopeDecl = getScopeDecl();
        JavaClass scopeClass = scopeDecl != null ? scopeDecl.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get method name and arg types
        String name = getName();
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaClass[] argClasses = new JavaClass[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            JavaClass argType = arg instanceof JExprLambda ? null : arg.getEvalClass();
            argClasses[i] = argType;
        }

        // Get scope node class type and search for compatible method for name and arg types
        List<JavaMethod> compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argClasses);
        if (compatibleMethods.size() > 0)
            return compatibleMethods;

        // If scope node class type is member class and not static, go up parent classes
        while (scopeClass.isMemberClass() && !scopeClass.isStatic()) {
            scopeClass = scopeClass.getDeclaringClass();
            compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argClasses);
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
    protected JavaType getResolvedTypeForType(JavaType aType)
    {
        // Get type
        JavaType resolvedType = aType;

        // Handle TypeVar
        if (aType instanceof JavaTypeVariable) {

            // Get name and method
            JavaTypeVariable typeVar = (JavaTypeVariable) resolvedType;
            JavaMethod method = getDecl();

            // See if TypeVar can be resolved by method
            if (method != null && typeVar.getOwner() == method) {
                JavaType resType = getResolvedTypeVarForMethod(typeVar, method);
                if (resType != null)
                    resolvedType = resType;
            }

            // See if TypeVar can be resolved by ScopeNode.Type
            else {
                JavaDecl scopeDecl = getScopeDecl();
                JavaType scopeType = scopeDecl != null ? scopeDecl.getEvalType() : null;
                JavaType resolvedDecl = scopeType != null ? scopeType.getResolvedType(resolvedType) : null;
                if (resolvedDecl != null)
                    resolvedType = resolvedDecl;
            }
        }

        // Handle ParameterizedType
        else if (aType instanceof JavaParameterizedType) {

            // Get parameterized type and parameter types
            JavaParameterizedType parameterizedType = (JavaParameterizedType) resolvedType;
            JavaType[] paramTypes = parameterizedType.getParamTypes();
            JavaType[] paramTypesResolved = paramTypes.clone();
            boolean didResolve = false;

            // Iterate over each and resolve if needed
            for (int i = 0; i < paramTypes.length; i++) {
                JavaType paramType = paramTypes[i];
                if (!paramType.isResolvedType()) {
                    JavaType paramTypeResolved = getResolvedTypeForType(paramType);
                    if (paramTypeResolved != paramType) {
                        paramTypesResolved[i] = paramTypeResolved;
                        didResolve = true;
                    }
                }
            }

            // If something was resolved, create new type with resolved parameter types
            if (didResolve) {
                JavaClass rawType = parameterizedType.getRawType();
                resolvedType = rawType.getParamTypeDecl(paramTypesResolved);
            }
        }

        // Handle Generic array type
        else if (aType instanceof JavaGenericArrayType)
            System.err.println("JExprMethodCall.getResolvedTypeForType: No support for GenericArrayType");

        // Do normal version (skip parent dot expression)
        if (!resolvedType.isResolvedType()) {
            JNode parentNode = getParent();
            if (parentNode instanceof JExprDot)
                parentNode = parentNode.getParent();
            resolvedType = parentNode.getResolvedTypeForType(resolvedType);
        }

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
        String name = aTypeVar.getName();

        // If method arg is TypeVar with same name, return arg expr eval type (if not null)
        if (methodParameterType instanceof JavaTypeVariable) {

            // If name matches, return arg expression eval type
             if (methodParameterType.getName().equals(name)) {
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
                if (paramType instanceof JavaTypeVariable && paramType.getName().equals(name)) {

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
            if (componentType.getName().equals(name)) {
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
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle can't resolve method
        JavaMethod method = getDecl();
        if (method == null) {
            String methodString = getMethodString();
            NodeError error = new NodeError(this, "Can't resolve method: " + methodString);
            errors = ArrayUtils.add(errors, error);
        }

        // Return
        return errors;
    }

    /**
     * Returns a string for method.
     */
    private String getMethodString()
    {
        // Get method name and arg types
        String methodName = getName();
        if (methodName == null)
            return "No name found";
        String argTypesString = getArgTypesString();
        String methodString = methodName + argTypesString;

        // Get scope node class name
        JavaDecl scopeDecl = getScopeDecl();
        String scopeClassName = scopeDecl != null ? scopeDecl.getEvalClassName() : null;
        if (scopeClassName != null)
            methodString = scopeClassName + '.' + methodName;

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
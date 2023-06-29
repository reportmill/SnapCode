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
public class JExprMethodCall extends JExpr {

    // The identifier
    JExprId  _id;

    // The args
    List<JExpr>  _args;

    // The param types
    private JavaType[]  _paramTypes;

    /**
     * Constructor.
     */
    public JExprMethodCall()  { }

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
    public JavaMethod getDecl()
    {
        return (JavaMethod) super.getDecl();
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    @Override
    protected JavaMethod getDeclImpl()
    {
        // Get method name and arg types
        String name = getName();

        // Get arg types
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaType[] argTypes = new JavaType[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            if (arg instanceof JExprLambda)
                return getMethodForLambdaArgs();
            JavaType argType = arg != null ? arg.getEvalType() : null;
            argTypes[i] = argType;
        }

        // Get scope node class
        JNode scopeNode = getScopeNode();
        JavaClass scopeClass = scopeNode != null ? scopeNode.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Search for compatible method for name and arg types
        JavaMethod method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argTypes);
        if (method != null)
            return method;

        // If scope node is class and not static, go up parent classes
        while (scopeNode instanceof JClassDecl && !scopeClass.isStatic()) {

            // Get enclosing JClassDecl and JavaClass
            scopeNode = scopeNode.getEnclosingClassDecl();
            scopeClass = scopeNode != null ? scopeNode.getEvalClass() : null;
            if (scopeClass == null)
                break;

            // If method found, return it
            method = JavaClassUtils.getCompatibleMethodAll(scopeClass, name, argTypes);
            if (method != null)
                return method;
        }

        // See if method is from static import
        JFile jfile = getFile();
        JavaMember importClassMember = jfile.getStaticImportMemberForNameAndParams(name, argTypes);
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
            JavaType paramType = method.getParamType(argIndex);
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
        // Get method name and args
        String name = getName();

        // Get arg types
        List<JExpr> args = getArgs();
        int argCount = args.size();
        JavaType[] argTypes = new JavaType[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = args.get(i);
            JavaType argType = arg instanceof JExprLambda ? null : arg.getEvalType();
            argTypes[i] = argType;
        }

        // Get scope node class type and search for compatible method for name and arg types
        JNode scopeNode = getScopeNode();
        JavaClass scopeClass = scopeNode != null ? scopeNode.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get scope node class type and search for compatible method for name and arg types
        List<JavaMethod> compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argTypes);
        if (compatibleMethods.size() > 0)
            return compatibleMethods;

        // If scope node class type is member class and not static, go up parent classes
        while (scopeClass.isMemberClass() && !scopeClass.isStatic()) {
            scopeClass = scopeClass.getDeclaringClass();
            compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argTypes);
            if (compatibleMethods.size() > 0)
                return compatibleMethods;
        }

        // See if method is from static import -
        JFile jfile = getFile();
        JavaMember importClassMember = jfile.getStaticImportMemberForNameAndParams(name, argTypes);
        if (importClassMember instanceof JavaMethod)
            return Collections.singletonList((JavaMethod) importClassMember);

        // Return
        return compatibleMethods;
    }

    /**
     * Override to handle method name.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        if (anExprId == _id)
            return getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Override to resolve Decl.EvalType from ParentExpr.EvalType.
     */
    @Override
    protected JavaType getEvalTypeImpl(JNode aNode)
    {
        // Handle MethodCall id
        if (aNode == _id)
            return getEvalType();

        // Handle this node
        else if (aNode == this) {

            // Get method, eval type and scope type
            JavaMethod method = getDecl();
            if (method == null)
                return null;
            JavaType evalType = method.getEvalType();

            // If eval type is TypeVar, try to resolve
            if (evalType instanceof JavaTypeVariable) {
                String name = evalType.getName();

                // See if TypeVar can be resolved by method
                JavaType resolvedDecl = getResolvedTypeVarForMethod(name, method);
                if (resolvedDecl != null)
                    return resolvedDecl;

                // See if TypeVar can be resolved by ScopeNode.Type
                JNode scopeNode = getScopeNode();
                JavaType scopeType = scopeNode != null ? scopeNode.getEvalType() : null;
                resolvedDecl = scopeType != null ? scopeType.getResolvedType(evalType) : null;
                if (resolvedDecl != null)
                    return resolvedDecl;

                // Otherwise, just return TypeVar default
                return evalType.getEvalType();
            }
        }

        // Do normal version
        return super.getEvalTypeImpl(aNode);
    }

    /**
     * Resolves a TypeVar for given method decl and arg types.
     */
    public JavaType getResolvedTypeVarForMethod(String aName, JavaMethod aMethod)
    {
        // If no type var for given name, just return
        JavaTypeVariable typeVar = aMethod.getTypeVarForName(aName);
        if (typeVar == null)
            return null;

        // Iterate over method arg types to see if any can resolve the type var
        JavaDecl[] argTypes = aMethod.getParamTypes();
        for (int i = 0, iMax = argTypes.length; i < iMax; i++) {
            JavaDecl arg = argTypes[i];

            // If method arg is TypeVar with same name, return arg expr eval type (if not null)
            if (arg instanceof JavaTypeVariable && arg.getName().equals(aName)) {
                JExpr argExpr = getArg(i);
                if (argExpr == null)
                    continue;
                JavaType argEvalType = argExpr.getEvalType();
                return argEvalType;
            }

            // If method arg is ParamType with matching param TypeVar,
            if (arg instanceof JavaParameterizedType) {

                // Iterate over ParamType params
                JavaParameterizedType argPT = (JavaParameterizedType) arg;
                JavaType[] paramTypes = argPT.getParamTypes();
                for (JavaType paramType : paramTypes) {

                    // If TypeVar with matching name, see if arg eval type can resolve
                    if (paramType instanceof JavaTypeVariable && paramType.getName().equals(aName)) {

                        // Get arg expr and eval type
                        JExpr argExpr = getArg(i);
                        if (argExpr == null) continue;
                        JavaDecl argEvalType = argExpr.getEvalType();
                        if (argEvalType == null)
                            continue;
                        if (argEvalType instanceof JavaParameterizedType) {
                            JavaParameterizedType argEvalPT = (JavaParameterizedType) argEvalType;
                            JavaType[] argParamTypes = argEvalPT.getParamTypes();
                            return argParamTypes[0];
                        }
                    }
                }
            }
        }

        // Return null since TypeVar name couldn't be resolved by method args
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
        JNode scopeNode = getScopeNode();
        String scopeClassName = scopeNode != null ? scopeNode.getEvalClassName() : null;
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
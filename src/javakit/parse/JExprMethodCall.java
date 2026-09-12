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
public class JExprMethodCall extends JExpr implements WithId, WithArgs {

    // The scope expression
    private JExpr _scopeExpr;

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
     * Returns the scope expression.
     */
    @Override
    public JExpr getScopeExpr()  { return _scopeExpr; }

    /**
     * Sets the scope expression.
     */
    public void setScopeExpr(JExpr scopeExpr)
    {
        addChild(_scopeExpr = scopeExpr, 0);
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
        return compatibleMethods.getFirst();
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
        return compatibleMethods.getFirst();
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
     * Returns the JavaType for the scope expression (if present) or enclosing class.
     */
    JavaType getScopeEvalType()
    {
        // If scope expression exists, forward to it
        JExpr scopeExpr = getScopeExpr();
        if (scopeExpr != null)
            return scopeExpr.getEvalType();

        // Otherwise, return enclosing class
        JClassDecl classDecl = getEnclosingClassDecl();
        return classDecl != null ? classDecl.getEvalType() : null;
    }

    /**
     * Override to return method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

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
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodCall"; }
}
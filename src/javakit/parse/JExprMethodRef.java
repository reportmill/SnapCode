/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import java.util.Objects;

/**
 * This JExpr subclass represents a method reference: obj::method.
 */
public class JExprMethodRef extends JExprLambdaBase {

    // The scope expression
    private JExpr _scopeExpr;

    // The method name identifier
    private JExprId _methodId;

    // The type of method ref
    private Type _type;

    // The method
    private JavaMethod _method;

    // The constructor
    private JavaConstructor _constructor;

    // A constant to define the types of method refs.
    // See https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html
    public enum Type { InstanceMethod, StaticMethod, HelperMethod, Constructor, ArrayInit, Unknown }

    /**
     * Creates a new Method Reference expression for expression and id.
     */
    public JExprMethodRef(JExpr scopeExpr, JExprId anId)
    {
        setScopeExpr(scopeExpr);
        setMethodId(anId);
    }

    /**
     * Returns the scope expression.
     */
    @Override
    public JExpr getScopeExpr()  { return _scopeExpr; }

    /**
     * Sets the scope expression.
     */
    public void setScopeExpr(JExpr anExpr)
    {
        addChild(_scopeExpr = anExpr, 0);
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
        // If something::new, return type Constructor or ArrayInit
        if (Objects.equals(getMethodName(), "new")) {
            if (_scopeExpr instanceof JExprType typeExpr) {
                JType scopeType = typeExpr.getType();
                if (scopeType != null && scopeType.isArrayType())
                    return Type.ArrayInit;
            }
            return Type.Constructor;
        }

        JavaMethod method = getMethod();
        if (method == null)
            return Type.Unknown;
        if (method.getParameterCount() == 0)
            return Type.InstanceMethod;
        if (_scopeExpr.isClassNameLiteral())
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

        // Get scope expr eval class
        JExpr scopeExpr = _scopeExpr;
        JavaType scopeEvalType = scopeExpr != null ? scopeExpr.getEvalType() : null;
        JavaClass scopeClass = scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get parameter types from lambda method and look for method
        JavaType[] paramTypes = getLambdaMethodParameterTypesResolved();
        JavaClass[] paramClasses = paramTypes != null ? ArrayUtils.map(paramTypes, type -> type.getEvalClass(), JavaClass.class) : null;
        if (paramClasses == null)
            return null;

        // If one parameter with same class as scope expression class, search for instance method with no args
        if (paramClasses.length == 1) {
            JavaClass paramClass = paramClasses[0];
            if (scopeClass.isAssignableFrom(paramClass)) {
                JavaMethod instanceMethod = JavaClassUtils.getCompatibleMethod(scopeClass, methodName, new JavaClass[0], false);
                if (instanceMethod != null && !instanceMethod.isStatic())
                    return instanceMethod;
            }
        }

        // Get lambda method - just return if null
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;

        // Get whether scope expression is class name literal
        boolean staticOnly = scopeExpr.isClassNameLiteral();

        // Search for static or helper method for name and arg types
        JavaMethod helperMethod = JavaClassUtils.getCompatibleMethod(scopeClass, methodName, paramClasses, staticOnly);
        if (helperMethod != null)
            return helperMethod;

        // Return not found
        return null;
    }

    /**
     * Tries to resolve the constructor declaration for this node (if type is constructor).
     */
    public JavaConstructor getConstructor()
    {
        if (_constructor != null) return _constructor;
        return _constructor = getConstructorImpl();
    }

    /**
     * Tries to resolve the constructor declaration for this node (if type is constructor).
     */
    protected JavaConstructor getConstructorImpl()
    {
        // Get method name
        String methodName = getMethodName();
        if (!Objects.equals(methodName, "new"))
            return null;

        // Get scope expr eval class
        JavaClass scopeClass = getScopeExprClass();
        if (scopeClass == null || scopeClass.isArray())
            return null;

        // Get parameter types from lambda method and look for constructor
        JavaType[] paramTypes = getLambdaMethodParameterTypesResolved();
        JavaClass[] paramClasses = paramTypes != null ? ArrayUtils.map(paramTypes, type -> type.getEvalClass(), JavaClass.class) : null;
        if (paramClasses != null) {
            JavaConstructor constructor = scopeClass.getDeclaredConstructorForClasses(paramClasses);
            if (constructor != null)
                return constructor;
        }

        // Return default constructor
        return scopeClass.getDeclaredConstructorForClasses(new JavaClass[0]);
    }

    /**
     * Returns the scope expression eval class.
     */
    public JavaClass getScopeExprClass()
    {
        JavaType scopeEvalType = _scopeExpr != null ? _scopeExpr.getEvalType() : null;
        return scopeEvalType != null ? scopeEvalType.getEvalClass() : null;
    }

    /**
     * Returns the executable, depending on whether method ref is method or constructor.
     */
    public JavaExecutable getExecutable()
    {
        if (getType() == Type.Constructor)
            return getConstructor();
        return getMethod();
    }

    /**
     * Override to get from executable or scope array class.
     */
    @Override
    protected JavaType getLambdaReturnType()
    {
        // If array creation, return scope expr class
        if (getType() == Type.ArrayInit)
            return getScopeExprClass();

        // Return method return type
        JavaExecutable methodRefMethod = getExecutable();
        if (methodRefMethod != null)
            return methodRefMethod.getEvalType();

        // Return not found
        return null;
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "MethodRef"; }
}
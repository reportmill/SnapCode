/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaExecutable;
import javakit.resolver.JavaMethod;

/**
 * A Java member for a method declaration.
 */
public class JMethodDecl extends JExecutableDecl {

    // The return type
    protected JType _returnType;

    // The method
    private JavaMethod _method;

    /**
     * Constructor.
     */
    public JMethodDecl()
    {
        super();
    }

    /**
     * Returns the return type.
     */
    public JType getReturnType()  { return _returnType; }

    /**
     * Sets the return type.
     */
    public void setReturnType(JType aType)
    {
        replaceChild(_returnType, _returnType = aType);
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
        // Get method name
        String name = getName();
        if (name == null)
            return null;

        // Get param classes
        JavaClass[] paramTypes = getParameterClasses();

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // Return method for name and param types
        return javaClass.getDeclaredMethodForNameAndClasses(name, paramTypes);
    }

    /**
     * Override to get decl from method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

    /**
     * Override to return method.
     */
    @Override
    public JavaExecutable getExecutable()  { return getMethod(); }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodDecl"; }

    /**
     * Override to return errors for ReturnValue, Parameters, ThrowsList and TypeVars.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Get errors for type
        JType returnType = getReturnType();
        if (returnType == null)
            return NodeError.newErrorArray(getChild(0), "Missing return type");

        // Do normal version
        return super.getErrorsImpl();
    }
}
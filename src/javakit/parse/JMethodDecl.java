/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaExecutable;
import javakit.resolver.JavaMethod;
import javakit.resolver.JavaType;

/**
 * A Java member for a method declaration.
 */
public class JMethodDecl extends JExecutableDecl {

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
     * Override to get decl from method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

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

        // Get param types
        JavaType[] paramTypes = getParamClassTypesSafe();
        if (paramTypes == null)
            return null; // Can happen if params are bogus

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl == null)
            return null;
        JavaClass javaClass = enclosingClassDecl.getDecl();
        if (javaClass == null)
            return null;

        // Return method for name and param types
        return javaClass.getDeclaredMethodForNameAndTypes(name, paramTypes);
    }

    /**
     * Override to return method.
     */
    @Override
    public JavaExecutable getExecutable()  { return getMethod(); }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodDecl"; }
}
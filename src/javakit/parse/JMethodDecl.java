/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaType;

/**
 * A Java member for a method declaration.
 */
public class JMethodDecl extends JExecutableDecl {

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
    protected JavaDecl getDeclImpl()
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
        return javaClass.getMethodForNameAndTypes(name, paramTypes);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodDecl"; }
}
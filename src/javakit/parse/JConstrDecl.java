/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * A Java member for a constructor declaration.
 */
public class JConstrDecl extends JExecutableDecl {

    // The constructor
    private JavaConstructor _constructor;

    /**
     * Constructor.
     */
    public JConstrDecl()
    {
        super();
    }

    /**
     * Override to get declaration from Constructor.
     */
    protected JavaConstructor getDeclImpl()  { return getConstructor(); }

    /**
     * Returns the Constructor.
     */
    public JavaConstructor getConstructor()
    {
        if (_constructor != null) return _constructor;
        return _constructor = getConstructorImpl();
    }

    /**
     * Returns the Constructor.
     */
    private JavaConstructor getConstructorImpl()
    {
        // Get param types
        JavaType[] paramTypes = getParameterClasses();

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // If inner class and not static, add implied class type to arg types array
        if (javaClass.isMemberClass() && !javaClass.isStatic()) {
            JavaClass parentClass = javaClass.getDeclaringClass();
            paramTypes = ArrayUtils.add(paramTypes, parentClass, 0);
        }

        // If enum, add implied args types for name (String) and ordinal (int)
        else if (javaClass.isEnum()) {
            paramTypes = ArrayUtils.add(paramTypes, getJavaClassForClass(String.class), 0);
            paramTypes = ArrayUtils.add(paramTypes, getJavaClassForClass(int.class), 1);
        }

        // Return Constructor for param types
        return javaClass.getDeclaredConstructorForTypes(paramTypes);
    }

    /**
     * Override to return constructor.
     */
    @Override
    public JavaExecutable getExecutable()  { return getConstructor(); }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "ConstrDecl"; }
}
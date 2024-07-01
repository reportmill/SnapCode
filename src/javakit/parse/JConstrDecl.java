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
        // Get parameter classes
        JavaClass[] parameterClasses = getParameterClasses();

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // If inner class and not static, add implied class type to arg types array
        if (javaClass.isMemberClass() && !javaClass.isStatic()) {
            JavaClass parentClass = javaClass.getDeclaringClass();
            parameterClasses = ArrayUtils.add(parameterClasses, parentClass, 0);
        }

        // If enum, add implied args types for name (String) and ordinal (int)
        else if (javaClass.isEnum()) {
            parameterClasses = ArrayUtils.add(parameterClasses, getJavaClassForClass(String.class), 0);
            parameterClasses = ArrayUtils.add(parameterClasses, getJavaClassForClass(int.class), 1);
        }

        // Get Constructor for param types - just return if found
        JavaConstructor javaConstructor = javaClass.getDeclaredConstructorForClasses(parameterClasses);
        if (javaConstructor != null)
            return javaConstructor;

        // Otherwise just create and return constructor
        return JavaClassUpdaterDecl.createConstructorForDecl(this);
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
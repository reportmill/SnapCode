/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;

/**
 * A Java member for a constructor declaration.
 */
public class JConstrDecl extends JExecutableDecl {

    /**
     * Constructor.
     */
    public JConstrDecl()
    {
        super();
    }

    /**
     * Override to get declaration from actual Constructor.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get param types
        JavaType[] paramTypes = getParamClassTypesSafe();
        if (paramTypes == null)
            return null; // Can happen if params bogus/editing

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl == null)
            return null;
        JavaClass javaClass = enclosingClassDecl.getDecl();
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
        return javaClass.getConstructorForTypes(paramTypes);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "ConstrDecl"; }
}
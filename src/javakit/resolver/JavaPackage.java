/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;

/**
 * This class represents a Java Package.
 */
public class JavaPackage extends JavaDecl {

    // The package
    private JavaPackage  _package;

    // The child declarations (packages and classes)
    private JavaDecl[] _children;

    // The child packages
    private JavaPackage[] _packages;

    // The child classes
    private JavaClass[] _classes;

    /**
     * Constructor.
     */
    public JavaPackage(Resolver aResolver, JavaPackage aParent, String aPackageName)
    {
        super(aResolver, DeclType.Package);

        // Set parent package
        _package = aParent;

        // Set Name, SimpleName
        _id = _name = aPackageName;
        _simpleName = getSimpleName(aPackageName);
    }

    /**
     * Returns the parent package.
     */
    public JavaPackage getPackage()  { return _package; }

    /**
     * Returns the child packages and classes.
     */
    public JavaDecl[] getChildren()
    {
        if (_children != null) return _children;

        // Get children
        String name = getName();
        JavaDecl[] children = _resolver.getChildrenForPackageName(name);

        // Set and return
        return _children = children;
    }

    /**
     * Returns the child packages.
     */
    public JavaPackage[] getPackages()
    {
        if (_packages != null) return _packages;

        // Get children and filter packages
        JavaDecl[] children = getChildren();
        JavaPackage[] packages = ArrayUtils.filterByClass(children, JavaPackage.class);

        // Set and return
        return _packages = packages;
    }

    /**
     * Returns the child classes.
     */
    public JavaClass[] getClasses()
    {
        if (_classes != null) return _classes;

        // Get children and filter classes
        JavaDecl[] children = getChildren();
        JavaClass[] classes = ArrayUtils.filterByClass(children, JavaClass.class);

        // Set and return
        return _classes = classes;
    }

    /**
     * Returns a simple class name.
     */
    private static String getSimpleName(String cname)
    {
        int i = cname.lastIndexOf('$');
        if (i < 0) i = cname.lastIndexOf('.');
        if (i > 0) cname = cname.substring(i + 1);
        return cname;
    }

    /**
     * Prints the tree.
     */
    protected void printTree(String indent)
    {
        System.out.println(indent + getSimpleName());
        String indent2 = indent + "  ";
        for (JavaPackage pkg : getPackages())
            pkg.printTree(indent2);
        for (JavaClass cls : getClasses())
            System.out.println(indent2 + cls.getSimpleName());
    }
}

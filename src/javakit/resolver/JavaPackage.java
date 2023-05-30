/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

/**
 * This class represents a Java Package.
 */
public class JavaPackage extends JavaDecl {

    // The package
    private JavaPackage  _package;

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
     * Returns a simple class name.
     */
    private static String getSimpleName(String cname)
    {
        int i = cname.lastIndexOf('$');
        if (i < 0) i = cname.lastIndexOf('.');
        if (i > 0) cname = cname.substring(i + 1);
        return cname;
    }
}

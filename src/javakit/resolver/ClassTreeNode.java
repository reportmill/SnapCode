/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A class to hold package info.
 */
public class ClassTreeNode {

    // Whether node is package
    private boolean _isPackage;

    // The parent package
    public ClassTreeNode parentPackage;

    // The package full name
    public final String fullName;

    // The package simple name
    public final String simpleName;

    // Child packages
    public ClassTreeNode[] packages = EMPTY_NODE_ARRAY;

    // Child classes
    public ClassTreeNode[] classes = EMPTY_NODE_ARRAY;

    // Constants
    private static final ClassTreeNode[] EMPTY_NODE_ARRAY = new ClassTreeNode[0];

    /**
     * Constructor.
     */
    public ClassTreeNode(ClassTreeNode aParentPackage, String aPackageName, boolean isPackage)
    {
        _isPackage = isPackage;
        parentPackage = aParentPackage;
        fullName = aPackageName;
        simpleName = ClassTree.getSimpleNodeName(aPackageName);
    }

    /**
     * Returns whether node is package.
     */
    public boolean isPackage()  { return  _isPackage; }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        // Add FullName, SimpleName, Parent
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb, "FullName", fullName);
        StringUtils.appendProp(sb, "SimpleName", simpleName);
        if (parentPackage != null)
            StringUtils.appendProp(sb, "Parent", parentPackage.fullName);

        // Add Packages
        if (packages.length > 0) {
            String[] pkgNames = Stream.of(packages).map(p -> p.simpleName).toArray(size -> new String[size]);
            StringUtils.appendProp(sb, "Packages", Arrays.toString(pkgNames));
        }

        // Add Classes
        if (classes.length > 0) {
            String[] classNames = Stream.of(classes).map(p -> p.simpleName).toArray(size -> new String[size]);
            StringUtils.appendProp(sb, "Classes", Arrays.toString(classNames));
        }

        // Return
        return sb.toString();
    }
}

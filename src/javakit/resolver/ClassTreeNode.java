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

    // The parent node (class or package)
    private ClassTreeNode _parent;

    // The package full name
    public final String fullName;

    // The package simple name
    public final String simpleName;

    // The child nodes
    public ClassTreeNode[] _children;

    // Child packages
    public ClassTreeNode[] packages = ClassTree.EMPTY_NODE_ARRAY;

    // Child classes
    public ClassTreeNode[] classes = ClassTree.EMPTY_NODE_ARRAY;

    /**
     * Constructor.
     */
    public ClassTreeNode(ClassTreeNode aParent, String aPackageName, boolean isPackage)
    {
        _isPackage = isPackage;
        _parent = aParent;
        fullName = aPackageName;
        simpleName = ClassTree.getSimpleNodeName(aPackageName);
    }

    /**
     * Returns whether node is package.
     */
    public boolean isPackage()  { return  _isPackage; }

    /**
     * Returns the parent.
     */
    public ClassTreeNode getParent()  { return _parent; }

    /**
     * Returns the parent package.
     */
    public ClassTreeNode getPackage()
    {
        for (ClassTreeNode parentNode = _parent; parentNode != null; parentNode = parentNode.getParent()) {
            if (parentNode.isPackage())
                return parentNode;
        }
        return null;
    }

    /**
     * Returns the child nodes.
     */
    public ClassTreeNode[] getChildren()
    {
        // If already set, just return
        if (_children != null) return _children;

        // Get children

        // Return
        return _children;
    }

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
        if (_parent != null)
            StringUtils.appendProp(sb, "Parent", _parent.fullName);

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

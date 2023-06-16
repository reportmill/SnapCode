/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import snap.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A class to hold package info.
 */
public class ClassTreeNode {

    // The ClassTree
    private ClassTree _classTree;

    // Whether node is package
    private boolean _isPackage;

    // The parent node (class or package)
    private ClassTreeNode _parent;

    // The package full name
    public final String fullName;

    // The package simple name
    public final String simpleName;

    // The child nodes
    protected ClassTreeNode[] _children;

    // Child packages
    private ClassTreeNode[] _packages;

    // Child classes
    private ClassTreeNode[] _classes;

    /**
     * Constructor.
     */
    public ClassTreeNode(ClassTree classTree, ClassTreeNode aParent, String aPackageName, boolean isPackage)
    {
        super();
        _classTree = classTree;
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
        ClassTreeNode[] childNodes = _classTree.getChildNodesForNode(this);

        // Return
        return _children = childNodes;
    }

    /**
     * Adds a child node.
     */
    protected void addChild(ClassTreeNode aClass)
    {
        getChildren();
        _children = ArrayUtils.addUnique(_children, aClass);
        _packages = _classes = null;
    }

    /**
     * Returns child packages.
     */
    public ClassTreeNode[] getPackages()
    {
        if (_packages != null) return _packages;
        ClassTreeNode[] children = getChildren();
        ClassTreeNode[] packages2 = ArrayUtils.filter(children, node -> node._isPackage);
        return _packages = packages2;
    }

    /**
     * Returns child packages.
     */
    public ClassTreeNode[] getClasses()
    {
        if (_classes != null) return _classes;
        ClassTreeNode[] children = getChildren();
        ClassTreeNode[] classes2 = ArrayUtils.filter(children, node -> !node._isPackage);
        return _classes = classes2;
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
        if (_packages != null && _packages.length > 0) {
            String[] pkgNames = Stream.of(_packages).map(p -> p.simpleName).toArray(size -> new String[size]);
            StringUtils.appendProp(sb, "Packages", Arrays.toString(pkgNames));
        }

        // Add Classes
        if (_classes != null && _classes.length > 0) {
            String[] classNames = Stream.of(_classes).map(p -> p.simpleName).toArray(size -> new String[size]);
            StringUtils.appendProp(sb, "Classes", Arrays.toString(classNames));
        }

        // Return
        return sb.toString();
    }
}

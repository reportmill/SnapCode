/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The root package
    protected ClassTreeNode _rootPackage;

    // The packages
    private Map<String, ClassTreeNode>  _packages = new HashMap<>();

    // Constants
    public static final ClassTreeNode[] EMPTY_NODE_ARRAY = new ClassTreeNode[0];

    // A shared instance
    private static ClassTree  _shared;

    /**
     * Constructor.
     */
    public ClassTree()
    {
        super();

        // Create RootPackage
        _rootPackage = new ClassTreeNode(this, null, "", true);
        _packages.put("", _rootPackage);

        // Add primitive classes
        Class<?>[] primitives = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };
        _rootPackage._children = ArrayUtils.map(primitives, cls -> new ClassTreeNode(this, _rootPackage, cls.getName(), false), ClassTreeNode.class);
    }

    /**
     * Returns the root package.
     */
    public ClassTreeNode getRootPackage()  { return _rootPackage; }

    /**
     * Returns a package for name.
     */
    public ClassTreeNode getPackageForName(String aName)
    {
        // Get package for name from cache - just return if found
        ClassTreeNode packageNode = _packages.get(aName);
        if (packageNode != null)
            return packageNode;

        // Get parent node
        String parentNodeName = getParentNodeName(aName);
        ClassTreeNode parentNode = getPackageForName(parentNodeName);

        // Create
        packageNode = new ClassTreeNode(this, parentNode, aName, true);

        // If simple ClassTree, add child
        if (getClass() == ClassTree.class)
            parentNode.addChild(packageNode);

        // Set, return
        _packages.put(aName, packageNode);
        return packageNode;
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    protected ClassTreeNode[] getChildNodesForNode(ClassTreeNode parentNode)
    {
        // Get child class names matching package name
        String prefix = parentNode.fullName + '.';
        Predicate<String> isChildNode = className -> className.startsWith(prefix) && className.indexOf('.', prefix.length()) < 0;
        String[] childClassNames = ArrayUtils.filter(COMMON_CLASS_NAMES, isChildNode);

        // Create/return ClassTreeNode array for class names
        return ArrayUtils.map(childClassNames, className -> new ClassTreeNode(this, parentNode, className, false), ClassTreeNode.class);
    }

    /**
     * Prints the tree.
     */
    protected void printTree(ClassTreeNode aNode, String indent)
    {
        System.out.println(indent + aNode.simpleName);
        if (aNode.isPackage()) {
            String indent2 = indent + "  ";
            for (ClassTreeNode pkg : aNode.getPackages())
                printTree(pkg, indent2);
            for (ClassTreeNode cls : aNode.getClasses())
                printTree(cls, indent2);
        }
    }

    /**
     * Returns a simple class name for given node name.
     */
    protected static String getSimpleNodeName(String aNodeName)
    {
        // Get index of last '$' or '.'
        int sepIndex = aNodeName.lastIndexOf('$');
        if (sepIndex < 0)
            sepIndex = aNodeName.lastIndexOf('.');

        // Return ClassName stripped of package and/or parent-class
        return aNodeName.substring(sepIndex + 1);
    }

    /**
     * Returns a parent node name for given node name.
     */
    protected static String getParentNodeName(String aNodeName)
    {
        int sepIndex = aNodeName.lastIndexOf('.');
        if (sepIndex < 0)
            return "";
        return aNodeName.substring(0, sepIndex);
    }

    /**
     * Returns the shared instance.
     */
    public static ClassTree getShared()
    {
        // If already set, just return
        if (_shared != null) return _shared;

        // Create simple class tree
        ClassTree classTree = new ClassTree();

        // Get unique packages names and make packages get explicitly created
        Set<String> packageNames = new HashSet<>();
        for (String className : COMMON_CLASS_NAMES)
            packageNames.add(getParentNodeName(className));
        for (String packageName : packageNames)
            classTree.getPackageForName(packageName);

        // Set and return
        return _shared = classTree;
    }

    /**
     * Adds to the CommonClassNames.
     */
    public static void addCommonClassNames(String[] moreNames)
    {
        COMMON_CLASS_NAMES = ArrayUtils.addAll(COMMON_CLASS_NAMES, moreNames);
    }

    /**
     * An array of common class names.
     */
    private static String[] COMMON_CLASS_NAMES = {

            // Java.lang
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Class", "java.lang.Double",
            "java.lang.Enum", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number",
            "java.lang.Object", "java.lang.String", "java.lang.StringBuffer", "java.lang.StringBuilder", "java.lang.System",
            "java.lang.Thread",

            // Java.util
            "java.util.List", "java.util.Map", "java.util.Set", "java.util.ArrayList", "java.util.Arrays",
            "java.util.Collections", "java.util.Date", "java.util.HashMap", "java.util.HashSet", "java.util.Hashtable",
            "java.util.Map", "java.util.Random", "java.util.Scanner", "java.util.Stack", "java.util.Timer",
            "java.util.Vector",

            // Java.io
            "java.io.File",

            // Snap.gfx
            "snap.gfx.Border", "snap.gfx.Color", "snap.gfx.Font",

            // Snap.view
            "snap.view.Button", "snap.view.Label", "snap.view.View", "snap.view.ViewOwner",
            "snap.view.Slider", "snap.view.TextField", "snap.view.ProgressBar", "snap.view.Spinner"
    };
}

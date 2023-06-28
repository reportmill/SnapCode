/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import java.util.function.Predicate;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The root package
    protected ClassTreeNode[] _rootChildren;

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

        // Add primitive classes
        Class<?>[] primitives = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class };
        _rootChildren = ArrayUtils.map(primitives, cls -> new ClassTreeNode(cls.getName(), false), ClassTreeNode.class);
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    protected ClassTreeNode[] getChildNodesForPackageName(String packageName)
    {
        // Handle root package special
        if (packageName.length() == 0)
            return _rootChildren;

        // Get child class names matching package name
        String prefix = packageName + '.';
        Predicate<String> isChildNode = className -> className.startsWith(prefix) && className.indexOf('.', prefix.length()) < 0;

        // Get childClassNames
        String[] childPackageNames = ArrayUtils.filter(COMMON_PACKAGE_NAMES, isChildNode);
        String[] childClassNames = ArrayUtils.filter(COMMON_CLASS_NAMES, isChildNode);
        String[] childNames = childPackageNames.length == 0 ? childClassNames :
                childClassNames.length == 0 ? childPackageNames :
                ArrayUtils.addAll(childPackageNames, childClassNames);

        // Create/return ClassTreeNode array for class names
        return ArrayUtils.map(childNames, className -> new ClassTreeNode(className, ArrayUtils.contains(childPackageNames, className)), ClassTreeNode.class);
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
     * Returns the shared instance.
     */
    public static ClassTree getShared()
    {
        // If already set, just return
        if (_shared != null) return _shared;

        // Create simple class tree
        ClassTree classTree = new ClassTree();
        String[] rooPackageNames = new String[] { "java", "snap", "snapcharts" };
        ClassTreeNode[] rootPackages = ArrayUtils.map(rooPackageNames, pkgName -> new ClassTreeNode(pkgName, true), ClassTreeNode.class);
        classTree._rootChildren = ArrayUtils.addAll(classTree._rootChildren, rootPackages);

        // Set and return
        return _shared = classTree;
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
            "snap.view.Slider", "snap.view.TextField", "snap.view.ProgressBar", "snap.view.Spinner",

            // Snapcharts.data, repl
            "snapcharts.data.DoubleArray",
            "snapcharts.repl.Quick3D", "snapcharts.repl.QuickCharts",
            "snapcharts.repl.QuickData", "snapcharts.repl.QuickDraw", "snapcharts.repl.QuickDrawPen"
    };

    /**
     * An array of common class names.
     */
    private static String[] COMMON_PACKAGE_NAMES = {
        "java", "java.lang", "java.util", "java.io",
        "snap", "snap.gfx", "snap.view",
        "snapcharts", "snapcharts.data", "snapcharts.repl"
    };

    /**
     * A class to hold package info.
     */
    public static class ClassTreeNode {

        // Whether node is package
        public final boolean isPackage;

        // The package full name
        public final String fullName;

        // The package simple name
        public final String simpleName;

        /**
         * Constructor.
         */
        public ClassTreeNode(String aPackageName, boolean isPackage)
        {
            super();
            this.isPackage = isPackage;
            fullName = aPackageName;
            simpleName = getSimpleNodeName(aPackageName);
        }

        /**
         * Standard toString implementation.
         */
        public String toString()
        {
            // Get class name
            String className = getClass().getSimpleName();

            // Get prop strings: FullName, SimpleName, Parent
            StringBuffer propStrings = new StringBuffer();
            if (isPackage)
                StringUtils.appendProp(propStrings, "Package", true);
            StringUtils.appendProp(propStrings, "FullName", fullName);
            StringUtils.appendProp(propStrings, "SimpleName", simpleName);

            // Return
            return className + " { " + propStrings + " }";
        }
    }
}

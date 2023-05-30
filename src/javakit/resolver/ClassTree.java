/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The root package
    private PackageNode  _rootPackage;

    // The packages
    private Map<String,PackageNode>  _packages = new HashMap<>();

    // All classes
    private ClassNode[]  _allClasses;

    // Constants
    private static final PackageNode[] EMPTY_PACKAGE_ARRAY = new PackageNode[0];
    private static final ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];

    /**
     * Constructor.
     */
    public ClassTree()
    {
        super();

        // Create RootPackage
        _rootPackage = new PackageNode(null, "");
        _packages.put("", _rootPackage);

        // Add primitives
        _rootPackage.classes = new ClassNode[] {
                new ClassNode(_rootPackage, boolean.class.getName()),
                new ClassNode(_rootPackage, char.class.getName()),
                new ClassNode(_rootPackage, byte.class.getName()),
                new ClassNode(_rootPackage, short.class.getName()),
                new ClassNode(_rootPackage, int.class.getName()),
                new ClassNode(_rootPackage, long.class.getName()),
                new ClassNode(_rootPackage, float.class.getName()),
                new ClassNode(_rootPackage, double.class.getName())
        };
    }

    /**
     * Returns the root package.
     */
    public PackageNode getRootPackage()  { return _rootPackage; }

    /**
     * Returns a package for name.
     */
    public PackageNode getPackageForName(String aName)
    {
        // Get package for name from cache - just return if found
        PackageNode packageNode = _packages.get(aName);
        if (packageNode != null)
            return packageNode;

        // Get parent node
        String parentNodeName = getParentNodeName(aName);
        PackageNode parentNode = getPackageForName(parentNodeName);

        // Create
        packageNode = new PackageNode(parentNode, aName);
        parentNode.packages = ArrayUtils.add(parentNode.packages, packageNode);

        // Set, return
        _packages.put(aName, packageNode);
        return packageNode;
    }

    /**
     * Returns all classes.
     */
    public ClassNode[] getAllClasses()
    {
        // If already set, just return
        if (_allClasses != null) return _allClasses;

        // Get all classes
        PackageNode rootPackage = getRootPackage();
        List<ClassNode> allClasses = new ArrayList<>();
        getAllClassesForPackageDeep(rootPackage, allClasses);

        // Set, return
        return _allClasses = allClasses.toArray(new ClassNode[0]);
    }

    /**
     * Returns all classes.
     */
    private void getAllClassesForPackageDeep(PackageNode aPackage, List<ClassNode> allClassesList)
    {
        // Add all package classes to list
        Collections.addAll(allClassesList, aPackage.classes);

        // Recurse for each child package
        for (PackageNode childPkg : aPackage.packages)
            getAllClassesForPackageDeep(childPkg, allClassesList);
    }

    /**
     * Returns an array of most common classes.
     */
    public ClassNode[] getCommonClasses()
    {
        ClassTree webClassTree = ClassTreeWeb.getShared();
        ClassNode[] commonClasses = webClassTree.getAllClasses();
        return commonClasses;
    }

    /**
     * Prints the tree.
     */
    protected void printTree(ClassTreeNode aNode, String indent)
    {
        System.out.println(indent + aNode.simpleName);
        if (aNode instanceof PackageNode) {
            PackageNode packageNode = (PackageNode) aNode;
            String indent2 = indent + "  ";
            for (PackageNode pkg : packageNode.packages)
                printTree(pkg, indent2);
            for (ClassNode cls : packageNode.classes)
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
     * A class to hold package info.
     */
    public static class ClassTreeNode {

        // The parent package
        public PackageNode  parentPackage;

        // The package full name
        public final String  fullName;

        // The package simple name
        public final String  simpleName;

        /** Constructor. */
        public ClassTreeNode(PackageNode aParentPackage, String aPackageName)
        {
            parentPackage = aParentPackage;
            fullName = aPackageName;
            simpleName = getSimpleNodeName(aPackageName);
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
            StringBuffer sb = new StringBuffer();
            StringUtils.appendProp(sb, "FullName", fullName);
            StringUtils.appendProp(sb, "SimpleName", simpleName);
            if (parentPackage != null)
                StringUtils.appendProp(sb, "Parent", parentPackage.fullName);
            return sb.toString();
        }
    }

    /**
     * A class to hold package info.
     */
    public static class PackageNode extends ClassTreeNode {

        // Child packages
        public PackageNode[]  packages = EMPTY_PACKAGE_ARRAY;

        // Child classes
        public ClassNode[]  classes = EMPTY_CLASS_ARRAY;

        /** Constructor. */
        public PackageNode(PackageNode parentPackage, String aPackageName)
        {
            super(parentPackage, aPackageName);
        }

        /**
         * Standard toStringProps implementation.
         */
        public String toStringProps()
        {
            String superProps = super.toStringProps();
            StringBuffer sb = new StringBuffer(superProps);
            if (packages.length > 0) {
                String[] pkgNames = Stream.of(packages).map(p -> p.simpleName).toArray(size -> new String[size]);
                StringUtils.appendProp(sb, "Packages", Arrays.toString(pkgNames));
            }
            if (classes.length > 0) {
                String[] classNames = Stream.of(classes).map(p -> p.simpleName).toArray(size -> new String[size]);
                StringUtils.appendProp(sb, "Classes", Arrays.toString(classNames));
            }
            return sb.toString();
        }
    }

    /**
     * A class to hold class info.
     */
    public static class ClassNode extends ClassTreeNode {

        /** Constructor. */
        public ClassNode(PackageNode parentPackage, String aClassName)
        {
            super(parentPackage, aClassName);
        }
    }
}

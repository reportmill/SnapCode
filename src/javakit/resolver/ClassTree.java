/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;

import java.util.*;

/**
 * Represents a tree of packages/classes.
 */
public class ClassTree {

    // The root package
    private ClassTreeNode _rootPackage;

    // The packages
    private Map<String, ClassTreeNode>  _packages = new HashMap<>();

    // All classes
    private ClassTreeNode[]  _allClasses;

    /**
     * Constructor.
     */
    public ClassTree()
    {
        super();

        // Create RootPackage
        _rootPackage = new ClassTreeNode(null, "", true);
        _packages.put("", _rootPackage);

        // Add primitives
        _rootPackage.classes = new ClassTreeNode[] {
                new ClassTreeNode(_rootPackage, boolean.class.getName(), false),
                new ClassTreeNode(_rootPackage, char.class.getName(), false),
                new ClassTreeNode(_rootPackage, byte.class.getName(), false),
                new ClassTreeNode(_rootPackage, short.class.getName(), false),
                new ClassTreeNode(_rootPackage, int.class.getName(), false),
                new ClassTreeNode(_rootPackage, long.class.getName(), false),
                new ClassTreeNode(_rootPackage, float.class.getName(), false),
                new ClassTreeNode(_rootPackage, double.class.getName(), false)
        };
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
        packageNode = new ClassTreeNode(parentNode, aName, true);
        parentNode.packages = ArrayUtils.add(parentNode.packages, packageNode);

        // Set, return
        _packages.put(aName, packageNode);
        return packageNode;
    }

    /**
     * Returns all classes.
     */
    public ClassTreeNode[] getAllClasses()
    {
        // If already set, just return
        if (_allClasses != null) return _allClasses;

        // Get all classes
        ClassTreeNode rootPackage = getRootPackage();
        List<ClassTreeNode> allClasses = new ArrayList<>();
        getAllClassesForPackageDeep(rootPackage, allClasses);

        // Set, return
        return _allClasses = allClasses.toArray(new ClassTreeNode[0]);
    }

    /**
     * Returns all classes.
     */
    private void getAllClassesForPackageDeep(ClassTreeNode aPackage, List<ClassTreeNode> allClassesList)
    {
        // Add all package classes to list
        Collections.addAll(allClassesList, aPackage.classes);

        // Recurse for each child package
        for (ClassTreeNode childPkg : aPackage.packages)
            getAllClassesForPackageDeep(childPkg, allClassesList);
    }

    /**
     * Returns an array of most common classes.
     */
    public ClassTreeNode[] getCommonClasses()
    {
        ClassTree webClassTree = ClassTreeWeb.getShared();
        ClassTreeNode[] commonClasses = webClassTree.getAllClasses();
        return commonClasses;
    }

    /**
     * Prints the tree.
     */
    protected void printTree(ClassTreeNode aNode, String indent)
    {
        System.out.println(indent + aNode.simpleName);
        if (aNode.isPackage()) {
            String indent2 = indent + "  ";
            for (ClassTreeNode pkg : aNode.packages)
                printTree(pkg, indent2);
            for (ClassTreeNode cls : aNode.classes)
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
}

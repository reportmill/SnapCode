/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;

/**
 * This ClassTree subclass loads classes from static array.
 */
public class ClassTreeWeb extends ClassTree {

    // A shared instance
    private static ClassTree  _shared;

    /**
     * Constructor.
     */
    public ClassTreeWeb()
    {
        super();

        for (String className : COMMON_CLASS_NAMES)
            addClassName(className);
    }

    /**
     * Returns the shared instance.
     */
    public static ClassTree getShared()
    {
        if (_shared != null) return _shared;
        ClassTreeWeb classTreeWeb = new ClassTreeWeb();
        return _shared = classTreeWeb;
    }

    /**
     * Adds a class name.
     */
    protected void addClassName(String aName)
    {
        String parentName = getParentNodeName(aName);
        PackageNode parent = getPackageForName(parentName);
        ClassNode classNode = new ClassNode(parent, aName);
        parent.classes = ArrayUtils.add(parent.classes, classNode);
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
            "snap.view.Button", "snap.view.Label", "snap.view.View", "snap.view.ViewOwner"
    };
}

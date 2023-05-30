/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utility methods to match nodes to Java declarations.
 */
public class NodeMatcher {

    /**
     * Returns reference nodes in given JNode that match given JavaDecl.
     */
    public static void getMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            JavaDecl decl = isPossibleMatch(aNode, aDecl) ? aNode.getDecl() : null;
            if (decl != null && aDecl.matches(decl))
                theMatches.add(aNode);
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getMatches(child, aDecl, theMatches);
    }

    /**
     * Returns reference nodes in given JNode that match given JavaDecl.
     */
    public static void getRefMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            if (isPossibleMatch(aNode, aDecl) && !aNode.isDecl()) {
                JavaDecl decl = aNode.getDecl();
                if (decl != null && aDecl.matches(decl) && aNode.getParent(JImportDecl.class) == null)
                    theMatches.add(aNode);
            }
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getRefMatches(child, aDecl, theMatches);
    }

    /**
     * Returns declaration nodes in given JNode that match given JavaDecl.
     */
    public static JNode getDeclMatch(JNode aNode, JavaDecl aDecl)
    {
        List<JNode> matches = new ArrayList<>();
        getDeclMatches(aNode, aDecl, matches);
        return matches.size() > 0 ? matches.get(0) : null;
    }

    /**
     * Returns declaration nodes in given JNode that match given JavaDecl.
     */
    public static void getDeclMatches(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JType check name
        if (aNode instanceof JType || aNode instanceof JExprId) {
            JavaDecl decl = aNode.isDecl() && isPossibleMatch(aNode, aDecl) ? aNode.getDecl() : null;
            if (decl != null && aDecl.matches(decl))
                theMatches.add(aNode);
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            getDeclMatches(child, aDecl, theMatches);
    }

    /**
     * Returns whether node is a possible match.
     */
    private static boolean isPossibleMatch(JNode aNode, JavaDecl aDecl)
    {
        // If Node is type and Decl is type and Decl.SimpleName contains Node.SimpleName
        if (aNode instanceof JType && aDecl instanceof JavaType) {
            JType type = (JType) aNode;
            String typeName = type.getSimpleName();
            JavaType javaType = (JavaType) aDecl;
            String javaTypeName = javaType.getSimpleName();
            return javaTypeName.contains(typeName);
        }

        // If Node is identifier and Decl.Name contains Node.Name
        if (aNode instanceof JExprId)
            return aDecl.getName().contains(aNode.getName());

        return false;
    }

    /**
     * Returns whether a JavaDecl is expected.
     */
    public static boolean isDeclExpected(JNode aNode)
    {
        if(aNode instanceof JExprLiteral)
            return !((JExprLiteral) aNode).isNull();

        try {
            return aNode.getClass().getDeclaredMethod("getDeclImpl") != null;
        }
        catch(Exception e) { return false; }
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import snap.util.ArrayUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utility methods to match nodes to Java declarations.
 */
public class NodeMatcher {

    /**
     * Returns matching nodes for given decl.
     */
    public static JNode[] getMatchingNodesForDecl(JNode aNode, JavaDecl aDecl)
    {
        List<JNode> matchingNodes = new ArrayList<>();
        findMatchingNodesForDecl(aNode, aDecl, matchingNodes);
        return matchingNodes.toArray(new JNode[0]);
    }

    /**
     * Finds matching nodes in given JNode that match given JavaDecl.
     */
    private static void findMatchingNodesForDecl(JNode aNode, JavaDecl aDecl, List<JNode> theMatches)
    {
        // If JExprId, check for match
        if (aNode instanceof JExprId) {
            if (isPossibleMatch(aNode, aDecl)) {
                JavaDecl decl = aNode.getDecl();
                if (decl != null && aDecl.matches(decl))
                    theMatches.add(aNode);
            }
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            findMatchingNodesForDecl(child, aDecl, theMatches);
    }

    /**
     * Returns nodes that reference given decl.
     */
    public static JNode[] getReferenceNodesForDecl(JNode aNode, JavaDecl aDecl)
    {
        JNode[] matchingNodex = getMatchingNodesForDecl(aNode, aDecl);
        return ArrayUtils.filter(matchingNodex, node -> isReferenceNode(aNode));
    }

    /**
     * Returns whether given node is a reference node.
     */
    private static boolean isReferenceNode(JNode aNode)
    {
        return !aNode.isDeclIdNode() && aNode.getParent(JImportDecl.class) == null;
    }

    /**
     * Returns the declaration node for given decl.
     */
    public static JNode getDeclarationNodeForDecl(JNode aNode, JavaDecl aDecl)
    {
        JNode[] matches = getDeclarationNodesForDecl(aNode, aDecl);
        return matches.length > 0 ? matches[0] : null;
    }

    /**
     * Returns nodes that are declarations or subclass declarations of given decl.
     */
    public static JNode[] getDeclarationNodesForDecl(JNode aNode, JavaDecl aDecl)
    {
        JNode[] matchingNodex = getMatchingNodesForDecl(aNode, aDecl);
        return ArrayUtils.filter(matchingNodex, node -> node.isDeclIdNode());
    }

    /**
     * Returns whether node is a possible match.
     */
    private static boolean isPossibleMatch(JNode aNode, JavaDecl aDecl)
    {
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

        try { aNode.getClass().getDeclaredMethod("getDeclImpl"); }
        catch(Exception e) { return false; }
        return true;
    }
}

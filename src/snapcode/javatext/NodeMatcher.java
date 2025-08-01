/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaLocalVar;
import snap.util.ArrayUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utility methods to match nodes to Java declarations.
 */
public class NodeMatcher {

    /**
     * Returns matching id expression nodes for given id node.
     */
    public static JExprId[] getMatchingIdNodesForIdNode(JExprId idExpr)
    {
        // Get decl - if null, return empty array
        JavaDecl nodeDecl = idExpr.getDecl();
        if (nodeDecl == null)
            return new JExprId[0];

        // Get root node
        JNode rootNode = getRootNodeForId(idExpr, nodeDecl);

        // Return matching nodes
        return getMatchingIdNodesForDecl(rootNode, nodeDecl);
    }

    /**
     * Returns the root node for given id node and decl.
     */
    private static JNode getRootNodeForId(JExprId idExpr, JavaDecl decl)
    {
        // If local var, return parent block that holds it
        if (decl instanceof JavaLocalVar) {
            JVarDecl varDecl = idExpr.getVarDecl();
            JNode declBlock = (JNode) varDecl.getParent(WithVarDecls.class);
            while (declBlock instanceof JExpr)
                declBlock = (JNode) declBlock.getParent(WithVarDecls.class);
            if (declBlock != null) // Probably not possible that this is null
                return declBlock;
        }

        // Return file
        return idExpr.getFile();
    }

    /**
     * Returns matching id expression nodes for given decl.
     */
    private static JExprId[] getMatchingIdNodesForDecl(JNode rootNode, JavaDecl aDecl)
    {
        List<JExprId> matchingNodes = new ArrayList<>();
        findMatchingIdNodesForDecl(rootNode, aDecl, matchingNodes);
        return matchingNodes.toArray(new JExprId[0]);
    }

    /**
     * Finds matching id expression nodes in given JNode that match given JavaDecl.
     */
    private static void findMatchingIdNodesForDecl(JNode aNode, JavaDecl aDecl, List<JExprId> matchingIdNodes)
    {
        // If JExprId, check for match
        if (aNode instanceof JExprId) {
            if (isPossibleMatch(aNode, aDecl)) {
                JavaDecl decl = aNode.getDecl();
                if (decl != null && aDecl.matches(decl))
                    matchingIdNodes.add((JExprId) aNode);
            }
        }

        // Recurse
        for (JNode child : aNode.getChildren())
            findMatchingIdNodesForDecl(child, aDecl, matchingIdNodes);
    }

    /**
     * Returns nodes that reference given decl.
     */
    public static List<JExprId> getReferenceNodesForDecl(JFile jfile, JavaDecl aDecl)
    {
        JExprId[] matchingIdNodes = getMatchingIdNodesForDecl(jfile, aDecl);
        return ArrayUtils.filterToList(matchingIdNodes, node -> isReferenceNode(node));
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
    public static JNode getDeclarationNodeForDecl(JFile jfile, JavaDecl aDecl)
    {
        List<JExprId> matches = getDeclarationNodesForDecl(jfile, aDecl);
        return !matches.isEmpty() ? matches.get(0) : null;
    }

    /**
     * Returns nodes that are declarations or subclass declarations of given decl.
     */
    public static List<JExprId> getDeclarationNodesForDecl(JFile jfile, JavaDecl aDecl)
    {
        JExprId[] matchingIdNodes = getMatchingIdNodesForDecl(jfile, aDecl);
        return ArrayUtils.filterToList(matchingIdNodes, node -> node.isDeclIdNode());
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

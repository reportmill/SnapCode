/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaLocalVar;
import java.util.*;

/**
 * This class provides utility methods to match nodes to Java declarations.
 */
public class NodeMatcher {

    /**
     * Returns matching id expression nodes for given id node.
     */
    public static List<JExprId> getMatchingIdNodesForIdNode(JExprId idExpr)
    {
        // Get decl - if null, return empty array
        JavaDecl nodeDecl = idExpr.getDecl();
        if (nodeDecl == null)
            return Collections.emptyList();

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
    private static List<JExprId> getMatchingIdNodesForDecl(JNode rootNode, JavaDecl aDecl)
    {
        List<JExprId> matchingNodes = new ArrayList<>();
        findMatchingIdNodesForDecl(rootNode, aDecl, matchingNodes);
        return matchingNodes;
    }

    /**
     * Finds matching id expression nodes in given JNode that match given JavaDecl.
     */
    private static void findMatchingIdNodesForDecl(JNode aNode, JavaDecl aDecl, List<JExprId> matchingIdNodes)
    {
        // If JExprId with matching name, check for actual match
        if (aNode instanceof JExprId idExpr && aDecl.getName().contains(idExpr.getName())) {
            JavaDecl decl = idExpr.getDecl();
            if (decl != null && aDecl.matches(decl))
                matchingIdNodes.add(idExpr);
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
        List<JExprId> matchingIdNodes = getMatchingIdNodesForDecl(jfile, aDecl);
        return matchingIdNodes.stream().filter(NodeMatcher::isReferenceNode).toList();
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
        List<JExprId> matchingIdNodes = getMatchingIdNodesForDecl(jfile, aDecl);
        return matchingIdNodes.stream().filter(JExprId::isDeclIdNode).toList();
    }

    /**
     * Returns whether a JavaDecl is expected.
     */
    public static boolean isDeclExpected(JNode aNode)
    {
        if(aNode instanceof JExprLiteral literalExpr)
            return !literalExpr.isNull();
        if (aNode instanceof JExprSwitch)
            return !(aNode.getParent() instanceof JStmtSwitch);

        try { aNode.getClass().getDeclaredMethod("getDeclImpl"); }
        catch(Exception e) { return false; }
        return true;
    }
}

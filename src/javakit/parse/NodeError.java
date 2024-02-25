/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an error in a JNode.
 */
public class NodeError {

    // The Node that holds error
    private JNode  _node;

    // The error message
    private String  _string;

    // Constant for no errors
    public static final NodeError[] NO_ERRORS = new NodeError[0];

    /**
     * Constructor.
     */
    public NodeError(JNode aNode, String aString)
    {
        _node = aNode;
        _string = aString;
    }

    /**
     * Returns the node that holds this error.
     */
    public JNode getNode()  { return _node; }

    /**
     * Returns the error message.
     */
    public String getString()  { return _string; }


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
        StringUtils.appendProp(sb, "Node", _node.getClass().getSimpleName());
        StringUtils.appendProp(sb, "String", getString());
        return sb.toString();
    }

    /**
     * Creates and returns an error in array.
     */
    public static NodeError[] newErrorArray(JNode aNode, String errorMessage)
    {
        return new NodeError[] { new NodeError(aNode, errorMessage) };
    }

    /**
     * Creates and returns an error in array.
     */
    public static NodeError[] addError(NodeError[] theErrors, JNode aNode, String errorMessage)
    {
        return addError(theErrors, aNode, errorMessage, theErrors.length);
    }

    /**
     * Creates and returns an error in array.
     */
    public static NodeError[] addError(NodeError[] theErrors, JNode aNode, String errorMessage, int index)
    {
        NodeError error = new NodeError(aNode, errorMessage);
        return ArrayUtils.add(theErrors, error, index);
    }

    /**
     * Return all node errors in given node.
     */
    public static NodeError[] getAllNodeErrors(JNode aNode)
    {
        List<NodeError> errorsList = new ArrayList<>();
        NodeError.findAllNodeErrors(aNode, errorsList);
        return errorsList.toArray(NodeError.NO_ERRORS);
    }

    /**
     * Find all node errors in given node and adds to given list.
     */
    public static void findAllNodeErrors(JNode aNode, List<NodeError> errorsList)
    {
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(errorsList, errors);

        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findAllNodeErrors(child, errorsList);
    }

    /**
     * Returns the node error for a JFile parse exception.
     */
    public static NodeError[] getNodeErrorForFileParseException(JFile jfile)
    {
        // Get exception - just return null if none
        Exception exception = jfile.getException();
        if (exception == null)
            return null;

        // Get last node
        JNode lastNode = getLastNodeForNode(jfile);
        if (lastNode == null || lastNode.getStartToken() == null)
            return null;

        // Get message
        String msg = exception.getMessage();
        if (jfile.isRepl() && msg.contains("Expecting: \";"))
            return null;

        // Return error
        return newErrorArray(lastNode, msg);
    }

    /**
     * Returns the last node for given node.
     */
    private static JNode getLastNodeForNode(JNode aNode)
    {
        JNode lastNode = aNode;
        while (lastNode.getChildCount() > 0)
            lastNode = lastNode.getChild(lastNode.getChildCount() - 1);
        return lastNode;
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import snap.util.ArrayUtils;
import snap.util.StringUtils;

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
}

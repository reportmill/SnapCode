/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

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
}

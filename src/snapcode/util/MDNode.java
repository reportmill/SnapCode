package snapcode.util;

/**
 * This class represents a Markdown node.
 */
public class MDNode {

    // The node type
    private NodeType _nodeType;

    // The text
    private String _text;

    // Child nodes
    private MDNode[] _childNodes;

    // Constants for node type
    public enum NodeType { Root, Header1, Header2, Content, Code }

    /**
     * Constructor.
     */
    public MDNode(NodeType aType, String theText)
    {
        _nodeType = aType;
        _text = theText;
    }

    /**
     * Returns the node type.
     */
    public NodeType getNodeType()  { return _nodeType; }

    /**
     * Returns the text.
     */
    public String getText()  { return _text; }

    /**
     * Returns the child nodes.
     */
    public MDNode[] getChildNodes()  { return _childNodes; }

    /**
     * Sets the child nodes.
     */
    public void setChildNodes(MDNode[] nodesArray)
    {
        _childNodes = nodesArray;
    }
}

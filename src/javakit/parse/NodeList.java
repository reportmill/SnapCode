package javakit.parse;
import snap.util.ArrayUtils;
import java.util.AbstractList;
import java.util.Arrays;

/**
 * This class holds a list of nodes.
 */
public class NodeList extends AbstractList<JNode> {

    // The nodes array
    private JNode[] _nodes = EMPTY_NODES_ARRAY;

    // The array length
    protected int _size;

    // Shared empty node list
    public static NodeList EMPTY_NODES = new NodeList();

    // Shared empty nodes array
    private static JNode[] EMPTY_NODES_ARRAY = new JNode[0];

    /**
     * Constructor.
     */
    public NodeList()
    {
        super();
    }

    /**
     * Returns the number of node in this list.
     */
    @Override
    public int size()  { return _size; }

    /**
     * Returns the node at the given index.
     */
    @Override
    public JNode get(int anIndex)  { return _nodes[anIndex]; }

    /**
     * Sets the String value at index.
     */
    @Override
    public JNode set(int anIndex, JNode aValue)
    {
        JNode oldValue = _nodes[anIndex];
        _nodes[anIndex] = aValue;
        return oldValue;
    }

    /**
     * Adds the value at index.
     */
    @Override
    public void add(int anIndex, JNode aValue)
    {
        // Expand components array if needed
        if (_size == _nodes.length)
            _nodes = Arrays.copyOf(_nodes, Math.max(_nodes.length * 2, 4));

        // If index is inside current length, shift existing elements over
        if (anIndex < _size)
            System.arraycopy(_nodes, anIndex, _nodes, anIndex + 1, _size - anIndex);

        // Set value and increment length
        _nodes[anIndex] = aValue;
        _size++;
    }

    /**
     * Removes the item at index.
     */
    @Override
    public JNode remove(int anIndex)
    {
        JNode oldValue = _nodes[anIndex];

        // Shift remaining elements in
        System.arraycopy(_nodes, anIndex + 1, _nodes, anIndex, _size - anIndex - 1);
        _size--;

        // Return
        return oldValue;
    }

    /**
     * Returns whether array is empty.
     */
    @Override
    public boolean isEmpty()  { return _size == 0; }

    /**
     * Returns the simple array (trimmed to length).
     */
    public JNode[] getArray()
    {
        if (_size != _nodes.length)
            _nodes = Arrays.copyOf(_nodes, _size);
        return _nodes;
    }

    /**
     * Returns the index of the given child in this node's children list.
     */
    public int indexOf(JNode aNode)  { return ArrayUtils.indexOfId(_nodes, aNode); }

    /**
     * Returns the last node of this list.
     */
    public JNode getFirst()  { return _size > 0 ? get(0) : null; }

    /**
     * Returns the last node of this list.
     */
    public JNode getLast()  { return _size > 0 ? get(_size - 1) : null; }
}

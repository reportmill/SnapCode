package snapcode.apptools;
import com.sun.jdi.*;
import snap.view.TreeResolver;
import snapcode.debug.DebugApp;
import snapcode.debug.ExprEval;

import java.util.*;

/**
 * A class to hold a debug variable or expression.
 */
public class DebugVarItem implements Comparable<DebugVarItem> {

    // Variable
    protected String _name;

    // The value
    protected Object _value;

    // The parent item
    protected DebugVarItem _parent;

    // The children
    protected List<DebugVarItem> _children;

    /**
     * Constructor.
     */
    private DebugVarItem(String aName, Object aValue)
    {
        _name = aName;
        _value = aValue;
    }

    /**
     * Returns the parent item.
     */
    public DebugVarItem getParent()  { return _parent; }

    /**
     * Returns name.
     */
    public String getName()  { return _name; }

    /**
     * Returns the value.
     */
    public Object getValue()  { return _value; }

    /**
     * Return value.
     */
    public String getValueString()
    {
        // Handle string
        if (_value instanceof StringReference stringRef)
            return stringRef.value();

        // Handle array
        if (_value instanceof ArrayReference arrayRef) {
            ReferenceType refType = arrayRef.referenceType();
            String name = refType.name();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0)
                name = name.substring(dotIndex + 1);
            return String.format("%s[%d] (id=%d)", name, arrayRef.length(), arrayRef.uniqueID());
        }

        // Handle object
        if (_value instanceof ObjectReference objRef) {
            ReferenceType refType = objRef.referenceType();
            String name = refType.name();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0)
                name = name.substring(dotIndex + 1);
            return String.format("%s (id=%d)", name, objRef.uniqueID());
        }

        // Handle null or exception
        return _value != null ? _value.toString() : "(null)";
    }

    /**
     * Return value class.
     */
    public String getVarClass()
    {
        if (_value instanceof ObjectReference objRef)
            return objRef.referenceType().name();
        return _value != null ? _value.getClass().getName() : null;
    }

    /**
     * Override to make primitive values leafs.
     */
    public boolean isParent()
    {
        Value value = _value instanceof Value ? (Value) _value : null;
        boolean isLeaf = value == null || value.type() instanceof PrimitiveType || _value instanceof StringReference;
        return !isLeaf;
    }

    /**
     * Override to get ObjectReference children.
     */
    public List<DebugVarItem> getChildren()
    {
        if (_children != null) return _children;

        ObjectReference objRef = (ObjectReference) _value;
        ReferenceType refType = objRef.referenceType();
        List<DebugVarItem> varItems = new ArrayList<>();

        // Handle Arrays
        if (objRef instanceof ArrayReference arrayRef) {
            int i = 0;
            for (Value value : arrayRef.getValues())
                varItems.add(new DebugVarItem("[" + (i++) + ']', value));
        }

        // Handle anything else: Iterate over fields for ReferenceType to create VarTableItems
        else {
            List<Field> fields = refType.allFields();
            for (Field field : fields) {
                if (field.isStatic() || field.isEnumConstant())
                    continue;
                varItems.add(DebugVarItem.createItemForObjectField(objRef, field));
            }
            Collections.sort(varItems);
        }

        // Set parents and return
        varItems.forEach(varItem -> varItem._parent = this);
        return _children = varItems;
    }

    /**
     * Returns the result of calling toString() on given debugger object.
     */
    public String invokeToStringMethod(DebugApp debugApp)
    {
        if (_value instanceof Value value)
            return debugApp.toString(value);
        return _value != null ? _value.toString() : "(null)";
    }

    /**
     * Makes var item re-evaluate expression.
     */
    public void evaluateExpression(DebugApp debugApp)
    {
        _children = null;

        try { _value = debugApp != null ? ExprEval.eval(debugApp, _name) : null; }
        catch (Exception e) { _value = e; }
    }

    /**
     * Comparable.
     */
    public int compareTo(DebugVarItem anItem)
    {
        if (_name == "this") return -1;
        if (anItem._name == "this") return 1;
        return _name.compareTo(anItem._name);
    }

    /**
     * Creates a var item for given object field.
     */
    public static DebugVarItem createItemForObjectField(ObjectReference objRef, Field field)
    {
        try {
            Value value = objRef.getValue(field);
            return new DebugVarItem(field.name(), value);
        }
        catch (Exception e) { return new DebugVarItem(e.toString(), null); }
    }

    /**
     * Creates a var item for given expression.
     */
    public static DebugVarItem createItemForExpression(String exprName)
    {
        return new DebugVarItem(exprName, null);
    }

    /**
     * Creates a var item for given frame local variable.
     */
    public static DebugVarItem createItemForLocalVariable(StackFrame frame, LocalVariable localVar)
    {
        try {
            String name = localVar.name();
            Value value = frame.getValue(localVar);
            return new DebugVarItem(name, value);
        }
        catch (Exception e) { return new DebugVarItem(e.toString(), null); }
    }

    /**
     * Creates a var items for given frame.
     */
    public static List<DebugVarItem> createItemsForFrame(StackFrame frame)
    {
        List<DebugVarItem> varItems = new ArrayList<>();

        // Add Frame.ThisObject
        ObjectReference objRef = frame.thisObject();
        if (objRef != null)
            varItems.add(new DebugVarItem("this", objRef));

        // Get Local Variables and add
        List<LocalVariable> localVars;
        try { localVars = frame.visibleVariables(); }
        catch (Exception e) {
            System.err.println(e.getMessage());
            localVars = Collections.emptyList();
        }

        for (LocalVariable localVar : localVars)
            varItems.add(DebugVarItem.createItemForLocalVariable(frame, localVar));

        // Sort and return
        Collections.sort(varItems);
        return varItems;
    }

    /**
     * A TreeResolver for DebugVarItem.
     */
    public static class VarTreeResolver extends TreeResolver<DebugVarItem> {

        /**
         * Returns the parent of given item.
         */
        public DebugVarItem getParent(DebugVarItem anItem)  { return anItem._parent; }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(DebugVarItem anItem)  { return anItem.isParent(); }

        /**
         * Returns the children.
         */
        public List<DebugVarItem> getChildren(DebugVarItem aParent)  { return aParent.getChildren(); }
    }
}

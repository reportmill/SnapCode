package snapcode.apptools;
import com.sun.jdi.*;
import snapcode.app.WorkspaceTool;
import snapcode.debug.DebugApp;
import snap.gfx.Font;
import snap.view.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This view owner class manages showing variables for current stack frame in the debugger.
 */
public class DebugVarsPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool _debugTool;

    // The variable tree
    private TreeView<VarTreeItem> _varsTree;

    // Whether VarsTree needs reset
    private boolean _resetVarsTree = true;

    /**
     * Constructor.
     */
    public DebugVarsPane(DebugTool debugTool)
    {
        super(debugTool.getWorkspacePane());
        _debugTool = debugTool;
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getDebugApp()  { return _debugTool.getSelDebugApp(); }

    /**
     * Create UI.
     */
    protected void initUI()
    {
        // Get VarsTree
        _varsTree = getView("TreeView", TreeView.class);
        _varsTree.setFont(Font.Arial11);
        _varsTree.setResolver(new VarTreeResolver());

        // Configure "Name" column
        TreeCol<VarTreeItem> nameCol = _varsTree.getCol(0);
        nameCol.setHeaderText("Name");
        nameCol.setPrefWidth(100);

        // Add "Value" column
        TreeCol<VarTreeItem> valueCol = new TreeCol<>();
        valueCol.setHeaderText("Value");
        valueCol.setPrefWidth(100);
        valueCol.setGrowWidth(true);
        _varsTree.addCol(valueCol);

        // Add "Type" column
        TreeCol<VarTreeItem> typeCol = new TreeCol<>();
        typeCol.setHeaderText("Type");
        typeCol.setPrefWidth(100);
        typeCol.setGrowWidth(true);
        _varsTree.addCol(typeCol);
    }

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Reset VarTable.Items
        if (_resetVarsTree) {
            _resetVarsTree = false;
            List<VarTreeItem> varItems = createVarItems();
            _varsTree.setItems(varItems);
        }

        // Update VarText
        VarTreeItem selVarTreeItem = _varsTree.getSelItem();
        if (selVarTreeItem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String varValueStr = selVarTreeItem.getValueToString();
            setViewText("TextView", varValueStr != null ? varValueStr : "(null)");
        }
        else setViewText("TextView", "");
    }

    /**
     * Called to notify that VarsTree needs reset from DebugApp.
     */
    protected void resetVarTable()
    {
        if (!isUISet()) return;
        _resetVarsTree = true;
        resetLater();
    }

    /**
     * Creates Var items.
     */
    private List<VarTreeItem> createVarItems()
    {
        // Get DebugApp and current frame
        DebugApp debugApp = getDebugApp();
        if (debugApp == null)
            return Collections.EMPTY_LIST;
        StackFrame frame = debugApp.getCurrentFrame();
        if (frame == null)
            return Collections.EMPTY_LIST;
        List<VarTreeItem> varItems = new ArrayList<>();

        // Add Frame.ThisObject
        ObjectReference objRef = frame.thisObject();
        if (objRef != null)
            varItems.add(new VarTreeItem(debugApp, "this", objRef));

        // Get Local Variables and add
        List<LocalVariable> localVars;
        try { localVars = frame.visibleVariables(); }
        catch (Exception e) {
            System.err.println(e.getMessage());
            localVars = Collections.emptyList();
        }

        for (LocalVariable localVar : localVars) {
            VarTreeItem varItem;
            try {
                String name = localVar.name();
                Value value = frame.getValue(localVar);
                varItem = new VarTreeItem(debugApp, name, value);
            }
            catch (Exception e) { varItem = new VarTreeItem(debugApp, e.toString(), null); }
            varItems.add(varItem);
        }

        // Sort and return
        Collections.sort(varItems);
        return varItems;
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Variables"; }

    /**
     * A class to hold a VarTree Variable.
     */
    public static class VarTreeItem implements Comparable<VarTreeItem> {

        // Ivars
        DebugApp _app;
        String _name;
        Object _value;
        VarTreeItem _parent;
        List<VarTreeItem> _children;

        /**
         * Create VarTableItem.
         */
        public VarTreeItem(DebugApp anApp, String aName, Object aValue)
        {
            _app = anApp;
            _name = aName;
            _value = aValue;
        }

        /**
         * Return the debug app.
         */
        public DebugApp getApp()  { return _app; }

        /**
         * Returns the parent item.
         */
        public VarTreeItem getParent()  { return _parent; }

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
            if (_value instanceof ArrayReference arrayRef) {
                ReferenceType refType = arrayRef.referenceType();
                String name = refType.name();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0)
                    name = name.substring(dotIndex + 1);
                return String.format("%s[%d] (id=%d)", name, arrayRef.length(), arrayRef.uniqueID());
            }

            if (_value instanceof StringReference stringRef)
                return stringRef.value();

            if (_value instanceof ObjectReference objRef) {
                ReferenceType refType = objRef.referenceType();
                String name = refType.name();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0)
                    name = name.substring(dotIndex + 1);
                return String.format("%s (id=%d)", name, objRef.uniqueID());
            }

            return _value != null ? _value.toString() : "(null)";
        }

        /**
         * Return value.
         */
        public String getValueToString()
        {
            if (_value instanceof Value)
                return getApp().toString((Value) _value);
            return _value != null ? _value.toString() : "(null)";
        }

        /**
         * Return value class.
         */
        public String getVarClass()
        {
            if (_value instanceof ObjectReference) return ((ObjectReference) _value).referenceType().name();
            Object v = getValue();
            return v != null ? v.getClass().getName() : null;
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
        public List<VarTreeItem> getChildren()
        {
            if (_children != null) return _children;

            ObjectReference objRef = (ObjectReference) _value;
            ReferenceType refType = objRef.referenceType();
            List<VarTreeItem> varItems = new ArrayList<>();

            // Handle Arrays
            if (objRef instanceof ArrayReference arrayRef) {
                int i = 0;
                for (Value value : arrayRef.getValues())
                    varItems.add(new VarTreeItem(getApp(), "[" + (i++) + ']', value));
            }

            // Handle anything else: Iterate over fields for ReferenceType to create VarTableItems
            else {
                List<Field> fields = refType.allFields();
                for (Field field : fields) {
                    if (field.isStatic() || field.isEnumConstant())
                        continue;
                    VarTreeItem varItem;
                    try {
                        Value value = objRef.getValue(field);
                        varItem = new VarTreeItem(getApp(), field.name(), value);
                    }
                    catch (Exception e) { varItem = new VarTreeItem(getApp(), e.toString(), null); }
                    varItems.add(varItem);
                }
                Collections.sort(varItems);
            }

            // Get array, set parents and return
            varItems.forEach(varItem -> varItem._parent = this);
            return _children = varItems;
        }

        /**
         * Comparable.
         */
        public int compareTo(VarTreeItem anItem)
        {
            if (_name == "this") return -1;
            if (anItem._name == "this") return 1;
            return _name.compareTo(anItem._name);
        }

        /** Standard Equals implementation. */
        //public boolean equals(Object anObj) {
        //    VarTreeItem other = anObj instanceof VarTreeItem? (VarTreeItem)anObj : null; if(other==null) return false;
        //    if(!Objects.equals(_name,other._name)) return false; return Objects.equals(_value, other._value);  }
        /** Standard hashCode implementation. */
        //public int hashCode()  { return (_name!=null? _name.hashCode() : 0) + (_value!=null? _value.hashCode() : 0); }
    }

    /**
     * A TreeResolver for VarTreeItem.
     */
    public static class VarTreeResolver extends TreeResolver<VarTreeItem> {

        /**
         * Returns the parent of given item.
         */
        public VarTreeItem getParent(VarTreeItem anItem)  { return anItem._parent; }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(VarTreeItem anItem)  { return anItem.isParent(); }

        /**
         * Returns the children.
         */
        public List<VarTreeItem> getChildren(VarTreeItem aParent)  { return aParent.getChildren(); }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(VarTreeItem anItem, int aCol)
        {
            if (aCol == 0)
                return anItem.getName();
            if (aCol == 1)
                return anItem.getValueString();
            return anItem.getVarClass();
        }
    }
}
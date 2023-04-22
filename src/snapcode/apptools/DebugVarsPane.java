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
 * A debug pane.
 */
public class DebugVarsPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool  _debugTool;

    // The variable table
    private TreeView<VarTreeItem>  _varTree;

    // The variable text
    private TextView  _varText;

    // Whether to reset VarTable
    private boolean  _resetVarTable = true;

    /**
     * Creates a new DebugVarsPane.
     */
    public DebugVarsPane(DebugTool debugTool)
    {
        super(debugTool.getWorkspacePane());
        _debugTool = debugTool;
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getDebugApp()
    {
        return _debugTool.getSelDebugApp();
    }

    /**
     * Create UI.
     */
    protected void initUI()
    {
        // Create VarTree and configure
        _varTree = getView("TreeView", TreeView.class);
        _varTree.setFont(Font.Arial11);
        TreeCol c0 = _varTree.getCol(0);
        c0.setHeaderText("Name");
        c0.setPrefWidth(100);
        TreeCol c1 = new TreeCol();
        c1.setHeaderText("Value");
        c1.setPrefWidth(100);
        c1.setGrowWidth(true);
        TreeCol c2 = new TreeCol();
        c2.setHeaderText("Type");
        c2.setPrefWidth(100);
        c2.setGrowWidth(true);
        _varTree.addCols(c1, c2);
        _varTree.setResolver(new VarTreeResolver());

        // Create VarText TextView and configure in ScrollView
        _varText = getView("TextView", TextView.class);
        _varText.setWrapLines(true);
        _varText.getScrollView().setBorder(null);
    }

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Reset VarTable.Items
        if (_resetVarTable) {
            _resetVarTable = false;
            List vitems = createVarItems();
            _varTree.setItems(vitems);
        }

        // Update VarText
        VarTreeItem vitem = _varTree.getSelItem();
        if (vitem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String pvalue = vitem.getValueToString();
            _varText.setText(pvalue != null ? pvalue : "(null)");
        } else _varText.setText("");
    }

    /**
     * Tells table to update model from DebugApp.
     */
    void resetVarTable()
    {
        if (isUISet()) {
            _resetVarTable = true;
            resetLater();
        }
    }

    /**
     * Creates Var items.
     */
    List createVarItems()
    {
        // Get DebugApp and current frame
        DebugApp dapp = getDebugApp();
        if (dapp == null) return Collections.EMPTY_LIST;
        StackFrame frame = dapp.getCurrentFrame();
        if (frame == null) return Collections.EMPTY_LIST;
        List vitems = new ArrayList();

        // Add Frame.ThisObject
        ObjectReference oref = frame.thisObject();
        if (oref != null) vitems.add(new VarTreeItem(dapp, "this", oref));

        // Get Local Variables and add
        List<LocalVariable> vars;
        try {
            vars = frame.visibleVariables();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            vars = Collections.emptyList();
        }
        for (LocalVariable lv : vars) {
            String name = null;
            Value value = null;
            try {
                name = lv.name();
                value = frame.getValue(lv);
            } catch (Exception e) {
                name = e.toString();
            }
            vitems.add(new VarTreeItem(dapp, name, value));
        }

        // Sort and return
        Collections.sort(vitems);
        return vitems;
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
        VarTreeItem[] _children;

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
        public DebugApp getApp()
        {
            return _app;
        }

        /**
         * Returns the parent item.
         */
        public VarTreeItem getParent()
        {
            return _parent;
        }

        /**
         * Returns name.
         */
        public String getName()
        {
            return _name;
        }

        /**
         * Returns the value.
         */
        public Object getValue()
        {
            return _value;
        }

        /**
         * Return value.
         */
        public String getValueString()
        {
            Object value = _value;
            if (value instanceof ArrayReference) {
                ArrayReference aref = (ArrayReference) value;
                ReferenceType rtype = aref.referenceType();
                String name = rtype.name();
                int ind = name.lastIndexOf('.');
                if (ind > 0) name = name.substring(ind + 1);
                return String.format("%s[%d] (id=%d)", name, aref.length(), aref.uniqueID());
            }

            if (value instanceof StringReference) {
                StringReference sref = (StringReference) value;
                return sref.value();
            }

            if (value instanceof ObjectReference) {
                ObjectReference oref = (ObjectReference) value;
                ReferenceType rtype = oref.referenceType();
                String name = rtype.name();
                int ind = name.lastIndexOf('.');
                if (ind > 0) name = name.substring(ind + 1);
                return String.format("%s (id=%d)", name, oref.uniqueID());
            }

            return value != null ? value.toString() : "(null)";
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
        public VarTreeItem[] getChildren()
        {
            if (_children != null) return _children;
            List<VarTreeItem> list = new ArrayList();
            ObjectReference or = (ObjectReference) _value;
            ReferenceType rt = or.referenceType();

            // Handle Arrays
            if (or instanceof ArrayReference) {
                ArrayReference aref = (ArrayReference) or;
                int i = 0;
                for (Value value : aref.getValues())
                    list.add(new VarTreeItem(getApp(), "[" + (i++) + ']', value));
            }

            // Handle anything else: Iterate over fields for ReferenceType to create VarTableItems
            else {
                List<Field> fields = rt.allFields();
                for (Field field : fields) {
                    if (field.isStatic() || field.isEnumConstant()) continue;
                    String name = field.name();
                    Value value = null;
                    try {
                        value = or.getValue(field);
                    } catch (Exception e) {
                        name = e.toString();
                    }
                    list.add(new VarTreeItem(getApp(), name, value));
                }
                Collections.sort(list);
            }

            // Get array, set parents and return
            _children = list.toArray(new VarTreeItem[0]);
            for (VarTreeItem item : _children) item._parent = this;
            return _children;
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
        public VarTreeItem getParent(VarTreeItem anItem)
        {
            return anItem._parent;
        }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(VarTreeItem anItem)
        {
            return anItem.isParent();
        }

        /**
         * Returns the children.
         */
        public VarTreeItem[] getChildren(VarTreeItem aParent)
        {
            return aParent.getChildren();
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(VarTreeItem anItem, int aCol)
        {
            if (aCol == 0) return anItem.getName();
            if (aCol == 1) return anItem.getValueString();
            return anItem.getVarClass();
        }
    }

}
package snapcode.apptools;
import com.sun.jdi.*;
import snapcode.app.WorkspaceTool;
import snapcode.debug.DebugApp;
import snap.gfx.Font;
import snap.view.*;
import java.util.Collections;
import java.util.List;

/**
 * This view owner class manages showing variables for current stack frame in the debugger.
 */
public class DebugVarsPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool _debugTool;

    // The variable tree
    private TreeView<DebugVarItem> _varsTree;

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
    @Override
    protected void initUI()
    {
        // Get VarsTree
        _varsTree = getView("TreeView", TreeView.class);
        _varsTree.setFont(Font.Arial11);
        _varsTree.setResolver(new DebugVarItem.VarTreeResolver());

        // Configure "Name" column
        TreeCol<DebugVarItem> nameCol = _varsTree.getCol(0);
        nameCol.setHeaderText("Name");
        nameCol.setItemTextFunction(DebugVarItem::getName);
        nameCol.setPrefWidth(100);

        // Add "Value" column
        TreeCol<DebugVarItem> valueCol = new TreeCol<>();
        valueCol.setHeaderText("Value");
        valueCol.setItemTextFunction(DebugVarItem::getValueString);
        valueCol.setPrefWidth(100);
        valueCol.setGrowWidth(true);
        _varsTree.addCol(valueCol);

        // Add "Type" column
        TreeCol<DebugVarItem> typeCol = new TreeCol<>();
        typeCol.setHeaderText("Type");
        typeCol.setItemTextFunction(DebugVarItem::getVarClass);
        typeCol.setPrefWidth(100);
        typeCol.setGrowWidth(true);
        _varsTree.addCol(typeCol);
    }

    /**
     * Reset UI controls.
     */
    @Override
    public void resetUI()
    {
        // Reset VarTable.Items
        if (_resetVarsTree) {
            _resetVarsTree = false;
            List<DebugVarItem> varItems = createVarItems();
            _varsTree.setItems(varItems);
        }

        // Update VarText
        DebugVarItem selVarTreeItem = _varsTree.getSelItem();
        if (selVarTreeItem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String varValueStr = selVarTreeItem.invokeToStringMethod(getDebugApp());
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
    private List<DebugVarItem> createVarItems()
    {
        DebugApp debugApp = getDebugApp();
        StackFrame frame = debugApp != null ? debugApp.getCurrentFrame() : null;
        return frame != null ? DebugVarItem.createItemsForFrame(frame) : Collections.emptyList();
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Variables"; }
}
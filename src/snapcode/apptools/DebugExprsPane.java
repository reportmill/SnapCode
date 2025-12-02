package snapcode.apptools;
import snapcode.app.WorkspaceTool;
import snapcode.debug.DebugApp;
import snap.view.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This view owner class manages evaluating and showing expressions in the debugger.
 */
public class DebugExprsPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool _debugTool;

    // The variables tree
    private TreeView<DebugVarItem> _varsTree;

    // The list of expression tree items
    private List<DebugVarItem> _exprItems = new ArrayList<>();

    // Whether VarsTree needs reset
    private boolean _resetVarsTree = true;

    /**
     * Constructor.
     */
    public DebugExprsPane(DebugTool debugTool)
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
        _varsTree.setResolver(new DebugVarItem.VarTreeResolver());

        // Configure "Name" column
        TreeCol<?> treeCol0 = _varsTree.getCol(0);
        treeCol0.setHeaderText("Name");
        treeCol0.setPrefWidth(150);
        treeCol0.setGrowWidth(true);

        // Add second "Value" column
        TreeCol<DebugVarItem> treeCol1 = new TreeCol<>();
        treeCol1.setHeaderText("Value");
        treeCol1.setPrefWidth(150);
        treeCol1.setGrowWidth(true);
        _varsTree.addCol(treeCol1);

        // Set default item
        _exprItems.add(DebugVarItem.createItemForExpression("this"));
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Set items
        _varsTree.setItems(_exprItems);
        if (_varsTree.getSelItem() == null && !_exprItems.isEmpty())
            _varsTree.setSelIndex(0);

        // Iterate over ExprTableItems and reset values
        if (_resetVarsTree) {
            _resetVarsTree = false;
            DebugApp debugApp = getDebugApp();
            _exprItems.forEach(exprItem -> exprItem.evaluateExpression(debugApp));
            _varsTree.updateItems();
        }

        // Update ExprText
        DebugVarItem varTreeItem = _varsTree.getSelItem();
        setViewText("ExprText", varTreeItem != null ? varTreeItem.getName() : null);
        setViewEnabled("ExprText", varTreeItem != null);

        // Update VarText
        if (varTreeItem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String varValueStr = varTreeItem.invokeToStringMethod(getDebugApp());
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
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle ExprText
            case "ExprText" -> {
                DebugVarItem selExprItem = _varsTree.getSelItem();
                if (selExprItem == null)
                    return;

                DebugVarItem newExprItem = DebugVarItem.createItemForExpression(anEvent.getStringValue());
                newExprItem.evaluateExpression(getDebugApp());
                _exprItems.set(_exprItems.indexOf(selExprItem), newExprItem);
                _varsTree.setItems(_exprItems);
                _varsTree.setSelItem(newExprItem);
                resetVarTable();
            }

            // Handle AddButton
            case "AddButton" -> {
                DebugVarItem newExprItem = DebugVarItem.createItemForExpression(anEvent.getStringValue());
                newExprItem.evaluateExpression(getDebugApp());
                _exprItems.add(newExprItem);
                _varsTree.setItems(_exprItems);
                _varsTree.setSelItem(newExprItem);
            }

            // Handle RemoveButton
            case "RemoveButton" -> {
                int index = _varsTree.getSelIndex();
                if (index >= 0 && index < _exprItems.size())
                    _exprItems.remove(index);
            }
        }

        // Everything makes text focus
        requestFocus("ExprText");
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Expressions"; }
}
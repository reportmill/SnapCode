package snapcode.apptools;
import snapcode.app.WorkspaceTool;
import snapcode.debug.DebugApp;
import snapcode.debug.ExprEval;
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
    private TreeView<ExprTreeItem> _varsTree;

    // The list of expression tree items
    private List<ExprTreeItem>  _exprItems = new ArrayList<>();

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
        // Get VarTree and configure first "Name" column
        _varsTree = getView("TreeView", TreeView.class);
        TreeCol<?> treeCol0 = _varsTree.getCol(0);
        treeCol0.setHeaderText("Name");
        treeCol0.setPrefWidth(150);
        treeCol0.setGrowWidth(true);

        // Add second "Value" column
        TreeCol<ExprTreeItem> treeCol1 = new TreeCol<>();
        treeCol1.setHeaderText("Value");
        treeCol1.setPrefWidth(150);
        treeCol1.setGrowWidth(true);
        _varsTree.addCol(treeCol1);
        TreeResolver<ExprTreeItem> resolver = (TreeResolver<ExprTreeItem>) (TreeResolver<?>) new DebugVarsPane.VarTreeResolver();
        _varsTree.setResolver(resolver);

        // Set default item
        _exprItems.add(new ExprTreeItem("this"));
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Set items
        _varsTree.setItems(_exprItems);
        if (_varsTree.getSelItem() == null && !_exprItems.isEmpty())
            _varsTree.setSelIndex(0);

        // Iterate over ExprTableItems and reset values
        if (_resetVarsTree) {
            _resetVarsTree = false;
            _exprItems.forEach(ExprTreeItem::eval);
            _varsTree.updateItems();
        }

        // Update ExprText
        DebugVarsPane.VarTreeItem varTreeItem = _varsTree.getSelItem();
        setViewText("ExprText", varTreeItem != null ? varTreeItem.getName() : null);
        setViewEnabled("ExprText", varTreeItem != null);

        // Update VarText
        if (varTreeItem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String varValueStr = varTreeItem.getValueToString();
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
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ExprText
        if (anEvent.equals("ExprText")) {
            ExprTreeItem selExprItem = _varsTree.getSelItem();
            if (selExprItem == null)
                return;

            ExprTreeItem newExprItem = new ExprTreeItem(anEvent.getStringValue());
            newExprItem.eval();
            _exprItems.set(_exprItems.indexOf(selExprItem), newExprItem);
            _varsTree.setItems(_exprItems);
            _varsTree.setSelItem(newExprItem);
            resetVarTable();
        }

        // Handle AddButton
        if (anEvent.equals("AddButton")) {
            ExprTreeItem newExprItem = new ExprTreeItem(anEvent.getStringValue());
            newExprItem.eval();
            _exprItems.add(newExprItem);
            _varsTree.setItems(_exprItems);
            _varsTree.setSelItem(newExprItem);
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton")) {
            int index = _varsTree.getSelIndex();
            if (index >= 0 && index < _exprItems.size())
                _exprItems.remove(index);
        }

        // Everything makes text focus
        requestFocus("ExprText");
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Expressions"; }

    /**
     * A class to hold a ExprTableItem Variable.
     */
    public class ExprTreeItem extends DebugVarsPane.VarTreeItem {

        // Ivars
        String _expr;

        /**
         * Create ExprTableItem.
         */
        public ExprTreeItem(String aName)
        {
            super(null, aName, null);
            _expr = aName;
        }

        /**
         * Override to use current app.
         */
        public DebugApp getApp()  { return getDebugApp(); }

        /**
         * Makes TreeItem re-eval expression.
         */
        void eval()
        {
            _children = null;

            DebugApp debugApp = getDebugApp();
            try { _value = debugApp != null ? ExprEval.eval(debugApp, _expr) : null; }
            catch (Exception e) { _value = e; }
        }
    }
}
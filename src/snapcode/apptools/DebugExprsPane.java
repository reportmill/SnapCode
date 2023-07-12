package snapcode.apptools;
import snapcode.app.WorkspaceTool;
import snapcode.debug.DebugApp;
import snapcode.debug.ExprEval;
import snap.view.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A debug pane.
 */
public class DebugExprsPane extends WorkspaceTool {

    // The DebugTool
    private DebugTool  _debugTool;

    // The variable table
    private TreeView<ExprTreeItem>  _varTree;

    // The variable text
    private TextView  _varText;

    // The list of expression tree items
    private List<ExprTreeItem>  _exprItems = new ArrayList<>();

    /**
     * Creates a new DebugExprsPane.
     */
    public DebugExprsPane(DebugTool debugTool)
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
    @Override
    protected void initUI()
    {
        // Create VarTree and configure
        _varTree = getView("TreeView", TreeView.class);
        TreeCol c0 = _varTree.getCol(0);
        c0.setHeaderText("Name");
        c0.setPrefWidth(150);
        c0.setGrowWidth(true);
        TreeCol c1 = new TreeCol();
        c1.setHeaderText("Value");
        c1.setPrefWidth(150);
        c1.setGrowWidth(true);
        _varTree.addCol(c1);
        TreeResolver<ExprTreeItem> resolver = (TreeResolver<ExprTreeItem>) (TreeResolver<?>) new DebugVarsPane.VarTreeResolver();
        _varTree.setResolver(resolver); //_varTree.setEditable(true);

        // Set default item
        _exprItems.add(new ExprTreeItem("this"));

        // Set Cell Factory
    /*TreeTableColumn col0 = (TreeTableColumn)_varTable.getColumns().get(0);
    col0.setEditable(true); col0.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
    col0.setOnEditCommit(new EventHandler<CellEditEvent>() {
        public void handle(CellEditEvent e) {
            if(!(e.getRowValue() instanceof ExprTableItem)) return;
            ExprTableItem titem = (ExprTableItem)e.getRowValue();
            titem._expr = titem._name = (String)e.getNewValue(); resetVarTable(); } });*/

        // Create VarText TextView and configure in ScrollView
        _varText = getView("TextView", TextView.class);
        _varText.setWrapLines(true);
        _varText.getScrollView().setBorder(null);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Set items
        _varTree.setItemsList(_exprItems);
        if (_varTree.getSelItem() == null && _exprItems.size() > 0) _varTree.setSelIndex(0);

        // Iterate over ExprTableItems and reset values
        if (_resetVarTable) {
            _resetVarTable = false;
            for (ExprTreeItem item : _exprItems) item.eval();
            _varTree.updateItems();
        }

        // Update ExprText
        DebugVarsPane.VarTreeItem varTreeItem = _varTree.getSelItem();
        setViewText("ExprText", varTreeItem instanceof ExprTreeItem ? varTreeItem.getName() : null);
        setViewEnabled("ExprText", varTreeItem instanceof ExprTreeItem);

        // Update VarText
        if (varTreeItem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String pvalue = varTreeItem.getValueToString();
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

    boolean _resetVarTable = true;

    /**
     * Respond UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ExprText
        if (anEvent.equals("ExprText")) {
            DebugVarsPane.VarTreeItem item = _varTree.getSelItem();
            ExprTreeItem exitem = item instanceof ExprTreeItem ? (ExprTreeItem) item : null;
            if (exitem == null) return;
            ExprTreeItem nitem = new ExprTreeItem(anEvent.getStringValue());
            nitem.eval();
            _exprItems.set(_exprItems.indexOf(exitem), nitem);
            _varTree.setItemsList(_exprItems);
            _varTree.setSelItem(nitem);
            resetVarTable();
        }

        // Handle AddButton
        if (anEvent.equals("AddButton")) {
            ExprTreeItem nitem = new ExprTreeItem(anEvent.getStringValue());
            nitem.eval();
            _exprItems.add(nitem);
            _varTree.setItemsList(_exprItems);
            _varTree.setSelItem(nitem);
            //runLater(() -> _varTree.edit(_exprItems.size()-1, _varTree.getCol(0)));
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton")) {
            int index = _varTree.getSelIndex();
            if (index >= 0 && index < _exprItems.size()) _exprItems.remove(index);
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
        public DebugApp getApp()
        {
            return getDebugApp();
        }

        /**
         * Makes TreeItem re-eval expression.
         */
        void eval()
        {
            DebugApp dapp = getDebugApp();
            _children = null; //if(isChildrenSet()) { resetChildren(); setExpanded(false); }
            try {
                _value = dapp != null ? ExprEval.eval(dapp, _expr) : null;
            } catch (Exception e) {
                _value = e;
            }
        }
    }

}
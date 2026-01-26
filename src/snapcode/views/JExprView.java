package snapcode.views;
import javakit.parse.*;
import snap.view.*;
import snapcode.javatext.JavaTextAreaNodeHpr;

/**
 * A JNodeView subclass for JExpr.
 */
public class JExprView<JNODE extends JExpr> extends JNodeView<JNODE> {

    // The text field
    private TextField _textField;

    // The text field name
    private static final String TextFieldName = "ExprText";

    /**
     * Constructor.
     */
    public JExprView()
    {
        super();
        setSpacing(5);
    }

    @Override
    public void setJNode(JNODE aJNode)
    {
        // Do normal version
        super.setJNode(aJNode);

        // Create child views
        addChildExprViews();
    }

    /**
     * Override to return textfield.
     */
    protected void addChildExprViews()
    {
        // Get expression
        JExpr expr = getJNode();
        String str = expr.getString();

        // Create text field, configure and return
        _textField = JNodeViewUtils.createTextField(str);
        _textField.setName(TextFieldName);
        _textField.addEventHandler(e -> handleTextEvent(e), KeyRelease); //enableEvents(_tfield, DragEvents);
        _textField.addEventHandler(e -> handleTextEvent(e), Action);
        addChild(_textField);
    }

    /**
     * Override to return row layout.
     */
    @Override
    protected ViewLayout getViewLayoutImpl()  { return new RowViewLayout(this, false); }

    /**
     * Fires TextFieldAction.
     */
    protected void fireTextFieldAction()
    {
        ViewUtils.fireActionEvent(_textField, null);
    }

    /**
     * Handle TextField event.
     */
    protected void handleTextEvent(ViewEvent anEvent)
    {
        // Handle KeyEvents: Update PopupList
        if (anEvent.isKeyRelease())
            getEnv().runLater(() ->
                    SnapEditorPopup.getShared().activatePopupList(this, _textField.getText(), _textField.getSelStart()));

        // Handle ExprText Action
        else {

            // Get expression string
            SnapEditorPopup javaPopup = SnapEditorPopup.getShared();
            String exprStr = _textField.getText();
            if (javaPopup.isShowing())
                exprStr = javaPopup.getFixedText();

            // Replace node with expression string
            JavaTextAreaNodeHpr nodeHpr = getNodeHpr();
            nodeHpr.replaceNodeWithString(_jnode, exprStr);
            javaPopup.hide();
        }
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aJNode, double anX, double aY)
    {
        // If TextField, forward to parent
        if (_textField == null) {
            JNodeView<?> parentView = getNodeViewParent();
            parentView.dropNode(aJNode, anX, aY);
            return;
        }

        JStmtExpr exprStmt = aJNode instanceof JStmtExpr ? (JStmtExpr) aJNode : null;
        JExpr expr = exprStmt != null ? exprStmt.getExpr() : null;
        if (expr == null) {
            System.out.println("SnapPartExprEditor: Can't drop node " + aJNode);
            return;
        }

        // Replace expression with DropNode
        String exprStr = expr.getString();
        JavaTextAreaNodeHpr nodeHpr = getNodeHpr();
        nodeHpr.replaceNodeWithString(getJNode(), exprStr);
    }
}
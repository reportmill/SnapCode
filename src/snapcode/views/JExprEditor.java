package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JNode;
import javakit.parse.JStmtExpr;
import snap.view.TextField;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

/**
 * A SnapPartExpr subclass to do text editing on expression.
 */
public class JExprEditor<JNODE extends JExpr> extends JExprView<JNODE> {

    // The text field
    private TextField _textField;

    // The text field name
    private static final String TextFieldName = "ExprText";

    /**
     * Constructor.
     */
    public JExprEditor()
    {
        super();
    }

    /**
     * Constructor.
     */
    public JExprEditor(JNODE aJNode)
    {
        this();
        setJNode(aJNode);
    }

    /**
     * Updates UI.
     */
    public void updateUI()
    {
        // Do normal version
        super.updateUI();
        setBlockType(BlockType.Middle);
        setColor(null);

        // Get expression
        JExpr expr = getJNode();
        String str = expr.getString();

        // Create text field, configure and return
        _textField = createTextField(str);
        _textField.setName(TextFieldName);
        _textField.addEventHandler(e -> handleTextEvent(e), KeyRelease); //enableEvents(_tfield, DragEvents);
        _textField.addEventHandler(e -> handleTextEvent(e), Action);
        addChildToRowView(_textField);
    }

    /**
     * Fires TextFieldAction.
     */
    void fireTextFieldAction()
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
            SnapEditorPopup hpop = SnapEditorPopup.getShared();
            String str = _textField.getText();
            if (hpop.isShowing()) str = hpop.getFixedText();
            SnapEditor sed = getEditor();
            sed.replaceJNode(getJNode(), str);
            hpop.hide();
        }
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aJNode, double anX, double aY)
    {
        if (aJNode instanceof JStmtExpr)
            aJNode = ((JStmtExpr) aJNode).getExpr();
        if (!(aJNode instanceof JExpr)) {
            System.out.println("SnapPartExprEditor: Can't drop node " + aJNode);
            return;
        }

        // Replace expression with DropNode
        String str = aJNode.getString();
        getEditor().replaceJNode(getJNode(), str);
    }
}
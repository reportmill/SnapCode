package snapcode.views;
import javakit.parse.*;
import snap.view.TextField;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

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
    }

    /**
     * Override to return textfield.
     */
    @Override
    protected View[] createRowViews()
    {
        // Get expression
        JExpr expr = getJNode();
        String str = expr.getString();

        // Create text field, configure and return
        _textField = createTextField(str);
        _textField.setName(TextFieldName);
        _textField.addEventHandler(e -> handleTextEvent(e), KeyRelease); //enableEvents(_tfield, DragEvents);
        _textField.addEventHandler(e -> handleTextEvent(e), Action);
        return new View[] { _textField };
    }

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
            SnapEditorPopup hpop = SnapEditorPopup.getShared();
            String str = _textField.getText();
            if (hpop.isShowing()) str = hpop.getFixedText();
            SnapEditor sed = getEditor();
            sed.replaceJNodeWithString(getJNode(), str);
            hpop.hide();
        }
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aJNode, double anX, double aY)
    {
        // If TextField, forward to parent
        if (_textField == null) {
            JNodeView<?> parentView = getJNodeViewParent();
            parentView.dropNode(aJNode, anX, aY);
            return;
        }

        if (aJNode instanceof JStmtExpr)
            aJNode = ((JStmtExpr) aJNode).getExpr();
        if (!(aJNode instanceof JExpr)) {
            System.out.println("SnapPartExprEditor: Can't drop node " + aJNode);
            return;
        }

        // Replace expression with DropNode
        String str = aJNode.getString();
        getEditor().replaceJNodeWithString(getJNode(), str);
    }
}
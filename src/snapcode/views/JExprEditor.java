package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JNode;
import javakit.parse.JStmtExpr;
import snap.view.RowView;
import snap.view.TextField;
import snap.view.ViewEvent;
import snap.view.ViewUtils;

/**
 * A SnapPartExpr subclass to do text editing on expression.
 */
public class JExprEditor<JNODE extends JExpr> extends JExprView<JNODE> {

    // The text field
    TextField _tfield;

    // The text field name
    static final String TextFieldName = "ExprText";

    /**
     * Updates UI.
     */
    public void updateUI()
    {
        // Do normal version
        super.updateUI();
        setSeg(Seg.Middle);
        setColor(null);

        // Get/configure HBox
        RowView hbox = getHBox();

        // Get expression
        JExpr expr = getJNode();
        String str = expr.getString();

        // Create text field, configure and return
        _tfield = createTextField(str);
        _tfield.setName(TextFieldName);
        _tfield.addEventHandler(e -> handleTextEvent(e), KeyRelease); //enableEvents(_tfield, DragEvents);
        _tfield.addEventHandler(e -> handleTextEvent(e), Action);
        hbox.addChild(_tfield);
    }

    /**
     * Fires TextFieldAction.
     */
    void fireTextFieldAction()
    {
        ViewUtils.fireActionEvent(_tfield, null);
    }

    /**
     * Handle TextField event.
     */
    protected void handleTextEvent(ViewEvent anEvent)
    {
        // Handle KeyEvents: Update PopupList
        if (anEvent.isKeyRelease())
            getEnv().runLater(() ->
                    SnapEditorPopup.getShared().activatePopupList(this, _tfield.getText(), _tfield.getSelStart()));

            // Handle ExprText Action
        else {
            SnapEditorPopup hpop = SnapEditorPopup.getShared();
            String str = _tfield.getText();
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
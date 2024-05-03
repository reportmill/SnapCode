package snapcode.views;
import javakit.parse.*;
import snap.view.*;

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
     * Override.
     */
    @Override
    protected double getPrefWidthImpl(double aH)
    {
        if (getChildCount() > 1)
            return RowView.getPrefWidth(this, aH);
        return super.getPrefWidthImpl(aH);
    }

    /**
     * Override.
     */
    @Override
    protected double getPrefHeightImpl(double aW)
    {
        if (getChildCount() > 1)
            return RowView.getPrefHeight(this, aW);
        return super.getPrefHeightImpl(aW);
    }

    /**
     * Override to resize rects.
     */
    @Override
    protected void layoutImpl()
    {
        if (getChildCount() > 1)
            RowView.layout(this, false);
        else super.layoutImpl();
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
            sed.replaceNodeWithString(getJNode(), str);
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
            JNodeView<?> parentView = getNodeViewParent();
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
        getEditor().replaceNodeWithString(getJNode(), str);
    }
}
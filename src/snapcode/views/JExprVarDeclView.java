package snapcode.views;
import javakit.parse.*;
import snap.view.Label;

/**
 * JExprView subclass for JExprVarDecl.
 */
public class JExprVarDeclView<JNODE extends JExprVarDecl> extends JExprView<JNODE> {

    /**
     * Constructor.
     */
    public JExprVarDeclView()
    {
        super();
    }

    /**
     * Override to return views for var decl parts.
     */
    @Override
    protected void addChildExprViews()
    {
        JExprVarDecl varDeclExpr = getJNode();
        JVarDecl varDecl = varDeclExpr.getVarDecl();

        // Add child for type
        JType typeDecl = varDecl.getType();
        String typeStr = typeDecl.getString();
        Label typeLabel = JNodeViewUtils.createLabel(typeStr);
        addChild(typeLabel);

        // Add child for var decl id
        JExpr idExpr = varDecl.getId();
        JNodeView<?> idExprView = JNodeView.createNodeViewForNode(idExpr);
        idExprView.setGrowWidth(true);
        addChild(idExprView);

        // Add children for init expr
        JExpr initExpr = varDecl.getInitExpr();
        if (initExpr != null) {

            // Create '=' label
            Label equalsLabel = JNodeViewUtils.createLabel("=");
            addChild(equalsLabel);

            // Add child for assign value expr
            JNodeView<?> exprView = JNodeView.createNodeViewForNode(initExpr);
            exprView.setGrowWidth(true);
            addChild(exprView);
         }
    }
}
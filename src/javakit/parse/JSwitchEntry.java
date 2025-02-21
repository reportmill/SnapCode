package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaField;
import javakit.resolver.JavaType;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent individual case entries in a switch statement.
 */
public class JSwitchEntry extends JNode implements WithStmts, WithVarDeclsX {

    // The case label expression(s)
    private List<JExpr> _labels = new ArrayList<>(1);

    // Whether case is default
    private boolean _default;

    // The body statements
    private List<JStmt> _stmts = new ArrayList<>();

    // An array of VarDecls held by JStmtVarDecls
    private JVarDecl[] _varDecls;

    /**
     * Constructor.
     */
    public JSwitchEntry()
    {
        super();
    }

    /**
     * Returns the label expression.
     */
    public JExpr getLabel()  { return !_labels.isEmpty() ? _labels.get(0) : null; }

    /**
     * Returns the label expressions.
     */
    public List<JExpr> getLabels()  { return _labels; }

    /**
     * Adds a label expression.
     */
    public void addLabel(JExpr anExpr)
    {
        _labels.add(anExpr);
        addChild(anExpr);
    }

    /**
     * Returns whether label is default.
     */
    public boolean isDefault()  { return _default; }

    /**
     * Sets whether label is default.
     */
    public void setDefault(boolean aValue)  { _default = aValue; }

    /**
     * Returns the statements.
     */
    public List<JStmt> getStatements()  { return _stmts; }

    /**
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt)
    {
        _stmts.add(aStmt);
        addChild(aStmt);
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        if (_varDecls != null) return _varDecls;
        JVarDecl[] varDecls = WithStmts.getVarDecls(this);
        return _varDecls = varDecls;
    }

    /**
     * Returns the eval type.
     */
    public JavaType getReturnType()
    {
        JStmt lastStmt = !_stmts.isEmpty() ? _stmts.get(_stmts.size() - 1) : null;

        // If last statement is expression statement, return expression type
        if (lastStmt instanceof JStmtExpr) {
            JExpr expr = ((JStmtExpr) lastStmt).getExpr();
            return expr.getEvalType();
        }

        // If last statement is return statement, return expression type
        if (lastStmt instanceof JStmtReturn) {
            JExpr expr = ((JStmtReturn) lastStmt).getExpr();
            return expr.getEvalType();
        }

        // Return not defined
        return null;
    }

    /**
     * Override to check inner variable declaration statements.
     * Override to try to resolve given id name from any preceding ClassDecl statements.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If node is case label id, try to evaluate against Switch expression enum type
        if (_labels.contains(anExprId))
            return getDeclForCaseLabel(anExprId);

        // If any previous statements are class decl statements that declare type, return class
        JavaClass javaClass = WithStmts.getJavaClassForChildTypeOrId(this, anExprId);
        if (javaClass != null)
            return javaClass;

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Returns the decl for the case expression.
     */
    private JavaDecl getDeclForCaseLabel(JExpr caseLabel)
    {
        JNode switchStmt = getParent();
        JExpr switchExpr = null;
        if (switchStmt instanceof JStmtSwitch)
            switchExpr = ((JStmtSwitch) switchStmt).getSelector();
        else if (switchStmt instanceof JExprSwitch)
            switchExpr = ((JExprSwitch) switchStmt).getSelector();

        // Get Switch expression type
        JavaType switchExprType = switchExpr != null ? switchExpr.getEvalType() : null;
        if (switchExprType == null)
            return null;

        // Handle enum switch
        if (switchExprType.isEnum()) {
            JavaClass enumClass = (JavaClass) switchExprType;
            String enumName = caseLabel.getName();
            JavaField enumConst = enumClass.getDeclaredFieldForName(enumName);
            if (enumConst != null)
                return enumConst;
        }

        // Return switch expr decl
        return switchExprType;
    }

    /**
     * Override to try to resolve given type from any preceding ClassDecl statements.
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType aJType)
    {
        // If any previous statements are class decl statements that declare type, return class
        JavaClass javaClass = WithStmts.getJavaClassForChildTypeOrId(this, aJType);
        if (javaClass != null)
            return javaClass;

        // Do normal version
        return super.getJavaTypeForChildType(aJType);
    }
}

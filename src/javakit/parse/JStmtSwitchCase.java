package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaField;
import javakit.resolver.JavaType;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent individual cases in a switch statement.
 */
public class JStmtSwitchCase extends JNode implements WithStmts, WithVarDeclsX {

    // The case expression
    private JExpr  _expr;

    // Whether case is default
    private boolean  _default;

    // The body statements
    private List<JStmt>  _stmts = new ArrayList<>();

    // An array of VarDecls held by JStmtVarDecls
    private JVarDecl[] _varDecls;

    /**
     * Constructor.
     */
    public JStmtSwitchCase()
    {
        super();
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
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
     * Override to check inner variable declaration statements.
     * Override to try to resolve given id name from any preceding ClassDecl statements.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If node is case label id, try to evaluate against Switch expression enum type
        if (anExprId == _expr)
            return getDeclForCaseExpr();

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
    private JavaDecl getDeclForCaseExpr()
    {
        // Get Switch expression type
        JStmtSwitch switchStmt = getParent(JStmtSwitch.class);
        JExpr switchExpr = switchStmt.getExpr();
        JavaType switchExprType = switchExpr != null ? switchExpr.getEvalType() : null;
        if (switchExprType == null)
            return null;

        // Handle enum switch
        if (switchExprType.isEnum()) {
            JavaClass enumClass = (JavaClass) switchExprType;
            String enumName = _expr.getName();
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

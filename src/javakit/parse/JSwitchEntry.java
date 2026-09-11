package javakit.parse;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent individual case entries in a switch statement.
 */
public class JSwitchEntry extends JNode implements WithStmts, WithBlockStmt, WithVarDeclsX {

    // The case label expression(s)
    private List<JExpr> _labels = new ArrayList<>(1);

    // The 'when' expression
    private JExpr _guard;

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
    public JExpr getLabel()  { return !_labels.isEmpty() ? _labels.getFirst() : null; }

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
     * Returns the guard ('when') expression, if set.
     */
    public JExpr getGuard()  { return _guard; }

    /**
     * Sets the guard ('when') expression, if set.
     */
    public void setGuard(JExpr guardExpr)  { addChild(_guard = guardExpr); }

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
     * Sets the statements.
     */
    public void setStatements(List<JStmt> stmtsList)
    {
        _stmts.forEach(this::removeChild);
        _stmts = stmtsList;
        _stmts.forEach(this::addChild);
    }

    /**
     * Adds a statement.
     */
    public void addStatement(JStmt aStmt)
    {
        _stmts.add(aStmt);
        addChild(aStmt);
    }

    /**
     * WithBlockStmt method: Returns the statement block.
     */
    @Override
    public JStmtBlock getBlock()
    {
        // If already set, just return
        if (_stmts.size() == 1 && _stmts.getFirst() instanceof JStmtBlock blockStmt)
            return blockStmt;

        // Create StmtBlock, add statement and replace
        JStmtBlock stmtBlock = new JStmtBlock();
        _stmts.forEach(stmtBlock::addStatement);
        setBlock(stmtBlock);

        // Return
        return stmtBlock;
    }

    /**
     * WithBlockStmt method: Sets a block.
     */
    @Override
    public void setBlock(JStmtBlock aBlock)
    {
        setStatements(List.of(aBlock));
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        if (_varDecls != null) return _varDecls;

        // Add var decls for statements (conventional switch entry)
        JVarDecl[] varDecls = WithStmts.getVarDecls(this);

        // If label is typed pattern, add var decl
        for (JExpr label : _labels) {
            if (label instanceof JExprPattern pattern) {
                JVarDecl patternVarDecl = pattern.getVarDecl();
                if (patternVarDecl != null)
                    varDecls = ArrayUtils.add(varDecls, patternVarDecl, 0);
            }
        }

        // Return
        return _varDecls = varDecls;
    }

    /**
     * Returns the eval type.
     */
    public JavaType getReturnType()
    {
        List<JavaType> returnTypes = new ArrayList<>();

        // If last statement is expression, add type
        if (!_stmts.isEmpty() && _stmts.getLast() instanceof JStmtExpr exprStmt) {
            JExpr expr = exprStmt.getExpr();
            JavaType exprType = expr.getEvalType();
            if (exprType != null)
                returnTypes.add(exprType);
        }

        // Look for any child return/yield statement
        findReturnTypes(this, returnTypes);

        // Get common ancestor type
        JavaType returnType = !returnTypes.isEmpty() ? returnTypes.getFirst() : null;
        for (int i = 1; i < returnTypes.size(); i++)
            returnType = returnType.getCommonAncestor(returnTypes.get(i));

        return returnType;
    }

    /**
     * Find types for all return/yield statements.
     */
    private void findReturnTypes(JNode node, List<JavaType> types)
    {
        switch (node) {

            case JStmtReturn returnStmt -> {
                JExpr expr = returnStmt.getExpr();
                JavaType exprType = expr != null ? expr.getEvalType() : null;
                if (exprType != null)
                    types.add(exprType);
            }

            case JStmtYield yieldStmt -> {
                JExpr expr = yieldStmt.getExpr();
                JavaType exprType = expr != null ? expr.getEvalType() : null;
                if (exprType != null)
                    types.add(exprType);
            }

            default -> node.getChildren().forEach(child -> findReturnTypes(child, types));
        }
    }
}

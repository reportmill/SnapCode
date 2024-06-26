package javakit.runner;
import javakit.parse.*;
import javakit.resolver.JavaLocalVar;
import java.util.List;

/**
 * This class does some simple compiling of Node tree.
 */
public class Simpiler {

    /**
     * Compile.
     */
    public static void setVarStackIndexForJFile(JFile aJFile)
    {
        JClassDecl classDecl = aJFile.getClassDecl();
        if (classDecl != null)
            setVarStackIndexForClass(classDecl);
    }

    /**
     * Compile class.
     */
    protected static void setVarStackIndexForClass(JClassDecl aClassDecl)
    {
        JBodyDecl[] bodyDecls = aClassDecl.getBodyDecls();

        // Iterate over members and setVarStackIndex for members WithBlockStmt (method, constr, initializer)
        for (JBodyDecl bodyDecl : bodyDecls)
            if (bodyDecl instanceof WithBlockStmt)
                setVarStackIndexForNode(bodyDecl, 0);
    }

    /**
     * Sets var stack index for node.
     */
    protected static void setVarStackIndexForNode(JNode aNode, int anIndex)
    {
        // Handle block statement
        if (aNode instanceof JStmtBlock)
            setVarStackIndexForBlockStmt((JStmtBlock) aNode, anIndex);

        // Handle WithVarDecls: JExecutableDecl, JStmtFor, JStmtTryCatch, JExprLambda
        else if (aNode instanceof WithVarDecls)
            setVarStackIndexForNodeWithVarDecls(aNode, anIndex);

        // Handle VarDecl: Shouldn't happen
        else if (aNode instanceof JVarDecl)
            System.err.println("Simpiler.setVarStackIndexForNode: Shouldn't hit VarDecl directly: " + aNode.getName());

        // Handle anything else: Recurse into children
        else {
            List<JNode> children = aNode.getChildren();
            for (JNode child : children)
                setVarStackIndexForNode(child, anIndex);
        }
    }

    /**
     * Sets var stack index for node WithVarDecls: JExecutableDecl (method/constr), JStmtFor, JStmtTryCatch, JExprLambda.
     *
     * Returns the new index after adding var decls.
     */
    private static int setVarStackIndexForNodeWithVarDecls(JNode aNode, int anIndex)
    {
        // Get VarDecls
        WithVarDecls withVarDecls = (WithVarDecls) aNode;
        JVarDecl[] varDecls = withVarDecls.getVarDecls();
        int varDeclCount = varDecls.length;

        // Iterate over VarDecls and set IndexInStackFrame for each
        for (int i = 0; i < varDeclCount; i++) {
            JVarDecl varDecl = varDecls[i];

            // If varDecl.InitExpr is lambda, recurse in
            JExpr initExpr = varDecl.getInitExpr();
            if (initExpr != null)
                setVarStackIndexForNode(initExpr, 0);

            // Set localVar.IndexInStackFrame
            JavaLocalVar localVar = (JavaLocalVar) varDecl.getDecl();
            if (localVar != null)
                localVar.setIndexInStackFrame(anIndex + i);
        }

        // If node is also WithBlockStmt, recurse in
        if (aNode instanceof WithBlockStmt) {
            WithBlockStmt withBlockStmt = (WithBlockStmt) aNode;
            JStmtBlock blockStmt = withBlockStmt.getBlock();
            if (blockStmt != null)
                setVarStackIndexForNode(blockStmt, anIndex + varDeclCount);
        }

        // Return new IndexInStackFrame accounting for VarDecls
        return anIndex + varDeclCount;
    }

    /**
     * Sets var stack index for block statement.
     */
    private static void setVarStackIndexForBlockStmt(JStmtBlock blockStmt, int anIndex)
    {
        // Get block statement statements
        List<JStmt> stmts = blockStmt.getStatements();
        int varIndex = anIndex;

        // Iterate over statements and bump varIndex for VarDecl stmts, but handle others normally
        for (JStmt stmt : stmts) {

            // Handle varDecl statement: Bump varIndex for VarDecl statement VarDecls
            if (stmt instanceof JStmtVarDecl) {
                JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
                JExprVarDecl varDeclExpr = varDeclStmt.getVarDeclExpr();
                varIndex = setVarStackIndexForNodeWithVarDecls(varDeclExpr, varIndex);
            }

            // Handle any other statement normally
            else setVarStackIndexForNode(stmt, varIndex);
        }
    }
}

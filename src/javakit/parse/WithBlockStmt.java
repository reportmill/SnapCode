package javakit.parse;
import java.util.Collections;
import java.util.List;

/**
 * This interface identifies nodes with a block statement (JExecDecl, JInitDecl, JExprLambda, JStmtTryCatch).
 */
public interface WithBlockStmt {

    /**
     * Returns the block statement.
     */
    JStmtBlock getBlock();

    /**
     * Sets the block statement.
     */
    void setBlock(JStmtBlock aBlockStmt);

    /**
     * Returns the block statements.
     */
    default JStmt[] getBlockStatements()
    {
        JStmtBlock blockStmt = getBlock();
        List<JStmt> statements = blockStmt != null ? blockStmt.getStatements() : Collections.EMPTY_LIST;
        if (statements != null && statements.size() > 0)
            return statements.toArray(new JStmt[0]);
        return new JStmt[0];
    }
}

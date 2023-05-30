package javakit.parse;

/**
 * This interface identifies nodes with a block statement (JExecDecl, JInitDecl, JExprLambda, JStmtTryCatch).
 */
public interface WithBlockStmt {

    JStmtBlock getBlock();

    void setBlock(JStmtBlock aBlockStmt);
}

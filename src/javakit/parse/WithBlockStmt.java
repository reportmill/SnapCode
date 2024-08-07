package javakit.parse;
import snap.parse.ParseToken;
import snap.util.ListUtils;
import java.util.Collections;
import java.util.List;

/**
 * This interface identifies nodes with a block statement (JExecDecl, JInitDecl, JExprLambda, JStmtTryCatch).
 */
public interface WithBlockStmt {

    default boolean isBlockSet()
    {
        JNode thisNode = (JNode) this;
        return ListUtils.hasMatch(thisNode.getChildren(), child -> child instanceof JStmtBlock);
    }

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
        if (statements != null && !statements.isEmpty())
            return statements.toArray(new JStmt[0]);
        return new JStmt[0];
    }

    /**
     * Replaces this node's block with new block. Called from updateJFileForChange().
     */
    default void replaceBlock(JStmtBlock newBlockStmt)
    {
        setBlock(newBlockStmt);

        // Extend ancestor end tokens if needed
        JNode thisNode = (JNode) this;
        ParseToken endToken = newBlockStmt.getEndToken();
        JNode ancestor = thisNode.getParent();
        while (ancestor != null) {
            if (ancestor.getEndToken() == null || ancestor.getEndCharIndex() < endToken.getEndCharIndex()) {
                ancestor.setEndToken(endToken);
                ancestor = ancestor.getParent();
            }
            else break;
        }

        // Clear ancestor errors
        ancestor = thisNode;
        while (ancestor != null) {
            ancestor._errors = null;
            ancestor = ancestor.getParent();
        }

        // Clear enclosing class decl ClassDecls in case new block added/removed anonymous inner class
        JClassDecl classDecl = thisNode.getParent(JClassDecl.class);
        classDecl._classDecls = null;
    }
}

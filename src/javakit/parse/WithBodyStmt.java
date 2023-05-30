package javakit.parse;

/**
 * This interface identifies nodes with a body statement (JStmtIf, JStmtFor, JStmtDo, JStmtWhile, JStmtLabeled).
 */
public interface WithBodyStmt {

    JStmt getStatement();
}

package javakit.parse;
import java.util.List;

/**
 * This interface identifies nodes with child statements (JStmtBlock, JStmtSwitchEntry).
 */
public interface WithStmts {

    List<JStmt> getStatements();
}

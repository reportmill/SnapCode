package javakit.parse;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This interface identifies nodes with child statements (JStmtBlock, JStmtSwitchEntry).
 */
public interface WithStmts {

    List<JStmt> getStatements();

    /**
     * Returns VarDecls encapsulated by WithStmts (JStmtVarDecl.VarDecls).
     */
    static List<JVarDecl> getWithStmtsVarDecls(WithStmts withStmts)
    {
        // Get Statment.VarDecls
        List<JStmt> statements = withStmts.getStatements();
        Stream<JStmtVarDecl> varDeclStmtsStream = (Stream<JStmtVarDecl>) (Stream<?>) statements.stream().filter(stmt -> stmt instanceof JStmtVarDecl);
        Stream<JVarDecl> varDeclsStream = varDeclStmtsStream.flatMap(stmt -> stmt.getVarDecls().stream());
        return varDeclsStream.collect(Collectors.toList());
    }
}

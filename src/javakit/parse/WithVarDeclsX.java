package javakit.parse;

/**
 * This interface extends WithVarDecls to identify nodes that directly or indirectly hold VarDecls up the parent chain.
 * This is useful to search for VarDecls in scope by traversing up node parents.
 * Known implementers:
 *    - JClassDecl (JFieldDecls)
 *    - JStmtBlock (JStmtVarDecls)
 *    - JStmtSwitchCase (JStmtVarDecls)
 */
public interface WithVarDeclsX extends WithVarDecls {

}

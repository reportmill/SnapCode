package javakit.parse;
import snap.util.ListUtils;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This interface identifies nodes with VarDecls. Known uses:
 *    - JExecDecl (method/constr params)
 *    - JStmtBlock (JStmtVarDecl)
 *    - JStmtFor (InitDecl.VarDecls)
 *    - JStmtTryCatch (parameter)
 *    - JExprLambda (parameters)
 */
public interface WithVarDecls {

    /**
     * Returns the list of VarDecls associated with this node.
     */
    List<JVarDecl> getVarDecls();

    /**
     * Returns the matching var decl for given name, if present.
     */
    default JVarDecl getVarDeclForName(String aName)
    {
        List<JVarDecl> varDecls = getVarDecls();
        Predicate<JVarDecl> nameEquals = vd -> Objects.equals(aName, vd.getName());
        return ListUtils.findMatch(varDecls, nameEquals);
    }
}

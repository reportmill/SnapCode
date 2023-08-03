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

    /**
     * Returns the matching var decl for given name, if found and in scope (appears before id expression).
     */
    default JVarDecl getVarDeclForId(JExprId anId)
    {
        // Get var decl for name (just return null if not found)
        String name = anId.getName();
        JVarDecl varDecl = getVarDeclForName(name);
        if (varDecl == null)
            return null;

        // If id appears before var decl, return null
        if (anId.getStartCharIndex() < varDecl.getEndCharIndex())
            return null;

        // Return
        return varDecl;
    }
}

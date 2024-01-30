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

        // If var decl is field, return make sure it's valid for id
        if (varDecl.getParent() instanceof JFieldDecl) {
            if (isFieldRefValidForId(varDecl, anId))
                return varDecl;
            return null;
        }

        // If id appears before var decl end, return null
        if (anId.getStartCharIndex() < varDecl.getEndCharIndex())
            return null;

        // Return
        return varDecl;
    }

    /**
     * Returns whether id is valid for given field var decl.
     */
    static boolean isFieldRefValidForId(JVarDecl varDecl, JExprId exprId)
    {
        JFieldDecl fieldDecl = (JFieldDecl) varDecl.getParent();

        // Get enclosing body decl matching main class
        JMemberDecl bodyDecl = exprId.getParent(JMemberDecl.class);
        while (bodyDecl != null && bodyDecl.getEnclosingClassDecl() != fieldDecl.getEnclosingClassDecl())
            bodyDecl = bodyDecl.getParent(JMemberDecl.class);

        // Just return false if no body decl (impossible?)
        if (bodyDecl == null)
            return false;

        // If constructor, return true
        if (bodyDecl instanceof JConstrDecl)
            return true;

        // If method, check static
        if (bodyDecl instanceof JMethodDecl) {

            // If field static, return true (static fields always accessible from any method)
            if (fieldDecl.getMods().isStatic())
                return true;

            // Return true if method and field are instance
            JMethodDecl methodDecl = (JMethodDecl) bodyDecl;
            return !methodDecl.getMods().isStatic();
        }

        // If initializer, check static
        if (bodyDecl instanceof JInitializerDecl) {

            // If field static, return true (static fields always accessible from any method)
            if (fieldDecl.getMods().isStatic())
                return true;

            // Return true if method and field are instance
            JInitializerDecl initDecl = (JInitializerDecl) bodyDecl;
            return !initDecl.getMods().isStatic();
        }

        // If field, check static and location
        if (bodyDecl instanceof JFieldDecl) {
            JFieldDecl otherField = (JFieldDecl) bodyDecl;
            if (fieldDecl.getMods().isStatic())
                return !otherField.getMods().isStatic() || varDecl.getStartCharIndex() < exprId.getEndCharIndex();
            return !otherField.getMods().isStatic() && varDecl.getStartCharIndex() < exprId.getEndCharIndex();
        }

        // Return not valid
        return false;
    }
}

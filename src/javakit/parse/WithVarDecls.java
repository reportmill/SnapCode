package javakit.parse;
import snap.util.ArrayUtils;
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
    JVarDecl[] getVarDecls();

    /**
     * Returns the matching var decl for given name, if present.
     */
    default JVarDecl getVarDeclForName(String aName)
    {
        JVarDecl[] varDecls = getVarDecls();
        Predicate<JVarDecl> nameEquals = vd -> Objects.equals(aName, vd.getName());
        return ArrayUtils.findMatch(varDecls, nameEquals);
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
            if (isFieldVarDeclValidForNode(varDecl, anId))
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
     * Returns whether given field var decl is valid for given node.
     */
    static boolean isFieldVarDeclValidForNode(JVarDecl varDecl, JNode otherNode)
    {
        JFieldDecl fieldDecl = (JFieldDecl) varDecl.getParent();

        // Get enclosing member for other node
        JMemberDecl otherNodeMember = otherNode.getParent(JMemberDecl.class);
        if (otherNodeMember == null) // Impossible?
            return false;

        // If other node member not in same class as given field, check parent member first
        boolean isOtherNodeInNestedClass = otherNodeMember.getEnclosingClassDecl() != fieldDecl.getEnclosingClassDecl();
        if (isOtherNodeInNestedClass && !otherNodeMember.isStatic()) {
            if (!isFieldVarDeclValidForNode(varDecl, otherNodeMember))
                return false;
        }

        // If field, check static and location
        if (otherNodeMember instanceof JFieldDecl otherNodeField) {

            // If given field static, return true if other node not static or declared after
            if (fieldDecl.isStatic()) {
                if (!otherNodeField.isStatic())
                    return true;
                return varDecl.getStartCharIndex() < otherNode.getEndCharIndex();
            }

            // If given field not static, return true if other node not static and declared after
            if (otherNodeField.isStatic())
                return false;
            return isOtherNodeInNestedClass || varDecl.getStartCharIndex() < otherNode.getEndCharIndex();
        }

        // If other (method, constructor, initializer, inner-class), return true if field is static or member is not static
        if (fieldDecl.isStatic())
            return true;

        // Return true if other is also not static
        return !otherNodeMember.isStatic();
    }
}

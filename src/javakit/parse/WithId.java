package javakit.parse;

/**
 * This interface identifies nodes with a simple Id property (identifier name):
 *    - JMemberDecl (class/method/field/constr/enum-constant)
 *    - JExprMethodCall/JExprMethodRef
 *    - JVarDecl
 *    - JTypeVar
 */
public interface WithId {

    /**
     * Returns the identifier.
     */
    JExprId getId();

    /**
     * Sets the identifier.
     */
    void setId(JExprId idExpr);
}

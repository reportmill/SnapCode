package javakit.parse;
import snap.util.ArrayUtils;

/**
 * This interface identifies nodes that can declare type variables, namely JClassDecl and JExecutableDecl.
 */
public interface WithTypeVars {

    /**
     * Returns the type variables for this member.
     */
    JTypeVar[] getTypeVarDecls();

    /**
     * Returns the type variable declaration for this class/method/constructor with given name.
     */
    default JTypeVar getTypeVarDeclForName(String aName)
    {
        JTypeVar[] typeVars = getTypeVarDecls();
        return ArrayUtils.findMatch(typeVars, tvar -> tvar.getName().equals(aName));
    }
}

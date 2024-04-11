package javakit.parse;
import snap.util.ArrayUtils;

/**
 * This interface identifies nodes that can declare type variables, namely JClassDecl and JExecutableDecl.
 */
public interface WithTypeVars {

    /**
     * Returns the type variables for this member.
     */
    JTypeVar[] getTypeVars();

    /**
     * Returns the type variable for this member with given name.
     */
    default JTypeVar getTypeVar(String aName)
    {
        JTypeVar[] typeVars = getTypeVars();
        return ArrayUtils.findMatch(typeVars, tvar -> tvar.getName().equals(aName));
    }
}

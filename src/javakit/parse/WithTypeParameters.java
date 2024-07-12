package javakit.parse;
import snap.util.ArrayUtils;

/**
 * This interface identifies nodes that can declare type parameters (JClassDecl and JExecutableDecl).
 */
public interface WithTypeParameters {

    /**
     * Returns the type variables for this member.
     */
    JTypeVar[] getTypeParamDecls();

    /**
     * Returns the type parameter declaration for this class/method/constructor with given name.
     */
    default JTypeVar getTypeParamDeclForName(String aName)
    {
        JTypeVar[] typeParams = getTypeParamDecls();
        return ArrayUtils.findMatch(typeParams, tvar -> tvar.getName().equals(aName));
    }
}

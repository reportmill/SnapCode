package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaExecutable;

/**
 * This interface identifies nodes with arguments: JExprMethodCall, JExprAlloc, JStmtConstrCall, JEnumConst).
 */
public interface WithArgs {

    /**
     * Returns the argument expressions.
     */
    JExpr[] getArgs();

    /**
     * Sets the argument expressions.
     */
    void setArgs(JExpr[] theArgs);

    /**
     * Returns the number of arguments.
     */
    default int getArgCount()  { return getArgs().length; }

    /**
     * Returns the individual argument at index.
     */
    default JExpr getArg(int anIndex)  { return getArgs()[anIndex]; }

    /**
     * Returns the method/constructor this node calls.
     */
    default JavaExecutable getExecutable()
    {
        JNode node = (JNode) this;
        JavaDecl decl = node.getDecl();
        return decl instanceof JavaExecutable ? (JavaExecutable) decl : null;
    }
}

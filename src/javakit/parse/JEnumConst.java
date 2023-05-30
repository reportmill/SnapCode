package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import java.util.Collections;
import java.util.List;

/**
 * A JNode for Enum constants.
 */
public class JEnumConst extends JMemberDecl {

    // The args
    protected List<JExpr>  _args = Collections.EMPTY_LIST;

    // The class or interface body
    protected String  _classBody;

    /**
     * Returns the arguments.
     */
    public List<JExpr> getArgs()
    {
        return _args;
    }

    /**
     * Sets the arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        if (_args != null) for (JExpr arg : _args) removeChild(arg);
        _args = theArgs;
        if (_args != null) for (JExpr arg : _args) addChild(arg, -1);
    }

    /**
     * Returns the class decl.
     */
    public String getClassBody()
    {
        return _classBody;
    }

    /**
     * Sets the class decl.
     */
    public void setClassBody(String aBody)
    {
        _classBody = aBody;
    }

    /**
     * Get class name from parent enum declaration.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get enum name, enclosing JClassDecl and its JavaClass (can be null if enum hasn't been compiled yet)
        String name = getName();
        JClassDecl cdecl = (JClassDecl) getParent();
        JavaClass jdecl = cdecl.getDecl();
        if (jdecl == null)
            return null;

        // Get JavaDecl for enum constant, which is just a field of enum class
        JavaDecl edecl = jdecl.getFieldForName(name);
        return edecl;
    }

    /**
     * Override to resolve enum id.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        if (anExprId == _id)
            return getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }
}
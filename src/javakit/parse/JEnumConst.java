package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaField;
import java.util.Collections;
import java.util.List;

/**
 * A JNode for Enum constants.
 */
public class JEnumConst extends JMemberDecl {

    // The args
    protected List<JExpr>  _args = Collections.EMPTY_LIST;

    // The class or interface body
    protected JBodyDecl[] _classBody;

    /**
     * Constructor.
     */
    public JEnumConst()
    {
        super();
    }

    /**
     * Returns the arguments.
     */
    public List<JExpr> getArgs()  { return _args; }

    /**
     * Sets the arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        // Remove old
        if (_args != null)
            _args.forEach(arg -> removeChild(arg));

        // Set new
        _args = theArgs;

        // Add new args
        if (_args != null)
            _args.forEach(arg -> addChild(arg));
    }

    /**
     * Returns the class decl.
     */
    public JBodyDecl[] getClassBody()  { return _classBody; }

    /**
     * Sets the class decl.
     */
    public void setClassBody(JBodyDecl[] aBody)
    {
        _classBody = aBody;
    }

    /**
     * Get class name from parent enum declaration.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // Get enum name, enclosing JClassDecl and its JavaClass (can be null if enum hasn't been compiled yet)
        String enumName = getName();
        JClassDecl classDecl = (JClassDecl) getParent();
        JavaClass javaClass = classDecl.getJavaClass();
        if (javaClass == null)
            return null;

        // Get JavaField for enum constant
        JavaField field = javaClass.getDeclaredFieldForName(enumName);
        return field;
    }
}
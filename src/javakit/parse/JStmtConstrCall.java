package javakit.parse;
import java.util.stream.Stream;
import javakit.resolver.JavaClassUtils;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import snap.util.ArrayUtils;

/**
 * A JStmt subclass to represent an explicit constructor invocation, like: this(x) or super(y).
 * Found in first line of JContrDecl only.
 */
public class JStmtConstrCall extends JStmt {

    // The array of ids
    private JExprId[] _idList = new JExprId[0];

    // The args
    private JExpr[] _args = new JExpr[0];

    /**
     * Returns the array of ids.
     */
    public JExprId[] getIds()  { return _idList; }

    /**
     * Adds an Id.
     */
    public void addId(JExprId anId)
    {
        _idList = ArrayUtils.add(_idList, anId);
        addChild(anId);
    }

    /**
     * Returns the method arguments.
     */
    public JExpr[] getArgs()  { return _args; }

    /**
     * Sets the method arguments.
     */
    public void setArgs(JExpr[] theArgs)
    {
        _args = theArgs;
        Stream.of(_args).forEach(this::addChild);
    }

    /**
     * Returns the arg eval classes.
     */
    public JavaClass[] getArgClasses()
    {
        JExpr[] args = getArgs();
        JavaClass[] argTypes = new JavaClass[args.length];

        for (int i = 0, iMax = args.length; i < iMax; i++) {
            JExpr arg = args[i];
            argTypes[i] = arg != null ? arg.getEvalClass() : null;
        }

        return argTypes;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get class decl and constructor call arg types
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl.getJavaClass();
        if (enclosingClass == null)
            return null;
        JavaClass[] argClasses = getArgClasses();

        // If Super, switch to super class
        JExprId[] exprIds = getIds();
        String name = exprIds[0].getName();
        if (name.equals("super"))
            enclosingClass = enclosingClass.getSuperClass();

        // Get compatible constructor for arg types and return
        JavaDecl constr = JavaClassUtils.getCompatibleConstructor(enclosingClass, argClasses);
        if (constr != null)
            return constr;

        // Return not found
        return null;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // Check IdList
        if (anExprId.getParent() == this && ArrayUtils.containsId(_idList, anExprId))
            return getDecl();

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }
}
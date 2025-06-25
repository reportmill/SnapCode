package javakit.parse;
import javakit.resolver.*;

/**
 * This expression subclass represents a dot ('.') chain, e.g.: this.something().
 */
public class JExprDot extends JExpr {

    // The prefix expression
    private JExpr _prefixExpr;

    // The primary expression (id or method call)
    private JExpr _expr;

    /**
     * Constructor.
     */
    public JExprDot(JExpr prefixExpr, JExpr anExpr)
    {
        super();
        if (prefixExpr != null)
            setPrefixExpr(prefixExpr);
        if (anExpr != null)
            setExpr(anExpr);
    }

    /**
     * Returns the prefix expression.
     */
    public JExpr getPrefixExpr()  { return _prefixExpr; }

    /**
     * Sets the prefix expression.
     */
    public void setPrefixExpr(JExpr anExpr)
    {
        replaceChild(_prefixExpr, _prefixExpr = anExpr);
    }

    /**
     * Returns the primary expression (id, method call/ref).
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the primary expression (id, method call/ref).
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Override to construct chain.
     */
    @Override
    protected String getNameImpl()
    {
        String prefixName = _prefixExpr.getName();
        String exprName = _expr != null ? _expr.getName() : "(null)";
        return prefixName + '.' + exprName;
    }

    /**
     * Override to get decl from prefix and expression.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // If expression is MethodCall, return its decl
        if (!(_expr instanceof JExprId)) {
            JavaDecl exprDecl = _expr != null ? _expr.getDecl() : null;
            return exprDecl;
        }

        // Get id string
        String name = _expr.getName();
        if (name == null)
            return null;

        // Get prefix declaration
        JExpr prefixExpr = getPrefixExpr();
        JavaDecl prefixDecl = prefixExpr.getDecl();
        if (prefixDecl == null) {
            System.err.println("JExprDot.getDeclForExpr: No decl for prefix in " + getName());
            return null;
        }

        // Handle prefix is Package: Return child class or package for name
        if (prefixDecl instanceof JavaPackage javaPkg)
            return javaPkg.getChildForName(name);

        // Get eval class
        JavaClass parentClass = prefixExpr.getEvalClass();
        if (parentClass == null)
            return null;

        // Handle Class.this: Return parent declaration
        if (name.equals("this"))
            return parentClass; // was FieldName

        // Handle Class.class: Return ParamType for Class<T>
        if (name.equals("class")) {
            JavaClass classClass = getJavaClassForClass(Class.class);
            return classClass.getParameterizedTypeForTypes(parentClass);
        }

        // Handle inner class
        JavaClass innerClass = parentClass.getClassForName(name);
        if (innerClass != null)
            return innerClass;

        // Handle Field
        JavaField field = parentClass.getFieldForName(name);
        if (field != null) // && Modifier.isStatic(field.getModifiers()))
            return field;

        // Return not found
        return null;
    }

    /**
     * Override to support MethodCall.
     */
    @Override
    protected JavaType getEvalTypeImpl()
    {
        // If expression is MethodCall, return its EvalType
        if (!(_expr instanceof JExprId) && _expr != null)
            return _expr.getEvalType();

        // Do normal version
        return super.getEvalTypeImpl();
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "DotExpr"; }
}

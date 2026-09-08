package javakit.parse;
import javakit.resolver.*;

/**
 * This expression subclass represents a dot ('.') chain, e.g.: this.something().
 */
public class JExprDot extends JExpr {

    // The scope expression
    private JExpr _scopeExpr;

    // The primary expression (id or method call)
    private JExpr _expr;

    /**
     * Constructor.
     */
    public JExprDot(JExpr scopeExpr, JExpr anExpr)
    {
        super();
        if (scopeExpr != null)
            setScopeExpr(scopeExpr);
        if (anExpr != null)
            setExpr(anExpr);
    }

    /**
     * Returns the scope expression.
     */
    @Override
    public JExpr getScopeExpr()  { return _scopeExpr; }

    /**
     * Sets the scope expression.
     */
    public void setScopeExpr(JExpr anExpr)
    {
        _scopeExpr = anExpr;
        addChild(_scopeExpr, 0);
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
        String scopeName = _scopeExpr != null? _scopeExpr.getName() : "(null)";
        String exprName = _expr != null ? _expr.getName() : "(null)";
        return scopeName + '.' + exprName;
    }

    /**
     * Override to get decl from scope and expression.
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

        // Get scope declaration
        JExpr scopeExpr = getScopeExpr();
        JavaDecl scopeDecl = scopeExpr.getDecl();
        if (scopeDecl == null) {
            System.err.println("JExprDot.getDeclForExpr: No decl for scope in " + getName());
            return null;
        }

        // Handle scope is Package: Return child class or package for name
        if (scopeDecl instanceof JavaPackage javaPkg)
            return javaPkg.getChildForName(name);

        // Handle scope is Module: Return child module for name
        if (scopeDecl instanceof JavaModule javaModule)
            return javaModule.getChildForName(name);

        // Get eval class
        JavaClass parentClass = scopeExpr.getEvalClass();
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

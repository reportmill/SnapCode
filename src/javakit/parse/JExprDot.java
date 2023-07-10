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
        setPrefixExpr(prefixExpr);
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
        String name = _expr != null ? _expr.getName() : null;
        if (name == null)
            return null;

        // Get prefix declaration
        JExpr prefixExpr = getPrefixExpr();
        JavaDecl prefixDecl = prefixExpr.getDecl();
        if (prefixDecl == null) {
            System.err.println("JExprDot.getDeclForExpr: No decl for prefix in " + getName());
            return null;
        }

        // Handle ParameterizedType
        if (prefixDecl instanceof JavaParameterizedType)
            prefixDecl = ((JavaParameterizedType) prefixDecl).getRawType();

        // Handle prefix is Package: Return child class or package for name
        if (prefixDecl instanceof JavaPackage) {
            JavaPackage javaPkg = (JavaPackage) prefixDecl;
            return javaPkg.getChildForName(name);
        }

        // Handle prefix is Class: Look for ".this", ".class", static field or inner class
        else if (prefixDecl instanceof JavaClass) {

            // Get parent class
            JavaClass parentClass = (JavaClass) prefixDecl;

            // Handle Class.this: Return parent declaration
            if (name.equals("this"))
                return parentClass; // was FieldName

            // Handle Class.class: Return ParamType for Class<T>
            if (name.equals("class")) {
                JavaClass classClass = getJavaClassForClass(Class.class);
                return classClass.getParamTypeDecl(parentClass);
            }

            // Handle inner class
            JavaClass innerClass = parentClass.getInnerClassDeepForName(name);
            if (innerClass != null)
                return innerClass;

            // Handle Field
            JavaField field = parentClass.getFieldDeepForName(name);
            if (field != null) // && Modifier.isStatic(field.getModifiers()))
                return field;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "DotExpr"; }
}
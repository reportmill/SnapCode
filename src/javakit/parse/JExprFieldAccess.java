package javakit.parse;
import javakit.resolver.*;

/**
 * This class represents a field reference.
 */
public class JExprFieldAccess extends JExpr implements WithId {

    // The scope expression
    private JExpr _scopeExpr;

    // The identifier
    private JExprId _id;

    /**
     * Constructor.
     */
    public JExprFieldAccess(JExprId idExpr)
    {
        super();
        setId(idExpr);
    }

    /**
     * Returns the scope expression.
     */
    @Override
    public JExpr getScopeExpr()  { return _scopeExpr; }

    /**
     * Sets the scope expression.
     */
    public void setScopeExpr(JExpr scopeExpr)
    {
        addChild(_scopeExpr = scopeExpr, 0);
    }

    /**
     * Returns the identifier.
     */
    @Override
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        if (_id == null)
            addChild(_id = anId, 0);
        else replaceChild(_id, _id = anId);
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Override to get decl from scope and expression.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        String fieldName = _id.getName();
        if (fieldName == null)
            return null;

        // Get scope class
        JavaType scopeType = getScopeEvalType();
        JavaClass scopeClass = scopeType != null ? scopeType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Handle Class.this: Return parent declaration
        //if (fieldName.equals("this")) return scopeClass;

        // Handle Class.class: Return ParamType for Class<T>
        //if (fieldName.equals("class")) { JavaClass classClass = getJavaClassForClass(Class.class);
        //    return classClass.getParameterizedTypeForTypes(scopeClass); }

        // Handle inner class
        //JavaClass innerClass = parentClass.getClassForName(name); if (innerClass != null) return innerClass;

        // Handle Field
        JavaField field = scopeClass.getFieldForName(fieldName);
        if (field != null) // && Modifier.isStatic(field.getModifiers()))
            return field;

        // Return not found
        return null;
    }

    /**
     * Returns the JavaType for the scope expression (if present) or enclosing class.
     */
    private JavaType getScopeEvalType()
    {
        // If scope expression exists, forward to it
        JExpr scopeExpr = getScopeExpr();
        if (scopeExpr != null)
            return scopeExpr.getEvalType();

        // Otherwise, return enclosing class
        JClassDecl classDecl = getEnclosingClassDecl();
        return classDecl != null ? classDecl.getEvalType() : null;
    }
}

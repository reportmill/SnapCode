/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import java.util.List;

/**
 * A JExpr subclass for ArrayInit expressions.
 */
public class JExprArrayInit extends JExpr {

    // The array class
    private JavaClass _arrayClass;

    /**
     * Constructor.
     */
    public JExprArrayInit()
    {
        super();
    }

    /**
     * Adds an initializer expression: Either a basic expression or a nested JExprArrayInit for multidimensional arrays.
     */
    public void addExpr(JExpr anExpr)
    {
        addChild(anExpr);
    }

    /**
     * Returns the expressions.
     */
    public List<JExpr> getExpressions()  { return (List<JExpr>) (List<?>) _children; }

    /**
     * Returns the number of expressions.
     */
    public int getExprCount()  { return _children.size(); }

    /**
     * Returns the expression at given index.
     */
    public JExpr getExpr(int anIndex)  { return (JExpr) _children.get(anIndex); }

    /**
     * Returns the array class.
     */
    public JavaClass getArrayClass()
    {
        if (_arrayClass != null) return _arrayClass;
        return _arrayClass = getArrayClassImpl();
    }

    /**
     * Returns the array class.
     */
    private JavaClass getArrayClassImpl()
    {
        // Get parent - should be alloc expression or parent array initializer
        JNode parentNode = getParent();
        JavaClass arrayClass = parentNode.getEvalClass();

        // If parent is also array initializer (multidimensional array), then get component type
        if (parentNode instanceof JExprArrayInit)
            arrayClass = arrayClass.getComponentType();

        // Return parent class (parent should be alloc expression)
        return arrayClass;
    }

    /**
     * Override to return array class.
     */
    @Override
    protected JavaDecl getDeclImpl()  { return getArrayClass(); }
}

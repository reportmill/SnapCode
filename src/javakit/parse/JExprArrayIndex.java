/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for ArrayIndex expressions.
 */
public class JExprArrayIndex extends JExpr {

    // The expression for array
    protected JExpr  _arrayExpr;

    // The expression for array index
    protected JExpr  _indexExpr;

    /**
     * Creates a new ArrayIndex.
     */
    public JExprArrayIndex(JExpr anArrayExpr, JExpr anIndexExpr)
    {
        if (anArrayExpr != null) setArrayExpr(anArrayExpr);
        if (anIndexExpr != null) setIndexExpr(anIndexExpr);
    }

    /**
     * Returns the array expression.
     */
    public JExpr getArrayExpr()  { return _arrayExpr; }

    /**
     * Sets the index expression.
     */
    public void setArrayExpr(JExpr anExpr)
    {
        if (_arrayExpr == null)
            addChild(_arrayExpr = anExpr, 0);
        else replaceChild(_arrayExpr, _arrayExpr = anExpr);
    }

    /**
     * Returns the index expression.
     */
    public JExpr getIndexExpr()  { return _indexExpr; }

    /**
     * Sets the index expression.
     */
    public void setIndexExpr(JExpr anExpr)
    {
        replaceChild(_indexExpr, _indexExpr = anExpr);
    }

    /**
     * Returns the part name.
     **/
    public String getNodeString()  { return "ArrayIndex"; }

    /**
     * Tries to resolve the class name for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        JavaClass arrayClass = _arrayExpr != null ? _arrayExpr.getEvalClass() : null;
        JavaClass compClass = arrayClass != null && arrayClass.isArray() ? arrayClass.getComponentType() : null;
        return compClass != null ? compClass : getJavaClassForClass(Object.class);
    }
}

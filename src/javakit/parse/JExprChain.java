/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ListUtils;
import java.util.*;

/**
 * A class to represent a chain of expressions.
 */
public class JExprChain extends JExpr {

    /**
     * Constructor.
     */
    public JExprChain()  { }

    /**
     * Constructor for given parts.
     */
    public JExprChain(JExpr... theExprs)
    {
        for (JExpr expr : theExprs)
            addExpr(expr);
    }

    /**
     * Returns the number of expressions.
     */
    public int getExprCount()  { return _children.size(); }

    /**
     * Returns the individual expression at given index.
     */
    public JExpr getExpr(int anIndex)  { return (JExpr) _children.get(anIndex); }

    /**
     * Returns the individual expression at given index.
     */
    public JExpr getLastExpr()
    {
        int exprCount = getExprCount();
        return exprCount > 0 ? getExpr(exprCount - 1) : null;
    }

    /**
     * Returns the expressions list.
     */
    public List<JExpr> getExpressions()  { return (List<JExpr>) (List<?>) _children; }

    /**
     * Adds a expression to this JExprChain.
     */
    public void addExpr(JExpr anExpr)
    {
        addChild(anExpr, getChildCount());
    }

    /**
     * Override to construct chain.
     */
    @Override
    protected String getNameImpl()
    {
        List<JExpr> children = getExpressions();
        return ListUtils.joinString(children, ".", expr -> expr.getName());
    }

    /**
     * Tries to resolve the class declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        JExpr lastExpr = getLastExpr();
        return lastExpr != null ? lastExpr.getDecl() : null;
    }

    /**
     * Override to resolve names in chain.
     */
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Get parent expression - if not found (first in chain) do normal version
        JExpr parExpr = anExprId.getParentExpr();
        if (parExpr == null)
            return super.getDeclForChildExprIdNode(anExprId);

        // Get parent declaration
        String name = anExprId.getName();
        JavaDecl parDecl = parExpr.getDecl();
        if (parDecl == null) {
            System.err.println("JExprChain.getDeclForChildExprIdNode: No parent decl for " + name + " in " + getName());
            return null;
        }

        // Handle Parent is Package: Look for package sub-package or package class
        if (parDecl instanceof JavaPackage) {

            // Add name to parent package and look for child package
            JavaPackage javaPkg = (JavaPackage) parDecl;
            String packageName = javaPkg.getName();
            String classPath = packageName + '.' + name;
            if (isKnownPackageName(classPath))
                return getJavaPackageForName(classPath);

            // Look for class for name
            JavaClass javaClass = getJavaClassForName(classPath);
            if (javaClass != null)
                return javaClass;
        }

        // Handle Parent is Class: Look for ".this", ".class", static field or inner class
        else if (parDecl instanceof JavaClass) {

            // Get parent class
            JavaClass parentClass = (JavaClass) parDecl;

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

        // Handle any parent with class: Look for field
        else if (parExpr.getEvalType() != null) {

            // Handle Array.length reference
            JavaType parExprType = parExpr.getEvalType();
            if (parExprType.isArray() && name.equals("length"))
                return getJavaClassForClass(int.class);

            // Handle ParameterizedType
            if (parExprType instanceof JavaParameterizedType)
                parExprType = ((JavaParameterizedType) parExprType).getRawType();

            // Handle Class.Field
            if (parExprType instanceof JavaClass) {
                JavaClass javaClass = (JavaClass) parExprType;
                JavaField field = javaClass.getFieldDeepForName(name);
                if (field != null)
                    return field;
            }
        }

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "ExprChain"; }
}
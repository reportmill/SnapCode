/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * A JNode subclass for type variables (aka type parameters).
 * These are the generic types found in class, method, constructor declarations:
 * public class MyClass <T>
 * public <T> T myMethod(T anObj) { ... }
 */
public class JTypeVar extends JNode implements WithId {

    // The name identifier
    private JExprId _id;

    // The array of bounds types
    private JType[] _bounds = JType.EMPTY_TYPES_ARRAY;

    /**
     * Constructor.
     */
    public JTypeVar()
    {
        super();
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
        replaceChild(_id, _id = anId);
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Returns the bounds.
     */
    public JType[] getBounds()  { return _bounds; }

    /**
     * Adds a bound.
     */
    public void addBound(JType aType)
    {
        _bounds = ArrayUtils.add(_bounds, aType);
        addChild(aType);
    }

    /**
     * Returns the bounds type.
     */
    public JavaType getBoundsType()
    {
        if (_bounds == JType.EMPTY_TYPES_ARRAY || _bounds.length == 0)
            return getJavaClassForClass(Object.class);
        return _bounds[0].getJavaType();
    }

    /**
     * Override to get JavaDecl from parent decl (Class, Method).
     */
    protected JavaDecl getDeclImpl()  { return getJavaTypeVariable(); }

    /**
     * Returns the JavaTypeVariable from parent (Class, Method).
     */
    protected JavaTypeVariable getJavaTypeVariable()
    {
        // Get Parent declaration
        JNode parent = getParent();
        JavaDecl parentDecl = parent.getDecl();
        if (parentDecl == null)
            return null;

        // Handle Class
        String typeVarName = getName();
        if (parentDecl instanceof JavaClass) {
            JavaClass parentClass = (JavaClass) parentDecl;
            return parentClass.getTypeVarForName(typeVarName);
        }

        // Handle Executable (Method/Constructor)
        if (parentDecl instanceof JavaExecutable) {
            JavaExecutable parentMethod = (JavaExecutable) parentDecl;
            return parentMethod.getTypeVarForName(typeVarName);
        }

        // Return
        System.out.println("JTypeVar.getJavaTypeVariable: Unsupported parent type: " + parentDecl);
        return null;
    }

    /**
     * Override to handle ID and nested case, e.g.: T extends Class <? super T>
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // Handle nested case, e.g.: T extends Class <? super T>
        if (anExprId.getName().equals(getName()))
            return getJavaClassForClass(Object.class);

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaDecl getDeclForChildType(JType aJType)
    {
        // Handle nested case, e.g.: T extends Class <? super T>
        if (aJType.getName().equals(getName()))
            return getJavaClassForClass(Object.class);

        // Do normal version
        return super.getDeclForChildType(aJType);
    }
}
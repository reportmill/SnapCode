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

    // The TypeVariable
    private JavaTypeVariable _typeVariable;

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
     * Returns the bounds class.
     */
    public JavaClass getBoundsClass()
    {
        // If no bounds, just return Object
        if (_bounds == JType.EMPTY_TYPES_ARRAY || _bounds.length == 0)
            return getJavaClassForName("java.lang.Object");

        // Get bounds type - should always be ClassType (though maybe nested type like: Class <? super T>)
        JavaType boundsType = _bounds[0].getJavaClass();
        if (boundsType == null) {
            System.err.println("JTypeVar.getBoundsClass: Couldn't resolve bounds: " + _bounds[0]);
            boundsType = getJavaClassForName("java.lang.Object");
        }

        // Return bounds class
        return boundsType.getEvalClass();
    }

    /**
     * Returns the JavaTypeVariable from parent (Class, Method).
     */
    public JavaTypeVariable getTypeVariable()
    {
        if (_typeVariable != null) return _typeVariable;
        return _typeVariable = getTypeVariableImpl();
    }

    /**
     * Returns the JavaTypeVariable from parent (Class, Method).
     */
    private JavaTypeVariable getTypeVariableImpl()
    {
        // Get Parent declaration
        JNode parent = getParent();
        JavaDecl parentDecl = parent.getDecl();
        if (parentDecl == null)
            return null;

        // Handle Class
        String typeVarName = getName();
        if (parentDecl instanceof JavaClass parentClass)
            return parentClass.getTypeParameterForName(typeVarName);

        // Handle Executable (Method/Constructor)
        if (parentDecl instanceof JavaExecutable parentMethod)
            return parentMethod.getTypeParameterForName(typeVarName);

        // Return
        System.out.println("JTypeVar.getJavaTypeVariable: Unsupported parent type: " + parentDecl);
        return null;
    }

    /**
     * Override to get JavaDecl from parent decl (Class, Method).
     */
    @Override
    protected JavaDecl getDeclImpl()  { return getTypeVariable(); }

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
    protected JavaType getJavaTypeForChildType(JType aJType)
    {
        // Handle nested case, e.g.: T extends Class <? super T>
        if (aJType.getName().equals(getName()))
            return getJavaClassForClass(Object.class);

        // Do normal version
        return super.getJavaTypeForChildType(aJType);
    }
}
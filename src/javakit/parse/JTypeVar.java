/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.*;

/**
 * A JNode subclass for type variables (aka type paramters).
 * These are the unresolved types found in class, method, constructor declarations:
 * public class MyClass <T>
 * public <T> T myMethod(T anObj) { ... }
 */
public class JTypeVar extends JNode {

    // The name identifier
    JExprId _id;

    // The list of types
    List<JType> _types = new ArrayList();

    /**
     * Returns the identifier.
     */
    public JExprId getId()
    {
        return _id;
    }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
        if (_id != null) setName(_id.getName());
    }

    /**
     * Returns the types.
     */
    public List<JType> getTypes()
    {
        return _types;
    }

    /**
     * Adds a type.
     */
    public void addType(JType aType)
    {
        _types.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the bounds type.
     */
    public JavaType getBoundsType()
    {
        return _types.size() > 0 ? _types.get(0).getDecl() : getJavaClassForClass(Object.class);
    }

    /**
     * Override to get JavaDecl from parent decl (Class, Method).
     */
    protected JavaDecl getDeclImpl()
    {
        // Get Parent declaration
        JNode parent = getParent();
        JavaDecl parentDecl = parent.getDecl();
        if (parentDecl == null)
            return null;

        // Handle Class
        String name = getName();
        if (parentDecl instanceof JavaClass) {
            JavaClass parentClass = (JavaClass) parentDecl;
            JavaTypeVariable typeVar = parentClass.getTypeVarForName(name);
            return typeVar;
        }

        // Handle Executable (Method/Constructor)
        if (parentDecl instanceof JavaExecutable) {
            JavaExecutable parentMethod = (JavaExecutable) parentDecl;
            JavaTypeVariable typeVar = parentMethod.getTypeVarForName(name);
            return typeVar;
        }

        // Return
        System.out.println("JTypeVar.getDeclImpl: Unsupported parent type: " + parentDecl);
        return null;
    }

    /**
     * Override to handle ID and nested case, e.g.: T extends Class <? super T>
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Handle ID
        if (anExprId == _id)
            return getDecl();

        // Handle nested case, e.g.: T extends Class <? super T>
        if (anExprId.getName().equals(getName()))
            return getJavaClassForClass(Object.class);

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaDecl getDeclForChildTypeNode(JType aJType)
    {
        // Handle nested case, e.g.: T extends Class <? super T>
        if (aJType.getName().equals(getName()))
            return getJavaClassForClass(Object.class);

        // Do normal version
        return super.getDeclForChildTypeNode(aJType);
    }
}
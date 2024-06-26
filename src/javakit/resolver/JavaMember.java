/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;

/**
 * This class represents
 */
public class JavaMember extends JavaDecl {

    // The declaring class
    protected JavaClass  _declaringClass;

    // The modifiers
    protected int  _mods;

    /**
     * Constructor.
     */
    public JavaMember(Resolver aResolver, DeclType aType, JavaClass aDeclaringClass)
    {
        super(aResolver, aType);
        _declaringClass = aDeclaringClass;
    }

    /**
     * Returns the declaring class.
     */
    public JavaClass getDeclaringClass()  { return _declaringClass; }

    /**
     * Returns the declaring class name.
     */
    public String getDeclaringClassName()  { return _declaringClass.getName(); }

    /**
     * Returns the modifiers.
     */
    public int getModifiers()  { return _mods; }

    /**
     * Returns whether member is public.
     */
    public boolean isPublic()  { return Modifier.isPublic(_mods); }

    /**
     * Returns whether member is protected.
     */
    public boolean isProtected()  { return Modifier.isProtected(_mods); }

    /**
     * Returns whether member is private.
     */
    public boolean isPrivate()  { return Modifier.isPrivate(_mods); }

    /**
     * Returns whether member is static.
     */
    public boolean isStatic()  { return Modifier.isStatic(_mods); }
}

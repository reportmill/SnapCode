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
    public JavaMember(Resolver aResolver, DeclType aType, JavaClass aDeclaringClass, Member aMember)
    {
        super(aResolver, aType);
        if (aMember == null) return;

        // Set id
        _id = ResolverUtils.getIdForMember(aMember);

        // Set mods, declaring class
        _mods = aMember.getModifiers();
        _declaringClass = aDeclaringClass;

        // Set name/simple name
        _name = _simpleName = aMember.getName();
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
     * Returns whether member is static.
     */
    public boolean isStatic()  { return Modifier.isStatic(_mods); }

    /**
     * Returns the full name.
     */
    @Override
    protected String getFullNameImpl()
    {
        // Get Match name
        String name = getMatchName();
        if (this instanceof JavaMethod || this instanceof JavaField)
            name = getEvalTypeName() + " " + name;

        // Add mod string
        String modifierStr = Modifier.toString(_mods);
        if (modifierStr.length() > 0)
            name = modifierStr + " " + name;

        // Return
        return name;
    }
}

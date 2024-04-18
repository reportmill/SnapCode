/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

/**
 * This class represents a generic Java Type: Class, ParameterizedType, TypeVariable, GenericArrayType.
 */
public class JavaType extends JavaDecl {

    // Constant for empty types array
    public static final JavaType[] EMPTY_TYPES_ARRAY = new JavaType[0];

    /**
     * Constructor.
     */
    protected JavaType(Resolver aResolver, DeclType aType)
    {
        super(aResolver, aType);

        // JavaTypes are their own eval type
        _evalType = this;
    }

    /**
     * Returns the class name.
     */
    public String getClassName()
    {
        JavaClass evalClass = getEvalClass();
        return evalClass != null ? evalClass.getName() : null;
    }

    /**
     * Returns whether is a enum reference.
     */
    public boolean isEnum()  { return false; }

    /**
     * Returns whether is a interface reference.
     */
    public boolean isInterface()  { return false; }

    /**
     * Returns whether is an array.
     */
    public boolean isArray()  { return false; }

    /**
     * Returns the Array component type (if Array).
     */
    public JavaType getComponentType()  { return null; }

    /**
     * Returns whether is primitive.
     */
    public boolean isPrimitive()  { return false; }

    /**
     * Returns the primitive counter part, if available.
     */
    public JavaClass getPrimitiveAlt()  { return null; }

    /**
     * Returns common ancestor of this type and given type.
     */
    public JavaType getCommonAncestor(JavaType aType)
    {
        // If same class, return it
        if (aType == this)
            return this;

        // Handle primitive
        if (isPrimitive() && aType.isPrimitive())
            return getCommonAncestorPrimitive(aType);
        else if (isPrimitive())
            return getPrimitiveAlt().getCommonAncestor(aType);
        else if (aType.isPrimitive())
            return getCommonAncestor(aType.getPrimitiveAlt());

        // Iterate up each super chain to check
        JavaClass thisClass = getEvalClass();
        JavaClass otherClass = aType.getEvalClass();
        for (JavaClass cls1 = thisClass; cls1 != null; cls1 = cls1.getSuperClass()) {
            for (JavaClass cls2 = otherClass; cls2 != null; cls2 = cls2.getSuperClass()) {
                if (cls1 == cls2)
                    return cls1;
            }
        }

        // Return Object (case where at least one was interface or ParamType of interface)
        return getJavaClassForClass(Object.class);
    }

    /**
     * Returns common ancestor of this decl and given decls.
     */
    protected JavaType getCommonAncestorPrimitive(JavaType aDecl)
    {
        String n0 = getName();
        String n1 = aDecl.getName();
        if (n0.equals("double")) return this;
        if (n1.equals("double")) return aDecl;
        if (n0.equals("float")) return this;
        if (n1.equals("float")) return aDecl;
        if (n0.equals("long")) return this;
        if (n1.equals("long")) return aDecl;
        if (n0.equals("int")) return this;
        if (n1.equals("int")) return aDecl;
        if (n0.equals("short")) return this;
        if (n1.equals("short")) return aDecl;
        if (n0.equals("char")) return this;
        if (n1.equals("char")) return aDecl;
        return this;
    }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    public boolean isResolvedType()  { return true; }

    /**
     * Returns a resolved type for given TypeVar.
     */
    public JavaType getResolvedTypeForTypeVariable(JavaTypeVariable aTypeVar)
    {
        return aTypeVar.getEvalType();
    }

    /**
     * Returns the Array decl for this base class.
     */
    public JavaType getArrayType()
    {
        // Handle ParamType or unexpected type: Return ClassType.getArrayTypeDecl()
        if (this instanceof JavaGenericArrayType)
            System.err.println("JavaType.getArrayTypeDecl: Unexpected type: " + this);

        // Forward to ClassType
        JavaClass javaClass = getEvalClass();
        return javaClass.getArrayType();
    }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // Handle ParamTypes: Test against ClassType instead
        if (this instanceof JavaParameterizedType)
            return getEvalClass().matches(aDecl);
        else if (aDecl instanceof JavaParameterizedType)
            return matches(aDecl.getEvalClass());

        // Return false, since no match
        return false;
    }

    /**
     * Returns whether this type is assignable from other type. Not sure whether this is lame or not.
     */
    public boolean isAssignableFrom(JavaType otherType)
    {
        JavaClass thisClass = getEvalClass();
        JavaClass otherClass = otherType.getEvalClass();
        return thisClass.isAssignableFrom(otherClass);
    }
}

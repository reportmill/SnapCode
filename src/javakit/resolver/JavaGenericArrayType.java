/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * This class represents a Java GenericArrayType.
 */
public class JavaGenericArrayType extends JavaType {

    // The Array component type
    private JavaType  _componentType;

    // The eval class
    private JavaClass _evalClass;

    /**
     * Constructor.
     */
    public JavaGenericArrayType(Resolver anOwner, GenericArrayType aGenArrayType)
    {
        // Do normal version
        super(anOwner, DeclType.GenArrayType);

        // Set Id
        _id = ResolverIds.getIdForGenericArrayType(aGenArrayType);

        // Set Name, SimpleName
        _name = _simpleName = aGenArrayType.getTypeName();

        // Set EvalClass: Probably need to do better than this
        _evalClass = getJavaClassForClass(Object[].class);

        // Set ComponentType
        Type compType = aGenArrayType.getGenericComponentType();
        _componentType = anOwner.getJavaTypeForType(compType);
    }

    /**
     * Constructor.
     */
    public JavaGenericArrayType(JavaType componentType)
    {
        // Do normal version
        super(componentType._resolver, DeclType.GenArrayType);

        // Set Id
        _id = componentType.getId() + "[]";

        // Set Name, SimpleName
        _name = _simpleName = componentType.getName() + "[]";

        // Set EvalClass: Probably need to do better than this
        _evalClass = getJavaClassForClass(Object[].class);

        // Set ComponentType
        _componentType = componentType;
    }

    /**
     * Returns whether is an array.
     */
    @Override
    public boolean isArray()  { return true; }

    /**
     * Returns the Array component type.
     */
    @Override
    public JavaType getComponentType()  { return _componentType; }

    /**
     * Override to return bounding class.
     */
    @Override
    public JavaClass getEvalClass()  { return _evalClass; }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }

    /**
     * Override to return if component type has given type variable.
     */
    @Override
    public boolean hasTypeVar(JavaTypeVariable typeVar)  { return _componentType.hasTypeVar(typeVar); }
}

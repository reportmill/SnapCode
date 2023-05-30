/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.TypeVariable;

/**
 * This class represents a Java TypeVariable.
 */
public class JavaTypeVariable extends JavaType {

    // The Class or Executable that owns this TypeVariable
    private JavaDecl  _owner;

    /**
     * Constructor.
     */
    public JavaTypeVariable(Resolver aResolver, JavaDecl anOwner, TypeVariable<?> typeVar)
    {
        // Do normal version
        super(aResolver, DeclType.TypeVar);

        // Set Id
        _id = ResolverUtils.getIdForTypeVariable(typeVar);

        // Set Owner: Class or Method/Constructor
        _owner = anOwner;

        // Set Name, SimpleName
        _name = _simpleName = typeVar.getName();

        // Set EvalType
        Class<?> typeVarClass = ResolverUtils.getClassForType(typeVar);
        _evalType = getJavaClassForClass(typeVarClass);
    }

    /**
     * Returns the Class or Executable that owns this TypeVariable.
     */
    public JavaDecl getOwner()  { return _owner; }

    /**
     * Override to return false.
     */
    @Override
    public boolean isResolvedType()  { return false; }
}

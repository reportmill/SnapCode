/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;

/**
 * This class represents a Java ParameterizedType.
 */
public class JavaParameterizedType extends JavaType {

    // The RawType
    private JavaClass _rawType;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    /**
     * Constructor.
     */
    public JavaParameterizedType(Resolver aResolver, JavaClass aRawType, JavaType[] theTypeArgs)
    {
        // Do normal version
        super(aResolver, DeclType.ParamType);

        // Set RawType, ParamTypes
        _rawType = aRawType;
        _paramTypes = theTypeArgs;

        // Set Id, Name
        _id = _name = ResolverIds.getIdForParameterizedTypeParts(aRawType, theTypeArgs);

        // Get/Set SimpleName
        _simpleName = aRawType.getSimpleName();
        if (theTypeArgs.length > 0) {
            String typeArgsStr = ArrayUtils.mapToStringsAndJoin(_paramTypes, type -> type.getSimpleName(), ",");
            _simpleName += '<' + typeArgsStr + '>';
        }
    }

    /**
     * Returns the RawType.
     */
    public JavaClass getRawType()  { return _rawType; }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParamTypes()  { return _paramTypes; }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    @Override
    public boolean isResolvedType()
    {
        JavaType[] paramTypes = getParamTypes();
        return !ArrayUtils.hasMatch(paramTypes, type -> !type.isResolvedType());
    }

    /**
     * Override to return RawType.
     */
    @Override
    public JavaClass getEvalClass()  { return _rawType; }

    /**
     * Override to return if this type variable matches given type variable.
     */
    @Override
    public boolean hasTypeVar(JavaTypeVariable typeVar)
    {
        return ArrayUtils.hasMatch(_paramTypes, type -> type.hasTypeVar(typeVar));
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    @Override
    public JavaType getResolvedTypeForTypeVariable(JavaTypeVariable aTypeVar)
    {
        // Search for TypeVar name in ParamTypes
        JavaClass javaClass = getEvalClass();
        String typeVarName = aTypeVar.getName();

        // If class (or superclasses or interfaces) can resolve type var, return type
        JavaType resolvedType = getResolvedTypeForClass(typeVarName, javaClass);
        if (resolvedType != null)
            return resolvedType;

        // Do normal version
        return null; //super.getResolvedTypeForTypeVariable(aTypeVar);
    }

    /**
     * Returns resolved type for interfaces.
     */
    private JavaType getResolvedTypeForClass(String typeVarName, JavaClass javaClass)
    {
        // Check class
        JavaTypeVariable[] typeParams = javaClass.getTypeParameters();
        int typeParamIndex = ArrayUtils.findMatchIndex(typeParams, tvar -> tvar.getName().equals(typeVarName));
        if (typeParamIndex >= 0 && typeParamIndex < _paramTypes.length)
            return _paramTypes[typeParamIndex];

        // Check superclass
        JavaClass superClass = javaClass.getSuperClass();
        if (superClass != null) {
            JavaType resolvedType = getResolvedTypeForClass(typeVarName, superClass);
            if (resolvedType != null)
                return resolvedType;
        }

        // Check interfaces
        JavaClass[] interfaces = javaClass.getInterfaces();
        for (JavaClass interfc : interfaces) {
            JavaType resolvedType = getResolvedTypeForClass(typeVarName, interfc);
            if (resolvedType != null)
                return resolvedType;
        }

        // Return not found
        return null;
    }
}

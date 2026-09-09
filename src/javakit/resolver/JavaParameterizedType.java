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
}

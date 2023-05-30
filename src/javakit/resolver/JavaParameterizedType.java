/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.StringUtils;

/**
 * This class represents a Java ParameterizedType.
 */
public class JavaParameterizedType extends JavaType {

    // The RawType
    private JavaType  _rawType;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    /**
     * Constructor.
     */
    public JavaParameterizedType(Resolver aResolver, JavaType aRawType, JavaType[] theTypeArgs)
    {
        // Do normal version
        super(aResolver, DeclType.ParamType);

        // Set RawType, ParamTypes
        _rawType = aRawType;
        _paramTypes = theTypeArgs;

        // Set Id, Name
        _id = _name = ResolverUtils.getIdForParameterizedTypeParts(aRawType, theTypeArgs);

        // Get/Set SimpleName
        _simpleName = aRawType.getSimpleName();
        if (theTypeArgs.length > 0) {
            String typeArgsStr = StringUtils.join(getParamTypeSimpleNames(), ",");
            _simpleName = _simpleName + '<' + typeArgsStr + '>';
        }

        // Set EvalType to RawType
        _evalType = aRawType;
    }

    /**
     * Returns the RawType.
     */
    public JavaType getRawType()  { return _rawType; }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParamTypes()  { return _paramTypes; }

    /**
     * Returns the parameter type simple names.
     */
    public String[] getParamTypeSimpleNames()
    {
        String[] names = new String[_paramTypes.length];
        for (int i = 0; i < names.length; i++) names[i] = _paramTypes[i].getSimpleName();
        return names;
    }

    /**
     * Returns whether is Type is explicit (doesn't contain any type variables).
     */
    public boolean isResolvedType()
    {
        // ParamType might subclass TypeVars
        JavaType rawType = getRawType();
        if (!rawType.isResolvedType())
            return false;

        // Or types might include TypeVars
        JavaType[] paramTypes = getParamTypes();
        for (JavaType paramType : paramTypes)
            if (!paramType.isResolvedType())
                return false;

        // Return
        return true;
    }

    /**
     * Returns a resolved type for given unresolved type (TypeVar or ParamType<TypeVar>), if this decl can resolve it.
     */
    @Override
    public JavaType getResolvedType(JavaType aType)
    {
        // Search for TypeVar name in ParamTypes
        JavaClass javaClass = getEvalClass();
        String typeVarName = aType.getName();
        int ind = javaClass.getTypeVarIndexForName(typeVarName);
        if (ind >= 0 && ind < _paramTypes.length)
            return _paramTypes[ind];

        // Do normal version
        return super.getResolvedType(aType);
    }
}

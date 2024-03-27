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
            String typeArgsStr = StringUtils.join(getParamTypeSimpleNames(), ",");
            _simpleName = _simpleName + '<' + typeArgsStr + '>';
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
        // Types might include TypeVars
        JavaType[] paramTypes = getParamTypes();
        for (JavaType paramType : paramTypes)
            if (!paramType.isResolvedType())
                return false;

        // Return
        return true;
    }

    /**
     * Override to return RawType.
     */
    @Override
    public JavaClass getEvalClass()  { return _rawType; }

    /**
     * Returns a resolved type for given type var, if type var name defined by base class.
     */
    public JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // Get type var name and this parameter type base class and type vars
        String typeVarName = aTypeVar.getName();
        JavaClass baseClass = getRawType();
        JavaTypeVariable[] typeVars = baseClass.getTypeVars();

        // Iterate over type vars - if type var name found, return respective parameter type
        for (int i = 0, iMax = typeVars.length; i < iMax; i++) {
            JavaTypeVariable typeVar = typeVars[i];
            if (typeVar.getName().equals(typeVarName)) {
                JavaType[] paramTypes = getParamTypes();
                if (i < paramTypes.length)
                    return paramTypes[i];
            }
        }

        // Return not found
        return null;
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
        return super.getResolvedTypeForTypeVariable(aTypeVar);
    }

    /**
     * Returns resolved type for interfaces.
     */
    private JavaType getResolvedTypeForClass(String typeVarName, JavaClass javaClass)
    {
        // Check class
        int ind = javaClass.getTypeVarIndexForName(typeVarName);
        if (ind >= 0 && ind < _paramTypes.length)
            return _paramTypes[ind];

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

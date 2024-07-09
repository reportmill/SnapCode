package javakit.resolver;
import snap.util.ArrayUtils;

/**
 * Utility methods for JavaType.
 */
public class JavaTypeUtils {

    /**
     * Returns the resolved type for given type variable and array of generic types and array of resolved types.
     */
    public static JavaType getResolvedTypeForTypeArrays(JavaType aType, JavaType[] paramTypes, JavaType[] argTypes)
    {
        if (aType instanceof JavaTypeVariable)
            return getResolvedTypeVariableForTypeArrays((JavaTypeVariable) aType, paramTypes, argTypes);

        // Complain and return
        System.err.println("JavaTypeUtils.getResolvedTypeForTypeArrays: Unsupported type: " + aType);
        return null;
    }

    /**
     * Returns the resolved type for given type variable and array of generic types and array of resolved types.
     */
    public static JavaType getResolvedTypeVariableForTypeArrays(JavaTypeVariable aTypeVar, JavaType[] paramTypes, JavaType[] argTypes)
    {
        // Iterate over method parameter types to see if any can resolve the type var name
        for (int i = 0; i < paramTypes.length; i++) {

            // If paramType doesn't reference type var, just continue
            JavaType paramType = paramTypes[i];
            if (!paramType.hasTypeVar(aTypeVar))
                continue;

            // Get arg type
            JavaType argType = argTypes[i];
            if (argType != null) {
                JavaType resolvedType = getResolvedTypeVariableForTypes(aTypeVar, paramType, argType);
                if (resolvedType != null)
                    return resolvedType;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns resolved type for given type variable, given a generic type and a resolved type.
     * Returns null if generic type doesn't reference type variable.
     */
    private static JavaType getResolvedTypeVariableForTypes(JavaTypeVariable aTypeVar, JavaType paramType, JavaType argType)
    {
        // Handle TypeVar: If name matches, return arg type
        if (paramType instanceof JavaTypeVariable) {
            JavaTypeVariable paramTypeVar = (JavaTypeVariable) paramType;
            if (paramTypeVar.getName().equals(aTypeVar.getName()))
                return argType;
            return null;
        }

        // Handle Parameterized type
        if (paramType instanceof JavaParameterizedType) {

            // Get types as JavaParameterized types
            JavaParameterizedType paramParamType = (JavaParameterizedType) paramType;
            JavaParameterizedType argParamType = argType instanceof JavaParameterizedType ? (JavaParameterizedType) argType : null;
            if (argParamType == null) {
                System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: arg type not parameterized type");
                return null;
            }

            // Get arrays of parameter types
            JavaType[] paramParamTypes = paramParamType.getParamTypes();
            JavaType[] argParamTypes = argParamType.getParamTypes();
            if (paramParamTypes.length != argParamTypes.length) {
                System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: param types length mismatch");
                return null;
            }

            // Forward to type arrays version
            return getResolvedTypeVariableForTypeArrays(aTypeVar, paramParamTypes, argParamTypes);
        }

        // Handle array type: Get component types and recurse
        if (paramType instanceof JavaGenericArrayType) {
            JavaType paramCompType = paramType.getComponentType();
            JavaType argCompType = argType.getComponentType();
            return getResolvedTypeVariableForTypes(aTypeVar, paramCompType, argCompType);
        }

        // Complain and return
        System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: Unsupported type: " + paramType);
        return null;
    }

    /**
     * Translates given parameter types from given class to given subclass.
     */
    public static JavaType[] translateParamTypesToSubclass(JavaType[] paramTypes, JavaClass paramsClass, JavaType subtype)
    {
        // If other class between paramsClass and subtype class, recurse
        JavaClass subtypeClass = subtype.getEvalClass();
        JavaType subtypeGenericSuperInterface = getSuperInterfaceForClass(subtypeClass, paramsClass);
        JavaClass subtypeSuperInterface = subtypeGenericSuperInterface != null ? subtypeGenericSuperInterface.getEvalClass() : null;
        if (paramsClass != subtypeSuperInterface) {
            if (subtypeSuperInterface == null) {
                System.err.println("JavaTypeUtils.translateParamTypesToSubclass: Subtype not subclass or params class");
                return paramTypes;
            }
            paramTypes = translateParamTypesToSubclass(paramTypes, paramsClass, subtypeGenericSuperInterface);
            paramsClass = subtypeSuperInterface;
        }

        // Get params class type variables and subtype types and translate
        JavaType[] paramsClassTypeVars = paramsClass.getTypeVars();
        JavaType[] subtypeTypes = ((JavaParameterizedType) subtypeGenericSuperInterface).getParamTypes();
        return ArrayUtils.map(paramTypes, type -> getResolvedTypeForTypeArrays(type, paramsClassTypeVars, subtypeTypes), JavaType.class);
    }

    /**
     * Returns the super interface for given sub-interface, or an intermediate, if classes not direct subclass-superclass.
     */
    private static JavaClass getSuperInterfaceForClass(JavaClass subInterface, JavaClass superInterface)
    {
        // If sub interface subclasses superInterface, just return superInterface
        JavaClass[] subtypeSuperInterfaces = subInterface.getInterfaces();
        if (ArrayUtils.containsId(subtypeSuperInterfaces, superInterface))
            return superInterface;

        // Search subtype-interfaces for super interface recursively and return first intermediate
        for (JavaClass subtypeSuperInterface : subtypeSuperInterfaces) {
            JavaClass superInterfaceForClass = getSuperInterfaceForClass(subtypeSuperInterface, superInterface);
            if (superInterfaceForClass != null)
                return subtypeSuperInterface;
        }

        // Return not found
        return null;
    }
}

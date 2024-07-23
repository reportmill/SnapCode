package javakit.resolver;
import snap.util.ArrayUtils;
import java.util.Arrays;

/**
 * Utility methods for JavaType.
 */
public class JavaTypeUtils {

    /**
     * Returns a resolved type for given type variable, method and arg types.
     */
    public static JavaType getResolvedTypeForTypeVarAndMethodAndArgTypes(JavaTypeVariable aTypeVar, JavaExecutable method, JavaType[] argTypes)
    {
        // Get method parameter types
        JavaType[] paramTypes = method.getGenericParameterTypes();

        // Handle VarArgs
        if (method.isVarArgs()) {

            // If no var args provided, trim param types
            if (argTypes.length < paramTypes.length)
                paramTypes = Arrays.copyOf(paramTypes, argTypes.length);

            // Otherwise trim and convert trailing arg types to array
            else {
                argTypes = Arrays.copyOf(argTypes, paramTypes.length);
                JavaType compClass = argTypes[paramTypes.length - 1];
                argTypes[paramTypes.length -1 ] = compClass.getArrayType();
            }
        }

        // Forward to getResolvedTypeVariableForTypeArrays()
        return getResolvedTypeVariableForTypeArrays(aTypeVar, paramTypes, argTypes);
    }

    /**
     * Returns the resolved type for given type variable and array of generic types and array of resolved types.
     */
    public static JavaType getResolvedTypeForTypeArrays(JavaType aType, JavaType[] paramTypes, JavaType[] argTypes)
    {
        if (aType instanceof JavaTypeVariable)
            return getResolvedTypeVariableForTypeArrays((JavaTypeVariable) aType, paramTypes, argTypes);

        // Complain and return
        System.err.println("JavaTypeUtils.getResolvedTypeForTypeArrays: Unsupported type: " + aType);
        return aType;
    }

    /**
     * Returns the resolved type for given type variable and array of generic types and array of resolved types.
     */
    public static JavaType getResolvedTypeVariableForTypeArrays(JavaTypeVariable aTypeVar, JavaType[] paramTypes, JavaType[] argTypes)
    {
        int arrayLength = Math.min(paramTypes.length, argTypes.length);

        // Iterate over method parameter types to see if any can resolve the type var name
        for (int i = 0; i < arrayLength; i++) {

            // If paramType doesn't reference type var, just continue
            JavaType paramType = paramTypes[i];
            if (!paramType.hasTypeVar(aTypeVar))
                continue;

            // Get arg type
            JavaType argType = argTypes[i];
            if (argType != null) {
                JavaType resolvedType = getResolvedTypeVariableForTypes(aTypeVar, paramType, argType);
                if (resolvedType != aTypeVar)
                    return resolvedType;
            }
        }

        // Return not found
        return aTypeVar;
    }

    /**
     * Returns resolved type for given type variable, given a generic type and a resolved type.
     * Returns given type variable if generic type doesn't reference type variable.
     */
    public static JavaType getResolvedTypeVariableForTypes(JavaTypeVariable aTypeVar, JavaType paramType, JavaType argType)
    {
        // Handle TypeVar: If name matches, return arg type
        if (paramType instanceof JavaTypeVariable) {
            JavaTypeVariable paramTypeVar = (JavaTypeVariable) paramType;
            if (paramTypeVar.getName().equals(aTypeVar.getName()))
                return argType;
            return aTypeVar;
        }

        // Handle Parameterized type
        if (paramType instanceof JavaParameterizedType) {

            // Get types as JavaParameterized types
            JavaParameterizedType paramParamType = (JavaParameterizedType) paramType;
            JavaParameterizedType argParamType = argType instanceof JavaParameterizedType ? (JavaParameterizedType) argType : null;
            if (argParamType == null) {
                System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: arg type not parameterized type");
                return aTypeVar;
            }

            // Get arrays of parameter types
            JavaType[] paramParamTypes = paramParamType.getParamTypes();
            JavaType[] argParamTypes = argParamType.getParamTypes();
            if (paramParamTypes.length != argParamTypes.length) {
                System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: param types length mismatch");
                return aTypeVar;
            }

            // Forward to type arrays version
            return getResolvedTypeVariableForTypeArrays(aTypeVar, paramParamTypes, argParamTypes);
        }

        // Handle array type: Get component types and recurse
        if (paramType instanceof JavaGenericArrayType) {
            JavaType paramCompType = paramType.getComponentType();
            if (argType.isArray()) {
                JavaType argCompType = argType.getComponentType();
                return getResolvedTypeVariableForTypes(aTypeVar, paramCompType, argCompType);
            }
            return aTypeVar;
        }

        // Complain and return
        System.err.println("JavaTypeUtils.getResolvedTypeVariableForTypes: Unsupported type: " + paramType);
        return aTypeVar;
    }

    /**
     * Translates given parameter type from given class to given subclass.
     * E.g.: Translate method params from BiFunction.apply(T,U) to subclass BinaryOperator<Integer> where
     *      BinaryOperator<Integer> extends BinaryOperator<T> extends BiFunction<T,T,T> extends BiFunction<T,U,R>.
     */
    public static JavaType translateParamTypeToSubclass(JavaType paramType, JavaClass paramsClass, JavaType subtype)
    {
        // Get subtype as parameterized type
        JavaParameterizedType subtypeParamType = subtype instanceof JavaParameterizedType ? (JavaParameterizedType) subtype : null;
        if (subtypeParamType == null) {
            System.err.println("JavaTypeUtils.translateParamTypesToSubclass: subtype not parameterized type");
            return paramType;
        }

        // If other class between paramsClass and subtype class, recurse
        JavaClass subtypeClass = subtype.getEvalClass();
//        JavaType subtypeGenericSuperInterface = getSuperInterfaceForClass(subtypeClass, paramsClass);
//        JavaClass subtypeSuperInterface = subtypeGenericSuperInterface != null ? subtypeGenericSuperInterface.getEvalClass() : null;
//        if (paramsClass != subtypeSuperInterface) {
//            if (subtypeSuperInterface == null) {
//                System.err.println("JavaTypeUtils.translateParamTypesToSubclass: Subtype not subclass or params class");
//                return paramTypes;
//            }
//            paramTypes = translateParamTypesToSubclass(paramTypes, paramsClass, subtypeGenericSuperInterface);
//            paramsClass = subtypeSuperInterface;
//        }

        // Convert paramType from paramsClass to subtype genericSuperclass (E.g: (T, U) from BiFunction<T,U,R> to BiFunction<T,T,T>)
        JavaTypeVariable[] paramsClassTypeVars = paramsClass.getTypeParameters(); // E.g.: <T,U,R> from BiFunction <T,U,R>
        JavaParameterizedType subtypeGenericSuperclass = getGenericSuperclass(subtypeClass);
        if (subtypeGenericSuperclass == null) {
            System.err.println("JavaTypeUtils.translateParamTypesToSubclass: subtype superclass not parameterized type");
            return paramType;
        }
        JavaType[] subtypeGenericSuperclassTypes = subtypeGenericSuperclass.getParamTypes(); // E.g.: <T,T,T> from BiFunction <T,T,T>
        JavaType paramType2 = getResolvedTypeForTypeArrays(paramType, paramsClassTypeVars, subtypeGenericSuperclassTypes);

        // Convert paramType from subtype.evalClass to subtype (E.g: (T, T, T) from BinaryOperator<T> to BinaryOperator<Integer>)
        JavaTypeVariable[] subtypeClassTypeParams = subtypeClass.getTypeParameters(); // E.g.: <T> from BinaryOperator <T>
        JavaType[] subtypeParamTypes = subtypeParamType.getParamTypes(); // E.g.: <Integer> from BinaryOperator <Integer>
        return getResolvedTypeForTypeArrays(paramType2, subtypeClassTypeParams, subtypeParamTypes);
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

    /**
     * Returns the generic superclass of given type.
     */
    private static JavaParameterizedType getGenericSuperclass(JavaClass javaClass)
    {
        // If not interface, just return class
        if (!javaClass.isInterface()) {
            JavaType genericSuperclass = javaClass.getGenericSuperclass();
            if (genericSuperclass instanceof JavaParameterizedType)
                return (JavaParameterizedType) genericSuperclass;
            return null;
        }

        // Return first interface
        JavaType[] genericInterfaces = javaClass.getGenericInterfaces();
        JavaType genericSuperclass = genericInterfaces.length > 0 ? genericInterfaces[0] : null;
        if (genericSuperclass instanceof JavaParameterizedType)
            return (JavaParameterizedType) genericSuperclass;

        // Return not found
        return null;
    }
}

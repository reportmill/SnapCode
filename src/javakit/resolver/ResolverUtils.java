/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for JavaParse package.
 */
public class ResolverUtils {

    /**
     * Returns a class for given class coding.
     */
    public static String getClassNameForClassCoding(String aName)
    {
        char char0 = aName.charAt(0);
        switch (char0) {
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'Z': return "boolean";
            case 'V': return "void";
            case 'L':
                int end = aName.indexOf(';', 1);
                return aName.substring(1, end);
            case '[': return getClassNameForClassCoding(aName.substring(1)) + "[]";
        }

        // Unsupported coding char
        throw new RuntimeException("ResolverUtils.getClassNameForClassCoding: Not a coded class string " + aName);
    }

    /**
     * Returns an Id for a Java.lang.Class.
     */
    public static String getIdForClass(Class<?> aClass)
    {
        if (aClass.isArray())
            return getIdForClass(aClass.getComponentType()) + "[]";
        return aClass.getName();
    }

    /**
     * Returns an Id for Java.lang.reflect.Type.
     */
    public static String getIdForType(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return getIdForClass((Class<?>) aType);

        // Handle GenericArrayType: CompType[]
        if (aType instanceof GenericArrayType)
            return getIdForGenericArrayType((GenericArrayType) aType);

        // Handle ParameterizedType: RawType<TypeArg,...>
        if (aType instanceof ParameterizedType)
            return getIdForParameterizedType((ParameterizedType) aType);

        // Handle TypeVariable: DeclType.Name
        if (aType instanceof TypeVariable)
            return getIdForTypeVariable((TypeVariable<?>) aType);

        // Handle WildcardType: Need to fix for
        WildcardType wc = (WildcardType) aType;
        Type[] bounds = wc.getLowerBounds().length > 0 ? wc.getLowerBounds() : wc.getUpperBounds();
        Type bound = bounds[0];
        String boundStr = getIdForType(bound);
        return boundStr;
    }

    /**
     * Returns an Id for Java.lang.reflect.ParameterizedType.
     */
    public static String getIdForGenericArrayType(GenericArrayType genericArrayType)
    {
        Type compType = genericArrayType.getGenericComponentType();
        String compTypeStr = getIdForType(compType);
        return compTypeStr + "[]";
    }

    /**
     * Returns an Id for Java.lang.reflect.ParameterizedType.
     */
    public static String getIdForParameterizedType(ParameterizedType parameterizedType)
    {
        // Get RawType id
        Type rawType = parameterizedType.getRawType();
        String rawTypeId = getIdForType(rawType);

        // Get typeArgs id (just return rawType id if no args
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        if (typeArgs.length == 0)
            return rawTypeId;

        // Return RawType<TypeArgs>          // Was map(typeArg -> getIdForType(typeArg))
        String typeArgsNameStr = Stream.of(typeArgs).map(Type::getTypeName).collect(Collectors.joining(","));
        return rawTypeId + '<' + typeArgsNameStr + '>';
    }

    /**
     * Returns an id string for given Java part.
     */
    public static String getIdForParameterizedTypeParts(JavaDecl aRawType, JavaType[] theTypes)
    {
        // Get RawType id (just return if no args)
        String rawTypeId = aRawType.getId();
        if (theTypes.length == 0)
            return rawTypeId;

        // Return RawType<TypeArgs>         // Was map(JavaType::getId)
        String typeArgsNameStr = Stream.of(theTypes).map(JavaType::getName).collect(Collectors.joining(","));
        return rawTypeId + '<' + typeArgsNameStr + '>';
    }

    /**
     * Returns an Id for Java.lang.reflect.TypeVariable.
     */
    public static String getIdForTypeVariable(TypeVariable<?> typeVariable)
    {
        // Get GenericDecl and TypeVar.Name
        GenericDeclaration genericDecl = typeVariable.getGenericDeclaration();
        String genericDeclId = genericDecl instanceof Member ?
                getIdForMember((Member) genericDecl) :
                getIdForClass((Class<?>) genericDecl);
        String typeVarName = typeVariable.getName();

        // Return GenericDecl.TypeVarName
        return genericDeclId + '.' + typeVarName;
    }

    /**
     * Returns an Id string for a Type array.
     */
    private static String getIdForTypeArray(Type[] theTypes)
    {
        return Stream.of(theTypes).map(type -> getIdForType(type)).collect(Collectors.joining(","));
    }

    /**
     * Returns an Id for a Java.lang.reflect.Member.
     */
    public static String getIdForMember(Member aMember)
    {
        // Get id for Member.DeclaringClass
        Class<?> declaringClass = aMember.getDeclaringClass();
        String classId = getIdForClass(declaringClass);

        // Start StringBuffer
        StringBuilder sb = new StringBuilder(classId);

        // Handle Field: DeclClassName.<Name>
        if (aMember instanceof Field)
            sb.append('.').append(aMember.getName());

            // Handle Method: DeclClassName.Name(<ParamType>,...)
        else if (aMember instanceof Method) {
            Method meth = (Method) aMember;
            sb.append('.').append(meth.getName()).append('(');
            Class<?>[] paramTypes = meth.getParameterTypes();
            if (paramTypes.length > 0)
                sb.append(getIdForTypeArray(paramTypes));
            sb.append(')');
        }

        // Handle Constructor: DeclClassName(<ParamType>,...)
        else if (aMember instanceof Constructor) {
            Constructor<?> constr = (Constructor<?>) aMember;
            Class<?>[] paramTypes = constr.getParameterTypes();
            sb.append('(');
            if (paramTypes.length > 0)
                sb.append(getIdForTypeArray(paramTypes));
            sb.append(')');
        }

        // Return
        return sb.toString();
    }

    /**
     * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
     */
    public static Class<?> getClassForType(Type aType)
    {
        // Handle Class
        if (aType instanceof Class)
            return (Class<?>) aType;

        // Handle GenericArrayType
        if (aType instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) aType;
            Class<?> cls = getClassForType(gat.getGenericComponentType());
            return Array.newInstance(cls, 0).getClass();
        }

        // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
        if (aType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) aType;
            Type rawType = parameterizedType.getRawType();
            return getClassForType(rawType);
        }

        // Handle TypeVariable
        if (aType instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) aType;
            Type[] boundsTypes = typeVar.getBounds();
            Type bounds0 = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getClassForType(bounds0);
        }

        // Handle WildcardType
        if (aType instanceof WildcardType) {
            WildcardType wc = (WildcardType) aType;
            Type[] boundsTypes = wc.getLowerBounds().length > 0 ? wc.getLowerBounds() : wc.getUpperBounds();
            Type boundsType = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getClassForType(boundsType);
        }

        // Complain about anything else
        throw new RuntimeException("JavaKitUtils.getClass: Can't get class from type: " + aType);
    }
}
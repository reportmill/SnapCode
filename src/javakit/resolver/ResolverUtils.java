/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;

/**
 * Utility methods for JavaParse package.
 */
public class ResolverUtils {

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

        // Return RawType<TypeArgs>
        String typeArgsId = getIdForTypeArray(typeArgs);
        return rawTypeId + '<' + typeArgsId + '>';
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

        // Return RawType<TypeArgs>
        String typeArgsId = getIdForJavaTypes(theTypes);
        return rawTypeId + '<' + typeArgsId + '>';
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
        // If empty, just return empty
        if (theTypes.length == 0) return "";

        // Create StringBuffer
        StringBuffer sb = new StringBuffer();

        // Iterate over types, get id and append for each
        for (int i = 0, iMax = theTypes.length, last = iMax - 1; i < iMax; i++) {
            Type type = theTypes[i];
            String typeStr = getIdForType(type);
            sb.append(typeStr);
            if (i != last)
                sb.append(',');
        }

        // Return
        return sb.toString();
    }

    /**
     * Returns an Id string for a Type array.
     */
    private static String getIdForJavaTypes(JavaType[] theTypes)
    {
        // If empty, just return empty
        if (theTypes.length == 0) return "";

        // Create StringBuffer
        StringBuffer sb = new StringBuffer();

        // Iterate over types, get id and append for each
        for (int i = 0, iMax = theTypes.length, last = iMax - 1; i < iMax; i++) {
            JavaType type = theTypes[i];
            String typeStr = type.getId();
            sb.append(typeStr);
            if (i != last)
                sb.append(',');
        }

        // Return
        return sb.toString();
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
        StringBuffer sb = new StringBuffer(classId);

        // Handle Field: DeclClassName.<Name>
        if (aMember instanceof Field)
            sb.append('.').append(aMember.getName());

            // Handle Method: DeclClassName.Name(<ParamType>,...)
        else if (aMember instanceof Method) {
            Method meth = (Method) aMember;
            sb.append('.').append(meth.getName()).append('(');
            Class<?>[] paramTypes = meth.getParameterTypes();
            if (paramTypes.length > 0) {
                String paramTypesId = getIdForTypeArray(paramTypes);
                sb.append(paramTypesId);
            }
            sb.append(')');
        }

        // Handle Constructor: DeclClassName(<ParamType>,...)
        else if (aMember instanceof Constructor) {
            Constructor<?> constr = (Constructor<?>) aMember;
            Class<?>[] paramTypes = constr.getParameterTypes();
            sb.append('(');
            if (paramTypes.length > 0) {
                String paramTypesId = getIdForTypeArray(paramTypes);
                sb.append(paramTypesId);
            }
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
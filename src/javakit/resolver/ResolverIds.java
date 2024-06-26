package javakit.resolver;
import snap.util.ArrayUtils;
import java.lang.reflect.*;

/**
 * Utility methods to get ids for Resolver objects.
 */
public class ResolverIds {

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
        String typeArgsNameStr = ArrayUtils.mapToStringsAndJoin(typeArgs, Type::getTypeName, ",");
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
        String typeArgsNameStr = ArrayUtils.mapToStringsAndJoin(theTypes, JavaType::getName, ",");
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
     * Returns an Id for a Java.lang.reflect.Member.
     */
    public static String getIdForMember(Member aMember)
    {
        // Get id for Member.DeclaringClass
        Class<?> declaringClass = aMember.getDeclaringClass();
        String classId = getIdForClass(declaringClass);

        // Handle Field: DeclClassName.<Name>
        if (aMember instanceof Field)
            return classId + '.' + aMember.getName();

        // Get executable parameter types string: (type1,type2,...)
        Executable executable = (Executable) aMember;
        Class<?>[] paramClasses = executable.getParameterTypes();
        String paramClassesStr = '(' + ArrayUtils.mapToStringsAndJoin(paramClasses, ResolverIds::getIdForClass, ",") + ')';

        // Handle constructor: DeclClassName(paramsTypesString)
        if (aMember instanceof Constructor)
            return classId + paramClassesStr;

        // Handle method: DeclClassName.MethodName(paramTypesString)
        return classId + '.' + aMember.getName() + paramClassesStr;
    }
}

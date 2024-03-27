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
     * Returns a class for a given name, using the class loader of the given class.
     */
    public static Class<?> getClassForName(String aName, ClassLoader aClassLoader)
    {
        // Handle arrays, either coded or uncoded (e.g. [I, [D, [LClassName; or  int[], double[] or ClassName[])
        if (aName.startsWith("["))
            return getClassForClassCoding(aName, aClassLoader);
        if (aName.endsWith("[]")) {
            String cname = aName.substring(0, aName.length() - 2);
            Class<?> cls = getClassForName(cname, aClassLoader);
            return cls != null ? Array.newInstance(cls, 0).getClass() : null;
        }

        // Handle primitive classes
        Class<?> primitiveClass = getPrimitiveClassForName(aName);
        if (primitiveClass != null)
            return primitiveClass;

        // Do normal Class.forName
        try { return Class.forName(aName, false, aClassLoader); }

        // Handle Exceptions
        catch(ClassNotFoundException e) { return null; }
        catch(NoClassDefFoundError t) { System.err.println("ResolverUtils.getClassForName: " + t); return null; }
        catch(Throwable t) { System.err.println("ResolverUtils.getClassForName: Unknown error: " + t); return null; }
    }

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
     * Returns a class for given class coding.
     */
    public static Class<?> getClassForClassCoding(String aName, ClassLoader aClassLoader)
    {
        char c = aName.charAt(0);
        switch (c) {
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'D': return double.class;
            case 'F': return float.class;
            case 'I': return int.class;
            case 'J': return long.class;
            case 'S': return short.class;
            case 'Z': return boolean.class;
            case 'V': return void.class;
            case 'L':
                int end = aName.indexOf(';', 1);
                return getClassForName(aName.substring(1, end), aClassLoader);
            case '[':
                Class<?> cls = getClassForClassCoding(aName.substring(1), aClassLoader);
                return cls != null ? Array.newInstance(cls, 0).getClass() : null;
        }

        // Unsupported coding char
        throw new RuntimeException("ResolverUtils.getClassForClassCoding: Not a coded class " + aName);
    }

    /**
     * Returns whether name is a primitive class name.
     */
    public static boolean isPrimitiveClassName(String aName)
    {
        return getPrimitiveClassForName(aName) != null;
    }

    /**
     * Returns a primitive class for name.
     */
    public static Class<?> getPrimitiveClassForName(String aName)
    {
        if (aName.length() > 7 || !Character.isLowerCase(aName.charAt(0)) || aName.indexOf('.') > 0)
            return null;

        switch (aName) {
            case "boolean": return boolean.class;
            case "char": return char.class;
            case "byte": return byte.class;
            case "short": return short.class;
            case "int": return int.class;
            case "long": return long.class;
            case "float": return float.class;
            case "double": return double.class;
            case "void": return void.class;
            default: return null;
        }
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
        throw new RuntimeException("ResolverUtils.getClassForType: Can't get class from type: " + aType);
    }
}
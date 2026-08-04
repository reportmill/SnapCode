/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.view.ViewUtils;

import java.lang.reflect.*;
import java.util.List;
import java.util.stream.Stream;

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
        return switch (aName.charAt(0)) {
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'D' -> "double";
            case 'F' -> "float";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'V' -> "void";
            case 'L' -> { int end = aName.indexOf(';', 1); yield aName.substring(1, end); }
            case '[' -> getClassNameForClassCoding(aName.substring(1)) + "[]";
            default -> throw new RuntimeException("ResolverUtils.getClassNameForClassCoding: Not a coded class string " + aName);
        };
    }

    /**
     * Returns a class for given class coding.
     */
    public static Class<?> getClassForClassCoding(String aName, ClassLoader aClassLoader)
    {
        return switch (aName.charAt(0)) {
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'D' -> double.class;
            case 'F' -> float.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'S' -> short.class;
            case 'Z' -> boolean.class;
            case 'V' -> void.class;
            case 'L' -> {
                int end = aName.indexOf(';', 1);
                yield getClassForName(aName.substring(1, end), aClassLoader);
            }
            case '[' -> {
                Class<?> cls = getClassForClassCoding(aName.substring(1), aClassLoader);
                yield cls != null ? Array.newInstance(cls, 0).getClass() : null;
            }
            default -> throw new RuntimeException("ResolverUtils.getClassForClassCoding: Not a coded class " + aName);
        };
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

        return switch (aName) {
            case "boolean" -> boolean.class;
            case "char" -> char.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> null;
        };
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
        if (aType instanceof GenericArrayType genericArrayType) {
            Class<?> cls = getClassForType(genericArrayType.getGenericComponentType());
            return Array.newInstance(cls, 0).getClass();
        }

        // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
        if (aType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            return getClassForType(rawType);
        }

        // Handle TypeVariable
        if (aType instanceof TypeVariable<?> typeVar) {
            Type[] boundsTypes = typeVar.getBounds();
            Type bounds0 = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getClassForType(bounds0);
        }

        // Handle WildcardType
        if (aType instanceof WildcardType wildcardType) {
            Type[] boundsTypes = wildcardType.getLowerBounds().length > 0 ? wildcardType.getLowerBounds() : wildcardType.getUpperBounds();
            Type boundsType = boundsTypes.length > 0 ? boundsTypes[0] : Object.class;
            return getClassForType(boundsType);
        }

        // Complain about anything else
        throw new RuntimeException("ResolverUtils.getClassForType: Can't get class from type: " + aType);
    }

    /**
     * Primes the resolver.
     */
    public static void primeResolver(Resolver aResolver)
    {
        if (pkgIndex < pkgNames.size())
            primeResolver(aResolver, pkgNames.get(pkgIndex++));
        if (pkgIndex < pkgNames.size())
            ViewUtils.runDelayed(() -> primeResolver(aResolver), 300);
    }

    // Common packages to preload
    private static List<String> pkgNames = List.of("java", "java.lang", "java.util", "java.io", "java.awt", "javax", "javax.swing");
    private static int pkgIndex;

    /**
     * Primes the resolver.
     */
    private static void primeResolver(Resolver aResolver, String packageName)
    {
        JavaPackage pkg = aResolver.getKnownJavaPackageForName(packageName);
        pkg.getClasses();
        JavaPackage[] childPackages = pkg.getPackages();
        if (!packageName.equals("java") && !packageName.equals("javax"))
            Stream.of(childPackages).forEach(childPackage -> primeResolver(aResolver, childPackage.getName()));
    }
}
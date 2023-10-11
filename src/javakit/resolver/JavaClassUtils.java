/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ListUtils;
import java.util.*;

/**
 * This class provides utility methods for JavaClass.
 */
public class JavaClassUtils {

    /**
     * Returns a compatible constructor for given param types.
     */
    public static JavaConstructor getCompatibleConstructor(JavaClass aClass, JavaClass[] paramTypes)
    {
        List<JavaConstructor> declaredConstructors = aClass.getDeclaredConstructors();
        JavaConstructor compatibleConstructor = null;
        int rating = 0;

        // Iterate over constructors to find highest rating
        for (JavaConstructor constr : declaredConstructors) {
            int rtg = JavaExecutable.getMatchRatingForArgClasses(constr, paramTypes);
            if (rtg > rating) {
                compatibleConstructor = constr;
                rating = rtg;
            }
        }

        // Return
        return compatibleConstructor;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    private static JavaMethod getCompatibleMethod(JavaClass aClass, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        List<JavaMethod> declaredMethods = aClass.getDeclaredMethods();
        JavaMethod compatibleMethod = null;
        int rating = 0;

        // Iterate over methods to find highest rating
        for (JavaMethod method : declaredMethods) {
            if (staticOnly && !method.isStatic())
                continue;
            if (method.getName().equals(aName)) {
                if (paramTypes == null)
                    return method;
                int rtg = JavaExecutable.getMatchRatingForArgClasses(method, paramTypes);
                if (rtg > rating) {
                    compatibleMethod = method;
                    rating = rtg;
                }
            }
        }

        // Return
        return compatibleMethod;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static JavaMethod getCompatibleMethodAll(JavaClass aClass, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        // Search this class and superclasses for compatible method
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaMethod compatibleMethod = getCompatibleMethod(cls, aName, paramTypes, staticOnly);
            if (compatibleMethod != null)
                return compatibleMethod;
        }

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass infc : interfaces) {
                JavaMethod compatibleMethod = getCompatibleMethodAll(infc, aName, paramTypes, staticOnly);
                if (compatibleMethod != null)
                    return compatibleMethod;
            }
        }

        // If this class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objClass = aClass.getJavaClassForClass(Object.class);
            return getCompatibleMethod(objClass, aName, paramTypes, staticOnly);
        }

        // Return not found
        return null;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethodsAll(JavaClass aClass, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        // Find compatible methods
        List<JavaMethod> compatibleMethods = new ArrayList<>();
        findCompatibleMethodsAll(aClass, aName, paramTypes, compatibleMethods, staticOnly);

        // If given class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objDecl = aClass.getJavaClassForClass(Object.class);
            findCompatibleMethods(objDecl, aName, paramTypes, compatibleMethods, staticOnly);
        }

        // Remove supers
        for (int i = 0; i < compatibleMethods.size(); i++) {
            JavaMethod method = compatibleMethods.get(i);
            for (JavaMethod superMethod = method.getSuper(); superMethod != null; superMethod = superMethod.getSuper())
                compatibleMethods.remove(superMethod);
        }

        // Return
        return compatibleMethods;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    private static void findCompatibleMethodsAll(JavaClass aClass, String aName, JavaClass[] paramTypes, List<JavaMethod> compatibleMethods, boolean staticOnly)
    {
        // Search this class and superclasses for compatible methods
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            findCompatibleMethods(cls, aName, paramTypes, compatibleMethods, staticOnly);
        }

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass infc : interfaces) {
                findCompatibleMethodsAll(infc, aName, paramTypes, compatibleMethods, staticOnly);
            }
        }
    }

    /**
     * Find the compatible methods for given class, name and param types.
     */
    private static void findCompatibleMethods(JavaClass aClass, String aName, JavaClass[] paramTypes, List<JavaMethod> compatibleMethods, boolean staticOnly)
    {
        // Iterate over declared methods to find compatible methods (matching name and args)
        List<JavaMethod> declaredMethods = aClass.getDeclaredMethods();
        for (JavaMethod method : declaredMethods) {
            if (staticOnly && !method.isStatic())
                continue;
            if (method.getName().equals(aName)) {
                int rating = paramTypes != null ? JavaExecutable.getMatchRatingForArgClasses(method, paramTypes) : 1;
                if (rating > 0)
                    ListUtils.addUniqueId(compatibleMethods, method);
            }
        }
    }
}

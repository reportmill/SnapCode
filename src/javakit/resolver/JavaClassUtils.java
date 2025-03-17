/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
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
        JavaConstructor[] declaredConstructors = aClass.getDeclaredConstructors();
        return ArrayUtils.getMax(declaredConstructors, (c1,c2) -> compareMethodMatchRatings(c1, c2, paramTypes));
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static JavaMethod getCompatibleMethod(JavaClass aClass, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        List<JavaMethod> compatibleMethods = getCompatibleMethods(aClass, aName, paramTypes, staticOnly);
        return compatibleMethods.isEmpty() ? null : compatibleMethods.get(0);
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethods(JavaClass aClass, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        // Find compatible methods
        List<JavaMethod> compatibleMethods = new ArrayList<>(2);
        findCompatibleMethods(aClass, aName, paramTypes, compatibleMethods, staticOnly);

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

        // Sort the methods
        if (compatibleMethods.size() > 1 && paramTypes != null)
            compatibleMethods.sort((m1,m2) -> compareMethodMatchRatings(m1, m2, paramTypes));

        // Return
        return compatibleMethods;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    private static void findCompatibleMethods(JavaClass aClass, String aName, JavaClass[] paramTypes, List<JavaMethod> compatibleMethods, boolean staticOnly)
    {
        // Search class declared methods
        JavaMethod[] declaredMethods = aClass.getDeclaredMethods();
        for (JavaMethod method : declaredMethods) {
            if (isCompatibleMethod(method, aName, paramTypes, staticOnly))
                ListUtils.addUniqueId(compatibleMethods, method);
        }

        // Search superclass
        JavaClass superClass = aClass.getSuperClass();
        if (superClass != null)
            findCompatibleMethods(superClass, aName, paramTypes, compatibleMethods, staticOnly);

        // Search interfaces
        JavaClass[] interfaces = aClass.getInterfaces();
        for (JavaClass infc : interfaces)
            findCompatibleMethods(infc, aName, paramTypes, compatibleMethods, staticOnly);
    }

    /**
     * Returns whether method is compatible method.
     */
    private static boolean isCompatibleMethod(JavaMethod method, String aName, JavaClass[] paramTypes, boolean staticOnly)
    {
        if (staticOnly && !method.isStatic())
            return false;
        if (!method.getName().equals(aName))
            return false;
        int rating = paramTypes != null ? JavaExecutable.getMatchRatingForArgClasses(method, paramTypes) : 1;
        if (rating <= 0)
            return false;
        return true;
    }

    /**
     * Returns the result of comparing two methods by match rating for given parameter types.
     */
    private static int compareMethodMatchRatings(JavaExecutable method1, JavaExecutable method2, JavaClass[] paramTypes)
    {
        int val1 = JavaExecutable.getMatchRatingForArgClasses(method1, paramTypes);
        int val2 = JavaExecutable.getMatchRatingForArgClasses(method2, paramTypes);
        return Integer.compare(val1, val2);
    }
}

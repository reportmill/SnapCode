/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.util.*;

/**
 * This class provides utility methods for JavaClass.
 */
public class JavaClassUtils {

    /**
     * Returns a compatible constructor for given name and param types.
     */
    public static JavaConstructor getCompatibleConstructor(JavaClass aClass, JavaType[] theTypes)
    {
        List<JavaConstructor> declaredConstructors = aClass.getDeclaredConstructors();
        JavaConstructor compatibleConstructor = null;
        int rating = 0;

        // Iterate over constructors to find highest rating
        for (JavaConstructor constr : declaredConstructors) {
            int rtg = JavaExecutable.getMatchRatingForTypes(constr, theTypes);
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
    public static JavaMethod getCompatibleMethod(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        List<JavaMethod> declaredMethods = aClass.getDeclaredMethods();
        JavaMethod compatibleMethod = null;
        int rating = 0;

        // Iterate over methods to find highest rating
        for (JavaMethod method : declaredMethods) {
            if (method.getName().equals(aName)) {
                int rtg = JavaExecutable.getMatchRatingForTypes(method, theTypes);
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
    public static JavaMethod getCompatibleMethodAll(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        // Search this class and superclasses for compatible method
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaMethod compatibleMethod = getCompatibleMethod(cls, aName, theTypes);
            if (compatibleMethod != null)
                return compatibleMethod;
        }

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass infc : interfaces) {
                JavaMethod compatibleMethod = getCompatibleMethodAll(infc, aName, theTypes);
                if (compatibleMethod != null)
                    return compatibleMethod;
            }
        }

        // If this class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objClass = aClass.getJavaClassForClass(Object.class);
            return getCompatibleMethod(objClass, aName, theTypes);
        }

        // Return not found
        return null;
    }

    /**
     * Returns a compatible methods for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethods(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        List<JavaMethod> compatibleMethods = Collections.EMPTY_LIST;
        List<JavaMethod> declaredMethods = aClass.getDeclaredMethods();

        // Iterate over methods to find highest rating
        for (JavaMethod method : declaredMethods) {
            if (method.getName().equals(aName)) {
                int rating = JavaExecutable.getMatchRatingForTypes(method, theTypes);
                if (rating > 0) {
                    if (compatibleMethods == Collections.EMPTY_LIST)
                        compatibleMethods = new ArrayList<>();
                    compatibleMethods.add(method);
                }
            }
        }

        // Return
        return compatibleMethods;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public static List<JavaMethod> getCompatibleMethodsAll(JavaClass aClass, String aName, JavaType[] theTypes)
    {
        List<JavaMethod> compatibleMethods = Collections.EMPTY_LIST;

        // Search this class and superclasses for compatible methods
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            List<JavaMethod> compMethods = getCompatibleMethods(cls, aName, theTypes);
            if (compMethods.size() > 0) {
                if (compatibleMethods == Collections.EMPTY_LIST)
                    compatibleMethods = compMethods;
                else compatibleMethods.addAll(compMethods);
            }
        }

        // Search this class and superclasses for compatible interface
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass infc : interfaces) {
                List<JavaMethod> methods = getCompatibleMethodsAll(infc, aName, theTypes);
                if (methods.size() > 0) {
                    if (compatibleMethods == Collections.EMPTY_LIST) compatibleMethods = methods;
                    else compatibleMethods.addAll(methods);
                }
            }
        }

        // If this class is Interface, check Object
        if (aClass.isInterface()) {
            JavaClass objDecl = aClass.getJavaClassForClass(Object.class);
            List<JavaMethod> methods = getCompatibleMethods(objDecl, aName, theTypes);
            if (methods.size() > 0) {
                if (compatibleMethods == Collections.EMPTY_LIST) compatibleMethods = methods;
                else compatibleMethods.addAll(methods);
            }
        }

        // Remove supers and duplicates
        for (int i = 0; i < compatibleMethods.size(); i++) {
            JavaMethod method = compatibleMethods.get(i);
            for (JavaMethod superMethod = method.getSuper(); superMethod != null; superMethod = superMethod.getSuper())
                compatibleMethods.remove(superMethod);
            for (int j = i + 1; j < compatibleMethods.size(); j++)
                if (compatibleMethods.get(j) == method)
                    compatibleMethods.remove(j);
        }

        // Return
        return compatibleMethods;
    }
}

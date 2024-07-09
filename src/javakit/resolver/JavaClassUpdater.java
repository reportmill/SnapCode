/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class updates a JavaClass from resolver.
 */
public class JavaClassUpdater {

    // The JavaClass
    protected JavaClass _javaClass;

    // The Resolver that produced this decl
    protected Resolver _resolver;

    // The real class (if available)
    private Class<?> _realClass;

    /**
     * Constructor.
     */
    public JavaClassUpdater(JavaClass aClass)
    {
        _javaClass = aClass;
        _resolver = aClass._resolver;
    }

    /**
     * Reloads the class using real class from resolver classloader. Returns whether the class changed during reload.
     */
    public boolean reloadClass() throws SecurityException
    {
        // Get current values
        JavaClass[] oldInterfaces = _javaClass.getInterfaces();
        JavaTypeVariable[] oldTypeVars = _javaClass.getTypeVars();
        JavaClass[] oldInnerClasses = _javaClass.getDeclaredClasses();
        JavaField[] oldFields = _javaClass.getDeclaredFields();
        JavaMethod[] oldMethods = _javaClass.getDeclaredMethods();
        JavaConstructor[] oldConstrs = _javaClass.getDeclaredConstructors();

        // Get real class
        _realClass = getRealClassImpl();
        if (_realClass == null) {
            System.err.println("JavaClass: Failed to load class: " + _javaClass.getClassName());
            return false;
        }

        // Declare return value for whether class changed
        boolean classChanged = false;

        // Update modifiers
        int oldMods = _javaClass._mods;
        int newMods = _javaClass._mods = getModifiers();
        if (oldMods != newMods)
            classChanged = true;

        // Update superclass
        String superClassName = getSuperClassName();
        if (!Objects.equals(superClassName, _javaClass._superClassName)) {
            _javaClass._superClassName = superClassName;
            _javaClass._superClass = null;
            classChanged = true;
        }

        // Update interfaces
        JavaClass[] newInterfaces = _javaClass._interfaces = getInterfaces();
        if (!Arrays.equals(oldInterfaces, newInterfaces))
            classChanged = true;

        // Update type variables
        JavaTypeVariable[] newTypeVars = _javaClass._typeVars = getTypeVariables();
        if (!Arrays.equals(oldTypeVars, newTypeVars))
            classChanged = true;

        // Update inner classes
        JavaClass[] newInnerClasses = _javaClass._innerClasses = getDeclaredClasses();
        if (!Arrays.equals(oldInnerClasses, newInnerClasses))
            classChanged = true;

        // Update fields
        JavaField[] newFields = _javaClass._fields = getMergedFields(oldFields);
        if (newFields != oldFields)
            classChanged = true;

        // Update methods
        JavaMethod[] newMethods = _javaClass._methods = getMergedMethods(oldMethods);
        if (newMethods != oldMethods)
            classChanged = true;

        // Update constructors
        JavaConstructor[] newConstrs = _javaClass._constructors = getMergedConstructors(oldConstrs);
        if (newConstrs != oldConstrs)
            classChanged = true;

        // Return
        return classChanged;
    }

    /**
     * Returns the real class.
     */
    protected Class<?> getRealClassImpl()
    {
        String className = _javaClass.getClassName();
        Class<?> realClass = _resolver.getClassForName(className);
        if (realClass == null) {
            //System.err.println("JavaClassUpdater.getRealClassImpl: Can't find real class: " + className);
            JavaClass superClass = _javaClass.getSuperClass();
            realClass = superClass != null ? superClass.getRealClass() : Object.class;
        }
        return realClass;
    }

    /**
     * Returns the modifiers.
     */
    protected int getModifiers()  { return _realClass.getModifiers(); }

    /**
     * Returns the super class name.
     */
    protected String getSuperClassName()
    {
        Class<?> superClass = _realClass.getSuperclass();
        return superClass != null ? superClass.getName() : null;
    }

    /**
     * Returns the generic superclass.
     */
    protected JavaType getGenericSuperclass()
    {
        Class<?> realClass = getRealClassImpl();
        Type genericSuperClass = realClass.getGenericSuperclass();
        return genericSuperClass != null ? _resolver.getJavaTypeForType(genericSuperClass) : null;
    }

    /**
     * Returns interfaces.
     */
    protected JavaClass[] getInterfaces()
    {
        Class<?> realClass = getRealClassImpl();
        Class<?>[] interfaces = realClass.getInterfaces();
        return ArrayUtils.map(interfaces, cls -> _javaClass.getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns interfaces.
     */
    protected JavaType[] getGenericInterfaces()
    {
        Class<?> realClass = getRealClassImpl();
        Type[] interfaces = realClass.getGenericInterfaces();
        return ArrayUtils.map(interfaces, type -> _resolver.getJavaTypeForType(type), JavaType.class);
    }

    /**
     * Returns JavaTypeVariable array for given class TypeVariables.
     */
    protected JavaTypeVariable[] getTypeVariables()
    {
        Class<?> realClass = getRealClassImpl();
        TypeVariable<?>[] typeVariables = realClass.getTypeParameters();
        return ArrayUtils.map(typeVariables, tvar -> new JavaTypeVariable(_resolver, _javaClass, tvar), JavaTypeVariable.class);
    }

    /**
     * Returns JavaClass array of declared inner classes for given class.
     */
    protected JavaClass[] getDeclaredClasses()
    {
        // Get Inner Classes
        Class<?> realClass = getRealClassImpl();
        Class<?>[] innerClasses;
        try { innerClasses = realClass.getDeclaredClasses(); }
        catch (Throwable e) {
            System.err.println("JavaClassUpdater.getDeclaredClasses: Can't get declared classes: " + e);
            return new JavaClass[0];
        }

        // Add JavaDecl for each inner class
        return ArrayUtils.map(innerClasses, cls -> _resolver.getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns JavaField array for given class.
     */
    protected JavaField[] getDeclaredFields()
    {
        Class<?> realClass = getRealClassImpl();
        Field[] fields;
        try { fields = realClass.getDeclaredFields(); }
        catch (Throwable e) { e.printStackTrace(); return new JavaField[0]; }
        return ArrayUtils.map(fields, field -> new JavaField(_resolver, _javaClass, field), JavaField.class);
    }

    /**
     * Returns JavaMethod array for given class.
     */
    protected JavaMethod[] getDeclaredMethods()
    {
        Class<?> realClass = getRealClassImpl();
        Method[] methods;
        try { methods = realClass.getDeclaredMethods(); }
        catch (Throwable e) { e.printStackTrace(); return new JavaMethod[0]; }
        return ArrayUtils.mapNonNull(methods, meth -> getJavaMethodForMethod(meth), JavaMethod.class);
    }

    /**
     * Returns a JavaMethod for given Method.
     */
    private JavaMethod getJavaMethodForMethod(Method aMethod)
    {
        if (aMethod.isSynthetic())
            return null;
        return new JavaMethod(_resolver, _javaClass, aMethod);
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        Class<?> realClass = getRealClassImpl();
        Constructor<?>[] constrs;
        try { constrs = realClass.getDeclaredConstructors(); }
        catch (Throwable e) { e.printStackTrace(); return new JavaConstructor[0]; }
        return ArrayUtils.mapNonNull(constrs, constr -> getJavaConstructorForConstructor(constr), JavaConstructor.class);
    }

    /**
     * Returns a JavaConstructor for given Constructor.
     */
    private JavaConstructor getJavaConstructorForConstructor(Constructor<?> aConstr)
    {
        if (aConstr.isSynthetic())
            return null;
        return new JavaConstructor(_resolver, _javaClass, aConstr);
    }

    /**
     * Returns the enum constants.
     */
    public Object[] getEnumConstants()
    {
        Class<?> realClass = getRealClassImpl();
        return realClass.getEnumConstants();
    }

    /**
     * Merges an array of old fields and new fields.
     */
    protected JavaField[] getMergedFields(JavaField[] oldFields)
    {
        JavaField[] newFields = getDeclaredFields();
        boolean didChange = false;

        // Iterate over new fields and merge with old
        for (int i = 0; i < newFields.length; i++) {
            JavaField newField = newFields[i];
            JavaField oldField = ArrayUtils.findMatch(oldFields, field -> field.equals(newField));
            if (oldField != null) {
                if (oldField.mergeField(newField))
                    didChange = true;
                newFields[i] = oldField;
            }
            else didChange = true;
        }

        // Return
        return didChange ? newFields : oldFields;
    }

    /**
     * Merges an array of old methods with new methods.
     */
    protected JavaMethod[] getMergedMethods(JavaMethod[] oldMethods)
    {
        JavaMethod[] newMethods = getDeclaredMethods();
        boolean didChange = false;

        // Iterate over new fields and merge with old
        for (int i = 0; i < newMethods.length; i++) {
            JavaMethod newMethod = newMethods[i];
            JavaMethod oldMethod = ArrayUtils.findMatch(oldMethods, method -> method.equals(newMethod));
            if (oldMethod != null) {
                if (mergeMethod(oldMethod, newMethod))
                    didChange = true;
                newMethods[i] = oldMethod;
            }
            else didChange = true;
        }

        // Return
        return didChange ? newMethods : oldMethods;
    }

    /**
     * Merges an array of old constructors with new constructors.
     */
    protected JavaConstructor[] getMergedConstructors(JavaConstructor[] oldConstructors)
    {
        JavaConstructor[] newConstructors = getDeclaredConstructors();
        boolean didChange = false;

        // Iterate over new constructors and merge with old
        for (int i = 0; i < newConstructors.length; i++) {
            JavaConstructor newConstr = newConstructors[i];
            JavaConstructor oldConstr = ArrayUtils.findMatch(oldConstructors, constr -> constr.equals(newConstr));
            if (oldConstr != null) {
                if (mergeConstructor(oldConstr, newConstr))
                    didChange = true;
                newConstructors[i] = oldConstr;
            }
            else didChange = true;
        }

        // Return
        return didChange ? newConstructors : oldConstructors;
    }

    /**
     * Merges the given new method into this method.
     */
    private static boolean mergeMethod(JavaMethod oldMethod, JavaMethod newMethod)
    {
        // Update modifiers
        boolean didChange = mergeExecutable(oldMethod, newMethod);

        // Update return type
        String oldMethodReturnTypeName = oldMethod.getGenericReturnType().getName();
        String newMethodReturnTypeName = newMethod.getGenericReturnType().getName();
        if (!oldMethodReturnTypeName.equals(newMethodReturnTypeName)) {
            oldMethod._genericReturnType = newMethod.getGenericReturnType();
            didChange = true;
        }

        // Update Method
        if (newMethod._method != null)
            oldMethod._method = newMethod._method;

        // Return
        return didChange;
    }

    /**
     * Merges the given new constructor into this constructor.
     */
    private static boolean mergeConstructor(JavaConstructor oldConstr, JavaConstructor newConstr)
    {
        // Update modifiers
        boolean didChange = mergeExecutable(oldConstr, newConstr);

        // Update Constructor
        if (newConstr._constructor != null)
            oldConstr._constructor = newConstr._constructor;

        // Return
        return didChange;
    }

    /**
     * Merges the given new constructor into this constructor.
     */
    private static boolean mergeExecutable(JavaExecutable oldExec, JavaExecutable newExec)
    {
        // Update modifiers
        boolean didChange = false;
        if (newExec._mods != oldExec._mods) {
            oldExec._mods = newExec._mods;
            didChange = true;
        }

        // Update generic parameter types (these can change as long as base class is same)

        // Return
        return didChange;
    }
}

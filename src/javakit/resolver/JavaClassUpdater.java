/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class updates a JavaClass from resolver.
 */
public class JavaClassUpdater {

    // The JavaClass
    protected JavaClass  _javaClass;

    // The real class (if available)
    protected Class<?> _realClass;

    // The Resolver that produced this decl
    protected Resolver  _resolver;

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
        _realClass = _javaClass._realClass = getRealClassImpl();
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
        if (!ArrayUtils.equalsId(oldInterfaces, newInterfaces))
            classChanged = true;

        // Update type variables
        JavaTypeVariable[] newTypeVars = _javaClass._typeVars = getTypeVariables();
        if (!ArrayUtils.equalsId(oldTypeVars, newTypeVars))
            classChanged = true;

        // Update inner classes
        JavaClass[] newInnerClasses = _javaClass._innerClasses = getDeclaredClasses();
        if (!ArrayUtils.equalsId(oldInnerClasses, newInnerClasses))
            classChanged = true;

        // Update fields
        JavaField[] newFields = _javaClass._fields = getDeclaredFields();
        if (!ArrayUtils.equalsId(oldFields, newFields))
            classChanged = true;

        // Update methods
        JavaMethod[] newMethods = _javaClass._methods = getDeclaredMethods();
        if (!ArrayUtils.equalsId(oldMethods, newMethods))
            classChanged = true;

        // Update constructors
        JavaConstructor[] newConstrs = _javaClass._constructors = getDeclaredConstructors();
        if (!ArrayUtils.equalsId(oldConstrs, newConstrs))
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
        return _resolver.getClassForName(className);
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
     * Returns interfaces.
     */
    protected JavaClass[] getInterfaces()
    {
        Class<?>[] interfaces = _realClass.getInterfaces();
        return ArrayUtils.map(interfaces, cls -> _javaClass.getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns JavaTypeVariable array for given class TypeVariables.
     */
    protected JavaTypeVariable[] getTypeVariables()
    {
        TypeVariable<?>[] typeVariables = _realClass.getTypeParameters();
        return ArrayUtils.map(typeVariables, tvar -> new JavaTypeVariable(_resolver, _javaClass, tvar), JavaTypeVariable.class);
    }

    /**
     * Returns JavaClass array of declared inner classes for given class.
     */
    protected JavaClass[] getDeclaredClasses()
    {
        // Get Inner Classes
        Class<?>[] innerClasses;
        try { innerClasses = _realClass.getDeclaredClasses(); }
        catch (Throwable e) {
            System.err.println("JavaClassUpdater.getDeclaredClasses: Can't get declared classes: " + e);
            innerClasses = new Class[0];
        }

        // Add JavaDecl for each inner class
        return ArrayUtils.map(innerClasses, cls -> _resolver.getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns JavaField array for given class.
     */
    protected JavaField[] getDeclaredFields()
    {
        Field[] fields;
        try { fields = _realClass.getDeclaredFields(); }
        catch (Throwable e) { return new JavaField[0]; }
        return ArrayUtils.map(fields, field -> new JavaField(_resolver, _javaClass, field), JavaField.class);
    }

    /**
     * Returns JavaMethod array for given class.
     */
    protected JavaMethod[] getDeclaredMethods()
    {
        Method[] methods;
        try { methods = _realClass.getDeclaredMethods(); }
        catch (Throwable e) { return new JavaMethod[0]; }
        return Stream.of(methods).filter(m -> !m.isSynthetic()).map(m -> new JavaMethod(_resolver, _javaClass, m)).toArray(size -> new JavaMethod[size]);
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        Constructor<?>[] constrs;
        try { constrs = _realClass.getDeclaredConstructors(); }
        catch (Throwable e) { return new JavaConstructor[0]; }
        return Stream.of(constrs).filter(c -> !c.isSynthetic()).map(c -> new JavaConstructor(_resolver, _javaClass, c)).toArray(size -> new JavaConstructor[size]);
    }

    /**
     * Returns the enum constants.
     */
    public Object[] getEnumConstants()
    {
        return _realClass.getEnumConstants();
    }
}

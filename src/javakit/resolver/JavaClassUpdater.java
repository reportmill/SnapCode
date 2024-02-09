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
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    public boolean updateDeclsImpl() throws SecurityException
    {
        // If first time, set decls
        if (_javaClass._fields == null)
            _javaClass._fields = new JavaField[0];

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
        JavaClass[] oldInterfaces = _javaClass._interfaces;
        JavaClass[] newInterfaces = _javaClass._interfaces = getInterfaces();
        if (!ArrayUtils.equalsId(oldInterfaces, newInterfaces))
            classChanged = true;

        // Update type variables
        JavaTypeVariable[] oldTypeVars = _javaClass._typeVars;
        JavaTypeVariable[] newTypeVars = _javaClass._typeVars = getTypeVariables();
        if (!ArrayUtils.equalsId(oldTypeVars, newTypeVars))
            classChanged = true;

        // Update inner classes
        JavaClass[] oldInnerClasses = _javaClass._innerClasses;
        JavaClass[] newInnerClasses = _javaClass._innerClasses = getDeclaredClasses();
        if (!ArrayUtils.equalsId(oldInnerClasses, newInnerClasses))
            classChanged = true;

        // Update fields
        JavaField[] oldFields = _javaClass._fields;
        JavaField[] newFields = _javaClass._fields = getDeclaredFields();
        if (!ArrayUtils.equalsId(oldFields, newFields))
            classChanged = true;

        // Update methods
        JavaMethod[] oldMethods = _javaClass._methods;
        JavaMethod[] newMethods = _javaClass._methods = getDeclaredMethods();
        if (!ArrayUtils.equalsId(oldMethods, newMethods))
            classChanged = true;

        // Update constructors
        JavaConstructor[] oldConstrs = _javaClass._constructors;
        JavaConstructor[] newConstrs = _javaClass._constructors = getDeclaredConstructors();
        if (!ArrayUtils.equalsId(oldConstrs, newConstrs))
            classChanged = true;

        // Return
        return classChanged;
    }

    /**
     * Returns the real class.
     */
    protected Class<?> getRealClassImpl()  { return _javaClass.getRealClass(); }

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
        return ArrayUtils.map(typeVariables, tvar -> getJavaTypeVarForTypeVar(tvar), JavaTypeVariable.class);
    }

    /**
     * Returns a JavaTypeVariable for given TypeVariable from JavaClass, creating if missing.
     */
    private JavaTypeVariable getJavaTypeVarForTypeVar(TypeVariable<?> aTypeVar)
    {
        String typeVarName = aTypeVar.getName();
        JavaTypeVariable javaTypeVar = _javaClass.getTypeVarForName(typeVarName);
        if (javaTypeVar == null)
            javaTypeVar = new JavaTypeVariable(_resolver, _javaClass, aTypeVar);
        return javaTypeVar;
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
        return ArrayUtils.map(innerClasses, cls -> getJavaClassForClass(cls), JavaClass.class);
    }

    /**
     * Returns a JavaClass for given inner Class from JavaClass, creating if missing.
     */
    private JavaClass getJavaClassForClass(Class<?> anInnerClass)
    {
        String innerClassName = anInnerClass.getSimpleName();
        JavaClass innerClass = _javaClass.getDeclaredClassForName(innerClassName);
        if (innerClass == null)
            innerClass = _resolver.getJavaClassForClass(anInnerClass);
        return innerClass;
    }

    /**
     * Returns JavaField array for given class.
     */
    protected JavaField[] getDeclaredFields()
    {
        Field[] fields;
        try { fields = _realClass.getDeclaredFields(); }
        catch (Throwable e) { return new JavaField[0]; }
        return ArrayUtils.map(fields, field -> getJavaFieldForField(field), JavaField.class);
    }

    /**
     * Returns a JavaField for given Field from JavaClass, creating if missing.
     */
    private JavaField getJavaFieldForField(Field aField)
    {
        JavaField javaField = _javaClass.getJavaFieldForField(aField);
        if (javaField == null)
            javaField = new JavaField(_resolver, _javaClass, aField);
        return javaField;
    }

    /**
     * Returns JavaMethod array for given class.
     */
    protected JavaMethod[] getDeclaredMethods()
    {
        Method[] methods;
        try { methods = _realClass.getDeclaredMethods(); }
        catch (Throwable e) { return new JavaMethod[0]; }
        return Stream.of(methods).filter(m -> !m.isSynthetic()).map(m -> getJavaMethodForMethod(m)).toArray(size -> new JavaMethod[size]);
    }

    /**
     * Returns a JavaMethod for given Method from JavaClass, creating if missing.
     */
    private JavaMethod getJavaMethodForMethod(Method aMethod)
    {
        JavaMethod javaMethod = _javaClass.getJavaMethodForMethod(aMethod);
        if (javaMethod == null)
            javaMethod = new JavaMethod(_resolver, _javaClass, aMethod);
        return javaMethod;
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        Constructor<?>[] constrs;
        try { constrs = _realClass.getDeclaredConstructors(); }
        catch (Throwable e) { return new JavaConstructor[0]; }
        return Stream.of(constrs).filter(c -> !c.isSynthetic()).map(c -> getJavaConstructorForConstructor(c)).toArray(size -> new JavaConstructor[size]);
    }

    /**
     * Returns a JavaConstructor for given Method from JavaClass, creating if missing.
     */
    private JavaConstructor getJavaConstructorForConstructor(Constructor<?> aConstr)
    {
        JavaConstructor javaConstr = _javaClass.getJavaConstructorForConstructor(aConstr);
        if (javaConstr == null)
            javaConstr = new JavaConstructor(_resolver, _javaClass, aConstr);
        return javaConstr;
    }
}

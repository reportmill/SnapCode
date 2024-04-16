/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.*;
import snap.util.ArrayUtils;
import java.lang.reflect.Modifier;

/**
 * This class updates a JavaClass from JClassDecl.
 */
public class JavaClassUpdaterDecl extends JavaClassUpdater {

    // The ClassDecl
    protected JClassDecl  _classDecl;

    /**
     * Constructor.
     */
    public JavaClassUpdaterDecl(JavaClass aClass, JClassDecl aClassDecl)
    {
        super(aClass);
        _classDecl = aClassDecl;
    }

    /**
     * Override to just return anything.
     */
    @Override
    protected Class<?> getRealClassImpl()
    {
        JavaClass superClass = _classDecl.getSuperClass();
        return superClass.getRealClass();
    }

    /**
     * Returns the modifiers.
     */
    @Override
    protected int getModifiers()  { return _classDecl.getModifiers().getValue(); }

    /**
     * Returns the super class name.
     */
    @Override
    protected String getSuperClassName()
    {
        JavaClass superClass = _classDecl.getSuperClass();
        return superClass != null ? superClass.getName() : null;
    }

    /**
     * Returns interfaces.
     */
    @Override
    protected JavaClass[] getInterfaces()
    {
        return new JavaClass[0];
    }

    /**
     * Returns JavaTypeVariable array for given class TypeVariables.
     */
    @Override
    protected JavaTypeVariable[] getTypeVariables()
    {
        return new JavaTypeVariable[0];
    }

    /**
     * Updates inner classes.
     */
    @Override
    protected JavaClass[] getDeclaredClasses()
    {
        JClassDecl[] classDecls = _classDecl.getDeclaredClassDecls();
        return ArrayUtils.mapNonNull(classDecls, cdecl -> cdecl.getJavaClass(), JavaClass.class);
    }

    /**
     * Updates methods.
     */
    @Override
    protected JavaField[] getDeclaredFields()
    {
        // Get FieldDecls
        JVarDecl[] fieldDecls = _classDecl.getVarDecls();

        // Get fields
        JavaField.Builder fb = new JavaField.Builder(_javaClass);
        JavaField[] fields = ArrayUtils.map(fieldDecls, vd -> getJavaFieldForVarDecl(vd, fb), JavaField.class);

        // Handle enum: Add constant fields
        if (_classDecl.isEnum()) {
            JEnumConst[] enumConsts = _classDecl.getEnumConstants();
            JavaField[] enumFields = ArrayUtils.map(enumConsts, ec -> getJavaFieldForEnumConst(ec, fb), JavaField.class);
            fields = ArrayUtils.addAll(fields, enumFields);
        }

        // Return
        return fields;
    }

    /**
     * Returns a JavaField for given field var decl from class decl, creating if missing.
     */
    private JavaField getJavaFieldForVarDecl(JVarDecl varDecl, JavaField.Builder fb)
    {
        String fieldName = varDecl.getName();
        fb.name(fieldName);
        JFieldDecl fieldDecl = (JFieldDecl) varDecl.getParent();
        fb.mods(fieldDecl.getModifiers().getValue());

        // Get/set type
        JavaType varType = varDecl.getJavaType();
        if (varType != null)
            fb.type(varType);

        // Add to builder list
        return fb.build();
    }

    /**
     * Returns a JavaField for given enum constant from class decl, creating if missing.
     */
    private JavaField getJavaFieldForEnumConst(JEnumConst enumConst, JavaField.Builder fb)
    {
        String enumConstName = enumConst.getName();
        fb.name(enumConstName);
        fb.type(_javaClass);
        fb.mods(Modifier.PUBLIC | Modifier.STATIC);
        return fb.build();
    }

    /**
     * Updates methods.
     */
    @Override
    protected JavaMethod[] getDeclaredMethods() throws SecurityException
    {
        // Get Methods
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        JavaMethod.Builder mb = new JavaMethod.Builder(_javaClass);

        // Add JavaDecl for each declared method
        for (JMethodDecl methodDecl : methodDecls) {

            // Set MethodDecl
            mb.methodDecl(methodDecl);

            // Get/set modifiers
            int mods = methodDecl.getModifiers().getValue();
            mb.mods(mods);

            // Get/set name
            String methodName = methodDecl.getName();
            if (methodName == null)
                continue;
            mb.name(methodName);

            // Get/set param types
            JavaType[] paramTypes = methodDecl.getParameterClasses();
            mb.paramTypes(paramTypes);

            // Get/set return type
            JType returnTypeDecl = methodDecl.getType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getJavaClass() : null;
            if (returnType == null)
                returnType = _resolver.getJavaTypeForType(Object.class);
            mb.returnType(returnType);

            // Add to builder list
            mb.save();
        }

        // Return Methods
        return mb.buildAll();
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        // Get constructors
        JConstrDecl[] constrDecls = _classDecl.getConstructorDecls();
        JavaConstructor.Builder cb = new JavaConstructor.Builder(_javaClass);

        // Add JavaDecl for each declared constructor
        for (JConstrDecl methodDecl : constrDecls) {

            // Get/set modifiers
            int mods = methodDecl.getModifiers().getValue();
            cb.mods(mods);

            // Get/set param types
            JavaType[] paramTypes = methodDecl.getParameterClasses();
            cb.paramTypes(paramTypes);

            // Add to builder list
            cb.save();
        }

        // Get constructors
        JavaConstructor[] constructors = cb.buildAll();

        // If none, add default
        if (constructors.length == 0) {
            cb.mods(Modifier.PUBLIC);
            cb.save();
            constructors = cb.buildAll();
        }

        // Return
        return constructors;
    }

    /**
     * Returns the enum constants.
     */
    @Override
    public Object[] getEnumConstants()
    {
        JavaField[] fields = super.getDeclaredFields();
        fields = ArrayUtils.filter(fields, field -> field.isStatic());
        Object[] enumConsts = new Object[fields.length];
        for (int i = 0; i < fields.length; i++)
            enumConsts[i] = new JavaEnum(_javaClass, fields[i].getName());
        return enumConsts;
    }
}

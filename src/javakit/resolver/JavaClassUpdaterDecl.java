/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.*;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import java.lang.reflect.Modifier;
import java.util.*;

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
     * Sets the class decl.
     */
    public void setClassDecl(JClassDecl aClassDecl)
    {
        _classDecl = aClassDecl;
        _javaClass._fields = null;
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
        List<JVarDecl> fieldDecls = _classDecl.getVarDecls();

        // Get fields
        JavaField.FieldBuilder fb = new JavaField.FieldBuilder();
        fb.init(_resolver, _javaClass.getClassName());
        JavaField[] fields = ListUtils.mapToArray(fieldDecls, vd -> getJavaFieldForVarDecl(vd, fb), JavaField.class);

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
    private JavaField getJavaFieldForVarDecl(JVarDecl varDecl, JavaField.FieldBuilder fb)
    {
        JFieldDecl fieldDecl = (JFieldDecl) varDecl.getParent();
        String fieldName = varDecl.getName();
        JavaField javaField = _javaClass.getDeclaredFieldForName(fieldName);
        if (javaField == null) {
            fb.name(fieldName);
            fb.mods(fieldDecl.getModifiers().getValue());

            // Get/set type
            JType varTypeDecl = varDecl.getType();
            JavaType varType = varTypeDecl != null ? varTypeDecl.getDecl() : null;
            if (varType != null)
                fb.type(varType);

            // Add to builder list
            javaField = fb.build();
        }
        return javaField;
    }

    /**
     * Returns a JavaField for given enum constant from class decl, creating if missing.
     */
    private JavaField getJavaFieldForEnumConst(JEnumConst enumConst, JavaField.FieldBuilder fb)
    {
        String enumConstName = enumConst.getName();
        JavaField enumField = _javaClass.getDeclaredFieldForName(enumConstName);
        if (enumField == null) // Need to make sure type and mods match too
            enumField = fb.name(enumConstName).type(_javaClass).mods(Modifier.PUBLIC | Modifier.STATIC).build();
        return enumField;
    }

    /**
     * Updates methods.
     */
    @Override
    protected JavaMethod[] getDeclaredMethods() throws SecurityException
    {
        // Get Methods
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder();
        mb.init(_resolver, _javaClass.getClassName());

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
            List<JVarDecl> paramsDecls = methodDecl.getParameters();
            JavaType[] paramTypes = ListUtils.mapToArray(paramsDecls, varDecl -> getJavaTypeForVarDecl(varDecl), JavaType.class);
            mb.paramTypes(paramTypes);

            // Get/set return type
            JType returnTypeDecl = methodDecl.getType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getDecl() : null;
            if (returnType != null)
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
        JavaConstructor.ConstructorBuilder cb = new JavaConstructor.ConstructorBuilder();
        cb.init(_resolver, _javaClass.getClassName());

        // Add JavaDecl for each declared constructor
        for (JConstrDecl methodDecl : constrDecls) {

            // Get/set modifiers
            int mods = methodDecl.getModifiers().getValue();
            cb.mods(mods);

            // Get/set param types
            List<JVarDecl> paramsDecls = methodDecl.getParameters();
            JavaType[] paramTypes = ListUtils.mapToArray(paramsDecls, varDecl -> getJavaTypeForVarDecl(varDecl), JavaType.class);
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

    /**
     * Returns a field value.
     */
    public Object getFieldValue(JavaField aField, Object anObj)
    {
        // Handle Enum constants
        if (_javaClass.isEnum() && aField.isStatic()) {
            String fieldName = aField.getName();
            Object[] enumConsts = _javaClass.getEnumConstants();
            return ArrayUtils.findMatch(enumConsts, obj -> obj.toString().equals(fieldName));
        }

        // Complain
        throw new RuntimeException("JavaClassUpdaterDecl.getFieldValue: Can't resolve field: " + aField.getName());
    }

    /**
     * Returns a JavaType for given var decl (substituting Object if not found).
     */
    private JavaType getJavaTypeForVarDecl(JVarDecl varDecl)
    {
        JavaType javaType = varDecl.getEvalType();
        if (javaType == null)
            javaType = _resolver.getJavaTypeForType(Object.class);
        return javaType;
    }
}

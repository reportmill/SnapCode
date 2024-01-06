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
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    @Override
    public boolean updateDeclsImpl() throws SecurityException
    {
        // If first time, set decls
        if (_javaClass._fields == null)
            _javaClass._fields = new JavaField[0];

        // Update SuperClass
        JavaClass superClass = _classDecl.getSuperClass();
        _javaClass._superClass = superClass;
        _javaClass._superClassName = superClass.getName();

        // Update interfaces
        //_javaClass._interfaces = getInterfaces();

        // Update type variables
        //_javaClass._typeVars = getTypeVariables();

        // Update inner classes
        _javaClass._innerClasses = getDeclaredClasses();

        // Update fields
        _javaClass._fields = getDeclaredFields();

        // Update methods
        _javaClass._methods = getDeclaredMethods();

        // Update constructors
        //_javaClass._constructors = getDeclaredConstructors();

        // Return
        return true;
    }

    /**
     * Updates inner classes.
     */
    private JavaClass[] getDeclaredClasses()
    {
        JClassDecl[] innerClassDecls = _classDecl.getClassDecls();
        return ArrayUtils.map(innerClassDecls, cdecl -> getJavaClassForClassDecl(cdecl), JavaClass.class);
    }

    /**
     * Returns a JavaClass for given inner Class from JavaClass, creating if missing.
     */
    private JavaClass getJavaClassForClassDecl(JClassDecl anInnerClassDecl)
    {
        // Get or create class for class name
        String innerClassName = anInnerClassDecl.getSimpleName();
        JavaClass innerClass = _javaClass.getInnerClassForName(innerClassName);
        if (innerClass == null)
            innerClass = anInnerClassDecl.getDecl();

        // Reset decl
        JavaClassUpdaterDecl classUpdater = (JavaClassUpdaterDecl) innerClass.getUpdater();
        classUpdater.setClassDecl(anInnerClassDecl);

        // Return
        return innerClass;
    }

    /**
     * Updates methods.
     */
    private JavaField[] getDeclaredFields()
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
        JavaField javaField = _javaClass.getFieldForName(fieldName);
        if (javaField == null) {
            fb.name(fieldName);
            fb.mods(fieldDecl.getMods().getValue());

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
        JavaField enumField = _javaClass.getFieldForName(enumConstName);
        if (enumField == null) // Need to make sure type and mods match too
            enumField = fb.name(enumConstName).type(_javaClass).mods(Modifier.PUBLIC | Modifier.STATIC).build();
        return enumField;
    }

    /**
     * Updates methods.
     */
    private JavaMethod[] getDeclaredMethods() throws SecurityException
    {
        // Get Methods
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder();
        mb.init(_resolver, _javaClass.getClassName());

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (JMethodDecl methodDecl : methodDecls) {

            // Set MethodDecl
            mb.methodDecl(methodDecl);

            // Get/set modifiers
            int mods = methodDecl.getMods().getValue();
            mb.mods(mods);

            // Get/set name
            String methodName = methodDecl.getName();
            if (methodName == null)
                continue;
            mb.name(methodName);

            // Get/set param types
            List<JVarDecl> paramsList = methodDecl.getParameters();
            JavaType[] params = new JavaType[paramsList.size()];
            for (int i = 0, iMax = paramsList.size(); i < iMax; i++) {
                JVarDecl paramDecl = paramsList.get(i);
                JavaType paramType = paramDecl.getEvalType();
                if (paramType == null)
                    paramType = _resolver.getJavaTypeForType(Object.class);
                params[i] = paramType;
            }
            mb.paramTypes(params);

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
}

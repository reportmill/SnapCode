/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.*;
import snap.util.ArrayUtils;
import java.lang.reflect.*;

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
        JavaField[] fields = ArrayUtils.map(fieldDecls, vd -> getJavaFieldForVarDecl(vd, _javaClass), JavaField.class);

        // Handle enum: Add constant fields
        if (_classDecl.isEnum()) {
            JEnumConst[] enumConsts = _classDecl.getEnumConstants();
            JavaField[] enumFields = ArrayUtils.map(enumConsts, ec -> getJavaFieldForEnumConst(ec, _javaClass), JavaField.class);
            fields = ArrayUtils.addAll(fields, enumFields);
        }

        // Return
        return fields;
    }

    /**
     * Returns a JavaField for given field var decl from class decl, creating if missing.
     */
    private JavaField getJavaFieldForVarDecl(JVarDecl varDecl, JavaClass javaClass)
    {
        String fieldName = varDecl.getName();
        JFieldDecl fieldDecl = (JFieldDecl) varDecl.getParent();
        int fieldMods = fieldDecl.getModifiers().getValue();
        JavaType fieldType = varDecl.getJavaType();
        if (fieldType == null)
            fieldType = javaClass.getJavaClassForClass(Object.class);
        return JavaField.createField(javaClass, fieldName, fieldType, fieldMods);
    }

    /**
     * Returns a JavaField for given enum constant from class decl, creating if missing.
     */
    private JavaField getJavaFieldForEnumConst(JEnumConst enumConst, JavaClass javaClass)
    {
        String enumConstName = enumConst.getName();
        return JavaField.createField(javaClass, enumConstName, javaClass, Modifier.PUBLIC | Modifier.STATIC);
    }

    /**
     * Updates methods.
     */
    @Override
    protected JavaMethod[] getDeclaredMethods() throws SecurityException
    {
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        return ArrayUtils.mapNonNull(methodDecls, mdecl -> getJavaMethodForMethodDecl(mdecl), JavaMethod.class);
    }

    /**
     * Returns a JavaMethod for given JMethodDecl.
     */
    private JavaMethod getJavaMethodForMethodDecl(JMethodDecl methodDecl)
    {
        if (methodDecl.getName() == null)
            return null;
        JavaMethod javaMethod = new JavaMethod(_javaClass._resolver, _javaClass, null);
        ReflectReader.ExecutableReader execReader = new ExecutableReaderDecl(methodDecl);
        javaMethod.setReader(execReader);
        return javaMethod;
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        // Get constructors
        JConstrDecl[] constrDecls = _classDecl.getConstructorDecls();
        if (constrDecls.length > 0)
            return ArrayUtils.map(constrDecls, this::getJavaConstructorForConstructorDecl, JavaConstructor.class);

        // If none, return default
        return new JavaConstructor[] { JavaConstructor.createDefaultConstructor(_javaClass) };
    }

    /**
     * Returns a JavaConstructor for given JConstrDecl.
     */
    private JavaConstructor getJavaConstructorForConstructorDecl(JConstrDecl constrDecl)
    {
        JavaConstructor javaConstr = new JavaConstructor(_javaClass._resolver, _javaClass, null);
        ReflectReader.ExecutableReader execReader = new ExecutableReaderDecl(constrDecl);
        javaConstr.setReader(execReader);
        return javaConstr;
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
     * This class is an ExecutableReader implementation for JExecutableDecl.
     */
    private static class ExecutableReaderDecl implements ReflectReader.ExecutableReader {

        // The JavaExecutable
        private JavaExecutable _javaExecutable;

        // The executable decl
        private JExecutableDecl _executableDecl;

        /**
         * Constructor.
         */
        public ExecutableReaderDecl(JExecutableDecl executableDecl)
        {
            _executableDecl = executableDecl;
        }

        /**
         * Sets the JavaExecutable.
         */
        @Override
        public void setJavaExecutable(JavaExecutable anExec)  { _javaExecutable = anExec; }

        /**
         * Returns the name.
         */
        @Override
        public String getName()  { return _executableDecl.getName(); }

        /**
         * Returns the simple name.
         */
        @Override
        public String getSimpleName()
        {
            if (_executableDecl instanceof JConstrDecl)
                return _javaExecutable._declaringClass.getSimpleName();
            return _executableDecl.getName();
        }

        /**
         * Returns the modifiers.
         */
        @Override
        public int getModifiers()  { return _executableDecl.getModifiers().getValue(); }

        /**
         * Returns whether Method/Constructor is VarArgs type.
         */
        @Override
        public boolean isVarArgs()  { return false; } //_executable.isVarArgs();

        /**
         * Returns whether Method is default type.
         */
        @Override
        public boolean isDefault()  { return false; } //_executable instanceof JMethodDecl && ((JMethodDecl) _executable).isDefault();

        /**
         * Returns the TypeVars.
         */
        @Override
        public JavaTypeVariable[] getTypeVars()
        {
            // Get TypeVariables
            JTypeVar[] typeVars = _executableDecl.getTypeVars();
            return ArrayUtils.map(typeVars, tvar -> getJavaTypeVariableForTypeVarDecl(tvar), JavaTypeVariable.class);
        }

        /**
         * Returns a JavaTypeVariable for given JTypeVar.
         */
        private JavaTypeVariable getJavaTypeVariableForTypeVarDecl(JTypeVar typeVarDecl)
        {
            String typeVarName = typeVarDecl.getName();
            JavaClass typeVarClass = typeVarDecl.getBoundsClass();
            return new JavaTypeVariable(_javaExecutable._resolver, _javaExecutable, typeVarName, typeVarClass);
        }

        /**
         * Returns the parameter types.
         */
        @Override
        public JavaType[] getGenericParameterTypes()  { return _executableDecl.getParameterClasses(); }

        /**
         * Returns the return type.
         */
        @Override
        public JavaType getGenericReturnType()
        {
            JType returnTypeDecl = ((JMethodDecl) _executableDecl).getReturnType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getJavaClass() : null;
            return returnType != null ? returnType : _javaExecutable._resolver.getJavaClassForName("java.lang.Object");
        }

        /**
         * Returns the parameter classes.
         */
        @Override
        public JavaClass[] getParameterClasses()
        {
            JVarDecl[] paramVarDecls = _executableDecl.getParameters();
            return ArrayUtils.map(paramVarDecls, JVarDecl::getJavaClass, JavaClass.class);
        }

        /**
         * Returns the parameter names.
         */
        @Override
        public String[] getParameterNames()  { return _executableDecl.getParameterNames(); }
    }
}

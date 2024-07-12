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
     * Returns the generic superclass.
     */
    protected JavaType getGenericSuperclass()
    {
        if (_classDecl.isInterface())
            return null;
        JType extendsType = _classDecl.getExtendsType();
        JavaType genericSuperClass = extendsType != null ? extendsType.getJavaType() : null;
        if (genericSuperClass != null)
            return genericSuperClass;
        return _classDecl.getSuperClass();
    }

    /**
     * Returns interfaces.
     */
    @Override
    protected JavaClass[] getInterfaces()
    {
        return _classDecl.getInterfaces();
    }

    /**
     * Returns JavaTypeVariable array for given class TypeVariables.
     */
    @Override
    protected JavaTypeVariable[] getTypeParameters()
    {
        JTypeVar[] typeVars = _classDecl.getTypeParamDecls();
        return ArrayUtils.map(typeVars, tvar -> new JavaTypeVariable(_resolver, _javaClass, tvar.getName(), tvar.getBoundsClass()), JavaTypeVariable.class);
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
        // If first call for JavaClass, init methods to empty to prevent infinite loop
        if (_javaClass._methods == null) _javaClass._methods = new JavaMethod[0];

        // Get methods from ClassDecl.MethodDecls
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        return ArrayUtils.mapNonNull(methodDecls, mdecl -> getJavaMethodForMethodDecl(mdecl), JavaMethod.class);
    }

    /**
     * Returns a JavaMethod for given JMethodDecl.
     */
    private JavaMethod getJavaMethodForMethodDecl(JMethodDecl methodDecl)
    {
        // Get method - just return if can't be found or created
        JavaMethod javaMethod = methodDecl.getMethod();
        if (javaMethod == null)
            return null;

        // If method not brand new, create new
        if (getJavaExecutableDecl(javaMethod) != methodDecl)
            javaMethod = createMethodForDecl(methodDecl);

        // Return
        return javaMethod;
    }

    /**
     * Returns JavaConstructor array for given class.
     */
    protected JavaConstructor[] getDeclaredConstructors()
    {
        // If first call for JavaClass, init methods to empty to prevent infinite loop
        if (_javaClass._constructors == null) _javaClass._constructors = new JavaConstructor[0];

        // Get constructors from ClassDecl.ConstructorDecls
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
        // Get constructor - just return if can't be found or created
        JavaConstructor javaConstr = constrDecl.getConstructor();
        if (javaConstr == null)
            return null;

        // If constructor not brand new, create new
        if (getJavaExecutableDecl(javaConstr) != constrDecl)
            javaConstr = createConstructorForDecl(constrDecl);

        // Return
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
     * Returns the method/constructor decl for given JavaExecutable.
     */
    private static JExecutableDecl getJavaExecutableDecl(JavaExecutable javaExecutable)
    {
        if (javaExecutable._execReader instanceof ExecutableReaderDecl)
            return ((ExecutableReaderDecl) javaExecutable._execReader)._executableDecl;
        return null;
    }

    /**
     * Creates a JavaMethod for given JMethodDecl.
     */
    public static JavaMethod createMethodForDecl(JMethodDecl methodDecl)
    {
        // If no method name, just return null
        if (methodDecl.getName() == null)
            return null;

        // Get parent class
        JClassDecl enclosingClassDecl = methodDecl.getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // Create JavaMethod
        JavaMethod javaMethod = new JavaMethod(methodDecl.getResolver(), javaClass, null);
        ExecutableReader execReader = new JavaClassUpdaterDecl.ExecutableReaderDecl(methodDecl);
        javaMethod.setReader(execReader);
        return javaMethod;
    }

    /**
     * Returns the Constructor.
     */
    public static JavaConstructor createConstructorForDecl(JConstrDecl constrDecl)
    {
        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = constrDecl.getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // Create JavaMethod
        JavaConstructor javaConstructor = new JavaConstructor(constrDecl.getResolver(), javaClass, null);
        ExecutableReader execReader = new JavaClassUpdaterDecl.ExecutableReaderDecl(constrDecl);
        javaConstructor.setReader(execReader);
        return javaConstructor;
    }

    /**
     * This class is an ExecutableReader implementation for JExecutableDecl.
     */
    private static class ExecutableReaderDecl implements ExecutableReader {

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
        public JavaTypeVariable[] getTypeParameters()
        {
            // Get TypeVariables
            JTypeVar[] typeVars = _executableDecl.getTypeParamDecls();
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
        public JavaType[] getGenericParameterTypes()  { return _executableDecl.getGenericParameterTypes(); }

        /**
         * Returns the return type.
         */
        @Override
        public JavaType getGenericReturnType()
        {
            JType returnTypeDecl = ((JMethodDecl) _executableDecl).getReturnType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getJavaType() : null;
            if (returnType != null)
                return returnType;

            // Fallback
            return _javaExecutable._resolver.getJavaClassForName("java.lang.Object");
        }

        /**
         * Returns the parameter classes.
         */
        @Override
        public JavaClass[] getParameterClasses()  { return _executableDecl.getParameterClasses(); }

        /**
         * Returns the parameter names.
         */
        @Override
        public String[] getParameterNames()  { return _executableDecl.getParameterNames(); }
    }
}

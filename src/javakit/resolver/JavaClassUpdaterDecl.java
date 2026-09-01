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
        return ArrayUtils.mapNonNull(classDecls, JClassDecl::getJavaClass, JavaClass.class);
    }

    /**
     * Updates methods.
     */
    @Override
    protected JavaField[] getDeclaredFields()
    {
        // Get fields for all field decl var decls
        JVarDecl[] fieldDecls = _classDecl.getFieldVarDecls();
        JavaField[] fields = ArrayUtils.map(fieldDecls, this::getJavaFieldForVarDecl, JavaField.class);

        // Handle enum: Add constant fields
        if (_classDecl.isEnum()) {
            JEnumConst[] enumConsts = _classDecl.getEnumConstants();
            JavaField[] enumFields = ArrayUtils.map(enumConsts, this::getJavaFieldForEnumConst, JavaField.class);
            fields = ArrayUtils.addAll(fields, enumFields);
        }

        // Handle record: Add fields for record parameters
        else if (_classDecl.isRecord()) {
            JVarDecl[] params = _classDecl.getParameters();
            JavaField[] recordFields = ArrayUtils.mapNonNull(params, this::getJavaFieldForVarDecl, JavaField.class);
            fields = ArrayUtils.addAll(recordFields, fields);
        }

        return fields;
    }

    /**
     * Returns a JavaField for given field var decl from class decl, creating if missing.
     */
    private JavaField getJavaFieldForVarDecl(JVarDecl varDecl)
    {
        String fieldName = varDecl.getName();
        int fieldMods = varDecl.getParent() instanceof JFieldDecl fieldDecl ? fieldDecl.getModifiers().getValue() : Modifier.PROTECTED;
        JavaType fieldType = varDecl.getJavaType();
        if (fieldType == null)
            fieldType = _javaClass.getJavaClassForClass(Object.class);
        return JavaField.createField(_javaClass, fieldName, fieldType, fieldMods);
    }

    /**
     * Returns a JavaField for given enum constant from class decl, creating if missing.
     */
    private JavaField getJavaFieldForEnumConst(JEnumConst enumConst)
    {
        String enumConstName = enumConst.getName();
        return JavaField.createField(_javaClass, enumConstName, _javaClass, Modifier.PUBLIC | Modifier.STATIC);
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
        JavaMethod[] methods = ArrayUtils.mapNonNull(methodDecls, JavaClassUpdaterDecl::getJavaMethodForMethodDecl, JavaMethod.class);

        // If record, create and add methods for record parameters
        if (_classDecl.isRecord()) {
            JVarDecl[] params = _classDecl.getParameters();
            JavaMethod[] recordMethods = ArrayUtils.mapNonNull(params, this::getJavaMethodForVarDecl, JavaMethod.class);
            methods = ArrayUtils.addAll(recordMethods, methods);
        }

        return methods;
    }

    /**
     * Returns a JavaMethod for given JMethodDecl.
     */
    private static JavaMethod getJavaMethodForMethodDecl(JMethodDecl methodDecl)
    {
        // Get method - just return if can't be found or created
        JavaMethod javaMethod = methodDecl.getMethod();
        if (javaMethod == null)
            return null;

        // If method not brand new, create new
        if (getJavaExecutableDecl(javaMethod) != methodDecl)
            javaMethod = createMethodForDecl(methodDecl);

        return javaMethod;
    }

    /**
     * Returns a JavaMethod for given JVarDecl.
     */
    private JavaMethod getJavaMethodForVarDecl(JVarDecl varDecl)
    {
        JExprId varDeclId = varDecl.getId();
        if (varDeclId == null)
            return null;

        // Create JavaMethod
        JavaMethod javaMethod = new JavaMethod(varDecl.getResolver(), _javaClass, null);
        javaMethod._mods = Modifier.PUBLIC;
        javaMethod._name = javaMethod._simpleName = varDeclId.getName();
        javaMethod._typeParameters = new JavaTypeVariable[0];
        javaMethod._genericParameterTypes = JavaType.EMPTY_TYPES_ARRAY;
        javaMethod._parameterTypes = new JavaClass[0];
        javaMethod._parameterNames = new String[0];
        javaMethod._genericReturnType = varDecl.getJavaType();
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
        JavaConstructor[] constructors = ArrayUtils.mapNonNull(constrDecls, JavaClassUpdaterDecl::getJavaConstructorForConstructorDecl, JavaConstructor.class);

        // If no explicit constructors or no record canonical constructor, add default constructor
        if (constructors.length == 0 || _classDecl.isRecord() && ArrayUtils.hasMatch(constrDecls, JConstrDecl::isRecordCanonicalConstructor)) {
            JavaConstructor defaultConstructor = createDefaultConstructor(_javaClass, _classDecl);
            constructors = ArrayUtils.add(constructors, defaultConstructor, 0);
        }

        return constructors;
    }

    /**
     * Returns a JavaConstructor for given JConstrDecl.
     */
    private static JavaConstructor getJavaConstructorForConstructorDecl(JConstrDecl constrDecl)
    {
        // Get constructor - just return if can't be found or created
        JavaConstructor javaConstr = constrDecl.getConstructor();
        if (javaConstr == null)
            return null;

        // If constructor not brand new, create new
        if (getJavaExecutableDecl(javaConstr) != constrDecl)
            javaConstr = createConstructorForDecl(constrDecl);

        return javaConstr;
    }

    /**
     * Returns the enum constants.
     */
    @Override
    public Object[] getEnumConstants()
    {
        JavaField[] fields = super.getDeclaredFields();
        fields = ArrayUtils.filter(fields, JavaField::isStatic);
        return ArrayUtils.map(fields, field -> new JavaEnum(_javaClass, field.getName()), JavaEnum.class);
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
     * Creates a default constructor for given class.
     */
    public static JavaConstructor createDefaultConstructor(JavaClass javaClass, JClassDecl classDecl)
    {
        JavaConstructor constr = new JavaConstructor(javaClass._resolver, javaClass, null);
        constr._mods = Modifier.PUBLIC;
        constr._name = constr._simpleName = javaClass.getSimpleName();
        constr._evalType = javaClass;
        constr._typeParameters = new JavaTypeVariable[0];
        constr._genericParameterTypes = JavaType.EMPTY_TYPES_ARRAY;
        constr._parameterTypes = new JavaClass[0];
        constr._parameterNames = new String[0];

        // If record, set constructor parameters
        if (classDecl.isRecord()) {
            JVarDecl[] paramDecls = classDecl.getParameters();
            JavaType[] genericParamTypes = ArrayUtils.mapNonNull(paramDecls, JVarDecl::getJavaType, JavaType.class);
            JavaClass[] paramTypes = ArrayUtils.mapNonNull(paramDecls, JVarDecl::getJavaClass, JavaClass.class);
            JExprId[] nameIds = ArrayUtils.mapNonNull(paramDecls, JVarDecl::getId, JExprId.class);
            String[] paramNames = ArrayUtils.map(nameIds, JExprId::getName, String.class);
            if (genericParamTypes.length == paramDecls.length && paramTypes.length == paramDecls.length &&
                paramNames.length == paramDecls.length) {
                constr._typeParameters = ArrayUtils.mapNonNull(classDecl.getTypeParamDecls(), JTypeVar::getTypeVariable, JavaTypeVariable.class);
                constr._genericParameterTypes = genericParamTypes;
                constr._parameterTypes = paramTypes;
                constr._parameterNames = paramNames;
            }
        }

        return constr;
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
            return ArrayUtils.map(typeVars, this::getJavaTypeVariableForTypeVarDecl, JavaTypeVariable.class);
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
        public JavaType[] getGenericParameterTypes()
        {
            if (_executableDecl instanceof JConstrDecl constrDecl)
                return constrDecl.getGenericParameterTypesAll();
            return _executableDecl.getGenericParameterTypes();
        }

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
        public JavaClass[] getParameterClasses()
        {
            if (_executableDecl instanceof JConstrDecl constrDecl)
                return constrDecl.getParameterClassesAll();
            return _executableDecl.getParameterClasses();
        }

        /**
         * Returns the parameter names.
         */
        @Override
        public String[] getParameterNames()  { return _executableDecl.getParameterNames(); }
    }
}

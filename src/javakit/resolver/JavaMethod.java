/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.JMethodDecl;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * This class represents a Java Method.
 */
public class JavaMethod extends JavaExecutable {

    // Whether method is Default method
    private boolean  _default;

    // The super implementation of this method
    protected JavaMethod  _super;

    // The method
    protected Method  _method;

    // The method decl
    private JMethodDecl  _methodDecl;

    /**
     * Constructor.
     */
    public JavaMethod(Resolver aResolver, JavaClass aDeclaringClass, Method aMethod)
    {
        super(aResolver, DeclType.Method, aDeclaringClass, aMethod);
        if (aMethod == null) return;

        // Get whether default
        _default = aMethod.isDefault();

        // Set Method
        _method = aMethod;
    }

    /**
     * Resolves types.
     */
    protected void initTypes(Method aMethod)
    {
        // Get/set EvalType to method return Type
        Type returnType = aMethod.getGenericReturnType();
        _evalType = _resolver.getJavaTypeForType(returnType);

        // Do normal version
        super.initTypes(aMethod);
    }

    /**
     * Returns whether Method is default type.
     */
    public boolean isDefault()  { return _default; }

    /**
     * Returns the Method.
     */
    public Method getMethod()  { return _method; }

    /**
     * Returns the JMethodDecl.
     */
    public JMethodDecl getMethodDecl()  { return _methodDecl; }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaMethod getSuper()
    {
        // If already set, just return
        if (_super != null)
            return _super != this ? _super : null;

        // Get superclass and helper
        JavaClass declaringClass = getDeclaringClass();
        JavaClass superClass = declaringClass != null ? declaringClass.getSuperClass() : null;
        if (superClass == null)
            return null;

        // Get super method
        String name = getName();
        JavaType[] paramTypes = getParameterTypes();
        JavaMethod superMethod = superClass.getMethodDeepForNameAndTypes(name, paramTypes);

        // If not found, check interfaces
        if (superMethod == null) {
            JavaClass[] interfaces = declaringClass.getInterfaces();
            for (JavaClass inf : interfaces) {
                superMethod = inf.getMethodDeepForNameAndTypes(name, paramTypes);
                if (superMethod != null)
                    break;
            }
        }

        // If not found, set to this
        if (superMethod == null)
            superMethod = this;

        // Set/return
        _super = superMethod;
        return _super != this ? _super : null;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get normal version and ClassName
        String superName = super.getSuggestionString();
        String classStr = getDeclaringClassName();

        // Construct string SimpleName(ParamType.SimpleName, ...) - ClassName
        return superName + " - " + classStr;
    }

    /**
     * Returns a signature.
     */
    public static String getSigForParts(JavaClass aClass, String aName, JavaType[] paramTypes)
    {
        // Basic "pkg.pkg.ClassName.MethodName()"
        String prefix = aClass.getId() + '.' + aName;
        if (paramTypes.length == 0)
            return prefix + "()";

        // Add ParamTypes: "(pkg.pkg.ClassName,pkg.pkg.ClassName,...)"
        StringBuilder sb = new StringBuilder(prefix).append('(');
        for (JavaType type : paramTypes)
            sb.append(type.getId()).append(',');
        sb.setLength(sb.length() - 1);

        // Return string
        return sb.append(')').toString();
    }

    /**
     * A Builder class for JavaMethod.
     */
    public static class MethodBuilder {

        // Ivars
        Resolver  _resolver;
        JavaClass  _declaringClass;
        int  _mods =  Modifier.PUBLIC;
        String  _name;
        JavaType[]  _paramTypes = new JavaType[0];
        JavaType  _returnType;
        JavaTypeVariable[]  _typeVars = new JavaTypeVariable[0];
        boolean  _default;
        JMethodDecl  _methodDecl;

        // For build all
        private JavaMethod[]  _methods = new JavaMethod[200];
        private int  _methodCount;

        /**
         * Constructor.
         */
        public MethodBuilder()  { }

        /**
         * Init.
         */
        public void init(Resolver aResolver, String aClassName)
        {
            _resolver = aResolver;
            _declaringClass = aResolver.getJavaClassForName(aClassName);
        }

        // Properties.
        public MethodBuilder mods(int mods)  { _mods = mods; return this; }
        public MethodBuilder name(String name)  { _name = name; return this; }
        public MethodBuilder paramTypes(JavaType ...  paramTypes)  { _paramTypes = paramTypes; return this; }
        public MethodBuilder paramTypes(Type ...  paramTypes)  { _paramTypes = _resolver.getJavaTypesForTypes(paramTypes); return this; }
        public MethodBuilder returnType(JavaType returnType)  { _returnType = returnType; return this; }
        public MethodBuilder typeVars(String aName)  { return this; }
        public MethodBuilder isDefault(boolean isDefault)  { _default = isDefault; return this; }
        public MethodBuilder methodDecl(JMethodDecl methodDecl)  { _methodDecl = methodDecl; return this; }

        /**
         * Build.
         */
        public JavaMethod build()
        {
            JavaMethod m = new JavaMethod(_resolver, _declaringClass, null);
            m._mods = _mods;
            m._id = getSigForParts(_declaringClass, _name, _paramTypes);
            m._name = m._simpleName = _name;
            m._declaringClass = _declaringClass;
            m._parameterTypes = _paramTypes;
            m._evalType = _returnType;
            m._typeVars = _typeVars;
            m._default = _default;
            m._methodDecl = _methodDecl;
            _mods = Modifier.PUBLIC;
            _name = null;
            _paramTypes = new JavaType[0];
            _typeVars = new JavaTypeVariable[0];
            _default = false;
            _methodDecl = null;
            return m;
        }

        /**
         * Builds current method and saves it in array for buildAll.
         */
        public MethodBuilder save()
        {
            _methods[_methodCount++] = build(); return this;
        }

        /**
         * Returns an array of all currently saved methods.
         */
        public JavaMethod[] buildAll()
        {
            if (_name != null) save();
            JavaMethod[] methods = Arrays.copyOf(_methods, _methodCount);
            _methodCount = 0;
            return methods;
        }
    }
}

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

    // The generic return type
    private JavaType _genericReturnType;

    // The super implementation of this method
    protected JavaMethod  _super;

    // The method
    protected Method  _method;

    // The method decl
    protected JMethodDecl  _methodDecl;

    /**
     * Constructor.
     */
    public JavaMethod(Resolver aResolver, JavaClass aDeclaringClass, Method aMethod)
    {
        super(aResolver, DeclType.Method, aDeclaringClass, aMethod);
        if (aMethod == null) return;

        // Set Method
        _method = aMethod;
    }

    /**
     * Override to customize for method.
     */
    @Override
    protected void setReader(ReflectReader.ExecutableReader executableReader)
    {
        super.setReader(executableReader);
        _default = _execReader.isDefault();
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
     * Returns the return type.
     */
    public JavaClass getReturnType()
    {
        JavaType returnType = getGenericReturnType();
        return returnType.getEvalClass();
    }

    /**
     * Returns the return type.
     */
    public JavaType getGenericReturnType()
    {
        if (_genericReturnType != null) return _genericReturnType;
        return _genericReturnType = _execReader.getGenericReturnType();
    }

    /**
     * Override to get eval type dynamically.
     */
    @Override
    public JavaType getEvalType()
    {
        if (_evalType != null) return _evalType;
        return _evalType = getGenericReturnType();
    }

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
        JavaType[] paramTypes = getGenericParameterTypes();
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
        String returnTypeStr = getReturnType().getSimpleName();

        // Construct string SimpleName(ParamType.SimpleName, ...) - ClassName
        return superName + " - " + returnTypeStr;
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
     * Invokes this method on given object and args.
     */
    public Object invoke(Object anObj, Object ... theArgs) throws Exception
    {
        // Get method - check object class to get top method if overridden
        Method method = getMethod();

        // If not static receiver object class has override method, make sure we use override method
        if (!isStatic()) {
            Class<?> objClass = anObj.getClass();
            if (objClass != method.getDeclaringClass()) {
                method = objClass.getMethod(method.getName(), method.getParameterTypes());
                method.setAccessible(true); // Not sure why we need this sometimes
            }
        }

        // Invoke
        return method.invoke(anObj, theArgs);
    }

    /**
     * Merges the given new method into this method.
     */
    public boolean mergeMethod(JavaMethod newMethod)
    {
        // Update modifiers
        boolean didChange = false;
        if (newMethod.getModifiers() != getModifiers()) {
            _mods = newMethod.getModifiers();
            didChange = true;
        }

        // Update return type
        //if (newMethod.getGenericReturnType() != getGenericReturnType()) { _genericReturnType = newMethod.getGenericReturnType(); didChange = true; }

        // Update Method
        if (newMethod._method != null) {
            _method = newMethod._method;
            _methodDecl = null;
        }
        else if (newMethod._methodDecl != null)
            _methodDecl = newMethod._methodDecl;

        // Return
        return didChange;
    }

    /**
     * This method repackages a given array of individual method args into an args array for VarArgs method.
     */
    public Object[] repackageArgsForVarArgsMethod(Object[] theArgs)
    {
        // Get VarArg class
        Method aMethod = getMethod();
        int argCount = aMethod.getParameterCount();
        int varArgIndex = argCount - 1;
        Class<?>[] paramClasses = aMethod.getParameterTypes();
        Class<?> varArgArrayClass = paramClasses[varArgIndex];
        Class<?> varArgClass = varArgArrayClass.getComponentType();

        // If only one varArg and it is already packaged as VarArgArrayClass, just return
        if (theArgs.length == argCount) {
            Object firstVarArg = theArgs[varArgIndex];
            if (varArgArrayClass.isAssignableFrom(firstVarArg.getClass()))
                return theArgs;
        }

        // Create new args array of proper length
        Object[] args = Arrays.copyOf(theArgs, argCount);

        // Create VarArgs array of proper class and set in new args array
        int varArgsCount = theArgs.length - varArgIndex;
        Object varArgArray = args[varArgIndex] = Array.newInstance(varArgClass, varArgsCount);

        // Copy var args over from given args array to new VarArgsArray
        for (int i = 0; i < varArgsCount; i++) {
            Object varArg = theArgs[i + varArgIndex];
            Array.set(varArgArray, i, varArg);
        }

        // Return
        return args;
    }

    /**
     * Returns whether method is equal to name and types.
     */
    public boolean isEqualToNameAndTypes(String methodName, JavaType[] theTypes)
    {
        // If name not equal, return false
        if (!methodName.equals(getName()))
            return false;

        // Return whether types are equal
        JavaType[] methodParamTypes = getGenericParameterTypes();
        return JavaType.isTypesEqual(methodParamTypes, theTypes);
    }

    /**
     * A Builder class for JavaMethod.
     */
    public static class Builder {

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
        public Builder(JavaClass declaringClass)
        {
            _declaringClass = declaringClass;
            _resolver = declaringClass._resolver;
        }

        // Properties.
        public Builder mods(int mods)  { _mods = mods; return this; }
        public Builder name(String name)  { _name = name; return this; }
        public Builder paramTypes(JavaType ...  paramTypes)  { _paramTypes = paramTypes; return this; }
        public Builder paramTypes(Type ...  paramTypes)  { _paramTypes = _resolver.getJavaTypesForTypes(paramTypes); return this; }
        public Builder returnType(JavaType returnType)  { _returnType = returnType; return this; }
        public Builder typeVars(String aName)  { return this; }
        public Builder isDefault(boolean isDefault)  { _default = isDefault; return this; }
        public Builder methodDecl(JMethodDecl methodDecl)  { _methodDecl = methodDecl; return this; }

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
            m._genericParameterTypes = _paramTypes;
            m._genericReturnType = _returnType;
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
        public Builder save()
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

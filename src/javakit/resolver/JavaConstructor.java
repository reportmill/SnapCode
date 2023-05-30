/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaConstructor extends JavaExecutable {

    // The super implementation of this method
    protected JavaConstructor  _super;

    // The method
    protected Constructor<?>  _constructor;

    /**
     * Constructor.
     */
    public JavaConstructor(Resolver aResolver, JavaClass aDeclaringClass, Constructor<?> constructor)
    {
        super(aResolver, DeclType.Constructor, aDeclaringClass, constructor);

        // Reset SimpleName
        _simpleName = aDeclaringClass.getSimpleName();

        // Set EvalType to DeclaringClass
        _evalType = aDeclaringClass;

        // Set Constructor
        _constructor = constructor;
    }

    /**
     * Returns the constructor.
     */
    public Constructor<?> getConstructor()  { return _constructor; }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaConstructor getSuper()
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
        JavaType[] paramTypes = getParamTypes();
        JavaConstructor superMethod = superClass.getConstructorDeepForTypes(paramTypes);
        if (superMethod == null)
            superMethod = this;

        // Set/return
        _super = superMethod;
        return _super != this ? _super : null;
    }

    /**
     * Override to return Executable version plus package name.
     */
    @Override
    public String getSuggestionString()
    {
        String nameAndParamsStr = super.getSuggestionString();
        JavaClass declClass = getDeclaringClass();
        JavaClass enclClass = declClass.getDeclaringClass();
        JavaPackage pkg = declClass.getPackage();
        String parentName = enclClass != null ? enclClass.getName() : pkg != null ? pkg.getName() : "";
        String infoStr = parentName != null ? " - " + parentName : "";
        return nameAndParamsStr + infoStr;
    }

    /**
     * Returns a signature.
     */
    public static String getSigForParts(JavaClass aClass, JavaType[] paramTypes)
    {
        // Basic "pkg.pkg.ClassName()"
        String prefix = aClass.getId();
        if (paramTypes.length == 0) return prefix + "()";

        // Add ParamTypes: "(pkg.pkg.ClassName,pkg.pkg.ClassName,...)"
        StringBuffer sb = new StringBuffer(prefix).append('(');
        for (JavaType type : paramTypes)
            sb.append(type.getId()).append(',');
        sb.setLength(sb.length() - 1);

        // Return string
        return sb.append(')').toString();
    }

    /**
     * A Builder class for JavaConstructor.
     */
    public static class ConstructorBuilder {

        // Ivars
        Resolver  _resolver;
        JavaClass  _declaringClass;
        int  _mods =  Modifier.PUBLIC;
        JavaType[]  _paramTypes = new JavaType[0];
        JavaTypeVariable[]  _typeVars = new JavaTypeVariable[0];
        boolean  _varArgs;

        // For build all
        private JavaConstructor[]  _constructors = new JavaConstructor[20];
        private int  _constructorCount;

        /**
         * Constructor.
         */
        public ConstructorBuilder()  { }

        /**
         * init.
         */
        public void init(Resolver aResolver, String aClassName)
        {
            _resolver = aResolver;
            _declaringClass = aResolver.getJavaClassForName(aClassName);
        }

        // Properties.
        public ConstructorBuilder mods(int mods)  { _mods = mods; return this; }
        public ConstructorBuilder paramTypes(Type...  paramTypes)  { _paramTypes = _resolver.getJavaTypesForTypes(paramTypes); return this; }
        public ConstructorBuilder typeVars(String aName)  { return this; }
        public ConstructorBuilder isVarArgs(boolean varArgs)  { _varArgs = varArgs; return this; }

        /**
         * Build.
         */
        public JavaConstructor build()
        {
            JavaConstructor c = new JavaConstructor(_resolver, _declaringClass, null);
            c._mods = _mods;
            c._id = getSigForParts(_declaringClass, _paramTypes);
            c._name = c._simpleName = _declaringClass.getSimpleName();
            c._declaringClass = _declaringClass;
            c._paramTypes = _paramTypes;
            c._evalType = _declaringClass;
            c._typeVars = _typeVars;
            c._varArgs = _varArgs;
            _mods = Modifier.PUBLIC;
            _paramTypes = new JavaType[0];
            _typeVars = new JavaTypeVariable[0];
            _varArgs = false;
            return c;
        }

        /**
         * Builds current constructors and saves it in array for buildAll.
         */
        public ConstructorBuilder save()
        {
            _constructors[_constructorCount++] = build(); return this;
        }

        /**
         * Returns an array of all currently saved methods.
         */
        public JavaConstructor[] buildAll()
        {
            if (_paramTypes.length != 0) save();
            JavaConstructor[] constructors = Arrays.copyOf(_constructors, _constructorCount);
            _constructorCount = 0;
            return constructors;
        }
    }
}

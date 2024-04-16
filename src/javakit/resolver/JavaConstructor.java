/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

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
        JavaType[] paramTypes = getGenericParameterTypes();
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
        String suggestionString = super.getSuggestionString();

        // Get context string
        JavaClass declClass = getDeclaringClass();
        JavaClass enclClass = declClass.getDeclaringClass();
        JavaPackage pkg = declClass.getPackage();
        String parentName = enclClass != null ? enclClass.getName() : pkg != null ? pkg.getName() : "";
        String contextStr = parentName != null ? " - " + parentName : "";

        // Return
        return suggestionString + contextStr;
    }

    /**
     * Merges the given new constructor into this constructor.
     */
    public boolean mergeConstructor(JavaConstructor newConstr)
    {
        // Update modifiers
        boolean didChange = false;
        if (newConstr.getModifiers() != getModifiers()) {
            _mods = newConstr.getModifiers();
            didChange = true;
        }

        // Update return type
        //if (newMethod.getGenericReturnType() != getGenericReturnType()) { _genericReturnType = newMethod.getGenericReturnType(); didChange = true; }

        // Update Method
        if (newConstr._constructor != null)
            _constructor = newConstr._constructor;

        // Return
        return didChange;
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
        StringBuilder sb = new StringBuilder(prefix).append('(');
        for (JavaType type : paramTypes)
            sb.append(type.getId()).append(',');
        sb.setLength(sb.length() - 1);

        // Return string
        return sb.append(')').toString();
    }

    /**
     * Creates a default constructor for given class.
     */
    public static JavaConstructor createDefaultConstructor(JavaClass javaClass)
    {
        JavaConstructor c = new JavaConstructor(javaClass._resolver, javaClass, null);
        c._mods = Modifier.PUBLIC;
        c._id = getSigForParts(javaClass, JavaType.EMPTY_TYPES_ARRAY);
        c._name = c._simpleName = javaClass.getSimpleName();
        c._genericParameterTypes = JavaType.EMPTY_TYPES_ARRAY;
        c._evalType = javaClass;
        c._typeVars = new JavaTypeVariable[0];
        return c;
    }
}

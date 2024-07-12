/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
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
        JavaClass[] parameterClasses = getParameterClasses();
        JavaConstructor superMethod = superClass.getConstructorForClasses(parameterClasses);
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
     * Creates the id: ClassName(param, param, ...)
     */
    @Override
    protected String createId()
    {
        String classId = _declaringClass.getId();
        JavaClass[] paramClasses = getParameterClasses();
        String paramClassesStr = ArrayUtils.mapToStringsAndJoin(paramClasses, JavaClass::getId, ",");
        return classId + '(' + paramClassesStr + ')';
    }

    /**
     * Creates a default constructor for given class.
     */
    public static JavaConstructor createDefaultConstructor(JavaClass javaClass)
    {
        JavaConstructor c = new JavaConstructor(javaClass._resolver, javaClass, null);
        c._mods = Modifier.PUBLIC;
        c._name = c._simpleName = javaClass.getSimpleName();
        c._genericParameterTypes = JavaType.EMPTY_TYPES_ARRAY;
        c._parameterTypes = new JavaClass[0];
        c._evalType = javaClass;
        c._typeParameters = new JavaTypeVariable[0];
        return c;
    }
}

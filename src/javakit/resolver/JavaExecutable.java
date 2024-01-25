/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import java.lang.reflect.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaExecutable extends JavaMember {

    // The JavaDecls for TypeVars for Method/Constructor
    protected JavaTypeVariable[]  _typeVars;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[] _parameterTypes;

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaExecutable(Resolver aResolver, DeclType aType, JavaClass aDeclaringClass, Executable anExecutable)
    {
        super(aResolver, aType, aDeclaringClass, anExecutable);
        if (anExecutable == null)
            return;

        // Get VarArgs
        _varArgs = anExecutable.isVarArgs();

        // Get TypeVariables
        TypeVariable<?>[] typeVars = anExecutable.getTypeParameters();
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaExecutable getSuper()  { return null; }

    /**
     * Returns the TypeVars.
     */
    public JavaTypeVariable[] getTypeVars()  { return _typeVars; }

    /**
     * Returns the TypeVar with given name.
     */
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        return ArrayUtils.findMatch(_typeVars, tvar -> tvar.getName().equals(aName));
    }

    /**
     * Returns the number of Method/ParamType parameters.
     */
    public int getParameterCount()
    {
        JavaType[] paramTypes = getParameterTypes();
        return paramTypes.length;
    }

    /**
     * Returns the individual Method parameter type at index.
     */
    public JavaType getParameterType(int anIndex)
    {
        JavaType[] paramTypes = getParameterTypes();
        return paramTypes[anIndex];
    }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParameterTypes()
    {
        if (_parameterTypes != null) return _parameterTypes;

        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Executable exec = this instanceof JavaMethod ? ((JavaMethod) this).getMethod() : ((JavaConstructor) this).getConstructor();
        Type[] paramTypesReal = exec.getGenericParameterTypes();
        if (paramTypesReal.length < exec.getParameterCount())
            paramTypesReal = exec.getParameterTypes();

        JavaType[] parameterTypes = _resolver.getJavaTypesForTypes(paramTypesReal);

        // Set and return
        return _parameterTypes = parameterTypes;
    }

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    public boolean isVarArgs()  { return _varArgs; }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    @Override
    public boolean matches(JavaDecl aDecl)
    {
        // Check identity
        if (aDecl == this) return true;

        // If Types don't match, just return
        if (aDecl.getClass() != getClass())
            return false;

        // For Method, Constructor: Check supers
        JavaExecutable otherExec = (JavaExecutable) aDecl;
        for (JavaExecutable superExec = otherExec.getSuper(); superExec != null; superExec = superExec.getSuper())
            if (superExec == this)
                return true;

        // Return no match
        return false;
    }

    /**
     * Returns the parameter types string.
     */
    protected String getParametersString(boolean simpleNames)
    {
        Function<JavaDecl,String> namesFunction = simpleNames ? JavaDecl::getSimpleName : JavaDecl::getName;
        String paramTypesString = Stream.of(_parameterTypes).map(namesFunction).collect(Collectors.joining(","));
        return '(' + paramTypesString + ')';
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    public static int getMatchRatingForArgClasses(JavaExecutable aMethod, JavaClass[] argClasses)
    {
        // Handle VarArg methods special
        if (aMethod.isVarArgs())
            return getMatchRatingForArgClassesWithVarArgs(aMethod, argClasses);

        // Get method param types and count (just return if given arg count doesn't match)
        JavaType[] paramTypes = aMethod.getParameterTypes();
        int paramCount = paramTypes.length;
        if (argClasses.length != paramCount)
            return 0;
        if (paramCount == 0)
            return 1000;

        // Get and return rating for args
        return getMatchRatingForParamTypesAndArgClasses(paramTypes, argClasses, paramCount);
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    private static int getMatchRatingForParamTypesAndArgClasses(JavaType[] paramTypes, JavaClass[] argClasses, int aCount)
    {
        int rating = 0;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0; i < aCount; i++) {
            JavaClass paramClass = paramTypes[i].getEvalClass();
            JavaClass argClass = argClasses[i];
            if (!paramClass.isAssignableFrom(argClass))
                return 0;
            rating += paramClass == argClass ? 1000 : argClass != null ? 100 : 10;
        }

        // Return rating
        return rating;
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    private static int getMatchRatingForArgClassesWithVarArgs(JavaExecutable aMethod, JavaClass[] argClasses)
    {
        // Get method param types and count (just return if given arg count is insufficient)
        JavaType[] paramTypes = aMethod.getParameterTypes();
        int paramCount = paramTypes.length;
        int varArgIndex = paramCount - 1;
        if (argClasses.length < varArgIndex)
            return 0;
        if (paramCount == 1 && argClasses.length == 0)
            return 10;

        // Get rating for args prior to var arg index
        int rating = getMatchRatingForParamTypesAndArgClasses(paramTypes, argClasses, varArgIndex);

        // Get VarArg type
        JavaType varArgArrayType = paramTypes[varArgIndex];
        JavaClass varArgArrayClass = varArgArrayType.getEvalClass();
        JavaClass varArgClass = varArgArrayClass.getComponentType();

        // If only one arg and it is of array type, return rating plus 1000
        if (argClasses.length == paramCount) {
            JavaClass argClass = argClasses[varArgIndex];
            if (argClass != null && argClass.isArray() && varArgArrayClass.isAssignableFrom(argClass))
                return rating + 1000;
        }

        // If any var args doesn't match, return 0
        for (int i = varArgIndex; i < argClasses.length; i++) {
            JavaClass argClass = argClasses[i];
            if (!varArgClass.isAssignableFrom(argClass))
                return 0;
        }

        // Return rating plus 1000
        return rating + 1000;
    }
}

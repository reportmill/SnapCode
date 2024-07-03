/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import java.lang.reflect.*;
import java.util.function.Function;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaExecutable extends JavaMember {

    // The reader that provides properties
    protected ExecutableReader _execReader;

    // The JavaDecls for TypeVars for Method/Constructor
    protected JavaTypeVariable[]  _typeVars;

    // The JavaTypes for parameter types for Constructor, Method
    protected JavaType[] _genericParameterTypes;

    // The JavaClasses for parameter types for Constructor, Method
    protected JavaClass[] _parameterTypes;

    // The parameter names
    private String[] _parameterNames;

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaExecutable(Resolver aResolver, DeclType aType, JavaClass aDeclaringClass, Executable anExecutable)
    {
        super(aResolver, aType, aDeclaringClass);
        if (anExecutable == null)
            return;

        // Create and set reader
        setReader(new ExecutableReaderImpl(anExecutable));
    }

    /**
     * Sets the reader.
     */
    protected void setReader(ExecutableReader executableReader)
    {
        _execReader = executableReader;
        _execReader.setJavaExecutable(this);
        _name = _execReader.getName();
        _simpleName = _execReader.getSimpleName();
        _mods = _execReader.getModifiers();
        _varArgs = _execReader.isVarArgs();
    }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaExecutable getSuper()  { return null; }

    /**
     * Returns the TypeVars.
     */
    public JavaTypeVariable[] getTypeVars()
    {
        if (_typeVars != null) return _typeVars;
        return _typeVars = _execReader.getTypeVars();
    }

    /**
     * Returns the TypeVar with given name.
     */
    public JavaTypeVariable getTypeVarForName(String aName)
    {
        JavaTypeVariable[] typeVars = getTypeVars();
        return ArrayUtils.findMatch(typeVars, tvar -> tvar.getName().equals(aName));
    }

    /**
     * Returns the number of Method/ParamType parameters.
     */
    public int getParameterCount()
    {
        JavaType[] paramTypes = getGenericParameterTypes();
        return paramTypes.length;
    }

    /**
     * Returns the individual Method parameter type at index.
     */
    public JavaType getGenericParameterType(int anIndex)
    {
        JavaType[] paramTypes = getGenericParameterTypes();
        return paramTypes[anIndex];
    }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getGenericParameterTypes()
    {
        if (_genericParameterTypes != null) return _genericParameterTypes;
        return _genericParameterTypes = _execReader.getGenericParameterTypes();
    }

    /**
     * Returns the parameter types.
     */
    public JavaClass[] getParameterClasses()
    {
        if (_parameterTypes != null) return _parameterTypes;
        return _parameterTypes = _execReader.getParameterClasses();
    }

    /**
     * Returns the parameter names.
     */
    public String[] getParameterNames()
    {
        if (_parameterNames != null) return _parameterNames;
        return _parameterNames = _execReader.getParameterNames();
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
    protected String getParametersString()
    {
        JavaType[] paramTypes = getGenericParameterTypes();
        String[] paramNames = getParameterNames();
        String[] paramStrings = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
            paramStrings[i] = paramTypes[i].getSimpleName() + ' ' + paramNames[i];
        String paramsString = StringUtils.join(paramStrings, ",");
        return '(' + paramsString + ')';
    }

    /**
     * Returns the parameter types string.
     */
    protected String getParameterTypesString(boolean simpleNames)
    {
        JavaType[] paramTypes = getGenericParameterTypes();
        Function<JavaDecl,String> namesFunction = simpleNames ? JavaDecl::getSimpleName : JavaDecl::getName;
        String paramTypesString = ArrayUtils.mapToStringsAndJoin(paramTypes, namesFunction, ",");
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
        JavaType[] paramTypes = aMethod.getGenericParameterTypes();
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

            // Get param class and arg class - if arg
            JavaClass paramClass = paramTypes[i].getEvalClass();
            JavaClass argClass = argClasses[i];

            // If null arg class, add 10 (1 if primitive)
            if (argClass == null)
                rating += !paramClass.isPrimitive() ? 10 : 1;

            // Otherwise, if not assignable, just return 0
            else if (!paramClass.isAssignableFrom(argClass))
                return 0;

            // Otherwise add 1000 for exact match, or 100 for subclass
            else rating += paramClass == argClass ? 1000 : 100;
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
        JavaType[] paramTypes = aMethod.getGenericParameterTypes();
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

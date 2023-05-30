/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.StringUtils;
import java.lang.reflect.*;

/**
 * This class represents a Java Method or Constructor.
 */
public class JavaExecutable extends JavaMember {

    // The JavaDecls for TypeVars for Method/Constructor
    protected JavaTypeVariable[]  _typeVars;

    // The JavaDecls for parameter types for Constructor, Method
    protected JavaType[]  _paramTypes;

    // Whether method has VarArgs
    protected boolean  _varArgs;

    /**
     * Constructor.
     */
    public JavaExecutable(Resolver aResolver, DeclType aType, JavaClass aDeclaringClass, Member aMember)
    {
        super(aResolver, aType, aDeclaringClass, aMember);
        if (aMember == null) return;

        // Get VarArgs
        _varArgs = isVarArgs(aMember);

        // Get TypeVariables
        TypeVariable<?>[] typeVars = aResolver.getTypeParametersForExecutable(aMember);
        _typeVars = new JavaTypeVariable[typeVars.length];
        for (int i = 0, iMax = typeVars.length; i < iMax; i++)
            _typeVars[i] = new JavaTypeVariable(_resolver, this, typeVars[i]);
    }

    /**
     * Resolves types.
     */
    protected void initTypes(Member aMember)
    {
        // Get ParameterTypes
        Type[] paramTypes = _resolver.getGenericParameterTypesForExecutable(aMember);
        _paramTypes = _resolver.getJavaTypesForTypes(paramTypes);
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
        // Check Method, Constructor TypeVars
        for (JavaTypeVariable typeVar : _typeVars)
            if (typeVar.getName().equals(aName))
                return typeVar;

        // Forward to class
        JavaClass declaringClass = getDeclaringClass();
        return declaringClass.getTypeVarForName(aName);
    }

    /**
     * Returns the number of Method/ParamType parameters.
     */
    public int getParamCount()
    {
        return _paramTypes.length;
    }

    /**
     * Returns the individual Method parameter type at index.
     */
    public JavaType getParamType(int anIndex)
    {
        return _paramTypes[anIndex];
    }

    /**
     * Returns the parameter types.
     */
    public JavaType[] getParamTypes()  { return _paramTypes; }

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    public boolean isVarArgs()
    {
        return _varArgs;
    }

    /**
     * Returns the parameter type names.
     */
    public String[] getParamTypeNames()
    {
        String[] names = new String[_paramTypes.length];
        for (int i = 0; i < names.length; i++) names[i] = _paramTypes[i].getName();
        return names;
    }

    /**
     * Returns the parameter type simple names.
     */
    public String[] getParamTypeSimpleNames()
    {
        String[] names = new String[_paramTypes.length];
        for (int i = 0; i < names.length; i++) names[i] = _paramTypes[i].getSimpleName();
        return names;
    }

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
        JavaExecutable other = (JavaExecutable) aDecl;
        for (JavaExecutable sup = other.getSuper(); sup != null; sup = other.getSuper())
            if (sup == this)
                return true;

        // Return false, since no match
        return false;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get SimpleName,
        String simpleName = getSimpleName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");

        // Construct string SimpleName(ParamType.SimpleName, ...)
        return simpleName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    @Override
    public String getReplaceString()
    {
        String name = getSimpleName();
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");
        return name + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    @Override
    public String getPrettyName()
    {
        // Get full MemberName for Constructor or Method
        String className = getDeclaringClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();

        // Get simple parameter names
        String[] paramTypeNames = getParamTypeSimpleNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");

        // Return ClassName(param1, ...) or ClassName.MethodName(param1, ...)
        return memberName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a name unique for matching declarations.
     */
    @Override
    public String getMatchName()
    {
        // Get full MemberName for Constructor or Method
        String className = getDeclaringClassName();
        String memberName = className;
        if (this instanceof JavaMethod)
            memberName = className + '.' + getName();

        // Get parameter names
        String[] paramTypeNames = getParamTypeNames();
        String paramTypeNamesStr = StringUtils.join(paramTypeNames, ",");

        // Return ClassName(param1, ...) or ClassName.MethodName(param1, ...)
        return memberName + '(' + paramTypeNamesStr + ')';
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    public static int getMatchRatingForTypes(JavaExecutable aMethod, JavaType[] theTypes)
    {
        // Handle VarArg methods special
        if (aMethod.isVarArgs())
            return getMatchRatingForTypesWidthVarArgs(aMethod, theTypes);

        // Get method param types and length (just return if given arg count doesn't match)
        JavaType[] paramTypes = aMethod.getParamTypes();
        int plen = paramTypes.length, rating = 0;
        if (theTypes.length != plen)
            return 0;
        if (plen == 0)
            return 1000;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0, iMax = plen; i < iMax; i++) {
            JavaType cls1 = paramTypes[i].getEvalClass();
            JavaType cls2 = theTypes[i];
            if (cls2 != null)
                cls2 = cls2.getEvalClass();
            if (!cls1.isAssignable(cls2))
                return 0;
            rating += cls1 == cls2 ? 1000 : cls2 != null ? 100 : 10;
        }

        // Return rating
        return rating;
    }

    /**
     * Returns a rating of a method for given possible arg classes.
     */
    private static int getMatchRatingForTypesWidthVarArgs(JavaExecutable aMethod, JavaType[] theTypes)
    {
        // Get method param types and length (just return if given arg count is insufficient)
        JavaType[] paramTypes = aMethod.getParamTypes();
        int argsLen = paramTypes.length;
        int varArgIndex = argsLen - 1;
        int rating = 0;
        if (theTypes.length < varArgIndex)
            return 0;
        if (argsLen == 1 && theTypes.length == 0)
            return 10;

        // Iterate over classes and add score based on matching classes
        // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
        for (int i = 0, iMax = varArgIndex; i < iMax; i++) {
            JavaType cls1 = paramTypes[i].getEvalClass();
            JavaType cls2 = theTypes[i];
            if (cls2 != null) cls2 = cls2.getEvalClass();
            if (!cls1.isAssignable(cls2))
                return 0;
            rating += cls1 == cls2 ? 1000 : cls2 != null ? 100 : 10;
        }

        // Get VarArg type
        JavaType varArgArrayType = paramTypes[varArgIndex];
        JavaType varArgType = varArgArrayType.getComponentType();

        // If only one arg and it is of array type, add 1000
        JavaType argType = theTypes.length == argsLen ? theTypes[varArgIndex] : null;
        if (argType != null && argType.isArray() && varArgArrayType.isAssignable(argType))
            rating += 1000;

            // If any var args match, add 1000
        else for (int i = varArgIndex; i < theTypes.length; i++) {
            JavaType type = theTypes[i];
            if (varArgType.isAssignable(type))
                rating += 1000;
        }

        // Return rating
        return rating;
    }

    /**
     * Returns whether is VarArgs.
     */
    private static boolean isVarArgs(Member aMember)
    {
        if (aMember instanceof Method)
            return ((Method) aMember).isVarArgs();
        return ((Constructor<?>) aMember).isVarArgs();
    }
}

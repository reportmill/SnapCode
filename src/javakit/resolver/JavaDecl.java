/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import snap.util.StringUtils;
import java.lang.reflect.Modifier;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {

    // The Resolver that produced this decl
    protected Resolver  _resolver;

    // A unique identifier
    protected String  _id;

    // The type
    protected DeclType  _type;

    // The name of this declaration
    protected String  _name;

    // The simple name of this declaration
    protected String  _simpleName;

    // The full name of this declaration
    private String  _fullName;

    // The type this decl evaluates to when referenced
    protected JavaType  _evalType;

    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar, GenArrayType, Word }

    /**
     * Constructor.
     */
    protected JavaDecl(Resolver aResolver, DeclType aType)
    {
        // Set Resolver, Type
        _resolver = aResolver;
        _type = aType; assert (aType != null);
    }

    /**
     * Returns the id.
     */
    public String getId()
    {
        if (_id != null) return _id;
        return _id = createId();
    }

    /**
     * Creates the id.
     */
    protected String createId()  { return null; }

    /**
     * Returns the type.
     */
    public DeclType getType()  { return _type; }

    /**
     * Returns the name.
     */
    public String getName()  { return _name; }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()  { return _simpleName; }

    /**
     * Returns the full name.
     */
    public String getFullName()
    {
        // If already set, just return
        if (_fullName != null) return _fullName;

        // Get full name
        String fullName = _name;
        if (this instanceof JavaMethod || this instanceof JavaField) {
            JavaMember member = (JavaMember) this;
            String className = member.getDeclaringClassName();
            fullName = className + '.' + fullName;
        }

        // Set and return
        return _fullName = fullName;
    }

    /**
     * Returns the JavaType this decl evaluates to when referenced.
     */
    public JavaType getEvalType()  { return _evalType; }

    /**
     * Returns the type of the most basic class associated with this type.
     */
    public JavaClass getEvalClass()
    {
        JavaType evalType = getEvalType();
        return evalType != null ? evalType.getEvalClass() : null;
    }

    /**
     * Returns the type name for class this decl evaluates to when referenced.
     */
    public String getEvalClassName()
    {
        JavaClass evalClass = getEvalClass();
        return evalClass != null ? evalClass.getName() : null;
    }

    /**
     * Returns the full name, with parameter type names appended if executable.
     */
    public String getSimpleNameWithParameterTypes()
    {
        String simpleName = getSimpleName();

        // If Executable, add parameter types string
        if (this instanceof JavaExecutable)
            simpleName += ((JavaExecutable) this).getParameterTypesString(true);

        // Return
        return simpleName;
    }

    /**
     * Returns the full name, with parameter type names appended if executable.
     */
    public String getFullNameWithParameterTypes()
    {
        String fullName = getFullName();

        // If Executable, add parameter types string
        if (this instanceof JavaExecutable)
            fullName += ((JavaExecutable) this).getParameterTypesString(false);

        // Return
        return fullName;
    }

    /**
     * Returns the full name, with parameter type simple-names appended if executable.
     */
    public String getFullNameWithSimpleParameterTypes()
    {
        String fullName = getFullName();

        // If Executable, add parameter types string
        if (this instanceof JavaExecutable)
            fullName += ((JavaExecutable) this).getParameterTypesString(true);

        // Return
        return fullName;
    }

    /**
     * Returns the full declaration string, including modifiers and return type (method/field).
     */
    public String getDeclarationString()
    {
        String declString = getSimpleName();
        if (this instanceof JavaExecutable)
            declString += ((JavaExecutable) this).getParametersString();

        // If method or field, prefix return type
        if (this instanceof JavaMethod || this instanceof JavaField) {
            JavaType returnType = getEvalType();
            String returnTypeName = returnType.getSimpleName();
            declString = returnTypeName + " " + declString;
        }

        // If Class or Member, prefix mod string
        if (this instanceof JavaClass || this instanceof JavaMember) {
            int mods = this instanceof JavaClass ? ((JavaClass) this).getModifiers() : ((JavaMember) this).getModifiers();
            String modifierStr = Modifier.toString(mods);
            if (!modifierStr.isEmpty())
                declString = modifierStr + " " + declString;
        }

        // Return
        return declString;
    }

    /**
     * Returns a string representation of suggestion.
     */
    public String getSuggestionString()
    {
        return getSimpleNameWithParameterTypes();
    }

    /**
     * Returns the string to use when inserting this suggestion into code.
     */
    public String getReplaceString()
    {
        return getSimpleNameWithParameterTypes();
    }

    /**
     * Returns a JavaDecl for given object.
     */
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        return _resolver.getJavaClassForClass(aClass);
    }

    /**
     * Returns whether given declaration collides with this declaration.
     */
    public boolean matches(JavaDecl aDecl)
    {
        return aDecl == this;
    }

    /**
     * Standard compareTo implementation.
     */
    public int compareTo(JavaDecl aDecl)
    {
        // Compare type order
        int typeOrder1 = getType().ordinal();
        int typeOrder2 = aDecl.getType().ordinal();
        if (typeOrder1 != typeOrder2)
            return typeOrder1 - typeOrder2;

        // Compare full names
        String fullName1 = getFullNameWithParameterTypes();
        String fullName2 = aDecl.getFullNameWithParameterTypes();
        return fullName1.compareTo(fullName2);
    }

    /**
     * Standard equals implementation.
     */
    @Override
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        JavaDecl other = anObj instanceof JavaDecl ? (JavaDecl) anObj : null;
        if (other == null) return false;
        String id = getId();
        String otherId = other.getId();
        return id.equals(otherId);
    }

    /**
     * Standard hashcode implementation.
     */
    public int hashCode()
    {
        String id = getId();
        if (id == null)
            return System.identityHashCode(this);
        return id.hashCode();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb,"Id", getId());
        return sb.toString();
    }
}
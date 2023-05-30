/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * This class represents a JavaClass Field.
 */
public class JavaField extends JavaMember {

    /**
     * Constructor.
     */
    public JavaField(Resolver aResolver, JavaClass aDeclaringClass, Field aField)
    {
        super(aResolver, DeclType.Field, aDeclaringClass, aField);
        if (aField == null) return;

        // Set EvalType
        Type fieldType = aResolver.getGenericTypeForField(aField);
        _evalType = _resolver.getJavaTypeForType(fieldType);
    }

    /**
     * Returns whether field is enum constant.
     */
    public boolean isEnumConstant()
    {
        JavaClass fieldClass = getDeclaringClass();
        return fieldClass != null && fieldClass.isEnum();
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get SimpleName, EvalType.SimpleName and DeclaringClass.SimpleName
        String simpleName = getSimpleName();
        JavaType evalType = getEvalType();
        String evalTypeName = evalType != null ? evalType.getSimpleName() : null;
        JavaClass declaringClass = getDeclaringClass();
        String declaringClassSimpleName = declaringClass.getSimpleName();

        // Construct string: SimpleName : EvalType.SimpleName - DeclaringCLass.SimpleName
        StringBuffer sb = new StringBuffer(simpleName);
        if (evalTypeName != null)
            sb.append(" : ").append(evalTypeName);
        sb.append(" - ").append(declaringClassSimpleName);

        // Return
        return sb.toString();
    }

    /**
     * Returns a name suitable to describe declaration.
     */
    @Override
    public String getPrettyName()
    {
        String className = getDeclaringClassName();
        String fieldName = getName();
        return className + '.' + fieldName;
    }

    /**
     * Returns a name unique for matching declarations.
     */
    @Override
    public String getMatchName()
    {
        String className = getDeclaringClassName();
        String fieldName = getName();
        return className + '.' + fieldName;
    }

    /**
     * A Builder class for JavaField.
     */
    public static class FieldBuilder {

        // Ivars
        Resolver  _resolver;
        JavaClass  _declaringClass;
        int  _mods =  Modifier.PUBLIC;
        String  _name;
        JavaType  _type;

        // For build all
        private JavaField[]  _fields = new JavaField[20];
        private int  _fieldCount;

        /**
         * Constructor.
         */
        public FieldBuilder()  { }

        /**
         * Init.
         */
        public void init(Resolver aResolver, String aClassName)
        {
            _resolver = aResolver;
            _declaringClass = aResolver.getJavaClassForName(aClassName);
        }

        // Properties.
        public FieldBuilder mods(int mods)  { _mods = mods; return this; }
        public FieldBuilder name(String name)  { _name = name; return this; }
        public FieldBuilder type(JavaType type)  { _type = type; return this; }
        public FieldBuilder type(Type type)  { _type = _resolver.getJavaTypeForType(type); return this; }

        /**
         * Build.
         */
        public JavaField build()
        {
            JavaField f = new JavaField(_resolver, _declaringClass, null);
            f._mods = _mods;
            f._id = _declaringClass.getId() + '.' + _name;
            f._name = f._simpleName = _name;
            f._declaringClass = _declaringClass;
            f._evalType = _type;
            _mods = Modifier.PUBLIC;
            _name = null;
            return f;
        }

        /**
         * Builds current field and saves it in array for buildAll.
         */
        public FieldBuilder save()
        {
            _fields[_fieldCount++] = build(); return this;
        }

        /**
         * Returns an array of all currently saved fields.
         */
        public JavaField[] buildAll()
        {
            if (_name != null) save();
            JavaField[] fields = Arrays.copyOf(_fields, _fieldCount);
            _fieldCount = 0;
            return fields;
        }
    }
}

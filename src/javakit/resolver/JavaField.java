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

    // The Java Field
    private Field _field;

    /**
     * Constructor.
     */
    public JavaField(Resolver aResolver, JavaClass aDeclaringClass, Field aField)
    {
        super(aResolver, DeclType.Field, aDeclaringClass, aField);
        if (aField == null)
            return;

        // Set field
        _field = aField;
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
     * Override to get eval type dynamically.
     */
    @Override
    public JavaType getEvalType()
    {
        if (_evalType != null) return _evalType;
        if (_field == null) { System.out.println("JavaField.getEvalType: Missing java.lang.Field for: " + getId()); return null; }
        Type fieldType = _field.getGenericType();
        JavaType evalType = _resolver.getJavaTypeForType(fieldType);
        return _evalType = evalType;
    }

    /**
     * Evaluates field for given object.
     */
    public Object get(Object anObj) throws IllegalArgumentException, IllegalAccessException
    {
        return _field.get(anObj);
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        String simpleName = super.getSuggestionString();

        // Get SimpleName, EvalType.SimpleName and DeclaringClass.SimpleName
        JavaType evalType = getEvalType();
        String evalTypeName = evalType != null ? evalType.getSimpleName() : null;

        // Construct string: SimpleName : EvalType.SimpleName
        StringBuilder sb = new StringBuilder(simpleName);
        if (evalTypeName != null)
            sb.append(" - ").append(evalTypeName);

        // Return
        return sb.toString();
    }

    /**
     * Merges the given new field into this field.
     */
    public boolean mergeField(JavaField newField)
    {
        boolean didChange = false;

        // Update modifiers
        if (newField.getModifiers() != getModifiers()) {
            _mods = newField.getModifiers();
            didChange = true;
        }

        // Update return type
        if (newField.getEvalType() != getEvalType() && newField.getEvalType() != null) {
            _evalType = newField.getEvalType();
            didChange = true;
        }

        // Update Field
        if (newField._field != null)
            _field = newField._field;

        // Return
        return didChange;
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

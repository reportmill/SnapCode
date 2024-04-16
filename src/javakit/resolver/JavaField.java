/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;

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
     * Creates a field for given name and type.
     */
    public static JavaField createField(JavaClass javaClass, String fieldName, Class<?> fieldType)
    {
        JavaField f = new JavaField(javaClass._resolver, javaClass, null);
        f._mods = Modifier.PUBLIC;
        f._id = javaClass.getId() + '.' + fieldName;
        f._name = f._simpleName = fieldName;
        f._declaringClass = javaClass;
        f._evalType = javaClass._resolver.getJavaClassForClass(fieldType);
        return f;
    }

    /**
     * Creates a field for given name and type.
     */
    public static JavaField createField(JavaClass javaClass, String fieldName, JavaType fieldType, int mods)
    {
        JavaField f = new JavaField(javaClass._resolver, javaClass, null);
        f._mods = mods;
        f._id = javaClass.getId() + '.' + fieldName;
        f._name = f._simpleName = fieldName;
        f._declaringClass = javaClass;
        f._evalType = fieldType;
        return f;
    }
}

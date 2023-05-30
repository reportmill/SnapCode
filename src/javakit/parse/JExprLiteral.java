/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;

/**
 * A JExpr subclass for literals.
 */
public class JExprLiteral extends JExpr {

    // The string used to represent literal
    String _valueStr;

    // The literal value
    Object _value;

    // The literal type
    LiteralType _literalType = LiteralType.Null;

    // Constants for Literal type
    public enum LiteralType {Boolean, Integer, Long, Float, Double, Character, String, Null}

    ;

    /**
     * Creates a new literal.
     */
    public JExprLiteral()
    {
    }

    /**
     * Creates a new literal with given value.
     */
    public JExprLiteral(Object aValue)
    {
        _value = aValue;
        if (aValue instanceof Boolean) _literalType = LiteralType.Boolean;
        else if (aValue instanceof Integer) _literalType = LiteralType.Integer;
        else if (aValue instanceof Long) _literalType = LiteralType.Long;
        else if (aValue instanceof Float) _literalType = LiteralType.Float;
        else if (aValue instanceof Double) _literalType = LiteralType.Double;
        else if (aValue instanceof Character) _literalType = LiteralType.Character;
        else if (aValue instanceof String) _literalType = LiteralType.String;
        else if (aValue == null) _literalType = LiteralType.Null;
    }

    /**
     * Returns the Literal type (String, Number, Boolean, Null).
     */
    public LiteralType getLiteralType()
    {
        return _literalType;
    }

    /**
     * Sets the literal type.
     */
    public void setLiteralType(LiteralType aType)
    {
        _literalType = aType;
    }

    /**
     * Returns whether this is null literal.
     */
    public boolean isNull()
    {
        return getLiteralType() == LiteralType.Null;
    }

    /**
     * Returns the value.
     */
    public Object getValue()
    {
        // If value already set or null, return
        if (_value != null || isNull()) return _value;

        // Get literal type and string info
        String s = getValueString();
        int len = s.length();
        char c = s.charAt(len - 1);

        // Decode type from string
        switch (getLiteralType()) {
            case Boolean:
                _value = Boolean.valueOf(s);
                break;
            case Integer:
                try {
                    _value = Integer.decode(s);
                } catch (Exception e) {
                }
                break;
            case Long:
                try {
                    _value = Long.decode(s.substring(0, len - 1));
                } catch (Exception e) {
                }
                break;
            case Float:
                _value = Float.valueOf(s.substring(0, len - 1));
                break;
            case Double:
                _value = c == 'd' || c == 'D' ? Double.valueOf(s.substring(0, len - 1)) : Double.valueOf(s);
                break;
            case Character:
                _value = s.charAt(1);
                break;
            case String:
                _value = s.substring(1, s.length() - 1);
                break;
            default:
                return null;
        }
        return _value;
    }

    /**
     * Returns the value string.
     */
    public String getValueString()
    {
        // If value string already set, just return
        if (_valueStr != null) return _valueStr;

        // Create value string from value
        if (_value == null) _valueStr = "null";
        else if (_value instanceof Boolean) _valueStr = _value.toString();
        else if (_value instanceof Character || _value instanceof String) _valueStr = _value.toString();
        else if (_value instanceof Byte || _value instanceof Short) _valueStr = _value.toString();
        else if (_value instanceof Integer) _valueStr = _value.toString();
        else if (_value instanceof Long) _valueStr = _value.toString() + 'L';
        else if (_value instanceof Float) {
            float val = (Float) _value;
            if (val == (int) val) _valueStr = Integer.toString((int) val) + 'f';
            else _valueStr = _value.toString() + 'f';
        } else if (_value instanceof Double) {
            double val = (Double) _value;
            if (val == (long) val) _valueStr = Long.toString((int) val) + 'd';
            else _valueStr = _value.toString() + 'd';
        }

        // Return value string
        return _valueStr;
    }

    /**
     * Returns the value class.
     */
    public Class<?> getValueClass()
    {
        switch (getLiteralType()) {
            case Boolean: return boolean.class;
            case Integer: return int.class;
            case Long: return long.class;
            case Float: return float.class;
            case Double: return double.class;
            case Character: return char.class;
            case String: return String.class;
            default: return null;
        }
    }

    /**
     * Sets the value string.
     */
    public void setValueString(String aString)
    {
        _valueStr = aString;
    }

    /**
     * Tries to resolve the class declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get value class (just return if null)
        Class<?> valueClass = getValueClass();
        if (valueClass == null)
            return null;

        JavaClass javaClass = getJavaClassForClass(valueClass);
        JavaClass declPrim = javaClass.getPrimitive();
        if (declPrim != null)
            javaClass = declPrim;

        // Return
        return javaClass;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        switch (getLiteralType()) {
            case Boolean: return "Boolean";
            case Integer: return "Integer";
            case Long: return "Long";
            case Float: return "Float";
            case Double: return "Double";
            case Character: return "Character";
            case String: return "String";
            case Null: return "Null";
            default: throw new RuntimeException("JLiteral unknown type: " + getLiteralType());
        }
    }

}
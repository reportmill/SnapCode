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
    protected String _valueStr;

    // The literal value
    private Object _value;

    // The literal type
    private LiteralType _literalType = LiteralType.Null;

    // Constants for Literal type
    public enum LiteralType { Boolean, Integer, Long, Float, Double, Character, String, Null }

    /**
     * Constructor.
     */
    public JExprLiteral()
    {
        super();
    }

    /**
     * Returns the Literal type (String, Number, Boolean, Null).
     */
    public LiteralType getLiteralType()  { return _literalType; }

    /**
     * Sets the literal type.
     */
    public void setLiteralType(LiteralType aType)  { _literalType = aType; }

    /**
     * Returns whether this is null literal.
     */
    public boolean isNull()  { return getLiteralType() == LiteralType.Null; }

    /**
     * Returns the value.
     */
    public Object getValue()
    {
        // If value already set or null, return
        if (_value != null || isNull()) return _value;

        // Get literal type and string info
        String valueStr = getValueString();
        int len = valueStr.length();
        char lastChar = valueStr.charAt(len - 1);

        // Decode type from string
        switch (getLiteralType()) {

            // Boolean
            case Boolean: _value = Boolean.valueOf(valueStr); break;

            // Integer
            case Integer:
                try { _value = Integer.decode(valueStr); }
                catch (Exception ignore) { }
                break;

            // Long
            case Long:
                try { _value = Long.decode(valueStr.substring(0, len - 1)); }
                catch (Exception ignore) { }
                break;

            // Float
            case Float: _value = Float.valueOf(valueStr.substring(0, len - 1)); break;

            // Double
            case Double:
                _value = lastChar == 'd' || lastChar == 'D' ? Double.valueOf(valueStr.substring(0, len - 1)) : Double.valueOf(valueStr);
                break;

            // Char
            case Character: {
                String str = getStringForStringLiteral(valueStr);
                if (!str.isEmpty())
                    _value = str.charAt(0);
                break;
            }

            // String
            case String: _value = getStringForStringLiteral(valueStr); break;

            // Dunno
            default: return null;
        }

        // Return
        return _value;
    }

    /**
     * Returns the value string.
     */
    public String getValueString()
    {
        if (_valueStr != null) return _valueStr;
        return _valueStr = getValueStringImpl();
    }

    /**
     * Returns the value string.
     */
    private String getValueStringImpl()
    {
        // Create value string from value
        if (_value == null)
            return "null";
        if (_value instanceof Boolean)
            return _value.toString();
        if (_value instanceof Character || _value instanceof String)
            return _value.toString();
        if (_value instanceof Byte || _value instanceof Short)
            return _value.toString();
        if (_value instanceof Integer)
            return _value.toString();
        if (_value instanceof Long)
            return _value.toString() + 'L';
        if (_value instanceof Float) {
            float val = (Float) _value;
            if (val == (int) val)
                return Integer.toString((int) val) + 'f';
            return _value.toString() + 'f';
        }
        if (_value instanceof Double) {
            double val = (Double) _value;
            if (val == (long) val)
                return Long.toString((int) val) + 'd';
            return _value.toString() + 'd';
        }

        // Complain
        System.err.println("JLiteral.getValueStringImpl(): Unknown value type: " + _value.getClass().getName());
        return null;
    }

    /**
     * Returns the value class.
     */
    public Class<?> getValueClass()
    {
        return switch (getLiteralType()) {
            case Boolean -> boolean.class;
            case Integer -> int.class;
            case Long -> long.class;
            case Float -> float.class;
            case Double -> double.class;
            case Character -> char.class;
            case String -> String.class;
            case Null -> null;
        };
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
        return switch (getLiteralType()) {
            case Boolean -> "Boolean";
            case Integer -> "Integer";
            case Long -> "Long";
            case Float -> "Float";
            case Double -> "Double";
            case Character -> "Character";
            case String -> "String";
            case Null -> "Null";
        };
    }

    /**
     * Returns a string from a string literal string.
     */
    private static String getStringForStringLiteral(String stringLiteral)
    {
        // Handle text block string
        if (stringLiteral.startsWith("\"\"\"") && stringLiteral.length() >= 6) {
            String textBlock = stringLiteral.substring(3, stringLiteral.length() - 3);
            return textBlock.stripIndent();
        }

        String str = stringLiteral.substring(1, stringLiteral.length() - 1);
        str = str.replace("\\n", "\n");
        str = str.replace("\\t", "\t");
        str = str.replace("\\r", "\r");
        str = str.replace("\\\\", "\\");
        str = str.replace("\\\"", "\"");
        return str;
    }
}
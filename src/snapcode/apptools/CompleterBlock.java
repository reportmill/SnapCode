/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;

import java.lang.reflect.Method;

import javakit.parse.JExprId;
import javakit.parse.JNode;

/**
 * A class to represent a block of code in the CodeBuilder.
 */
public class CompleterBlock {

    // The JNode
    JNode _node;

    // The Method
    Method _method;

    // The code String
    String _string;

    /**
     * Initialize CodeBlock for given Node and Method.
     */
    public CompleterBlock init(JNode aNode, Method aMethod)
    {
        _node = aNode;
        _method = aMethod;
        return this;
    }

    /**
     * Returns the string for CodeBlock.
     */
    public String getString()
    {
        return _string != null ? _string : (_string = createString());
    }

    /**
     * Returns the string for CodeBlock.
     */
    protected String createString()
    {
        return getMethodString(_method);
    }

    /**
     * Returns the method string.
     */
    private String getMethodString(Method aMethod)
    {
        StringBuffer sb = new StringBuffer(aMethod.getName()).append('(');
        Class classes[] = aMethod.getParameterTypes();
        for (Class c : classes)
            sb.append(c.getSimpleName()).append(",");
        if (classes.length > 0) sb.delete(sb.length() - 1, sb.length());
        String string = sb.append(");").toString();
        if (_node instanceof JExprId) {
            JExprId id = (JExprId) _node;
            if (id.isVarId()) string = id.getName() + "." + string;
        }
        return string;
    }

    /**
     * Returns the string to use for replacing.
     */
    public String getReplaceString()
    {
        return getReplaceMethodString(_method);
    }

    /**
     * Returns the method string to use when replacing.
     */
    private String getReplaceMethodString(Method aMethod)
    {
        StringBuffer sb = new StringBuffer(aMethod.getName()).append('(');
        Class classes[] = aMethod.getParameterTypes();
        String argSep = "";
        for (Class c : classes) {
            String string = "null";
            if (c == boolean.class || c == Boolean.class) string = "true";
            else if (c == char.class || c == Character.class) string = "\'a\'";
            else if (c == byte.class || c == Byte.class || c == short.class || c == Short.class) string = "0";
            else if (c == int.class || c == Integer.class || c == long.class || c == Long.class) string = "0";
            else if (c == float.class || c == Float.class || c == double.class || c == Double.class) string = "0";
            else if (c == String.class) string = "\"String\"";
            sb.append(argSep).append(string);
            argSep = ", ";
        }
        String string = sb.append(");").toString();
        if (_node instanceof JExprId) {
            JExprId id = (JExprId) _node;
            if (id.isVarId()) string = id.getName() + "." + string;
        }
        return string;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + getString();
    }

}
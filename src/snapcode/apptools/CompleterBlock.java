/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import java.lang.reflect.Method;
import javakit.resolver.JavaClass;
import snap.util.ArrayUtils;

/**
 * A class to represent a block of code in the CodeBuilder.
 */
public class CompleterBlock {

    // The Method
    private Method _method;

    // The prefix
    private String _prefix = "";

    // The code String
    private String _string;

    /**
     * Constructor.
     */
    public CompleterBlock(Method aMethod, String prefix)
    {
        _method = aMethod;
        _prefix = prefix != null ? (prefix + '.') : "";
    }

    /**
     * Returns the string for CodeBlock.
     */
    public String getString()
    {
        if (_string != null) return _string;
        Class<?>[] parameterTypes = _method.getParameterTypes();
        String paramTypesStr = ArrayUtils.mapToStringsAndJoin(parameterTypes, Class::getSimpleName, ",");
        return _string = _prefix + _method.getName() + '(' + paramTypesStr + ");";
    }

    /**
     * Returns the string to use for replacing.
     */
    public String getReplaceString()
    {
        Class<?>[] parameterTypes = _method.getParameterTypes();
        String argsStr = ArrayUtils.mapToStringsAndJoin(parameterTypes, CompleterBlock::getArgStringForClass, ",");
        return _prefix + _method.getName() + '(' + argsStr + ");";
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + getString();
    }

    /**
     * Returns suggestions for class.
     */
    public static CompleterBlock[] getCodeBlocksForNode(JavaClass javaClass, String prefix)
    {
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        Method[] methods = realClass != null ? realClass.getMethods() : new Method[0];
        return ArrayUtils.mapNonNull(methods, method -> createCompleterBlockForMethod(method, prefix), CompleterBlock.class);
    }

    /**
     * Returns a completer block for given method.
     */
    private static CompleterBlock createCompleterBlockForMethod(Method method, String prefix)
    {
        if (method.getDeclaringClass() == Object.class)
            return null;
        return new CompleterBlock(method, prefix);
    }

    /**
     * Returns an arg string for given class.
     */
    private static String getArgStringForClass(Class<?> c)
    {
        if (c == boolean.class || c == Boolean.class) return "true";
        if (c == char.class || c == Character.class) return "\'a\'";
        if (c == byte.class || c == Byte.class || c == short.class || c == Short.class) return "0";
        if (c == int.class || c == Integer.class || c == long.class || c == Long.class) return "0";
        if (c == float.class || c == Float.class || c == double.class || c == Double.class) return "0";
        if (c == String.class) return "\"String\"";
        return "null";
    }
}
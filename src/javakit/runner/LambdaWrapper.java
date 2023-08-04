package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JExprLambda;
import javakit.parse.JVarDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import snap.util.Convert;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Wraps Lambda expression.
 */
public class LambdaWrapper {

    // The ExprEval
    private JSExprEval _exprEval;

    // The VarStack
    private JSVarStack _varStack;

    // The OR
    private Object _OR;

    // The Content expression
    private JExpr _contentExpr;

    // The params
    private JVarDecl _param0, _param1;

    /**
     * Constructor.
     */
    public LambdaWrapper(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr)
    {
        _exprEval = exprEval;
        _varStack = exprEval._varStack;
        _OR = anOR;
        _contentExpr = lambdaExpr.getExpr();

        List<JVarDecl> varDecls = lambdaExpr.getParameters();
        _param0 = varDecls.size() > 0 ? varDecls.get(0) : null;
        _param1 = varDecls.size() > 1 ? varDecls.get(1) : null;
    }

    public Object invokeObjectMethod()
    {
        _varStack.pushStackFrame();
        try { return _exprEval.evalExpr(_OR, _contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    public Object invokeObjectMethodWithObject(Object arg1)
    {
        _varStack.pushStackFrame();
        _varStack.setStackValueForNode(_param0, arg1);
        try { return _exprEval.evalExpr(_OR, _contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    public Object invokeObjectMethodWithObjectObject(Object arg1, Object arg2)
    {
        _varStack.pushStackFrame();
        _varStack.setStackValueForNode(_param0, arg1);
        _varStack.setStackValueForNode(_param1, arg2);
        try { return _exprEval.evalExpr(_OR, _contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    public void invokeVoidMethod()
    {
        invokeObjectMethod();
    }

    public void invokeVoidMethodWithObject(Object arg1)
    {
        invokeObjectMethodWithObject(arg1);
    }

    public void invokeVoidMethodWithObjectObject(Object arg1, Object arg2)
    {
        invokeObjectMethodWithObject(arg1);
    }

    public int invokeIntMethod()
    {
        Object value = invokeObjectMethod();
        return Convert.intValue(value);
    }

    public int invokeIntMethodWithObject(Object arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.intValue(value);
    }

    public int invokeIntMethodWithObjectObject(Object arg1, Object arg2)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.intValue(value);
    }

    public boolean invokeBooleanMethod()
    {
        Object value = invokeObjectMethod();
        return Convert.boolValue(value);
    }

    public boolean invokeBooleanMethodWithObject(Object arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.boolValue(value);
    }

    public boolean invokeBooleanMethodWithObjectObject(Object arg1, Object arg2)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.boolValue(value);
    }

    public float invokeFloatMethod()
    {
        Object value = invokeObjectMethod();
        return Convert.floatValue(value);
    }

    public float invokeFloatMethodWithObject(Object arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.floatValue(value);
    }

    public float invokeFloatMethodWithObjectObject(Object arg1, Object arg2)
    {
        Object value = invokeObjectMethodWithObjectObject(arg1, arg2);
        return Convert.floatValue(value);
    }

    public float invokeFloatMethodWithFloat(float arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.floatValue(value);
    }

    public float invokeFloatMethodWithFloatFloat(float arg1, float arg2)
    {
        Object value = invokeObjectMethodWithObjectObject(arg1, arg2);
        return Convert.floatValue(value);
    }

    public double invokeDoubleMethod()
    {
        Object value = invokeObjectMethod();
        return Convert.doubleValue(value);
    }

    public double invokeDoubleMethodWithObject(Object arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.doubleValue(value);
    }

    public double invokeDoubleMethodWithObjectObject(Object arg1, Object arg2)
    {
        Object value = invokeObjectMethodWithObjectObject(arg1, arg2);
        return Convert.doubleValue(value);
    }

    public double invokeDoubleMethodWithDouble(double arg1)
    {
        Object value = invokeObjectMethodWithObject(arg1);
        return Convert.doubleValue(value);
    }

    public double invokeDoubleMethodWithDoubleDouble(double arg1, double arg2)
    {
        Object value = invokeObjectMethodWithObjectObject(arg1, arg2);
        return Convert.doubleValue(value);
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr)
    {
        // Get lambda class
        JavaClass lambdaClass = lambdaExpr.getLambdaClass();
        if (lambdaClass == null)
            throw new RuntimeException("LambdaWrapper.getWrappedLambdaExpression: Can't determine lambda class for expr: " + lambdaExpr);

        try { return getWrappedLambdaExpressionImpl(exprEval, anOR, lambdaExpr, lambdaClass); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpressionImpl(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr, JavaClass lambdaClass) throws Throwable
    {
        // Get params and content expression
        Class<?> realClass = lambdaClass.getRealClass();

        // Get lambda method name
        JavaMethod lambdaJavaMethod = lambdaExpr.getLambdaMethod();
        Method lambdaMethod = lambdaJavaMethod.getMethod();
        String lambdaMethodName = lambdaMethod.getName();
        MethodType lambdaMethodType = MethodType.methodType(realClass, LambdaWrapper.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        LambdaWrapper lambdaWrapper = new LambdaWrapper(exprEval, anOR, lambdaExpr);

        // Get MethodType and LambdaWrapper method name
        Class<?> returnType = lambdaMethod.getReturnType();
        if (!returnType.isPrimitive())
            returnType = Object.class;
        Class<?>[] parameterTypes = lambdaMethod.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++)
            if (!parameterTypes[i].isPrimitive())
                parameterTypes[i] = Object.class;
        MethodType methodType = MethodType.methodType(returnType, parameterTypes);
        String lambdaWrapperMethodName = getInvokeMethodNameForMethodType(methodType);

        // Get MethodHandle for LambdaWrapper method for name/types
        MethodHandle methodHandle = lookup.findVirtual(LambdaWrapper.class, lambdaWrapperMethodName, methodType);

        // Get CallSite
        CallSite callSite = LambdaMetafactory.metafactory(lookup, lambdaMethodName, lambdaMethodType, methodType, methodHandle, methodType);

        // Bind call to LambdaWrapper and get a real lambda instance
        MethodHandle lambdaWrapped2 = callSite.getTarget();
        MethodHandle lambdaWrapped3 = lambdaWrapped2.bindTo(lambdaWrapper);
        return lambdaWrapped3.invoke();
    }

    /**
     * Returns the LambdaWrapper invoke method name for given MethodType.
     */
    private static String getInvokeMethodNameForMethodType(MethodType methodType)
    {
        Class<?> returnType = methodType.returnType();
        String prefix = "invokeObjectMethod";
        if (returnType.isPrimitive()) {
            if (returnType == void.class) prefix = "invokeVoidMethod";
            else if (returnType == boolean.class) prefix = "invokeBooleanMethod";
            else if (returnType == int.class) prefix = "invokeIntMethod";
            else if (returnType == long.class) prefix = "invokeLongMethod";
            else if (returnType == float.class) prefix = "invokeFloatMethod";
            else if (returnType == double.class) prefix = "invokeDoubleMethod";
        }

        // Get parameters
        Class<?>[] parameterTypes = methodType.parameterArray();
        if (parameterTypes.length == 0)
            return prefix;

        String suffix = "With";
        for (Class<?> paramType : parameterTypes) {
            String paramString = "Object";
            if (paramType.isPrimitive()) {
                if (paramType == int.class) paramString = "Int";
                else if (paramType == long.class) paramString = "Long";
                else if (paramType == float.class) paramString = "Float";
                else if (paramType == double.class) paramString = "Double";
            }
            suffix += paramString;
        }

        // Return
        return prefix + suffix;
    }

}

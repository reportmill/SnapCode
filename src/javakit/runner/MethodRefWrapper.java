package javakit.runner;
import javakit.parse.JExprMethodRef;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import snap.util.Convert;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * Wraps MethodRef expression.
 */
public class MethodRefWrapper {

    // The MethodRef expr
    private JExprMethodRef _methodRefExpr;

    // The MethodRef type
    private JExprMethodRef.Type _type;

    // The target, if type is HelperMethod and method is instance method
    private Object _target;

    /**
     * Constructor.
     */
    public MethodRefWrapper(JExprMethodRef methodRefExpr, Object target)
    {
        _methodRefExpr = methodRefExpr;
        _type = methodRefExpr.getType();
        _target = target;
    }

    /**
     * Invoke MethodRef.
     */
    public Object invokeWithArgs(Object[] args)
    {
        JavaMethod method = _methodRefExpr.getMethod();
        Method method1 = method.getMethod();
        Object thisObj = args[0];

        // Handle InstanceMethod: Invoke MethodRef.Method on first arg, with empty args array
        if (_type == JExprMethodRef.Type.InstanceMethod)
            args = new Object[0];

        // Handle StaticMethod or HelperMethod:
        else thisObj = _target;

        // Invoke method
        try { return method1.invoke(thisObj, args); }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a wrapped MethodRef expression for given class.
     */
    public static Object getWrappedMethodRefExpression(JExprMethodRef methodRefExpr, Object target)
    {
        // Get lambda class
        JavaClass lambdaClass = methodRefExpr.getLambdaClass();
        if (lambdaClass == null)
            throw new RuntimeException("MethodRefWrapper.getWrappedLambdaExpression: Can't determine lambda class for expr: " + methodRefExpr);

        // Get params and content expression
        Class<?> realClass = lambdaClass.getRealClass();

        // Get lambda method name
        JavaMethod lambdaJavaMethod = methodRefExpr.getLambdaMethod();
        Method lambdaMethod = lambdaJavaMethod.getMethod();
        MethodRefWrapper lambdaWrapper = new MethodRefWrapper(methodRefExpr, target);

        // Get MethodType and LambdaWrapper method name
        Class<?> returnType = lambdaMethod.getReturnType();

        ClassLoader classLoader = MethodRefWrapper.class.getClassLoader();
        Class<?>[] interfaces = { realClass };

        // Get converter for return type
        Function<Object,Object> converter = getConverterForClass(returnType);
        String lambdaMethodName = lambdaMethod.getName();

        // Create invocation handler
        InvocationHandler handler =  (Object proxy, Method method, Object[] args) -> {

            // If functional interface method, do that version
            if (method.getName().equals(lambdaMethodName)) {
                Object result = lambdaWrapper.invokeWithArgs(args);
                if (converter != null)
                    result = converter.apply(result);
                return result;
            }

            // Handle default methods
            if (method.isDefault()) {
                Constructor<MethodHandles.Lookup> constr = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                constr.setAccessible(true);
                MethodHandles.Lookup instance = constr.newInstance(realClass, MethodHandles.Lookup.PRIVATE);
                MethodHandle instanceProxy = instance.unreflectSpecial(method, realClass).bindTo(proxy);
                return instanceProxy.invokeWithArguments(args);
            }

            // Probably bogus
            return method.invoke(lambdaWrapper, args);
        };

        // Create and return proxy class for lambda class
        Object lambda = Proxy.newProxyInstance(classLoader, interfaces, handler);
        return lambda;
    }

    private static Function<Object,Object> getConverterForClass(Class<?> aClass)
    {
        if (!aClass.isPrimitive())
            return null;
        if (aClass == boolean.class) return obj -> Convert.booleanValue(obj);
        if (aClass == int.class) return obj -> Convert.getInteger(obj);
        if (aClass == float.class) return obj -> Convert.getFloat(obj);
        if (aClass == double.class) return obj -> Convert.getDouble(obj);
        return null;
    }
}

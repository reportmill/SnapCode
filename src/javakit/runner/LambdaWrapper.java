package javakit.runner;
import javakit.parse.*;
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
 * Wraps Lambda expression.
 */
public abstract class LambdaWrapper implements InvocationHandler {

    // The lambda class
    protected Class<?> _lambdaClass;

    // The lambda method name
    protected String _lambdaMethodName;

    // The result type converter
    protected Function<Object,Object> _converter;

    // The proxy object
    protected Object _proxy;

    /**
     * Constructor.
     */
    public LambdaWrapper(JExprLambdaBase lambdaExpr)
    {
        // Get lambda class
        JavaClass lambdaClass = lambdaExpr.getLambdaClass();
        if (lambdaClass == null)
            throw new RuntimeException("LambdaWrapper.init: Can't determine lambda class for expr: " + lambdaExpr);

        // Get lambda method name
        JavaMethod lambdaJavaMethod = lambdaExpr.getLambdaMethod();
        Method lambdaMethod = lambdaJavaMethod.getMethod();

        // Set lambda class and method
        _lambdaClass = lambdaClass.getRealClass();
        _lambdaMethodName = lambdaMethod.getName();

        // Get converter for return type
        Class<?> returnType = lambdaMethod.getReturnType();
        _converter = getConverterForClass(returnType);

        // Create proxy class for lambda class
        ClassLoader classLoader = getClass().getClassLoader();
        Class<?>[] interfaces = {_lambdaClass};
        _proxy = Proxy.newProxyInstance(classLoader, interfaces, this);
    }

    /**
     * InvocationHandler method: Called when proxy gets method call.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        // If functional interface method, do that version
        if (method.getName().equals(_lambdaMethodName)) {
            Object result = invokeLambdaMethodWithArgs(args);
            if (_converter != null)
                result = _converter.apply(result);
            return result;
        }

        // Handle default methods
        if (method.isDefault()) {
            Constructor<MethodHandles.Lookup> constr = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constr.setAccessible(true);
            MethodHandles.Lookup instance = constr.newInstance(_lambdaClass, MethodHandles.Lookup.PRIVATE);
            MethodHandle instanceProxy = instance.unreflectSpecial(method, _lambdaClass).bindTo(proxy);
            return instanceProxy.invokeWithArguments(args);
        }

        // Evaluate any other (assumed Object?) method using this LambdaWrapper as generic object?
        return method.invoke(this, args);
    }

    /**
     * Invokes the lambda method with given args.
     */
    protected abstract Object invokeLambdaMethodWithArgs(Object[] args);

    /**
     * Returns a converter function.
     */
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

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JavaShell javaShell, Object thisObject, JExprLambda lambdaExpr)
    {
        LambdaWrapper lambdaWrapper = new LambdaExprWrapper(javaShell, thisObject, lambdaExpr);
        return lambdaWrapper._proxy;
    }

    /**
     * Returns a wrapped MethodRef expression for given class.
     */
    public static Object getWrappedMethodRefExpression(JExprMethodRef methodRefExpr, Object target)
    {
        MethodRefExprWrapper lambdaWrapper = new MethodRefExprWrapper(methodRefExpr, target);
        return lambdaWrapper._proxy;
    }

    /**
     * Subclass for Lambda expression.
     */
    private static class LambdaExprWrapper extends LambdaWrapper {

        // The JavaShell
        private JavaShell _javaShell;

        // The 'this' object
        private Object _thisObject;

        // The Content expression
        private JExpr _contentExpr;

        // The params
        private JVarDecl _param0, _param1;

        /**
         * Constructor.
         */
        public LambdaExprWrapper(JavaShell javaShell, Object thisObject, JExprLambda lambdaExpr)
        {
            super(lambdaExpr);
            _javaShell = javaShell;

            // Get lambda expression stuff
            _thisObject = thisObject;
            _contentExpr = lambdaExpr.getExpr();

            JVarDecl[] varDecls = lambdaExpr.getParameters();
            _param0 = varDecls.length > 0 ? varDecls[0] : null;
            _param1 = varDecls.length > 1 ? varDecls[1] : null;
        }

        /**
         * Override to call lambda expression.
         */
        @Override
        protected Object invokeLambdaMethodWithArgs(Object[] args)
        {
            // Push var stack and params
            JSVarStack varStack = _javaShell._varStack;
            varStack.pushStackFrame();
            if (_param0 != null) {
                varStack.setStackValueForNode(_param0, args[0]);
                if (_param1 != null)
                    varStack.setStackValueForNode(_param1, args[1]);
            }

            // Eval content expression, pop var stack and return
            try { return _javaShell._exprEval.evalExpr(_contentExpr, _thisObject); }
            catch (Exception e) { throw new RuntimeException(e); }
            finally { varStack.popStackFrame(); }
        }
    }

    /**
     * Subclass for Lambda expression.
     */
    protected static class MethodRefExprWrapper extends LambdaWrapper {

        // The MethodRef expr
        private JExprMethodRef _methodRefExpr;

        // The MethodRef type
        private JExprMethodRef.Type _type;

        // The target, if type is HelperMethod and method is instance method
        private Object _target;

        // The method ref method
        private Method _methodRefMethod;

        /**
         * Constructor.
         */
        public MethodRefExprWrapper(JExprMethodRef methodRefExpr, Object target)
        {
            super(methodRefExpr);

            // Set MethodRef stuff
            _methodRefExpr = methodRefExpr;
            _type = methodRefExpr.getType();
            _target = target;

            // Get and set MethodRef method
            JavaMethod method = _methodRefExpr.getMethod();
            _methodRefMethod = method.getMethod();
        }

        /**
         * Override to call method ref.
         */
        @Override
        protected Object invokeLambdaMethodWithArgs(Object[] args)
        {
            Object thisObj = args[0];

            // Handle InstanceMethod: Invoke MethodRef.Method on first arg, with empty args array
            if (_type == JExprMethodRef.Type.InstanceMethod)
                args = new Object[0];

            // Handle StaticMethod or HelperMethod:
            else thisObj = _target;

            // Invoke method
            try { return _methodRefMethod.invoke(thisObj, args); }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JExprMethodRef;
import javakit.parse.JVarDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import snap.util.Convert;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * Wraps Lambda expression.
 */
public class MethodRefWrapper {

    // The ExprEval
    private JSExprEval _exprEval;

    // The VarStack
    private JSVarStack _varStack;

    // The OR
    private Object _OR;

    // The MethodRef expr
    private JExprMethodRef _methodRefExpr;

    // The Content expression
    private JExpr _contentExpr;

    // The params
    private JVarDecl _param0, _param1;

    /**
     * Constructor.
     */
    public MethodRefWrapper(JSExprEval exprEval, Object anOR, JExprMethodRef methodRefExpr)
    {
        _exprEval = exprEval;
        _varStack = exprEval._varStack;
        _OR = anOR;
        _methodRefExpr = methodRefExpr;
        //_contentExpr = lambdaExpr.getExpr();

        //List<JVarDecl> varDecls = lambdaExpr.getParameters();
        //_param0 = varDecls.size() > 0 ? varDecls.get(0) : null;
        //_param1 = varDecls.size() > 1 ? varDecls.get(1) : null;
    }

    public Object invokeWithArgs(Object[] args)
    {
        JavaMethod method = _methodRefExpr.getLambdaMethod();
        Method method1 = method.getMethod();
        try { return method1.invoke(_OR, args); }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

//        _varStack.pushStackFrame();
//        if (_param0 != null) {
//            _varStack.setStackValueForNode(_param0, args[0]);
//            if (_param1 != null)
//                _varStack.setStackValueForNode(_param1, args[1]);
//        }
//        try { return _exprEval.evalExpr(_OR, _contentExpr); }
//        catch (Exception e) { throw new RuntimeException(e); }
//        finally { _varStack.popStackFrame(); }
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JSExprEval exprEval, Object anOR, JExprMethodRef methodRefExpr)
    {
        // Get lambda class
        JavaClass lambdaClass = methodRefExpr.getLambdaClass();
        if (lambdaClass == null)
            throw new RuntimeException("MethodRefWrapper.getWrappedLambdaExpression: Can't determine lambda class for expr: " + methodRefExpr);

        try { return getWrappedLambdaExpressionImpl(exprEval, anOR, methodRefExpr, lambdaClass); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpressionImpl(JSExprEval exprEval, Object anOR, JExprMethodRef methodRefExpr, JavaClass lambdaClass) throws Throwable
    {
        // Get params and content expression
        Class<?> realClass = lambdaClass.getRealClass();

        // Get lambda method name
        JavaMethod lambdaJavaMethod = methodRefExpr.getLambdaMethod();
        Method lambdaMethod = lambdaJavaMethod.getMethod();
        MethodRefWrapper lambdaWrapper = new MethodRefWrapper(exprEval, anOR, methodRefExpr);

        // Get MethodType and LambdaWrapper method name
        Class<?> returnType = lambdaMethod.getReturnType();

        ClassLoader classLoader = lambdaWrapper.getClass().getClassLoader();
        Class<?>[] interfaces = { realClass };

        // Get converter for return type
        Function<Object,Object> converter = getConverterForClass(returnType);
        String lambdaMethodName = lambdaMethod.getName();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                // If functional interface method, do that version
                if (method.getName().equals(lambdaMethodName)) {
                    Object result = lambdaWrapper.invokeWithArgs(args);
                    if (converter != null)
                        result = converter.apply(result);
                    else result = returnType.cast(result);
                    return result;
                }

                return method.invoke(lambdaWrapper, args);
            }
        };

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

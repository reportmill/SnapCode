package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JExprLambda;
import javakit.parse.JVarDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;
import java.lang.invoke.*;
import java.util.List;
import java.util.function.Function;

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

    /**
     * Invokes void.
     */
    public Object invokeVoidMethod()
    {
        _varStack.pushStackFrame();
        try { _exprEval.evalExpr(_OR, _contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
        return null;
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

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr)
    {
        // Get lambda class
        JavaClass lambdaClass = lambdaExpr.getLambdaClass();
        if (lambdaClass == null)
            throw new RuntimeException("JSExprEval.evalLambdaExpr: Can't determine lambda class for expr: " + lambdaExpr);

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
        JavaMethod lambdaMethod = lambdaExpr.getLambdaMethod();
        String lambdaMethodName = lambdaMethod.getName();
        MethodType lambdaMethodType = MethodType.methodType(realClass, LambdaWrapper.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        LambdaWrapper lambdaWrapper = new LambdaWrapper(exprEval, anOR, lambdaExpr);

        // Handle Runnable
        if (realClass == Runnable.class) {

            // Create LambdaWrapper for expression
            MethodType methodType = MethodType.methodType(Object.class);
            MethodHandle methodHandle = lookup.findVirtual(LambdaWrapper.class, "invokeVoidMethod", methodType);

            // Get CallSite
            CallSite callSite = LambdaMetafactory.metafactory(lookup, lambdaMethodName, lambdaMethodType, methodType, methodHandle, methodType);

            // Bind call to LambdaWrapper and get a real lambda instance
            MethodHandle lambdaWrapped2 = callSite.getTarget();
            MethodHandle lambdaWrapped3 = lambdaWrapped2.bindTo(lambdaWrapper);
            return lambdaWrapped3.invoke();
        }

        // Handle Function
        if (realClass == Function.class) {

            // Create LambdaWrapper for expression
            MethodType methodType = MethodType.methodType(Object.class, Object.class);
            MethodHandle methodHandle = lookup.findVirtual(LambdaWrapper.class, "invokeObjectMethodWithObject", methodType);

            // Get CallSite
            CallSite callSite = LambdaMetafactory.metafactory(lookup, lambdaMethodName, lambdaMethodType, methodType, methodHandle, methodType);

            // Bind call to LambdaWrapper and get a real lambda instance
            MethodHandle lambdaWrapped2 = callSite.getTarget();
            MethodHandle lambdaWrapped3 = lambdaWrapped2.bindTo(lambdaWrapper);
            return lambdaWrapped3.invoke();
        }

        return null;
    }
}

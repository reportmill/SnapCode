package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JExprLambda;
import javakit.parse.JVarDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaMethod;

import java.lang.invoke.*;
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

    /**
     * Constructor.
     */
    public LambdaWrapper(JSExprEval exprEval, Object anOR, JExpr contentExpr)
    {
        _exprEval = exprEval;
        _varStack = exprEval._varStack;
        _OR = anOR;
        _contentExpr = contentExpr;
    }

    /**
     * Invokes void.
     */
    public Object invokeVoid()
    {
        _varStack.pushStackFrame();
        try { _exprEval.evalExpr(_OR, _contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
        return null;
    }

    public void invokeLambdaVoidMethod0(Object anOR, JExpr contentExpr)
    {
        _varStack.pushStackFrame();
        try { _exprEval.evalExpr(anOR, contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    public Object invokeLambdaObjectMethod(Object anOR, JExpr contentExpr)
    {
        _varStack.pushStackFrame();
        try { return _exprEval.evalExpr(anOR, contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    public Object invokeLambdaObjectMethodWithArg(Object anOR, JExpr contentExpr, JVarDecl param0, Object a)
    {
        _varStack.pushStackFrame();
        _varStack.setStackValueForNode(param0, a);
        try { return _exprEval.evalExpr(anOR, contentExpr); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { _varStack.popStackFrame(); }
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr, JavaClass lambdaClass)
    {
        try { return getWrappedLambdaExpressionImpl(exprEval, anOR, lambdaExpr, lambdaClass); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpressionImpl(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr, JavaClass lambdaClass) throws Throwable
    {
        // Get params and content expression
        List<JVarDecl> varDecls = lambdaExpr.getParameters();
        JVarDecl param0 = varDecls.size() > 0 ? varDecls.get(0) : null;
        JVarDecl param1 = varDecls.size() > 1 ? varDecls.get(1) : null;
        JExpr contentExpr = lambdaExpr.getExpr();
        Class<?> realClass = lambdaClass.getRealClass();

        // Handle Runnable
        if (realClass == Runnable.class) {

            // Get lambda method name
            JavaMethod lambdaMethod = lambdaExpr.getLambdaMethod();
            String lambdaMethodName = lambdaMethod.getName();

            // Create LambdaWrapper for expression
            LambdaWrapper lambdaWrapper = new LambdaWrapper(exprEval, anOR, contentExpr);
            MethodType methodType = MethodType.methodType(Object.class);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle methodHandle = lookup.findVirtual(LambdaWrapper.class, "invokeVoid", methodType);

            // Get CallSite
            MethodType lambdaMethodType = MethodType.methodType(realClass, LambdaWrapper.class);
            CallSite callSite = LambdaMetafactory.metafactory(lookup, lambdaMethodName, lambdaMethodType, methodType, methodHandle, methodType);

            // Bind call to LambdaWrapper and get a real lambda instance
            MethodHandle lambdaWrapped2 = callSite.getTarget();
            MethodHandle lambdaWrapped3 = lambdaWrapped2.bindTo(lambdaWrapper);
            return lambdaWrapped3.invoke();
        }

        return null;
    }
}

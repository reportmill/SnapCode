package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JExprLambda;
import javakit.parse.JVarDecl;
import javakit.resolver.JavaClass;
import snap.util.Convert;
import snap.view.EventListener;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Legacy LambdaWrapper code.
 */
public class LambdaWrapper2 {

    /**
     * Returns a wrapped lambda expression for given class.
     */
    public static Object getWrappedLambdaExpression(JSExprEval exprEval, Object anOR, JExprLambda lambdaExpr, JavaClass lambdaClass)
    {
        // Get params and content expression
        List<JVarDecl> varDecls = lambdaExpr.getParameters();
        JVarDecl param0 = varDecls.size() > 0 ? varDecls.get(0) : null;
        JVarDecl param1 = varDecls.size() > 1 ? varDecls.get(1) : null;
        JExpr contentExpr = lambdaExpr.getExpr();
        Class<?> realClass = lambdaClass.getRealClass();
        JSVarStack _varStack = exprEval._varStack;

        // Handle Runnable
        if (realClass == Runnable.class) {
            return (Runnable) () -> {
                _varStack.pushStackFrame();
                try { exprEval.evalExpr(anOR, contentExpr); }
                catch (Exception e) { throw new RuntimeException(e); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Handle Function
        if (realClass == Function.class) {
            return (Function) a -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, a);
                try { return exprEval.evalExpr(anOR, contentExpr); }
                catch (Exception e) { throw new RuntimeException(e); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Handle Function
        if (realClass == ToIntFunction.class) {
            return (ToIntFunction) a -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, a);
                try {
                    Object value = exprEval.evalExpr(anOR, contentExpr);
                    return Convert.intValue(value);
                }
                catch (Exception e) { throw new RuntimeException(e); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Handle DoubleUnaryOperator
        if (realClass == DoubleUnaryOperator.class) {
            return (DoubleUnaryOperator) d -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, d);
                try {
                    Object value = exprEval.evalExpr(anOR, contentExpr);
                    return Convert.doubleValue(value);
                }
                catch (Exception e) { throw new RuntimeException(e); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Handle DoubleBinaryOperator
        if (realClass == DoubleBinaryOperator.class) {
            return (DoubleBinaryOperator) (x,y) -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, x);
                _varStack.setStackValueForNode(param1, y);
                try {
                    Object value = exprEval.evalExpr(anOR, contentExpr);
                    return Convert.doubleValue(value);
                }
                catch (Exception e) { throw new RuntimeException(e); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Handle EventListener
        if (realClass == EventListener.class) {
            return (EventListener) (e) -> {
                _varStack.pushStackFrame();
                _varStack.setStackValueForNode(param0, e);
                try { exprEval.evalExpr(anOR, contentExpr); }
                catch (Exception e2) { throw new RuntimeException(e2); }
                finally { _varStack.popStackFrame(); }
            };
        }

        // Complain
        throw new RuntimeException("JSExprEval.getWrappedLambdaExpr: Unknown lambda class: " + lambdaClass.getName());
    }
}

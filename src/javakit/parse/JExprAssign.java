/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;

/**
 * A class to represent assignment expressions (including +=, -=, etc.).
 */
public class JExprAssign extends JExpr {

    // The operator
    public Op op;

    // Constants for op
    public enum Op {

        Assign ("="),
        Add ("+="), Subtract ("-="), Multiply ("*="), Divide ("/="), Mod ("%="),
        And ("&="), Or ("|="), Xor ("^="),
        ShiftLeft ("<<="), ShiftRight (">>="), ShiftRightUnsigned (">>>=");

        // The Op string
        private String  _string;

        /** Constructor. */
        Op(String aString)  { _string = aString; }

        /** Returns Op string. */
        String getString()  { return _string; }
    }

    /**
     * Constructor for given op string, target id expression and value expression.
     */
    public JExprAssign(String aString, JExpr aFirst, JExpr aSecond)
    {
        op = getOpForString(aString);
        addChild(aFirst);
        if (aSecond != null)
            addChild(aSecond);
    }

    /**
     * Returns the op.
     */
    public Op getOp()  { return op; }

    /**
     * Returns the id expression.
     */
    public JExpr getIdExpr()  { return (JExpr) getChild(0); }

    /**
     * Returns the value expression.
     */
    public JExpr getValueExpr()
    {
        if (getChildCount() < 2)
            return null;
        return (JExpr) getChild(1);
    }

    /**
     * Sets the value expression.
     */
    public void setValueExpr(JExpr anExpr)
    {
        if (getChildCount() == 1)
            addChild(anExpr);
        else {
            JExpr valueExpr = getValueExpr();
            replaceChild(valueExpr, anExpr);
        }
    }

    /**
     * Returns the class name for expression.
     */
    protected JavaDecl getDeclImpl()
    {
        JExpr target = getIdExpr();
        return target.getEvalType();
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        if (op == Op.Assign)
            return "AssignExpr";
        return "Assign" + op + "Expr";
    }

    /**
     * Returns the Op string for op.
     */
    public static Op getOpForString(String aString)
    {
        for (Op op : Op.values())
            if (op.getString().equals(aString))
                return op;
        throw new RuntimeException("JExprAssign: Unknown Op: " + aString);
    }
}
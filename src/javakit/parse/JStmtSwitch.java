/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for Switch statement.
 */
public class JStmtSwitch extends JStmt {

    // The expression
    protected JExpr  _expr;

    // The list of SwitchLabels
    protected List<JStmtSwitchCase>  _switchCases = new ArrayList<>();

    /**
     * Constructor.
     */
    public JStmtSwitch()
    {
        super();
    }

    /**
     * Returns the expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
    }

    /**
     * Returns the switch labels.
     */
    public List<JStmtSwitchCase> getSwitchCases()  { return _switchCases; }

    /**
     * Adds a switch case.
     */
    public void addSwitchCase(JStmtSwitchCase aSwitchCase)
    {
        _switchCases.add(aSwitchCase);
        addChild(aSwitchCase);
    }
}
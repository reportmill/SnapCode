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
    protected List<JSwitchEntry>  _switchCases = new ArrayList<>();

    // The default case
    private JSwitchEntry _defaultCase;

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
    public List<JSwitchEntry> getSwitchCases()  { return _switchCases; }

    /**
     * Adds a switch case.
     */
    public void addSwitchCase(JSwitchEntry aSwitchCase)
    {
        _switchCases.add(aSwitchCase);
        addChild(aSwitchCase);

        // If given case is default case, set it
        if (aSwitchCase.isDefault())
            _defaultCase = aSwitchCase;
    }

    /**
     * Returns the default case.
     */
    public JSwitchEntry getDefaultCase()  { return _defaultCase; }
}
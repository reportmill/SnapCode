/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for Switch statement.
 */
public class JStmtSwitch extends JStmt {

    // The Switch expression
    private JExprSwitch _switchExpr;

    /**
     * Constructor.
     */
    public JStmtSwitch()
    {
        super();
    }

    /**
     * Returns the switch expression.
     */
    public JExprSwitch getSwitchExpr()  { return _switchExpr; }

    /**
     * Sets the switch expression.
     */
    public void setSwitchExpr(JExprSwitch switchExpr)
    {
        replaceChild(_switchExpr, _switchExpr = switchExpr);
    }

    /**
     * Returns the selector expression.
     */
    public JExpr getSelector()  { return _switchExpr.getSelector(); }

    /**
     * Returns the switch entries.
     */
    public List<JSwitchEntry> getEntries()  { return _switchExpr.getEntries(); }

    /**
     * Returns the default case.
     */
    public JSwitchEntry getDefaultEntry()  { return _switchExpr.getDefaultEntry(); }
}
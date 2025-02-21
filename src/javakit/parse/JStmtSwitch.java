/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for Switch statement.
 */
public class JStmtSwitch extends JStmt {

    // The selector expression
    protected JExpr _selector;

    // The list of switch entries
    protected List<JSwitchEntry> _entries = new ArrayList<>();

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
     * Returns the selector expression.
     */
    public JExpr getSelector()  { return _selector; }

    /**
     * Sets the selector expression.
     */
    public void setSelector(JExpr anExpr)
    {
        replaceChild(_selector, _selector = anExpr);
    }

    /**
     * Returns the switch entries.
     */
    public List<JSwitchEntry> getEntries()  { return _entries; }

    /**
     * Adds a switch entry.
     */
    public void addEntry(JSwitchEntry switchEntry)
    {
        _entries.add(switchEntry);
        addChild(switchEntry);

        // If given case is default case, set it
        if (switchEntry.isDefault())
            _defaultCase = switchEntry;
    }

    /**
     * Returns the default case.
     */
    public JSwitchEntry getDefaultCase()  { return _defaultCase; }
}
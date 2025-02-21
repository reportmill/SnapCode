/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.ArrayList;
import java.util.List;

/**
 * A JExpr subclass for Switch expressions.
 */
public class JExprSwitch extends JExpr {

    // The switch selector
    private JExpr _selector;

    // The list of SwitchLabels
    protected List<JSwitchEntry> _switchCases = new ArrayList<>();

    // The default entry
    private JSwitchEntry _defaultEntry;

    /**
     * Constructor.
     */
    public JExprSwitch()
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
    public List<JSwitchEntry> getEntries()  { return _switchCases; }

    /**
     * Adds a switch entry.
     */
    public void addSwitchEntry(JSwitchEntry aSwitchCase)
    {
        _switchCases.add(aSwitchCase);
        addChild(aSwitchCase);

        // If given case is default case, set it
        if (aSwitchCase.isDefault())
            _defaultEntry = aSwitchCase;
    }

    /**
     * Returns the default entry.
     */
    public JSwitchEntry getDefaultEntry()  { return _defaultEntry; }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "SwitchExpr"; }
}

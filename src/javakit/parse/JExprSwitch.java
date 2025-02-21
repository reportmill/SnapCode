/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import java.util.ArrayList;
import java.util.List;

/**
 * A JExpr subclass for Switch expressions.
 */
public class JExprSwitch extends JExpr {

    // The switch selector
    private JExpr _selector;

    // The list of switch entries
    protected List<JSwitchEntry> _entries = new ArrayList<>();

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
    public List<JSwitchEntry> getEntries()  { return _entries; }

    /**
     * Adds a switch entry.
     */
    public void addEntry(JSwitchEntry aSwitchCase)
    {
        _entries.add(aSwitchCase);
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
     * Returns the expression type.
     */
    public JavaDecl getExprType()
    {
        JavaType exprType = null;

        // Get return type
        for (JSwitchEntry switchEntry : _entries) {
            exprType = switchEntry.getReturnType();
            if (exprType != null)
                break;
        }

        // Return
        return exprType;
    }

    /**
     * Override to return declaration of type.
     */
    protected JavaDecl getDeclImpl()  { return getExprType(); }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "SwitchExpr"; }
}

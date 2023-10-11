/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import java.util.ArrayList;
import java.util.List;

/**
 * This JavaTextDoc subclass supports Java Repl.
 */
public class JeplTextDoc extends JavaTextDoc {

    // The array of imports
    private String[]  _imports;

    /**
     * Constructor.
     */
    public JeplTextDoc()
    {
        super();

        // Initialize imports
        List<String> imports = new ArrayList<>();
        imports.add("java.util.*");
        imports.add("java.util.stream.*");
        imports.add("snap.view.*");
        imports.add("snap.gfx.*");
        imports.add("snapcharts.data.*");
        imports.add("snapcharts.repl.*");
        imports.add("static snapcharts.repl.ReplObject.*");
        imports.add("static snapcharts.repl.QuickCharts.*");
        imports.add("static snapcharts.repl.QuickData.*");
        _imports = imports.toArray(new String[0]);
    }

    /**
     * Returns the imports.
     */
    public String[] getImports()  { return _imports; }

    /**
     * Returns the base class name.
     */
    public String getSuperClassName()  { return "Object"; }

    /**
     * Override to return as JeplAgent.
     */
    @Override
    public JeplAgent getAgent()  { return (JeplAgent) super.getAgent(); }
}

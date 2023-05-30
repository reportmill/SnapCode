/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ArrayUtils;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This JavaTextDoc subclass supports Java Repl.
 */
public class JeplTextDoc extends JavaTextDoc {

    // The array of imports
    private String[]  _imports = DEFAULT_IMPORTS;

    // The super class name
    private String  _superClassName = "Object";

    // A configure
    private static Consumer<JeplTextDoc>  _jeplDocConfig;

    // Constants for imports
    private static final String IMPORT1 = "java.util.*";
    private static final String IMPORT2 = "java.util.stream.*";
    private static final String IMPORT3 = "snap.view.*";
    private static final String[] DEFAULT_IMPORTS = { IMPORT1, IMPORT2, IMPORT3 };

    /**
     * Constructor.
     */
    public JeplTextDoc()
    {
        super();

        // If config set, do configure
        if (_jeplDocConfig != null)
            _jeplDocConfig.accept(this);
    }

    /**
     * Returns the imports.
     */
    public String[] getImports()  { return _imports; }

    /**
     * Adds an import.
     */
    public void addImport(String anImportStr)
    {
        _imports = ArrayUtils.add(_imports, anImportStr);
        Arrays.sort(_imports);
    }

    /**
     * Returns the base class name.
     */
    public String getSuperClassName()  { return _superClassName; }

    /**
     * Sets the base class name.
     */
    public void setSuperClassName(String aName)
    {
        _superClassName = aName;
    }

    /**
     * Override to return as JeplAgent.
     */
    @Override
    public JeplAgent getAgent()  { return (JeplAgent) super.getAgent(); }

    /**
     * Sets a configure function.
     */
    public static void setJeplDocConfig(Consumer<JeplTextDoc> aConfig)
    {
        _jeplDocConfig = aConfig;
    }

    /**
     * Returns the JeplTextDoc for given source.
     */
    public static JavaTextDoc getJeplTextDocForSourceURL(Object aSource)
    {
        // If Source is null, create temp file
        if (aSource == null)
            aSource = ProjectUtils.getTempSourceFile(null, "Untitled", "jepl");
        return JavaTextDoc.getJavaTextDocForSource(aSource);
    }
}

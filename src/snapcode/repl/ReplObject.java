/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.repl;
import javakit.runner.JavaShell;

/**
 * This is a REPL base class to provide some convenience methods.
 */
public class ReplObject extends QuickCharts {

    /**
     * Conveniences.
     */
    public static void print(Object anObj)  { System.out.print(anObj); }

    /**
     * Conveniences.
     */
    public static void println(Object anObj)  { System.out.println(anObj); }

    /**
     * Show object.
     */
    public static void show(Object anObj)
    {
        JavaShell.getClient().processOutput(anObj);
    }
}

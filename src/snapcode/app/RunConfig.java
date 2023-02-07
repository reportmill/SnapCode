/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;

/**
 * A class to represent a RunConfiguration.
 */
public class RunConfig {

    // The name
    String _name;

    // The main class
    String _mainClassName;

    // App args
    String _appArgs;

    // VM args
    String _vmArgs;

    /**
     * Returns the name.
     */
    public String getName()
    {
        return _name;
    }

    /**
     * Sets the name.
     */
    public RunConfig setName(String aName)
    {
        _name = aName;
        return this;
    }

    /**
     * Returns the Main Class Name.
     */
    public String getMainClassName()
    {
        return _mainClassName;
    }

    /**
     * Sets the Main Class Name.
     */
    public RunConfig setMainClassName(String aClassName)
    {
        _mainClassName = aClassName;
        return this;
    }

    /**
     * Returns the main class file path.
     */
    public String getMainFilePath()
    {
        String name = getMainClassName();
        if (name == null) name = "null";
        return "/" + name.replace('.', '/') + ".class";
    }

    /**
     * Returns the app args.
     */
    public String getAppArgs()
    {
        return _appArgs;
    }

    /**
     * Sets the app args.
     */
    public RunConfig setAppArgs(String theArgs)
    {
        _appArgs = theArgs;
        return this;
    }

    /**
     * Returns the VM args.
     */
    public String getVMArgs()
    {
        return _vmArgs;
    }

    /**
     * Sets the VM args.
     */
    public RunConfig setVMArgs(String theArgs)
    {
        _vmArgs = theArgs;
        return this;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + getName() + " (" + getMainClassName() + ")";
    }

}
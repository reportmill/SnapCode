/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.web.WebFile;

/**
 * A class to represent a RunConfiguration.
 */
public class RunConfig {

    // The workspace owner
    protected Workspace _workspace;

    // The name
    private String _name;

    // The main class
    private String _mainClassName;

    // App args
    private String _appArgs;

    // VM args
    private String _vmArgs;

    // The main Java file
    private WebFile _mainJavaFile;

    // The main class file
    private WebFile _mainClassFile;

    /**
     * Constructor.
     */
    public RunConfig()
    {
        super();
    }

    /**
     * Returns the project.
     */
    public Project getProject()
    {
        WebFile javaFile = getMainJavaFile();
        return javaFile != null ? Project.getProjectForFile(javaFile) : null;
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        if (_name != null) return _name;
        int dotIndex = _mainClassName != null ? _mainClassName.lastIndexOf('.') : -1;
        String name = dotIndex > 0 ? _mainClassName.substring(dotIndex + 1) : _mainClassName;
        return _name = name != null ? name : "Current File";
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
    public String getMainClassName()  { return _mainClassName; }

    /**
     * Sets the Main Class Name.
     */
    public RunConfig setMainClassName(String aClassName)
    {
        _mainClassName = aClassName;
        return this;
    }

    /**
     * Returns the app args.
     */
    public String getAppArgs()  { return _appArgs; }

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
    public String getVMArgs()  { return _vmArgs; }

    /**
     * Sets the VM args.
     */
    public RunConfig setVMArgs(String theArgs)
    {
        _vmArgs = theArgs;
        return this;
    }

    /**
     * Returns the main class file path.
     */
    public String getMainClassFilePath()
    {
        String className = getMainClassName();
        if (className == null) className = "null";
        return "/" + className.replace('.', '/') + ".class";
    }

    /**
     * Returns the main java file.
     */
    public WebFile getMainJavaFile()
    {
        if (_mainJavaFile != null) return _mainJavaFile;
        return _mainJavaFile = getMainJavaFileImpl();
    }

    /**
     * Returns the main java file.
     */
    private WebFile getMainJavaFileImpl()
    {
        // Search workspace projects for java file
        String mainClassName = getMainClassName();
        Project[] projects = _workspace.getProjects();
        for (Project project : projects) {
            WebFile javaFile = project.getJavaFileForClassName(mainClassName);
            if (javaFile != null)
                return javaFile;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the main class file.
     */
    public WebFile getMainClassFile()
    {
        if (_mainClassFile != null) return _mainClassFile;
        return _mainClassFile = getMainClassFileImpl();
    }

    /**
     * Returns the main class file.
     */
    private WebFile getMainClassFileImpl()
    {
        Project project = getProject();
        WebFile javaFile = getMainJavaFile();
        return project != null ? project.getProjectFiles().getClassFileForJavaFile(javaFile) : null;
    }

    /**
     * Returns whether config is swing.
     */
    public boolean isSwing()
    {
        WebFile javaFile = getMainJavaFile();
        return javaFile != null && javaFile.getText().contains("javax.swing");
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + getName() + " (" + getMainClassName() + ")";
    }

    /**
     * Creates a run config for Java file.
     */
    public static RunConfig createRunConfigForJavaFile(WebFile javaFile)
    {
        Project project = Project.getProjectForFile(javaFile);
        Workspace workspace = project.getWorkspace();
        String className = project.getClassNameForFile(javaFile);
        return createRunConfigForWorkspaceAndClassName(workspace, className);
    }

    /**
     * Creates a run config for Java file.
     */
    public static RunConfig createRunConfigForWorkspaceAndClassName(Workspace workspace, String className)
    {
        RunConfig runConfig = new RunConfig();
        runConfig._workspace = workspace;
        runConfig._mainClassName = className;
        return runConfig;
    }
}
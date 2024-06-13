package snapcode.debug;
import snap.gfx.Color;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.view.TextArea;
import snap.view.View;
import snap.view.ViewEnv;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.viewx.Console;
import snapcode.apptools.RunTool;
import snapcode.project.Breakpoint;
import snap.web.WebURL;
import snapcode.project.Project;
import snapcode.project.RunConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to run an external process.
 */
public abstract class RunApp {

    // The RunTook
    protected RunTool _runTool;

    // The RunConfig
    protected RunConfig _runConfig;

    // The args
    private String[] _args;

    // Main class name
    private String _mainClassName;

    // Class path (also in vmArguments)
    private String _classPath;

    // VM args
    private String _vmArgs;

    // Command args
    private String _appArgs;

    // The working directory
    private File _workDir;

    // Text to hold system console output
    protected ConsoleText _consoleText;

    // TextView to hold console text
    protected TextArea _consoleTextView;

    // The console view
    private View _consoleView;

    // The alternate console view
    private View _altConsoleView;

    // Whether app is running
    protected boolean _running;

    // Whether app has been paused
    protected boolean _paused;

    // Whether app has finished running
    protected boolean _terminated;

    // App listeners
    protected AppListener[] _appLsnrs = new AppListener[0];

    /**
     * Creates a new RunApp for URL and args.
     */
    public RunApp(RunTool runTool, RunConfig runConfig)
    {
        super();
        _runTool = runTool;
        _runConfig = runConfig;

        // Get args and set
        String[] args = getDefaultRunArgs();
        setArgs(args);

        // Create TextBlock to hold system console output
        _consoleText = new ConsoleText();
        _consoleText.setRunTool(_runTool);

        // Create ConsoleTextView
        _consoleTextView = new TextArea();
        _consoleTextView.setEditable(true);
        _consoleTextView.setFill(Color.WHITE);
        _consoleTextView.setPadding(8, 8, 8, 8);
        _consoleTextView.setGrowHeight(true);
        _consoleTextView.setSourceText(_consoleText);
        _consoleView = _consoleTextView;
    }

    /**
     * Returns the name of this app.
     */
    public String getName()
    {
        String appName = _runConfig.getName();
        if (isTerminated())
            appName += " <terminated>";
        return appName;
    }

    /**
     * Returns the main file.
     */
    public WebFile getMainFile()  { return _runConfig.getMainJavaFile(); }

    /**
     * Returns the app launch args.
     */
    public String[] getArgs()  { return _args; }

    /**
     * Sets the app launch args.
     */
    public boolean setArgs(String[] argv)
    {
        // Set args
        _args = argv;

        // Declare/initialize some variables
        String className = "";
        String classPath = "";
        String javaArgs = "";
        String progArgs = "";

        // Iterate over args
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            // Handle -X
            if (token.equals("-X")) {
                error("Use 'java -X' to see the available non-standard options\n");
                return false;
            }

            // Handle Standard VM options passed on
            else if (token.equals("-v") || token.startsWith("-v:") ||  // -v[:...]
                    token.startsWith("-verbose") ||                  // -verbose[:...]
                    token.startsWith("-D") ||
                    // NonStandard options passed on
                    token.startsWith("-X") ||
                    // Old-style options (These should remain in place as long as the standard VM accepts them)
                    token.equals("-noasyncgc") || token.equals("-prof") ||
                    token.equals("-verify") || token.equals("-noverify") ||
                    token.equals("-verifyremote") ||
                    token.equals("-verbosegc") ||
                    token.startsWith("-ms") || token.startsWith("-mx") ||
                    token.startsWith("-ss") || token.startsWith("-oss")) {
                javaArgs += token + " ";
            }

            // Handle -classpath
            else if (token.equals("-classpath") || token.equals("-cp")) {
                if (i == argv.length - 1) {
                    error("No classpath specified.");
                    return false;
                }
                classPath = argv[++i];
                javaArgs += "-cp \"" + classPath + '"';
            }

            // Handle -help, -version or -[unknown]
            else if (token.equals("-help")) {
                error("");
                return false;
            }
            else if (token.equals("-version")) {
                error("Version 1");
                return false;
            }
            else if (token.startsWith("-")) {
                error("invalid option: " + token);
                return false;
            }

            // Everything from here is part of the command line
            else {
                className = token;
                for (i++; i < argv.length; i++) progArgs += argv[i] + " ";
                break;
            }
        }

        // Configure context
        setVmArgs(javaArgs);
        setAppArgs(progArgs);
        setMainClassName(className);
        setClassPath(classPath);

        // Return true since configure was fine
        return true;
    }

    /**
     * Returns the working directory.
     */
    public File getWorkingDirectory()  { return _workDir; }

    /**
     * Sets the working directory.
     */
    public void setWorkingDirectory(Object aDir)
    {
        if (aDir instanceof File)
            _workDir = (File) aDir;
        else {
            WebURL url = WebURL.getURL(aDir);
            _workDir = url != null ? url.getJavaFile() : null;
        }
    }

    /**
     * Returns VM arguments.
     */
    public String getVmArgs()  { return _vmArgs; }

    /**
     * Sets VM arguments.
     */
    public void setVmArgs(String theArgs)  { _vmArgs = theArgs; }

    /**
     * Returns program arguments.
     */
    public String getAppArgs()  { return _appArgs; }

    /**
     * Sets program arguments.
     */
    public void setAppArgs(String theArgs)  { _appArgs = theArgs; }

    /**
     * Returns the app Main class name.
     */
    public String getMainClassName()  { return _mainClassName; }

    /**
     * Sets the app Main class name.
     */
    public void setMainClassName(String mainClassName)  { _mainClassName = mainClassName; }

    /**
     * Returns the app class path.
     */
    public String getClassPath()  { return _classPath; }

    /**
     * Sets the app class path.
     */
    public void setClassPath(String aPath)  { _classPath = aPath; }

    /**
     * Returns the project for the main file.
     */
    public Project getMainFileProject()
    {
        WebFile mainFile = getMainFile();
        return Project.getProjectForFile(mainFile);
    }

    /**
     * Returns the console view.
     */
    public View getConsoleView()  { return _consoleView; }

    /**
     * Sets the console view.
     */
    public void setConsoleView(View aView)
    {
        if (aView == _consoleView) return;
        _consoleView = aView;
        _runTool.consoleViewDidChange(this);
    }

    /**
     * Returns the alternate console view.
     */
    public View getAltConsoleView()  { return _altConsoleView; }

    /**
     * Sets the console view.
     */
    public void setAltConsoleView(View aView)
    {
        if (aView == _altConsoleView) return;
        _altConsoleView = aView;
        setConsoleView(aView != null ? aView : _consoleTextView);
    }

    /**
     * Returns the console text view.
     */
    public TextArea getConsoleTextView()  { return _consoleTextView; }

    /**
     * Returns the console text.
     */
    public ConsoleText getConsoleText()  { return _consoleText; }

    /**
     * Clears the console text.
     */
    public void clearConsole()
    {
        _consoleText.clear();
        if (_altConsoleView != null) {
            Console.setShared(null);
            setAltConsoleView(null);
        }
    }

    /**
     * Executes the given command + args.
     */
    public abstract void exec();

    /**
     * Terminates the process.
     */
    public abstract void terminate();

    /**
     * Returns whether process is running.
     */
    public boolean isRunning()  { return _running; }

    /**
     * Returns whether process is terminated.
     */
    public boolean isTerminated()  { return _terminated; }

    /**
     * Returns whether process is paused.
     */
    public boolean isPaused()  { return _paused; }

    /**
     * Sets whether process is paused.
     */
    protected void setPaused(boolean aVal)
    {
        _paused = aVal;
    }

    /**
     * Post an error message to the PrintWriter.
     */
    public void error(String message)  { printDiagnostic(message);  }

    /**
     * Post an error message to the PrintWriter.
     */
    public void failure(String message)  { printDiagnostic(message); }

    /**
     * Post an error message to the PrintWriter.
     */
    public void notice(String message)  { printDiagnostic(message); }

    /**
     * Called to append to output buffer.
     */
    protected void appendOut(String aStr)
    {
        appendConsoleOutput(aStr, false);
    }

    /**
     * Called to append to error buffer.
     */
    protected void appendErr(String aStr)
    {
        appendConsoleOutput(aStr, true);
    }

    /**
     * Called to append console output.
     */
    public void appendConsoleOutput(String aString, boolean isError)
    {
        if (!ViewEnv.getEnv().isEventThread())
            ViewUtils.runLater(() -> appendConsoleOutput(aString, isError));
        else _consoleText.appendString(aString, isError);
    }

    /**
     * Sends input to process.
     */
    public abstract void sendInput(String aStr);

    /**
     * Called when process exited.
     */
    protected void notifyAppExited()
    {
        for (AppListener lsnr : _appLsnrs)
            lsnr.appExited(this);
    }

    /**
     * Adds a breakpoint.
     */
    public void addBreakpoint(Breakpoint aBP)  { }

    /**
     * Removes a breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)  { }

    /**
     * Adds listener.
     */
    public void addListener(AppListener aListener)
    {
        _appLsnrs = ArrayUtils.add(_appLsnrs, aListener);
    }

    /**
     * Removes listener.
     */
    public void removeListener(AppListener aListener)
    {
        _appLsnrs = ArrayUtils.removeId(_appLsnrs, aListener);
    }

    /**
     * Prints a diagnostic.
     */
    public void printDiagnostic(String aString)
    {
        System.out.println(aString);
    }


    /**
     * Returns an array of args for given run config.
     */
    public String[] getDefaultRunArgs()
    {
        // Get project (just complain and return if not found)
        Project project = _runConfig.getProject();
        if (project == null) {
            System.err.println("RunApp: no project for main class: " + _runConfig.getMainClassName());
            return null;
        }

        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // Add Java command path
        String javaCmdPath = getJavaCmdPath();
        if (SnapUtils.isWebVM) {
            boolean isSnapKit = project.getBuildFile().isIncludeSnapKitRuntime();
            boolean isSnapKitDom = isSnapKit && !ViewUtils.isAltDown() && !_runConfig.isSwing();
            if (isSnapKitDom)
                javaCmdPath = "java-dom";
        }
        commands.add(javaCmdPath);

        // Get Class path and add to list
        String[] classPaths = project.getRuntimeClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = _runConfig.getMainClassName();
        commands.add(className);

        // Add App Args
        String appArgs = _runConfig.getAppArgs();
        if (appArgs != null && !appArgs.isEmpty())
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
    }

    /**
     * Returns the path for the java command.
     */
    private static String getJavaCmdPath()
    {
        if (SnapUtils.isWebVM) return "java";
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
}
package snapcode.debug;
import snap.util.ArrayUtils;
import snap.view.ViewEnv;
import snap.view.ViewUtils;
import snapcode.project.Breakpoint;
import snap.web.WebURL;
import java.io.*;

/**
 * A class to run an external process.
 */
public abstract class RunApp {

    // The URL
    private WebURL _url;

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
    protected ConsoleText _outputText;

    // Whether app is running
    protected boolean _running;

    // Whether app has been paused
    protected boolean _paused;

    // Whether app has finished running
    protected boolean _terminated;

    // App listeners
    protected AppListener[] _appLsnrs = new AppListener[0];

    // Proxies for AppOutput, AppError and Diagnostics.
    protected OutputListener _diagnostics = str -> System.out.println(str);

    /**
     * Creates a new RunApp for URL and args.
     */
    public RunApp(WebURL aURL, String[] theArgs)
    {
        _url = aURL;
        setArgs(theArgs);

        // Create TextBlock to hold system console output
        _outputText = new ConsoleText();
    }

    /**
     * Returns the args.
     */
    public String[] getArgs()  { return _args; }

    /**
     * Sets DebugApp launch args.
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
            } else if (token.equals("-version")) {
                error("Version 1");
                return false;
            } else if (token.startsWith("-")) {
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
     * Returns the main class url.
     */
    public WebURL getURL()  { return _url; }

    /**
     * Returns the name.
     */
    public String getName()
    {
        String name = getURL().getFilenameSimple();
        if (isTerminated()) name += " <terminated>";
        return name;
    }

    /**
     * Returns the output text.
     */
    public ConsoleText getOutputText()  { return _outputText; }

    /**
     * Clears the output text.
     */
    public void clearOutputText()  { _outputText.clear(); }

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
    public void error(String message)  { _diagnostics.putString(message);  }

    /**
     * Post an error message to the PrintWriter.
     */
    public void failure(String message)  { _diagnostics.putString(message); }

    /**
     * Post an error message to the PrintWriter.
     */
    public void notice(String message)  { _diagnostics.putString(message); }

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
        else _outputText.appendString(aString, isError);
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
     * Sets listener.
     */
    public void addListener(AppListener aListener)
    {
        _appLsnrs = ArrayUtils.add(_appLsnrs, aListener);
    }

    /**
     * An interface for objects providing input.
     */
    public interface InputListener {
        String getLine();
    }

    /**
     * An interface for objects providing input.
     */
    public interface OutputListener {
        void putString(String str);
    }
}
package snapcode.debug;
import snapcode.project.Breakpoint;
import snap.util.StringUtils;
import snap.web.WebURL;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to run an external process.
 */
public class RunApp {

    // The URL
    WebURL _url;

    // The args
    String[] _args;

    // Main class name
    String _mainClassName;

    // Class path (also in vmArguments)
    String _classPath;

    // VM args
    String _vmArgs;

    // Command args
    String _appArgs;

    // The working directory
    File _workDir;

    // The process
    Process _process;

    // The writer to process stdin
    BufferedWriter _stdInWriter;

    // The readers
    StreamReader _outReader, _errReader;

    // String buffer for output text
    List<Output> _output = new ArrayList();

    // Whether app is running
    boolean _running;

    // Whether app has finished running
    boolean _terminated;

    // Whether error was printed
    boolean _hadError;

    // The listener
    AppListener _listener;
    List<AppListener> _lsnrs = new ArrayList();

    /**
     * Creates a new RunApp for URL and args.
     */
    public RunApp(WebURL aURL, String[] theArgs)
    {
        _url = aURL;
        setArgs(theArgs);
    }

    /**
     * Sets DebugApp launch args.
     */
    public boolean setArgs(String[] argv)
    {
        // Set args
        _args = argv;

        // Declare/initialize some variables
        String clsName = "", classPath = "", javaArgs = "", progArgs = "";

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
                clsName = token;
                for (i++; i < argv.length; i++) progArgs += argv[i] + " ";
                break;
            }
        }

        // Configure context
        setVmArgs(javaArgs);
        setAppArgs(progArgs);
        setMainClassName(clsName);
        setClassPath(classPath);

        // Return true since configure was fine
        return true;
    }

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
    public String getVmArgs()
    {
        return _vmArgs;
    }

    /**
     * Sets VM arguments.
     */
    public void setVmArgs(String theArgs)
    {
        _vmArgs = theArgs;
    }

    /**
     * Returns program arguments.
     */
    public String getAppArgs()
    {
        return _appArgs;
    }

    /**
     * Sets program arguments.
     */
    public void setAppArgs(String theArgs)
    {
        _appArgs = theArgs;
    }

    /**
     * Returns the app Main class name.
     */
    public String getMainClassName()
    {
        return _mainClassName;
    }

    /**
     * Sets the app Main class name.
     */
    public void setMainClassName(String mainClassName)
    {
        this._mainClassName = mainClassName;
    }

    /**
     * Returns the app class path.
     */
    public String getClassPath()
    {
        return _classPath;
    }

    /**
     * Sets the app class path.
     */
    public void setClassPath(String aPath)
    {
        _classPath = aPath;
    }

    /**
     * Executes the given command + args.
     */
    public void exec()
    {
        // Print exec args
        appendOut(StringUtils.join(_args, " ") + '\n');

        // Run process
        try {
            _process = Runtime.getRuntime().exec(_args, null, _workDir);
            _running = true;
            startProcessReaders();
        }

        // Catch exceptions
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Returns the main class url.
     */
    public WebURL getURL()
    {
        return _url;
    }

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
     * Returns the running process.
     */
    public Process getProcess()
    {
        return _process;
    }

    /**
     * Starts the process readers.
     */
    protected void startProcessReaders()
    {
        _outReader = new StreamReader(_process.getInputStream(), false); // Start standard out
        _outReader.setPriority(Thread.MAX_PRIORITY - 1);
        _outReader.start();
        _errReader = new StreamReader(_process.getErrorStream(), true);  // Start standard err
        _errReader.setPriority(Thread.MAX_PRIORITY - 1);
        _errReader.start();
    }

    /**
     * Returns the output text.
     */
    public List<Output> getOutput()
    {
        return _output;
    }

    /**
     * Clears the output text.
     */
    public void clearOutput()
    {
        _output.clear();
    }

    /**
     * Terminates the process.
     */
    public void terminate()
    {
        _process.destroy();
    }

    /**
     * Returns whether process is running.
     */
    public boolean isRunning()
    {
        return _running;
    }

    /**
     * Returns whether process is terminated.
     */
    public boolean isTerminated()
    {
        return _terminated;
    }

    /**
     * Returns whether process is paused.
     */
    public boolean isPaused()
    {
        return false;
    }

    /**
     * Post an error message to the PrintWriter.
     */
    public void error(String message)
    {
        _diagnostics.putString(message);
    }

    /**
     * Post an error message to the PrintWriter.
     */
    public void failure(String message)
    {
        _diagnostics.putString(message);
    }

    /**
     * Post an error message to the PrintWriter.
     */
    public void notice(String message)
    {
        _diagnostics.putString(message);
    }

    /**
     * Proxies for AppOutput, AppError and Diagnostics.
     */
    OutputListener _diagnostics = new OutputListener() {
        public void putString(String aStr)
        {
            System.out.println(aStr);
        }
    };

    /**
     * Called to append to output buffer.
     */
    protected void appendOut(String aStr)
    {
        _output.add(new Output(aStr, false));
        for (AppListener lsnr : _lsnrs)
            lsnr.appendOut(this, aStr);
    }

    /**
     * Called to append to error buffer.
     */
    protected void appendErr(String aStr)
    {
        _output.add(new Output(aStr, true));
        for (AppListener lsnr : _lsnrs)
            lsnr.appendErr(this, aStr);
    }

    /**
     * Sends input to process.
     */
    public void sendInput(String aStr)
    {
        // If writer not yet set, create and set
        if (_stdInWriter == null) {
            OutputStream stdin = _process.getOutputStream();
            _stdInWriter = new BufferedWriter(new OutputStreamWriter(stdin));
        }

        // Append to output
        _output.add(new Output(aStr, false));

        // Write and flush
        try {
            _stdInWriter.write(aStr);
            _stdInWriter.flush();
        } catch (Exception e) {
            appendErr("RunApp.sendInput: Failed to write to process: " + e);
        }
    }

    /**
     * Called when process exited.
     */
    protected void notifyAppExited()
    {
        for (AppListener lsnr : _lsnrs)
            lsnr.appExited(this);
    }

    /**
     * Returns whether process had error.
     */
    public boolean hadError()
    {
        return _hadError;
    }

    /**
     * Adds a breakpoint.
     */
    public void addBreakpoint(Breakpoint aBP)
    {
    }

    /**
     * Removes a breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)
    {
    }

    /**
     * Sets listener.
     */
    public void addListener(AppListener aListener)
    {
        _lsnrs.add(aListener);
    }

    /**
     * Sets listener.
     */
    public void setListener(AppListener aListener)
    {
        _listener = aListener;
        _lsnrs.add(aListener);
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

    /**
     * An interface for objects wanting notification of change of Session status.
     */
    public interface AppListener {

        // App started, paused, resumed, exited
        void appStarted(RunApp em);

        void appPaused(DebugApp em);

        void appResumed(DebugApp em);

        void appExited(RunApp em);

        // FrameChanged
        void frameChanged(DebugApp anApp);

        // DebugEvent
        void processDebugEvent(DebugApp anApp, DebugEvent e);

        // Notifications for Breakpoints, Watchpoints and Exception points.
        void requestSet(BreakpointReq e);

        void requestDeferred(BreakpointReq e);

        void requestDeleted(BreakpointReq e);

        void requestError(BreakpointReq e);

        // Notification for debugger output
        void appendOut(RunApp aProc, String aStr);

        void appendErr(RunApp aProc, String aStr);
    }

    /**
     * A listener for JDI events (with embedded adapter).
     */
    public static class AppAdapter implements AppListener {

        // App started, paused, resumed, exited
        public void appStarted(RunApp em)
        {
        }

        public void appPaused(DebugApp em)
        {
        }

        public void appResumed(DebugApp em)
        {
        }

        public void appExited(RunApp em)
        {
        }

        // FrameChanged
        public void frameChanged(DebugApp anApp)
        {
        }

        // DebugEvent
        public void processDebugEvent(DebugApp anApp, DebugEvent e)
        {
        }

        // Notifications for Breakpoints, Watchpoints and Exception points.
        public void requestSet(BreakpointReq e)
        {
        }

        public void requestDeferred(BreakpointReq e)
        {
        }

        public void requestDeleted(BreakpointReq e)
        {
        }

        public void requestError(BreakpointReq e)
        {
        }

        // Notification for debugger output
        public void appendOut(RunApp aProc, String aStr)
        {
        }

        public void appendErr(RunApp aProc, String aStr)
        {
        }
    }

    /**
     * An inner class to read from an input stream in a separate thread to a string buffer.
     */
    private class StreamReader extends Thread {
        InputStream _is;
        boolean _isErr;

        StreamReader(InputStream anIS, boolean isErr)
        {
            _is = anIS;
            _isErr = isErr;
        }

        public void run()
        {
            try {
                InputStreamReader isr = new InputStreamReader(_is);
                BufferedReader br = new BufferedReader(isr);
                char[] chars = new char[1024];
                for (int len = br.read(chars, 0, 1024); len >= 0; len = br.read(chars, 0, 1024)) {
                    String line = new String(chars, 0, len);
                    if (line.length() > 0 && _isErr) _hadError = true;
                    if (_isErr) appendErr(line);
                    else appendOut(line);
                }
                if (!_terminated) {
                    _running = false;
                    _terminated = true;
                    notifyAppExited();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Output from app.
     */
    public class Output {
        String _str;
        boolean _isErr;

        Output(String aStr, boolean isErr)
        {
            _str = aStr;
            _isErr = isErr;
        }

        public String getString()
        {
            return _str;
        }

        public boolean isErr()
        {
            return _isErr;
        }
    }

}
package snapcode.debug;
import javakit.runner.JavaShell;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcharts.repl.Console;
import snapcharts.repl.DefaultConsole;
import snapcharts.repl.ScanPane;
import snapcode.apptools.RunTool;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * This RunApp subclass runs an app from source.
 */
public class RunAppSrc extends RunApp {

    // The JavaShell
    protected JavaShell _javaShell;

    // The thread to reset views
    private Thread _runAppThread;

    // The Console
    protected Console _console;

    // An input stream for standard in
    protected ScanPane.BytesInputStream _inputStream;

    // The real system in/out/err
    private static final InputStream REAL_SYSTEM_IN = System.in;
    private static final PrintStream REAL_SYSTEM_OUT = System.out;
    private static final PrintStream REAL_SYSTEM_ERR = System.err;

    /**
     * Constructor.
     */
    public RunAppSrc(RunTool runTool, WebURL mainClassFileURL, String[] args)
    {
        super(runTool, mainClassFileURL, args);

        // Create JavaShell
        _javaShell = new JavaShell(this);

        // Create console
        _console = new DefaultConsole();
    }

    /**
     * Override to run source app.
     */
    @Override
    public void exec()
    {
        // Set console
        Console.setShared(null);
        Console.setConsoleCreatedHandler(() -> setAltConsoleView(Console.getShared().getConsoleView()));

        // Create and start new thread to run
        _runAppThread = new Thread(() -> runAppImpl());
        _runAppThread.start();
        _running = true;
    }

    /**
     * Runs Java Code.
     */
    protected void runAppImpl()
    {
        // Get JavaAgent
        WebURL mainClassURL = getURL();
        WebFile mainClassFile = mainClassURL.getFile();
        JavaAgent javaAgent = mainClassFile != null ? JavaAgent.getAgentForFile(mainClassFile) : null;
        if (javaAgent == null)
            return;

        // Replace System.in with proxy versions to allow input/output
        System.setIn(_inputStream = new ScanPane.BytesInputStream(null));
        System.setOut(new ProxyPrintStream(REAL_SYSTEM_OUT));
        System.setErr(new ProxyPrintStream(REAL_SYSTEM_ERR));

        // Run code
        _javaShell.runJavaCode(javaAgent);

        // Process terminate
        finalizeTermination();
    }

    /**
     * Terminates the process.
     */
    @Override
    public void terminate()
    {
        // If already cancelled, just return
        if (_runAppThread == null) return;

        // Send interrupt to shell (soft interrupt)
        _javaShell.interrupt();

        // Register timeout to check if hard thread interrupt is needed
        ViewUtils.runDelayed(() -> hardTerminate(), 600);
    }

    /**
     * Called to really terminate run with thread interrupt, if in system code.
     */
    private void hardTerminate()
    {
        // If standard terminate worked, just return
        Thread runAppThread = _runAppThread;
        if (runAppThread == null)
            return;

        // Interrupt thread
        runAppThread.interrupt();

        // Process termination
        finalizeTermination();
    }

    /**
     * Called to do cleanup when after app is terminated.
     */
    private void finalizeTermination()
    {
        // If already called, just return (possible if soft interrupt somehow finishes after hard thread interrupt has been triggered)
        if (_runAppThread == null) return;

        // Restore System.in/out/err
        System.setIn(REAL_SYSTEM_IN);
        System.setOut(REAL_SYSTEM_OUT);
        System.setErr(REAL_SYSTEM_ERR);

        // Reset thread
        _runAppThread = null;
        _running = false;

        // Notify exited
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appExited(this);
    }

    /**
     * Adds an input string.
     */
    @Override
    public void sendInput(String aString)
    {
        _inputStream.add(aString);
    }

    /**
     * Returns whether run app is source hybrid.
     */
    public boolean isSrcHybrid()
    {
        Project project = _runTool.getProject();
        return project.getBuildFile().isRunWithInterpreter();
    }

    /**
     * Returns the source file for class name.
     */
    public WebFile getSourceFileForClassName(String className)
    {
        Project project = _runTool.getProject();
        return project.getProjectFiles().getJavaFileForClassName(className);
    }

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    private class ProxyPrintStream extends PrintStream {

        /**
         * Constructor.
         */
        public ProxyPrintStream(PrintStream printStream)
        {
            super(printStream);
        }

        /**
         * Override to send to local console.
         */
        public void write(int b)
        {
            // Do normal version
            super.write(b);

            // Write char to console
            String str = String.valueOf(Character.valueOf((char) b));
            appendConsoleOutput(str, this == System.err);
        }

        /**
         * Override to send to local console.
         */
        public void write(byte[] buf, int off, int len)
        {
            // Do normal version
            super.write(buf, off, len);

            // Write buff to console
            String str = new String(buf, off, len);
            appendConsoleOutput(str, this == System.err);
        }
    }
}

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

    // The real system in
    private static final InputStream REAL_SYSTEM_IN = System.in;

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

        // Replace System.in with our own input stream to allow input
        System.setIn(_inputStream = new ScanPane.BytesInputStream(null));

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

        // Restore System.in
        System.setIn(REAL_SYSTEM_IN);

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
}

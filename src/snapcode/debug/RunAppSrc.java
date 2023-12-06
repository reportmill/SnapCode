package snapcode.debug;
import javakit.parse.JFile;
import javakit.parse.JStmt;
import javakit.runner.JavaShell;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcharts.repl.Console;
import snapcharts.repl.DefaultConsole;
import snapcharts.repl.ScanPane;
import snapcode.project.JavaAgent;
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

    /**
     * Constructor.
     */
    public RunAppSrc(WebURL mainClassFileURL)
    {
        super(mainClassFileURL, new String[0]);

        // Create JavaShell
        _javaShell = new JavaShell(this);

        // Create console
        _console = new DefaultConsole();
    }

    @Override
    public void exec()
    {
        // Set console
        Console.setShared(_console);

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

        // Get statements
        JFile jfile = javaAgent.getJFile();
        JStmt[] javaStmts = javaAgent.getJFileStatements();

        // Replace System.in with our own input stream to allow input
        InputStream stdIn = System.in;
        System.setIn(_inputStream = new ScanPane.BytesInputStream(null));

        // Run code
        _javaShell.runJavaCode(jfile, javaStmts);

        // Restore System.in
        System.setIn(stdIn);

        // Reset thread
        _runAppThread = null;
        _running = false;

        // Notify exited
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appExited(this);
    }

    @Override
    public void terminate()
    {
        // If already cancelled, just return
        if (_runAppThread == null) return;

        // Interrupt and clear
        _javaShell.interrupt();
        ViewUtils.runDelayed(() -> cancelRunExtreme(_runAppThread), 600);
    }

    /**
     * Called to really cancel run with thread interrupt, if in system code.
     */
    private void cancelRunExtreme(Thread runAppThread)
    {
        if (runAppThread != null) {
            runAppThread.interrupt();

            // Notify exited
            for (AppListener appLsnr : _appLsnrs)
                appLsnr.appExited(this);
        }
    }

    /**
     * Adds an input string.
     */
    @Override
    public void sendInput(String aString)
    {
        _inputStream.add(aString);
    }
}

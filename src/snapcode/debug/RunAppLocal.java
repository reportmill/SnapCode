package snapcode.debug;
import snap.view.*;
import snap.viewx.Console;
import snapcode.apptools.RunTool;
import snapcode.project.Project;
import snapcode.project.RunConfig;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * This RunApp subclass runs an app in SnapCode process for various unfortunate reasons.
 */
public class RunAppLocal extends RunApp {

    // The thread to reset views
    private Thread _runAppThread;

    // Whether runAppThread is waiting for console app
    private boolean _runAppThreadWaiting;

    // An output stream to write user input to
    private PipedOutputStream _standardInOutputStream;

    // An input stream for standard in
    private InputStream _standardInInputStream;

    // The real system in/out/err
    private static final InputStream REAL_SYSTEM_IN = System.in;
    private static final PrintStream REAL_SYSTEM_OUT = System.out;
    private static final PrintStream REAL_SYSTEM_ERR = System.err;

    /**
     * Constructor.
     */
    public RunAppLocal(RunTool runTool, RunConfig runConfig)
    {
        super(runTool, runConfig);
        _standardInOutputStream = new PipedOutputStream();
        try { _standardInInputStream = new ProxyPipedInputStream(_standardInOutputStream); }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Override to just return main class name.
     */
    @Override
    public String[] getDefaultRunArgs()
    {
        String mainClassName = _runConfig.getMainClassName();
        return new String[] { mainClassName };
    }

    /**
     * Override to run local app.
     */
    @Override
    public void exec()
    {
        // Create and start new thread to run
        _runAppThread = new Thread(this::runAppImpl);
        _running = true;
        _runAppThread.start();
    }

    /**
     * Runs main file main method.
     */
    protected void runAppImpl()
    {
        // Set shared resources
        synchronized (RunAppLocal.class) {

            // Replace System.in with proxy versions to allow input/output
            System.setIn(_standardInInputStream);
            System.setOut(new ProxyPrintStream(REAL_SYSTEM_OUT));
            System.setErr(new ProxyPrintStream(REAL_SYSTEM_ERR));

            // Set console
            Console.setShared(null);
            Console.setConsoleCreatedHandler(this::handleConsoleCreated);
        }

        // Run code
        runMainMethod();

        // Check back after slight delay to terminate if no console was activated
        ViewUtils.runDelayed(this::terminateIfConsoleNotActivated, 200);

        // Wait for explicit termination
        synchronized (this) {
            try {
                _runAppThreadWaiting = true;
                wait();
            }
            catch (Exception e) { throw new RuntimeException(e); }
        }

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

        // If RunAppThreadWaiting (console app), just activate thread
        if (_runAppThreadWaiting) {
            synchronized (this) {
                try { notifyAll(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
            return;
        }

        // Otherwise, hard terminate
        hardTerminate();
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

        // Reset shared resources
        synchronized (RunAppLocal.class) {

            // If another app already set new values, just skip
            if (System.in == _standardInInputStream) {

                // Restore System.in/out/err
                System.setIn(REAL_SYSTEM_IN);
                System.setOut(REAL_SYSTEM_OUT);
                System.setErr(REAL_SYSTEM_ERR);

                // Close standard in output stream (here or before change out?)
                try { _standardInOutputStream.close(); }
                catch (IOException e) { e.printStackTrace(); }

                // Reset Console
                Console.setShared(null);
                Console.setConsoleCreatedHandler(null);
            }
        }

        // Reset thread
        _runAppThread = null;
        _running = false;

        // If console app, clear console
        if (_runAppThreadWaiting)
            setAltConsoleView(null);

        // Notify exited
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appExited(this);
    }

    /**
     * Called after launch to terminate if no console.
     */
    private void terminateIfConsoleNotActivated()
    {
        if (getAltConsoleView() == null)
            terminate();
    }

    /**
     * Adds an input string.
     */
    @Override
    public void sendInput(String aString)
    {
        try {
            _standardInOutputStream.write(aString.getBytes());
            _standardInOutputStream.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Runs the main method.
     */
    private void runMainMethod()
    {
        Class<?> mainClass = getMainClass();
        if (mainClass == null) {
            System.out.println("Can't find main class for: " + getMainFile());
            return;
        }

        // Get main method and invoke
        try {

            // Get main method
            Method mainMethod = getMainMethod(mainClass);
            mainMethod.setAccessible(true);

            // Set target for static or instance
            Object target = null;
            if (!Modifier.isStatic(mainMethod.getModifiers()))
                target = mainClass.getConstructor().newInstance();

            // Invoke with appropriate arg (none or String[])
            if (mainMethod.getParameterTypes().length == 0)
                mainMethod.invoke(target);
            else mainMethod.invoke(target, (Object) new String[0]);
        }

        // Handle exception: Just print - goes to RunTool console
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the main class.
     */
    private Class<?> getMainClass()
    {
        String className = getMainClassName();
        Project project = getMainFileProject();
        ClassLoader classLoader = project.getRuntimeClassLoader();

        // Do normal Class.forName
        try { return Class.forName(className, false, classLoader); }

        // Handle Exceptions
        catch(ClassNotFoundException e) { return null; }
        catch(NoClassDefFoundError t) { System.err.println("RunAppLocal.getMainClass: " + t); return null; }
        catch(Throwable t) { System.err.println("RunAppLocal.getMainClass: Unknown error: " + t); return null; }
    }

    /**
     * Returns the main method for given main class, using main() method conventions.
     */
    private static Method getMainMethod(Class<?> mainClass) throws NoSuchMethodException
    {
        // Try: static void main(String[])
        Method mainWithArgs = null;
        try { mainWithArgs = mainClass.getDeclaredMethod("main", String[].class); }
        catch (NoSuchMethodException ignore) { }
        if (mainWithArgs != null && Modifier.isStatic(mainWithArgs.getModifiers()) && mainWithArgs.getReturnType() == void.class)
            return mainWithArgs;

        // Try: static void main()
        Method mainNoArgs = null;
        try { mainNoArgs = mainClass.getDeclaredMethod("main"); }
        catch (NoSuchMethodException ignore) { }
        if (mainNoArgs != null && Modifier.isStatic(mainNoArgs.getModifiers()) && mainNoArgs.getReturnType() == void.class)
            return mainNoArgs;

        // Try: void main(String[])
        if (mainWithArgs != null && mainWithArgs.getReturnType() == void.class)
            return mainWithArgs;

        // Try: void main()
        if (mainNoArgs != null && mainNoArgs.getReturnType() == void.class)
            return mainNoArgs;

        throw new NoSuchMethodException(mainClass.getName() + ".main()");
    }

    /**
     * Called when Console is created.
     */
    private void handleConsoleCreated()
    {
        View consoleView = Console.getShared().getConsoleView();
        setAltConsoleView(consoleView);
        Console.setConsoleCreatedHandler(null);
    }

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    private class ProxyPrintStream extends PrintStream {

        /** Constructor. */
        public ProxyPrintStream(PrintStream printStream)
        {
            super(printStream);
        }

        /** Override to send to local console. */
        public void write(int b)
        {
            super.write(b);
            String str = String.valueOf(Character.valueOf((char) b));
            appendConsoleOutput(str, this == System.err);
        }

        /** Override to send to local console. */
        public void write(byte[] buf, int off, int len)
        {
            super.write(buf, off, len);
            String str = new String(buf, off, len);
            appendConsoleOutput(str, this == System.err);
        }
    }

    /**
     * A PipedInputStream - because something weird is happening.
     */
    private static class ProxyPipedInputStream extends PipedInputStream {

        public ProxyPipedInputStream(PipedOutputStream out) throws IOException { super(out); }

        @Override
        public int read(byte[] theBytes, int offset, int length) throws IOException
        {
            // This should be impossible, but it is happening in Java 25
            if (this != System.in)
                return System.in.read(theBytes, offset, length);
            return super.read(theBytes, offset, length);
        }
    }
}

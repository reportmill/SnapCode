package snapcode.debug;
import snap.view.View;
import snap.view.ViewUtils;
import snap.viewx.Console;
import snapcode.apptools.RunTool;
import snapcode.project.Project;
import snapcode.project.RunConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This RunApp subclass runs an app from source.
 */
public class RunAppSrc extends RunApp {

    // The thread to reset views
    private Thread _runAppThread;

    // Whether runAppThread is waiting for console app
    private boolean _runAppThreadWaiting;

    // An input stream for standard in
    private BytesInputStream _standardInInputStream;

    // The real system in/out/err
    private static final InputStream REAL_SYSTEM_IN = System.in;
    private static final PrintStream REAL_SYSTEM_OUT = System.out;
    private static final PrintStream REAL_SYSTEM_ERR = System.err;

    /**
     * Constructor.
     */
    public RunAppSrc(RunTool runTool, RunConfig runConfig)
    {
        super(runTool, runConfig);
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
     * Override to run source app.
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
     * Runs Java Code.
     */
    protected void runAppImpl()
    {
        // Set shared resources
        synchronized (RunAppSrc.class) {

            // Replace System.in with proxy versions to allow input/output
            System.setIn(_standardInInputStream = new BytesInputStream());
            System.setOut(new ProxyPrintStream(REAL_SYSTEM_OUT));
            System.setErr(new ProxyPrintStream(REAL_SYSTEM_ERR));

            // Set console
            Console.setShared(null);
            Console.setConsoleCreatedHandler(this::consoleWasCreated);
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
        synchronized (RunAppSrc.class) {

            // If another app already set new values, just skip
            if (System.in == _standardInInputStream) {

                // Restore System.in/out/err
                System.setIn(REAL_SYSTEM_IN);
                System.setOut(REAL_SYSTEM_OUT);
                System.setErr(REAL_SYSTEM_ERR);

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
        _standardInInputStream.addString(aString);
    }

    /**
     * Runs the main method.
     */
    private void runMainMethod()
    {
        // Get main method and invoke
        try {
            Class<?> mainClass = getMainClass();
            if (mainClass == null) {
                System.out.println("Can't find main class for: " + getMainFile());
                return;
            }
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
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
        catch(NoClassDefFoundError t) { System.err.println("ClassUtils.getClass: " + t); return null; }
        catch(Throwable t) { System.err.println("ClassUtils.getClass: Unknown error: " + t); return null; }
    }

    /**
     * Called when Console is created.
     */
    private void consoleWasCreated()
    {
        View consoleView = Console.getShared().getConsoleView();
        setAltConsoleView(consoleView);
        Console.setConsoleCreatedHandler(null);
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

    /**
     * An InputStream that lets you add bytes on the fly.
     */
    private static class BytesInputStream extends InputStream {

        // The byte array to write to
        private byte[] _writeBytesBuffer = new byte[0];

        // The byte array to read from
        private byte[] _readBytesBuffer = new byte[1];

        // The index of the next character to read
        private int _readBytesIndex;

        // The currently marked position
        private int _markedIndex;

        // The number of bytes write bytes.
        private int _writeBytesLength;

        // Whether waiting for more input
        private boolean  _waiting;

        /** Constructor */
        public BytesInputStream()
        {
            super();
        }

        /** Adds string to stream. */
        public void addString(String aStr)
        {
            addBytes(aStr.getBytes());
        }

        /** Adds bytes to stream. */
        public void addBytes(byte[] addBytes)
        {
            // Add new bytes to write buffer
            int oldLength = _writeBytesBuffer.length;
            _writeBytesBuffer = Arrays.copyOf(_writeBytesBuffer, oldLength + addBytes.length);
            System.arraycopy(addBytes, 0, _writeBytesBuffer, oldLength, addBytes.length);
            _writeBytesLength = _writeBytesBuffer.length;

            // If waiting, wake up
            if (_waiting) {
                synchronized (this) {
                    try { notifyAll(); _waiting = false; }
                    catch(Exception e) { throw new RuntimeException(e); }
                }
            }
        }

        /** Reads the next byte of data from this input stream. */
        @Override
        public int read()
        {
            int len = read(_readBytesBuffer, 0, 1);
            return len > 0 ? _readBytesBuffer[0] : -1;
        }

        /** Reads up to <code>len</code> bytes of data into an array of bytes from this input stream. */
        @Override
        public int read(byte[] theBytes, int offset, int length)
        {
            while (_readBytesIndex >= _writeBytesLength) {
                synchronized (this) {
                    try { _waiting = true; wait(); }
                    catch(Exception ignore) { }
                }
            }

            int availableBytesCount = _writeBytesLength - _readBytesIndex;
            if (length > availableBytesCount)
                length = availableBytesCount;
            if (length <= 0)
                return 0;
            System.arraycopy(_writeBytesBuffer, _readBytesIndex, theBytes, offset, length);
            _readBytesIndex += length;
            return length;
        }

        /** Skips <code>n</code> bytes of input from this input stream. */
        @Override
        public synchronized long skip(long n)
        {
            long k = _writeBytesLength - _readBytesIndex;
            if (n < k) {
                k = n < 0 ? 0 : n;
            }
            _readBytesIndex += k;
            return k;
        }

        /** Returns the number of remaining bytes that can be read (or skipped over) from this input stream. */
        public synchronized int available() { return _writeBytesLength - _readBytesIndex; }

        /** Tests if this <code>InputStream</code> supports mark/reset. */
        public boolean markSupported() { return true; }

        /** Set the current marked position in the stream. */
        public void mark(int readAheadLimit) { _markedIndex = _readBytesIndex; }

        /** Resets the buffer to the marked position. */
        public synchronized void reset() { _readBytesIndex = _markedIndex; }

        /** Closing a <tt>BytesArrayInputStream</tt> has no effect. */
        public void close() throws IOException  { }
    }
}

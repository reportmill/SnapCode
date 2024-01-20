package snapcode.debug;
import snap.gfx.GFXEnv;
import snap.util.StringUtils;
import snap.web.WebFile;
import snapcode.apptools.RunTool;
import java.io.*;

/**
 * A class to run an external process.
 */
public class RunAppWeb extends RunApp {

    // The process
    protected Process _process;

    // The system out reader thread
    private StreamReaderThread _outReaderThread;

    // The system err reader thread
    //private RunAppBin.StreamReaderThread _errReaderThread;

    // The writer to process stdin
    private BufferedWriter _stdInWriter;

    /**
     * Constructor for URL and args.
     */
    public RunAppWeb(RunTool runTool, WebFile mainFile, String[] theArgs)
    {
        super(runTool, mainFile, theArgs);
    }

    /**
     * Executes the given command + args.
     */
    @Override
    public void exec()
    {
        // Print exec args
        String[] args = getArgs();
        String argsStr = StringUtils.join(args, " ") + '\n';
        appendOut(argsStr);

        // Run process
        try {
            _process = (Process) GFXEnv.getEnv().execProcess(args);
            _running = true;
            startSystemConsoleReaders();
        }

        // Handle exceptions
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Terminates the process.
     */
    @Override
    public void terminate()
    {
        if (!_running)
            return;
        _running = false;
        _process.destroy();
        _terminated = true;
    }

    /**
     * Starts threads to read system console (out/err).
     */
    protected void startSystemConsoleReaders()
    {
        // Start standard out reader
        InputStream standardOutInputStream = _process.getInputStream();
        _outReaderThread = new StreamReaderThread(standardOutInputStream, false);
        _outReaderThread.start();
//
//        // Start standard err reader
//        InputStream standardErrInputStream = _process.getErrorStream();
//        _errReaderThread = new StreamReaderThread(standardErrInputStream, true);
//        _errReaderThread.start();
    }

    /**
     * Sends input to process.
     */
    @Override
    public void sendInput(String aStr)
    {
//        // If writer not yet set, create and set
//        if (_stdInWriter == null) {
//            OutputStream standardInOutputStream = _process.getOutputStream();
//            OutputStreamWriter standardInOutputStreamWriter = new OutputStreamWriter(standardInOutputStream);
//            _stdInWriter = new BufferedWriter(standardInOutputStreamWriter);
//        }
//
//        // Append to output
//        appendConsoleOutput(aStr, false);
//
//        // Write and flush
//        try {
//            _stdInWriter.write(aStr);
//            _stdInWriter.flush();
//        }
//
//        // Handle exceptions
//        catch (Exception e) {
//            appendErr("RunApp.sendInput: Failed to write to process: " + e);
//        }
    }

    /**
     * An inner class to read from an input stream in a separate thread to a string buffer.
     */
    private class StreamReaderThread extends Thread {

        // The input stream
        private InputStream _inputStream;

        // Whether this is error stream
        private boolean _isErr;

        /**
         * Constructor.
         */
        StreamReaderThread(InputStream anIS, boolean isErr)
        {
            _inputStream = anIS;
            _isErr = isErr;
            setPriority(Thread.MAX_PRIORITY - 1);
        }

        /**
         * Thread: run().
         */
        @Override
        public void run()
        {
            try {

                // Get input stream reader
                InputStreamReader inputStreamReader = new InputStreamReader(_inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                char[] chars = new char[1024];

                // Read until end
                for (int len = bufferedReader.read(chars, 0, 1024); len >= 0; len = bufferedReader.read(chars, 0, 1024)) {
                    String line = new String(chars, 0, len);
                    appendConsoleOutput(line, _isErr);
                }

                // Terminate app
                if (!_terminated) {
                    _running = false;
                    _terminated = true;
                    notifyAppExited();
                }
            }

            // Handle exception
            catch (IOException ioe) { ioe.printStackTrace(); }
        }
    }
}
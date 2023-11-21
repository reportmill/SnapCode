package snapcode.debug;
import snap.util.StringUtils;
import snap.web.WebURL;
import java.io.*;

/**
 * A class to run an external process.
 */
public class RunAppBin extends RunApp {

    // The process
    protected Process _process;

    // The writer to process stdin
    private BufferedWriter _stdInWriter;

    // The readers
    private StreamReader _outReader, _errReader;

    /**
     * Constructor for URL and args.
     */
    public RunAppBin(WebURL aURL, String[] theArgs)
    {
        super(aURL, theArgs);
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
            File workingDir = getWorkingDirectory();
            _process = Runtime.getRuntime().exec(args, null, workingDir);
            _running = true;
            startProcessReaders();
        }

        // Catch exceptions
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Returns the running process.
     */
    public Process getProcess()  { return _process; }

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
     * Terminates the process.
     */
    @Override
    public void terminate()  { _process.destroy(); }

    /**
     * Sends input to process.
     */
    @Override
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
}
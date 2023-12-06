package snapcode.debug;
import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import java.io.*;
import java.util.Map;

/**
 * Debug Utils.
 */
public class DebugUtils {

    /**
     * Creates a VM for VM args and a command line.
     */
    public static VirtualMachine getVM(String theVMArgs, String cmdLine, RunApp runApp)
    {
        VirtualMachineManager manager = Bootstrap.virtualMachineManager();
        LaunchingConnector connector = manager.defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("options").setValue(theVMArgs);
        args.get("main").setValue(cmdLine);  // Should probably figure out how to specify 'localhost'

        // Return general version
        return generalGetVM(connector, args, runApp);
    }

    private static VirtualMachine generalGetVM(LaunchingConnector connector, Map<String, Connector.Argument> args, RunApp runApp)
    {
        VirtualMachine vm = null;
        try {
            runApp.printDiagnostic("Starting child.");
            vm = connector.launch(args);
        }
        catch (IOException | IllegalConnectorArgumentsException ioe) {
            runApp.printDiagnostic("Unable to start child: " + ioe.getMessage());
        }
        catch (VMStartException vmse) {
            runApp.printDiagnostic("Unable to start child: " + vmse.getMessage() + '\n');
            dumpFailedLaunchInfo(runApp, vmse.process());
        }

        // Return
        return vm;
    }

    /**
     * dumpFailedLaunchInfo.
     */
    private static void dumpFailedLaunchInfo(RunApp runApp, Process process)
    {
        try {
            dumpStream(runApp, process.getErrorStream());
            dumpStream(runApp, process.getInputStream());
        }
        catch (IOException e) {
            runApp.printDiagnostic("Unable to display process output: " + e.getMessage());
        }
    }

    /**
     * dumpStream.
     */
    private static void dumpStream(RunApp runApp, InputStream stream) throws IOException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        for (String line = in.readLine(); line != null; line = in.readLine())
            runApp.printDiagnostic(line);
    }

    /**
     * Return the thread status description.
     */
    public static String getStatus(ThreadReference threadReference)
    {
        int status = threadReference.status();
        String result;
        switch (status) {
            case ThreadReference.THREAD_STATUS_UNKNOWN: result = "unknown status"; break;
            case ThreadReference.THREAD_STATUS_ZOMBIE: result = "zombie"; break;
            case ThreadReference.THREAD_STATUS_RUNNING: result = "running"; break;
            case ThreadReference.THREAD_STATUS_SLEEPING: result = "sleeping"; break;
            case ThreadReference.THREAD_STATUS_MONITOR: result = "waiting to acquire a monitor lock"; break;
            case ThreadReference.THREAD_STATUS_WAIT: result = "waiting on a condition"; break;
            default: result = "<invalid thread status>";
        }

        // If suspended, add text
        if (threadReference.isSuspended())
            result += " (suspended)";

        // Return
        return result;
    }

    /**
     * Return a description of an object.
     */
    public static String description(ObjectReference ref)
    {
        ReferenceType clazz = ref.referenceType();
        long id = ref.uniqueID();  //### TODO use real id
        if (clazz == null)
            return toHex(id);
        return "(" + clazz.name() + ")" + toHex(id);
    }

    /**
     * Convert a long to a hexadecimal string.
     */
    public static String toHex(long n)
    {
        // Store digits in reverse order.
        char[] s1 = new char[16], s2 = new char[18];
        int i = 0;
        do {
            long d = n & 0xf;
            s1[i++] = (char) ((d < 10) ? ('0' + d) : ('a' + d - 10));
        } while ((n >>>= 4) > 0);

        // Now reverse the array.
        s2[0] = '0';
        s2[1] = 'x';
        int j = 2;
        while (--i >= 0) s2[j++] = s1[i];
        return new String(s2, 0, j);
    }

    /**
     * Convert hexadecimal strings to longs.
     */
    public static long fromHex(String hexStr)
    {
        String str = hexStr.startsWith("0x") ? hexStr.substring(2).toLowerCase() : hexStr.toLowerCase();
        if (hexStr.length() == 0)
            throw new NumberFormatException();

        long ret = 0;
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            if (c >= '0' && c <= '9') ret = (ret * 16) + (c - '0');
            else if (c >= 'a' && c <= 'f') ret = (ret * 16) + (c - 'a' + 10);
            else throw new NumberFormatException();
        }
        return ret;
    }

    /**
     * The next two methods are used by this class and by EventHandler
     * to print consistent locations and error messages.
     */
    public static String locationString(Location loc)
    {
        return loc.declaringType().name() + "." + loc.method().name() + "(), line=" + loc.lineNumber();
    }

    public static boolean isValidMethodName(String s)
    {
        return isJavaIdentifier(s) || s.equals("<init>") || s.equals("<clinit>");
    }

    public static boolean isJavaIdentifier(String s)
    {
        if (s.length() == 0) return false;
        int cp = s.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp))
            return false;
        for (int i = Character.charCount(cp); i < s.length(); i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp))
                return false;
        }
        return true;
    }

    /**
     * A thread subclass to forward text from InputListener to given PrintWriter.
     */
    public static class InputWriterThread extends Thread {

        // PrintWriter
        private PrintWriter _printWriter;

        // InputListener
        private InputListener _inputLsnr;

        InputWriterThread(String aName, PrintWriter printWriter, InputListener inputListener)
        {
            super(aName);
            _printWriter = printWriter;
            _inputLsnr = inputListener;
            setPriority(Thread.MAX_PRIORITY - 1);
        }

        public void run()
        {
            while (true) {
                String line = _inputLsnr.getLine();
                _printWriter.println(line);
                _printWriter.flush(); // Should not be needed for println above!
            }
        }
    }

    /**
     * An interface for objects providing input.
     */
    public interface InputListener {
        String getLine();
    }
}
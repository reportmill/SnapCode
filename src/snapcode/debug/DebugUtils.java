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
    public static String getStatusForThreadReference(ThreadReference threadReference)
    {
        int status = threadReference.status();
        String result = switch (status) {
            case ThreadReference.THREAD_STATUS_UNKNOWN -> "unknown status";
            case ThreadReference.THREAD_STATUS_ZOMBIE -> "zombie";
            case ThreadReference.THREAD_STATUS_RUNNING -> "running";
            case ThreadReference.THREAD_STATUS_SLEEPING -> "sleeping";
            case ThreadReference.THREAD_STATUS_MONITOR -> "waiting to acquire a monitor lock";
            case ThreadReference.THREAD_STATUS_WAIT -> "waiting on a condition";
            default -> "<invalid thread status>";
        };

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
            return "0x" + Long.toHexString(id);
        return "(" + clazz.name() + ")0x" + Long.toHexString(id);
    }

    /**
     * The next two methods are used by this class and by EventHandler
     * to print consistent locations and error messages.
     */
    public static String locationString(Location loc)
    {
        return loc.declaringType().name() + "." + loc.method().name() + "(), line=" + loc.lineNumber();
    }

    /**
     * Returns whether given name is valid method name.
     */
    public static boolean isValidMethodName(String methodName)
    {
        return isJavaIdentifier(methodName) || methodName.equals("<init>") || methodName.equals("<clinit>");
    }

    /**
     * Returns whether given string is Java identifier.
     */
    public static boolean isJavaIdentifier(String string)
    {
        if (string.isEmpty()) return false;
        int cp = string.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp))
            return false;
        for (int i = Character.charCount(cp); i < string.length(); i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
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
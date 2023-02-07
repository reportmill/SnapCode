package snapcode.debug;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;

/**
 * A custom class.
 */
public class DebugFrame {

    // The DebugThread
    DebugThread _thread;

    // The StackFrame
    StackFrame _frame;

    // The index
    int _index;

    // The description string
    String _desc;

    /**
     * Creates a new DebugFrame for thread, frame and index.
     */
    public DebugFrame(DebugThread aDT, StackFrame aFrm, int anIndex)
    {
        _thread = aDT;
        _frame = aFrm;
        _index = anIndex;
    }

    /**
     * Returns the DebugThread.
     */
    public DebugThread getThread()
    {
        return _thread;
    }

    /**
     * Returns the StackFrame.
     */
    public StackFrame getFrame()
    {
        return _frame;
    }

    /**
     * Returns the StackFrame index.
     */
    public int getIndex()
    {
        return _index;
    }

    /**
     * Returns the frame source path.
     */
    public String getSourcePath()
    {
        try {
            Location loc = _frame.location();
            return '/' + loc.sourcePath();
        } catch (Exception e) {
            System.err.println("DebugFrame.getSourcePath Failed: " + e);
            return null;
        }
    }

    /**
     * Returns the frame line number.
     */
    public int getLineNumber()
    {
        try {
            Location loc = _frame.location();
            return loc.lineNumber();
        } catch (Exception e) {
            System.err.println("DebugFrame.getLineNumber Failed: " + e);
            return -1;
        }
    }

    /**
     * Returns a description of a stack frame.
     */
    public String getDescription()
    {
        if (_desc != null) return _desc;
        try {
            Location loc = _frame.location();
            Method method = loc.method();
            StringBuffer sb = new StringBuffer(loc.sourceName()).append('.').append(method.name()).append('(');
            for (String type : method.argumentTypeNames()) sb.append(type).append(',');
            if (method.argumentTypeNames().size() > 0) sb.delete(sb.length() - 1, sb.length());
            sb.append(") line:").append(loc.lineNumber());
            return _desc = sb.toString();
        } catch (Exception e) {
            return "StackFrame read error: " + e;
        }
    }

    /**
     * Makes this frame the current frame.
     */
    public void select()
    {
        DebugThread thread = getThread();
        DebugApp dapp = thread.getApp();
        dapp.setFrame(this);
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        DebugFrame other = anObj instanceof DebugFrame ? (DebugFrame) anObj : null;
        if (other == null) return false;
        return _frame == other._frame;
    }

}
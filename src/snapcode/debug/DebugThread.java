package snapcode.debug;

import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;

import java.util.List;

/**
 * A custom class.
 */
public class DebugThread implements Comparable<DebugThread> {

    // The DebugApp
    DebugApp _app;

    // The ThreadReference
    ThreadReference _tref;

    // The thread name
    String _name;

    // An empty DebugFrame array
    static DebugFrame[] NO_FRAMES = new DebugFrame[0];

    /**
     * Creates a new DebugThread for given DebugApp and ThreadReference.
     */
    public DebugThread(DebugApp aDApp, ThreadReference aTRef)
    {
        _app = aDApp;
        _tref = aTRef;
    }

    /**
     * Returns the DebugApp.
     */
    public DebugApp getApp()
    {
        return _app;
    }

    /**
     * Returns the thread name.
     */
    public String getName()
    {
        if (_name != null) return _name;
        try {
            return _name = _tref.name();
        } catch (Exception e) {
            System.err.println("DebugThread.getName: " + e);
            return _name = "Error";
        }
    }

    /**
     * Returns whether thread is suspended.
     */
    public boolean isSuspended()
    {
        try {
            return _tref.isSuspended();
        } catch (Exception e) {
            System.err.println("DebugThread.isSuspended: " + e);
            return false;
        }
    }

    /**
     * Returns the stack frames.
     */
    public DebugFrame[] getFrames()
    {
        if (!isSuspended()) return NO_FRAMES;
        try {
            List<StackFrame> frames = _tref.frames();
            DebugFrame[] dframes = new DebugFrame[frames.size()];
            for (int i = 0, iMax = frames.size(); i < iMax; i++) dframes[i] = new DebugFrame(this, frames.get(i), i);
            return dframes;
        } catch (Exception e) {
            return NO_FRAMES;
        }
    }

    /**
     * Returns a comparison of thread.
     */
    public int compareTo(DebugThread anObj)
    {
        if (anObj == null) return -1;
        return getName().compareTo(anObj.getName());
    }

    /**
     * Standard hashCode implementation.
     */
    public int hashCode()
    {
        return _tref.hashCode();
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        DebugThread other = anObj instanceof DebugThread ? (DebugThread) anObj : null;
        if (other == null) return false;
        return _tref == other._tref || getName().equals(other.getName());
    }

}
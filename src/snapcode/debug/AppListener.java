package snapcode.debug;

/**
 * An interface for objects wanting notification of change of Session status.
 */
public interface AppListener {

    /**
     * App started.
     */
    void appStarted(RunApp em);

    /**
     * App paused.
     */
    void appPaused(DebugApp em);

    /**
     * App resumed.
     */
    void appResumed(DebugApp em);

    /**
     * App exited.
     */
    void appExited(RunApp em);

    /**
     * FrameChanged.
     */
    void frameChanged(DebugApp anApp);

    /**
     * DebugEvent.
     */
    void processDebugEvent(DebugApp anApp, DebugEvent e);
}

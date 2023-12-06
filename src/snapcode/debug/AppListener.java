package snapcode.debug;

/**
 * An interface for objects wanting notification of change of Session status.
 */
public interface AppListener {

    /**
     * App started.
     */
    void appStarted(RunApp runApp);

    /**
     * App paused.
     */
    void appPaused(DebugApp runApp);

    /**
     * App resumed.
     */
    void appResumed(DebugApp runApp);

    /**
     * App exited.
     */
    void appExited(RunApp runApp);

    /**
     * FrameChanged.
     */
    void frameChanged(DebugApp runApp);

    /**
     * DebugEvent.
     */
    void processDebugEvent(DebugApp runApp, DebugEvent e);
}

package snapcode.debug;
import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.*;
import snap.util.ListUtils;
import snapcode.project.Breakpoint;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import snapcode.debug.Exceptions.*;

/**
 * This class manages a DebugApp.
 * Originated from pieces of debug.gui.ContentManager/Environment/CommandInterpreter and debug.bdi.ExecutionManager.
 */
public class DebugApp extends RunAppBin {

    // The virtual machine
    VirtualMachine _vm;

    // Current thread
    ThreadReference _currentThread;

    // Current frame index
    int _frameIndex;

    // all specs
    //List <BreakpointReq>          _eventRequestSpecs = Collections.synchronizedList(new ArrayList());
    BreakpointReq[] _eventRequestSpecs = new BreakpointReq[0];

    // Event source thread
    JDIEventDispatcher _eventDispatchThread;

    // Whether to print events
    boolean _printEVs;

    // Whether app is invoking methods
    boolean _invoking;

    // The current RunToLine breakpoint
    Breakpoint _runToLineBreak;

    // Constants for method types
    static final int STATIC = 0;
    static final int INSTANCE = 1;

    // Constants for method matching
    static final int SAME = 0;
    static final int ASSIGNABLE = 1;
    static final int DIFFERENT = 2;

    /**
     * Creates a new DebugApp.
     */
    public DebugApp(WebURL aURL, String[] args)
    {
        super(aURL, args);
    }

    /**
     * Start a new VM.
     */
    @Override
    public void exec()
    {
        // Get class name (if no class name, complain and return)
        String cname = getMainClassName();
        if (cname.equals("")) {
            failure("No main class specifed and no current default defined.");
            return;
        }

        // Run process and return true - was run(suspended, vmArgs, className, args);
        try {
            endSession();

            // Get command line, create session and start it
            String vmArgs = getVmArgs();
            String appArgs = getAppArgs();
            String cmdLine = cname + " " + appArgs;
            _vm = Utils.getVM(vmArgs, cmdLine, _diagnostics);
            _running = true;
            startSession();
            startSystemConsoleReaders();
        }

        // Complain on VMLaunchFailureException and return false
        catch (VMLaunchFailureException e) {
            failure("Attempt to launch main class \"" + cname + "\" failed.");
        }
    }

    /**
     * Detach.
     */
    @Override
    public void terminate()
    {
        try {
            ensureActiveSession();
            endSession();
        } catch (Exception e) {
            failure("Failure to detach: " + e.getMessage());
        }
    }

    /**
     * Start session.
     */
    private void startSession() throws VMLaunchFailureException
    {
        // Get process
        _process = _vm.process();

        // Start InputWriter
        PrintWriter in = new PrintWriter(new OutputStreamWriter(_process.getOutputStream()));
        Utils.InputWriter inputWriter = new Utils.InputWriter("input writer", in, _appInput);
        inputWriter.setPriority(Thread.MAX_PRIORITY - 1);
        inputWriter.start();

        _vm.setDebugTraceMode(VirtualMachine.TRACE_NONE);
        notice("Connected to VM");
        _eventDispatchThread = new JDIEventDispatcher();
        _eventDispatchThread.start();

        // We must allow the deferred breakpoints to be resolved before we continue executing the class.  We could
        // optimize if there were no deferred breakpoints outstanding for a particular class. Can we do this with JDI?
        EventRequestManager em = _vm.eventRequestManager();
        ClassPrepareRequest classPrepareRequest = em.createClassPrepareRequest();
        classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        classPrepareRequest.enable();
        ClassUnloadRequest classUnloadRequest = em.createClassUnloadRequest();
        classUnloadRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        classUnloadRequest.enable();
        ThreadStartRequest threadStartRequest = em.createThreadStartRequest();
        threadStartRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        threadStartRequest.enable();
        ThreadDeathRequest threadDeathRequest = em.createThreadDeathRequest();
        threadDeathRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        threadDeathRequest.enable();
        ExceptionRequest exceptionRequest = em.createExceptionRequest(null, false, true);
        exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        exceptionRequest.enable();

        // Notify session start
        notifyAppStarted();
    }

    /**
     * End session.
     */
    protected void endSession()
    {
        if (!_running) return;

        if (_eventDispatchThread != null) {
            _eventDispatchThread.interrupt();
            _eventDispatchThread = null;
            //### The VM may already be disconnected if the debuggee did a System.exit(). Exception handler here is a
            //### kludge, Rather, there are many other places where we need to handle this exception, and initiate a
            //### detach due to an error condition, e.g., connection failure.
            try {
                _vm.dispose();
            }
            catch (VMDisconnectedException ignore) { }
            notice("Disconnected from VM");
        }

        // Destroy process
        if (_process != null) { // inputWriter.quit(); outputReader.quit();  errorReader.quit();
            _process.destroy();
            _process = null;
        }

        _running = false;
        setPaused(false);
        _terminated = true;
        setCurrentThread(null, -1);
        notifyAppExited();
    }

    /**
     * Pause the app.
     */
    public void pause()
    {
        try {
            ensureActiveSession();
            setPaused(true);
            _vm.suspend();
            notifyAppPaused();
        }

        // Failure
        catch (Exception e) {
            failure("Failure to interrupt: " + e.getMessage());
            setPaused(false);
        }
    }

    /**
     * Resume the app.
     */
    public void resume()
    {
        resumeQuiet();
        notifyAppResumed();
    }

    /**
     * Resume the app.
     */
    public void resumeQuiet()
    {
        try {
            ensureActiveSession();
            setPaused(false);
            setCurrentThread(_currentThread, -1);
            _vm.resume();
        }

        // Failure  //catch(VMNotInterruptedException e) { notice("Target VM is already running."); } //### failure?
        catch (Exception e) {
            failure("Failure to resume: " + e.getMessage());
            setPaused(true);
        }
    }

    public void reset()
    {
        _paused = true;
        setCurrentThread(_currentThread, 0);
    }

    /**
     * Step into line.
     */
    public void stepIntoLine()
    {
        generalStep(StepRequest.STEP_LINE, StepRequest.STEP_INTO);
    }

    /**
     * Step into instruction.
     */
    public void stepIntoInstruction()
    {
        generalStep(StepRequest.STEP_MIN, StepRequest.STEP_INTO);
    }

    /**
     * Step out.
     */
    public void stepOverLine()
    {
        generalStep(StepRequest.STEP_LINE, StepRequest.STEP_OVER);
    }

    /**
     * Step out.
     */
    public void stepOverInstruction()
    {
        generalStep(StepRequest.STEP_MIN, StepRequest.STEP_OVER);
    }

    /**
     * Step out.
     */
    public void stepOut()
    {
        generalStep(StepRequest.STEP_MIN, StepRequest.STEP_OUT);
    }

    /**
     * General purpose step method.
     */
    private void generalStep(int size, int depth)
    {
        ThreadReference thread = getCurrentThread();
        if (thread == null) {
            failure("No current thread.");
            return;
        }

        try { generalStep(thread, size, depth); }
        catch (Exception e) { failure("Failure to step: " + e.getMessage()); }
    }

    /**
     * General purpose step method.
     */
    private void generalStep(ThreadReference thread, int size, int depth) throws NoSessionException
    {
        ensureActiveSession();
        setPaused(false);

        // Create step request
        clearPreviousStep(thread);
        EventRequestManager reqMgr = _vm.eventRequestManager();
        StepRequest request = reqMgr.createStepRequest(thread, size, depth);

        // We want just the next step event and no others
        request.addCountFilter(1);
        request.enable();

        // Resume
        resumeQuiet();
    }

    /**
     * Run to given line.
     */
    public void runToLine(WebFile aFile, int aLine)
    {
        _runToLineBreak = new Breakpoint(aFile, aLine);
        addBreakpoint(_runToLineBreak);
        resume();
    }

    /**
     * Ensures active session.
     */
    void ensureActiveSession() throws NoSessionException
    {
        if (!_running)
            throw new NoSessionException();
    }

    /*
     * Stepping.
     */
    void clearPreviousStep(ThreadReference thread)
    {
        // A previous step may not have completed on this thread; if so, it gets removed here.
        EventRequestManager mgr = _vm.eventRequestManager();
        List<StepRequest> requests = mgr.stepRequests();
        for (StepRequest request : requests) {
            if (request.thread().equals(thread)) {
                mgr.deleteEventRequest(request);
                break;
            }
        }
    }

    /**
     * Returns the current 'this' object.
     */
    public ObjectReference thisObject()
    {
        StackFrame frame = getCurrentFrame();
        ObjectReference thisObj = frame.thisObject();
        return thisObj;
    }

    /**
     * ToString.
     */
    public String toString(Value aVal)
    {
        // Handle StringReference: Just return string
        if (aVal instanceof StringReference)
            return ((StringReference) aVal).value();

        // Handle ArrayReference: Concatenate values
        if (aVal instanceof ArrayReference) {
            ArrayReference arrayRef = (ArrayReference) aVal;
            List<Value> values = arrayRef.getValues();
            String valuesStr = ListUtils.mapAndJoinStrings(values, this::toString, ", ");
            return '[' + valuesStr + ']';
        }

        // Handle ObjectReference: Invoke toString() method
        if (aVal instanceof ObjectReference) {
            ObjectReference oref = (ObjectReference) aVal;
            Value val = invokeMethod(oref, "toString", Collections.EMPTY_LIST);
            return toString(val);
        }

        // Handle Anything else (?)
        return aVal != null ? aVal.toString() : "(null)";
    }

    /**
     * Invoke method.
     */
    public Value invokeMethod(ObjectReference anOR, String aName, List<Value> args)
    {
        ThreadReference thread = getCurrentThread();
        if (thread == null) {
            failure("No current thread.");
            return null;
        }

        // Find method for name and args
        ReferenceType refType = anOR.referenceType();  // Thread invalid after invokeMethod which resumes thread
        List<Method> methods = methods(refType, aName, INSTANCE);
        Method method = method(methods, args);
        if (method == null)
            return null;

        // Invoke method
        try {
            _invoking = true;
            Value val = anOR.invokeMethod(thread, method, args, 0); //ObjectReference.INVOKE_NONVIRTUAL
            return val;
        }

        // Catch exceptions
        catch (Exception e) {
            System.out.println("DebugApp.invokeMethod: Error invoking method: " + method + ", " + e);
            setPaused(true);
            return null;
        }

        // Turn off invoking
        finally {
            _invoking = false;
        }
    }

    /**
     * Returns the DebugThreads for a DebugApp.
     */
    public DebugThread[] getThreads()
    {
        // Get threads, sort (should trigger VMDisconnectedException if VM has disconnected) and return
        try {
            ThreadReference[] threads = _vm.allThreads().toArray(EMPTY_THREADREFS);
            DebugThread[] dthreads = new DebugThread[threads.length];
            for (int i = 0, iMax = threads.length; i < iMax; i++) dthreads[i] = getThread(threads[i]);
            Arrays.sort(dthreads);
            return dthreads;
        }

        // If there is no session or VM is dead, just returns empty list
        catch (Exception e) {
            System.err.println("DebugApp.getThreads: " + e);
            return new DebugThread[0];
        }
    }

    /**
     * Returns a DebugThread instance for given ThreadReference.
     */
    private DebugThread getThread(ThreadReference aTR)
    {
        return new DebugThread(this, aTR);
    }

    private static final ThreadReference[] EMPTY_THREADREFS = new ThreadReference[0];

    /**
     * Returns a list of ThreadReference objects corresponding to the threads that are currently active in the VM.
     * A thread is removed from the list just before the thread terminates.
     */
    public List<ThreadReference> allThreads()
    {
        // Get threads, sort (should trigger VMDisconnectedException if VM has disconnected) and return
        try {
            List<ThreadReference> threads = new ArrayList(_vm.allThreads());
            Collections.sort(threads, new Comparator<ThreadReference>() {
                public int compare(ThreadReference o1, ThreadReference o2)
                {
                    return o1.name().compareTo(o2.name());
                }
            });
            return threads;
        }

        // If there is no session or VM is dead, just returns empty list
        catch (Exception e) {
            return Collections.emptyList();
        }
    }

//public List topLevelThreadGroups() throws NoSessionException{ensureActiveSession();return _vm.topLevelThreadGroups();}
//public ThreadGroupReference systemThreadGroup() throws NoSessionException
//{ ensureActiveSession(); return _vm.topLevelThreadGroups().get(0);}

    /**
     * Thread control.
     */
    void pauseThread(ThreadReference thread) throws NoSessionException
    {
        ensureActiveSession();
        thread.suspend();
    }

    void resumeThread(ThreadReference thread) throws NoSessionException
    {
        ensureActiveSession();
        thread.resume();
    }

    void stopThread(ThreadReference thread) throws NoSessionException
    {
        ensureActiveSession();
    } //thread.stop();

    /**
     * Returns the current thread.
     */
    public DebugThread getThread()
    {
        return _currentThread != null ? getThread(_currentThread) : null;
    }

    /**
     * Returns the current frame.
     */
    public DebugFrame getFrame()
    {
        int ind = _frameIndex; //getCurrentFrameIndex();
        StackFrame frame = ind >= 0 ? getCurrentFrame() : null;
        DebugThread thread = frame != null ? getThread() : null;
        return thread != null ? new DebugFrame(thread, frame, ind) : null;
    }

    /**
     * Sets the current frame.
     */
    public void setFrame(DebugFrame aFrame)
    {
        ThreadReference tref = aFrame != null ? aFrame._thread._tref : null;
        int ind = aFrame != null ? aFrame.getIndex() : -1;
        setCurrentThread(tref, ind);
    }

    /**
     * Returns the current thread.
     */
    public ThreadReference getCurrentThread()  { return _currentThread; }

    /**
     * Sets the current thread.
     */
    public void setCurrentThread(ThreadReference aThread, int aFrameIndex)
    {
        if (aThread == _currentThread && aFrameIndex == _frameIndex) return;
        _currentThread = aThread;
        _frameIndex = aFrameIndex;
        notifyFrameChanged();
    }

    /**
     * Returns the current stack frame.
     */
    public StackFrame getCurrentFrame()
    {
        try {
            return _running && _currentThread != null && _frameIndex >= 0 ? _currentThread.frame(_frameIndex) : null;
        }
        catch (Exception e) {
            System.err.println("DebugApp.getCurrentThread: " + e);
            return null;
        }
    }

    /**
     * Returns the current stack frame index.
     */
    public int getCurrentFrameIndex()
    {
        return _running && _paused ? _frameIndex : -1;
    }

    /**
     * Send line of input to app.
     */
    public void sendLineToApp(String line)
    {
        synchronized (inputLock) {
            inputBuffer.addFirst(line);
            inputLock.notifyAll();
        }
    }

    /**
     * SendLineToApp support.
     */
    private LinkedList inputBuffer = new LinkedList();
    private Object inputLock = new Object();
    private InputListener _appInput = new InputListener() {
        public String getLine()
        {
            // Don't allow reader to be interrupted -- catch and retry.
            String line = null;
            while (line == null) {
                synchronized (inputLock) {
                    try {
                        while (inputBuffer.size() < 1) inputLock.wait();
                        line = (String) inputBuffer.removeLast();
                    }
                    catch (InterruptedException ignore) { }
                }
            }

            // Must not be holding inputLock here, as listener that we call to echo line might call us re-entrantly
            echoInputLine(line);
            return line;
        }
    };

    /**
     * Called to echo input.
     */
    private void echoInputLine(String line)  { }

    /**
     * Adds a breakpoint.
     */
    public void addBreakpoint(Breakpoint aBP)
    {
        // If Breakpoint already set, just return
        for (BreakpointReq bpr : _eventRequestSpecs) {
            if (bpr._bpoint.equals(aBP)) {
                System.err.println("Breakpoint already added " + aBP);
                return;
            }
        }

        // Create new BreakpointReq and add
        BreakpointReq bpr = new BreakpointReq(this, aBP);
        _eventRequestSpecs = ArrayUtils.add(_eventRequestSpecs, bpr); // Add request to array
        if (_vm != null && !_terminated)  // Have event resolve immediately
            bpr.install(_vm);
    }

    /**
     * Removes a breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)
    {
        // Find BreakpointReq
        BreakpointReq bpr = null;
        for (BreakpointReq br : _eventRequestSpecs) {
            if (br._bpoint == aBP) {
                bpr = br;
                break;
            }
        }
        if (bpr == null)
            return;

        // Remove BreakpointReq
        _eventRequestSpecs = ArrayUtils.remove(_eventRequestSpecs, bpr); // Add request to array
        bpr.delete();  // Delete
    }

    /**
     * Resolve all deferred eventRequests waiting for 'refType'.
     */
    protected void resolve(ReferenceType refType)
    {
        for (BreakpointReq ers : _eventRequestSpecs)
            ers.attemptResolve(refType);
    }

    /**
     * Return a list of ReferenceType objects for all currently loaded classes and interfaces. Array types are not returned.
     */
    public List<ReferenceType> allClasses() throws NoSessionException
    {
        ensureActiveSession();
        return _vm.allClasses();
    }

    /**
     * Return a ReferenceType object for the currently loaded class or interface whose fully-qualified
     * class name is specified, else return null if there is none. In general, we must return a list of types, because
     * multiple class loaders could have loaded a class with the same fully-qualified name.
     */
    public List<ReferenceType> findClassesByName(String name) throws NoSessionException
    {
        ensureActiveSession();
        return _vm.classesByName(name);
    }

    /**
     * Return a list of ReferenceType objects for all currently loaded classes and interfaces whose name matches the given
     * pattern.  The pattern syntax is open to some future revision, but currently consists of a fully-qualified class name
     * in which the first component may optionally be a "*" character, designating an arbitrary prefix.
     */
    public List<ReferenceType> findClassesMatchingPattern(String aPattern) throws NoSessionException
    {
        ensureActiveSession();

        // Wildcard matches any leading package name.
        if (aPattern.startsWith("*.")) {
            String pattern = aPattern.substring(1);
            List result = new ArrayList();  //### Is default size OK?
            List<ReferenceType> classes = _vm.allClasses();
            for (ReferenceType type : classes) {
                if (type.name().endsWith(pattern))
                    result.add(type);
            }
            return result;
        }

        // It's a class name.
        return _vm.classesByName(aPattern);
    }

    /**
     * Notifications for app start, exit, interrupted, continued.
     */
    protected void notifyAppStarted()
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appStarted(this);
    }

    protected void notifyAppPaused()
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appPaused(this);
    }

    protected void notifyAppResumed()
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appResumed(this);
    }

    protected void notifyAppExited()
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.appExited(this);
    }

    /**
     * Notification that current StackFrame changed.
     * This notification is fired only in response to USER-INITIATED changes to the current thread and current frame.
     * When the current thread is set automatically after a breakpoint hit or step completion, no event is generated.
     * Instead, interested parties are expected to listen for the BreakpointHit and StepCompleted events.  This convention
     * is unclean, and I believe that it reflects a defect in the current architecture.  Unfortunately, however, we cannot
     * guarantee the order in which various listeners receive a given event, and the handlers for the very same events that
     * cause automatic changes to the current thread may also need to know the current thread.
     */
    protected void notifyFrameChanged()
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.frameChanged(this);
    }

    /**
     * Notifications for breakpoints.
     */
    protected void notifySet(BreakpointReq aBP)
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.requestSet(aBP);
    }

    protected void notifyDeferred(BreakpointReq aBP)
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.requestDeferred(aBP);
    }

    protected void notifyDeleted(BreakpointReq aBP)
    {
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.requestDeleted(aBP);
    }

    protected void notifyError(BreakpointReq aBP)
    {
        error("Failed to set BP: " + aBP);
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.requestError(aBP);
    }

    /**
     * Handle DebugEvents from EventDispatch thread.
     */
    void dispatchEvent(DebugEvent anEvent)
    {
        // Notify listeners
        DebugEvent.Type type = anEvent._type;
        boolean eventPaused = anEvent.suspendedAll(), wantsPause = isPaused();
        if (_printEVs) System.out.println("JDIEvent: " + anEvent);

        // If invoking a method, we just want to get back to business
        if (_invoking) {
            if (type == DebugEvent.Type.ClassPrepare)
                resolve(anEvent.getReferenceType());
            if (eventPaused)
                try {
                    _vm.resume();
                    return;
                } catch (Exception e) {
                    failure("Failed to resume: " + e.getMessage());
                }
            return;
        }

        // Handle event types (VMStart, VMDeath, VMDisconnect, ThreadStart, ThreadDeath, ClassPrepare, ClassUnload)
        switch (type) {

            // Handle Class prepare
            case ClassPrepare: resolve(anEvent.getReferenceType()); break;

            // Handle VMDisconnect
            case VMDisconnect: endSession(); break;

            // Handle LocationTrigger
            case LocationTrigger:
                if (_runToLineBreak != null) {
                    removeBreakpoint(_runToLineBreak);
                    _runToLineBreak = null;
                }
                setCurrentThread(anEvent.getThread(), 0);
                wantsPause = true;
                break;

            // Handle Exception
            case Exception:
                setCurrentThread(anEvent.getThread(), 0);
                wantsPause = true;
                break;

            // Handle AccessWatchpoint
            case AccessWatchpoint: wantsPause = true; break;

            // Handle ModificationWatchpoint
            case ModificationWatchpoint: wantsPause = true; break;
        }

        // If event paused VM either resume or make it official
        if (eventPaused) {

            // If we don't want pause, resume
            if (!wantsPause)
                resumeQuiet();

                // Otherwise if pause not official, make official
            else if (!_paused) {
                setPaused(true);
                notifyAppPaused();
            }
        }

        // Dispatch event to listener
        for (AppListener appLsnr : _appLsnrs)
            appLsnr.processDebugEvent(this, anEvent);
    }

    /**
     * A thread to read and send events from vm.
     */
    public class JDIEventDispatcher extends Thread {

        EventQueue _queue;

        /**
         * Create JDIEventDispatcher.
         */
        public JDIEventDispatcher()
        {
            super("JDI Event Set Dispatcher");
            _queue = _vm.eventQueue();
        }

        /**
         * Override for thread meat.
         */
        public void run()
        {
            try {
                while (true) {
                    EventSet jdiEvents = _queue.remove();
                    DebugEvent event = new DebugEvent(jdiEvents);
                    dispatchEvent(event);
                    if (event.getType() == DebugEvent.Type.VMDisconnect) break; // Quit on VMDisconnect
                }
            }

            // Handle Exceptions
            catch (Exception ignore) { }

            // Set everything stopped
            _running = false;
            setPaused(false);
            _terminated = true;
        }
    }

    /**
     * Returns a list of methods for a given ReferenceType, name and kind.
     */
    private static List<Method> methods(ReferenceType refType, String aName, int kind)
    {
        List<Method> list = refType.methodsByName(aName);
        Iterator<Method> iter = list.iterator();
        while (iter.hasNext()) {
            Method method = iter.next();
            boolean isStatic = method.isStatic();
            if ((kind == STATIC && !isStatic) || (kind == INSTANCE && isStatic)) {
                iter.remove();
            }
        }
        return list;
    }

    /**
     * Returns a method for given args.
     */
    static Method method(List<Method> overloads, List<Value> args)
    {
        // If there is only one method to call, we'll just choose that without looking at the args.  If they aren't right
        // the invoke will return a better error message than we could generate here.
        if (overloads.size() == 1)
            return overloads.get(0);

        // Resolving overloads is beyond the scope of this exercise. So, we will look for a method that matches exactly the
        // types of the arguments.  If we can't find one, then if there is exactly one method whose param types are
        // assignable from the arg types, we will use that.  Otherwise, it is an error.  We won't guess which of multiple
        // possible methods to call. And, since casts aren't implemented, the user can't use them to pick a particular
        // overload to call. IE, the user is out of luck in this case.
        Method retVal = null;
        int assignableCount = 0;
        for (Method mm : overloads) {

            // This probably won't happen for the method that we are really supposed to call.
            List<Type> argTypes;
            try {
                argTypes = mm.argumentTypes();
            } catch (ClassNotLoadedException ee) {
                continue;
            }

            //
            int compare = argumentsMatch(argTypes, args);
            if (compare == SAME)
                return mm;
            if (compare == DIFFERENT)
                continue;

            // Else, it is assignable.  Remember it.
            retVal = mm;
            assignableCount++;
        }

        // At this point, we didn't find an exact match, but we found one for which the args are assignable.
        if (retVal != null) {
            if (assignableCount == 1)
                return retVal;
            throw new RuntimeException("Arguments match multiple methods");
        }
        throw new RuntimeException("Arguments match no method");
    }

    /**
     * Return SAME, DIFFERENT or ASSIGNABLE.
     * SAME means each arg type is the same as type of the corr. arg.
     * ASSIGNABLE means that not all the pairs are the same, but
     * for those that aren't, at least the argType is assignable from the type of the argument value.
     * DIFFERENT means that in at least one pair, the argType is not assignable from the type of the argument value.
     * IE, one is an Apple and the other is an Orange.
     */
    private static int argumentsMatch(List<Type> argTypes, List<Value> args)
    {
        if (argTypes.size() != args.size())
            return DIFFERENT;

        Iterator<Type> typeIter = argTypes.iterator();
        Iterator<Value> valIter = args.iterator();
        int result = SAME;

        // If any pair aren't the same, change the
        // result to ASSIGNABLE.  If any pair aren't
        // assignable, return DIFFERENT
        while (typeIter.hasNext()) {
            Type argType = typeIter.next();
            Value value = valIter.next();
            if (value == null) {
                // Null values can be passed to any non-primitive argument
                if (primitiveTypeNames.contains(argType.name()))
                    return DIFFERENT;
                // Else, we will assume that a null value
                // exactly matches an object type.
            }
            if (!value.type().equals(argType)) {
                if (isAssignableTo(value.type(), argType))
                    result = ASSIGNABLE;
                else return DIFFERENT;
            }
        }
        return result;
    }

    /**
     * Returns whether fromType is assignable toType.
     */
    private static boolean isAssignableTo(Type fromType, Type toType)
    {
        if (fromType.equals(toType))
            return true;

        // If one is boolean, so must be the other.
        if (fromType instanceof BooleanType)
            return toType instanceof BooleanType;
        if (toType instanceof BooleanType)
            return false;

        // Other primitive types are intermixable only with each other.
        if (fromType instanceof PrimitiveType)
            return toType instanceof PrimitiveType;
        if (toType instanceof PrimitiveType)
            return false;

        // neither one is primitive.
        if (fromType instanceof ArrayType)
            return isArrayAssignableTo((ArrayType) fromType, toType);

        List<InterfaceType> interfaces;
        if (fromType instanceof ClassType) {
            ClassType superclazz = ((ClassType) fromType).superclass();
            if (superclazz != null && isAssignableTo(superclazz, toType))
                return true;
            interfaces = ((ClassType) fromType).interfaces();
        }

        // fromType must be an InterfaceType
        else interfaces = ((InterfaceType) fromType).superinterfaces();

        for (InterfaceType interfaze : interfaces)
            if (isAssignableTo(interfaze, toType))
                return true;
        return false;
    }

    /**
     * Returns whether fromType is assignable toType (for ArrayType).
     */
    private static boolean isArrayAssignableTo(ArrayType fromType, Type toType)
    {
        if (toType instanceof ArrayType) {
            try {
                Type toComponentType = ((ArrayType) toType).componentType();
                return isComponentAssignable(fromType.componentType(), toComponentType);
            }

            // One or both component types has not yet been loaded => can't assign
            catch (ClassNotLoadedException e) {
                return false;
            }
        }

        // Only valid InterfaceType assignee is Cloneable
        if (toType instanceof InterfaceType)
            return toType.name().equals("java.lang.Cloneable");

        // Only valid ClassType assignee is Object
        return toType.name().equals("java.lang.Object");
    }

    /**
     * Returns whether fromType is assignable toType (for ArrayType).
     */
    private static boolean isComponentAssignable(Type fromType, Type toType)
    {
        // Assignment of primitive arrays requires identical component types
        if (fromType instanceof PrimitiveType)
            return fromType.equals(toType);
        if (toType instanceof PrimitiveType)
            return false;
        // Assignment of object arrays requires availability of widening conversion of component types
        return isAssignableTo(fromType, toType);
    }

    /**
     * PrimitiveTypeNames.
     */
    private static String[] ptnames = {"boolean", "byte", "char", "short", "int", "long", "float", "double"};
    private static List<String> primitiveTypeNames = Arrays.asList(ptnames);
}
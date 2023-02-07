package snapcode.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

/**
 * A class to simplify JDIEventSet.
 */
public class DebugEvent {

    // The JDI event set
    EventSet _jdiEvents;

    // The first event from event set
    Event _jdiEvent;

    // The type
    Type _type;

    // Constants for type
    public enum Type {
        Exception, AccessWatchpoint, ModificationWatchpoint, LocationTrigger, ClassPrepare,
        ClassUnload, ThreadDeath, ThreadStart, VMDeath, VMDisconnect, VMStart
    }

    /**
     * Create new DebugEvent.
     */
    public DebugEvent(EventSet theJDIEvents)
    {
        _jdiEvents = theJDIEvents; //_vm = theJDIEvents.virtualMachine();
        _jdiEvent = theJDIEvents.eventIterator().nextEvent();
        _type = getType(_jdiEvent);
    }

/**
 * Returns the VirtualMachine.
 */
//public VirtualMachine getVM()  { return _jdiEvents.virtualMachine(); }

    /**
     * Returns the type.
     */
    public Type getType()
    {
        return _type;
    }

    /**
     * Gets the thrown exception object. The exception object is an instance of Throwable or a subclass in the target VM.
     *
     * @return an {@link ObjectReference} which mirrors the thrown object in the target VM.
     */
    public ObjectReference getException()
    {
        return ((ExceptionEvent) _jdiEvent).exception();
    }

    /**
     * Gets the location where the exception will be caught. An exception is considered to be caught if, at the point
     * of the throw, the current location is dynamically enclosed in a try statement that handles the exception.
     * (See the JVM specification for details). If there is such a try statement, the catch location is the
     * first code index of the appropriate catch clause. <p>
     * If there are native methods in the call stack at the time of the exception, there are important restrictions
     * to note about the returned catch location. In such cases, it is not possible to predict whether an exception
     * will be handled by some native method on the call stack. Thus, it is possible that exceptions considered
     * uncaught here will, in fact, be handled by a native method and not cause termination of the target VM. Also,
     * it cannot be assumed that the catch location returned here will ever be reached by the throwing thread. If
     * there is a native frame between the current location and the catch location, the exception might be handled
     * and cleared in that native method instead.
     *
     * @return the {@link Location} where the exception will be caught or null if the exception is uncaught.
     */
    public Location getCatchLocation()
    {
        return ((ExceptionEvent) _jdiEvent).catchLocation();
    }

    /**
     * Value that will be assigned to the field when the instruction completes.
     */
    public Value getValueToBe()
    {
        return ((ModificationWatchpointEvent) _jdiEvent).valueToBe();
    }

    /**
     * Returns the thread in which this event has occurred.
     *
     * @return a {@link ThreadReference} which mirrors the event's thread in the target VM.
     */
    public ThreadReference getThread()
    {
        if (_jdiEvent instanceof LocatableEvent) return ((LocatableEvent) _jdiEvent).thread();
        if (_jdiEvent instanceof ClassPrepareEvent) return ((ClassPrepareEvent) _jdiEvent).thread();
        if (_jdiEvent instanceof ThreadDeathEvent) return ((ThreadDeathEvent) _jdiEvent).thread();
        if (_jdiEvent instanceof ThreadStartEvent) return ((ThreadStartEvent) _jdiEvent).thread();
        if (_jdiEvent instanceof VMStartEvent) return ((VMStartEvent) _jdiEvent).thread();
        throw new RuntimeException("JDIEventSet.getThread: Can't return thread for " + _type);
    }

    /**
     * Returns the {@link Location} of this mirror. Depending on context and on available debug information, this
     * location will have varying precision.
     *
     * @return the {@link Location} of this mirror.
     */
    public Location getLocation()
    {
        return ((LocatableEvent) _jdiEvent).location();
    }

    /**
     * Returns the reference type for which this event was generated.
     *
     * @return a {@link ReferenceType} which mirrors the class, interface, or array which has been linked.
     */
    public ReferenceType getReferenceType()
    {
        return ((ClassPrepareEvent) _jdiEvent).referenceType();
    }

    /**
     * Returns the name of the class that has been unloaded.
     */
    public String getClassName()
    {
        return ((ClassUnloadEvent) _jdiEvent).className();
    }

    /**
     * Returns the JNI-style signature of the class that has been unloaded.
     */
    public String getClassSignature()
    {
        return ((ClassUnloadEvent) _jdiEvent).classSignature();
    }

    /**
     * Returns the field that is about to be accessed/modified.
     *
     * @return a {@link Field} which mirrors the field in the target VM.
     */
    public Field getField()
    {
        return ((WatchpointEvent) _jdiEvent).field();
    }

    /**
     * Returns the object whose field is about to be accessed/modified.
     * Return null is the access is to a static field.
     *
     * @return a {@link ObjectReference} which mirrors the event's object in the target VM.
     */
    public ObjectReference getObject()
    {
        return ((WatchpointEvent) _jdiEvent).object();
    }

    /**
     * Current value of the field.
     */
    public Value getValueCurrent()
    {
        return ((WatchpointEvent) _jdiEvent).valueCurrent();
    }

    /**
     * Returns the policy used to suspend threads in target VM for this event set. This policy is selected from the suspend
     * policies for each event's request. The one that suspends the most threads is chosen when the event occurs in
     * target VM and that policy is returned here. See com.sun.jdi.request.EventRequest for possible policy values.
     */
    public int getSuspendPolicy()
    {
        return _jdiEvents.suspendPolicy();
    }

    public boolean suspendedAll()
    {
        return _jdiEvents.suspendPolicy() == EventRequest.SUSPEND_ALL;
    }

    public boolean suspendedThread()
    {
        return _jdiEvents.suspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD;
    }

    public boolean suspendedNone()
    {
        return _jdiEvents.suspendPolicy() == EventRequest.SUSPEND_NONE;
    }

    public void resume()
    {
        _jdiEvents.resume();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "JDIEventSet: " + _type;
    }

    /**
     * Returns a type for a JDIEvent.
     */
    private static Type getType(Event anEvent)
    {
        if (anEvent instanceof VMStartEvent) return Type.VMStart;
        if (anEvent instanceof VMDeathEvent) return Type.VMDeath;
        if (anEvent instanceof VMDisconnectEvent) return Type.VMDisconnect;
        if (anEvent instanceof ThreadStartEvent) return Type.ThreadStart;
        if (anEvent instanceof ThreadDeathEvent) return Type.ThreadDeath;
        if (anEvent instanceof ExceptionEvent) return Type.Exception;
        if (anEvent instanceof AccessWatchpointEvent) return Type.AccessWatchpoint;
        if (anEvent instanceof WatchpointEvent) return Type.ModificationWatchpoint;
        if (anEvent instanceof ClassPrepareEvent) return Type.ClassPrepare;
        if (anEvent instanceof ClassUnloadEvent) return Type.ClassUnload;
        if (anEvent instanceof LocatableEvent) return Type.LocationTrigger;
        throw new IllegalArgumentException("Unknown event " + anEvent);
    }

}
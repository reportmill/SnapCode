package snapcode.debug;
import com.sun.jdi.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import snapcode.project.Breakpoint;
import snapcode.project.Breakpoint.Type;
import java.util.ArrayList;
import java.util.List;
import snapcode.debug.Exceptions.*;

/**
 * This class represents a breakpoint or watchpoint, creating an EventRequest as necessary.
 */
public class BreakpointReq {

    // App
    DebugApp _app;

    // Breakpoint
    Breakpoint _bpoint;

    // Request
    EventRequest _request = null;

    // The exception, if request couldn't be resolved
    Exception _error;

    // Constants for status
    public enum Status {Deferred, Resolved, Erroneous}

    /**
     * Creates a new EventRequestSpec.
     */
    public BreakpointReq(DebugApp aRuntime, Breakpoint aBP)
    {
        _app = aRuntime;
        _bpoint = aBP;
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        return _bpoint.getName();
    }

    /**
     * Returns the type.
     */
    public Type getType()
    {
        return _bpoint.getType();
    }

    /**
     * Called when ClassPrepare event is received to resolve EventRequests added prior to run.
     */
    public void attemptResolve(ReferenceType aRefType)
    {
        if (!isResolved() && matches(aRefType))
            install(aRefType);
    }

    /**
     * Called when EventRequest is created to try to resolve immediately.
     */
    public void install(VirtualMachine vm)
    {
        // try to resolve immediately: Iterate over vm classes and try to resolve if ref-type found
        List<ReferenceType> classes = vm.allClasses();
        for (ReferenceType refType : classes)
            if (matches(refType)) {
                install(refType);
                return;
            }

        // If ref-type not found, notify deferred
        _app.breakpointReqWasDeferred(this);
    }

    /**
     * Called to install EventRequest.
     */
    protected void install(ReferenceType aRefType)
    {
        // Create event request, set and enable
        try {
            _request = createEventRequest(aRefType);
            _request.putProperty("spec", this); // Don't think we need this
            _request.enable();
            _app.breakpointReqWasSet(this);  // Notify successful set
            System.out.println("Installed BP " + getName() + " :" + _bpoint.getLineNum());
        }

        // If create event request fails, set error and notify
        catch (Exception exc) {
            _error = exc;
            _app.breakpointReqSetFailed(this);
        }
    }

    /**
     * Called to delete request.
     */
    public void delete()
    {
        // Delete request and notify
        EventRequest request = getEventRequest();
        if (request != null && _app.isRunning())
            request.virtualMachine().eventRequestManager().deleteEventRequest(request);
        _app.breakpointReqWasDeleted(this);
    }

    /**
     * Does the specified ReferenceType match this spec.
     */
    public boolean matches(ReferenceType aRefType)
    {
        // If source name not equal, bail
    /*if(_sourceName!=null) { try { if(!refType.sourceName().equals(_sourceName)) return false; }
        catch(AbsentInformationException exc) { return false; }
        try { refType.locationsOfLine(_lineNumber); }  // Try to find line number
        catch(AbsentInformationException exc) { return false; } catch(ObjectCollectedException  exc) { return false; }
        return true; //catch(InvalidLineNumberException  exc) { } }*/

        // Pattern
        //return _isWild? refType.name().endsWith(_className) : refType.name().equals(_className);

        // See if root name matches
        String rname = aRefType.name(), cname = _bpoint.getClassName();
        boolean match = rname.startsWith(cname) && (rname.length() == cname.length() || rname.startsWith(cname + '$'));

        // Make sure location is available
        if (match) {
            try {
                location((ClassType) aRefType);
            } catch (Exception e) {
                return false;
            }
        }

        // Ignore matches for ProjectClassLoader. TODO: Need better way to resolve duplicate classes
        if (match)
            if (aRefType.classLoader().type().name().endsWith("ProjectClassLoaderX"))
                match = false;
        return match;
    }

    /**
     * Returns the event request.
     */
    public EventRequest getEventRequest()
    {
        return _request;
    }

    /**
     * Create EventRequest.
     */
    protected EventRequest createEventRequest(ReferenceType aRefType) throws InvalidTypeException, LineNotFoundException,
            MalformedMemberNameException, NoSessionException, NoSuchMethodException, AmbiguousMethodException, NoSuchFieldException
    {
        // Bail if refType isn't ClassType
        if (!(aRefType instanceof ClassType))
            throw new InvalidTypeException();

        // Handle LineNumber
        if (getType() == Type.LineBreakpoint) {
            Location location = location((ClassType) aRefType);
            return aRefType.virtualMachine().eventRequestManager().createBreakpointRequest(location);
        }

        // Handle Method
        if (getType() == Type.MethodBreakpoint) {
            Location location = location((ClassType) aRefType);
            if (!isValidMethodName(_bpoint.getMethodName()))
                throw new MalformedMemberNameException(_bpoint.getMethodName());
            return aRefType.virtualMachine().eventRequestManager().createBreakpointRequest(location);
        }

        // Handle Exception
        if (getType() == Type.Exception) {
            EventRequestManager eventRequestManager = aRefType.virtualMachine().eventRequestManager();
            return eventRequestManager.createExceptionRequest(aRefType, _bpoint.isNotifyCaught(), _bpoint.isNotifyUncaught());
        }

        // Handle AccessWatchpoint and ModificationWatchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint) {
            Field field = aRefType.fieldByName(_bpoint.getFieldName());
            if (field == null)
                throw new NoSuchFieldException(_bpoint.getFieldName());

            // Handle Access/Modification Watchpoint
            if (getType() == Type.AccessWatchpoint)
                return aRefType.virtualMachine().eventRequestManager().createAccessWatchpointRequest(field);
            return aRefType.virtualMachine().eventRequestManager().createModificationWatchpointRequest(field);
        }

        // Unsupported type
        throw new RuntimeException("Unsupported Type: " + getType());
    }

    /**
     * Returns the location of the break point.
     */
    private Location location(ClassType aClass) throws LineNotFoundException, NoSessionException, NoSuchMethodException,
            AmbiguousMethodException
    {
        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint) try {
            List locs = aClass.locationsOfLine(_bpoint.getLineNum());
            if (locs.size() == 0)
                throw new LineNotFoundException();

            // TODO handle multiple locations
            Location location = (Location) locs.get(0);
            if (location.method() == null)
                throw new LineNotFoundException();
            return location;
        }

        // TO DO: throw something more specific, or allow AbsentInfo exception to pass through.
        catch (AbsentInformationException e) {
            throw new LineNotFoundException();
        }

        // Handle Type Method
        Method method = findMatchingMethod(_app, aClass, _bpoint.getMethodName(), _bpoint.getMethodArgs());
        Location location = method.location();
        return location;
    }

    /**
     * Returns the exception, if request hit error.
     */
    public Exception getError()
    {
        return _error;
    }

    /**
     * Returns the error message.
     */
    public String getErrorMessage()
    {
        // Handle Type LineNumber
        if (_error instanceof LineNotFoundException)
            return "No code at line " + _bpoint.getLineNum() + " in " + getName();
        if (_error instanceof InvalidTypeException)
            return "Breakpoints can be located only in classes. " + getName() + " is an interface or array";

        // Handle Type Method
        if (_error instanceof AmbiguousMethodException)
            return "Method " + _bpoint.getMethodName() + " is overloaded; specify arguments"; // Should list methods here
        if (_error instanceof NoSuchMethodException)
            return "No method " + _bpoint.getMethodName() + " in " + getName();
        if (_error instanceof InvalidTypeException)
            return "Breakpoints can be located only in classes. " + getName() + " is an interface or array";

        // Handle Type Watchpoint
        if (_error instanceof NoSuchFieldException)
            return "No field " + _bpoint.getFieldName() + " in " + getName();

        // Basic errors
        if (_error instanceof IllegalArgumentException)
            return ("Invalid command syntax");
        else if (_error instanceof RuntimeException) // A runtime exception that we were not expecting
            throw (RuntimeException) _error;
        return "Internal error; unable to set " + this;
    }

    /**
     * @return true if this spec has been resolved.
     */
    public boolean isResolved()
    {
        return _request != null;
    }

    /**
     * @return true if this spec has not yet been resolved.
     */
    public boolean isUnresolved()
    {
        return _request == null && _error == null;
    }

    /**
     * @return true if this spec is unresolvable due to error.
     */
    public boolean isErroneous()
    {
        return _error != null;
    }

    /**
     * Returns the status.
     */
    public Status getStatus()
    {
        return _error != null ? Status.Erroneous : _request != null ? Status.Resolved : Status.Deferred;
    }

    /**
     * Returns whether given name is valid method name.
     */
    public static boolean isValidMethodName(String aName)
    {
        return DebugUtils.isJavaIdentifier(aName) || aName.equals("<init>") || aName.equals("<clinit>");
    }

    /**
     * Attempt an unambiguous match of the method name and argument specification to a method. If no arguments
     * are specified, the method must not be overloaded. Otherwise, the argument types much match exactly
     */
    public static Method findMatchingMethod(DebugApp aRT, ClassType aCls, String aMethodName, List<String> theArgs)
            throws AmbiguousMethodException, NoSuchMethodException, NoSessionException
    {
        // Normalize the argument string once before looping below.
        List argTypeNames = null;
        if (theArgs != null) {
            argTypeNames = new ArrayList(theArgs.size());
            for (String name : theArgs) {
                name = normalizeArgTypeName(name, aRT);
                argTypeNames.add(name);
            }
        }

        // Iterate over Class.Methods and check for matches
        Method firstMatch = null;
        int matchCount = 0; // > 1 implies overload
        for (Method candidate : aCls.methods()) {

            // If name matches, bump MatchCount and check for args
            if (candidate.name().equals(aMethodName)) {
                matchCount++;

                // Remember the first match in case it is the only one
                if (matchCount == 1)
                    firstMatch = candidate;

                // If argument types were specified, check against candidate - if exact match, return it
                if (argTypeNames != null && compareArgTypes(candidate, argTypeNames))
                    return candidate;
            }
        }

        // If at least one name matched and no arg types were specified...
        if (argTypeNames == null && matchCount > 0) {
            if (matchCount == 1)
                return firstMatch;       // Only one match; safe to use it
            throw new AmbiguousMethodException();
        }

        // Throw NoSuchMethodException
        throw new NoSuchMethodException(aMethodName);
    }

    /**
     * Remove unneeded spaces and expand class names to fully qualified names, if necessary and possible.
     */
    private static String normalizeArgTypeName(String aName, DebugApp anApp) throws NoSessionException
    {
        // Separate the type name from any array modifiers, stripping whitespace after the name ends.
        int i = 0;
        StringBuffer typePart = new StringBuffer();
        StringBuffer arrayPart = new StringBuffer();
        aName = aName.trim();
        int nameLength = aName.length();

        // For varargs, there can be spaces before ellipses but not within.  So, just ignore ellipses while stripping blanks
        boolean isVarArgs = aName.endsWith("...");
        if (isVarArgs)
            nameLength -= 3;

        while (i < nameLength) {
            char c = aName.charAt(i);
            if (Character.isWhitespace(c) || c == '[')
                break;      // name is complete
            typePart.append(c);
            i++;
        }
        while (i < nameLength) {
            char c = aName.charAt(i);
            if (c == '[' || c == ']')
                arrayPart.append(c);
            else if (!Character.isWhitespace(c))
                throw new IllegalArgumentException("Invalid argument type name");
            i++;
        }

        aName = typePart.toString();

        // When there's no sign of a package name already, try to expand the the name to a fully qualified class name
        if (aName.indexOf('.') == -1 || aName.startsWith("*.")) {
            try {
                List refs = anApp.findClassesMatchingPattern(aName);
                if (refs.size() > 0)  //### ambiguity???
                    aName = ((ReferenceType) (refs.get(0))).name();
            } catch (IllegalArgumentException e) {
            } // We'll try the name as is
        }
        aName += arrayPart.toString();
        if (isVarArgs)
            aName += "...";
        return aName;
    }

    /*
     * Compare a method's argument types with a Vector of type names. Return true if each argument type has a name
     * identical to the corresponding string in the vector (allowing for varargs) and if the number of arguments in the
     * method matches the number of names passed
     */
    private static boolean compareArgTypes(Method aMethod, List<String> theNames)
    {
        // Get arg type names - if argument counts differ, we can stop here
        List<String> argTypeNames = aMethod.argumentTypeNames();
        if (argTypeNames.size() != theNames.size())
            return false;

        // Compare each argument type's name
        for (int i = 0, iMax = argTypeNames.size(); i < iMax; i++) {

            // Get both names
            String comp1 = argTypeNames.get(i), comp2 = theNames.get(i);

            // If names not equal...
            if (!comp1.equals(comp2)) {

                // We have to handle varargs.  EG, the method's last arg type is xxx[] while the nameList contains xxx...
                // Note that the nameList can also contain xxx[] in which case we don't get here.
                if (i != iMax - 1 || !aMethod.isVarArgs() || !comp2.endsWith("..."))
                    return false;

                // The last types differ, it is a varargs method and the nameList item is varargs. We just have to compare
                // the type names, eg, make sure we don't have xxx[] for the method arg type and yyy... for nameList item.
                int comp1Length = comp1.length();
                if (comp1Length + 1 != comp2.length()) // The type names are different lengths
                    return false;

                // We know the two type names are the same length
                return comp1.regionMatches(0, comp2, 0, comp1Length - 2);

                // We do have xxx[] and xxx... as the last param type
            }
        }

        // Return true since everything matched
        return true;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return _bpoint + " (" + getStatus() + ")";
    }

}
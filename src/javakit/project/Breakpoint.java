/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.web.WebFile;
import java.util.List;
import java.util.Objects;

/**
 * Represents a debugger break point for a project file.
 */
public class Breakpoint implements Comparable<Breakpoint> {

    // The type of breakpoint
    private Type  _type;

    // The file path
    private WebFile  _file;

    // The class name
    private String  _className;

    // Whether class name is wild carded (if Class breakpoint)
    private boolean  _isWild;

    // The line number
    private int  _line;

    // The field name (for Watchpoints)
    private String  _fieldId;

    // The method name (for MethodBreakpoint)
    private String  _methodName;

    // The method args (for MethodBreakpoint)
    private List<String> _methodArgs;

    // Whether to notify caught, uncaught (for Exception)
    private boolean  _notifyCaught, _notifyUncaught;

    // Whether breakpoint is enabled
    private boolean  _enabled = true;

    // Constants for type
    public enum Type { LineBreakpoint, MethodBreakpoint, Exception, AccessWatchpoint, ModificationWatchpoint }

    /**
     * Constructor.
     */
    protected Breakpoint()
    {
        super();
    }

    /**
     * Constructor for File and Line.
     */
    public Breakpoint(WebFile aFile, int aLine)
    {
        _type = Type.LineBreakpoint;
        setFile(aFile);
        setLine(aLine);
        Project proj = Project.getProjectForFile(aFile);
        _className = proj.getClassNameForFile(aFile);
    }

    /**
     * Constructor for class name and Line.
     */
    public Breakpoint(String aClsName, int line)
    {
        _type = Type.LineBreakpoint;
        _className = aClsName;
        _line = line;
        if (_className != null && _className.startsWith("*")) {
            _isWild = true;
            _className = _className.substring(1);
        }
    }

    /**
     * Constructor for class name, method name and args.
     * For example: initMethod("snap.app.App", "main", Collections.singletonList("java.lang.String[]"));
     */
    public Breakpoint(String aClsName, String aMethod, List<String> theArgs)
    {
        _type = Type.MethodBreakpoint;
        _className = aClsName;
        _methodName = aMethod;
        _methodArgs = theArgs;
    }

    /**
     * Initializes a ExceptionIntercept.
     */
    public static Breakpoint getExceptionBreak(String aClsName, boolean notifyCaught, boolean notifyUncaught)
    {
        Breakpoint bp = new Breakpoint();
        bp._type = Type.Exception;
        bp._className = aClsName;
        bp._notifyCaught = notifyCaught;
        bp._notifyUncaught = notifyUncaught;
        return bp;
    }

    /**
     * Initializes a Watchpoint.
     */
    public static Breakpoint getAccessWatchpoint(String aClsName, String fieldId, boolean isAccess)
    {
        Breakpoint bp = new Breakpoint();
        bp._type = isAccess ? Type.AccessWatchpoint : Type.ModificationWatchpoint;
        bp._className = aClsName;
        bp._fieldId = fieldId; //if(!isJavaIdentifier(fieldId)) throw new MalformedMemberNameException(fieldId);
        return bp;
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        if (_file != null)
            return _file.getPath(); // + '@' + _lineNumber;
        return _isWild ? "*" + _className : _className;
    }

    /**
     * Returns the type.
     */
    public Type getType()  { return _type; }

    /**
     * Returns the file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Sets the file.
     */
    protected void setFile(WebFile aFile)
    {
        _file = aFile;
    }

    /**
     * Returns the file path.
     */
    public String getFilePath()
    {
        return getFile().getPath();
    }

    /**
     * Returns the line index.
     */
    public int getLine()
    {
        return _line;
    }

    /**
     * Sets the line number.
     */
    public void setLine(int aLine)
    {
        _line = aLine;
    }

    /**
     * Returns the line number.
     */
    public int getLineNum()  { return _line + 1; }

    /**
     * Returns the class name.
     */
    public String getClassName()  { return _className; }

    /**
     * Returns the field name (if field break point).
     */
    public String getFieldName()  { return _fieldId; }

    /**
     * Returns the method name (if method break point).
     */
    public String getMethodName()  { return _methodName; }

    /**
     * Returns the method args (if method break point).
     */
    public List<String> getMethodArgs()  { return _methodArgs; }

    /**
     * Returns whether to notify caught.
     */
    public boolean isNotifyCaught()  { return _notifyCaught; }

    /**
     * Returns whether to notify caught.
     */
    public boolean isNotifyUncaught()  { return _notifyUncaught; }

    /**
     * Returns whether breakpoint is enabled.
     */
    public boolean isEnabled()  { return _enabled; }

    /**
     * Sets whether breakpoint is enabled.
     */
    public void setEnabled(boolean aValue)
    {
        _enabled = aValue;
    }

    /**
     * Returns a descriptor string.
     */
    public String getDescriptor()
    {
        String filePath = getFilePath();
        return filePath + " [Line: " + getLineNum() + "]";
    }

    /**
     * Standard compare implementation.
     */
    public int compareTo(Breakpoint aBP)
    {
        if (_file != aBP._file)
            return _file.compareTo(aBP._file);
        return _line < aBP._line ? -1 : _line == aBP._line ? 0 : 1;
    }

    /**
     * Return hash code.
     */
    public int hashCode()
    {
        // Get base
        int base = _file != null ? _file.hashCode() : _className.hashCode();

        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return base + _line;

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint)
            return base + (_methodName != null ? _methodName.hashCode() : 0) +
                    (_methodArgs != null ? _methodArgs.hashCode() : 0);

        // Handle Type AccessWatchpoint and ModificationWatchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return base + _fieldId.hashCode() + getClass().hashCode();

        // Everything else
        return base;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        // Check identity and get other
        if (anObj == this) return true;
        if (!(anObj instanceof Breakpoint)) return false;
        Breakpoint other = (Breakpoint) anObj;

        // Check Type, SourceName, ClassName
        if (other._type != _type) return false;
        if (!Objects.equals(other._file, _file)) return false;
        if (!Objects.equals(other._className, _className)) return false;

        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return other._line == _line;

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint)
            return other._methodName.equals(_methodName) && other._methodArgs.equals(_methodArgs);

        // Handle Type AccessWatchpoint and ModificationWatchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return other._fieldId.equals(_fieldId);

        // Return true
        return true;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        // Handle Type LineNumber
        if (getType() == Type.LineBreakpoint)
            return String.format("breakpoint %s:%d", getName(), getLineNum());

        // Handle Type Method
        if (getType() == Type.MethodBreakpoint) {
            StringBuilder sb = new StringBuilder("breakpoint ");
            sb.append(getName()).append('.').append(_methodName);
            if (_methodArgs != null) {
                boolean first = true;
                sb.append('(');
                for (String arg : _methodArgs) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(arg);
                }
                sb.append(")");
            }
            return sb.toString();
        }

        // Handle Exception
        if (getType() == Type.Exception)
            return "Exception catch " + getName();

        // Handle Watchpoint
        if (getType() == Type.AccessWatchpoint || getType() == Type.ModificationWatchpoint)
            return "Watchpoint: " + _fieldId;

        // Unknown
        return "Unknown Event request type " + getType();
    }
}
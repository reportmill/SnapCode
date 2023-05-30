/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;

/**
 * This class represents a local variable definition in a statement block, for-loop declaration or catch block.
 */
public class JavaLocalVar extends JavaDecl {

    // The index of this local var in the current frame of the call stack
    private int  _indexInStackFrame = -1;

    /**
     * Constructor.
     */
    public JavaLocalVar(Resolver anOwner, String aName, JavaType aType, String anId)
    {
        super(anOwner, DeclType.VarDecl);

        // Set Id, Name, SimpleName, EvalType
        _id = anId;
        _name = _simpleName = aName;
        _evalType = aType;
    }

    /**
     * Returns the index of this local var in the current frame of the call stack.
     */
    public int getIndexInStackFrame()  { return _indexInStackFrame; }

    /**
     * Sets the index of this local var in the current frame of the call stack.
     */
    public void setIndexInStackFrame(int anIndex)  { _indexInStackFrame = anIndex; }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get SimpleName, EvalType.SimpleName
        String simpleName = getSimpleName();
        JavaType evalType = getEvalType();
        String evalTypeName = evalType != null ? evalType.getSimpleName() : null;

        // Construct string: SimpleName : EvalType.SimpleName
        StringBuffer sb = new StringBuffer(simpleName);
        if (evalTypeName != null)
            sb.append(" : ").append(evalTypeName);

        // Return
        return sb.toString();
    }
}

/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.JExpr;
import javakit.parse.JNode;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaLocalVar;
import java.util.*;

/**
 * A class to manage variables in a running interpreter session.
 */
public class JSVarStack {

    // The stack
    private Object[]  _stack = new Object[100];

    // The stack length
    private int  _stackLength;

    // The frame pointers
    private int[]  _frameIndexes = new int[100];

    // The frame pointer length
    private int  _frameIndexesLength;

    // The current frame index
    private int  _frameIndex;

    /**
     * Constructor.
     */
    public JSVarStack()
    {
        reset();
    }

    /**
     * Resets stack.
     */
    public void reset()
    {
        _stackLength = 0;
        _frameIndex = 0;
        _frameIndexes[0] = _frameIndex;
        _frameIndexesLength = 1;
    }

    /**
     * Returns a stack value at index.
     */
    public Object getStackValue(int anIndex)
    {
        int index = anIndex + _frameIndex;
        Object value = _stack[index];
        return value;
    }

    /**
     * Sets a stack value at index.
     */
    public void setStackValue(Object aValue, int anIndex)
    {
        int index = anIndex + _frameIndex;
        _stackLength = Math.max(_stackLength, index + 1);
        if (_stackLength > _stack.length)
            _stack = Arrays.copyOf(_stack, _stackLength * 2);

        _stack[index] = aValue;
    }

    /**
     * Returns a stack value for given node (JExprId or JVarDecl).
     */
    public Object getStackValueForNode(JExpr varNode)
    {
        // Get node decl - should be LocalVar with IndexInStackFrame
        JavaDecl varDecl = varNode.getDecl();
        if (varDecl instanceof JavaLocalVar) {
            JavaLocalVar localVar = (JavaLocalVar) varDecl;
            int indexInStackFrame = localVar.getIndexInStackFrame();
            if (indexInStackFrame >= 0) {
                Object value = getStackValue(indexInStackFrame);
                return value;
            }
        }

        // Should never happen
        System.err.println("JSVarStack.getStackValueForExprId: No local var stack index for id expr: " + varNode);
        return null;
    }

    /**
     * Sets a stack value for given node (JExprId or JVarDecl).
     */
    public boolean setStackValueForNode(JNode varNode, Object aValue)
    {
        // Get node decl - should be LocalVar with IndexInStackFrame
        JavaDecl varDecl = varNode.getDecl();
        if (varDecl instanceof JavaLocalVar) {
            JavaLocalVar localVar = (JavaLocalVar) varDecl;
            int indexInStackFrame = localVar.getIndexInStackFrame();
            if (indexInStackFrame >= 0) {
                setStackValue(aValue, indexInStackFrame);
                return true;
            }
        }

        // Should never happen
        System.err.println("JSVarStack.setStackValueForExprId: No local var stack index for id expr: " + varNode);
        return false;
    }

    /**
     * Push a stack frame.
     */
    public void pushStackFrame()
    {
        // Make sure FrameIndexes array has room
        if (_frameIndexesLength + 1 > _frameIndexes.length)
            _frameIndexes = Arrays.copyOf(_frameIndexes, _frameIndexes.length * 2);

        // Push FrameIndex on FrameIndexes stack
        _frameIndexes[_frameIndexesLength++] = _frameIndex;
        _frameIndex = _stackLength;
    }

    /**
     * Pops a stack frame.
     */
    public void popStackFrame()
    {
        _stackLength = _frameIndex;
        _frameIndex = _frameIndexes[--_frameIndexesLength];
    }
}

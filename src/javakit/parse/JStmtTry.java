/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;

/**
 * A Java statement for TryStatement.
 */
public class JStmtTry extends JStmt implements WithBlockStmt {

    // The statement block
    protected JStmtBlock  _block;

    // The catch blocks
    protected List<JStmtTryCatch>  _catchBlocks = new ArrayList<>();

    // The finally block
    protected JStmtBlock  _finallyBlock;

    /**
     * Returns the try block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the try block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * Returns the catch blocks.
     */
    public List<JStmtTryCatch> getCatchBlocks()  { return _catchBlocks; }

    /**
     * Adds a catch block.
     */
    public void addCatchBlock(JStmtTryCatch aBlock)
    {
        _catchBlocks.add(aBlock);
        addChild(aBlock, -1);
    }

    /**
     * Returns the finally block.
     */
    public JStmtBlock getFinallyBlock()  { return _finallyBlock; }

    /**
     * Sets the finally block.
     */
    public void setFinallyBlock(JStmtBlock aBlock)
    {
        replaceChild(_finallyBlock, _finallyBlock = aBlock);
    }

    /**
     * Adds a statement block.
     */
    public void addStatementBlock(JStmtBlock aBlock)
    {
        // If last CatchBlock doesn't have StatementBlock, set it
        int catchCount = _catchBlocks.size();
        if (catchCount > 0) {
            JStmtTryCatch lastCatchNode = _catchBlocks.get(catchCount - 1);
            if (lastCatchNode.getBlock() == null) {
                lastCatchNode.setBlock(aBlock);
                return;
            }
        }

        // Otherwise set Finally Block
        setFinallyBlock(aBlock);
    }
}
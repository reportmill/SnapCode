/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.util.ArrayUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Java statement for TryStatement.
 */
public class JStmtTry extends JStmt implements WithVarDecls, WithBlockStmt {

    // The array of resources
    private JExpr[] _resources = new JExpr[0];

    // The statement block
    protected JStmtBlock  _block;

    // The catch blocks
    protected JStmtTryCatch[] _catchBlocks = new JStmtTryCatch[0];

    // The finally block
    protected JStmtBlock  _finallyBlock;

    // Cached array of VarDecls
    private List<JVarDecl> _varDecls;

    /**
     * Constructor.
     */
    public JStmtTry()
    {
        super();
    }

    /**
     * Returns the array of resources.
     */
    public JExpr[] getResources()  { return _resources; }

    /**
     * Adds a resource.
     */
    public void addResource(JExpr aResourceExpr)
    {
        _resources = ArrayUtils.add(_resources, aResourceExpr);
        addChild(aResourceExpr);
    }

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
    public JStmtTryCatch[] getCatchBlocks()  { return _catchBlocks; }

    /**
     * Adds a catch block.
     */
    public void addCatchBlock(JStmtTryCatch aBlock)
    {
        _catchBlocks = ArrayUtils.add(_catchBlocks, aBlock);
        addChild(aBlock);
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
        int catchCount = _catchBlocks.length;
        if (catchCount > 0) {
            JStmtTryCatch lastCatchNode = _catchBlocks[catchCount - 1];
            if (lastCatchNode.getBlock() == null) {
                lastCatchNode.setBlock(aBlock);
                return;
            }
        }

        // Otherwise set Finally Block
        setFinallyBlock(aBlock);
    }

    /**
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        // If already set, just return
        if (_varDecls != null) return _varDecls;

        // If no resources, just return empty list
        if (_resources.length == 0)
            return _varDecls = Collections.EMPTY_LIST;

        // Get list of VarDecls from VarDecl expression resources
        Stream<JExpr> resourcesStream = Stream.of(_resources);
        Stream<JExprVarDecl> varDeclExprStream = (Stream<JExprVarDecl>) (Stream<?>) resourcesStream.filter(expr -> expr instanceof JExprVarDecl);
        Stream<JVarDecl> varDeclsStream = varDeclExprStream.flatMap(expr -> expr.getVarDecls().stream());
        return _varDecls = varDeclsStream.collect(Collectors.toList());
    }
}
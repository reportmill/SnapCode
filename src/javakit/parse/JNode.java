/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.*;
import snap.parse.ParseToken;
import snap.util.*;

/**
 * The base class for all nodes of a JFile.
 */
public class JNode {

    // The name for this node (if it has one)
    protected String  _name;

    // The start/end tokens for this node
    protected ParseToken _startToken, _endToken;

    // The parent node
    protected JNode  _parent;

    // The list of child nodes
    protected List<JNode> _children = Collections.EMPTY_LIST;

    // The declaration most closely associated with this node
    protected JavaDecl  _decl;

    // The type this node evaluates to (resolved, if TypeVar)
    protected JavaType _evalType;

    // The errors in this node
    private NodeError[]  _errors;

    /**
     * Constructor.
     */
    public JNode()
    {
        super();
    }

    /**
     * Returns the parent file node (root).
     */
    public JFile getFile()
    {
        return getParent(JFile.class);
    }

    /**
     * Returns the node name, if it has one.
     */
    public String getName()
    {
        // If already set, just return
        if (_name != null) return _name;

        // Get, set, return
        String name = getNameImpl();
        return _name = name;
    }

    /**
     * Sets the node name, if it has one.
     */
    public void setName(String aName)
    {
        _name = aName;
    }

    /**
     * Resolves the name, if possible.
     */
    protected String getNameImpl()  { return null; }

    /**
     * Returns whether node is a declaration name (JClassDecl JMethodDecl, JFieldDecl, JVarDecl).
     */
    public boolean isDecl()
    {
        JExprId id = this instanceof JExprId ? (JExprId) this : null;
        if (id == null)
            return false;

        //
        JNode par = id.getParent();
        return par instanceof JMemberDecl || par instanceof JVarDecl;
    }

    /**
     * Returns the JavaDecl most closely associated with this JNode.
     */
    public JavaDecl getDecl()
    {
        // If already set, just return
        if (_decl != null) return _decl;

        // Get, set, return
        JavaDecl decl = getDeclImpl();
        return _decl = decl;
    }

    /**
     * Returns the JavaDecl most closely associated with this JNode.
     */
    protected JavaDecl getDeclImpl()  { return null; }

    /**
     * Returns the JavaDecl most closely associated with given child JExprId node.
     */
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Forward to parent
        if (_parent != null)
            return _parent.getDeclForChildExprIdNode(anExprId);
        return null;
    }

    /**
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    protected JavaDecl getDeclForChildTypeNode(JType aJType)
    {
        // Forward to parent
        if (_parent != null)
            return _parent.getDeclForChildTypeNode(aJType);
        return null;
    }

    /**
     * Returns the JavaDecl that this nodes evaluates to (resolved, if TypeVar).
     */
    public JavaType getEvalType()
    {
        // If already set, just return
        if (_evalType != null) return _evalType;

        // Get, set, return
        JavaType evalType = getEvalTypeImpl();
        return _evalType = evalType;
    }

    /**
     * Returns the JavaDecl that this nodes evaluates to (resolved, if TypeVar).
     */
    protected JavaType getEvalTypeImpl()
    {
        // Get decl (just return if null)
        JavaDecl decl = getDecl();
        if (decl == null)
            return null;

        // Get EvalType
        JavaType evalType = decl.getEvalType();

        // Handle TypeVariables: Try to resolve from Class/Method TypeVars
        if (evalType != null && !evalType.isResolvedType()) {
            JavaType resolvedType = getEvalTypeImpl(this);
            evalType = resolvedType != null ? resolvedType : evalType.getEvalType();
        }

        // Return
        return evalType;
    }

    /**
     * Returns the resolved eval type for child node, if this ancestor can.
     */
    protected JavaType getEvalTypeImpl(JNode aNode)
    {
        if (_parent != null)
            return _parent.getEvalTypeImpl(aNode);

        return null;
    }

    /**
     * Returns the JavaClass that this node evaluates to.
     */
    public JavaClass getEvalClass()
    {
        JavaType evalType = getEvalType();
        JavaClass evalClass = evalType != null ? evalType.getEvalClass() : null;
        return evalClass;
    }

    /**
     * Returns the class name for this node, if it has one.
     */
    public String getEvalClassName()
    {
        JavaType evalType = getEvalType();
        return evalType != null ? evalType.getClassName() : null;
    }

    /**
     * Returns the enclosing class.
     */
    public JClassDecl getEnclosingClassDecl()
    {
        return getParent(JClassDecl.class);
    }

    /**
     * Returns the enclosing method declaration, if in method.
     */
    public JMethodDecl getEnclosingMethodDecl()
    {
        return getParent(JMethodDecl.class);
    }

    /**
     * Returns the enclosing member declaration, if in member.
     */
    public JMemberDecl getEnclosingMemberDecl()
    {
        return getParent(JMemberDecl.class);
    }

    /**
     * Returns the enclosing JavaDecl (JVarDecl, JConstrDecl, JMethodDecl or JClassDecl).
     */
    public JavaDecl getEnclosingDecl()
    {
        JNode n = getEnclosingDeclNode();
        return n != null ? n.getDecl() : null;
    }

    /**
     * Returns the enclosing JavaDecl (JVarDecl, JConstrDecl, JMethodDecl or JClassDecl).
     */
    public JNode getEnclosingDeclNode()
    {
        JNode node = getParent(JMemberDecl.class);
        if (node instanceof JFieldDecl && getParent(JVarDecl.class) != null) node = getParent(JVarDecl.class);
        return node;
    }

    /**
     * Returns the start token of this node.
     */
    public ParseToken getStartToken()  { return _startToken; }

    /**
     * Sets the start token of this node.
     */
    public void setStartToken(ParseToken aToken)
    {
        _startToken = aToken;
    }

    /**
     * Returns the start char index of this node.
     */
    public int getStartCharIndex()
    {
        return _startToken != null ? _startToken.getStartCharIndex() : 0;
    }

    /**
     * Returns the end token of this node.
     */
    public ParseToken getEndToken()  { return _endToken; }

    /**
     * Sets the end token of this node.
     */
    public void setEndToken(ParseToken aToken)
    {
        _endToken = aToken;
    }

    /**
     * Returns the end char index of this node.
     */
    public int getEndCharIndex()
    {
        return _endToken != null ? _endToken.getEndCharIndex() : 0;
    }

    /**
     * Returns the line index of this node.
     */
    public int getLineIndex()
    {
        return _startToken.getLineIndex();
    }

    /**
     * Returns the char index of this node in line.
     */
    public int getLineCharIndex()
    {
        return _startToken.getColumnIndex();
    }

    /**
     * Returns the parent node.
     */
    public JNode getParent()  { return _parent; }

    /**
     * Sets the parent node.
     */
    public void setParent(JNode aParent)
    {
        _parent = aParent;
    }

    /**
     * Returns the parent node of given class.
     */
    public <T> T getParent(Class<T> aClass)
    {
        return _parent == null || aClass.isInstance(_parent) ? (T) _parent : _parent.getParent(aClass);
    }

    /**
     * Returns the array of child nodes.
     */
    public List<JNode> getChildren()  { return _children; }

    /**
     * Returns the number of child nodes.
     */
    public final int getChildCount()  { return _children.size(); }

    /**
     * Returns the individual child node at given index.
     */
    public final JNode getChild(int anIndex)  { return _children.get(anIndex); }

    /**
     * Add child node to list.
     */
    protected void addChild(JNode aNode)
    {
        addChild(aNode, -1);
    }

    /**
     * Add child node to list.
     */
    protected void addChild(JNode aNode, int anIndex)
    {
        // Checks
        if (aNode == null) return;
        if (anIndex < 0) anIndex = _children.size();

        // Make sure Children array is real
        if (_children == Collections.EMPTY_LIST)
            _children = new ArrayList<>();

        // Add child and set Parent
        _children.add(anIndex, aNode);
        aNode.setParent(this);

        // Set StartToken
        if (getStartToken() == null || getStartCharIndex() > aNode.getStartCharIndex()) {
            if (aNode.getStartToken() == null)
                System.err.println("JNode.addChild: Bogus start token");
            else setStartToken(aNode.getStartToken());
        }

        // Set end token
        if (getEndToken() == null || getEndCharIndex() < aNode.getEndCharIndex())
            setEndToken(aNode.getEndToken());
    }

    /**
     * Removes a child.
     */
    protected int removeChild(JNode aNode)
    {
        if (aNode == null) return -1;

        // Get index and remove
        int index = ListUtils.indexOfId(_children, aNode);
        if (index >= 0)
            _children.remove(index);
        return index;
    }

    /**
     * Replaces a given child with a new child - though this is mostly used to add.
     */
    protected void replaceChild(JNode oNode, JNode nNode)
    {
        int index = oNode != null ? removeChild(oNode) : -1;
        addChild(nNode, index);
    }

    /**
     * Returns the node at given char index.
     */
    public JNode getNodeAtCharIndex(int anIndex)
    {
        // Iterate over nodes and recurse in to one in range (return top level node in range)
        for (int i = 0, iMax = getChildCount(); i < iMax; i++) {
            JNode node = getChild(i);
            if (node.getStartCharIndex() <= anIndex && anIndex <= node.getEndCharIndex())
                return node.getNodeAtCharIndex(anIndex);
        }

        // Return
        return this;
    }

    /**
     * Returns the node at given char index.
     */
    public JNode getNodeAtCharIndex(int aStart, int anEnd)
    {
        // Iterate over nodes and recurse in to one in range (return top level node in range)
        for (int i = 0, iMax = getChildCount(); i < iMax; i++) {
            JNode node = getChild(i);
            if (node.getStartCharIndex() <= aStart && anEnd <= node.getEndCharIndex())
                return node.getNodeAtCharIndex(aStart, anEnd);
        }

        // Return
        return this;
    }

    /**
     * Returns the Resolver.
     */
    public Resolver getResolver()
    {
        if (_parent != null)
            return _parent.getResolver();
        return null;
    }

    /**
     * Returns whether given class name is known.
     */
    public boolean isKnownClassName(String aName)
    {
        JavaDecl javaClass = getJavaClassForName(aName);
        return javaClass != null;
    }

    /**
     * Returns a JavaClass for given class name.
     */
    public JavaClass getJavaClassForName(String className)
    {
        Resolver resolver = getResolver(); if (resolver == null) return null;
        JavaClass javaClass = resolver.getJavaClassForName(className);
        return javaClass;
    }

    /**
     * Returns a JavaClass for given java.lang.Class.
     */
    public JavaClass getJavaClassForClass(Class<?> aClass)
    {
        Resolver resolver = getResolver(); if (resolver == null) return null;
        JavaClass javaClass = resolver.getJavaClassForClass(aClass);
        return javaClass;
    }

    /**
     * Returns whether given class name is known.
     */
    public boolean isKnownPackageName(String aName)
    {
        Resolver resolver = getResolver(); if (resolver == null) return false;
        return resolver.isKnownPackageName(aName);
        //return javaPackage != null;
    }

    /**
     * Returns a JavaPackage for given name.
     */
    public JavaPackage getJavaPackageForName(String className)
    {
        Resolver resolver = getResolver(); if (resolver == null) return null;
        JavaPackage pkg = resolver.getJavaPackageForName(className);
        return pkg;
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()
    {
        String className = getClass().getSimpleName();
        if (className.charAt(0) == 'J')
            className = className.substring(1);
        return className;
    }

    /**
     * Returns the string for this node (from Token.Tokenizer.getInput(Start,End)).
     */
    public String getString()
    {
        // Java string, and start/end tokens
        JFile jfile = getFile();
        String javaString = jfile != null ? jfile.getJavaFileString() : null;
        ParseToken startToken = getStartToken();
        ParseToken endToken = getEndToken();
        if (javaString == null || startToken == null || endToken == null)
            return "(No string available - JFile, Java string or tokens not found)";

        // Get JavaString
        int startIndex = startToken.getStartCharIndex();
        int endIndex = endToken.getEndCharIndex();
        if (endIndex > javaString.length())
            return "(No string available - token start/end out of range/synch)";

        // Return string
        String str = javaString.substring(startIndex, endIndex);
        return str;
    }

    /**
     * Returns the node errors.
     */
    public NodeError[] getErrors()
    {
        if (_errors != null) return _errors;
        return _errors = getErrorsImpl();
    }

    /**
     * Returns the node errors.
     */
    protected NodeError[] getErrorsImpl()  { return NodeError.NO_ERRORS; }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        // Get ClassName, PropStrings and full node String
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        String nodeString = getString();
        if (nodeString.length() > 800)
            nodeString = nodeString.substring(0, 800);

        // Return
        return className + " { " + propStrings + " } " + nodeString;
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();

        // Apend Name
        String name = getName();
        if (name != null)
            StringUtils.appendProp(sb, "Name", name);

        // Append StartCharIndex, EndCharIndex
        StringUtils.appendProp(sb, "Start", getStartCharIndex());
        StringUtils.appendProp(sb, "End", getEndCharIndex());

        // Append LineIndex, ColumnIndex
        ParseToken startToken = getStartToken();
        if (startToken != null) {
            StringUtils.appendProp(sb, "LineIndex", startToken.getLineIndex());
            StringUtils.appendProp(sb, "ColumnIndex", startToken.getColumnIndex());
        }

        // Append Length
        ParseToken endToken = getEndToken();
        if (endToken != null) {
            int length = endToken.getEndCharIndex() - startToken.getStartCharIndex();
            StringUtils.appendProp(sb, "Length", length);
        }

        // Return
        return sb.toString();
    }
}

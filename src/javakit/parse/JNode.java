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
    protected NodeError[]  _errors;

    // The string for this node
    private String _string;

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
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // Forward to parent
        if (_parent != null)
            return _parent.getDeclForChildId(anExprId);
        return null;
    }

    /**
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    protected JavaDecl getDeclForChildType(JType aJType)
    {
        // Forward to parent
        if (_parent != null)
            return _parent.getDeclForChildType(aJType);
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
            JavaType resolvedType = getResolvedTypeForType(evalType);
            evalType = resolvedType != null ? resolvedType : evalType.getEvalClass();
        }

        // Return
        return evalType;
    }

    /**
     * Returns a resolved type for given type.
     */
    protected JavaType getResolvedTypeForType(JavaType aType)
    {
        // Handle TypeVar
        if (aType instanceof JavaTypeVariable) {
            JavaTypeVariable typeVar = (JavaTypeVariable) aType;
            return getResolvedTypeForTypeVar(typeVar);
        }

        // Handle ParameterizedType
        else if (aType instanceof JavaParameterizedType) {

            // Get parameterized type and parameter types
            JavaParameterizedType parameterizedType = (JavaParameterizedType) aType;
            JavaType[] paramTypes = parameterizedType.getParamTypes();
            JavaType[] paramTypesResolved = paramTypes.clone();
            boolean didResolve = false;

            // Iterate over each and resolve if needed
            for (int i = 0; i < paramTypes.length; i++) {
                JavaType paramType = paramTypes[i];
                if (!paramType.isResolvedType()) {
                    JavaType paramTypeResolved = getResolvedTypeForType(paramType);
                    if (paramTypeResolved != paramType) {
                        paramTypesResolved[i] = paramTypeResolved;
                        didResolve = true;
                    }
                }
            }

            // If something was resolved, create new type with resolved parameter types
            if (didResolve) {
                JavaClass rawType = parameterizedType.getRawType();
                return rawType.getParameterizedTypeForTypes(paramTypesResolved);
            }
        }

        // Handle Generic array type
        else if (aType instanceof JavaGenericArrayType) {
            JavaGenericArrayType arrayType = (JavaGenericArrayType) aType;
            JavaType compType = arrayType.getComponentType();
            JavaType compTypeResolved = getResolvedTypeForType(compType);
            if (compTypeResolved != compType)
                return compTypeResolved.getArrayType();
        }

        // Return
        return aType;
    }

    /**
     * Returns a resolved type for given type.
     */
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // Forward to upper resolvables (parent class or method definition)
        JNode parent = getParent(JClassDecl.class);
        if (parent != null)
            return parent.getResolvedTypeForTypeVar(aTypeVar);

        return aTypeVar;
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
     * Returns whether node is a declaration id node (JClassDecl JMethodDecl, JFieldDecl, JVarDecl).
     */
    public boolean isDeclIdNode()
    {
        // If node not JExprId, return false
        if (!(this instanceof JExprId))
            return false;

        // Return whether parent is declaration node
        JNode parentNode = getParent();
        return parentNode instanceof JMemberDecl || parentNode instanceof JVarDecl;
    }

    /**
     * Returns the start token of this node.
     */
    public ParseToken getStartToken()  { return _startToken; }

    /**
     * Sets the start token of this node.
     */
    public void setStartToken(ParseToken aToken)  { _startToken = aToken; }

    /**
     * Returns the start char index of this node.
     */
    public int getStartCharIndex()  { return _startToken != null ? _startToken.getStartCharIndex() : 0; }

    /**
     * Returns the end token of this node.
     */
    public ParseToken getEndToken()  { return _endToken; }

    /**
     * Sets the end token of this node.
     */
    public void setEndToken(ParseToken aToken)  { _endToken = aToken; }

    /**
     * Returns the end char index of this node.
     */
    public int getEndCharIndex()  { return _endToken != null ? _endToken.getEndCharIndex() : 0; }

    /**
     * Returns the char length.
     */
    public int getCharLength() { return getEndCharIndex() - getStartCharIndex(); }

    /**
     * Returns the line index of this node.
     */
    public int getLineIndex()  { return _startToken.getLineIndex(); }

    /**
     * Returns the parent node.
     */
    public JNode getParent()  { return _parent; }

    /**
     * Sets the parent node.
     */
    public void setParent(JNode aParent)  { _parent = aParent; }

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
        addChild(aNode, _children.size());
    }

    /**
     * Add child node to list.
     */
    protected void addChild(JNode aNode, int anIndex)
    {
        // Checks
        if (aNode == null) return;

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
        if (index < 0)
            index = _children.size();
        addChild(nNode, index);
    }

    /**
     * Returns whether given node is ancestor of this node.
     */
    public boolean isAncestor(JNode aNode)
    {
        // Iterate up parents and return true if node is found
        for (JNode parent = _parent; parent != null; parent = parent.getParent()) {
            if (aNode == parent)
                return true;
        }

        // Return not ancestor
        return false;
    }

    /**
     * Returns the node at given char index.
     */
    public JNode getNodeForCharIndex(int anIndex)
    {
        // Iterate over nodes and recurse in to one in range (return top level node in range)
        for (int i = 0, iMax = getChildCount(); i < iMax; i++) {
            JNode node = getChild(i);
            if (node.getStartCharIndex() <= anIndex && anIndex <= node.getEndCharIndex())
                return node.getNodeForCharIndex(anIndex);
        }

        // Return
        return this;
    }

    /**
     * Returns the node for given char range.
     */
    public JNode getNodeForCharRange(int aStart, int anEnd)
    {
        // Iterate over nodes and recurse in to one in range (return top level node in range)
        for (int i = 0, iMax = getChildCount(); i < iMax; i++) {
            JNode node = getChild(i);
            if (node.getStartCharIndex() <= aStart && anEnd <= node.getEndCharIndex())
                return node.getNodeForCharRange(aStart, anEnd);
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
        if (_string != null) return _string;
        return _string = getStringImpl();
    }

    /**
     * Returns the string for this node (from Token.Tokenizer.getInput(Start,End)).
     */
    protected String getStringImpl()
    {
        // Get start/end token - if same, just return token string
        ParseToken startToken = getStartToken();
        ParseToken endToken = getEndToken();
        if (startToken == endToken && startToken != null && startToken.getString().length() > 0)
            return startToken.getString();

        // Java string, and start/end tokens
        JFile jfile = getFile();
        CharSequence javaChars = jfile != null ? jfile.getJavaChars() : null;
        if (javaChars == null || startToken == null || endToken == null)
            return createString(); // "(No string available - JFile, Java string or tokens not found)";

        // Get JavaString
        int startIndex = startToken.getStartCharIndex();
        int endIndex = endToken.getEndCharIndex();
        if (endIndex > javaChars.length())
            return createString(); // "(No string available - token start/end out of range/synch)";

        // Return string
        CharSequence nodeChars = javaChars.subSequence(startIndex, endIndex);
        return nodeChars.toString();
    }

    /**
     * Creates a string for this part.
     */
    protected String createString()  { return ""; }

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
    protected NodeError[] getErrorsImpl()
    {
        // If no children, return no errors
        if (_children == Collections.EMPTY_LIST)
            return NodeError.NO_ERRORS;

        NodeError[] errors = NodeError.NO_ERRORS;

        // Iterate over children and add any errors for each
        for (JNode child : _children) {
            NodeError[] childErrors = child.getErrors();
            if (childErrors.length > 0)
                errors = ArrayUtils.addAll(errors, childErrors);
        }

        // Return
        return errors;
    }

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
            StringUtils.appendProp(sb, "ColumnIndex", startToken.getStartCharIndexInLine());
        }

        // Append Length
        ParseToken endToken = getEndToken();
        if (endToken != null && startToken != null) {
            int length = endToken.getEndCharIndex() - startToken.getStartCharIndex();
            StringUtils.appendProp(sb, "Length", length);
        }

        // Return
        return sb.toString();
    }
}

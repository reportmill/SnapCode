/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaType;
import snap.parse.ParseToken;
import snap.util.ClassUtils;

/**
 * A JNode for types.
 */
public class JType extends JNode {

    // Whether type is primitive type
    protected boolean  _primitive;

    // Whether is reference (array or class/interface type)
    protected int  _arrayCount;

    // The generic Types
    private List<JType>  _typeArgs;

    // The base type
    private JavaType  _baseDecl;

    /**
     * Constructor.
     */
    public JType()
    {
        super();
    }

    /**
     * Returns whether type is primitive type.
     */
    public boolean isPrimitive()  { return _primitive; }

    /**
     * Sets whether type is primitive type.
     */
    public void setPrimitive(boolean aValue)  { _primitive = aValue; }

    /**
     * Returns whether type is array.
     */
    public boolean isArrayType()  { return _arrayCount > 0; }

    /**
     * Returns the array count if array type.
     */
    public int getArrayCount()  { return _arrayCount; }

    /**
     * Sets the array count.
     */
    public void setArrayCount(int aValue)  { _arrayCount = aValue; }

    /**
     * Returns the generic types.
     */
    public List<JType> getTypeArgs()  { return _typeArgs; }

    /**
     * Adds a type arg.
     */
    public void addTypeArg(JType aType)
    {
        if (_typeArgs == null) _typeArgs = new ArrayList<>();
        _typeArgs.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the number of type args.
     */
    public int getTypeArgCount()  { return _typeArgs != null ? _typeArgs.size() : 0; }

    /**
     * Returns the type arg type at given index.
     */
    public JType getTypeArg(int anIndex)  { return _typeArgs.get(anIndex); }

    /**
     * Returns the type arg decl at given index.
     */
    public JavaType getTypeArgType(int anIndex)
    {
        JType typeArg = getTypeArg(anIndex);
        return typeArg.getDecl();
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        int index = _name.lastIndexOf('.');
        return index > 0 ? _name.substring(index + 1) : _name;
    }

    /**
     * Override to return as JavaType.
     */
    @Override
    public JavaType getDecl()
    {
        JavaType javaType = (JavaType) super.getDecl();
        return javaType;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getBaseDecl()
    {
        // If already set, just return
        if (_baseDecl != null) return _baseDecl;

        // Handle primitive type
        Class<?> primitiveClass = ClassUtils.getPrimitiveClassForName(_name);
        if (primitiveClass != null)
            return _baseDecl = getJavaClassForClass(primitiveClass);

        // Try to find class directly
        JavaType javaClass = getJavaClassForName(_name);
        if (javaClass != null)
            return _baseDecl = javaClass;

        // If not primitive, try to resolve class
        javaClass = (JavaType) getDeclForChildTypeNode(this);

        // Set/return
        return _baseDecl = javaClass;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getDeclImpl()
    {
        // Get base decl
        JavaType javaType = getBaseDecl();
        if (javaType == null) {
            System.err.println("JType.getDeclImpl: Can't find base decl: " + getName());
            return getJavaClassForClass(Object.class);
        }

        // If type args, build array and get decl for ParamType
        int typeArgCount = getTypeArgCount();
        if (typeArgCount > 0) {
            JavaType[] typeArgTypes = new JavaType[typeArgCount];
            for (int i = 0; i < typeArgCount; i++)
                typeArgTypes[i] = getTypeArgType(i);
            javaType = javaType.getParamTypeDecl(typeArgTypes);
        }

        // If ArrayCount, get decl for array
        for (int i = 0; i < _arrayCount; i++)
            javaType = javaType.getArrayType();

        // If no type, complain
        if (javaType == null)
            System.err.println("JType.getDeclImpl: Shouldn't happen: decl not found for " + getName());

        // Return
        return javaType;
    }

    /**
     * A convenient builder class.
     */
    public static class Builder {

        // Ivars
        private ParseToken  _startToken, _endToken;
        private String  _name;
        private JavaType  _type;

        public Builder()  { }
        public Builder token(ParseToken aToken)  { _startToken = _endToken = aToken; return this; }
        public Builder startToken(ParseToken aToken)  { _startToken = aToken; return this; }
        public Builder endToken(ParseToken aToken)  { _endToken = aToken; return this; }
        public Builder name(String aName)  { _name = aName; return this; }
        public Builder type(JavaType aType)  { _type = aType; return this; }
        public JType build()
        {
            JType type = new JType();
            type._startToken = _startToken;
            type._endToken = _endToken;
            type._name = _name;
            type._decl = _type;
            if (_type != null) {
                type._primitive = _type.isPrimitive();
                if (_name == null)
                    type._name = _type.getName();
            }

            // Return
            return type;
        }
    }
}
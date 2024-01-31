/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.List;

/**
 * A JNode for type members: Initializer, TypeDecl, EnumDecl, ConstrDecl, FieldDecl, MedthodDecl, AnnotationDecl.
 * For JavaParseRule: ClassBodyDecl.
 */
public class JMemberDecl extends JNode implements WithId {

    // The modifiers
    protected JModifiers  _mods;

    // The name identifier
    protected JExprId  _id;

    /**
     * Constructor.
     */
    public JMemberDecl()
    {
        super();
    }

    /**
     * Returns the modifiers.
     */
    public JModifiers getMods()
    {
        if (_mods == null)
            _mods = new JModifiers();
        return _mods;
    }

    /**
     * Sets the modifiers.
     */
    public void setMods(JModifiers aValue)
    {
        if (_mods == null)
            addChild(_mods = aValue, 0);
        else replaceChild(_mods, _mods = aValue);
    }

    /**
     * Returns the identifier.
     */
    @Override
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Returns the type variables for this member.
     */
    public List<JTypeVar> getTypeVars()  { return null; }

    /**
     * Returns the type variable for this member with given name.
     */
    public JTypeVar getTypeVar(String aName)
    {
        List<JTypeVar> typeVars = getTypeVars();
        if (typeVars == null)
            return null;

        for (JTypeVar tvar : typeVars)
            if (tvar.getName().equals(aName))
                return tvar;

        // Return not found
        return null;
    }
}
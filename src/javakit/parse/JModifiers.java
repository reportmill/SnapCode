/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

import java.lang.reflect.Modifier;

/**
 * A JNode for modifiers.
 */
public class JModifiers extends JNode {

    // The modifiers value
    int _mods;

    /**
     * Returns the value.
     */
    public int getValue()
    {
        return _mods;
    }

    /**
     * Adds a modifier value.
     */
    public void addValue(int aValue)
    {
        _mods |= aValue;
    }

    /**
     * Returns whether modifiers includes abstract.
     */
    public boolean isAbstract()
    {
        return Modifier.isAbstract(_mods);
    }

    /**
     * Returns whether modifiers includes final.
     */
    public boolean isFinal()
    {
        return Modifier.isFinal(_mods);
    }

    /**
     * Returns whether modifiers includes interface.
     */
    public boolean isInterface()
    {
        return Modifier.isInterface(_mods);
    }

    /**
     * Returns whether modifiers includes native.
     */
    public boolean isNative()
    {
        return Modifier.isNative(_mods);
    }

    /**
     * Returns whether modifiers includes private.
     */
    public boolean isPrivate()
    {
        return Modifier.isPrivate(_mods);
    }

    /**
     * Returns whether modifiers includes protected.
     */
    public boolean isProtected()
    {
        return Modifier.isProtected(_mods);
    }

    /**
     * Returns whether modifiers includes public.
     */
    public boolean isPublic()
    {
        return Modifier.isPublic(_mods);
    }

    /**
     * Returns whether modifiers includes static.
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(_mods);
    }

    /**
     * Returns whether modifiers includes strict.
     */
    public boolean isStrict()
    {
        return Modifier.isStrict(_mods);
    }

    /**
     * Returns whether modifiers includes synchronized.
     */
    public boolean isSynchronized()
    {
        return Modifier.isSynchronized(_mods);
    }

}
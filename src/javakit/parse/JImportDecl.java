/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.*;

/**
 * A Java part for import declaration.
 */
public class JImportDecl extends JNode {

    // The import name expression
    protected JExpr  _nameExpr;

    // Whether import is static
    protected boolean  _static;

    // Whether import is inclusive (ends with '.*')
    protected boolean  _inclusive;

    // Whether import is used
    protected boolean  _used;

    // The list of child class names found by this import, if inclusive
    protected Set<String> _found = Collections.EMPTY_SET;

    /**
     * Constructor.
     */
    public JImportDecl()
    {
        super();
    }

    /**
     * Returns the name expression.
     */
    public JExpr getNameExpr()  { return _nameExpr; }

    /**
     * Sets the name expression.
     */
    public void setNameExpr(JExpr anExpr)
    {
        replaceChild(_nameExpr, _nameExpr = anExpr);
    }

    /**
     * Returns whether import is static.
     */
    public boolean isStatic()  { return _static; }

    /**
     * Sets whether import is static.
     */
    public void setStatic(boolean aValue)
    {
        _static = aValue;
    }

    /**
     * Returns whether import is inclusive.
     */
    public boolean isInclusive()  { return _inclusive; }

    /**
     * Sets whether import is inclusive.
     */
    public void setInclusive(boolean aValue)
    {
        _inclusive = aValue;
    }

    /**
     * Returns whether import is class name.
     */
    public boolean isClassName()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaClass;
    }

    /**
     * Returns whether import matches given name.
     * For example: import java.util.List matches "List" and "java.util.List".
     */
    public boolean matchesName(String aName)
    {
        // If inclusive, just return
        if (isInclusive())
            return false;

        // Get import name (just continue if null)
        String importName = getName();
        if (importName == null)
            return false;

        // If import matches given name
        if (importName.endsWith(aName)) {
            if (importName.length() == aName.length())
                return true;
            if (importName.charAt(importName.length() - aName.length() - 1) == '.')
                return true;
        }

        // Return no match
        return false;
    }

    /**
     * Returns whether import contains given name, either as package class (inclusive) or inner member (static class).
     * For example:
     *      import java.util.* contains "List"
     *      import static java.lang.Math.* contains "PI"
     */
    public boolean containsName(String aName)
    {
        // Get import name (just continue if null)
        String importName = getName();
        if (importName == null)
            return false;

        // If import is inclusive ("import xxx.*") and ImportName.aName is known class, return class name
        if (isInclusive()) {
            String className = importName + '.' + aName;
            if (isKnownClassName(className))
                return true;
        }

        // If import is class, see if name is inner class - should also check for inner member
        if (isClassName()) {
            String innerClassName = importName + '$' + aName;
            if (isKnownClassName(innerClassName))
                return true;
        }

        // Return no match
        return false;
    }

    /**
     * Override to get name.
     */
    @Override
    protected String getNameImpl()
    {
        return _nameExpr != null ? _nameExpr.getName() : "";
    }

    /**
     * Returns class or package declaration.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        String importName = getName();
        if (importName.isEmpty())
            return null;

        // If package name, return package
        if (_inclusive && isKnownPackageName(importName))
            return getJavaPackageForName(importName);

        // If full import name contains class, return it
        JavaClass importClass = getJavaClassForFullImportName(importName);
        if (importClass != null)
            return importClass;

        // If class not found, return as package decl anyway
        return getJavaPackageForName(importName);
    }

    /**
     * Returns class for given name.
     */
    private JavaClass getJavaClassForFullImportName(String importName)
    {
        String name = importName;

        // Iterate up parts of import till we find Class in case import is like: import path.Class.InnerClass;
        while (name != null) {

            // If known class name
            if (isKnownClassName(name)) {

                // If
                if (name.length() < getName().length()) {
                    String innerClassName = getName().substring(name.length()).replace('.', '$');
                    String name2 = name + innerClassName;
                    if (isKnownClassName(name2))
                        name = name2;
                }

                // Return
                return getJavaClassForName(name);
            }

            // Strip off last name
            int i = name.lastIndexOf('.');
            if (i > 0)
                name = name.substring(0, i);
            else name = null;
        }

        // Return not found
        return null;
    }

    /**
     * Override to return errors for import.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Handle missing package or class name
        if (getName().isEmpty())
            return NodeError.newErrorArray(this, "Import needs package or class name");

        // Handle super errors
        NodeError[] superErrors = super.getErrorsImpl();
        if (superErrors.length > 0)
            return superErrors;

        // Handle missing wildcard
        if (getDecl() instanceof JavaPackage && !isInclusive())
            return NodeError.newErrorArray(this, "Import needs to end with class name or wildcard (*)");

        // Return no errors
        return NodeError.NO_ERRORS;
    }

    /**
     * Returns the class name for a given name.
     */
    public String getImportClassName(String aName)
    {
        String className = isClassName() ? getEvalClassName() : getName();
        if (_inclusive) {
            if (!isStatic() || !className.endsWith(aName))
                className += (isClassName() ? '$' : '.') + aName;
        }
        return className;
    }

    /**
     * Returns the member for given name and parameter types (if method) for static import.
     */
    public JavaMember getImportMemberForNameAndParamTypes(String aName, JavaClass[] paramTypes)
    {
        JavaClass importClass = (JavaClass) getEvalType();
        if (importClass == null)
            return null;

        // If no params, try for field
        if (paramTypes == null)
            return importClass.getDeclaredFieldForName(aName);

        // Otherwise, look for method
        return JavaClassUtils.getCompatibleMethod(importClass, aName, paramTypes, true);
    }

    /**
     * Returns the list of child class names found by this import (if inclusive).
     */
    public Set<String> getFoundClassNames()  { return _found; }

    /**
     * Adds a child class name to list of those
     */
    protected void addFoundClassName(String aName)
    {
        if (_found == Collections.EMPTY_SET)
            _found = new HashSet<>();
        _found.add(aName);
    }

    /**
     * Override to customize for this class.
     */
    @Override
    protected String createString()
    {
        StringBuilder sb = new StringBuilder("import ");
        if (isStatic())
            sb.append("static ");
        sb.append(getName());
        if (isInclusive())
            sb.append(".*");
        sb.append(';');
        return sb.toString();
    }
}
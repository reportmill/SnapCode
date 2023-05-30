/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.*;

import java.util.*;

/**
 * This class updates a JavaClass from JClassDecl.
 */
public class JavaClassUpdaterDecl extends JavaClassUpdater {

    // The ClassDecl
    protected JClassDecl  _classDecl;

    /**
     * Constructor.
     */
    public JavaClassUpdaterDecl(JavaClass aClass, JClassDecl aClassDecl)
    {
        super(aClass);
        _classDecl = aClassDecl;
    }

    /**
     * Sets the class decl.
     */
    public void setClassDecl(JClassDecl aClassDecl)
    {
        _classDecl = aClassDecl;
        _javaClass._fieldDecls = null;
    }

    /**
     * Updates JavaDecls. Returns whether the decls changed since last update.
     */
    @Override
    public boolean updateDeclsImpl() throws SecurityException
    {
        // If first time, set decls
        if (_javaClass._fieldDecls == null)
            _javaClass._fieldDecls = Collections.EMPTY_LIST;

        // Update interfaces
        //updateInterfaces();
        _javaClass._interfaces = new JavaClass[0];

        // Update type variables
        //updateTypeVariables(realClass, removedDecls);
        _javaClass._typeVarDecls = Collections.EMPTY_LIST;

        // Update inner classes
        //updateInnerClasses();
        _javaClass._innerClasses = Collections.EMPTY_LIST;

        // Update fields
        updateFields();

        // Update methods
        updateMethods();

        // Update constructors
        //updateConstructors();
        _javaClass._constrDecls = Collections.EMPTY_LIST;

        // Return
        return true;
    }

    /**
     * Updates methods.
     */
    private void updateFields() throws SecurityException
    {
        // Get Methods
        JFieldDecl[] fieldDecls = _classDecl.getFieldDecls();
        JavaField.FieldBuilder fb = new JavaField.FieldBuilder();
        fb.init(_resolver, _javaClass.getClassName());

        // Add JavaDecl for each declared field
        for (JFieldDecl fieldDecl : fieldDecls) {

            // Get VarDecls
            List<JVarDecl> varDecls = fieldDecl.getVarDecls();

            // Iterate over varDecls
            for (JVarDecl varDecl : varDecls) {

                // Get/set name
                String fieldName = varDecl.getName();
                fb.name(fieldName);

                // Get/set type
                JType varTypeDecl = varDecl.getType();
                JavaType varType = varTypeDecl != null ? varTypeDecl.getDecl() : null;
                if (varType != null)
                    fb.type(varType);

                // Add to builder list
                fb.save();
            }
        }

        // Set fields
        JavaField[] fields = fb.buildAll();
        _javaClass._fieldDecls = Arrays.asList(fields);
    }

    /**
     * Updates methods.
     */
    private void updateMethods() throws SecurityException
    {
        // Get Methods
        JMethodDecl[] methodDecls = _classDecl.getMethodDecls();
        JavaMethod.MethodBuilder mb = new JavaMethod.MethodBuilder();
        mb.init(_resolver, _javaClass.getClassName());

        // Add JavaDecl for each declared method - also make sure return/parameter types are in refs
        for (JMethodDecl methodDecl : methodDecls) {

            // Get/set name
            String methodName = methodDecl.getName();
            mb.name(methodName);
            mb.methodDecl(methodDecl);

            // Get/set param types
            List<JVarDecl> paramsList = methodDecl.getParameters();
            JavaType[] params = new JavaType[paramsList.size()];
            for (int i = 0, iMax = paramsList.size(); i < iMax; i++) {
                JVarDecl paramDecl = paramsList.get(i);
                JavaType paramType = paramDecl.getEvalType();
                if (paramType == null)
                    paramType = _resolver.getJavaTypeForType(Object.class);
                params[i] = paramType;
            }
            mb.paramTypes(params);

            // Get/set return type
            JType returnTypeDecl = methodDecl.getType();
            JavaType returnType = returnTypeDecl != null ? returnTypeDecl.getDecl() : null;
            if (returnType != null)
                mb.returnType(returnType);

            // Add to builder list
            mb.save();
        }

        // Set Methods
        JavaMethod[] methods = mb.buildAll();
        _javaClass._methDecls = Arrays.asList(methods);
    }
}

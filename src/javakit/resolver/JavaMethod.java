/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import javakit.parse.JMethodDecl;
import snap.util.ArrayUtils;
import java.lang.reflect.*;

/**
 * This class represents a Java Method.
 */
public class JavaMethod extends JavaExecutable {

    // Whether method is Default method
    protected boolean  _default;

    // The generic return type
    protected JavaType _genericReturnType;

    // The super implementation of this method
    protected JavaMethod  _super;

    // The method
    protected Method  _method;

    // The method decl
    protected JMethodDecl  _methodDecl;

    /**
     * Constructor.
     */
    public JavaMethod(Resolver aResolver, JavaClass aDeclaringClass, Method aMethod)
    {
        super(aResolver, DeclType.Method, aDeclaringClass, aMethod);
        if (aMethod == null) return;

        // Set Method
        _method = aMethod;
    }

    /**
     * Override to customize for method.
     */
    @Override
    protected void setReader(ExecutableReader executableReader)
    {
        super.setReader(executableReader);
        _default = _execReader.isDefault();
    }

    /**
     * Returns whether Method is default type.
     */
    public boolean isDefault()  { return _default; }

    /**
     * Returns the Method.
     */
    public Method getMethod()  { return _method; }

    /**
     * Returns the return type.
     */
    public JavaClass getReturnType()
    {
        JavaType returnType = getGenericReturnType();
        return returnType.getEvalClass();
    }

    /**
     * Returns the return type.
     */
    public JavaType getGenericReturnType()
    {
        if (_genericReturnType != null) return _genericReturnType;
        return _genericReturnType = _execReader.getGenericReturnType();
    }

    /**
     * Override to get eval type dynamically.
     */
    @Override
    public JavaType getEvalType()
    {
        if (_evalType != null) return _evalType;
        return _evalType = getGenericReturnType();
    }

    /**
     * Returns the JMethodDecl.
     */
    public JMethodDecl getMethodDecl()  { return _methodDecl; }

    /**
     * Returns the super decl of this JavaDecl (Class, Method, Constructor).
     */
    public JavaMethod getSuper()
    {
        // If already set, just return
        if (_super != null)
            return _super != this ? _super : null;

        // Get superclass and helper
        JavaClass declaringClass = getDeclaringClass();
        JavaClass superClass = declaringClass != null ? declaringClass.getSuperClass() : null;
        if (superClass == null)
            return null;

        // Get super method
        String name = getName();
        JavaClass[] parameterClasses = getParameterClasses();
        JavaMethod superMethod = superClass.getMethodForNameAndClasses(name, parameterClasses);

        // If not found, check interfaces
        if (superMethod == null) {
            JavaClass[] interfaces = declaringClass.getInterfaces();
            for (JavaClass inf : interfaces) {
                superMethod = inf.getMethodForNameAndClasses(name, parameterClasses);
                if (superMethod != null)
                    break;
            }
        }

        // If not found, set to this
        if (superMethod == null)
            superMethod = this;

        // Set/return
        _super = superMethod;
        return _super != this ? _super : null;
    }

    /**
     * Returns a string representation of suggestion.
     */
    @Override
    public String getSuggestionString()
    {
        // Get normal version and ClassName
        String superName = super.getSuggestionString();
        String returnTypeStr = getReturnType().getSimpleName();

        // Construct string SimpleName(ParamType.SimpleName, ...) - ClassName
        return superName + " - " + returnTypeStr;
    }

    /**
     * Creates the Id: ClassName.methodName(param,param,...)
     */
    @Override
    protected String createId()
    {
        String classId = _declaringClass.getId();
        String methodName = getName();
        JavaClass[] paramClasses = getParameterClasses();
        String paramClassesStr = ArrayUtils.mapToStringsAndJoin(paramClasses, JavaClass::getId, ",");
        return classId + '.' + methodName + '(' + paramClassesStr + ')';
    }

    /**
     * Invokes this method on given object and args.
     */
    public Object invoke(Object anObj, Object ... theArgs) throws Exception
    {
        // Get method - check object class to get top method if overridden
        Method method = getMethod();

        // If not static receiver object class has override method, make sure we use override method
        if (!isStatic()) {
            Class<?> objClass = anObj.getClass();
            if (objClass != method.getDeclaringClass()) {
                method = objClass.getMethod(method.getName(), method.getParameterTypes());
                method.setAccessible(true); // Not sure why we need this sometimes
            }
        }

        // Invoke
        return method.invoke(anObj, theArgs);
    }

    /**
     * Returns whether method is equal to given name and parameter classes.
     */
    public boolean isEqualToNameAndClasses(String methodName, JavaClass[] paramClasses)
    {
        // If name not equal, return false
        if (!methodName.equals(getName()))
            return false;

        // Return whether given parameter classes are equal to this method's parameter classes
        JavaClass[] thisParamClasses = getParameterClasses();
        return ArrayUtils.equalsId(thisParamClasses, paramClasses);
    }
}

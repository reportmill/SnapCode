package javakit.resolver;
import snap.util.ArrayUtils;

import java.lang.reflect.*;

/**
 * This class is an ExecutableReader implementation for java.lang.Executable.
 */
public class ExecutableReaderImpl implements ExecutableReader {

    // The executable
    private Executable _executable;

    // The JavaExecutable
    private JavaExecutable _javaExecutable;

    // The Resolver
    private Resolver _resolver;

    /**
     * Constructor.
     */
    public ExecutableReaderImpl(Executable anExec)
    {
        _executable = anExec;
    }

    /**
     * Sets the JavaExecutable.
     */
    @Override
    public void setJavaExecutable(JavaExecutable anExec)
    {
        _javaExecutable = anExec;
        _resolver = _javaExecutable._resolver;
    }

    /**
     * Returns the name.
     */
    @Override
    public String getName()  { return _executable.getName(); }

    /**
     * Returns the simple name.
     */
    @Override
    public String getSimpleName()
    {
        if (_executable instanceof Constructor)
            return _javaExecutable._declaringClass.getSimpleName();
        return _executable.getName();
    }

    /**
     * Returns the modifiers.
     */
    @Override
    public int getModifiers()  { return _executable.getModifiers(); }

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    @Override
    public boolean isVarArgs()  { return _executable.isVarArgs(); }

    /**
     * Returns whether Method is default type.
     */
    @Override
    public boolean isDefault()  { return _executable instanceof Method && ((Method) _executable).isDefault(); }

    /**
     * Returns the TypeVars.
     */
    @Override
    public JavaTypeVariable[] getTypeVars()
    {
        TypeVariable<?>[] typeVars = _executable.getTypeParameters();
        return ArrayUtils.map(typeVars, tvar -> new JavaTypeVariable(_resolver, _javaExecutable, tvar), JavaTypeVariable.class);
    }

    /**
     * Returns the parameter types.
     */
    @Override
    public JavaType[] getGenericParameterTypes()
    {
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type[] paramTypesReal = _executable.getGenericParameterTypes();
        if (paramTypesReal.length < _executable.getParameterCount())
            paramTypesReal = _executable.getParameterTypes();

        // Return JavaTypes
        return _resolver.getJavaTypesForTypes(paramTypesReal);
    }

    /**
     * Returns the parameter classes.
     */
    @Override
    public JavaClass[] getParameterClasses()
    {
        Class<?>[] paramClasses = _executable.getParameterTypes();
        return ArrayUtils.map(paramClasses, _resolver::getJavaClassForClass, JavaClass.class);
    }

    /**
     * Returns the return type.
     */
    @Override
    public JavaType getGenericReturnType()
    {
        Type returnTypeReal = ((Method) _executable).getGenericReturnType();
        return _resolver.getJavaTypeForType(returnTypeReal);
    }

    /**
     * Returns the parameter names.
     */
    @Override
    public String[] getParameterNames()
    {
        Parameter[] parameters = _executable.getParameters();
        return ArrayUtils.map(parameters, param -> param.getName(), String.class);
    }
}

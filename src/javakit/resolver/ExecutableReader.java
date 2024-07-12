package javakit.resolver;

/**
 * This class provides properties for JavaExecutable (JavaMethod, JavaConstructor).
 */
public interface ExecutableReader {

    /**
     * Sets the JavaExecutable.
     */
    void setJavaExecutable(JavaExecutable anExec);

    /**
     * Returns the name.
     */
    String getName();

    /**
     * Returns the simple name.
     */
    String getSimpleName();

    /**
     * Returns the modifiers.
     */
    int getModifiers();

    /**
     * Returns whether Method/Constructor is VarArgs type.
     */
    boolean isVarArgs();

    /**
     * Returns whether Method is default type.
     */
    boolean isDefault();

    /**
     * Returns the TypeVars.
     */
    JavaTypeVariable[] getTypeParameters();

    /**
     * Returns the parameter types.
     */
    JavaType[] getGenericParameterTypes();

    /**
     * Returns the parameter classes.
     */
    JavaClass[] getParameterClasses();

    /**
     * Returns the return type.
     */
    JavaType getGenericReturnType();

    /**
     * Returns the parameter names.
     */
    String[] getParameterNames();
}

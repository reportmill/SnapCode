/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.resolver;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * A class that manages all the JavaDecls for a project.
 */
public class ResolverSys extends Resolver {

    /**
     * Constructor.
     */
    public ResolverSys()  { super(null); }

    /**
     * Constructor.
     */
    public ResolverSys(ClassLoader aClassLoader)
    {
        super(aClassLoader);
    }

    /**
     * Invokes a method on given object for name and args.
     */
    @Override
    public Object invokeMethod(Object anObj, JavaMethod javaMethod, Object[] theArgs) throws Exception
    {
        if (isTeaVM)
            return super.invokeMethod(anObj, javaMethod, theArgs);

        // Get method
        Method meth = javaMethod.getMethod();

        // If VarArgs, need to repackage args
        if (meth.isVarArgs())
            theArgs = repackageArgsForVarArgsMethod(meth, theArgs);

        // Invoke
        return meth.invoke(anObj, theArgs);
    }

    /**
     * Invokes a constructor on given class with given args.
     */
    @Override
    public Object invokeConstructor(Class<?> aClass, JavaConstructor javaConstructor, Object[] theArgs) throws Exception
    {
        if (isTeaVM)
            return super.invokeConstructor(aClass, javaConstructor, theArgs);

        // Get method
        Constructor<?> constructor = javaConstructor.getConstructor();

        // Invoke method
        return constructor.newInstance(theArgs);
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericSuperClassForClass(Class<?> aClass)
    {
        return aClass.getGenericSuperclass();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForClass(Class<?> aClass)
    {
        return aClass.getTypeParameters();
    }

    /**
     * Needed for TeaVM.
     */
    public Class<?>[] getDeclaredClassesForClass(Class<?> aClass)
    {
        return aClass.getDeclaredClasses();
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericTypeForField(Field aField)
    {
        return aField.getGenericType();
    }

    /**
     * Needed for TeaVM.
     */
    public Type getGenericReturnTypeForMethod(Method aMethod)
    {
        return aMethod.getGenericReturnType();
    }

    /**
     * Needed for TeaVM.
     */
    public TypeVariable<?>[] getTypeParametersForExecutable(Member aMember)
    {
        Executable exec = (Executable) aMember;
        return exec.getTypeParameters();
    }

    /**
     * Needed for TeaVM.
     */
    public Type[] getGenericParameterTypesForExecutable(Member aMember)
    {
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Executable exec = (Executable) aMember;
        Type[] paramTypes = exec.getGenericParameterTypes();
        if (paramTypes.length < exec.getParameterCount())
            paramTypes = exec.getParameterTypes();
        return paramTypes;
    }

    /**
     * Needed for TeaVM.
     */
    public boolean isDefaultMethod(Method aMethod)
    {
        return aMethod.isDefault();
    }

    /**
     * This method takes an array of args from a method call and repackages them for VarArgs call.
     * It basically moves collates the var args into an array.
     */
    private static Object[] repackageArgsForVarArgsMethod(Method aMethod, Object[] theArgs)
    {
        // Get VarArg class
        int argCount = aMethod.getParameterCount();
        int varArgIndex = argCount - 1;
        Class<?>[] paramClasses = aMethod.getParameterTypes();
        Class<?> varArgArrayClass = paramClasses[varArgIndex];
        Class<?> varArgClass = varArgArrayClass.getComponentType();

        // If only one varArg and it is already packaged as VarArgArrayClass, just return
        if (theArgs.length == argCount) {
            Object firstVarArg = theArgs[varArgIndex];
            if (firstVarArg.getClass() == varArgArrayClass)
                return theArgs;
        }

        // Create new args array of proper length
        Object[] args = Arrays.copyOf(theArgs, argCount);

        // Create VarArgs array of proper class and set in new args array
        int varArgsCount = theArgs.length - varArgIndex;
        Object varArgArray = args[varArgIndex] = Array.newInstance(varArgClass, varArgsCount);

        // Copy var args over from given args array to new VarArgsArray
        for (int i = 0; i < varArgsCount; i++) {
            Object varArg = theArgs[i + varArgIndex];
            Array.set(varArgArray, i, varArg);
        }

        // Return
        return args;
    }
}
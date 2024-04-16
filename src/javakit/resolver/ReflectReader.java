package javakit.resolver;
import snap.util.ArrayUtils;
import java.lang.reflect.*;

/**
 * This class provides properties for JavaExecutable, JavaMethod, JavaConstructor and JavaTypeVariable.
 */
public class ReflectReader {

    /**
     * Executable reader.
     */
    public interface ExecutableReader {

        /**
         * Sets the JavaExecutable.
         */
        void setJavaExecutable(JavaExecutable anExec);

        /**
         * Returns the id.
         */
        String getId();

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
        JavaTypeVariable[] getTypeVars();

        /**
         * Returns the parameter types.
         */
        JavaType[] getGenericParameterTypes();

        /**
         * Returns the return type.
         */
        JavaType getGenericReturnType();

        /**
         * Returns the parameter names.
         */
        String[] getParameterNames();
    }

    /**
     * This class is an ExecutableReader implementation for java.lang.Executable.
     */
    public static class ExecutableReaderImpl implements ExecutableReader {

        // The JavaExecutable
        private JavaExecutable _javaExecutable;

        // The executable
        private Executable _executable;

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
        public void setJavaExecutable(JavaExecutable anExec)  { _javaExecutable = anExec; }

        /**
         * Returns the id.
         */
        @Override
        public String getId()  { return ResolverIds.getIdForMember(_executable); }

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
            // Get TypeVariables
            TypeVariable<?>[] typeVars = _executable.getTypeParameters();
            JavaTypeVariable[] javaTypeVars = new JavaTypeVariable[typeVars.length];
            for (int i = 0, iMax = typeVars.length; i < iMax; i++)
                javaTypeVars[i] = new JavaTypeVariable(_javaExecutable._resolver, _javaExecutable, typeVars[i]);

            // Return
            return javaTypeVars;
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
            return _javaExecutable._resolver.getJavaTypesForTypes(paramTypesReal);
        }

        /**
         * Returns the return type.
         */
        @Override
        public JavaType getGenericReturnType()
        {
            Type returnTypeReal = ((Method) _executable).getGenericReturnType();
            return _javaExecutable._resolver.getJavaTypeForType(returnTypeReal);
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
}

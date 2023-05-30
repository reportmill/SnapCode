package javakit.resolver;
import javakit.resolver.JavaField.FieldBuilder;
import javakit.resolver.JavaMethod.MethodBuilder;
import javakit.resolver.JavaConstructor.ConstructorBuilder;
import snap.util.Convert;
import java.io.PrintStream;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver {

    // Shared field, method, constructor builders
    protected static FieldBuilder fb = new FieldBuilder();
    protected static MethodBuilder mb = new MethodBuilder();
    protected static ConstructorBuilder cb = new ConstructorBuilder();

    // A chained StaticResolver
    public StaticResolver  _next;

    // The shared StaticResolver
    private static StaticResolver  _shared = new StaticResolver();

    /**
     * Returns shared.
     */
    public static StaticResolver shared()  { return _shared; }

    /**
     * Returns the declared fields for given class.
     */
    public JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)
    {
        fb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.System
            case "java.lang.System":
                fb.name("out").type(PrintStream.class).save();
                return fb.name("err").type(PrintStream.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getFieldsForClass(aResolver, aClassName);
                return new JavaField[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)
    {
        mb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.Object
            case "java.lang.Object":
                mb.name("equals").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("hashCode").returnType(int.class).save();
                return mb.name("getClass").returnType(java.lang.Class.class).buildAll();

            // Handle java.util.Objects
            case "java.util.Objects":
                mb.name("equals").paramTypes(java.lang.Object.class,java.lang.Object.class).returnType(boolean.class).save();
                mb.name("toString").paramTypes(java.lang.Object.class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(java.lang.Object.class,java.lang.String.class).returnType(java.lang.String.class).save();
                return mb.name("hashCode").paramTypes(java.lang.Object.class).returnType(int.class).buildAll();

            // Handle java.lang.Class
            case "java.lang.Class":
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("getSuperclass").returnType(java.lang.Class.class).save();
                mb.name("getName").returnType(java.lang.String.class).save();
                mb.name("getInterfaces").returnType(java.lang.Class[].class).save();
                return mb.name("getSimpleName").returnType(java.lang.String.class).buildAll();

            // Handle java.lang.String
            case "java.lang.String":
                mb.name("equals").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("hashCode").returnType(int.class).save();
                mb.name("compareTo").paramTypes(java.lang.Object.class).returnType(int.class).save();
                mb.name("compareTo").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(int.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("valueOf").paramTypes(char.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(java.lang.Object.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(boolean.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(char[].class,int.class,int.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(char[].class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(double.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(float.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(long.class).returnType(java.lang.String.class).save();
                mb.name("valueOf").paramTypes(int.class).returnType(java.lang.String.class).save();
                mb.name("length").returnType(int.class).save();
                mb.name("isEmpty").returnType(boolean.class).save();
                mb.name("charAt").paramTypes(int.class).returnType(char.class).save();
                mb.name("getBytes").returnType(byte[].class).save();
                mb.name("getBytes").paramTypes(java.lang.String.class).returnType(byte[].class).save();
                mb.name("getBytes").paramTypes(java.nio.charset.Charset.class).returnType(byte[].class).save();
                mb.name("equalsIgnoreCase").paramTypes(java.lang.String.class).returnType(boolean.class).save();
                mb.name("compareToIgnoreCase").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("startsWith").paramTypes(java.lang.String.class).returnType(boolean.class).save();
                mb.name("startsWith").paramTypes(java.lang.String.class,int.class).returnType(boolean.class).save();
                mb.name("endsWith").paramTypes(java.lang.String.class).returnType(boolean.class).save();
                mb.name("lastIndexOf").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("lastIndexOf").paramTypes(int.class).returnType(int.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("substring").paramTypes(int.class).returnType(java.lang.String.class).save();
                mb.name("substring").paramTypes(int.class,int.class).returnType(java.lang.String.class).save();
                mb.name("concat").paramTypes(java.lang.String.class).returnType(java.lang.String.class).save();
                mb.name("replace").paramTypes(char.class,char.class).returnType(java.lang.String.class).save();
                mb.name("replace").paramTypes(java.lang.CharSequence.class,java.lang.CharSequence.class).returnType(java.lang.String.class).save();
                mb.name("matches").paramTypes(java.lang.String.class).returnType(boolean.class).save();
                mb.name("contains").paramTypes(java.lang.CharSequence.class).returnType(boolean.class).save();
                mb.name("replaceAll").paramTypes(java.lang.String.class,java.lang.String.class).returnType(java.lang.String.class).save();
                mb.name("split").paramTypes(java.lang.String.class,int.class).returnType(java.lang.String[].class).save();
                mb.name("split").paramTypes(java.lang.String.class).returnType(java.lang.String[].class).save();
                mb.name("join").paramTypes(java.lang.CharSequence.class,java.lang.Iterable.class).returnType(java.lang.String.class).save();
                mb.name("toLowerCase").paramTypes(java.util.Locale.class).returnType(java.lang.String.class).save();
                mb.name("toLowerCase").returnType(java.lang.String.class).save();
                mb.name("toUpperCase").returnType(java.lang.String.class).save();
                mb.name("toUpperCase").paramTypes(java.util.Locale.class).returnType(java.lang.String.class).save();
                mb.name("trim").returnType(java.lang.String.class).save();
                return mb.name("toCharArray").returnType(char[].class).buildAll();

            // Handle java.lang.Number
            case "java.lang.Number":
                mb.name("byteValue").returnType(byte.class).save();
                mb.name("shortValue").returnType(short.class).save();
                mb.name("intValue").returnType(int.class).save();
                mb.name("longValue").returnType(long.class).save();
                mb.name("floatValue").returnType(float.class).save();
                return mb.name("doubleValue").returnType(double.class).buildAll();

            // Handle java.lang.System
            case "java.lang.System":
                mb.name("getProperty").paramTypes(java.lang.String.class).returnType(java.lang.String.class).save();
                mb.name("getProperty").paramTypes(java.lang.String.class,java.lang.String.class).returnType(java.lang.String.class).save();
                mb.name("identityHashCode").paramTypes(java.lang.Object.class).returnType(int.class).save();
                mb.name("currentTimeMillis").returnType(long.class).save();
                mb.name("nanoTime").returnType(long.class).save();
                mb.name("arraycopy").paramTypes(java.lang.Object.class,int.class,java.lang.Object.class,int.class,int.class).returnType(void.class).save();
                return mb.name("getProperties").returnType(java.util.Properties.class).buildAll();

            // Handle java.lang.Math
            case "java.lang.Math":
                mb.name("abs").paramTypes(int.class).returnType(int.class).save();
                mb.name("abs").paramTypes(double.class).returnType(double.class).save();
                mb.name("abs").paramTypes(float.class).returnType(float.class).save();
                mb.name("abs").paramTypes(long.class).returnType(long.class).save();
                mb.name("sin").paramTypes(double.class).returnType(double.class).save();
                mb.name("cos").paramTypes(double.class).returnType(double.class).save();
                mb.name("tan").paramTypes(double.class).returnType(double.class).save();
                mb.name("atan2").paramTypes(double.class,double.class).returnType(double.class).save();
                mb.name("sqrt").paramTypes(double.class).returnType(double.class).save();
                mb.name("log").paramTypes(double.class).returnType(double.class).save();
                mb.name("log10").paramTypes(double.class).returnType(double.class).save();
                mb.name("pow").paramTypes(double.class,double.class).returnType(double.class).save();
                mb.name("exp").paramTypes(double.class).returnType(double.class).save();
                mb.name("min").paramTypes(long.class,long.class).returnType(long.class).save();
                mb.name("min").paramTypes(double.class,double.class).returnType(double.class).save();
                mb.name("min").paramTypes(float.class,float.class).returnType(float.class).save();
                mb.name("min").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("max").paramTypes(double.class,double.class).returnType(double.class).save();
                mb.name("max").paramTypes(float.class,float.class).returnType(float.class).save();
                mb.name("max").paramTypes(long.class,long.class).returnType(long.class).save();
                mb.name("max").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("copySign").paramTypes(float.class,float.class).returnType(float.class).save();
                mb.name("copySign").paramTypes(double.class,double.class).returnType(double.class).save();
                mb.name("acos").paramTypes(double.class).returnType(double.class).save();
                mb.name("atan").paramTypes(double.class).returnType(double.class).save();
                mb.name("toRadians").paramTypes(double.class).returnType(double.class).save();
                mb.name("toDegrees").paramTypes(double.class).returnType(double.class).save();
                mb.name("cbrt").paramTypes(double.class).returnType(double.class).save();
                mb.name("ceil").paramTypes(double.class).returnType(double.class).save();
                mb.name("floor").paramTypes(double.class).returnType(double.class).save();
                mb.name("round").paramTypes(double.class).returnType(long.class).save();
                mb.name("round").paramTypes(float.class).returnType(int.class).save();
                mb.name("random").returnType(double.class).save();
                mb.name("floorDiv").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("floorDiv").paramTypes(long.class,long.class).returnType(long.class).save();
                mb.name("floorMod").paramTypes(int.class,int.class).returnType(int.class).save();
                mb.name("floorMod").paramTypes(long.class,long.class).returnType(long.class).save();
                mb.name("sinh").paramTypes(double.class).returnType(double.class).save();
                mb.name("cosh").paramTypes(double.class).returnType(double.class).save();
                mb.name("tanh").paramTypes(double.class).returnType(double.class).save();
                return mb.name("hypot").paramTypes(double.class,double.class).returnType(double.class).buildAll();

            // Handle java.lang.StringBuffer
            case "java.lang.StringBuffer":
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("append").paramTypes(float.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(double.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(boolean.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(char.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(int.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(long.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(java.lang.Object.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(java.lang.String.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(char.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class,int.class,int.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class,int.class,int.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(char[].class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(java.lang.StringBuffer.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("append").paramTypes(char[].class,int.class,int.class).returnType(java.lang.StringBuffer.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("length").returnType(int.class).save();
                mb.name("charAt").paramTypes(int.class).returnType(char.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("substring").paramTypes(int.class).returnType(java.lang.String.class).save();
                mb.name("substring").paramTypes(int.class,int.class).returnType(java.lang.String.class).save();
                mb.name("replace").paramTypes(int.class,int.class,java.lang.String.class).returnType(java.lang.StringBuffer.class).save();
                return mb.name("delete").paramTypes(int.class,int.class).returnType(java.lang.StringBuffer.class).buildAll();

            // Handle java.lang.StringBuilder
            case "java.lang.StringBuilder":
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(char.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class,int.class,int.class).returnType(java.lang.Appendable.class).save();
                mb.name("append").paramTypes(boolean.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(char.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(int.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(java.lang.StringBuffer.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(char[].class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(java.lang.CharSequence.class,int.class,int.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(double.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(char[].class,int.class,int.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(java.lang.String.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(java.lang.Object.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(long.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("append").paramTypes(float.class).returnType(java.lang.StringBuilder.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("indexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("length").returnType(int.class).save();
                mb.name("charAt").paramTypes(int.class).returnType(char.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class,int.class).returnType(int.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.String.class).returnType(int.class).save();
                mb.name("substring").paramTypes(int.class).returnType(java.lang.String.class).save();
                mb.name("substring").paramTypes(int.class,int.class).returnType(java.lang.String.class).save();
                mb.name("replace").paramTypes(int.class,int.class,java.lang.String.class).returnType(java.lang.StringBuilder.class).save();
                return mb.name("delete").paramTypes(int.class,int.class).returnType(java.lang.StringBuilder.class).buildAll();

            // Handle java.util.Arrays
            case "java.util.Arrays":
                mb.name("equals").paramTypes(byte[].class,byte[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(boolean[].class,boolean[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(double[].class,double[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(float[].class,float[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(java.lang.Object[].class,java.lang.Object[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(short[].class,short[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(int[].class,int[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(long[].class,long[].class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(char[].class,char[].class).returnType(boolean.class).save();
                mb.name("toString").paramTypes(boolean[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(byte[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(float[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(double[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(long[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(int[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(short[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(char[].class).returnType(java.lang.String.class).save();
                mb.name("toString").paramTypes(java.lang.Object[].class).returnType(java.lang.String.class).save();
                mb.name("hashCode").paramTypes(byte[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(boolean[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(float[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(double[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(long[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(int[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(short[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(char[].class).returnType(int.class).save();
                mb.name("hashCode").paramTypes(java.lang.Object[].class).returnType(int.class).save();
                return mb.name("asList").paramTypes(java.lang.Object[].class).returnType(java.util.List.class).varArgs().buildAll();

            // Handle java.util.List
            case "java.util.List":
                mb.name("add").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("add").paramTypes(int.class,java.lang.Object.class).returnType(void.class).save();
                mb.name("remove").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("remove").paramTypes(int.class).returnType(java.lang.Object.class).save();
                mb.name("get").paramTypes(int.class).returnType(java.lang.Object.class).save();
                mb.name("equals").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("hashCode").returnType(int.class).save();
                mb.name("indexOf").paramTypes(java.lang.Object.class).returnType(int.class).save();
                mb.name("isEmpty").returnType(boolean.class).save();
                mb.name("lastIndexOf").paramTypes(java.lang.Object.class).returnType(int.class).save();
                mb.name("contains").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("replaceAll").paramTypes(java.util.function.UnaryOperator.class).returnType(void.class).save();
                mb.name("size").returnType(int.class).save();
                mb.name("toArray").returnType(java.lang.Object[].class).save();
                mb.name("toArray").paramTypes(java.lang.Object[].class).returnType(java.lang.Object[].class).save();
                mb.name("addAll").paramTypes(java.util.Collection.class).returnType(boolean.class).save();
                mb.name("addAll").paramTypes(int.class,java.util.Collection.class).returnType(boolean.class).save();
                mb.name("set").paramTypes(int.class,java.lang.Object.class).returnType(java.lang.Object.class).save();
                return mb.name("removeAll").paramTypes(java.util.Collection.class).returnType(boolean.class).buildAll();

            // Handle java.util.Map
            case "java.util.Map":
                mb.name("remove").paramTypes(java.lang.Object.class).returnType(java.lang.Object.class).save();
                mb.name("remove").paramTypes(java.lang.Object.class,java.lang.Object.class).returnType(boolean.class).save();
                mb.name("get").paramTypes(java.lang.Object.class).returnType(java.lang.Object.class).save();
                mb.name("put").paramTypes(java.lang.Object.class,java.lang.Object.class).returnType(java.lang.Object.class).save();
                mb.name("equals").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("hashCode").returnType(int.class).save();
                mb.name("isEmpty").returnType(boolean.class).save();
                mb.name("replace").paramTypes(java.lang.Object.class,java.lang.Object.class).returnType(java.lang.Object.class).save();
                mb.name("replace").paramTypes(java.lang.Object.class,java.lang.Object.class,java.lang.Object.class).returnType(boolean.class).save();
                return mb.name("size").returnType(int.class).buildAll();

            // Handle java.util.Random
            case "java.util.Random":
                mb.name("nextInt").paramTypes(int.class).returnType(int.class).save();
                mb.name("nextInt").returnType(int.class).save();
                return mb.name("nextDouble").returnType(double.class).buildAll();

            // Handle java.util.Set
            case "java.util.Set":
                mb.name("add").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("remove").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("equals").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("hashCode").returnType(int.class).save();
                mb.name("isEmpty").returnType(boolean.class).save();
                mb.name("contains").paramTypes(java.lang.Object.class).returnType(boolean.class).save();
                mb.name("size").returnType(int.class).save();
                mb.name("toArray").paramTypes(java.lang.Object[].class).returnType(java.lang.Object[].class).save();
                mb.name("toArray").returnType(java.lang.Object[].class).save();
                mb.name("addAll").paramTypes(java.util.Collection.class).returnType(boolean.class).save();
                return mb.name("removeAll").paramTypes(java.util.Collection.class).returnType(boolean.class).buildAll();

            // Handle java.io.PrintStream
            case "java.io.PrintStream":
                mb.name("println").paramTypes(double.class).returnType(void.class).save();
                mb.name("println").paramTypes(float.class).returnType(void.class).save();
                mb.name("println").paramTypes(long.class).returnType(void.class).save();
                mb.name("println").paramTypes(java.lang.Object.class).returnType(void.class).save();
                mb.name("println").paramTypes(java.lang.String.class).returnType(void.class).save();
                mb.name("println").returnType(void.class).save();
                mb.name("println").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("println").paramTypes(char.class).returnType(void.class).save();
                mb.name("println").paramTypes(int.class).returnType(void.class).save();
                mb.name("print").paramTypes(long.class).returnType(void.class).save();
                mb.name("print").paramTypes(double.class).returnType(void.class).save();
                mb.name("print").paramTypes(java.lang.Object.class).returnType(void.class).save();
                mb.name("print").paramTypes(java.lang.String.class).returnType(void.class).save();
                mb.name("print").paramTypes(char[].class).returnType(void.class).save();
                mb.name("print").paramTypes(char.class).returnType(void.class).save();
                return mb.name("print").paramTypes(int.class).returnType(void.class).buildAll();

            // Handle java.util.stream.Stream
            case "java.util.stream.Stream":
                mb.name("min").paramTypes(java.util.Comparator.class).returnType(java.util.Optional.class).save();
                mb.name("max").paramTypes(java.util.Comparator.class).returnType(java.util.Optional.class).save();
                mb.name("concat").paramTypes(java.util.stream.Stream.class,java.util.stream.Stream.class).returnType(java.util.stream.Stream.class).save();
                mb.name("toArray").paramTypes(java.util.function.IntFunction.class).returnType(java.lang.Object[].class).save();
                mb.name("toArray").returnType(java.lang.Object[].class).save();
                mb.name("of").paramTypes(java.lang.Object[].class).returnType(java.util.stream.Stream.class).varArgs().save();
                mb.name("of").paramTypes(java.lang.Object.class).returnType(java.util.stream.Stream.class).save();
                mb.name("filter").paramTypes(java.util.function.Predicate.class).returnType(java.util.stream.Stream.class).save();
                return mb.name("map").paramTypes(java.util.function.Function.class).returnType(java.util.stream.Stream.class).buildAll();

            // Handle java.util.stream.DoubleStream
            case "java.util.stream.DoubleStream":
                mb.name("min").returnType(java.util.OptionalDouble.class).save();
                mb.name("max").returnType(java.util.OptionalDouble.class).save();
                mb.name("concat").paramTypes(java.util.stream.DoubleStream.class,java.util.stream.DoubleStream.class).returnType(java.util.stream.DoubleStream.class).save();
                mb.name("toArray").returnType(double[].class).save();
                mb.name("of").paramTypes(double.class).returnType(java.util.stream.DoubleStream.class).save();
                mb.name("filter").paramTypes(java.util.function.DoublePredicate.class).returnType(java.util.stream.DoubleStream.class).save();
                return mb.name("map").paramTypes(java.util.function.DoubleUnaryOperator.class).returnType(java.util.stream.DoubleStream.class).buildAll();

            // Handle java.util.function.DoubleUnaryOperator
            case "java.util.function.DoubleUnaryOperator":
                return mb.name("applyAsDouble").paramTypes(double.class).returnType(double.class).buildAll();

            // Handle java.util.function.DoubleBinaryOperator
            case "java.util.function.DoubleBinaryOperator":
                return mb.name("applyAsDouble").paramTypes(double.class,double.class).returnType(double.class).buildAll();

            // Handle snap.props.PropObject
            case "snap.props.PropObject":
                mb.name("getPropValue").paramTypes(java.lang.String.class).returnType(java.lang.Object.class).save();
                mb.name("setPropValue").paramTypes(java.lang.String.class,java.lang.Object.class).returnType(void.class).save();
                return mb.name("toString").returnType(java.lang.String.class).buildAll();

            // Handle snap.view.View
            case "snap.view.View":
                mb.name("getPropValue").paramTypes(java.lang.String.class).returnType(java.lang.Object.class).save();
                mb.name("setPropValue").paramTypes(java.lang.String.class,java.lang.Object.class).returnType(void.class).save();
                mb.name("getPrefWidth").returnType(double.class).save();
                mb.name("getPrefWidth").paramTypes(double.class).returnType(double.class).save();
                mb.name("setPrefWidth").paramTypes(double.class).returnType(void.class).save();
                mb.name("getPrefHeight").returnType(double.class).save();
                mb.name("getPrefHeight").paramTypes(double.class).returnType(double.class).save();
                mb.name("setPrefHeight").paramTypes(double.class).returnType(void.class).save();
                mb.name("getPrefSize").returnType(snap.geom.Size.class).save();
                mb.name("setPrefSize").paramTypes(snap.geom.Size.class).returnType(void.class).save();
                mb.name("setPrefSize").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("getBorder").returnType(snap.gfx.Border.class).save();
                mb.name("setBorder").paramTypes(snap.gfx.Color.class,double.class).returnType(void.class).save();
                mb.name("setBorder").paramTypes(snap.gfx.Border.class).returnType(void.class).save();
                mb.name("getEffect").returnType(snap.gfx.Effect.class).save();
                mb.name("setEffect").paramTypes(snap.gfx.Effect.class).returnType(void.class).save();
                mb.name("getOpacity").returnType(double.class).save();
                mb.name("setOpacity").paramTypes(double.class).returnType(void.class).save();
                mb.name("getMargin").returnType(snap.geom.Insets.class).save();
                mb.name("setMargin").paramTypes(snap.geom.Insets.class).returnType(void.class).save();
                mb.name("setMargin").paramTypes(double.class,double.class,double.class,double.class).returnType(void.class).save();
                mb.name("getPadding").returnType(snap.geom.Insets.class).save();
                mb.name("setPadding").paramTypes(double.class,double.class,double.class,double.class).returnType(void.class).save();
                mb.name("setPadding").paramTypes(snap.geom.Insets.class).returnType(void.class).save();
                mb.name("getSpacing").returnType(double.class).save();
                mb.name("setSpacing").paramTypes(double.class).returnType(void.class).save();
                mb.name("isVisible").returnType(boolean.class).save();
                mb.name("setVisible").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("setRotate").paramTypes(double.class).returnType(void.class).save();
                mb.name("setScaleX").paramTypes(double.class).returnType(void.class).save();
                mb.name("setScaleY").paramTypes(double.class).returnType(void.class).save();
                mb.name("setTransX").paramTypes(double.class).returnType(void.class).save();
                mb.name("setTransY").paramTypes(double.class).returnType(void.class).save();
                mb.name("getFill").returnType(snap.gfx.Paint.class).save();
                mb.name("setFill").paramTypes(snap.gfx.Paint.class).returnType(void.class).save();
                mb.name("setScale").paramTypes(double.class).returnType(void.class).save();
                mb.name("getAnim").paramTypes(int.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setText").paramTypes(java.lang.String.class).returnType(void.class).save();
                mb.name("getName").returnType(java.lang.String.class).save();
                mb.name("contains").paramTypes(double.class,double.class).returnType(boolean.class).save();
                mb.name("contains").paramTypes(snap.geom.Shape.class).returnType(boolean.class).save();
                return mb.name("contains").paramTypes(snap.geom.Point.class).returnType(boolean.class).buildAll();

            // Handle snap.view.ChildView
            case "snap.view.ChildView":
                mb.name("removeChild").paramTypes(int.class).returnType(snap.view.View.class).save();
                mb.name("removeChild").paramTypes(snap.view.View.class).returnType(int.class).save();
                mb.name("setChildren").paramTypes(snap.view.View[].class).returnType(void.class).varArgs().save();
                mb.name("removeChildren").returnType(void.class).save();
                mb.name("addChild").paramTypes(snap.view.View.class,int.class).returnType(void.class).save();
                return mb.name("addChild").paramTypes(snap.view.View.class).returnType(void.class).buildAll();

            // Handle snap.view.Label
            case "snap.view.Label":
                mb.name("setText").paramTypes(java.lang.String.class).returnType(void.class).save();
                return mb.name("toString").returnType(java.lang.String.class).buildAll();

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim":
                mb.name("setPrefWidth").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setPrefHeight").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setPrefSize").paramTypes(double.class,double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setOpacity").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setRotate").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setScaleX").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setScaleY").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setTransX").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setTransY").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setLoopCount").paramTypes(int.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setOnFinish").paramTypes(java.lang.Runnable.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setOnFinish").paramTypes(java.util.function.Consumer.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setFill").paramTypes(snap.gfx.Paint.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("setScale").paramTypes(double.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("getAnim").paramTypes(int.class).returnType(snap.view.ViewAnim.class).save();
                mb.name("play").returnType(void.class).save();
                mb.name("toString").returnType(java.lang.String.class).save();
                return mb.name("isEmpty").returnType(boolean.class).buildAll();

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner":
                return mb.name("setWindowVisible").paramTypes(boolean.class).returnType(void.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getMethodsForClass(aResolver, aClassName);
                return new JavaMethod[0];
        }
    }

    /**
     * Invokes methods for given method id, object and args.
     */
    public Object invokeMethod(String anId, Object anObj, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle java.lang.Object
            case "java.lang.Object.equals(java.lang.Object)":
                return ((java.lang.Object) anObj).equals(theArgs[0]);
            case "java.lang.Object.toString()":
                return ((java.lang.Object) anObj).toString();
            case "java.lang.Object.hashCode()":
                return ((java.lang.Object) anObj).hashCode();
            case "java.lang.Object.getClass()":
                return ((java.lang.Object) anObj).getClass();

            // Handle java.util.Objects
            case "java.util.Objects.equals(java.lang.Object,java.lang.Object)":
                return java.util.Objects.equals(theArgs[0],theArgs[1]);
            case "java.util.Objects.toString(java.lang.Object)":
                return java.util.Objects.toString(theArgs[0]);
            case "java.util.Objects.toString(java.lang.Object,java.lang.String)":
                return java.util.Objects.toString(theArgs[0],(java.lang.String) theArgs[1]);
            case "java.util.Objects.hashCode(java.lang.Object)":
                return java.util.Objects.hashCode(theArgs[0]);

            // Handle java.lang.Class
            case "java.lang.Class.getSuperclass()":
                return ((java.lang.Class) anObj).getSuperclass();
            case "java.lang.Class.getName()":
                return ((java.lang.Class) anObj).getName();
            case "java.lang.Class.getInterfaces()":
                return ((java.lang.Class) anObj).getInterfaces();
            case "java.lang.Class.getSimpleName()":
                return ((java.lang.Class) anObj).getSimpleName();

            // Handle java.lang.String
            case "java.lang.String.compareTo(java.lang.String)":
                return ((java.lang.String) anObj).compareTo((java.lang.String) theArgs[0]);
            case "java.lang.String.indexOf(java.lang.String,int)":
                return ((java.lang.String) anObj).indexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.String.indexOf(int)":
                return ((java.lang.String) anObj).indexOf(intVal(theArgs[0]));
            case "java.lang.String.indexOf(java.lang.String)":
                return ((java.lang.String) anObj).indexOf((java.lang.String) theArgs[0]);
            case "java.lang.String.indexOf(int,int)":
                return ((java.lang.String) anObj).indexOf(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.String.valueOf(char)":
                return java.lang.String.valueOf((char) theArgs[0]);
            case "java.lang.String.valueOf(java.lang.Object)":
                return java.lang.String.valueOf(theArgs[0]);
            case "java.lang.String.valueOf(boolean)":
                return java.lang.String.valueOf(boolVal(theArgs[0]));
            case "java.lang.String.valueOf(char[],int,int)":
                return java.lang.String.valueOf((char[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.String.valueOf(char[])":
                return java.lang.String.valueOf((char[]) theArgs[0]);
            case "java.lang.String.valueOf(double)":
                return java.lang.String.valueOf(doubleVal(theArgs[0]));
            case "java.lang.String.valueOf(float)":
                return java.lang.String.valueOf(floatVal(theArgs[0]));
            case "java.lang.String.valueOf(long)":
                return java.lang.String.valueOf((long) theArgs[0]);
            case "java.lang.String.valueOf(int)":
                return java.lang.String.valueOf(intVal(theArgs[0]));
            case "java.lang.String.length()":
                return ((java.lang.String) anObj).length();
            case "java.lang.String.isEmpty()":
                return ((java.lang.String) anObj).isEmpty();
            case "java.lang.String.charAt(int)":
                return ((java.lang.String) anObj).charAt(intVal(theArgs[0]));
            case "java.lang.String.getBytes()":
                return ((java.lang.String) anObj).getBytes();
            case "java.lang.String.getBytes(java.lang.String)":
                return ((java.lang.String) anObj).getBytes((java.lang.String) theArgs[0]);
            case "java.lang.String.getBytes(java.nio.charset.Charset)":
                return ((java.lang.String) anObj).getBytes((java.nio.charset.Charset) theArgs[0]);
            case "java.lang.String.equalsIgnoreCase(java.lang.String)":
                return ((java.lang.String) anObj).equalsIgnoreCase((java.lang.String) theArgs[0]);
            case "java.lang.String.compareToIgnoreCase(java.lang.String)":
                return ((java.lang.String) anObj).compareToIgnoreCase((java.lang.String) theArgs[0]);
            case "java.lang.String.startsWith(java.lang.String)":
                return ((java.lang.String) anObj).startsWith((java.lang.String) theArgs[0]);
            case "java.lang.String.startsWith(java.lang.String,int)":
                return ((java.lang.String) anObj).startsWith((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.String.endsWith(java.lang.String)":
                return ((java.lang.String) anObj).endsWith((java.lang.String) theArgs[0]);
            case "java.lang.String.lastIndexOf(int,int)":
                return ((java.lang.String) anObj).lastIndexOf(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.String.lastIndexOf(java.lang.String,int)":
                return ((java.lang.String) anObj).lastIndexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.String.lastIndexOf(int)":
                return ((java.lang.String) anObj).lastIndexOf(intVal(theArgs[0]));
            case "java.lang.String.lastIndexOf(java.lang.String)":
                return ((java.lang.String) anObj).lastIndexOf((java.lang.String) theArgs[0]);
            case "java.lang.String.substring(int)":
                return ((java.lang.String) anObj).substring(intVal(theArgs[0]));
            case "java.lang.String.substring(int,int)":
                return ((java.lang.String) anObj).substring(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.String.concat(java.lang.String)":
                return ((java.lang.String) anObj).concat((java.lang.String) theArgs[0]);
            case "java.lang.String.replace(char,char)":
                return ((java.lang.String) anObj).replace((char) theArgs[0],(char) theArgs[1]);
            case "java.lang.String.replace(java.lang.CharSequence,java.lang.CharSequence)":
                return ((java.lang.String) anObj).replace((java.lang.CharSequence) theArgs[0],(java.lang.CharSequence) theArgs[1]);
            case "java.lang.String.matches(java.lang.String)":
                return ((java.lang.String) anObj).matches((java.lang.String) theArgs[0]);
            case "java.lang.String.contains(java.lang.CharSequence)":
                return ((java.lang.String) anObj).contains((java.lang.CharSequence) theArgs[0]);
            case "java.lang.String.replaceAll(java.lang.String,java.lang.String)":
                return ((java.lang.String) anObj).replaceAll((java.lang.String) theArgs[0],(java.lang.String) theArgs[1]);
            case "java.lang.String.split(java.lang.String,int)":
                return ((java.lang.String) anObj).split((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.String.split(java.lang.String)":
                return ((java.lang.String) anObj).split((java.lang.String) theArgs[0]);
            case "java.lang.String.join(java.lang.CharSequence,java.lang.Iterable)":
                return java.lang.String.join((java.lang.CharSequence) theArgs[0],(java.lang.Iterable) theArgs[1]);
            case "java.lang.String.toLowerCase(java.util.Locale)":
                return ((java.lang.String) anObj).toLowerCase((java.util.Locale) theArgs[0]);
            case "java.lang.String.toLowerCase()":
                return ((java.lang.String) anObj).toLowerCase();
            case "java.lang.String.toUpperCase()":
                return ((java.lang.String) anObj).toUpperCase();
            case "java.lang.String.toUpperCase(java.util.Locale)":
                return ((java.lang.String) anObj).toUpperCase((java.util.Locale) theArgs[0]);
            case "java.lang.String.trim()":
                return ((java.lang.String) anObj).trim();
            case "java.lang.String.toCharArray()":
                return ((java.lang.String) anObj).toCharArray();

            // Handle java.lang.Number
            case "java.lang.Number.byteValue()":
                return ((java.lang.Number) anObj).byteValue();
            case "java.lang.Number.shortValue()":
                return ((java.lang.Number) anObj).shortValue();
            case "java.lang.Number.intValue()":
                return ((java.lang.Number) anObj).intValue();
            case "java.lang.Number.longValue()":
                return ((java.lang.Number) anObj).longValue();
            case "java.lang.Number.floatValue()":
                return ((java.lang.Number) anObj).floatValue();
            case "java.lang.Number.doubleValue()":
                return ((java.lang.Number) anObj).doubleValue();

            // Handle java.lang.System
            case "java.lang.System.getProperty(java.lang.String)":
                return java.lang.System.getProperty((java.lang.String) theArgs[0]);
            case "java.lang.System.getProperty(java.lang.String,java.lang.String)":
                return java.lang.System.getProperty((java.lang.String) theArgs[0],(java.lang.String) theArgs[1]);
            case "java.lang.System.identityHashCode(java.lang.Object)":
                return java.lang.System.identityHashCode(theArgs[0]);
            case "java.lang.System.currentTimeMillis()":
                return java.lang.System.currentTimeMillis();
            case "java.lang.System.nanoTime()":
                return java.lang.System.nanoTime();
            case "java.lang.System.arraycopy(java.lang.Object,int,java.lang.Object,int,int)":
                java.lang.System.arraycopy(theArgs[0],intVal(theArgs[1]),theArgs[2],intVal(theArgs[3]),intVal(theArgs[4])); return null;
            case "java.lang.System.getProperties()":
                return java.lang.System.getProperties();

            // Handle java.lang.Math
            case "java.lang.Math.abs(int)":
                return java.lang.Math.abs(intVal(theArgs[0]));
            case "java.lang.Math.abs(double)":
                return java.lang.Math.abs(doubleVal(theArgs[0]));
            case "java.lang.Math.abs(float)":
                return java.lang.Math.abs(floatVal(theArgs[0]));
            case "java.lang.Math.abs(long)":
                return java.lang.Math.abs((long) theArgs[0]);
            case "java.lang.Math.sin(double)":
                return java.lang.Math.sin(doubleVal(theArgs[0]));
            case "java.lang.Math.cos(double)":
                return java.lang.Math.cos(doubleVal(theArgs[0]));
            case "java.lang.Math.tan(double)":
                return java.lang.Math.tan(doubleVal(theArgs[0]));
            case "java.lang.Math.atan2(double,double)":
                return java.lang.Math.atan2(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "java.lang.Math.sqrt(double)":
                return java.lang.Math.sqrt(doubleVal(theArgs[0]));
            case "java.lang.Math.log(double)":
                return java.lang.Math.log(doubleVal(theArgs[0]));
            case "java.lang.Math.log10(double)":
                return java.lang.Math.log10(doubleVal(theArgs[0]));
            case "java.lang.Math.pow(double,double)":
                return java.lang.Math.pow(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "java.lang.Math.exp(double)":
                return java.lang.Math.exp(doubleVal(theArgs[0]));
            case "java.lang.Math.min(long,long)":
                return java.lang.Math.min((long) theArgs[0],(long) theArgs[1]);
            case "java.lang.Math.min(double,double)":
                return java.lang.Math.min(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "java.lang.Math.min(float,float)":
                return java.lang.Math.min(floatVal(theArgs[0]),floatVal(theArgs[1]));
            case "java.lang.Math.min(int,int)":
                return java.lang.Math.min(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.Math.max(double,double)":
                return java.lang.Math.max(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "java.lang.Math.max(float,float)":
                return java.lang.Math.max(floatVal(theArgs[0]),floatVal(theArgs[1]));
            case "java.lang.Math.max(long,long)":
                return java.lang.Math.max((long) theArgs[0],(long) theArgs[1]);
            case "java.lang.Math.max(int,int)":
                return java.lang.Math.max(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.Math.copySign(float,float)":
                return java.lang.Math.copySign(floatVal(theArgs[0]),floatVal(theArgs[1]));
            case "java.lang.Math.copySign(double,double)":
                return java.lang.Math.copySign(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "java.lang.Math.acos(double)":
                return java.lang.Math.acos(doubleVal(theArgs[0]));
            case "java.lang.Math.atan(double)":
                return java.lang.Math.atan(doubleVal(theArgs[0]));
            case "java.lang.Math.toRadians(double)":
                return java.lang.Math.toRadians(doubleVal(theArgs[0]));
            case "java.lang.Math.toDegrees(double)":
                return java.lang.Math.toDegrees(doubleVal(theArgs[0]));
            case "java.lang.Math.cbrt(double)":
                return java.lang.Math.cbrt(doubleVal(theArgs[0]));
            case "java.lang.Math.ceil(double)":
                return java.lang.Math.ceil(doubleVal(theArgs[0]));
            case "java.lang.Math.floor(double)":
                return java.lang.Math.floor(doubleVal(theArgs[0]));
            case "java.lang.Math.round(double)":
                return java.lang.Math.round(doubleVal(theArgs[0]));
            case "java.lang.Math.round(float)":
                return java.lang.Math.round(floatVal(theArgs[0]));
            case "java.lang.Math.random()":
                return java.lang.Math.random();
            case "java.lang.Math.floorDiv(int,int)":
                return java.lang.Math.floorDiv(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.Math.floorDiv(long,long)":
                return java.lang.Math.floorDiv((long) theArgs[0],(long) theArgs[1]);
            case "java.lang.Math.floorMod(int,int)":
                return java.lang.Math.floorMod(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.Math.floorMod(long,long)":
                return java.lang.Math.floorMod((long) theArgs[0],(long) theArgs[1]);
            case "java.lang.Math.sinh(double)":
                return java.lang.Math.sinh(doubleVal(theArgs[0]));
            case "java.lang.Math.cosh(double)":
                return java.lang.Math.cosh(doubleVal(theArgs[0]));
            case "java.lang.Math.tanh(double)":
                return java.lang.Math.tanh(doubleVal(theArgs[0]));
            case "java.lang.Math.hypot(double,double)":
                return java.lang.Math.hypot(doubleVal(theArgs[0]),doubleVal(theArgs[1]));

            // Handle java.lang.StringBuffer
            case "java.lang.StringBuffer.toString()":
                return ((java.lang.StringBuffer) anObj).toString();
            case "java.lang.StringBuffer.append(float)":
                return ((java.lang.StringBuffer) anObj).append(floatVal(theArgs[0]));
            case "java.lang.StringBuffer.append(double)":
                return ((java.lang.StringBuffer) anObj).append(doubleVal(theArgs[0]));
            case "java.lang.StringBuffer.append(boolean)":
                return ((java.lang.StringBuffer) anObj).append(boolVal(theArgs[0]));
            case "java.lang.StringBuffer.append(char)":
                return ((java.lang.StringBuffer) anObj).append((char) theArgs[0]);
            case "java.lang.StringBuffer.append(int)":
                return ((java.lang.StringBuffer) anObj).append(intVal(theArgs[0]));
            case "java.lang.StringBuffer.append(long)":
                return ((java.lang.StringBuffer) anObj).append((long) theArgs[0]);
            case "java.lang.StringBuffer.append(java.lang.Object)":
                return ((java.lang.StringBuffer) anObj).append(theArgs[0]);
            case "java.lang.StringBuffer.append(java.lang.String)":
                return ((java.lang.StringBuffer) anObj).append((java.lang.String) theArgs[0]);
            case "java.lang.StringBuffer.append(java.lang.CharSequence)":
                return ((java.lang.StringBuffer) anObj).append((java.lang.CharSequence) theArgs[0]);
            case "java.lang.StringBuffer.append(java.lang.CharSequence,int,int)":
                return ((java.lang.StringBuffer) anObj).append((java.lang.CharSequence) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.StringBuffer.append(char[])":
                return ((java.lang.StringBuffer) anObj).append((char[]) theArgs[0]);
            case "java.lang.StringBuffer.append(java.lang.StringBuffer)":
                return ((java.lang.StringBuffer) anObj).append((java.lang.StringBuffer) theArgs[0]);
            case "java.lang.StringBuffer.append(char[],int,int)":
                return ((java.lang.StringBuffer) anObj).append((char[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.StringBuffer.indexOf(java.lang.String)":
                return ((java.lang.StringBuffer) anObj).indexOf((java.lang.String) theArgs[0]);
            case "java.lang.StringBuffer.indexOf(java.lang.String,int)":
                return ((java.lang.StringBuffer) anObj).indexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.StringBuffer.length()":
                return ((java.lang.StringBuffer) anObj).length();
            case "java.lang.StringBuffer.charAt(int)":
                return ((java.lang.StringBuffer) anObj).charAt(intVal(theArgs[0]));
            case "java.lang.StringBuffer.lastIndexOf(java.lang.String,int)":
                return ((java.lang.StringBuffer) anObj).lastIndexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.StringBuffer.lastIndexOf(java.lang.String)":
                return ((java.lang.StringBuffer) anObj).lastIndexOf((java.lang.String) theArgs[0]);
            case "java.lang.StringBuffer.substring(int)":
                return ((java.lang.StringBuffer) anObj).substring(intVal(theArgs[0]));
            case "java.lang.StringBuffer.substring(int,int)":
                return ((java.lang.StringBuffer) anObj).substring(intVal(theArgs[0]),intVal(theArgs[1]));
            case "java.lang.StringBuffer.replace(int,int,java.lang.String)":
                return ((java.lang.StringBuffer) anObj).replace(intVal(theArgs[0]),intVal(theArgs[1]),(java.lang.String) theArgs[2]);
            case "java.lang.StringBuffer.delete(int,int)":
                return ((java.lang.StringBuffer) anObj).delete(intVal(theArgs[0]),intVal(theArgs[1]));

            // Handle java.lang.StringBuilder
            case "java.lang.StringBuilder.toString()":
                return ((java.lang.StringBuilder) anObj).toString();
            case "java.lang.StringBuilder.append(boolean)":
                return ((java.lang.StringBuilder) anObj).append(boolVal(theArgs[0]));
            case "java.lang.StringBuilder.append(java.lang.CharSequence)":
                return ((java.lang.StringBuilder) anObj).append((java.lang.CharSequence) theArgs[0]);
            case "java.lang.StringBuilder.append(char)":
                return ((java.lang.StringBuilder) anObj).append((char) theArgs[0]);
            case "java.lang.StringBuilder.append(int)":
                return ((java.lang.StringBuilder) anObj).append(intVal(theArgs[0]));
            case "java.lang.StringBuilder.append(java.lang.StringBuffer)":
                return ((java.lang.StringBuilder) anObj).append((java.lang.StringBuffer) theArgs[0]);
            case "java.lang.StringBuilder.append(char[])":
                return ((java.lang.StringBuilder) anObj).append((char[]) theArgs[0]);
            case "java.lang.StringBuilder.append(java.lang.CharSequence,int,int)":
                return ((java.lang.StringBuilder) anObj).append((java.lang.CharSequence) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.StringBuilder.append(double)":
                return ((java.lang.StringBuilder) anObj).append(doubleVal(theArgs[0]));
            case "java.lang.StringBuilder.append(char[],int,int)":
                return ((java.lang.StringBuilder) anObj).append((char[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.StringBuilder.append(java.lang.String)":
                return ((java.lang.StringBuilder) anObj).append((java.lang.String) theArgs[0]);
            case "java.lang.StringBuilder.append(java.lang.Object)":
                return ((java.lang.StringBuilder) anObj).append(theArgs[0]);
            case "java.lang.StringBuilder.append(long)":
                return ((java.lang.StringBuilder) anObj).append((long) theArgs[0]);
            case "java.lang.StringBuilder.append(float)":
                return ((java.lang.StringBuilder) anObj).append(floatVal(theArgs[0]));
            case "java.lang.StringBuilder.indexOf(java.lang.String,int)":
                return ((java.lang.StringBuilder) anObj).indexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.StringBuilder.indexOf(java.lang.String)":
                return ((java.lang.StringBuilder) anObj).indexOf((java.lang.String) theArgs[0]);
            case "java.lang.StringBuilder.lastIndexOf(java.lang.String,int)":
                return ((java.lang.StringBuilder) anObj).lastIndexOf((java.lang.String) theArgs[0],intVal(theArgs[1]));
            case "java.lang.StringBuilder.lastIndexOf(java.lang.String)":
                return ((java.lang.StringBuilder) anObj).lastIndexOf((java.lang.String) theArgs[0]);
            case "java.lang.StringBuilder.replace(int,int,java.lang.String)":
                return ((java.lang.StringBuilder) anObj).replace(intVal(theArgs[0]),intVal(theArgs[1]),(java.lang.String) theArgs[2]);
            case "java.lang.StringBuilder.delete(int,int)":
                return ((java.lang.StringBuilder) anObj).delete(intVal(theArgs[0]),intVal(theArgs[1]));

            // Handle java.util.Arrays
            case "java.util.Arrays.equals(byte[],byte[])":
                return java.util.Arrays.equals((byte[]) theArgs[0],(byte[]) theArgs[1]);
            case "java.util.Arrays.equals(boolean[],boolean[])":
                return java.util.Arrays.equals((boolean[]) theArgs[0],(boolean[]) theArgs[1]);
            case "java.util.Arrays.equals(double[],double[])":
                return java.util.Arrays.equals((double[]) theArgs[0],(double[]) theArgs[1]);
            case "java.util.Arrays.equals(float[],float[])":
                return java.util.Arrays.equals((float[]) theArgs[0],(float[]) theArgs[1]);
            case "java.util.Arrays.equals(java.lang.Object[],java.lang.Object[])":
                return java.util.Arrays.equals((Object[]) theArgs[0],(Object[]) theArgs[1]);
            case "java.util.Arrays.equals(short[],short[])":
                return java.util.Arrays.equals((short[]) theArgs[0],(short[]) theArgs[1]);
            case "java.util.Arrays.equals(int[],int[])":
                return java.util.Arrays.equals((int[]) theArgs[0],(int[]) theArgs[1]);
            case "java.util.Arrays.equals(long[],long[])":
                return java.util.Arrays.equals((long[]) theArgs[0],(long[]) theArgs[1]);
            case "java.util.Arrays.equals(char[],char[])":
                return java.util.Arrays.equals((char[]) theArgs[0],(char[]) theArgs[1]);
            case "java.util.Arrays.toString(boolean[])":
                return java.util.Arrays.toString((boolean[]) theArgs[0]);
            case "java.util.Arrays.toString(byte[])":
                return java.util.Arrays.toString((byte[]) theArgs[0]);
            case "java.util.Arrays.toString(float[])":
                return java.util.Arrays.toString((float[]) theArgs[0]);
            case "java.util.Arrays.toString(double[])":
                return java.util.Arrays.toString((double[]) theArgs[0]);
            case "java.util.Arrays.toString(long[])":
                return java.util.Arrays.toString((long[]) theArgs[0]);
            case "java.util.Arrays.toString(int[])":
                return java.util.Arrays.toString((int[]) theArgs[0]);
            case "java.util.Arrays.toString(short[])":
                return java.util.Arrays.toString((short[]) theArgs[0]);
            case "java.util.Arrays.toString(char[])":
                return java.util.Arrays.toString((char[]) theArgs[0]);
            case "java.util.Arrays.toString(java.lang.Object[])":
                return java.util.Arrays.toString((Object[]) theArgs[0]);
            case "java.util.Arrays.hashCode(byte[])":
                return java.util.Arrays.hashCode((byte[]) theArgs[0]);
            case "java.util.Arrays.hashCode(boolean[])":
                return java.util.Arrays.hashCode((boolean[]) theArgs[0]);
            case "java.util.Arrays.hashCode(float[])":
                return java.util.Arrays.hashCode((float[]) theArgs[0]);
            case "java.util.Arrays.hashCode(double[])":
                return java.util.Arrays.hashCode((double[]) theArgs[0]);
            case "java.util.Arrays.hashCode(long[])":
                return java.util.Arrays.hashCode((long[]) theArgs[0]);
            case "java.util.Arrays.hashCode(int[])":
                return java.util.Arrays.hashCode((int[]) theArgs[0]);
            case "java.util.Arrays.hashCode(short[])":
                return java.util.Arrays.hashCode((short[]) theArgs[0]);
            case "java.util.Arrays.hashCode(char[])":
                return java.util.Arrays.hashCode((char[]) theArgs[0]);
            case "java.util.Arrays.hashCode(java.lang.Object[])":
                return java.util.Arrays.hashCode((Object[]) theArgs[0]);
            case "java.util.Arrays.asList(java.lang.Object[])":
                return java.util.Arrays.asList(theArgs);

            // Handle java.util.List
            case "java.util.List.add(java.lang.Object)":
                return ((java.util.List) anObj).add(theArgs[0]);
            case "java.util.List.add(int,java.lang.Object)":
                ((java.util.List) anObj).add(intVal(theArgs[0]),theArgs[1]); return null;
            case "java.util.List.remove(java.lang.Object)":
                return ((java.util.List) anObj).remove(theArgs[0]);
            case "java.util.List.remove(int)":
                return ((java.util.List) anObj).remove(intVal(theArgs[0]));
            case "java.util.List.get(int)":
                return ((java.util.List) anObj).get(intVal(theArgs[0]));
            case "java.util.List.equals(java.lang.Object)":
                return ((java.util.List) anObj).equals(theArgs[0]);
            case "java.util.List.hashCode()":
                return ((java.util.List) anObj).hashCode();
            case "java.util.List.indexOf(java.lang.Object)":
                return ((java.util.List) anObj).indexOf(theArgs[0]);
            case "java.util.List.isEmpty()":
                return ((java.util.List) anObj).isEmpty();
            case "java.util.List.lastIndexOf(java.lang.Object)":
                return ((java.util.List) anObj).lastIndexOf(theArgs[0]);
            case "java.util.List.contains(java.lang.Object)":
                return ((java.util.List) anObj).contains(theArgs[0]);
            case "java.util.List.replaceAll(java.util.function.UnaryOperator)":
                ((java.util.List) anObj).replaceAll((java.util.function.UnaryOperator) theArgs[0]); return null;
            case "java.util.List.size()":
                return ((java.util.List) anObj).size();
            case "java.util.List.toArray()":
                return ((java.util.List) anObj).toArray();
            case "java.util.List.toArray(java.lang.Object[])":
                return ((java.util.List) anObj).toArray((Object[]) theArgs[0]);
            case "java.util.List.addAll(java.util.Collection)":
                return ((java.util.List) anObj).addAll((java.util.Collection) theArgs[0]);
            case "java.util.List.addAll(int,java.util.Collection)":
                return ((java.util.List) anObj).addAll(intVal(theArgs[0]),(java.util.Collection) theArgs[1]);
            case "java.util.List.set(int,java.lang.Object)":
                return ((java.util.List) anObj).set(intVal(theArgs[0]),theArgs[1]);
            case "java.util.List.removeAll(java.util.Collection)":
                return ((java.util.List) anObj).removeAll((java.util.Collection) theArgs[0]);

            // Handle java.util.Map
            case "java.util.Map.remove(java.lang.Object)":
                return ((java.util.Map) anObj).remove(theArgs[0]);
            case "java.util.Map.remove(java.lang.Object,java.lang.Object)":
                return ((java.util.Map) anObj).remove(theArgs[0],theArgs[1]);
            case "java.util.Map.get(java.lang.Object)":
                return ((java.util.Map) anObj).get(theArgs[0]);
            case "java.util.Map.put(java.lang.Object,java.lang.Object)":
                return ((java.util.Map) anObj).put(theArgs[0],theArgs[1]);
            case "java.util.Map.equals(java.lang.Object)":
                return ((java.util.Map) anObj).equals(theArgs[0]);
            case "java.util.Map.hashCode()":
                return ((java.util.Map) anObj).hashCode();
            case "java.util.Map.isEmpty()":
                return ((java.util.Map) anObj).isEmpty();
            case "java.util.Map.replace(java.lang.Object,java.lang.Object)":
                return ((java.util.Map) anObj).replace(theArgs[0],theArgs[1]);
            case "java.util.Map.replace(java.lang.Object,java.lang.Object,java.lang.Object)":
                return ((java.util.Map) anObj).replace(theArgs[0],theArgs[1],theArgs[2]);
            case "java.util.Map.size()":
                return ((java.util.Map) anObj).size();

            // Handle java.util.Random
            case "java.util.Random.nextInt(int)":
                return ((java.util.Random) anObj).nextInt(intVal(theArgs[0]));
            case "java.util.Random.nextInt()":
                return ((java.util.Random) anObj).nextInt();
            case "java.util.Random.nextDouble()":
                return ((java.util.Random) anObj).nextDouble();

            // Handle java.util.Set
            case "java.util.Set.add(java.lang.Object)":
                return ((java.util.Set) anObj).add(theArgs[0]);
            case "java.util.Set.remove(java.lang.Object)":
                return ((java.util.Set) anObj).remove(theArgs[0]);
            case "java.util.Set.equals(java.lang.Object)":
                return ((java.util.Set) anObj).equals(theArgs[0]);
            case "java.util.Set.hashCode()":
                return ((java.util.Set) anObj).hashCode();
            case "java.util.Set.isEmpty()":
                return ((java.util.Set) anObj).isEmpty();
            case "java.util.Set.contains(java.lang.Object)":
                return ((java.util.Set) anObj).contains(theArgs[0]);
            case "java.util.Set.size()":
                return ((java.util.Set) anObj).size();
            case "java.util.Set.toArray(java.lang.Object[])":
                return ((java.util.Set) anObj).toArray((Object[]) theArgs[0]);
            case "java.util.Set.toArray()":
                return ((java.util.Set) anObj).toArray();
            case "java.util.Set.addAll(java.util.Collection)":
                return ((java.util.Set) anObj).addAll((java.util.Collection) theArgs[0]);
            case "java.util.Set.removeAll(java.util.Collection)":
                return ((java.util.Set) anObj).removeAll((java.util.Collection) theArgs[0]);

            // Handle java.io.PrintStream
            case "java.io.PrintStream.println(double)":
                ((java.io.PrintStream) anObj).println(doubleVal(theArgs[0])); return null;
            case "java.io.PrintStream.println(float)":
                ((java.io.PrintStream) anObj).println(floatVal(theArgs[0])); return null;
            case "java.io.PrintStream.println(long)":
                ((java.io.PrintStream) anObj).println((long) theArgs[0]); return null;
            case "java.io.PrintStream.println(java.lang.Object)":
                ((java.io.PrintStream) anObj).println(theArgs[0]); return null;
            case "java.io.PrintStream.println(java.lang.String)":
                ((java.io.PrintStream) anObj).println((java.lang.String) theArgs[0]); return null;
            case "java.io.PrintStream.println()":
                ((java.io.PrintStream) anObj).println(); return null;
            case "java.io.PrintStream.println(boolean)":
                ((java.io.PrintStream) anObj).println(boolVal(theArgs[0])); return null;
            case "java.io.PrintStream.println(char)":
                ((java.io.PrintStream) anObj).println((char) theArgs[0]); return null;
            case "java.io.PrintStream.println(int)":
                ((java.io.PrintStream) anObj).println(intVal(theArgs[0])); return null;
            case "java.io.PrintStream.print(long)":
                ((java.io.PrintStream) anObj).print((long) theArgs[0]); return null;
            case "java.io.PrintStream.print(double)":
                ((java.io.PrintStream) anObj).print(doubleVal(theArgs[0])); return null;
            case "java.io.PrintStream.print(java.lang.Object)":
                ((java.io.PrintStream) anObj).print(theArgs[0]); return null;
            case "java.io.PrintStream.print(java.lang.String)":
                ((java.io.PrintStream) anObj).print((java.lang.String) theArgs[0]); return null;
            case "java.io.PrintStream.print(char[])":
                ((java.io.PrintStream) anObj).print((char[]) theArgs[0]); return null;
            case "java.io.PrintStream.print(char)":
                ((java.io.PrintStream) anObj).print((char) theArgs[0]); return null;
            case "java.io.PrintStream.print(int)":
                ((java.io.PrintStream) anObj).print(intVal(theArgs[0])); return null;

            // Handle java.util.stream.Stream
            case "java.util.stream.Stream.min(java.util.Comparator)":
                return ((java.util.stream.Stream) anObj).min((java.util.Comparator) theArgs[0]);
            case "java.util.stream.Stream.max(java.util.Comparator)":
                return ((java.util.stream.Stream) anObj).max((java.util.Comparator) theArgs[0]);
            case "java.util.stream.Stream.concat(java.util.stream.Stream,java.util.stream.Stream)":
                return java.util.stream.Stream.concat((java.util.stream.Stream) theArgs[0],(java.util.stream.Stream) theArgs[1]);
            case "java.util.stream.Stream.toArray(java.util.function.IntFunction)":
                return ((java.util.stream.Stream) anObj).toArray((java.util.function.IntFunction) theArgs[0]);
            case "java.util.stream.Stream.toArray()":
                return ((java.util.stream.Stream) anObj).toArray();
            case "java.util.stream.Stream.of(java.lang.Object[])":
                return java.util.stream.Stream.of(theArgs);
            case "java.util.stream.Stream.of(java.lang.Object)":
                return java.util.stream.Stream.of(theArgs[0]);
            case "java.util.stream.Stream.filter(java.util.function.Predicate)":
                return ((java.util.stream.Stream) anObj).filter((java.util.function.Predicate) theArgs[0]);
            case "java.util.stream.Stream.map(java.util.function.Function)":
                return ((java.util.stream.Stream) anObj).map((java.util.function.Function) theArgs[0]);

            // Handle java.util.stream.DoubleStream
            case "java.util.stream.DoubleStream.min()":
                return ((java.util.stream.DoubleStream) anObj).min();
            case "java.util.stream.DoubleStream.max()":
                return ((java.util.stream.DoubleStream) anObj).max();
            case "java.util.stream.DoubleStream.concat(java.util.stream.DoubleStream,java.util.stream.DoubleStream)":
                return java.util.stream.DoubleStream.concat((java.util.stream.DoubleStream) theArgs[0],(java.util.stream.DoubleStream) theArgs[1]);
            case "java.util.stream.DoubleStream.toArray()":
                return ((java.util.stream.DoubleStream) anObj).toArray();
            case "java.util.stream.DoubleStream.of(double)":
                return java.util.stream.DoubleStream.of(doubleVal(theArgs[0]));
            case "java.util.stream.DoubleStream.filter(java.util.function.DoublePredicate)":
                return ((java.util.stream.DoubleStream) anObj).filter((java.util.function.DoublePredicate) theArgs[0]);
            case "java.util.stream.DoubleStream.map(java.util.function.DoubleUnaryOperator)":
                return ((java.util.stream.DoubleStream) anObj).map((java.util.function.DoubleUnaryOperator) theArgs[0]);

            // Handle java.util.function.DoubleUnaryOperator
            case "java.util.function.DoubleUnaryOperator.applyAsDouble(double)":
                return ((java.util.function.DoubleUnaryOperator) anObj).applyAsDouble(doubleVal(theArgs[0]));

            // Handle java.util.function.DoubleBinaryOperator
            case "java.util.function.DoubleBinaryOperator.applyAsDouble(double,double)":
                return ((java.util.function.DoubleBinaryOperator) anObj).applyAsDouble(doubleVal(theArgs[0]),doubleVal(theArgs[1]));

            // Handle snap.props.PropObject
            case "snap.props.PropObject.getPropValue(java.lang.String)":
                return ((snap.props.PropObject) anObj).getPropValue((java.lang.String) theArgs[0]);
            case "snap.props.PropObject.setPropValue(java.lang.String,java.lang.Object)":
                ((snap.props.PropObject) anObj).setPropValue((java.lang.String) theArgs[0],theArgs[1]); return null;

            // Handle snap.view.View
            case "snap.view.View.getPrefWidth()":
                return ((snap.view.View) anObj).getPrefWidth();
            case "snap.view.View.getPrefWidth(double)":
                return ((snap.view.View) anObj).getPrefWidth(doubleVal(theArgs[0]));
            case "snap.view.View.setPrefWidth(double)":
                ((snap.view.View) anObj).setPrefWidth(doubleVal(theArgs[0])); return null;
            case "snap.view.View.getPrefHeight()":
                return ((snap.view.View) anObj).getPrefHeight();
            case "snap.view.View.getPrefHeight(double)":
                return ((snap.view.View) anObj).getPrefHeight(doubleVal(theArgs[0]));
            case "snap.view.View.setPrefHeight(double)":
                ((snap.view.View) anObj).setPrefHeight(doubleVal(theArgs[0])); return null;
            case "snap.view.View.getPrefSize()":
                return ((snap.view.View) anObj).getPrefSize();
            case "snap.view.View.setPrefSize(snap.geom.Size)":
                ((snap.view.View) anObj).setPrefSize((snap.geom.Size) theArgs[0]); return null;
            case "snap.view.View.setPrefSize(double,double)":
                ((snap.view.View) anObj).setPrefSize(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snap.view.View.getBorder()":
                return ((snap.view.View) anObj).getBorder();
            case "snap.view.View.setBorder(snap.gfx.Color,double)":
                ((snap.view.View) anObj).setBorder((snap.gfx.Color) theArgs[0],doubleVal(theArgs[1])); return null;
            case "snap.view.View.setBorder(snap.gfx.Border)":
                ((snap.view.View) anObj).setBorder((snap.gfx.Border) theArgs[0]); return null;
            case "snap.view.View.getEffect()":
                return ((snap.view.View) anObj).getEffect();
            case "snap.view.View.setEffect(snap.gfx.Effect)":
                ((snap.view.View) anObj).setEffect((snap.gfx.Effect) theArgs[0]); return null;
            case "snap.view.View.getOpacity()":
                return ((snap.view.View) anObj).getOpacity();
            case "snap.view.View.setOpacity(double)":
                ((snap.view.View) anObj).setOpacity(doubleVal(theArgs[0])); return null;
            case "snap.view.View.getMargin()":
                return ((snap.view.View) anObj).getMargin();
            case "snap.view.View.setMargin(snap.geom.Insets)":
                ((snap.view.View) anObj).setMargin((snap.geom.Insets) theArgs[0]); return null;
            case "snap.view.View.setMargin(double,double,double,double)":
                ((snap.view.View) anObj).setMargin(doubleVal(theArgs[0]),doubleVal(theArgs[1]),doubleVal(theArgs[2]),doubleVal(theArgs[3])); return null;
            case "snap.view.View.getPadding()":
                return ((snap.view.View) anObj).getPadding();
            case "snap.view.View.setPadding(double,double,double,double)":
                ((snap.view.View) anObj).setPadding(doubleVal(theArgs[0]),doubleVal(theArgs[1]),doubleVal(theArgs[2]),doubleVal(theArgs[3])); return null;
            case "snap.view.View.setPadding(snap.geom.Insets)":
                ((snap.view.View) anObj).setPadding((snap.geom.Insets) theArgs[0]); return null;
            case "snap.view.View.getSpacing()":
                return ((snap.view.View) anObj).getSpacing();
            case "snap.view.View.setSpacing(double)":
                ((snap.view.View) anObj).setSpacing(doubleVal(theArgs[0])); return null;
            case "snap.view.View.isVisible()":
                return ((snap.view.View) anObj).isVisible();
            case "snap.view.View.setVisible(boolean)":
                ((snap.view.View) anObj).setVisible(boolVal(theArgs[0])); return null;
            case "snap.view.View.setRotate(double)":
                ((snap.view.View) anObj).setRotate(doubleVal(theArgs[0])); return null;
            case "snap.view.View.setScaleX(double)":
                ((snap.view.View) anObj).setScaleX(doubleVal(theArgs[0])); return null;
            case "snap.view.View.setScaleY(double)":
                ((snap.view.View) anObj).setScaleY(doubleVal(theArgs[0])); return null;
            case "snap.view.View.setTransX(double)":
                ((snap.view.View) anObj).setTransX(doubleVal(theArgs[0])); return null;
            case "snap.view.View.setTransY(double)":
                ((snap.view.View) anObj).setTransY(doubleVal(theArgs[0])); return null;
            case "snap.view.View.getFill()":
                return ((snap.view.View) anObj).getFill();
            case "snap.view.View.setFill(snap.gfx.Paint)":
                ((snap.view.View) anObj).setFill((snap.gfx.Paint) theArgs[0]); return null;
            case "snap.view.View.setScale(double)":
                ((snap.view.View) anObj).setScale(doubleVal(theArgs[0])); return null;
            case "snap.view.View.getAnim(int)":
                return ((snap.view.View) anObj).getAnim(intVal(theArgs[0]));
            case "snap.view.View.setText(java.lang.String)":
                ((snap.view.View) anObj).setText((java.lang.String) theArgs[0]); return null;
            case "snap.view.View.getName()":
                return ((snap.view.View) anObj).getName();
            case "snap.view.View.contains(double,double)":
                return ((snap.view.View) anObj).contains(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snap.view.View.contains(snap.geom.Shape)":
                return ((snap.view.View) anObj).contains((snap.geom.Shape) theArgs[0]);
            case "snap.view.View.contains(snap.geom.Point)":
                return ((snap.view.View) anObj).contains((snap.geom.Point) theArgs[0]);

            // Handle snap.view.ChildView
            case "snap.view.ChildView.removeChild(int)":
                return ((snap.view.ChildView) anObj).removeChild(intVal(theArgs[0]));
            case "snap.view.ChildView.removeChild(snap.view.View)":
                return ((snap.view.ChildView) anObj).removeChild((snap.view.View) theArgs[0]);
            case "snap.view.ChildView.setChildren(snap.view.View[])":
                ((snap.view.ChildView) anObj).setChildren((snap.view.View[]) theArgs); return null;
            case "snap.view.ChildView.removeChildren()":
                ((snap.view.ChildView) anObj).removeChildren(); return null;
            case "snap.view.ChildView.addChild(snap.view.View,int)":
                ((snap.view.ChildView) anObj).addChild((snap.view.View) theArgs[0],intVal(theArgs[1])); return null;
            case "snap.view.ChildView.addChild(snap.view.View)":
                ((snap.view.ChildView) anObj).addChild((snap.view.View) theArgs[0]); return null;

            // Handle snap.view.Button

            // Handle snap.view.Label

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim.setPrefWidth(double)":
                return ((snap.view.ViewAnim) anObj).setPrefWidth(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setPrefHeight(double)":
                return ((snap.view.ViewAnim) anObj).setPrefHeight(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setPrefSize(double,double)":
                return ((snap.view.ViewAnim) anObj).setPrefSize(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snap.view.ViewAnim.setOpacity(double)":
                return ((snap.view.ViewAnim) anObj).setOpacity(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setRotate(double)":
                return ((snap.view.ViewAnim) anObj).setRotate(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setScaleX(double)":
                return ((snap.view.ViewAnim) anObj).setScaleX(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setScaleY(double)":
                return ((snap.view.ViewAnim) anObj).setScaleY(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setTransX(double)":
                return ((snap.view.ViewAnim) anObj).setTransX(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setTransY(double)":
                return ((snap.view.ViewAnim) anObj).setTransY(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.setLoopCount(int)":
                return ((snap.view.ViewAnim) anObj).setLoopCount(intVal(theArgs[0]));
            case "snap.view.ViewAnim.setOnFinish(java.lang.Runnable)":
                return ((snap.view.ViewAnim) anObj).setOnFinish((java.lang.Runnable) theArgs[0]);
            case "snap.view.ViewAnim.setOnFinish(java.util.function.Consumer)":
                return ((snap.view.ViewAnim) anObj).setOnFinish((java.util.function.Consumer) theArgs[0]);
            case "snap.view.ViewAnim.setFill(snap.gfx.Paint)":
                return ((snap.view.ViewAnim) anObj).setFill((snap.gfx.Paint) theArgs[0]);
            case "snap.view.ViewAnim.setScale(double)":
                return ((snap.view.ViewAnim) anObj).setScale(doubleVal(theArgs[0]));
            case "snap.view.ViewAnim.getAnim(int)":
                return ((snap.view.ViewAnim) anObj).getAnim(intVal(theArgs[0]));
            case "snap.view.ViewAnim.play()":
                ((snap.view.ViewAnim) anObj).play(); return null;
            case "snap.view.ViewAnim.isEmpty()":
                return ((snap.view.ViewAnim) anObj).isEmpty();

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner.setWindowVisible(boolean)":
                ((snap.view.ViewOwner) anObj).setWindowVisible(boolVal(theArgs[0])); return null;

            // Handle anything else
            default:
                if (_next != null) return _next.invokeMethod(anId, anObj, theArgs);
                throw new NoSuchMethodException("Unknown method: " + anId);
        }
    }

    /**
     * Returns the declared constructors for given class.
     */
    public JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)
    {
        cb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.String
            case "java.lang.String":
                cb.paramTypes(byte[].class,int.class,int.class).save();
                cb.paramTypes(byte[].class,java.nio.charset.Charset.class).save();
                cb.paramTypes(byte[].class,java.lang.String.class).save();
                cb.paramTypes(byte[].class,int.class,int.class,java.nio.charset.Charset.class).save();
                cb.paramTypes(byte[].class,int.class,int.class,java.lang.String.class).save();
                cb.paramTypes(java.lang.StringBuilder.class).save();
                cb.paramTypes(byte[].class).save();
                cb.paramTypes(int[].class,int.class,int.class).save();
                cb.paramTypes(char[].class).save();
                return cb.paramTypes(char[].class,int.class,int.class).buildAll();

            // Handle java.lang.StringBuffer
            case "java.lang.StringBuffer":
                cb.paramTypes(java.lang.CharSequence.class).save();
                cb.paramTypes(java.lang.String.class).save();
                return cb.paramTypes(int.class).buildAll();

            // Handle java.lang.StringBuilder
            case "java.lang.StringBuilder":
                cb.paramTypes(java.lang.CharSequence.class).save();
                cb.paramTypes(java.lang.String.class).save();
                return cb.paramTypes(int.class).buildAll();

            // Handle java.util.Random
            case "java.util.Random":
                return cb.paramTypes(long.class).buildAll();

            // Handle java.io.PrintStream
            case "java.io.PrintStream":
                cb.paramTypes(java.io.OutputStream.class).save();
                cb.paramTypes(java.io.OutputStream.class,boolean.class).save();
                return cb.paramTypes(java.io.OutputStream.class,boolean.class,java.lang.String.class).buildAll();

            // Handle snap.view.Button
            case "snap.view.Button":
                return cb.paramTypes(java.lang.String.class).buildAll();

            // Handle snap.view.Label
            case "snap.view.Label":
                cb.paramTypes(java.lang.String.class).save();
                return cb.paramTypes(snap.view.View.class,java.lang.String.class,snap.view.View.class).buildAll();

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim":
                return cb.paramTypes(snap.view.View.class).buildAll();

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner":
                return cb.paramTypes(snap.view.View.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getConstructorsForClass(aResolver, aClassName);
                return cb.save().buildAll();
        }
    }

    /**
     * Invokes constructors for given constructor id and args.
     */
    public Object invokeConstructor(String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle java.lang.String
            case "java.lang.String(byte[],int,int)":
                return new java.lang.String((byte[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.String(byte[],java.nio.charset.Charset)":
                return new java.lang.String((byte[]) theArgs[0],(java.nio.charset.Charset) theArgs[1]);
            case "java.lang.String(byte[],java.lang.String)":
                return new java.lang.String((byte[]) theArgs[0],(java.lang.String) theArgs[1]);
            case "java.lang.String(byte[],int,int,java.nio.charset.Charset)":
                return new java.lang.String((byte[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]),(java.nio.charset.Charset) theArgs[3]);
            case "java.lang.String(byte[],int,int,java.lang.String)":
                return new java.lang.String((byte[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]),(java.lang.String) theArgs[3]);
            case "java.lang.String(java.lang.StringBuilder)":
                return new java.lang.String((java.lang.StringBuilder) theArgs[0]);
            case "java.lang.String(byte[])":
                return new java.lang.String((byte[]) theArgs[0]);
            case "java.lang.String(int[],int,int)":
                return new java.lang.String((int[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));
            case "java.lang.String(char[])":
                return new java.lang.String((char[]) theArgs[0]);
            case "java.lang.String(char[],int,int)":
                return new java.lang.String((char[]) theArgs[0],intVal(theArgs[1]),intVal(theArgs[2]));

            // Handle java.lang.StringBuffer
            case "java.lang.StringBuffer(java.lang.CharSequence)":
                return new java.lang.StringBuffer((java.lang.CharSequence) theArgs[0]);
            case "java.lang.StringBuffer(java.lang.String)":
                return new java.lang.StringBuffer((java.lang.String) theArgs[0]);
            case "java.lang.StringBuffer(int)":
                return new java.lang.StringBuffer(intVal(theArgs[0]));

            // Handle java.lang.StringBuilder
            case "java.lang.StringBuilder(java.lang.CharSequence)":
                return new java.lang.StringBuilder((java.lang.CharSequence) theArgs[0]);
            case "java.lang.StringBuilder(java.lang.String)":
                return new java.lang.StringBuilder((java.lang.String) theArgs[0]);
            case "java.lang.StringBuilder(int)":
                return new java.lang.StringBuilder(intVal(theArgs[0]));

            // Handle java.util.Random
            case "java.util.Random(long)":
                return new java.util.Random((long) theArgs[0]);

            // Handle java.io.PrintStream
            case "java.io.PrintStream(java.io.OutputStream)":
                return new java.io.PrintStream((java.io.OutputStream) theArgs[0]);
            case "java.io.PrintStream(java.io.OutputStream,boolean)":
                return new java.io.PrintStream((java.io.OutputStream) theArgs[0],boolVal(theArgs[1]));
            case "java.io.PrintStream(java.io.OutputStream,boolean,java.lang.String)":
                return new java.io.PrintStream((java.io.OutputStream) theArgs[0],boolVal(theArgs[1]),(java.lang.String) theArgs[2]);

            // Handle snap.view.Button
            case "snap.view.Button(java.lang.String)":
                return new snap.view.Button((java.lang.String) theArgs[0]);

            // Handle snap.view.Label
            case "snap.view.Label(java.lang.String)":
                return new snap.view.Label((java.lang.String) theArgs[0]);
            case "snap.view.Label(snap.view.View,java.lang.String,snap.view.View)":
                return new snap.view.Label((snap.view.View) theArgs[0],(java.lang.String) theArgs[1],(snap.view.View) theArgs[2]);

            // Handle snap.view.ViewAnim
            case "snap.view.ViewAnim(snap.view.View)":
                return new snap.view.ViewAnim((snap.view.View) theArgs[0]);

            // Handle snap.view.ViewOwner
            case "snap.view.ViewOwner(snap.view.View)":
                return new snap.view.ViewOwner((snap.view.View) theArgs[0]);

            // Handle anything else
            default:
                if (_next != null) return _next.invokeConstructor(anId, theArgs);
                throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }

    // Conveniences
    protected static boolean boolVal(Object anObj)  { return Convert.boolValue(anObj); }
    protected static int intVal(Object anObj)  { return Convert.intValue(anObj); }
    protected static double doubleVal(Object anObj)  { return Convert.doubleValue(anObj); }
    protected static float floatVal(Object anObj)  { return Convert.floatValue(anObj); }
}

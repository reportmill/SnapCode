package javakit.resolver;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class generates StaticResolver.java for TeaVM.
 */
public class StaticResolverGen {

    // Whether this is root generator
    private boolean  _isRoot = getClass() == StaticResolverGen.class;

    // Package
    protected static String _package = "javakit.resolver";

    // StringBuffer
    protected static StringBuffer _sb = new StringBuffer();

    // A resolver
    private static Resolver  _resolver = new ResolverSys(snap.view.View.class.getClassLoader());

    // A set of white list method names
    private static Set<String>  _whiteList;

    // A set of black list member ids
    private static Set<String>  _blackList;

    /**
     * Prints the preamble.
     */
    public void printPreamble()
    {
        // Append imports
        append("package ").append(_package).appendln(";");
        if (!_isRoot)
            appendln("import javakit.resolver.*;");
        if (_isRoot) {
            appendln("import javakit.resolver.JavaField.FieldBuilder;");
            appendln("import javakit.resolver.JavaMethod.MethodBuilder;");
            appendln("import javakit.resolver.JavaConstructor.ConstructorBuilder;");
            appendln("import snap.util.Convert;");
        }
        appendln("import java.io.PrintStream;");

        // Append class header
        appendln("");
        appendln("/**");
        appendln(" * Provide reflection info for TeaVM.");
        appendln(" */");
        if (_isRoot) {
            appendln("public class StaticResolver {");
            appendln("");
            appendln("    // Shared field, method, constructor builders");
            appendln("    protected static FieldBuilder fb = new FieldBuilder();");
            appendln("    protected static MethodBuilder mb = new MethodBuilder();");
            appendln("    protected static ConstructorBuilder cb = new ConstructorBuilder();");
            appendln("");
            appendln("    // A chained StaticResolver");
            appendln("    public StaticResolver  _next;");
            appendln("");
            appendln("    // The shared StaticResolver");
            appendln("    private static StaticResolver  _shared = new StaticResolver();");
            appendln("");
            appendln("    /**");
            appendln("     * Returns shared.");
            appendln("     */");
            appendln("    public static StaticResolver shared()  { return _shared; }");
            appendln("");
        }
        else {
            append("public class StaticResolver ");
            append("extends ").append(StaticResolver.class.getName()).appendln(" {");
            appendln("");
        }
    }

    /**
     * Prints the getFieldsForClass().
     */
    public void printGetFieldsForClass()
    {
        // Append method header
        appendln("    /**");
        appendln("     * Returns the declared fields for given class.");
        appendln("     */");
        appendln("    public JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)");
        appendln("    {");
        appendln("        fb.init(aResolver, aClassName);");
        appendln("");
        appendln("        switch (aClassName) {");
        appendln("");

        // Append bogus
        appendln("            // Handle java.lang.System");
        appendln("            case \"java.lang.System\":");
        appendln("                fb.name(\"out\").type(PrintStream.class).save();");
        appendln("                return fb.name(\"err\").type(PrintStream.class).buildAll();");

        // Append method trailer
        appendln("");
        appendln("            // Handle anything else");
        appendln("            default:");
        appendln("                if (_next != null) return _next.getFieldsForClass(aResolver, aClassName);");
        appendln("                return new JavaField[0];");
        appendln("        }");
        appendln("    }");
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForClasses(Class<?>[] theClasses)
    {
        // Append method header
        appendln("");
        appendln("/**");
        appendln(" * Returns the declared methods for given class.");
        appendln(" */");
        appendln("public JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)");
        appendln("{");
        appendln("    mb.init(aResolver, aClassName);");
        appendln("");
        appendln("    switch (aClassName) {");

        // Iterate over classes and print methods for each
        for (Class<?> cls : theClasses)
            printGetMethodsForClassForClass(cls);

        // Append method trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default:");
        appendln("            if (_next != null) return _next.getMethodsForClass(aResolver, aClassName);");
        appendln("            return new JavaMethod[0];");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForClass(Class aClass)
    {
        // Get methods
        Method[] methods = aClass.getDeclaredMethods();
        Stream<Method> methodsStream = Stream.of(methods);
        methodsStream = methodsStream.filter(m -> isValidMethod(m));
        methods = methodsStream.toArray(size -> new Method[size]);
        if (methods.length == 0)
            return;

        // Append case statement for class
        String className = aClass.getName();
        appendln("");
        append("        // Handle ").appendln(className);
        appendln("        case \"" + className + "\":");

        // Iterate over methods and print builder line for each
        for (int i = 0, iMax = methods.length; i < iMax; i++) {
            Method method = methods[i];
            printGetMethodsForClassForMethod(method, i + 1 == iMax);
        }
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetMethodsForClassForMethod(Method aMethod, boolean isLast)
    {
        // Append indent (and 'return ' if last)
        append("            ");
        if (isLast)
            append("return ");

        // Append method name
        String methodName = aMethod.getName();
        append("mb.name(\"" + methodName + "\")");

        // Append parameters
        Class<?>[] paramTypes = aMethod.getParameterTypes();
        if (paramTypes.length > 0) {
            append(".paramTypes(");
            for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
                String className = className(paramTypes[i]);
                append(className).append(".class");
                if (i + 1 < iMax) append(",");
            }
            append(")");
        }

        // Append return type
        Class<?> returnType = aMethod.getReturnType();
        if (returnType != null) {
            String className = className(returnType);
            append(".returnType(").append(className).append(".class").append(")");
        }

        // Append varArgs
        if (aMethod.isVarArgs())
            append(".varArgs()");

        // Append Save() or BuildAll()
        if (isLast)
            appendln(".buildAll();");
        else appendln(".save();");
    }

    /**
     * Prints getConstructorsForClass() method.
     */
    public void printGetConstructorsForClassForClasses(Class<?>[] theClasses)
    {
        // Append method header
        appendln("");
        appendln("/**");
        appendln(" * Returns the declared constructors for given class.");
        appendln(" */");
        appendln("public JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)");
        appendln("{");
        appendln("    cb.init(aResolver, aClassName);");
        appendln("");
        appendln("    switch (aClassName) {");

        // Iterate over classes and generate constructors for each
        for (Class<?> cls : theClasses)
            printGetConstructorsForClassForClass(cls);

        // Append method trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default:");
        appendln("            if (_next != null) return _next.getConstructorsForClass(aResolver, aClassName);");
        appendln("            return cb.save().buildAll();");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints getConstructorsForClass() method.
     */
    public void printGetConstructorsForClassForClass(Class aClass)
    {
        // Get valid constructors (just return if none)
        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaConstructor[] constructors = javaClass.getConstructors().toArray(new JavaConstructor[0]);
        Stream<JavaConstructor> constrStream = Stream.of(constructors);
        constrStream = constrStream.filter(c -> isValidConstructor(c));
        constructors = constrStream.toArray(size -> new JavaConstructor[size]);
        if (constructors.length == 0)
            return;

        // Append case statement for class name
        String className = aClass.getName();
        appendln("");
        append("        // Handle ").appendln(className);
        appendln("        case \"" + className + "\":");

        // Iterate over constructors and print constructor create line for each
        for (int i = 0, iMax = constructors.length; i < iMax; i++) {
            Constructor constructor = constructors[i].getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) continue;
            printGetConstructorsForClassForConstructor(constructor, i + 1 == iMax);
        }
    }

    /**
     * Prints getMethodsForClass() method.
     */
    public void printGetConstructorsForClassForConstructor(Constructor aConstructor, boolean isLast)
    {
        // Append prefix
        append("            ");
        if (isLast)
            append("return ");
        append("cb");

        // Iterate over parameters and append each
        Class<?>[] paramTypes = aConstructor.getParameterTypes();
        if (paramTypes.length > 0) {
            append(".paramTypes(");
            for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
                String className = className(paramTypes[i]);
                append(className).append(".class");
                if (i + 1 < iMax) append(",");
            }
            append(")");
        }

        // Append Save() or BuildAll()
        if (isLast)
            appendln(".buildAll();");
        else appendln(".save();");
    }

    /**
     * Prints invokeMethod() method.
     */
    public void printInvokeMethodForClasses(Class<?>[] theClasses)
    {
        // Append method header
        appendln("");
        appendln("/**");
        appendln(" * Invokes methods for given method id, object and args.");
        appendln(" */");
        appendln("public Object invokeMethod(String anId, Object anObj, Object ... theArgs) throws Exception");
        appendln("{");
        appendln("    switch (anId) {");

        // Iterate over classes and append case statement for each
        for (Class<?> cls : theClasses)
            printInvokeMethodForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default:");
        appendln("            if (_next != null) return _next.invokeMethod(anId, anObj, theArgs);");
        appendln("            throw new NoSuchMethodException(\"Unknown method: \" + anId);");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints invokeMethod() method.
     */
    public void printInvokeMethodForClass(Class aClass)
    {
        // Get methods
        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaMethod[] methods = javaClass.getMethods().toArray(new JavaMethod[0]);

        // Append comment for class
        String className = className(aClass);
        appendln("");
        append("        // Handle ").appendln(className);

        // Iterate over methods
        for (JavaMethod method : methods) {

            // If method is not valid, skip
            Method realMethod = method.getMethod();
            if (!isValidMethod(realMethod))
                continue;

            // If super method is available, skip
            JavaMethod methSuper = method.getSuper();
            if (methSuper != null && methSuper.isPublic() && Modifier.isPublic(methSuper.getDeclaringClass().getModifiers()))
                continue;

            // Print method
            printInvokeMethodForClassMethod(method);
        }
    }

    /**
     * Prints invokeMethod() for method.
     */
    public void printInvokeMethodForClassMethod(JavaMethod aMethod)
    {
        // Get method and return type
        Method meth = aMethod.getMethod();
        Class<?> returnType = meth.getReturnType();

        // Append case statement
        appendln("        case \"" + aMethod.getId() + "\":");

        // Append indent and return
        append("            ");
        if (returnType != void.class)
            append("return ");

        // If static just append "ClassName."
        String castClassName = meth.getDeclaringClass().getName();
        if (Modifier.isStatic(meth.getModifiers()))
            append(castClassName);

        // Else if instance method, add cast: "((pkg.pkg.ClassName) anObj).name("
        else append("((").append(castClassName).append(") anObj)");

        // Append .name(
        append(".").append(meth.getName()).append("(");

        // Get parameterTypes
        Class<?>[] paramTypes = meth.getParameterTypes();

        // Handle VarArgs
        if (aMethod.isVarArgs()) {
            if (paramTypes[0] != Object[].class) {
                String className = className(paramTypes[0]);
                append("(").append(className).append(") ");
            }
            append("theArgs");
        }

        // Otherwise, iterate over parameters
        else {
            for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
                appendParamType(paramTypes[i], i);
                if (i + 1 < iMax) append(",");
            }
        }

        // Append Save()/BuildAll()
        if (returnType == void.class)
            appendln("); return null;");
        else appendln(");");
    }

    /**
     * Prints invokeConstructor() method.
     */
    public void printInvokeConstructorForClasses(Class<?>[] theClasses)
    {
        // Append method header
        appendln("");
        appendln("/**");
        appendln(" * Invokes constructors for given constructor id and args.");
        appendln(" */");
        appendln("public Object invokeConstructor(String anId, Object ... theArgs) throws Exception");
        appendln("{");
        appendln("    switch (anId) {");

        // Iterate over classes and print invoke constructors for each
        for (Class<?> cls : theClasses)
            printInvokeConstructorForClass(cls);

        // Append trailer
        appendln("");
        appendln("        // Handle anything else");
        appendln("        default:");
        appendln("            if (_next != null) return _next.invokeConstructor(anId, theArgs);");
        appendln("            throw new NoSuchMethodException(\"Unknown constructor: \" + anId);");
        appendln("    }");
        appendln("}");
    }

    /**
     * Prints invokeConstructor() method.
     */
    public void printInvokeConstructorForClass(Class aClass)
    {
        // Get constructors for class
        JavaClass javaClass = _resolver.getJavaClassForClass(aClass);
        JavaConstructor[] constructors = javaClass.getConstructors().toArray(new JavaConstructor[0]);
        Stream<JavaConstructor> constrStream = Stream.of(constructors);
        constrStream = constrStream.filter(c -> isValidConstructor(c));
        constructors = constrStream.toArray(size -> new JavaConstructor[size]);
        if (constructors.length == 0)
            return;

        // Append comment
        String className = className(aClass);
        appendln("");
        append("        // Handle ").appendln(className);

        // Iterate over constructors and print invoke constructor for each
        for (JavaConstructor constructor : constructors)
            printInvokeConstructorForConstructor(constructor);
    }

    /**
     * Prints invokeConstructor() for method.
     */
    public void printInvokeConstructorForConstructor(JavaConstructor aConstructor)
    {
        appendln("        case \"" + aConstructor.getId() + "\":");

        Constructor constructor = aConstructor.getConstructor();

        // Preface
        append("            return new ");

        // If static just do ClassName.
        String castClassName = constructor.getDeclaringClass().getName();
        append(castClassName).append("(");

        // Parameters
        Class<?>[] paramTypes = constructor.getParameterTypes();
        for (int i = 0, iMax = paramTypes.length; i < iMax; i++) {
            appendParamType(paramTypes[i], i);
            if (i + 1 < iMax) append(",");
        }

        // Append close
        appendln(");");
    }

    /**
     * Appends a method/constructor parameter type.
     */
    private void appendParamType(Class<?> aClass, int anIndex)
    {
        if (aClass == double.class)
            append("doubleVal(theArgs[").append(anIndex).append("])");
        else if (aClass == int.class)
            append("intVal(theArgs[").append(anIndex).append("])");
        else if (aClass == float.class)
            append("floatVal(theArgs[").append(anIndex).append("])");
        else if (aClass == boolean.class)
            append("boolVal(theArgs[").append(anIndex).append("])");
        else if (aClass == Object.class)
            append("theArgs[").append(anIndex).append("]");
        else if (aClass == Object[].class)
            append("(").append("Object[]").append(") ").append("theArgs[").append(anIndex).append("]");
        else {
            String className = className(aClass);
            append("(").append(className).append(") ").append("theArgs[").append(anIndex).append("]");
        }
    }

    /**
     * Returns whether method should be included.
     */
    private boolean isValidMethod(Method m)
    {
        // If not public, return false
        if (!Modifier.isPublic(m.getModifiers()))
            return false;

        // If return type not public, return false
        Class<?> returnType = m.getReturnType();
        if (returnType != null && !Modifier.isPublic(returnType.getModifiers()))
            return false;

        // If not in WhiteList, return false
        if (!_whiteList.contains(m.getName()))
            return false;

        // If method in blacklist, return false
        JavaClass javaClass = _resolver.getJavaClassForClass(m.getDeclaringClass());
        if (_blackList.contains(new JavaMethod(_resolver, javaClass, m).getId()))
            return false;

        // Return true
        return true;
    }

    /**
     * Returns whether constructor should be included.
     */
    private boolean isValidConstructor(JavaConstructor c)
    {
        if (!c.isPublic()) return false;
        if (c.getParamTypes().length == 0) return false;
        if (_blackList.contains(c.getId())) return false;
        return true;
    }

    // Append method
    StaticResolverGen append(int aVal) { _sb.append(aVal); return this;}
    StaticResolverGen append(String aStr) { _sb.append(aStr); return this;}
    StaticResolverGen appendln(String aStr) { _sb.append(aStr).append('\n'); return this;}
    String className(Class<?> aClass) {
        if (aClass.isPrimitive() || aClass.isArray())
            return _resolver.getJavaClassForClass(aClass).getClassName();
        return aClass.getName();
    }

    /**
     * Prints the postamble.
     */
    public void printPostamble()
    {
        if (_isRoot) {
            appendln("");
            appendln("    // Conveniences");
            appendln("    protected static boolean boolVal(Object anObj)  { return Convert.boolValue(anObj); }");
            appendln("    protected static int intVal(Object anObj)  { return Convert.intValue(anObj); }");
            appendln("    protected static double doubleVal(Object anObj)  { return Convert.doubleValue(anObj); }");
            appendln("    protected static float floatVal(Object anObj)  { return Convert.floatValue(anObj); }");
        }
        appendln("}");
    }

    /**
     * Prints packages for class.
     */
    public static void printClassesForPackage(String packageName)
    {
        // Get JRE site
        WebURL jreURL = WebURL.getURL(List.class);
        WebSite jreSite = jreURL.getSite();

        // Get class files
        WebFile pkgDir = jreSite.getFileForPath(packageName);
        WebFile[] files = pkgDir.getFiles();
        Stream<WebFile> filesStream = Stream.of(files);
        Stream<WebFile> classFilesStream = filesStream.filter(f -> f.getType().equals("class") && f.getName().indexOf('$') <0);
        WebFile[] classFiles = classFilesStream.toArray(size -> new WebFile[size]);

        // Print
        for (int i = 0; i < classFiles.length; i++) {
            WebFile classFile = classFiles[i];
            System.out.println(classFile.getPath().substring(1).replace("/", ".") + ",");
        }
    }

    /**
     * Generate StaticResolver for classes.
     */
    public void generateStaticResolverForClasses(Class<?>[] theClasses, String[] whiteList, String[] blackList)
    {
        // Set WhiteList, BlackList
        _whiteList = new HashSet<>(Arrays.asList(whiteList));
        _blackList = new HashSet<>(Arrays.asList(blackList));

        // Generate
        printPreamble();
        printGetFieldsForClass();
        printGetMethodsForClassForClasses(theClasses);
        printInvokeMethodForClasses(theClasses);
        printGetConstructorsForClassForClasses(theClasses);
        printInvokeConstructorForClasses(theClasses);
        printPostamble();
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {

        StaticResolverGen codeGen = new StaticResolverGen();
        codeGen.generateStaticResolverForClasses(_javaUtilClasses, _whiteListStrings, _blackListStrings);

        WebFile webFile = WebURL.getURL("/tmp/StaticResolver.java").createFile(false);
        webFile.setText(_sb.toString());
        webFile.save();
    }

    // Packages
    private static Class[]  _javaUtilClasses = {

            java.lang.Object.class,
            Objects.class,
            java.lang.Class.class,
            java.lang.String.class,
            java.lang.Number.class,
            //java.lang.StringBuffer.class,
            //java.lang.StringBuilder.class,
            java.lang.System.class,
            java.lang.Math.class,

            StringBuffer.class,
            StringBuilder.class,

            java.util.Arrays.class,
            //java.util.BitSet.class,
            //java.util.Collection.class,
            //java.util.Collections.class,
            //java.util.Comparator.class,
            //java.util.Date.class,
            //java.util.EventListener.class,
            //java.util.EventObject.class,
            java.util.List.class,
            java.util.Map.class,
            //java.util.Objects.class,
            java.util.Random.class,
            java.util.Set.class,

            java.io.PrintStream.class,

            java.util.stream.Stream.class,
            java.util.stream.DoubleStream.class,

            java.util.function.DoubleUnaryOperator.class,
            java.util.function.DoubleBinaryOperator.class,

            snap.props.PropObject.class,
            snap.view.View.class,
            snap.view.ChildView.class,
            snap.view.Button.class,
            snap.view.Label.class,

            snap.view.ViewAnim.class,
            snap.view.ViewOwner.class,

            // SnapCharts
            snapcharts.data.DoubleArray.class,
            snapcharts.data.DataArray.class,
            snapcharts.data.DataSet.class,
            snapcharts.repl.ReplObject.class,
            snapcharts.repl.Quick3D.class,
            snapcharts.repl.QuickCharts.class,
            snapcharts.repl.QuickData.class,
            snapcharts.repl.QuickDraw.class,
            snapcharts.repl.QuickDrawPen.class
    };

    // WhiteList
    protected static String[] _whiteListStrings = {

            // Object
            "clone", "equals", "getClass", "hashCode", "toString",

            // Class
            "getName", "getSimpleName", "getSuperclass", "getInterfaces",

            // String
            "charAt", "compareTo", "compareToIgnoreCase", "concat", "contains", "endsWith", "equals", "equalsIgnoreCase",
            "format", "getBytes", "indexOf", "isEmpty", "join", "lastIndexOf", "length", "matches", "replace",
            "replaceAll", "split", "startsWith", "substring", "toLowerCase", "toCharArray", "toUpperCase", "trim",
            "valueOf",

            // Number
            "byteValue", "doubleValue", "floatValue", "intValue", "longValue", "shortValue",

            // System
            "arraycopy", "currentTimeMillis", "getProperties", "getProperty", "identityHashCode", "nanoTime",

            // Math
            "abs", "acos", "atan", "atan2", "cbrt", "ceil", "copySign", "cos", "cosh", "exp", "floor", "floorDiv",
            "floorMod", "hypot", "log", "log10", "max", "min", "pow", "random", "round", "sin", "sinh", "sqrt", "tan",
            "tanh", "toDegrees", "toRadians",

            // Arrays
            "asList",

            // StringBuilder, StringBuffer
            "append", "replace", "delete",

            // List
            "get", "set", "size", "add", "remove", "addAll", "removeAll",

            // Map
            "put",

            // Random
            "nextInt", "nextDouble",

            // Set

            // PrintStream
            "print", "println",

            // Stream, DoubleStream
            "of", "map", "filter", "toArray",

            // DoubleUnaryOperator, DoubleBinaryOperator
            "applyAsDouble",

            // PropObject
            "getPropValue", "setPropValue",

            // Button
            "setTitle",

            // View
            "getPrefWidth", "setPrefWidth", "getPrefHeight", "setPrefHeight", "getPrefSize", "setPrefSize",
            "getBorder", "setBorder", "getFill", "setFill", "getEffect", "setEffect", "getOpacity", "setOpacity",
            "getMargin", "setMargin", "getPadding", "setPadding", "getSpacing", "setSpacing",
            "isVisible", "setVisible",
            "setRotate", "setScaleX", "setScaleY", "setScale", "setTransX", "setTransY", "getAnim",
            "setText",

            // ChildView
            "getChild", "addChild", "removeChild", "setChildren", "removeChildren",

            // ViewAnim
            "play", "setLoopCount", "setOnFinish",

            // ViewOwner
            "setWindowVisible",

            // SnapCharts
            // DoubleArray
            "length", "map", "filter", "doubleArray", "toArray", "of", "fromMinMax", "fromMinMaxCount",

            // ReplObject
            "print", "println", "show",

            // Quick3D
            "createCube", "createImage3D",

            // QuickCharts
            "chart", "chart3D",

            // QuickData
            "doubleArray", "dataArray", "dataSet", "minMaxArray", "mapXY",
            "getTextForSource", "getImageForSource",

            // QuickDraw, QuickDrawPen
            "createDrawView", "isShowGrid", "setShowGrid", "getGridSpacing", "setGridSpacing", "isAnimate", "setAnimate",
            "getPen", "setPenColor",
            "getColor", "setColor", "getWidth", "setWidth", "getDirection", "setDirection",
            "moveTo", "lineTo", "closePath", "forward", "turn", "getAnimPen",
    };
    private static String[] _blackListStrings = {

            // String
            "java.lang.String(java.lang.String)",
            "java.lang.String(byte[],int,int,int)",
            "java.lang.String(java.lang.StringBuffer)",
            "java.lang.String(byte[],int)",
            "java.lang.String.getBytes(int,int,byte[],int)",
            "java.lang.String.join(java.lang.CharSequence,java.lang.CharSequence[])",
            "java.lang.String.format(java.util.Locale,java.lang.String,java.lang.Object[])",
            "java.lang.String.format(java.lang.String,java.lang.Object[])",

            // StringBuffer, StringBuilder
            "java.lang.StringBuffer.compareTo(java.lang.StringBuffer)",
            "java.lang.StringBuilder.compareTo(java.lang.StringBuilder)",

            // Arrays
            "java.util.Arrays.equals(boolean[],int,int,boolean[],int,int)",
            "java.util.Arrays.equals(byte[],int,int,byte[],int,int)",
            "java.util.Arrays.equals(char[],int,int,char[],int,int)",
            "java.util.Arrays.equals(short[],int,int,short[],int,int)",
            "java.util.Arrays.equals(int[],int,int,int[],int,int)",
            "java.util.Arrays.equals(long[],int,int,long[],int,int)",
            "java.util.Arrays.equals(float[],int,int,float[],int,int)",
            "java.util.Arrays.equals(double[],int,int,double[],int,int)",
            "java.util.Arrays.equals(java.lang.Object[],int,int,java.lang.Object[],int,int)",
            "java.util.Arrays.equals(java.lang.Object[],java.lang.Object[],java.util.Comparator)",
            "java.util.Arrays.equals(java.lang.Object[],int,int,java.lang.Object[],int,int,java.util.Comparator)",

            // Map
            "java.util.Map.replaceAll(java.util.function.BiFunction)",

            // PrintStream
            "java.io.PrintStream.println(char[])",
            "java.io.PrintStream.format(java.util.Locale,java.lang.String,java.lang.Object[])",
            "java.io.PrintStream.format(java.lang.String,java.lang.Object[])",
            "java.io.PrintStream.print(boolean)",
            "java.io.PrintStream.print(float)",
            "java.io.PrintStream(java.lang.String)",
            "java.io.PrintStream(java.lang.String,java.lang.String)",
            "java.io.PrintStream(java.lang.String,java.nio.charset.Charset)",
            "java.io.PrintStream(java.io.File)",
            "java.io.PrintStream(java.io.File,java.lang.String)",
            "java.io.PrintStream(java.io.File,java.nio.charset.Charset)",
            "java.io.PrintStream(java.io.OutputStream,java.nio.charset.Charset)",
            "java.io.PrintStream(java.io.OutputStream,boolean,java.nio.charset.Charset)",
            "java.io.PrintStream.append(char)",
            "java.io.PrintStream.append(java.lang.CharSequence,int,int)",
            "java.io.PrintStream.append(java.lang.CharSequence)",

            // DoubleStream
            "java.util.stream.DoubleStream.of(double[])"
    };
}

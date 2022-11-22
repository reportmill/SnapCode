package snapcode.app;
import javakit.resolver.*;
import java.io.PrintStream;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver extends javakit.resolver.StaticResolver {

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

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray":
                mb.name("doubleArray").returnType(double[].class).save();
                mb.name("fromMinMax").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("fromMinMaxCount").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("clone").returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("clone").returnType(snapcharts.data.DataArray.class).save();
                mb.name("clone").returnType(snapcharts.data.NumberArray.class).save();
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("toArray").returnType(double[].class).save();
                mb.name("of").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("filter").paramTypes(java.util.function.DoublePredicate.class).returnType(snapcharts.data.DoubleArray.class).save();
                return mb.name("map").paramTypes(java.util.function.DoubleUnaryOperator.class).returnType(snapcharts.data.DoubleArray.class).buildAll();

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray":
                mb.name("toString").returnType(java.lang.String.class).save();
                mb.name("getName").returnType(java.lang.String.class).save();
                return mb.name("length").returnType(int.class).buildAll();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet":
                mb.name("getPropValue").paramTypes(java.lang.String.class).returnType(java.lang.Object.class).save();
                mb.name("setPropValue").paramTypes(java.lang.String.class,java.lang.Object.class).returnType(void.class).save();
                mb.name("clone").returnType(snap.props.PropObject.class).save();
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("clone").returnType(snapcharts.data.DataSet.class).save();
                return mb.name("getName").returnType(java.lang.String.class).buildAll();

            // Handle snapcode.app.ChartsREPL
            case "snapcode.app.ChartsREPL":
                mb.name("println").paramTypes(java.lang.Object.class).returnType(void.class).save();
                return mb.name("print").paramTypes(java.lang.Object.class).returnType(void.class).buildAll();

            // Handle snapcode.app.Quick3D
            case "snapcode.app.Quick3D":
                mb.name("createCube").returnType(snap.gfx3d.CameraView.class).save();
                return mb.name("createImage3D").paramTypes(snap.gfx.Image.class).returnType(snap.gfx3d.CameraView.class).buildAll();

            // Handle snapcode.app.QuickCharts
            case "snapcode.app.QuickCharts":
                mb.name("chart").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().save();
                return mb.name("chart3D").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().buildAll();

            // Handle snapcode.app.QuickData
            case "snapcode.app.QuickData":
                mb.name("dataSet").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DataSet.class).varArgs().save();
                mb.name("mapXY").paramTypes(double[].class,double[].class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("mapXY").paramTypes(snapcharts.data.DoubleArray.class,snapcharts.data.DoubleArray.class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("doubleArray").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("dataArray").paramTypes(java.lang.Object.class).returnType(snapcharts.data.DataArray.class).save();
                mb.name("minMaxArray").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("minMaxArray").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("getTextForSource").paramTypes(java.lang.Object.class).returnType(java.lang.String.class).save();
                return mb.name("getImageForSource").paramTypes(java.lang.Object.class).returnType(snap.gfx.Image.class).buildAll();

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

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray.fromMinMax(double,double)":
                return snapcharts.data.DoubleArray.fromMinMax(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcharts.data.DoubleArray.fromMinMaxCount(double,double,int)":
                return snapcharts.data.DoubleArray.fromMinMaxCount(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));
            case "snapcharts.data.DoubleArray.clone()":
                return ((snapcharts.data.DoubleArray) anObj).clone();
            case "snapcharts.data.DoubleArray.toArray()":
                return ((snapcharts.data.DoubleArray) anObj).toArray();
            case "snapcharts.data.DoubleArray.of(java.lang.Object[])":
                return snapcharts.data.DoubleArray.of(theArgs);
            case "snapcharts.data.DoubleArray.filter(java.util.function.DoublePredicate)":
                return ((snapcharts.data.DoubleArray) anObj).filter((java.util.function.DoublePredicate) theArgs[0]);
            case "snapcharts.data.DoubleArray.map(java.util.function.DoubleUnaryOperator)":
                return ((snapcharts.data.DoubleArray) anObj).map((java.util.function.DoubleUnaryOperator) theArgs[0]);

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray.getName()":
                return ((snapcharts.data.DataArray) anObj).getName();
            case "snapcharts.data.DataArray.length()":
                return ((snapcharts.data.DataArray) anObj).length();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet.clone()":
                return ((snapcharts.data.DataSet) anObj).clone();
            case "snapcharts.data.DataSet.getName()":
                return ((snapcharts.data.DataSet) anObj).getName();

            // Handle snapcode.app.ChartsREPL
            case "snapcode.app.ChartsREPL.println(java.lang.Object)":
                snapcode.app.ChartsREPL.println(theArgs[0]); return null;
            case "snapcode.app.ChartsREPL.print(java.lang.Object)":
                snapcode.app.ChartsREPL.print(theArgs[0]); return null;

            // Handle snapcode.app.Quick3D
            case "snapcode.app.Quick3D.createCube()":
                return snapcode.app.Quick3D.createCube();
            case "snapcode.app.Quick3D.createImage3D(snap.gfx.Image)":
                return snapcode.app.Quick3D.createImage3D((snap.gfx.Image) theArgs[0]);

            // Handle snapcode.app.QuickCharts
            case "snapcode.app.QuickCharts.chart(java.lang.Object[])":
                return snapcode.app.QuickCharts.chart(theArgs);
            case "snapcode.app.QuickCharts.chart3D(java.lang.Object[])":
                return snapcode.app.QuickCharts.chart3D(theArgs);

            // Handle snapcode.app.QuickData
            case "snapcode.app.QuickData.dataSet(java.lang.Object[])":
                return snapcode.app.QuickData.dataSet(theArgs);
            case "snapcode.app.QuickData.mapXY(double[],double[],java.util.function.DoubleBinaryOperator)":
                return snapcode.app.QuickData.mapXY((double[]) theArgs[0],(double[]) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.app.QuickData.mapXY(snapcharts.data.DoubleArray,snapcharts.data.DoubleArray,java.util.function.DoubleBinaryOperator)":
                return snapcode.app.QuickData.mapXY((snapcharts.data.DoubleArray) theArgs[0],(snapcharts.data.DoubleArray) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.app.QuickData.doubleArray(java.lang.Object[])":
                return snapcode.app.QuickData.doubleArray(theArgs);
            case "snapcode.app.QuickData.dataArray(java.lang.Object)":
                return snapcode.app.QuickData.dataArray(theArgs[0]);
            case "snapcode.app.QuickData.minMaxArray(double,double)":
                return snapcode.app.QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcode.app.QuickData.minMaxArray(double,double,int)":
                return snapcode.app.QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));
            case "snapcode.app.QuickData.getTextForSource(java.lang.Object)":
                return snapcode.app.QuickData.getTextForSource(theArgs[0]);
            case "snapcode.app.QuickData.getImageForSource(java.lang.Object)":
                return snapcode.app.QuickData.getImageForSource(theArgs[0]);

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

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray":
                return cb.paramTypes(double[].class).buildAll();

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

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray(double[])":
                return new snapcharts.data.DoubleArray((double[]) theArgs[0]);

            // Handle anything else
            default:
                if (_next != null) return _next.invokeConstructor(anId, theArgs);
                throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }
}

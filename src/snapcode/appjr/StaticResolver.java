package snapcode.appjr;
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
                mb.name("clone").returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("clone").returnType(snapcharts.data.NumberArray.class).save();
                mb.name("clone").returnType(snapcharts.data.DataArray.class).save();
                mb.name("toArray").returnType(double[].class).save();
                mb.name("filter").paramTypes(java.util.function.DoublePredicate.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("map").paramTypes(java.util.function.DoubleUnaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("of").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("fromMinMax").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("doubleArray").returnType(double[].class).save();
                return mb.name("fromMinMaxCount").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).buildAll();

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray":
                mb.name("length").returnType(int.class).save();
                mb.name("toString").returnType(java.lang.String.class).save();
                return mb.name("getName").returnType(java.lang.String.class).buildAll();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet":
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("clone").returnType(snapcharts.data.DataSet.class).save();
                mb.name("clone").returnType(snap.props.PropObject.class).save();
                mb.name("getName").returnType(java.lang.String.class).save();
                mb.name("getPropValue").paramTypes(java.lang.String.class).returnType(java.lang.Object.class).save();
                return mb.name("setPropValue").paramTypes(java.lang.String.class,java.lang.Object.class).returnType(void.class).buildAll();

            // Handle snapcode.appjr.ChartsREPL
            case "snapcode.appjr.ChartsREPL":
                mb.name("println").paramTypes(java.lang.Object.class).returnType(void.class).save();
                return mb.name("print").paramTypes(java.lang.Object.class).returnType(void.class).buildAll();

            // Handle snapcode.appjr.Quick3D
            case "snapcode.appjr.Quick3D":
                mb.name("createCube").returnType(snap.gfx3d.CameraView.class).save();
                return mb.name("createImage3D").paramTypes(snap.gfx.Image.class).returnType(snap.gfx3d.CameraView.class).buildAll();

            // Handle snapcode.appjr.QuickCharts
            case "snapcode.appjr.QuickCharts":
                mb.name("chart").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().save();
                return mb.name("chart3D").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().buildAll();

            // Handle snapcode.appjr.QuickData
            case "snapcode.appjr.QuickData":
                mb.name("dataArray").paramTypes(java.lang.Object.class).returnType(snapcharts.data.DataArray.class).save();
                mb.name("dataSet").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DataSet.class).varArgs().save();
                mb.name("mapXY").paramTypes(double[].class,double[].class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("mapXY").paramTypes(snapcharts.data.DoubleArray.class,snapcharts.data.DoubleArray.class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("doubleArray").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("minMaxArray").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("minMaxArray").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("getTextForSource").paramTypes(java.lang.Object.class).returnType(java.lang.String.class).save();
                return mb.name("getImageForSource").paramTypes(java.lang.Object.class).returnType(snap.gfx.Image.class).buildAll();

            // Handle snapcode.appjr.QuickDraw
            case "snapcode.appjr.QuickDraw":
                mb.name("isShowGrid").returnType(boolean.class).save();
                mb.name("isAnimate").returnType(boolean.class).save();
                mb.name("setAnimate").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("getPen").paramTypes(int.class).returnType(snapcode.appjr.QuickDrawPen.class).save();
                mb.name("getPen").returnType(snapcode.appjr.QuickDrawPen.class).save();
                mb.name("moveTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("lineTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("closePath").returnType(void.class).save();
                mb.name("forward").paramTypes(double.class).returnType(void.class).save();
                mb.name("turn").paramTypes(double.class).returnType(void.class).save();
                mb.name("createDrawView").paramTypes(int.class,int.class).returnType(snapcode.appjr.QuickDraw.class).save();
                mb.name("createDrawView").returnType(snapcode.appjr.QuickDraw.class).save();
                mb.name("setShowGrid").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("getGridSpacing").returnType(double.class).save();
                mb.name("setGridSpacing").paramTypes(double.class).returnType(void.class).save();
                return mb.name("setPenColor").paramTypes(snap.gfx.Color.class).returnType(void.class).buildAll();

            // Handle snapcode.appjr.QuickDrawPen
            case "snapcode.appjr.QuickDrawPen":
                mb.name("getColor").returnType(snap.gfx.Color.class).save();
                mb.name("setColor").paramTypes(snap.gfx.Color.class).returnType(void.class).save();
                mb.name("getWidth").returnType(double.class).save();
                mb.name("setWidth").paramTypes(double.class).returnType(void.class).save();
                mb.name("moveTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("lineTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("closePath").returnType(void.class).save();
                mb.name("forward").paramTypes(double.class).returnType(void.class).save();
                mb.name("turn").paramTypes(double.class).returnType(void.class).save();
                mb.name("getAnimPen").returnType(snapcode.appjr.QuickDrawPen.class).save();
                mb.name("getDirection").returnType(double.class).save();
                return mb.name("setDirection").paramTypes(double.class).returnType(void.class).buildAll();

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
            case "snapcharts.data.DoubleArray.clone()":
                return ((snapcharts.data.DoubleArray) anObj).clone();
            case "snapcharts.data.DoubleArray.toArray()":
                return ((snapcharts.data.DoubleArray) anObj).toArray();
            case "snapcharts.data.DoubleArray.filter(java.util.function.DoublePredicate)":
                return ((snapcharts.data.DoubleArray) anObj).filter((java.util.function.DoublePredicate) theArgs[0]);
            case "snapcharts.data.DoubleArray.map(java.util.function.DoubleUnaryOperator)":
                return ((snapcharts.data.DoubleArray) anObj).map((java.util.function.DoubleUnaryOperator) theArgs[0]);
            case "snapcharts.data.DoubleArray.of(java.lang.Object[])":
                return snapcharts.data.DoubleArray.of(theArgs);
            case "snapcharts.data.DoubleArray.fromMinMax(double,double)":
                return snapcharts.data.DoubleArray.fromMinMax(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcharts.data.DoubleArray.fromMinMaxCount(double,double,int)":
                return snapcharts.data.DoubleArray.fromMinMaxCount(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray.length()":
                return ((snapcharts.data.DataArray) anObj).length();
            case "snapcharts.data.DataArray.getName()":
                return ((snapcharts.data.DataArray) anObj).getName();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet.clone()":
                return ((snapcharts.data.DataSet) anObj).clone();
            case "snapcharts.data.DataSet.getName()":
                return ((snapcharts.data.DataSet) anObj).getName();

            // Handle snapcode.appjr.ChartsREPL
            case "snapcode.appjr.ChartsREPL.println(java.lang.Object)":
                snapcode.appjr.ChartsREPL.println(theArgs[0]); return null;
            case "snapcode.appjr.ChartsREPL.print(java.lang.Object)":
                snapcode.appjr.ChartsREPL.print(theArgs[0]); return null;

            // Handle snapcode.appjr.Quick3D
            case "snapcode.appjr.Quick3D.createCube()":
                return snapcode.appjr.Quick3D.createCube();
            case "snapcode.appjr.Quick3D.createImage3D(snap.gfx.Image)":
                return snapcode.appjr.Quick3D.createImage3D((snap.gfx.Image) theArgs[0]);

            // Handle snapcode.appjr.QuickCharts
            case "snapcode.appjr.QuickCharts.chart(java.lang.Object[])":
                return snapcode.appjr.QuickCharts.chart(theArgs);
            case "snapcode.appjr.QuickCharts.chart3D(java.lang.Object[])":
                return snapcode.appjr.QuickCharts.chart3D(theArgs);

            // Handle snapcode.appjr.QuickData
            case "snapcode.appjr.QuickData.dataArray(java.lang.Object)":
                return snapcode.appjr.QuickData.dataArray(theArgs[0]);
            case "snapcode.appjr.QuickData.dataSet(java.lang.Object[])":
                return snapcode.appjr.QuickData.dataSet(theArgs);
            case "snapcode.appjr.QuickData.mapXY(double[],double[],java.util.function.DoubleBinaryOperator)":
                return snapcode.appjr.QuickData.mapXY((double[]) theArgs[0],(double[]) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.appjr.QuickData.mapXY(snapcharts.data.DoubleArray,snapcharts.data.DoubleArray,java.util.function.DoubleBinaryOperator)":
                return snapcode.appjr.QuickData.mapXY((snapcharts.data.DoubleArray) theArgs[0],(snapcharts.data.DoubleArray) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.appjr.QuickData.doubleArray(java.lang.Object[])":
                return snapcode.appjr.QuickData.doubleArray(theArgs);
            case "snapcode.appjr.QuickData.minMaxArray(double,double)":
                return snapcode.appjr.QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcode.appjr.QuickData.minMaxArray(double,double,int)":
                return snapcode.appjr.QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));
            case "snapcode.appjr.QuickData.getTextForSource(java.lang.Object)":
                return snapcode.appjr.QuickData.getTextForSource(theArgs[0]);
            case "snapcode.appjr.QuickData.getImageForSource(java.lang.Object)":
                return snapcode.appjr.QuickData.getImageForSource(theArgs[0]);

            // Handle snapcode.appjr.QuickDraw
            case "snapcode.appjr.QuickDraw.isShowGrid()":
                return ((snapcode.appjr.QuickDraw) anObj).isShowGrid();
            case "snapcode.appjr.QuickDraw.isAnimate()":
                return ((snapcode.appjr.QuickDraw) anObj).isAnimate();
            case "snapcode.appjr.QuickDraw.setAnimate(boolean)":
                ((snapcode.appjr.QuickDraw) anObj).setAnimate(boolVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDraw.getPen(int)":
                return ((snapcode.appjr.QuickDraw) anObj).getPen(intVal(theArgs[0]));
            case "snapcode.appjr.QuickDraw.getPen()":
                return ((snapcode.appjr.QuickDraw) anObj).getPen();
            case "snapcode.appjr.QuickDraw.moveTo(double,double)":
                ((snapcode.appjr.QuickDraw) anObj).moveTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.appjr.QuickDraw.lineTo(double,double)":
                ((snapcode.appjr.QuickDraw) anObj).lineTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.appjr.QuickDraw.closePath()":
                ((snapcode.appjr.QuickDraw) anObj).closePath(); return null;
            case "snapcode.appjr.QuickDraw.forward(double)":
                ((snapcode.appjr.QuickDraw) anObj).forward(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDraw.turn(double)":
                ((snapcode.appjr.QuickDraw) anObj).turn(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDraw.createDrawView(int,int)":
                return snapcode.appjr.QuickDraw.createDrawView(intVal(theArgs[0]),intVal(theArgs[1]));
            case "snapcode.appjr.QuickDraw.createDrawView()":
                return snapcode.appjr.QuickDraw.createDrawView();
            case "snapcode.appjr.QuickDraw.setShowGrid(boolean)":
                ((snapcode.appjr.QuickDraw) anObj).setShowGrid(boolVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDraw.getGridSpacing()":
                return ((snapcode.appjr.QuickDraw) anObj).getGridSpacing();
            case "snapcode.appjr.QuickDraw.setGridSpacing(double)":
                ((snapcode.appjr.QuickDraw) anObj).setGridSpacing(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDraw.setPenColor(snap.gfx.Color)":
                ((snapcode.appjr.QuickDraw) anObj).setPenColor((snap.gfx.Color) theArgs[0]); return null;

            // Handle snapcode.appjr.QuickDrawPen
            case "snapcode.appjr.QuickDrawPen.getColor()":
                return ((snapcode.appjr.QuickDrawPen) anObj).getColor();
            case "snapcode.appjr.QuickDrawPen.setColor(snap.gfx.Color)":
                ((snapcode.appjr.QuickDrawPen) anObj).setColor((snap.gfx.Color) theArgs[0]); return null;
            case "snapcode.appjr.QuickDrawPen.getWidth()":
                return ((snapcode.appjr.QuickDrawPen) anObj).getWidth();
            case "snapcode.appjr.QuickDrawPen.setWidth(double)":
                ((snapcode.appjr.QuickDrawPen) anObj).setWidth(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDrawPen.moveTo(double,double)":
                ((snapcode.appjr.QuickDrawPen) anObj).moveTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.appjr.QuickDrawPen.lineTo(double,double)":
                ((snapcode.appjr.QuickDrawPen) anObj).lineTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.appjr.QuickDrawPen.closePath()":
                ((snapcode.appjr.QuickDrawPen) anObj).closePath(); return null;
            case "snapcode.appjr.QuickDrawPen.forward(double)":
                ((snapcode.appjr.QuickDrawPen) anObj).forward(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDrawPen.turn(double)":
                ((snapcode.appjr.QuickDrawPen) anObj).turn(doubleVal(theArgs[0])); return null;
            case "snapcode.appjr.QuickDrawPen.getAnimPen()":
                return ((snapcode.appjr.QuickDrawPen) anObj).getAnimPen();
            case "snapcode.appjr.QuickDrawPen.getDirection()":
                return ((snapcode.appjr.QuickDrawPen) anObj).getDirection();
            case "snapcode.appjr.QuickDrawPen.setDirection(double)":
                ((snapcode.appjr.QuickDrawPen) anObj).setDirection(doubleVal(theArgs[0])); return null;

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

            // Handle snapcode.appjr.QuickDraw
            case "snapcode.appjr.QuickDraw":
                return cb.paramTypes(int.class,int.class).buildAll();

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

            // Handle snapcode.appjr.QuickDraw
            case "snapcode.appjr.QuickDraw(int,int)":
                return new snapcode.appjr.QuickDraw(intVal(theArgs[0]),intVal(theArgs[1]));

            // Handle anything else
            default:
                if (_next != null) return _next.invokeConstructor(anId, theArgs);
                throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }
}

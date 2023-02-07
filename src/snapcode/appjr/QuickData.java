/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import snap.gfx.Image;
import snap.web.WebURL;
import snapcharts.data.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

/**
 * This is a REPL base class specific for charts.
 */
public class QuickData {

    /**
     * Creates and return a DataArray.
     */
    public static DoubleArray doubleArray(Object ... theDoubles)
    {
        return DoubleArray.of(theDoubles);
    }

    /**
     * Creates and return a DataArray.
     */
    public static DataArray dataArray(Object anObj)
    {
        // Handle null
        if (anObj == null) return null;

        // Handle DataArray
        if (anObj instanceof DataArray)
            return (DataArray) anObj;

        // Handle array of anything
        if (anObj.getClass().isArray())
            return doubleArray(anObj);

        // Return not handled
        return null;
    }

    /**
     * Creates and returns a Dataset.
     */
    public static DataSet dataSet(Object ... theObjects)
    {
        // Handle DataSet
        if (theObjects.length > 0 && theObjects[0] instanceof DataSet)
            return (DataSet) theObjects[0];

        // Get objects as DataArray array
        List<DataArray> dataArraysList = new ArrayList<>();
        for (Object obj : theObjects) {
            DataArray dataArray = dataArray(obj);
            if (dataArray != null)
                dataArraysList.add(dataArray);
        }

        // Return if empty
        if (dataArraysList.size() < 2)
            throw new IllegalArgumentException("ChartsREPL.dataSet: Not enough data arrays");

        // Create DataArrays and get DataType
        DataArray[] dataArrays = dataArraysList.toArray(new DataArray[0]);
        DataType dataType = dataArrays.length < 3 ? DataType.XY : DataType.XYZ;
        if (dataType == DataType.XYZ && dataArrays[0].length() != dataArrays[2].length())
            dataType = DataType.XYZZ;

        // Create/config DataSet
        DataSet dataSet = dataType == DataType.XYZZ ? new DataSetXYZZ() : new DataSetImpl();
        dataSet.setDataType(dataType);
        dataSet.setDataArrays(dataArrays);

        // Return
        return dataSet;
    }

    /**
     * Returns text for given source.
     */
    public static String getTextForSource(Object aSource)
    {
        // Get URL for source
        WebURL url = WebURL.getURL(aSource);
        if (url == null)
            return null;

        // Get text for URL and return
        String text = url.getText();
        return text;
    }

    /**
     * Returns text for given source.
     */
    public static Image getImageForSource(Object aSource)
    {
        // Get URL for source
        WebURL url = WebURL.getURL(aSource);
        if (url == null)
            return null;

        // Get image for URL and return
        Image image = Image.get(url);
        return image;
    }

    /**
     * Creates a double array of min/max.
     */
    public static DoubleArray minMaxArray(double aMin, double aMax)
    {
        return DoubleArray.fromMinMax(aMin, aMax);
    }

    /**
     * Creates a double array of min/max/count.
     */
    public static DoubleArray minMaxArray(double aMin, double aMax, int aCount)
    {
        return DoubleArray.fromMinMaxCount(aMin, aMax, aCount);
    }

    /**
     * Maps XY to Z.
     */
    public static DoubleArray mapXY(double[] x, double[] y, DoubleBinaryOperator mapper)
    {
        // Get Z double array
        double[] z = new double[x.length * y.length];

        // Iterate over X/Y and generate Z
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < y.length; j++) {
                z[i * y.length + j] = mapper.applyAsDouble(x[i], y[j]);
            }
        }

        // Return new double Array
        return new DoubleArray(z);
    }

    /**
     * Maps XY to Z.
     */
    public static DoubleArray mapXY(DoubleArray aX, DoubleArray aY, DoubleBinaryOperator mapper)
    {
        return mapXY(aX.doubleArray(), aY.doubleArray(), mapper);
    }
}

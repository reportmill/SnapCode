

# Basic Java

## Variable Definitions

```
int x = 1;
float y = 2.5f;
double pi = 3.14d;
String str = "Hello World";
show(str);
```

## System logging

```
System.out.println("Hello World");
```

## System Input

```
// Print prompt message
System.out.println("Enter a number to be squared: ");
    
// Create scanner from System.in and read int value
Scanner scanner = new Scanner(System.in);
int value = scanner.nextInt();
    
// Print result of value squared
System.out.println(value + " * " + value + " = " + (value * value));
```

## Conditionals

```
if (2 + 2 == 4)
    System.out.println("All is right with the world");
```

## For/while loops

```
for (int i = 0; i < 10; i++)
    System.out.println("Looping: " + i);
```

## Method definition

```
public int factorial(int aValue)
{
    int factorial = 1;
    for (int i = aValue; i > 0; i--)
        factorial *= i;
    return factorial;
}
```

# Advanced Java

## Streams

```
var names = Stream.of("John", "Paul", "George", "Ringo");
var names2 = names.map(str -> str.toUpperCase());
var namesStr = names2.collect(Collectors.joining(", "));
show(namesStr);
```

## Lambda

```
// Create button that prints any mouse events it receives
Button button = new Button("Hello World");
button.setPrefSize(120, 30);
button.addEventFilter(e -> show(e), View.MouseEvents);
show(button);
```

# Define datasets

## Simple array

```
var x = new double[] { 1, 2, 3, 4 };
```

## Array from Range

```
var x = DoubleArray.fromMinMax(-3, 3);
```

## Array from Range with count

```
var x = DoubleArray.fromMinMaxCount(-3, 3, 100);
```

## Array from other array via function

```
var y = DoubleArray.of(x).map(d -> Math.sin(d));
```

## DataSet from data arrays

```
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };
var dataSet = dataSet(x, y);
show(dataSet)
```

## DataSet from 3D data

```
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));
var dataSet = dataSet(x, y, z);
show(dataSet);
```

# Create Chart

## From data arrays

```
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };
var chart = chart(x,y);
show(chart);
```

## From data set

```
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };
var dataSet = dataSet(x, y);
var chart = chart(dataSet);
show(dataSet);
show(chart);
```

## From 3D data

```
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));
var chart = chart(x, y, z);
show(chart);
```

# Create 3D Chart

## From data arrays

```
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));
var chart = chart3D(x, y, z);
show(chart);
```

## From data set

```
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));
var dataSet = dataSet(x, y, z);
var chart = chart3D(dataSet);
show(chart);
```

# Draw Vector Graphics

## Draw box

```
var drawView = QuickDraw.createDrawView();
drawView.moveTo(100, 100);
for (int i = 0; i < 4; i++) {
    drawView.forward(200);
    drawView.turn(90);
}
show(drawView);
```

## Draw Spiral

```
var drawView = QuickDraw.createDrawView();
var drawView.moveTo(200, 200);
for (int i = 0; i < 1080; i++) {
    drawView.forward(i / 360d);
    drawView.turn(1);
}
show(drawView);
```

# Fetch remote data / images

## Text

```
var text = getTextForSource("https://reportmill.com/examples/AAPL.csv");
show(text);
```

## Image

```
var image = getImageForSource("https://reportmill.com/examples/Weird.jpg");
show(image);
```

# Create UI

## Create Button

```
var button = new Button("Hello World");
button.setPrefSize(100, 25);
button.setMargin(20,20,20,20);
show(button);
```

## Create Slider

```
var slider = new Slider();
slider.setPrefSize(300, 25);
slider.setMargin(20,20,20,20);
show(slider);
```

## Create TextField

```
var textField = new TextField();
textField.setPrefSize(300, 25);
textField.setMargin(20,20,20,20);
show(textField);
```

## Create Window

```
var button = new Button("Hello World");
button.setPrefSize(400, 400);
ViewOwner viewOwner = new ViewOwner(button);
viewOwner.setWindowVisible(true);
```

# Animate UI

## Animate Button - shorthand

```
var button = new Button("Hello World");
button.setMargin(50,50,50,50);
button.setAnimString("time: 1000; scale: 3; time: 2000; scale: 1");
button.setAnimString("time: 2000; rotate: 360");
button.getAnim(0).setLoopCount(4).play();
show(button);
```

## Animate Button - traditional

```
var button = new Button("Hello World");
button.setMargin(50,50,50,50);
var anim = button.getAnim(0);
anim.getAnim(1000).setScale(3).getAnim(2000).setScale(1);
anim.getAnim(2000).setRotate(360);
anim.setLoopCount(4).play();
show(button);
```

# Create 3D

## 3D cube

```
var cube = Quick3D.createCube();
show(cube);
```

## 3D image

```
var image = getImageForSource("https://reportmill.com/examples/Weird.jpg");
var image3D = Quick3D.createImage3D(image);
show(image3D);
```

# Swing

## Create Button

```
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;

// Create button
JButton button = new JButton("Hello World");

// Create frame and show
JFrame frame = new JFrame("Hello World");
frame.setPreferredSize(new Dimension(300, 300));
frame.setContentPane(button);
frame.pack();
frame.setLocationRelativeTo(null);
frame.setVisible(true);
```

# External Libraries

## JFiglet

```
//DEPS com.github.lalyos:jfiglet:0.0.8
import com.github.lalyos.jfiglet.FigletFont;

var str = FigletFont.convertOneLine("Hello World");
System.out.println(str);
```

## ReportMill

```
//DEPS com.reportmill:ReportMill16:2025.01
import com.reportmill.base.*;
import com.reportmill.shape.RMDocument;
import java.io.File;

// Get template and dataset
RMDocument template = new RMDocument(RMExtras.getMoviesURL());
Object dataSet = new RMXMLReader().readObject(RMExtras.getHollywoodURL());

// Generate report, write PDF and open file
RMDocument report = template.generateReport(dataSet);
String filePath = System.getProperty("java.io.tmpdir") + File.separator + "Report.pdf";
report.writePDF(filePath);
GFXEnv.getEnv().openFile(filePath);
```

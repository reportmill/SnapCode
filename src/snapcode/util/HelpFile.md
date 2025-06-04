

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

# Files

## List files in home directory

```
// Get home directory
String homeDir = System.getProperty("user.home");
Path homePath = Paths.get(homeDir);

// Get files and print
try (Stream<Path> files = Files.list(homePath)) {
    System.out.println("Files in home directory: " + homeDir);
    files.forEach(System.out::println);
}

// Catch exceptions
catch (IOException e) { System.err.println("Error listing files: " + e.getMessage()); }
```

## Read and Write to file

```
// Get path to temp file and write text
Path filePath = Paths.get(System.getProperty("java.io.tmpdir"), "hello.txt");
Files.write(filePath, "Hello World".getBytes());

// Read lines from file and print
List<String> lines = Files.readAllLines(filePath);
System.out.println("Contents of the file:");
lines.forEach(System.out::println);
```

# Streams

## Map names list to uppercase

```
// Create list of names, map to upper case, join and print
var names = List.of("John", "Paul", "George", "Ringo");
var names2 = names.stream().map(str -> str.toUpperCase());
var namesStr = names2.collect(Collectors.joining(", "));
println(namesStr);
```

# Lambdas

## Create button with simple lambda defined action

```
// Create button and show
Button button = new Button("Hello World");
button.setPrefSize(120, 30);
show(button);

// Add event listener to print any mouse events it receives
button.addEventFilter(e -> show(e), View.MouseEvents);
```

# Records

## Simple Record

```
// Simple record
record Player(String last, String first, int level) {}
var jane = new Player("Doe", "Jane", 42);
System.out.println(jane);
```

## Composing

```
// Composing
record Population(int population) {}
record City(String name, Population population) {
    // static methods are allowed in records
    public static City of(String name, int p) {
        var population = new Population(p);
        return new City(name, population);
    }
}

var paris = City.of("Paris", 2161);
System.out.println(paris);
```

## Method Overriding

```
// Method overriding
record City(String name) {

    public boolean equals(Object other) {
        return other instanceof City city &&
                this.name.equalsIgnoreCase(city.name);
    }

    public int hashCode() {
        return name != null ? name.toUpperCase().hashCode() : 0;
    }
}

var paris1 = new City("Paris");
var paris2 = new City("paris");
var paris3 = new City("PARIS");
System.out.println("1 == 2 ? " + paris1.equals(paris2));
System.out.println("2 == 3 ? " + paris2.equals(paris3));
System.out.println("1 == 3 ? " + paris1.equals(paris3));
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
// Create XY data arrays
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };

// Create dataset and show
var dataSet = dataSet(x, y);
show(dataSet);
```

## DataSet from 3D data

```
// Create XYZ data arrays
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));

// Create dataset and show
var dataSet = dataSet(x, y, z);
show(dataSet);
```

# Create Chart

## From data arrays

```
// Create XY data arrays
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };

// Create chart and show
var chart = chart(x,y);
show(chart);
```

## From data set

```
// Create XY data arrays and dataset
var x = new double[] { 1, 2, 3, 4 };
var y = new double[] { 1, 4, 9, 16 };

// Create dataset and show
var dataSet = dataSet(x, y);
show(dataSet);

// Create chart and show
var chart = chart(dataSet);
show(chart);
```

## From 3D data

```
// Create XYZ data arrays
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));

// Create and show chart
var chart = chart(x, y, z);
show(chart);
```

# Create 3D Chart

## From data arrays

```
// Create XYZ data arrays
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));

// Create and show 3D chart
var chart = chart3D(x, y, z);
show(chart);
```

## From data set

```
// Create XYZ data arrays and dataset
var x = DoubleArray.fromMinMax(-3, 3);
var y = DoubleArray.fromMinMax(-4, 4);
var z = mapXY(x, y, (a,b) -> Math.sin(a) + Math.cos(b));
var dataSet = dataSet(x, y, z);

// Create and show 3D chart
var chart = chart3D(dataSet);
show(chart);
```

# Draw Vector Graphics

## Draw box

```
// Create draw view and show
var drawView = QuickDraw.createDrawView();
show(drawView);

// Set start point and make four move-forward/turn calls
drawView.moveTo(100, 100);
for (int i = 0; i < 4; i++) {
    drawView.forward(200);
    drawView.turn(90);
}
```

## Draw Spiral

```
// Create draw view and show
var drawView = QuickDraw.createDrawView();
show(drawView);

// Set start point and make a thousand move-forward/turn calls
drawView.moveTo(200, 200);
for (int i = 0; i < 1080; i++) {
    drawView.forward(i / 360d);
    drawView.turn(1);
}
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
// Create button, configure and show
var button = new Button("Hello World");
button.setPrefSize(100, 25);
button.setMargin(20,20,20,20);
show(button);
```

## Create Slider

```
// Create slider, configure and show
var slider = new Slider();
slider.setPrefSize(300, 25);
slider.setMargin(20,20,20,20);
show(slider);
```

## Create TextField

```
// Create textfield, configure and show
var textField = new TextField();
textField.setPrefSize(300, 25);
textField.setMargin(20,20,20,20);
show(textField);
```

## Create Window

```
// Create button and configure
var button = new Button("Hello World");
button.setPrefSize(400, 400);

// Create controller for button and show window
ViewOwner viewOwner = new ViewOwner(button);
viewOwner.setWindowVisible(true);
```

# Animate UI

## Animate Button - shorthand

```
// Create button, configure and show
var button = new Button("Hello World");
button.setMargin(50,50,50,50);
show(button);

// Add animation frames, set loop count and play
button.setAnimString("time: 1000; scale: 3; time: 2000; scale: 1");
button.setAnimString("time: 2000; rotate: 360");
button.getAnim(0).setLoopCount(4).play();
```

## Animate Button - traditional

```
// Create button, configure and show
var button = new Button("Hello World");
button.setMargin(50,50,50,50);
show(button);

// Add animation frames, set loop count and play
var anim = button.getAnim(0);
anim.getAnim(1000).setScale(3).getAnim(2000).setScale(1);
anim.getAnim(2000).setRotate(360);
anim.setLoopCount(4).play();
```

# Create 3D

## 3D cube

```
// Create cube and show
var cube = Quick3D.createCube();
show(cube);
```

## 3D image

```
// Get image, create image 3D and show
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

// Create frame, add button and show
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
//DEPS com.reportmill:ReportMill16:2025.06
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

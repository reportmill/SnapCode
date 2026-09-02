

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

var paris = City.of("Paris", 2_161);
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

# Sealed Classes

## Sealed classes with switch

```
// Switch on Sealed Types
sealed interface Shape permits Rectangle, Square, Circle { }
record Point(double x, double y) { }
record Edge(double u, double v) {
    public double scalar(Edge other)  { return this.u*other.u + this.v*other.v; }
    public double norm()  { return Math.sqrt(this.scalar(this)); }
}
record Square(Point p, Edge e) implements Shape { }
record Rectangle(Point p, Edge e1, Edge e2) implements Shape {
    public Rectangle {
        if (Math.abs(e1.scalar(e2)) > 1E-06)
            throw new IllegalArgumentException("Edges must be orthogonal");
    }
}
record Circle(Point center, double radius) implements Shape { }

ToDoubleFunction<Shape> surface = shape ->
        switch (shape) {
            case Rectangle r -> Math.sqrt(r.e1().norm()*r.e2().norm());
            case Square s -> s.e().norm();
            case Circle c -> Math.PI * c.radius() * c.radius();
        };
Function<Shape, String> toString = shape ->
        switch (shape) {
            case Rectangle r -> "Rectangle -> %.2f".formatted(surface.applyAsDouble(r));
            case Square s -> "Square    -> %.2f".formatted(surface.applyAsDouble(s));
            case Circle c -> "Circle    -> %.2f".formatted(surface.applyAsDouble(c));
        };

Shape s0 = new Square(new Point(0d, 0d), new Edge(0d, 0d));
Shape s1 = new Square(new Point(0d, 1d), new Edge(1d, 0d));
Shape s2 = new Square(new Point(2d, 3d), new Edge(1d, 1d));
Shape s3 = new Square(new Point(5d, 0d), new Edge(1d, 2d));
Rectangle r0 = new Rectangle(new Point(0d, 0d), new Edge(0d, 0d), new Edge(0d, 0d));
Rectangle r1 = new Rectangle(new Point(1d, 2d), new Edge(1d, 0d), new Edge(0d, 0d));
Rectangle r2 = new Rectangle(new Point(4d, 1d), new Edge(0d, 0d), new Edge(1d, 0d));
Rectangle r3 = new Rectangle(new Point(0d, 3d), new Edge(1d, 0d), new Edge(0d, 1d));
Rectangle r4 = new Rectangle(new Point(2d, 3d), new Edge(1d, 1d), new Edge(1d, -1d));
Circle c1 = new Circle(new Point(0d, 0d), 1d);
Circle c2 = new Circle(new Point(2d, 3d), 2d);
var shapes = List.of(s0, s1, s2, s3, r0, r1, r2, r3, r4, c1, c2);
shapes.stream().map(toString).forEach(System.out::println);
```

# Java 21

## Pattern Matching in Switch

```
// Create object of random collection class
var testObj = switch (new Random().nextInt(4)) {
    case 0 -> new ArrayList<>();
    case 1 -> new HashMap<>();
    case 2 -> new HashSet<>();
    case 3 -> List.of("Hello");
    default -> { System.err.println("Impossible value"); yield null; }
};

// Print object by class
switch (testObj) {
    case List<?> list when list.isEmpty() -> System.out.println("List class (empty)");
    case List<?> list -> System.out.println("List class");
    case Map<?,?> map -> System.out.println("Map class");
    case Set<?> set -> System.out.println("Set class");
    default -> throw new RuntimeException("Impossible value: " + testObj);
}
```

## Nested Record Patterns

```
record Point(int x, int y) { }
enum Color { RED, GREEN, BLUE }
record ColoredPoint(Point p, Color c) { }
record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) { }

var cp1 = new ColoredPoint(new Point(1,3), Color.RED);
var cp2 = new ColoredPoint(new Point(2,4), Color.BLUE);
var rect = new Rectangle(cp1, cp2);

printUpperLeftColoredPoint(rect);

static void printUpperLeftColoredPoint(Rectangle r) {
    if (r instanceof Rectangle(ColoredPoint ul, ColoredPoint lr)) {
         System.out.println(ul.c());
    }
}
```

# Java 25

## Compact source file

```
void main()
{
    IO.println("Hello World");
}
```

## Unnamed Variables and Patterns

```
// Consumer with Unnamed Pattern
List<String> strings = List.of("one", "two", "three");
Consumer<String> notInterested = _ -> System.out.println("I'm not interested in this argument");
strings.forEach(notInterested);
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
var image = Image.getImageForSource("https://reportmill.com/examples/Weird.jpg");
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
ViewController viewController = new ViewController(button);
viewController.setWindowVisible(true);
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
var image = Image.getImageForSource("https://reportmill.com/examples/Weird.jpg");
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

# DOM Manipulation (WebAPIs)

## Hello World

```
import snap.webapi.*;

var doc = Window.getDocument();
var button = doc.createElement("button");
button.getStyle().setCssText("position:fixed; top:20px; left:20px; padding:20px");
button.setTextContent("Hello World!");
doc.getBody().appendChild(button);
```

# External Libraries

## JFiglet

```
//DEPS com.github.lalyos:jfiglet:0.0.8
import com.github.lalyos.jfiglet.FigletFont;

var str = FigletFont.convertOneLine("Hello World");
System.out.println(str);
```

## JBox2D

```
//DEPS org.jbox2d:jbox2d-testbed:2.2.1.1
import javax.swing.*;

org.jbox2d.testbed.framework.TestbedMain.main(new String[0]);
```

## ReportMill

```
//DEPS com.reportmill:ReportMill16:2026.09
import com.reportmill.base.*;
import com.reportmill.shape.RMDocument;
import java.io.File;

// Get template and dataset
RMDocument template = new RMDocument(RMExtras.getMoviesURL());
Object dataSet = new RMXMLReader().readObjectFromUrl(RMExtras.getHollywoodURL(), null);

// Generate report, write PDF and open file
RMDocument report = template.generateReport(dataSet);
String filePath = System.getProperty("java.io.tmpdir") + File.separator + "Report.pdf";
report.writePDF(filePath);
GFXEnv.getEnv().openFile(filePath);
```

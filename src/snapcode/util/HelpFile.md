

# Define data array

## Simple

```
    x = new double[] { 1, 2, 3, 4 }
```

## From Range

```
    x = DoubleArray.fromMinMax(-3, 3)
```

## From Range with count

```
    x = DoubleArray.fromMinMaxCount(-3, 3, 100)
```

## From other array via function

```
    y = DoubleArray.of(x).map(d -> Math.sin(d))
```

# Define dataset

## From data arrays

```
    x = new double[] { 1, 2, 3, 4 }
    y = new double[] { 1, 4, 9, 16 }
    dataSet = dataSet(x, y)
```

## From 3D data

```
    x = DoubleArray.fromMinMax(-3, 3)
    y = DoubleArray.fromMinMax(-4, 4)
    z = mapXY(x, y, (x,y) -> Math.sin(x) + Math.cos(y))
    dataSet = dataSet(x, y, z)
```

# Create Chart

## From data arrays

```
    x = new double[] { 1, 2, 3, 4 }
    y = new double[] { 1, 4, 9, 16 }
    chart = chart(x,y)
```

## From data set

```
    x = new double[] { 1, 2, 3, 4 }
    y = new double[] { 1, 4, 9, 16 }
    dataSet = dataSet(x, y)
    chart = chart(dataSet)
```

## From 3D data

```
    x = DoubleArray.fromMinMax(-3, 3)
    y = DoubleArray.fromMinMax(-4, 4)
    z = mapXY(x, y, (x,y) -> Math.sin(x) + Math.cos(y))
    chart = chart(x, y, z)
```

# Create 3D Chart

## From data arrays

```
    x = DoubleArray.fromMinMax(-3, 3)
    y = DoubleArray.fromMinMax(-4, 4)
    z = mapXY(x, y, (x,y) -> Math.sin(x) + Math.cos(y))
    chart = chart3D(x, y, z)
```

## From data set

```
    x = DoubleArray.fromMinMax(-3, 3)
    y = DoubleArray.fromMinMax(-4, 4)
    z = mapXY(x, y, (x,y) -> Math.sin(x) + Math.cos(y))
    dataSet = dataSet(x, y, z)
    chart = chart3D(dataSet)
```

# Draw Vector Graphics

## Draw box

```
    drawView = QuickDraw.createDrawView()
    drawView.moveTo(100, 100)
    for (int i = 0; i < 4; i++) {
        drawView.forward(200)
        drawView.turn(90)
    }
```

## Draw Spiral

```
    drawView = QuickDraw.createDrawView()
    drawView.moveTo(200, 200)
    for (int i = 0; i < 1080; i++) {
        drawView.forward(i / 360d)
        drawView.turn(1)
    }
```

# Fetch remote data / images

## Text

```
    text = getTextForSource("https://reportmill.com/examples/AAPL.csv")
```

## Image

```
    image = getImageForSource("https://reportmill.com/examples/Weird.jpg")
```

# Create UI

## Create Button

```
    button = new Button("Hello World")
    button.setPrefSize(100, 25)
    button.setMargin(20,20,20,20)
```

## Create Slider

```
    slider = new Slider()
    slider.setPrefSize(300, 25)
    slider.setMargin(20,20,20,20)
```

# Animate UI

## Animate Button

```
    button = new Button("Hello World")
    button.setMargin(50,50,50,50)
    anim = button.getAnim(0)
    anim.getAnim(1000).setScale(3).getAnim(2000).setScale(1)
    anim.getAnim(2000).setRotate(360)
    anim.setLoopCount(4)
    anim.play()
```

# Create 3D

## 3D cube

```
    cube = Quick3D.createCube()
```

## 3D image

```
    image3D = Quick3D.createImage3D(image)
```

# Basic Java code

## Variable Definitions

```
    int x = 1
    float y = 2.5f
    double pi = 3.14d
    String str = "Hello World"
```

## System logging

```
    System.out.println("Hello World");
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

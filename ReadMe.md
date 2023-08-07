<!-- title: "SnapCode - a Java workbench for Data Science and Education" -->

<div style="position:relative;left:-25px;top:-40px;height:60px;">
  <script src="https://reportmill.com/shared/navbar.js"></script>
  <font size="1" face="Verdana, Helvetica, Arial" color="#CB0017">
    <a href="http://www.reportmill.com">HOME</a> &gt; SnapCode
  </font>
</div>

# <a name="title">SnapCode - a Java workbench for Data Science and Education</a>

<div align="center">
  <a href="https://reportmill.com/SnapCode/app">
    <img src="https://reportmill.com/SnapCode/SnapCode.png">
  </a>
  <br>
  <a href="https://reportmill.com/SnapCode/app">( Run SnapCode )</a>
</div>

<!-- [ ![SnapCode](https://reportmill.com/SnapCode/SnapCode.png)](http://www.reportmill.com/SnapCode/app/) -->

## <a name="Overview">Overview</a>

SnapCode makes it fast and easy to start coding in Java. SnapCode offers modern IDE coding features, support for
Java REPL (Read-Eval-Print-Loop), and full featured library support for UI, graphics, charting, 3D and more.
All of this makes SnapCode effective for a broad range of uses from data science to education.

SnapCode gets things started quickly with these features:

- Tiny and instant [in-browser download](https://reportmill.com/SnapCode)
- "No Compile" run using Java interpreter
- Incremental REPL style output (shows values for each line as interactive views)

## <a name="ModernFeatures">Modern IDE Features</a>

SnapCode has everything you would expect from a modern IDE to make writing code fast and easy:

- Syntax highlighting
- Code completion
- Matching symbol highlighting
- Balanced character pair handling for parens/brackets
- Inline compile warnings and errors
- One click access to JRE JavaDoc and source
- Editor shows the AST hierarchy of selected symbol

## <a name="JavaRepl">Java REPL (Read-Eval-Print-Loop)</a>

SnapCode allows you to avoid boilerplate code and evaluates code snippets as you type for instant feedback.

- Class declaration is optional
- Main method declaration is optional
- Variable type declarations are optional
- Statement terminators (semi-colons) are optional
- Direct access to common System methods without preamble (print(), println(), etc.)

Java REPL support is optional - full Java is supported, and can be auto-generated, for when code needs to be
portable and compliant.

## <a name="FullLibrary">Full Featured Library Support</a>

SnapCode is built using the SnapKit and SnapCharts libraries giving access to advanced application features.

- Complete UI programming with [SnapKit](https://github.com/reportmill/SnapKit)
  - Label, Button, Slider, TextField
  - ListView, TableView, TreeView, TabView, Browser
  - SplitView, ScrollView, DrawerView
  - Vector graphics shapes, arbitrary transforms, image effects, gradients, textures
  - 3D library support with OpenGL/WebGL (CameraView, VertexArray, Scene, Shape3D, Texture)
  - UI Animation to make things sizzle, slide, jiggle and bounce
- Complete charting support with [SnapCharts](https://github.com/reportmill/SnapCharts)
  - Bar, Pie 
  - Scatter, Area, Line
  - Contour, Polar Contour, Contour 3D
  - Log Axes, Multi-Y axes
  - Pan, Zoom, Mouse-Over labels

## <a name="HelpSystem">Interactive Help System</a>

SnapCode also has an integrated help system to quickly find templates for almost any kind of code.

## <a name="ComingSoon">Coming Soon</a>

Many features are on the immediate roadmap:

- Visual debugger, stepped run, break points
- Stack frame and stack variable inspection
- Turtle graphics
- Image processing
- Puzzle block coding
- File/project sharing via cloud and Github integration

## <a name="Technology">Technology</a>

This project is built with the following tools:

- [SnapCode](https://github.com/reportmill/SnapCode) - Source for SnapCode (Github)
- [SnapKit](https://github.com/reportmill/SnapKit) - Java UI Kit (Github)
- [JavaKit](https://github.com/reportmill/JavaKit) - Java Parsing library (Github)
- [SnapCharts](https://github.com/reportmill/SnapCharts) - Java charting library (Github)
- [TeaVM](https://teavm.org/) - A Java to JavaScript transpiler (Github)
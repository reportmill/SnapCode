<!-- title: "SnapCode - a real Java IDE for the Web" -->

<div style="position:relative;left:-25px;top:-40px;height:60px;">
  <script src="https://reportmill.com/shared/navbar.js"></script>
  <font size="1" face="Verdana, Helvetica, Arial" color="#CB0017">
    <a href="http://www.reportmill.com">HOME</a> &gt; SnapCode
  </font>
</div>

# <a name="title">SnapCode - a real Java IDE for the Web</a>

<div style="margin-left:20px;">
  <a href="https://reportmill.com/SnapCode/app">
    <div style="margin:0; padding:0;">
      <img src="https://reportmill.com/SnapCode/images/Charting.png" width="300" height="262">
      <img src="https://reportmill.com/SnapCode/images/Tetris.png" width="300" height="262">
    </div>
    <div style="margin:0; padding:0;">
      <img src="https://reportmill.com/SnapCode/images/Flappy.png" width="300" height="262">
      <img src="https://reportmill.com/SnapCode/images/Vector.png" width="300" height="262">
    </div>
  </a>
</div>  
<div style="margin-left:260px">
  <a href="https://reportmill.com/SnapCode/app">( Run SnapCode )</a>
</div>

<!-- [ ![SnapCode](https://reportmill.com/SnapCode/SnapCode.png)](http://www.reportmill.com/SnapCode/app/) -->

## <a name="Overview">Overview</a>

SnapCode makes it fast and easy to start coding Java in any modern browser. SnapCode offers modern coding features, support for
Java REPL (Read-Eval-Print-Loop), and full featured library support for UI, graphics, charting, 3D and more.
All of this makes SnapCode effective for a broad range of uses from education to engineering and data science.

SnapCode gets things started quickly with these features:

- No install - runs in browser [(Click here to run)](https://reportmill.com/SnapCode/app)
- Java REPL coding - just start coding (no class/main-method boilerplate needed)
- REPL style output (shows stacked output as rich interactive views)
- Integrated UI builder
- Integrated help system
- Block coding support
- Project level search (strings or symbols, references and declarations)
- Integrated developer tools
- Support for 'Share' links to easily share code
- Desktop app: [Download Page](https://reportmill.com/SnapCode/download.html)

## <a name="ModernFeatures">Modern IDE Features</a>

SnapCode has everything you expect from a modern IDE to make writing code fast and easy:

- Syntax highlighting
- Code completion (receiving class/type aware)
- Matching symbol highlighting
- Balanced character pair handling for parens/brackets
- Inline as-you-type warnings and errors
- One click jump to symbol declaration, super declarations, JavaDocs and source
- Editor shows the AST hierarchy of selected symbol

## <a name="JavaRepl">Java REPL (Read-Eval-Print-Loop)</a>

SnapCode allows you to avoid boilerplate code and evaluates code snippets as you type for instant feedback.

- Implicit Class and main method declarations - just start typing code
- Variable type 'var' for implicit variable type declaration
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

## <a name="UIBuilder">Integrated UI Builder</a>

Because the best line of code is the one you don't have to write, UI is almost always created using the with the
integrated UI builder and stored in simple XML files ('.snp' files). Simply create/save a .snp file with the same
name as your custom controller class, and the default controller.createUI() method will load it.

[ ![SnapBuilder](https://reportmill.com/snaptea/SnapBuilder/SnapBuilder.gif)](https://reportmill.com/snaptea/SnapBuilder/)

## <a name="BlockCoding">Block Coding</a>

Learn the basic concepts of coding in a visual way, by drag and drop. (This feature is still in preview).

[ ![Block Coding](https://reportmill.com/SnapCode/images/BlockCoding.png)](https://reportmill.com/snaptea/SnapBuilder/)

## <a name="HelpSystem">Interactive Help System</a>

SnapCode also has an integrated help system to quickly find templates for almost any kind of code.

## <a name="ComingSoon">Coming Soon</a>

Many features are on the immediate roadmap:

- Visual debugger in browser version
- Support for Java 11 and beyond
- Puzzle block coding
- Github support

## <a name="Technology">Technology</a>

This project is built with the following tools:

- [SnapCode](https://github.com/reportmill/SnapCode) - Source for SnapCode (Github)
- [SnapKit](https://github.com/reportmill/SnapKit) - Java UI Kit (Github)
- [SnapCharts](https://github.com/reportmill/SnapCharts) - Java charting library (Github)
- [CheerpJ](https://leaningtech.com/cheerpj/) - A Java JVM in the browser

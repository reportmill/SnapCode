# Building UI with SnapKit

**SnapKit** (package `snap.view` and friends) is a Java UI toolkit. It is not Swing or JavaFX.
Read this before writing UI code, and use `lookup_class` / `find_classes` to check any API you are not
sure of: don't guess method names.

SnapKit apps run inside SnapCode's browser IDE — for the project workflow (`create_project`,
`builder_*`, `build`, `run_app`) and the browser runtime rules, see the **SnapCode guide**
(`snapcode_guide` tool). A SnapKit UI layout lives in a `.snp` file (XML), edited with the
`builder_*` tools.

## Files

| File | What it is                                                                           |
|---|--------------------------------------------------------------------------------------|
| `Foo.java` | A normal Java class. Runnable if it has `main`.                                      |
| `Foo.snp` | A SnapKit UI layout (XML), edited with the SnapCode GUI builder (`builder_*` tools). |

## The usual workflow

1. `create_project` (or `open_project`), then `create_file` for `MyApp.java`.
2. Design `MyApp.snp` with `builder_open` + `builder_set_xml` (whole layout) or `builder_add_view` /
   `builder_set_properties` (step by step); check it with `builder_screenshot`; `builder_save`.
3. `build`, fix what `get_build_issues` reports, then `run_app` and `screenshot_app`.
4. Read `get_console_output` for printed output and exceptions.

## App structure: a ViewOwner and its .snp file

A window app is a subclass of `snap.view.ViewOwner`. Always include a `.snp` file with the
**same simple name as the class**, from the same folder (`MyApp.snp` next to `MyApp.java`) to define the UI
in SnapKit UI XML.
The UI in the `.snp` file is loaded automatically with the `createUI()` method.
Do not add a `createUI()` method - always define UI in the `.snp` file.

```java
import snap.view.*;

public class MyApp extends ViewOwner {

    private int _count;

    /** Called once, after the UI is created: configure views here. */
    @Override
    protected void initUI()
    {
    }

    /** Updates the UI from your data. Called after every respondUI(), and whenever you call resetLater(). */
    @Override
    protected void resetUI()
    {
        setViewText("CountText", String.valueOf(_count));
        setViewEnabled("ResetButton", _count > 0);
    }

    /** Called when a named control fires (button press, text field Enter, slider move, ...). */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {
            case "AddButton" -> _count++;
            case "ResetButton" -> _count = 0;
        }
    }

    public static void main(String[] args)
    {
        new MyApp().setWindowVisible(true);
    }
}
```

Key points:

- Controls are found **by their Name** (the `Name` attribute in the `.snp`). Give every control your code
  touches a unique Name.
- Change state in `respondUI()`, show state in `resetUI()`. You don't need to call `resetLater()` at the end
  of `respondUI()`; it happens automatically. Call `resetLater()` when state changes from somewhere else (a
  timer, a background thread via `runLater`).
- `initUI()` is only neccessary for UI initialization that isn't handled in `resetUI()`
- `initUI()` doesn't need to call `resetUI()`, which is called automatically before UI is first shown.
- Changes made inside `resetUI()` don't fire `respondUI()`.
- Useful `ViewOwner` helpers, all taking a view Name: `getView`, `getViewText` / `setViewText`,
  `getViewValue` / `setViewValue`, `getViewStringValue`, `getViewIntValue`, `getViewFloatValue`,
  `getViewBoolValue`, `setViewItems` / `getViewSelItem` / `getViewSelIndex`, `setViewEnabled`,
  `setViewVisible`, `requestFocus`. Also `runLater(Runnable)`, `runDelayed(Runnable, millis)`,
  `getImage("pic.png")` (image next to the class), `getWindow()`.
- On a `ViewEvent`: `equals("Name")`, `getName()`, `getStringValue()`, `getIntValue()`, `getBoolValue()`,
  `getView()`, and for mouse and key events `getX()`, `getY()`, `isMousePress()`, `getKeyCode()`, ...

## Layout (.snp)

Layouts are containers, not absolute positions:

- `ColView` stacks children vertically, `RowView` horizontally. `Spacing` sets the gap, `Padding` the inner
  margin, `Align` the alignment (`CENTER`, `TOP_LEFT`, `CENTER_LEFT`, ...). `FillWidth="true"` on a `ColView`
  (or `FillHeight` on a `RowView`) stretches children across.
- On a child: `GrowWidth` / `GrowHeight` take extra space, `PrefWidth` / `PrefHeight` set the preferred
  size, `Margin` adds outer space, `LeanX` / `LeanY` position it within its parent.
- Other containers: `BoxView` (one child), `BorderView`, `ScrollView`, `TitleView`, `TabView`, `SplitView`.
- Common controls: `Label`, `Button`, `ToggleButton`, `CheckBox`, `RadioButton`, `TextField`, `TextView`,
  `ComboBox`, `ListView`, `TableView`, `TreeView`, `Slider`, `Spinner`, `ProgressBar`, `ImageView`.
  `builder_list_view_types` lists them all; `builder_list_properties` lists a type's properties.
- Property values are strings: `Font="Arial Bold 18"`, `Fill="#EEEEEE"`, `Padding="10"` or
  `Padding="5,10,5,10"`, `Border="LINE #999999 1"`.

```xml
<ColView Padding="12" Spacing="8" FillWidth="true" PrefWidth="320">
  <Label Name="TitleLabel" Text="Counter" Font="Arial Bold 20" LeanX="CENTER" />
  <Label Name="CountText" Text="0" Font="Arial 36" LeanX="CENTER" />
  <RowView Spacing="8" Align="CENTER">
    <Button Name="AddButton" Text="Add" PrefWidth="90" />
    <Button Name="ResetButton" Text="Reset" PrefWidth="90" />
  </RowView>
</ColView>
```

## Timers and animation

**Use `snap.view.ViewTimer` for anything periodic** (clocks, stopwatches, game loops). It fires on the UI
thread, so it can update views directly:

```java
private ViewTimer _timer = new ViewTimer(this::tick, 20);   // run tick() every 20 ms

private void tick()  { _elapsed += 20; resetLater(); }

// in respondUI():  _timer.start();   _timer.pause();   _timer.stop();
```

For a one-off delay use `runDelayed(runnable, millis)`. To animate a view's properties, use its animator:
`view.getAnim(1000).setX(200).setRotate(360).play();` (also `setScale`, `setOpacity`, `setLoopCount(n)`,
`setOnFinish(runnable)`). The `Animate UI` help section has examples.

> Running in the browser, **never** use `ScheduledExecutorService` for timing — it freezes the tab when
> stopped. Use `ViewTimer`. See the browser rules in the SnapCode guide (`snapcode_guide`).

## Custom drawing and input

Subclass `View`, override `protected void paintFront(Painter aPntr)`, and call `repaint()` when the data
changes. `Painter` has `setColor(Color)`, `setFont(Font)`, `fillRect`, `drawRect`, `fillOval`, `drawLine`,
`drawString`, `drawImage`, `fill(Shape)`, `draw(Shape)`. Colors: `Color.RED` or `new Color("#3366CC")`;
fonts: `new Font("Arial", 14)`. For mouse input on a custom view, register in its constructor:
`addEventHandler(this::handleMouse, MousePress, MouseDrag, MouseRelease);`.

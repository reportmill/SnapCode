# Using the SnapCode IDE

SnapCode is a Java IDE that runs in the browser. You drive it with the WebMCP tools:
`create_project` / `open_project`, `create_file`, the `builder_*` tools for `.snp` layouts,
`build` / `get_build_issues`, `run_app` / `screenshot_app` / `get_console_output`, and the
`help_*` / `lookup_class` / `find_classes` tools.

Apps you build here usually use **SnapKit** (package `snap.view` and friends), a UI toolkit.
For how to actually write the UI code — the `ViewOwner` + `.snp` app structure, layout, events, timers and
drawing — read the **SnapKit guide** (`snapkit_guide` tool). This guide covers the IDE and its runtime.

## Files

| File | What it is |
|---|---|
| `Foo.java` | A normal Java class. Runnable if it has `main`. |
| `Foo.snp` | A SnapKit UI layout (XML), edited with the GUI builder (`builder_*` tools). |

Java files go in the project's source folder (`/src`).

## The usual workflow

1. `create_project` (or `open_project`), then `create_file` for `MyApp.java`.
2. Design `MyApp.snp` with `builder_open` + `builder_set_xml` (whole layout) or `builder_add_view` /
   `builder_set_properties` (step by step); check it with `builder_screenshot`; `builder_save`.
3. `build`, fix what `get_build_issues` reports, then `run_app` and `screenshot_app`.
4. Read `get_console_output` for printed output and exceptions.

## Rules for the browser

SnapCode runs apps on a Java VM inside the browser (CheerpJ). In this environment:

- **Never use `ScheduledExecutorService`** (`Executors.newScheduledThreadPool`,
  `newSingleThreadScheduledExecutor`, `ScheduledThreadPoolExecutor`). Stopping one with `shutdown()` or
  `shutdownNow()` **freezes the whole browser tab**. Use `ViewTimer` instead (see the SnapKit guide).
- Plain threads, `java.util.Timer` and ordinary thread pools do work, but **only touch views on the UI
  thread**: from another thread, wrap UI changes in `ViewUtils.runLater(...)` (or `runLater` on the
  `ViewOwner`). `ViewTimer` needs none of this.
- Don't block the UI thread: no `Thread.sleep` or long loops in `respondUI`, `resetUI` or timer callbacks.
  The whole UI stops responding until they return.
- `build` and `run_app` report a warning if a source file uses a pattern known to break here.

## Charts, scripts and help

- For charts, enable SnapCharts on the project: `set_build_settings` with `include_snapcharts: true`. The
  help sections `Define datasets` and `Create Chart` show how.
- `help_list_sections` / `help_get_section` give tested examples for common tasks (Basic Java, Create UI,
  Animate UI, Draw Vector Graphics, Create Chart, Create 3D, ...).
- `lookup_class` returns a class's real constructors, methods and docs; `find_classes` searches class names.
  Use them instead of guessing API — don't guess method names.

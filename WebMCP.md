# WebMCP support

SnapCode in the browser registers its features as [WebMCP](https://github.com/webmachinelearning/webmcp) tools
(`document.modelContext`, or the older `navigator.modelContext`), so AI agents running in the browser can build apps with SnapCode the way a user does:
create projects and files, design UIs in the GUI builder, build, run, and inspect the result.

The code is in `src/snapcode/webmcp`. It is only active in the browser (`SnapEnv.isWebVM`), and only registers
tools when the browser provides `document.modelContext` or `navigator.modelContext`. The desktop app is unaffected.

## How it works

- `WebMCP.install()` runs at startup (from `App`). It adds a small inline script that creates
  `window.__snapcodeWebMCP` and registers each tool with `modelContext.registerTool()` (on `document`, else `navigator`)
  (or `provideContext()`).
- When an agent calls a tool, the script dispatches a `change` `CustomEvent` on a private `EventTarget`.
  SnapKit's `EventQueue` delivers it to Java on the event thread, so tools can safely touch the UI.
- `McpToolRegistry` runs the tool. Results use the MCP `CallToolResult` format (`content` with `text` and `image`
  items, plus `isError`). Java sends the result back with `__snapcodeWebMCP.resolve(id, json)`.
- Long-running tools (build, run, open a remote project) complete when the work finishes.
- No new browser files or changes to `index.html` are needed.

For debugging without a WebMCP browser, the bridge also exposes `__snapcodeWebMCP.listTools()` and
`__snapcodeWebMCP.callTool(name, args)` in the browser console.

## Tools

| Area | Tools |
|---|---|
| Guide & API lookup | `snapcode_guide` (marked as a skill), `lookup_class`, `find_classes` |
| Workspace & projects | `get_state`, `list_local_projects`, `create_project`, `open_project`, `close_project`, `list_samples`, `open_sample`, `open_greenfoot_scenario`, `get_share_link` |
| Files & code | `list_files`, `read_file`, `create_file`, `write_file`, `edit_file`, `rename_file`, `delete_file`, `open_file`, `close_file`, `save_file`, `revert_file`, `search_project` (text / references / declarations) |
| Build & run | `build`, `get_build_issues`, `run_app`, `get_console_output`, `send_console_input`, `stop_app`, `screenshot_app` |
| GUI builder (`.snp`) | `builder_open`, `builder_get_tree`, `builder_list_view_types`, `builder_list_properties`, `builder_add_view`, `builder_set_properties`, `builder_remove_view`, `builder_move_view`, `builder_wrap_view`, `builder_select_view`, `builder_undo`, `builder_get_xml`, `builder_set_xml`, `builder_show`, `builder_screenshot`, `builder_save` |
| Build settings | `get_build_settings`, `set_build_settings`, `add_dependency`, `remove_dependency`, `add_maven_repository` |
| Version control | `vcs_status`, `vcs_commit`, `vcs_update`, `vcs_discard_changes`, `save_to_snapcloud` |
| Help & IDE | `help_list_sections`, `help_get_section`, `help_insert_example`, `show_panel` |

Notes:

- `snapcode_guide` returns `src/snapcode/webmcp/AgentGuide.md`: how SnapKit apps are structured, layout,
  events, timers and animation, drawing, and the rules for code that runs in the browser. It is read-only and
  annotated `skill: true`, so agent hosts that support page skills offer it to the user as a skill to approve;
  other agents are pointed to it by the `get_state`, `create_file` and `builder_open` descriptions. Keep the
  guide's claims tested: each browser rule in it was reproduced in SnapCode web.
- `lookup_class` reports a class's members by reflection on the project's own classpath, with parameter
  names and doc comments taken from the source: the project's files, or SnapKit and SnapCharts read from
  GitHub. `find_classes` searches the class tree SnapCode uses for code completion.
- `build`, `get_build_issues` and `run_app` add `browser_warnings` for code patterns known to break in the
  browser (currently: `ScheduledExecutorService`, whose `shutdown()` freezes the tab under CheerpJ).

- Every tool declares `readOnlyHint`, plus `consequentialHint` (the WebMCP draft name) and
  `destructiveHint` (MCP's older name). Only tools that can't be undone or that act outside the IDE are
  consequential: `delete_file`, `revert_file`, `vcs_commit`, `vcs_update`,
  `vcs_discard_changes` and `save_to_snapcloud`. Browsers and agent hosts can then let ordinary editing
  run and confirm only those. An undeclared `destructiveHint` would make every tool look destructive.

- Text edits to a file that is open in an editor go through the editor's text model, so they are undoable
  and the editor stays in sync. `.snp` edits to a layout open in the builder go through the builder.
- GUI builder views are addressed by `Name` or by a child-index path such as `/1/0`. Builder edits are
  recorded by the builder's undoer.
- `screenshot_app` returns the app window for apps that run in their own browser frame (captured in
  JavaScript from the frame's canvas), and otherwise the Run/Console panel.
- Signing in to SnapCloud or GitHub is intentionally left to the user. The debugger has no tools because
  SnapCode doesn't support debugging in the browser.

## Trying it locally

`scripts/run-webmcp-local.sh` builds this checkout for the browser and serves it:

```bash
scripts/run-webmcp-local.sh          # optional argument: port (default 8765)
```

It needs git, curl, python3 and a JDK 21 or newer that still includes the `jdk.jsobject` module. It uses SnapKit, SnapBuilder and Greenfoot from sibling
directories, the layout `build.gradle` expects, and clones any that are missing. It compiles everything
into `build/webmcp-local/`, adds the hosted `index.html` and `launcher.html` from reportmill.com, and
serves the result with HTTP Range support, which CheerpJ needs. Then open
`http://localhost:8765/index.html?loader=none` in a browser that provides `document.modelContext` or `navigator.modelContext`, or try
the tools from the browser console with `__snapcodeWebMCP.callTool(name, args)`.

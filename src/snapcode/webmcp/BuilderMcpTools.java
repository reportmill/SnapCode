package snapcode.webmcp;
import snap.gfx.Image;
import snap.props.Prop;
import snap.props.PropSet;
import snap.props.StringCodec;
import snap.view.*;
import snap.web.WebFile;
import snapbuild.app.Editor;
import snapbuild.app.EditorPane;
import snapbuild.app.ViewHpr;
import snapcode.app.WorkspacePane;
import snapcode.webbrowser.SnapBuilderPage;
import snapcode.webbrowser.WebPage;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;
import java.util.function.Consumer;

/**
 * WebMCP tools for the GUI builder (SnapBuilder), which edits '.snp' SnapKit UI layout files.
 *
 * Views are identified by:
 *   - Name: a view's Name property (unique names recommended), or
 *   - Path: '/' for the root view, '/0/2' for the third child of the first child (by builder child index).
 */
public class BuilderMcpTools {

    // Common descriptions
    private static final String SNP_PATH_DESC = "The .snp file (project-relative path or name). Default: the selected file if it is a .snp file.";
    private static final String VIEW_ID_DESC = "View to act on: its Name, or a path like '/' (root) or '/0/2' (child indexes). Default: the builder's selected view.";

    // The view classes offered by the builder gallery
    private static final Object[][] VIEW_TYPES = {
            { Label.class, "Text and/or image label" },
            { Button.class, "Push button (fires an action event named by its Name)" },
            { ToggleButton.class, "Toggle button (on/off)" },
            { CheckBox.class, "Check box" },
            { RadioButton.class, "Radio button (set Group to group buttons)" },
            { TextField.class, "Single line text input" },
            { ComboBox.class, "Drop down list / editable combo" },
            { Slider.class, "Slider for numeric value" },
            { ThumbWheel.class, "Thumb wheel for numeric value" },
            { ProgressBar.class, "Progress bar" },
            { Spinner.class, "Numeric spinner" },
            { ArrowView.class, "Arrow buttons" },
            { TextView.class, "Multi-line text area with scroll" },
            { TitleView.class, "Titled border container (one child)" },
            { TabView.class, "Tabbed container" },
            { ScrollView.class, "Scrolling container (one child)" },
            { SplitView.class, "Resizable split container" },
            { ListView.class, "List of items" },
            { TableView.class, "Table with columns" },
            { TreeView.class, "Tree" },
            { BrowserView.class, "Column browser" },
            { BoxView.class, "Box container (one child, aligned)" },
            { ColView.class, "Column container: lays out children vertically" },
            { RowView.class, "Row container: lays out children horizontally" },
            { BorderView.class, "Border layout container (top/center/bottom/left/right)" },
            { ImageView.class, "Image" },
            { View.class, "Plain view (placeholder / custom component; set RuntimeClassName for a custom class)" },
    };

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        String builderIntro = "The SnapCode GUI builder edits SnapKit UI layout files ('.snp' XML). A layout 'Foo.snp' next " +
                "to 'Foo.java' (class Foo extends snap.view.ViewOwner) is loaded automatically by Foo's createUI(); " +
                "controls are referenced in code by Name: respondUI(ViewEvent e) { if (e.equals(\"OKButton\")) ... }, " +
                "getViewText(\"NameField\"), setViewValue(...). Layout is container based (ColView, RowView, BoxView, " +
                "etc. with Spacing, Padding, Align, GrowWidth) rather than absolute positioning. See snapcode_guide " +
                "for the full app structure. ";

        reg.addTool(new McpTool("builder_open",
                builderIntro + "Opens a .snp file in the GUI builder (creating it with a starter layout if needed) and " +
                "returns its view tree.",
                new Schema()
                        .str("path", "The .snp file path (e.g. '/src/MyApp.snp').", true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .bool("create", "Create the file if it doesn't exist (default true).", false),
                BuilderMcpTools::builderOpen));

        reg.addTool(new McpTool("builder_get_tree",
                "Returns the view hierarchy of a .snp layout open in the GUI builder: for each view its path, type, " +
                "Name, Text and non-default properties, plus the selected view.",
                new Schema()
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderGetTree)).readOnly();

        reg.addTool(new McpTool("builder_list_view_types",
                "Lists the view types available in the GUI builder gallery (controls and containers).",
                new Schema(), BuilderMcpTools::builderListViewTypes)).readOnly();

        reg.addTool(new McpTool("builder_list_properties",
                "Lists the properties (name, value type, default) of a view type or of a view in the open layout, " +
                "for use with builder_set_properties. Values are strings in SnapKit format, e.g. Font 'Arial Bold 14', " +
                "Fill '#FF8800', Padding '5,10,5,10', Align 'CENTER_LEFT', Border 'LINE #999999 1'.",
                new Schema()
                        .str("view_type", "View type name, e.g. 'Button' (alternatively give 'view').", false)
                        .str("view", VIEW_ID_DESC, false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderListProperties)).readOnly();

        reg.addTool(new McpTool("builder_add_view",
                "Adds a new view to the layout open in the GUI builder and selects it. Give a container 'parent' " +
                "(ColView, RowView, BoxView, ScrollView, TitleView, TabView, SplitView, BorderView) and optional index. " +
                "Set 'name' for controls you will reference from code.",
                new Schema()
                        .str("view_type", "View type, e.g. 'Button', 'TextField', 'RowView' (see builder_list_view_types).", true)
                        .str("parent", "Parent container (Name or path). Default: the root view.", false)
                        .integer("index", "Child index in parent (default: append at end).", false)
                        .str("name", "The view's Name (used to reference it from code).", false)
                        .str("text", "The view's Text (e.g. button/label title).", false)
                        .object("properties", "Other properties to set, as {PropName: value}.", false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderAddView));

        reg.addTool(new McpTool("builder_set_properties",
                "Sets properties of a view in the GUI builder layout, e.g. {\"Text\": \"OK\", \"PrefWidth\": 120, " +
                "\"Font\": \"Arial Bold 16\", \"GrowWidth\": true, \"Spacing\": 8, \"Padding\": \"10\", " +
                "\"Align\": \"CENTER\", \"Fill\": \"#EEEEEE\"}. Use builder_list_properties for valid names.",
                new Schema()
                        .str("view", VIEW_ID_DESC, false)
                        .object("properties", "Properties to set, as {PropName: value}.", true)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderSetProperties));

        reg.addTool(new McpTool("builder_remove_view",
                "Removes a view (and its children) from the GUI builder layout.",
                new Schema()
                        .str("view", VIEW_ID_DESC, true)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderRemoveView));

        reg.addTool(new McpTool("builder_move_view",
                "Moves a view to a new parent container and/or index in the GUI builder layout.",
                new Schema()
                        .str("view", VIEW_ID_DESC, true)
                        .str("parent", "New parent container (Name or path). Default: current parent.", false)
                        .integer("index", "New index in parent (default: end).", false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderMoveView));

        reg.addTool(new McpTool("builder_wrap_view",
                "Wraps a view in a new container (e.g. RowView, ColView, BoxView, ScrollView, TitleView) placed where " +
                "the view was, like the builder's group commands.",
                new Schema()
                        .str("view", VIEW_ID_DESC, false)
                        .str("container_type", "Container type (default 'RowView').", false)
                        .str("container_name", "Name for the new container (optional).", false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderWrapView));

        reg.addTool(new McpTool("builder_select_view",
                "Selects a view in the GUI builder (shows it in the inspector).",
                new Schema()
                        .str("view", VIEW_ID_DESC, true)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderSelectView));

        reg.addTool(new McpTool("builder_undo",
                "Undoes the last GUI builder edit (or redoes with redo=true).",
                new Schema()
                        .bool("redo", "Redo instead of undo (default false).", false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderUndo));

        reg.addTool(new McpTool("builder_get_xml",
                "Returns the current .snp XML of the layout open in the GUI builder (including unsaved edits).",
                new Schema()
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderGetXml)).readOnly();

        reg.addTool(new McpTool("builder_set_xml",
                "Replaces the whole layout with the given .snp XML (fastest way to build a complex UI). Example: " +
                "<ColView PrefWidth=\"300\" Padding=\"10\" Spacing=\"8\" FillWidth=\"true\"><Label Name=\"TitleLabel\" " +
                "Text=\"Hello\" Font=\"Arial Bold 18\" /><RowView Spacing=\"5\"><TextField Name=\"NameField\" " +
                "GrowWidth=\"true\" /><Button Name=\"OKButton\" Text=\"OK\" /></RowView></ColView>",
                new Schema()
                        .str("xml", "The full .snp XML.", true)
                        .bool("save", "Save the file afterwards (default true).", false)
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderSetXml));

        reg.addTool(new McpTool("builder_show",
                "Switches the GUI builder display: 'editor' (design view), 'preview' (live preview of the UI), or " +
                "'xml' (XML source view).",
                new Schema()
                        .enumStr("mode", "Display mode.", true, "editor", "preview", "xml")
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderShow));

        reg.addTool(new McpTool("builder_screenshot",
                "Returns a PNG image of the layout being designed (or of the whole builder with target 'builder').",
                new Schema()
                        .enumStr("target", "What to capture (default 'layout').", false, "layout", "builder")
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderScreenshot)).readOnly();

        reg.addTool(new McpTool("builder_save",
                "Saves the layout open in the GUI builder to its .snp file.",
                new Schema()
                        .str("path", SNP_PATH_DESC, false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                BuilderMcpTools::builderSave));
    }

    /**
     * Returns the EditorPane for a .snp file if it is open in a builder page (or null).
     */
    public static EditorPane getOpenEditorPane(WebFile aFile)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        WebPage page = workspacePane.getPagePane().getBrowser().getPageForURL(aFile.getUrl());
        if (page instanceof SnapBuilderPage builderPage) {
            builderPage.getUI(); // Make sure editor is loaded
            return builderPage.getEditorPane();
        }
        return null;
    }

    /**
     * Gets the .snp file for args, opens it in the builder if needed, then calls handler with editor pane.
     */
    private static void withEditorPane(McpArgs args, McpResult result, Consumer<EditorPane> aHandler)
    {
        WebFile file = McpUtils.getFileOrSelFile(args);
        if (!file.getFileType().equals("snp"))
            throw new McpException(file.getPath() + " is not a .snp UI file. Use builder_open with a .snp path.");

        // If already open, use it
        EditorPane editorPane = getOpenEditorPane(file);
        if (editorPane != null) {
            runHandler(aHandler, editorPane, result);
            return;
        }

        // Open file and wait for builder page
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        workspacePane.openFile(file);
        McpUtils.waitUntil(result, () -> getOpenEditorPane(file) != null, 10000,
                () -> runHandler(aHandler, getOpenEditorPane(file), result),
                () -> result.error("Could not open " + file.getPath() + " in the GUI builder"));
    }

    /**
     * Runs handler, converting exceptions to errors.
     */
    private static void runHandler(Consumer<EditorPane> aHandler, EditorPane editorPane, McpResult result)
    {
        try { aHandler.accept(editorPane); }
        catch (McpException e) { result.error(e.getMessage()); }
        catch (Throwable t) {
            t.printStackTrace();
            result.error("GUI builder error: " + t);
        }
    }

    /**
     * builder_open
     */
    private static void builderOpen(McpArgs args, McpResult result)
    {
        String pathArg = args.requireString("path");
        if (!pathArg.endsWith(".snp"))
            pathArg += ".snp";
        WebFile file = McpUtils.findFile(args.getString("project"), pathArg);

        // Create if missing
        if (file == null) {
            if (!args.getBool("create", true))
                throw new McpException("File not found: " + pathArg);
            Map<String,Object> createArgs = new LinkedHashMap<>();
            createArgs.put("path", pathArg);
            createArgs.put("project", args.getString("project"));
            createArgs.put("open", false);
            List<Map<String,Object>> createResults = new ArrayList<>();
            McpToolRegistry.getShared().callTool("create_file", createArgs, createResults::add);
            if (createResults.isEmpty() || Boolean.TRUE.equals(createResults.get(0).get("isError")))
                throw new McpException("Could not create " + pathArg + ": " + (createResults.isEmpty() ? "" : Json.write(createResults.get(0))));
            file = McpUtils.getFile(args.getString("project"), pathArg);
        }

        Map<String,Object> openArgs = new LinkedHashMap<>(args.getMap());
        openArgs.put("path", file.getPath());
        withEditorPane(new McpArgs(openArgs), result, editorPane -> result.json(getTreeMap(editorPane)));
    }

    /**
     * builder_get_tree
     */
    private static void builderGetTree(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> result.json(getTreeMap(editorPane)));
    }

    /**
     * Returns the tree map for editor.
     */
    private static Map<String,Object> getTreeMap(EditorPane editorPane)
    {
        Editor editor = editorPane.getEditor();
        Map<String,Object> map = new LinkedHashMap<>();
        WebFile file = editor.getSourceFile(false);
        map.put("file", file != null ? WorkspaceMcpTools.getFileRef(file) : null);
        View selView = editor.getSelView();
        map.put("selectedView", selView != null ? getViewPath(editor, selView) : null);
        map.put("root", getViewMap(editor.getContent(), "/"));
        return map;
    }

    /**
     * Returns a map for view (recursive).
     */
    private static Map<String,Object> getViewMap(View aView, String aPath)
    {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("path", aPath);
        map.put("type", aView.getClass().getSimpleName());
        if (aView.getName() != null)
            map.put("Name", aView.getName());
        Map<String,Object> props = getNonDefaultProps(aView);
        props.remove("Name");
        if (!props.isEmpty())
            map.put("props", props);

        if (aView instanceof ViewHost host && host.getGuestCount() > 0) {
            List<Map<String,Object>> children = new ArrayList<>();
            String prefix = aPath.equals("/") ? "/" : aPath + '/';
            for (int i = 0; i < host.getGuestCount(); i++)
                children.add(getViewMap(host.getGuest(i), prefix + i));
            map.put("children", children);
        }
        return map;
    }

    /**
     * Returns the non-default archival props of a view as strings.
     */
    private static Map<String,Object> getNonDefaultProps(View aView)
    {
        Map<String,Object> props = new LinkedHashMap<>();
        PropSet propSet = aView.getPropSet();
        for (Prop prop : propSet.getArchivalProps()) {
            if (prop.isRelation() || prop.isArray() && !prop.getPropClass().getComponentType().isPrimitive() && prop.getPropClass() != String[].class)
                continue;
            String propName = prop.getName();
            try {
                if (aView.isPropDefault(propName))
                    continue;
                Object value = aView.getPropValue(propName);
                String str = value != null ? StringCodec.SHARED.codeString(value) : null;
                if (str != null)
                    props.put(propName, str);
            }
            catch (Throwable ignore) { }
        }
        return props;
    }

    /**
     * Returns the path for a view (e.g. "/0/2"), found by searching guests from the content root.
     */
    private static String getViewPath(Editor editor, View aView)
    {
        String path = findViewPath(editor.getContent(), aView, "");
        return path == null || path.isEmpty() ? "/" : path;
    }

    /**
     * Returns path of view relative to given host view (or null if not found).
     */
    private static String findViewPath(View aRoot, View aView, String aPrefix)
    {
        if (aRoot == aView)
            return aPrefix;
        if (aRoot instanceof ViewHost host) {
            for (int i = 0; i < host.getGuestCount(); i++) {
                String path = findViewPath(host.getGuest(i), aView, aPrefix + '/' + i);
                if (path != null)
                    return path;
            }
        }
        return null;
    }

    /**
     * Returns the container (ViewHost) that holds the given view as a guest, found by searching guests from the
     * root, the same relationship view paths use. Not view.getParent(): some containers hold their guests inside
     * internal views (a TabView tab's content is in an internal box, or has no parent when the tab isn't selected).
     */
    private static ViewHost getGuestHost(Editor editor, View aView)
    {
        ViewHost host = findGuestHost(editor.getContent(), aView);
        if (host == null)
            throw new McpException("Can't find the container of this view");
        return host;
    }

    private static ViewHost findGuestHost(View aRoot, View aView)
    {
        if (!(aRoot instanceof ViewHost host))
            return null;
        for (int i = 0; i < host.getGuestCount(); i++) {
            View guest = host.getGuest(i);
            if (guest == aView)
                return host;
            ViewHost match = findGuestHost(guest, aView);
            if (match != null)
                return match;
        }
        return null;
    }

    /**
     * Returns the view for given identifier (Name or path), or the selected view if null.
     */
    private static View getView(Editor editor, String anId)
    {
        if (anId == null || anId.isEmpty())
            return editor.getSelView();
        View content = editor.getContent();
        String id = anId.trim();

        // Handle path
        if (id.startsWith("/")) {
            View view = content;
            for (String part : id.substring(1).split("/")) {
                if (part.isEmpty()) continue;
                if (!(view instanceof ViewHost host))
                    throw new McpException("View path '" + id + "' is invalid: " + view.getClass().getSimpleName() + " has no children");
                int index;
                try { index = Integer.parseInt(part); }
                catch (NumberFormatException e) { throw new McpException("Invalid view path: " + id); }
                if (index < 0 || index >= host.getGuestCount())
                    throw new McpException("View path '" + id + "' is invalid: index " + index + " out of range");
                view = host.getGuest(index);
            }
            return view;
        }

        // Handle name
        View view = findViewForName(content, id);
        if (view == null)
            throw new McpException("No view named '" + id + "' in the layout. Use builder_get_tree to see views.");
        return view;
    }

    /**
     * Finds a view for name (searching guests).
     */
    private static View findViewForName(View aView, String aName)
    {
        if (aName.equals(aView.getName()))
            return aView;
        if (aView instanceof ViewHost host) {
            for (int i = 0; i < host.getGuestCount(); i++) {
                View match = findViewForName(host.getGuest(i), aName);
                if (match != null)
                    return match;
            }
        }
        return null;
    }

    /**
     * Returns the view class for a type name.
     */
    private static Class<? extends View> getViewClass(String aType)
    {
        String type = aType.trim();
        for (Object[] viewType : VIEW_TYPES) {
            Class<?> cls = (Class<?>) viewType[0];
            if (cls.getSimpleName().equalsIgnoreCase(type))
                return cls.asSubclass(View.class);
        }
        String[] prefixes = type.contains(".") ? new String[] { "" } : new String[] { "snap.view.", "snap.viewx." };
        for (String prefix : prefixes) {
            try {
                Class<?> cls = Class.forName(prefix + type);
                if (View.class.isAssignableFrom(cls))
                    return cls.asSubclass(View.class);
            }
            catch (Throwable ignore) { }
        }
        throw new McpException("Unknown view type: " + aType + ". See builder_list_view_types.");
    }

    /**
     * Creates a new configured view for type name.
     */
    @SuppressWarnings("unchecked")
    private static View createView(String aType)
    {
        Class<? extends View> viewClass = getViewClass(aType);
        View view;
        try { view = viewClass.getDeclaredConstructor().newInstance(); }
        catch (Exception e) { throw new McpException("Can't create view of type " + aType + ": " + e); }
        ViewHpr<View> hpr = ViewHpr.getHpr(view);
        if (hpr != null)
            hpr.configure(view);
        return view;
    }

    /**
     * builder_list_view_types
     */
    private static void builderListViewTypes(McpArgs args, McpResult result)
    {
        List<Map<String,Object>> list = new ArrayList<>();
        for (Object[] viewType : VIEW_TYPES) {
            Class<?> cls = (Class<?>) viewType[0];
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("type", cls.getSimpleName());
            map.put("container", ViewHost.class.isAssignableFrom(cls));
            map.put("description", viewType[1]);
            list.add(map);
        }
        result.json(list);
    }

    /**
     * builder_list_properties
     */
    private static void builderListProperties(McpArgs args, McpResult result)
    {
        String type = args.getString("view_type");
        if (type != null && !type.isEmpty()) {
            result.json(getPropsList(createView(type)));
            return;
        }
        withEditorPane(args, result, editorPane -> {
            View view = getView(editorPane.getEditor(), args.getString("view"));
            result.json(getPropsList(view));
        });
    }

    /**
     * Returns list of prop descriptions.
     */
    private static Map<String,Object> getPropsList(View aView)
    {
        List<Map<String,Object>> list = new ArrayList<>();
        for (Prop prop : aView.getPropSet().getArchivalProps()) {
            if (prop.isRelation())
                continue;
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("name", prop.getName());
            Class<?> propClass = prop.getPropClass();
            map.put("type", propClass != null ? propClass.getSimpleName() : "Object");
            if (propClass != null && propClass.isEnum())
                map.put("values", Arrays.stream(propClass.getEnumConstants()).map(Object::toString).toList());
            try {
                Object value = aView.getPropValue(prop.getName());
                if (value != null)
                    map.put("value", StringCodec.SHARED.codeString(value));
            }
            catch (Throwable ignore) { }
            list.add(map);
        }
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("type", aView.getClass().getSimpleName());
        map.put("properties", list);
        return map;
    }

    /**
     * Sets the given props on view. Returns list of errors (empty if none).
     */
    private static List<String> setViewProps(View aView, Map<String,Object> theProps)
    {
        List<String> errors = new ArrayList<>();
        if (theProps == null)
            return errors;
        PropSet propSet = aView.getPropSet();
        for (Map.Entry<String,Object> entry : theProps.entrySet()) {
            String propName = entry.getKey();
            Object value = entry.getValue();
            Prop prop = propSet.getPropForName(propName);
            if (prop == null) {
                errors.add("Unknown property '" + propName + "' for " + aView.getClass().getSimpleName());
                continue;
            }
            try {
                Object propValue = value;
                Class<?> propClass = prop.getPropClass();
                if (value instanceof String str && propClass != null && propClass != String.class && propClass != Object.class) {
                    propValue = StringCodec.SHARED.decodeString(str, propClass);
                    if (propValue == null && !str.equals("null")) {
                        errors.add("Invalid value for " + propName + ": " + str);
                        continue;
                    }
                }
                else if (value instanceof Number && (propClass == int.class || propClass == Integer.class))
                    propValue = ((Number) value).intValue();
                else if (value instanceof Number && (propClass == double.class || propClass == Double.class))
                    propValue = ((Number) value).doubleValue();
                else if (value != null && propClass != null && !propClass.isPrimitive() && !propClass.isInstance(value) &&
                        !(value instanceof Number || value instanceof Boolean))
                    propValue = StringCodec.SHARED.decodeString(value.toString(), propClass);
                else if ((value instanceof Number || value instanceof Boolean) && propClass != null && !propClass.isPrimitive() &&
                        !propClass.isInstance(value) && propClass != Object.class)
                    propValue = StringCodec.SHARED.decodeString(value.toString(), propClass);
                aView.setPropValue(propName, propValue);
            }
            catch (Throwable t) {
                errors.add("Failed to set " + propName + "=" + value + ": " + t.getMessage());
            }
        }
        return errors;
    }

    /**
     * Finishes an edit: repaints, marks file modified and returns summary.
     */
    private static void finishEdit(EditorPane editorPane, View aView, List<String> errors, McpResult result)
    {
        Editor editor = editorPane.getEditor();
        if (aView != null && aView.getParent() != null)
            editor.setSelView(aView);
        editor.repaint();
        editorPane.resetLater();

        Map<String,Object> map = new LinkedHashMap<>();
        if (aView != null) {
            map.put("view", getViewPath(editor, aView));
            map.put("type", aView.getClass().getSimpleName());
            if (aView.getName() != null)
                map.put("Name", aView.getName());
        }
        if (!errors.isEmpty())
            map.put("warnings", errors);
        map.put("note", "Unsaved: call builder_save (or save_file) to save the layout.");
        result.json(map);
    }

    /**
     * builder_add_view
     */
    private static void builderAddView(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            String parentId = args.getString("parent");
            View parent = parentId != null && !parentId.isEmpty() ? getView(editor, parentId) : editor.getContent();
            if (!(parent instanceof ViewHost host))
                throw new McpException(parent.getClass().getSimpleName() + " can't hold children. Use a container like ColView or RowView.");

            View view = createView(args.requireString("view_type"));
            String name = args.getString("name");
            if (name != null && !name.isEmpty()) {
                if (findViewForName(editor.getContent(), name) != null)
                    throw new McpException("A view named '" + name + "' already exists");
                view.setName(name);
            }
            String text = args.getString("text");
            if (text != null)
                view.setText(text);
            List<String> errors = setViewProps(view, args.getObject("properties"));

            int index = args.getInt("index", host.getGuestCount());
            index = Math.max(0, Math.min(index, host.getGuestCount()));
            host.addGuest(view, index);
            finishEdit(editorPane, view, errors, result);
        });
    }

    /**
     * builder_set_properties
     */
    private static void builderSetProperties(McpArgs args, McpResult result)
    {
        Map<String,Object> props = args.getObject("properties");
        if (props == null || props.isEmpty())
            throw new McpException("properties is required");
        withEditorPane(args, result, editorPane -> {
            View view = getView(editorPane.getEditor(), args.getString("view"));
            List<String> errors = setViewProps(view, props);
            if (errors.size() == props.size())
                throw new McpException(String.join("; ", errors));
            finishEdit(editorPane, view, errors, result);
        });
    }

    /**
     * builder_remove_view
     */
    private static void builderRemoveView(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            View view = getView(editor, args.requireString("view"));
            if (view == editor.getContent())
                throw new McpException("Can't remove the root view (use builder_set_xml to replace the layout)");
            ViewHost host = getGuestHost(editor, view);
            String path = getViewPath(editor, view);
            host.removeGuest(ViewHost.indexOfGuest(host, view));
            editor.setSelView((View) host);
            editor.repaint();
            result.json(Map.of("removed", path, "note", "Unsaved: call builder_save to save the layout."));
        });
    }

    /**
     * builder_move_view
     */
    private static void builderMoveView(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            View view = getView(editor, args.requireString("view"));
            if (view == editor.getContent())
                throw new McpException("Can't move the root view");
            ViewHost oldHost = getGuestHost(editor, view);
            String parentId = args.getString("parent");
            View newParent = parentId != null && !parentId.isEmpty() ? getView(editor, parentId) : (View) oldHost;
            if (!(newParent instanceof ViewHost newHost))
                throw new McpException("New parent must be a container");
            if (newParent == view || findViewPath(view, newParent, "") != null)
                throw new McpException("Can't move a view into itself");

            // Remove and re-add
            int oldIndex = ViewHost.indexOfGuest(oldHost, view);
            oldHost.removeGuest(oldIndex);
            int index = args.getInt("index", newHost.getGuestCount());
            index = Math.max(0, Math.min(index, newHost.getGuestCount()));
            try { newHost.addGuest(view, index); }
            catch (Throwable t) {
                oldHost.addGuest(view, oldIndex);
                throw new McpException("Move failed: " + t.getMessage());
            }
            finishEdit(editorPane, view, List.of(), result);
        });
    }

    /**
     * builder_wrap_view
     */
    private static void builderWrapView(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            View view = getView(editor, args.getString("view"));
            if (view == editor.getContent())
                throw new McpException("Can't wrap the root view");
            ViewHost host = getGuestHost(editor, view);
            View container = createView(args.getString("container_type", "RowView"));
            if (!(container instanceof ViewHost containerHost))
                throw new McpException("container_type must be a container view");
            String containerName = args.getString("container_name");
            if (containerName != null && !containerName.isEmpty())
                container.setName(containerName);

            int index = ViewHost.indexOfGuest(host, view);
            host.removeGuest(index);
            containerHost.addGuest(view);
            host.addGuest(container, index);
            finishEdit(editorPane, container, List.of(), result);
        });
    }

    /**
     * builder_select_view
     */
    private static void builderSelectView(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            View view = getView(editor, args.requireString("view"));
            editor.setSelView(view);
            Map<String,Object> map = getViewMap(view, getViewPath(editor, view));
            map.remove("children");
            result.json(map);
        });
    }

    /**
     * builder_undo
     */
    private static void builderUndo(McpArgs args, McpResult result)
    {
        boolean redo = args.getBool("redo", false);
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            boolean available = redo ? editor.getUndoer().getLastRedoSet() != null : editor.getUndoer().getLastUndoSet() != null;
            if (!available)
                throw new McpException("Nothing to " + (redo ? "redo" : "undo"));
            if (redo) editor.redo();
            else editor.undo();
            result.json(getTreeMap(editorPane));
        });
    }

    /**
     * Sets the builder XML for an open .snp file (or file text if not open).
     */
    public static void setBuilderXml(WebFile aFile, String anXml, boolean doSave)
    {
        EditorPane editorPane = getOpenEditorPane(aFile);
        if (editorPane == null) {
            McpUtils.setFileText(aFile, anXml, doSave);
            return;
        }

        // Parse XML into views (as the builder does: don't instantiate custom classes)
        View newContent;
        boolean useRealClass = UILoader.isUseRealClassDefault();
        UILoader.setUseRealClassDefault(false);
        try { newContent = UILoader.loadViewForBytes(anXml.getBytes()); }
        catch (Throwable t) { throw new McpException("Invalid .snp XML: " + t.getMessage()); }
        finally { UILoader.setUseRealClassDefault(useRealClass); }
        if (!(newContent instanceof ParentView))
            throw new McpException("The root element of a .snp layout must be a container view (e.g. ColView)");

        // Set content and update file
        Editor editor = editorPane.getEditor();
        editor.setContent(newContent);
        editor.setSelView(newContent);
        editor.updateFile();
        if (doSave) {
            try { editor.save(); }
            catch (Exception e) { throw new McpException("Save failed: " + e.getMessage()); }
        }
    }

    /**
     * builder_get_xml
     */
    private static void builderGetXml(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> result.text(editorPane.getEditor().getContentXML().getString()));
    }

    /**
     * builder_set_xml
     */
    private static void builderSetXml(McpArgs args, McpResult result)
    {
        String xml = args.requireString("xml");
        boolean save = args.getBool("save", true);
        withEditorPane(args, result, editorPane -> {
            WebFile file = editorPane.getEditor().getSourceFile(true);
            setBuilderXml(file, xml, save);
            result.json(getTreeMap(editorPane));
        });
    }

    /**
     * builder_show
     */
    private static void builderShow(McpArgs args, McpResult result)
    {
        String mode = args.requireString("mode");
        withEditorPane(args, result, editorPane -> {
            switch (mode) {
                case "editor" -> editorPane.showEditor();
                case "preview" -> editorPane.showPreview();
                case "xml" -> editorPane.showXMLEditor();
                default -> throw new McpException("mode must be editor, preview or xml");
            }
            // Let the transition animation finish so screenshots show the new mode
            ViewUtils.runDelayed(() -> result.json(Map.of("mode", mode)), 700);
        });
    }

    /**
     * builder_screenshot
     */
    private static void builderScreenshot(McpArgs args, McpResult result)
    {
        String target = args.getString("target", "layout");
        withEditorPane(args, result, editorPane -> {
            View view = target.equals("builder") ? editorPane.getUI() : editorPane.getEditor().getContent();
            if (view.getWidth() <= 0 || view.getHeight() <= 0)
                throw new McpException("The builder is not visible; open the file with builder_open first");
            Image image = ViewUtils.getImageForDpiScale(view, 1);
            result.addImage(image.getBytesPNG());
            result.done();
        });
    }

    /**
     * builder_save
     */
    private static void builderSave(McpArgs args, McpResult result)
    {
        withEditorPane(args, result, editorPane -> {
            Editor editor = editorPane.getEditor();
            try { editor.save(); }
            catch (Exception e) { throw new McpException("Save failed: " + e.getMessage()); }
            WebFile file = editor.getSourceFile(false);
            result.json(Map.of("saved", file != null ? WorkspaceMcpTools.getFileRef(file) : "?"));
        });
    }
}

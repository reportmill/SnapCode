package snapcode.webmcp;
import javakit.resolver.ClassTree;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snapcode.webmcp.McpTool.Schema;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebMCP tools that teach an agent how to build SnapKit apps in SnapCode: the app-building guide (offered to
 * agent hosts as a skill) and API lookup grounded in the classes the project actually runs with.
 */
public class GuideMcpTools {

    // Where SnapKit / SnapCharts sources are read from for doc comments
    private static final String SNAPKIT_SRC = "https://raw.githubusercontent.com/reportmill/SnapKit/master/src/";
    private static final String SNAPCHARTS_SRC = "https://raw.githubusercontent.com/reportmill/SnapCharts/master/src/";

    // Hosted javadoc roots (for people to follow)
    private static final String SNAPKIT_JAVADOC = "https://reportmill.com/snap1/javadoc/";
    private static final String JDK_JAVADOC = "https://docs.oracle.com/en/java/javase/21/docs/api/";

    // Fetched source text by URL (null value means the fetch failed)
    private static final Map<String,String> _sourceCache = new HashMap<>();

    // The class index of each project: its class tree and class names, for the classpath they were built from
    private record ClassIndex(String classPath, ClassTree tree, Map<Boolean,List<String>> names) { }
    private static final Map<Project,ClassIndex> _classIndexes = new WeakHashMap<>();

    // Workspaces whose builds clear the class indexes (a build can add or remove project classes)
    private static final Set<Workspace> _watchedWorkspaces = Collections.newSetFromMap(new WeakHashMap<>());

    // Packages that belong to the JDK (searched only with include_jdk)
    private static final Set<String> JDK_PACKAGES = Set.of("java", "javax", "jdk", "sun", "com.sun", "netscape");

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("snapcode_guide",
                "How to build apps in SnapCode with SnapKit: file types, the ViewOwner + .snp app structure, layout, " +
                "events, timers and animation, custom drawing, and rules for code that runs in the browser (for " +
                "example: use snap.view.ViewTimer, never ScheduledExecutorService, which freezes the tab when stopped). " +
                "Read this before writing SnapCode app code.",
                new Schema(), GuideMcpTools::guide)).readOnly().skill().title("Building apps in SnapCode");

        reg.addTool(new McpTool("lookup_class",
                "Returns a Java class's real API as SnapCode's project sees it: superclasses, constructors, methods and " +
                "fields with signatures, plus doc comments where the source is available (SnapKit, SnapCharts and the " +
                "project's own classes). Use it instead of guessing method names. Accepts a simple name " +
                "('ViewTimer'), a qualified name ('snap.view.ViewTimer') or a JDK class ('java.util.List').",
                new Schema()
                        .str("name", "Class name, simple or fully qualified.", true)
                        .str("member", "Only show members whose name contains this text (optional).", false)
                        .bool("include_inherited", "Also list inherited public methods by superclass (default false).", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                GuideMcpTools::lookupClass)).readOnly();

        reg.addTool(new McpTool("find_classes",
                "Searches class names available to the project: SnapKit (snap.*), SnapCharts when enabled, and the " +
                "project's own classes; with include_jdk also the JDK. Matches any part of the simple or qualified " +
                "name, case-insensitively, e.g. 'timer', 'anim', 'snap.gfx.'.",
                new Schema()
                        .str("query", "Text to find in class names.", true)
                        .bool("include_jdk", "Also search JDK classes (default false).", false)
                        .integer("max_results", "Maximum results (default 50).", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                GuideMcpTools::findClasses)).readOnly();
    }

    /**
     * snapcode_guide
     */
    private static void guide(McpArgs args, McpResult result)
    {
        WebURL guideUrl = WebURL.getResourceUrl(GuideMcpTools.class, "AgentGuide.md");
        String text = guideUrl != null ? guideUrl.getText() : null;
        if (text == null)
            throw new McpException("The SnapCode guide is not available in this build");
        result.text(text);
    }

    /**
     * Returns the class index for a project, rebuilding it when the project's classpath has changed.
     */
    private static ClassIndex getClassIndex(Project aProj)
    {
        Workspace workspace = aProj.getWorkspace();
        if (workspace != null && _watchedWorkspaces.add(workspace))
            workspace.addPropChangeListener(pc -> { if (!workspace.isBuilding()) _classIndexes.clear(); }, Workspace.Building_Prop);

        String classPath = String.join("\n", aProj.getRuntimeClassPaths());
        ClassIndex index = _classIndexes.get(aProj);
        if (index == null || !index.classPath().equals(classPath)) {
            index = new ClassIndex(classPath, ClassTree.getClassTreeForClassPaths(aProj.getRuntimeClassPaths()), new HashMap<>());
            _classIndexes.put(aProj, index);
        }
        return index;
    }

    /**
     * Adds the class names under given package (recursively), skipping JDK packages unless asked.
     */
    private static void findClassNames(ClassTree aTree, String aPackage, boolean includeJdk, List<String> classNames)
    {
        for (ClassTree.ClassTreeNode node : aTree.getClassTreeNodesForPackageName(aPackage)) {
            String name = node.fullName();
            if (node.isPackage()) {
                if (!includeJdk && JDK_PACKAGES.contains(name))
                    continue;
                findClassNames(aTree, name, includeJdk, classNames);
            }
            else if (name.contains(".") || !aPackage.isEmpty() || !Character.isLowerCase(name.charAt(0)))
                classNames.add(name);
        }
    }

    /**
     * Returns the sorted class names for a project (cached in its class index).
     */
    private static List<String> getClassNames(Project aProj, boolean includeJdk)
    {
        ClassIndex index = getClassIndex(aProj);
        return index.names().computeIfAbsent(includeJdk, jdk -> {
            List<String> names = new ArrayList<>();
            findClassNames(index.tree(), "", jdk, names);
            return List.copyOf(new TreeSet<>(names));   // sorted, and without duplicates when a path is listed twice
        });
    }

    /**
     * find_classes
     */
    private static void findClasses(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        String query = args.requireString("query").trim().toLowerCase();
        if (query.isEmpty())
            throw new McpException("query must not be empty");
        int max = args.getInt("max_results", 50);
        List<String> matches = new ArrayList<>();
        int total = 0;
        for (String name : getClassNames(proj, args.getBool("include_jdk", false))) {
            if (name.toLowerCase().contains(query)) {
                total++;
                if (matches.size() < max)
                    matches.add(name);
            }
        }
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("matches", matches);
        if (total > matches.size())
            map.put("more", total - matches.size());
        if (matches.isEmpty())
            map.put("note", "No match. Try a shorter query" + (args.getBool("include_jdk", false) ? "." : ", or include_jdk: true."));
        result.json(map);
    }

    /**
     * Resolves a class name (simple or qualified) for a project.
     */
    private static Class<?> resolveClass(Project aProj, String aName)
    {
        String name = aName.trim();
        if (name.endsWith(".java"))
            name = name.substring(0, name.length() - 5);

        // Qualified name
        if (name.contains(".")) {
            Class<?> cls = aProj.getResolver().getClassForName(name);
            if (cls == null)
                throw new McpException("Class not found: " + name + ". Try find_classes.");
            return cls;
        }

        // Simple name: look for unique match (prefer snap.*, then project classes, then java.lang/java.util)
        List<String> candidates = new ArrayList<>();
        for (String className : getClassNames(aProj, false))
            if (className.equals(name) || className.endsWith("." + name))
                candidates.add(className);
        for (String pkg : List.of("java.lang.", "java.util.", "java.io.", "java.util.function."))
            if (aProj.getResolver().getClassForName(pkg + name) != null)
                candidates.add(pkg + name);
        if (candidates.isEmpty())
            throw new McpException("No class named '" + name + "'. Try find_classes with part of the name.");
        if (candidates.size() > 1) {
            String snapMatch = findUnique(candidates, c -> c.startsWith("snap.view.") || c.startsWith("snap.gfx."));
            if (snapMatch == null)
                throw new McpException("'" + name + "' is ambiguous: " + candidates + ". Use a qualified name.");
            name = snapMatch;
        }
        else name = candidates.get(0);
        Class<?> cls = aProj.getResolver().getClassForName(name);
        if (cls == null)
            throw new McpException("Class not found: " + name);
        return cls;
    }

    /**
     * Returns the only element matching the predicate, or null if none or several match.
     */
    private static String findUnique(List<String> aList, Predicate<String> aPred)
    {
        List<String> matches = aList.stream().filter(aPred).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * lookup_class
     */
    private static void lookupClass(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        Class<?> cls = resolveClass(proj, args.requireString("name"));
        String member = args.getString("member");
        boolean inherited = args.getBool("include_inherited", false);

        // Project source is local; SnapKit/SnapCharts source is fetched in the background
        String sourceUrl = getSourceUrl(cls);
        WebFile projectSource = getProjectSourceFile(proj, cls);
        if (projectSource != null) {
            result.text(describeClass(cls, McpUtils.getFileText(projectSource), "the project's source", member, inherited));
            return;
        }
        if (sourceUrl == null || _sourceCache.containsKey(sourceUrl)) {
            String src = sourceUrl != null ? _sourceCache.get(sourceUrl) : null;
            result.text(describeClass(cls, src, src != null ? sourceUrl : null, member, inherited));
            return;
        }
        new Thread(() -> {
            String src = null;
            try {
                WebURL url = WebURL.getUrl(sourceUrl);
                src = url != null ? url.getText() : null;
            }
            catch (Throwable ignore) { }
            String finalSrc = src;
            ViewUtils.runLater(() -> {
                _sourceCache.put(sourceUrl, finalSrc);
                try { result.text(describeClass(cls, finalSrc, finalSrc != null ? sourceUrl : null, member, inherited)); }
                catch (Throwable t) { result.error("lookup_class failed: " + t); }
            });
        }).start();
    }

    /**
     * Returns the URL of the source of a SnapKit/SnapCharts class, or null.
     */
    private static String getSourceUrl(Class<?> aClass)
    {
        Class<?> topClass = aClass;
        while (topClass.getEnclosingClass() != null)
            topClass = topClass.getEnclosingClass();
        String path = topClass.getName().replace('.', '/') + ".java";
        if (path.startsWith("snap/"))
            return SNAPKIT_SRC + path;
        if (path.startsWith("snapcharts/"))
            return SNAPCHARTS_SRC + path;
        return null;
    }

    /**
     * Returns the project source file for a class (if it is one of the project's classes).
     */
    private static WebFile getProjectSourceFile(Project aProj, Class<?> aClass)
    {
        Class<?> topClass = aClass;
        while (topClass.getEnclosingClass() != null)
            topClass = topClass.getEnclosingClass();
        try { return aProj.getJavaFileForClassName(topClass.getName()); }
        catch (Throwable t) { return null; }
    }

    /**
     * Returns the javadoc URL for a class.
     */
    private static String getJavadocUrl(Class<?> aClass)
    {
        String name = aClass.getName();
        String path = name.replace('.', '/').replace('$', '.') + ".html";
        if (name.startsWith("snap."))
            return SNAPKIT_JAVADOC + path;
        if (name.startsWith("java.") || name.startsWith("javax."))
            return JDK_JAVADOC + (aClass.getModule() != null && aClass.getModule().getName() != null ? aClass.getModule().getName() + "/" : "") + path;
        return null;
    }

    /**
     * Describes a class as compact text.
     */
    static String describeClass(Class<?> aClass, String aSource, String aSourceName, String aMemberFilter, boolean inherited)
    {
        SourceDocs docs = aSource != null ? new SourceDocs(aSource, aClass.getSimpleName()) : null;
        String filter = aMemberFilter != null && !aMemberFilter.isBlank() ? aMemberFilter.toLowerCase() : null;
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(classKind(aClass)).append(' ').append(aClass.getName());
        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null && superClass != Object.class)
            sb.append(" extends ").append(simpleTypes(superClass.getName()));
        Class<?>[] interfaces = aClass.getInterfaces();
        if (interfaces.length > 0)
            sb.append(aClass.isInterface() ? " extends " : " implements ")
                    .append(String.join(", ", Arrays.stream(interfaces).map(i -> simpleTypes(i.getName())).toList()));
        sb.append('\n');
        if (docs != null && docs._classDoc != null)
            sb.append(docs._classDoc).append('\n');
        List<String> chain = new ArrayList<>();
        for (Class<?> c = superClass; c != null && c != Object.class; c = c.getSuperclass())
            chain.add(c.getName());
        if (!chain.isEmpty())
            sb.append("Superclasses: ").append(String.join(" > ", chain)).append(" (use lookup_class on them for their members)\n");
        String javadoc = getJavadocUrl(aClass);
        if (javadoc != null)
            sb.append("Javadoc: ").append(javadoc).append('\n');
        sb.append(aSourceName != null ? "Docs from: " + aSourceName + "\n" : "Docs: not available (signatures only)\n");

        // Constructors
        List<String> lines = new ArrayList<>();
        if (filter == null) {
            for (Constructor<?> ctor : aClass.getDeclaredConstructors()) {
                if (!isVisible(ctor.getModifiers()) || ctor.isSynthetic()) continue;
                String key = "<init>/" + ctor.getParameterCount();
                lines.add(memberLine(docs, key, ctor.getParameterTypes(), modifiers(ctor.getModifiers()) + aClass.getSimpleName() + params(ctor.getGenericParameterTypes(), ctor.isVarArgs())));
            }
        }
        appendSection(sb, "Constructors", lines);

        // Methods
        lines = new ArrayList<>();
        Method[] methods = aClass.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount));
        for (Method method : methods) {
            if (!isVisible(method.getModifiers()) || method.isSynthetic() || method.isBridge()) continue;
            if (filter != null && !method.getName().toLowerCase().contains(filter)) continue;
            String key = method.getName() + "/" + method.getParameterCount();
            String sig = modifiers(method.getModifiers()) + simpleTypes(method.getGenericReturnType().getTypeName()) + ' ' +
                    method.getName() + params(method.getGenericParameterTypes(), method.isVarArgs());
            lines.add(memberLine(docs, key, method.getParameterTypes(), sig));
        }
        appendSection(sb, "Methods", lines);

        // Fields
        lines = new ArrayList<>();
        for (Field field : aClass.getDeclaredFields()) {
            if (!isVisible(field.getModifiers()) || field.isSynthetic()) continue;
            if (filter != null && !field.getName().toLowerCase().contains(filter)) continue;
            String sig = modifiers(field.getModifiers()) + simpleTypes(field.getGenericType().getTypeName()) + ' ' + field.getName();
            lines.add(memberLine(docs, field.getName(), null, sig));
        }
        appendSection(sb, "Fields", lines);

        // Nested types
        lines = new ArrayList<>();
        for (Class<?> nested : aClass.getDeclaredClasses())
            if (Modifier.isPublic(nested.getModifiers()))
                lines.add("  " + classKind(nested) + ' ' + aClass.getSimpleName() + '.' + nested.getSimpleName());
        appendSection(sb, "Nested types", lines);

        // Inherited methods
        if (inherited) {
            for (Class<?> c = superClass; c != null && c != Object.class; c = c.getSuperclass()) {
                TreeSet<String> names = new TreeSet<>();
                for (Method method : c.getDeclaredMethods())
                    if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()
                            && (filter == null || method.getName().toLowerCase().contains(filter)))
                        names.add(method.getName());
                if (!names.isEmpty())
                    sb.append("\nInherited from ").append(c.getName()).append(": ").append(String.join(", ", names)).append('\n');
            }
        }

        // Truncate very long output
        String text = sb.toString();
        if (text.length() > 24000)
            text = text.substring(0, 24000) + "\n...[truncated: use 'member' to filter]";
        return text;
    }

    private static void appendSection(StringBuilder sb, String aTitle, List<String> theLines)
    {
        if (theLines.isEmpty()) return;
        sb.append('\n').append(aTitle).append(":\n");
        theLines.forEach(line -> sb.append(line).append('\n'));
    }

    /**
     * Returns a member line: the source declaration (with parameter names) if one matches, else the reflected
     * signature, followed by its doc comment.
     */
    private static String memberLine(SourceDocs docs, String aKey, Class<?>[] theParamTypes, String aSig)
    {
        SourceDocs.Doc doc = docs != null ? docs.getDoc(aKey, theParamTypes) : null;
        String line = "  " + (doc != null && doc.decl() != null ? doc.decl() : aSig);
        if (doc != null && doc.text() != null && !doc.text().isEmpty())
            line += "\n      " + doc.text();
        return line;
    }

    private static boolean isVisible(int mods)  { return Modifier.isPublic(mods) || Modifier.isProtected(mods); }

    private static String modifiers(int mods)
    {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(mods)) sb.append("public ");
        if (Modifier.isProtected(mods)) sb.append("protected ");
        if (Modifier.isStatic(mods)) sb.append("static ");
        if (Modifier.isAbstract(mods)) sb.append("abstract ");
        if (Modifier.isFinal(mods)) sb.append("final ");
        return sb.toString();
    }

    private static String params(Type[] theTypes, boolean isVarArgs)
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < theTypes.length; i++) {
            String name = simpleTypes(theTypes[i].getTypeName());
            if (isVarArgs && i == theTypes.length - 1 && name.endsWith("[]"))
                name = name.substring(0, name.length() - 2) + "...";
            names.add(name);
        }
        return "(" + String.join(", ", names) + ")";
    }

    private static String classKind(Class<?> aClass)
    {
        if (aClass.isAnnotation()) return "@interface";
        if (aClass.isInterface()) return "interface";
        if (aClass.isEnum()) return "enum";
        if (aClass.isRecord()) return "record";
        return Modifier.isAbstract(aClass.getModifiers()) ? "abstract class" : "class";
    }

    /** Shortens qualified type names (snap.view.View -> View, java.util.List<java.lang.String> -> List<String>). */
    static String simpleTypes(String aTypeName)
    {
        return aTypeName.replaceAll("\\b(?:[a-z_][a-z0-9_]*\\.)+([A-Z])", "$1").replace('$', '.');
    }

    /**
     * Doc comments and declarations read from a Java source file.
     */
    static class SourceDocs {

        // A declaration and its doc
        record Doc(String decl, String text, List<String> paramTypes) { }

        // The class doc
        String _classDoc;

        // Docs by key: "name/paramCount" for methods, "<init>/paramCount" for constructors, "name" for fields
        Map<String,List<Doc>> _docs = new HashMap<>();

        // Pattern for a doc comment followed by annotations and a declaration
        private static final Pattern DOC_DECL = Pattern.compile(
                "/\\*\\*(.*?)\\*/\\s*((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)([^;{=/]+?)\\s*[;{=]", Pattern.DOTALL);

        SourceDocs(String aSource, String aSimpleName)
        {
            Matcher matcher = DOC_DECL.matcher(aSource);
            while (matcher.find()) {
                String doc = cleanDoc(matcher.group(1));
                String decl = matcher.group(3).replaceAll("\\s+", " ").trim();

                // Class doc
                if (_classDoc == null && decl.matches(".*\\b(class|interface|enum|record)\\s+" + Pattern.quote(aSimpleName) + "\\b.*")) {
                    _classDoc = doc;
                    continue;
                }
                if (!decl.startsWith("public") && !decl.startsWith("protected"))
                    continue;

                // Executable
                int paren = decl.indexOf('(');
                if (paren > 0) {
                    String before = decl.substring(0, paren).trim();
                    String name = before.substring(before.lastIndexOf(' ') + 1);
                    int close = decl.lastIndexOf(')');
                    String paramText = close > paren ? decl.substring(paren + 1, close) : "";
                    String key = (name.equals(aSimpleName) ? "<init>" : name) + "/" + countParams(paramText);
                    String cleanDecl = close > 0 ? decl.substring(0, close + 1) : decl;
                    _docs.computeIfAbsent(key, k -> new ArrayList<>()).add(new Doc(cleanDecl, doc, paramTypeNames(paramText)));
                }

                // Field
                else {
                    String name = decl.substring(decl.lastIndexOf(' ') + 1);
                    _docs.computeIfAbsent(name, k -> new ArrayList<>()).add(new Doc(decl, doc, null));
                }
            }
        }

        /**
         * Returns the doc for a key whose parameter types match the given reflected types (null types: a field).
         * Overloads with the same parameter count are told apart by type; no match means no doc, never a wrong one.
         */
        Doc getDoc(String aKey, Class<?>[] theParamTypes)
        {
            List<Doc> docs = _docs.get(aKey);
            if (docs == null)
                return null;
            if (theParamTypes == null)
                return docs.get(0);
            for (Doc doc : docs)
                if (paramTypesMatch(doc.paramTypes(), theParamTypes))
                    return doc;
            return null;
        }

        /** Whether source parameter type names match reflected parameter types. */
        private static boolean paramTypesMatch(List<String> theNames, Class<?>[] theTypes)
        {
            if (theNames == null || theNames.size() != theTypes.length)
                return false;
            for (int i = 0; i < theTypes.length; i++) {
                String srcName = theNames.get(i);
                String refName = theTypes[i].getSimpleName();
                if (srcName.equals(refName))
                    continue;
                // A type variable (T, E, K...) erases to its bound, so it matches whatever reflection reports
                String base = srcName.replace("[]", "");
                if (base.length() <= 2 && Character.isUpperCase(base.charAt(0)) &&
                        srcName.endsWith("[]") == refName.endsWith("[]"))
                    continue;
                return false;
            }
            return true;
        }

        /** Returns the erased simple type names of a parameter list, e.g. "Object anObj, List<?> items" -> [Object, List]. */
        static List<String> paramTypeNames(String aParams)
        {
            List<String> names = new ArrayList<>();
            if (aParams.isBlank())
                return names;
            int depth = 0, start = 0;
            for (int i = 0; i <= aParams.length(); i++) {
                char c = i < aParams.length() ? aParams.charAt(i) : ',';
                if (c == '<') depth++;
                else if (c == '>') depth--;
                else if (c == ',' && depth == 0) {
                    names.add(paramTypeName(aParams.substring(start, i)));
                    start = i + 1;
                }
            }
            return names;
        }

        /** Returns the erased simple type name of one parameter, e.g. "final Map<String,Object> aMap" -> Map. */
        private static String paramTypeName(String aParam)
        {
            String param = aParam.replaceAll("@[\\w.]+(\\([^)]*\\))?", "").replace("final ", "").trim();
            while (param.contains("<"))   // erase generics, innermost first
                param = param.replaceAll("<[^<>]*>", "");
            int lastSpace = param.lastIndexOf(' ');
            String type = (lastSpace > 0 ? param.substring(0, lastSpace) : param).replace(" ", "");
            type = type.replace("...", "[]");
            int dot = type.lastIndexOf('.', type.indexOf('[') >= 0 ? type.indexOf('[') : type.length());
            return dot >= 0 ? type.substring(dot + 1) : type;
        }

        /** Counts top-level parameters in a parameter list (ignores commas inside generics). */
        static int countParams(String aParams)
        {
            if (aParams.isBlank()) return 0;
            int depth = 0, count = 1;
            for (char c : aParams.toCharArray()) {
                if (c == '<') depth++;
                else if (c == '>') depth--;
                else if (c == ',' && depth == 0) count++;
            }
            return count;
        }

        /** Turns a doc comment body into one compact paragraph. */
        static String cleanDoc(String aDoc)
        {
            StringBuilder sb = new StringBuilder();
            for (String line : aDoc.split("\n")) {
                String l = line.trim();
                if (l.startsWith("*")) l = l.substring(1).trim();
                if (l.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(l);
            }
            String text = sb.toString().replaceAll("<[^>]+>", "").replaceAll("\\{@\\w+\\s+([^}]*)}", "$1");
            return text.length() > 400 ? text.substring(0, 400) + "..." : text;
        }
    }
}

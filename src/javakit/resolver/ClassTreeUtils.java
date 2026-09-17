package javakit.resolver;
import snap.util.*;
import snap.web.*;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Utility methods for ClassTree.
 */
class ClassTreeUtils {

    /**
     * Finds child packages and classes for given package node.
     */
    public static void findChildNodesForDirFile(WebFile dirFile, List<ClassTree.ClassTreeNode> classTreeNodes)
    {
        // Get directory files
        List<WebFile> dirFiles = dirFile.getFiles();

        // Iterate over dir files and add to ClassFiles or PackageDirs
        for (WebFile file : dirFiles) {

            // Handle class file
            if (isClassFile(file)) {
                String className = getClassNameForClassFile(file);
                ClassTree.ClassTreeNode classNode = createClassTreeNode(className, false);
                classTreeNodes.add(classNode);
            }

            // Handle package
            else if (isPackageDir(file)) {
                String packageName = getPackageNameForPackageDirFile(file);
                if (!ListUtils.hasMatch(classTreeNodes, classTreeNode -> classTreeNode.fullName().equals(packageName))) {
                    ClassTree.ClassTreeNode packageNode = createClassTreeNode(packageName, true);
                    classTreeNodes.add(packageNode);
                }
            }
        }
    }

    /**
     * Returns class name for class file.
     */
    private static String getClassNameForClassFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        String filePathNoExtension = filePath.substring(1, filePath.length() - 6);
        return filePathNoExtension.replace('/', '.');
    }

    /**
     * Returns package name for package file.
     */
    private static String getPackageNameForPackageDirFile(WebFile aFile)
    {
        String filePath = aFile.getPath();
        return filePath.substring(1).replace('/', '.');
    }

    /**
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isPackageDir(WebFile aFile)
    {
        if (!aFile.isDir() || aFile.getName().indexOf('.') > 0)
            return false;
        return !isIgnorePath(aFile.getPath());
    }

    /**
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isClassFile(WebFile aFile)
    {
        String path = aFile.getPath();
        if (!path.endsWith(".class"))
            return false;
        return !isIgnorePath(path);
    }

    /**
     * Creates a class tree node for given full name and whether name is package.
     */
    public static ClassTree.ClassTreeNode createClassTreeNode(String fullName, boolean isPackage)
    {
        String simpleName = getSimpleName(fullName);
        return new ClassTree.ClassTreeNode(fullName, isPackage, simpleName);
    }

    /**
     * Returns a simple class/package name for given full name.
     */
    private static String getSimpleName(String fullName)
    {
        int sepIndex = fullName.lastIndexOf('$');
        if (sepIndex < 0) sepIndex = fullName.lastIndexOf('.');
        return fullName.substring(sepIndex + 1);
    }

    /**
     * Returns whether given package/class path should be ignored.
     */
    private static boolean isIgnorePath(String aPath)
    {
        if (aPath.startsWith("/module")) return true;
        if (aPath.startsWith("/sun")) return true;
        if (aPath.startsWith("/apple")) return true;
        if (aPath.startsWith("/com/sun")) return true;
        if (aPath.startsWith("/com/apple")) return true;
        if (aPath.startsWith("/com/oracle")) return true;
        if (aPath.startsWith("/java/applet")) return true;
        if (aPath.startsWith("/java/awt/dnd")) return true;
        if (aPath.startsWith("/java/awt/peer")) return true;
        if (aPath.startsWith("/java/beans")) return true;
        if (aPath.startsWith("/java/lang/model")) return true;
        if (aPath.startsWith("/java/lang/management")) return true;
        if (aPath.startsWith("/java/nio/channels")) return true;
        if (aPath.startsWith("/java/rmi")) return true;
        if (aPath.startsWith("/java/sql")) return true;
        if (aPath.startsWith("/java/util/spi")) return true;
        if (aPath.startsWith("/java/util/Spliterators")) return true;
        if (aPath.startsWith("/javax/jws")) return true;
        if (aPath.startsWith("/javax/lang")) return true;
        if (aPath.startsWith("/javax/naming")) return true;
        if (aPath.startsWith("/javax/net")) return true;
        if (aPath.startsWith("/javax/security")) return true;
        if (aPath.startsWith("/javax/accessibility")) return true;
        if (aPath.startsWith("/javax/management")) return true;
        if (aPath.startsWith("/javax/print")) return true;
        if (aPath.startsWith("/javax/rmi")) return true;
        if (aPath.startsWith("/javax/smartcardio")) return true;
        if (aPath.startsWith("/javax/sql")) return true;
        if (aPath.startsWith("/javax/swing/plaf")) return true;
        if (aPath.startsWith("/javax/swing/tree")) return true;
        if (aPath.startsWith("/javax/swing/undo")) return true;
        if (aPath.startsWith("/javax/transaction")) return true;
        if (aPath.startsWith("/javax/xml")) return true;
        if (aPath.startsWith("/jdk")) return true;
        if (aPath.startsWith("/org/jcp")) return true;
        if (aPath.startsWith("/org/omg")) return true;
        if (aPath.startsWith("/org/w3c")) return true;
        if (aPath.startsWith("/org/xml")) return true;
        if (aPath.startsWith("/META-INF")) return true;
        return aPath.contains("$"); // If inner class, ignore
    }

    /**
     * Writes module classes/packages to a file.
     */
    private static void writeClassesForModuleName(String moduleName)
    {
        ClassTree classTree = ClassTree.getClassTreeForModuleName(moduleName);
        String classTreeString = writeClassTreeToString(classTree);
        SnapUtils.writeBytes(classTreeString.getBytes(), "/tmp/" + moduleName + ".txt");
    }

    private static String writeClassTreeToString(ClassTree classTree)
    {
        StringBuilder sb = new StringBuilder();
        writeClassTreePackageToStringBuilder(classTree, "", sb);
        return sb.toString();
    }

    private static void writeClassTreePackageToStringBuilder(ClassTree classTree, String packageName, StringBuilder sb)
    {
        List<ClassTree.ClassTreeNode> rootNodes = classTree.getClassTreeNodesForPackageName(packageName);
        List<ClassTree.ClassTreeNode> classNodes = ListUtils.filter(rootNodes, node -> !node.isPackage());
        List<ClassTree.ClassTreeNode> packageNodes = ListUtils.filter(rootNodes, ClassTree.ClassTreeNode::isPackage);

        // Write /package-name
        sb.append('/').append(packageName).append('\n');
        classNodes.forEach(classNode -> writeClassNodeToStringBuilder(classNode, sb));
        packageNodes.forEach(packageNode -> writeClassTreePackageToStringBuilder(classTree, packageNode.fullName(), sb));
    }

    private static void writeClassNodeToStringBuilder(ClassTree.ClassTreeNode classNode, StringBuilder sb)
    {
        Class<?> cls;
        try { cls = Class.forName(classNode.fullName()); }
        catch (ClassNotFoundException e) { System.err.println("Cannot find class " + classNode.fullName()); return; }
        if (!Modifier.isPublic(cls.getModifiers()))
            return;
        if (List.of("StringTemplate", "TemplateRuntime", "FormatProcessor").contains(classNode.simpleName()))
            return; // Some java 21 excludes

        sb.append(classNode.simpleName()).append('\n');
    }

    public static Map<String,List<ClassTree.ClassTreeNode>> readClassNodesForModuleName(String moduleName)
    {
        WebURL moduleUrl = WebURL.getResourceUrl(ClassTreeUtils.class, moduleName + ".txt");
        assert moduleUrl != null;
        Iterator<String> moduleEntries = moduleUrl.getText().lines().iterator();
        Map<String,List<ClassTree.ClassTreeNode>> classPathNodes = new LinkedHashMap<>();

        String packagePath = moduleEntries.next();
        readPackageEntry(packagePath, moduleEntries, classPathNodes);
        return classPathNodes;
    }

    private static String readPackageEntry(String packagePath, Iterator<String> moduleEntries, Map<String, List<ClassTree.ClassTreeNode>> classPathNodes)
    {
        List<ClassTree.ClassTreeNode> packagePathNodes = new ArrayList<>();
        String packageName = packagePath.substring(1) + '.';
        classPathNodes.put(packagePath.replace('.', '/'), packagePathNodes);

        while (moduleEntries.hasNext()) {
            String nextEntry = moduleEntries.next();

            // Handle package path entry
            if (nextEntry.startsWith("/")) {

                while (nextEntry != null) {

                    // Handle child package
                    if (nextEntry.startsWith(packagePath + '.') || packagePath.equals("/"))
                        packagePathNodes.add(createClassTreeNode(nextEntry.substring(1), true));
                    else return nextEntry;

                    nextEntry = readPackageEntry(nextEntry, moduleEntries, classPathNodes);
                }
            }

            // Handle class entry
            else packagePathNodes.add(createClassTreeNode(packageName + nextEntry, false));
        }

        return null;
    }

    public static void main(String[] args)
    {
        writeClassesForModuleName("java.base");
        //writeClassesForModuleName("java.desktop");
    }
}

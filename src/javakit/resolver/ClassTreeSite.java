package javakit.resolver;
import snap.util.*;
import snap.web.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class represents a class tree site.
 */
public class ClassTreeSite {

    // The web site
    private WebSite _site;

    /**
     * Constructor.
     */
    public ClassTreeSite(WebSite aSite)
    {
        _site = aSite;
    }

    /**
     * Returns whether given package file path is known.
     */
    boolean isKnownPackageFilePath(String filePath)
    {
        WebFile file = _site.getFileForPath(filePath);
        return file != null && file.isDir() && file.getPath().equals(filePath);
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    public List<ClassTree.ClassTreeNode> getClassTreeNodesForPackageName(String packageName)
    {
        String filePath = '/' + packageName.replace(".", "/");
        List<ClassTree.ClassTreeNode> classTreeNodes = new ArrayList<>();
        findClassTreeNodesForPackageFilePath(filePath, classTreeNodes);
        return classTreeNodes;
    }

    /**
     * Returns ClassTreeNode array for classes and child packages for given node.
     */
    void findClassTreeNodesForPackageFilePath(String filePath, List<ClassTree.ClassTreeNode> classTreeNodes)
    {
        // Get files
        WebFile nodeFile = _site.getFileForPath(filePath);
        if (nodeFile == null)
            return;

        // If root package and base module, add primitives
        if (filePath.equals("/") && _site.getName().endsWith("java.base")) {
            List<Class<?>> primitives = List.of(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
            List<ClassTree.ClassTreeNode> primitiveNodes = ListUtils.map(primitives, cls -> createClassTreeNode(cls.getName(), false));
            classTreeNodes.addAll(primitiveNodes);
        }

        // Iterate over files and Find child classes and packages for each
        findChildNodesForDirFile(nodeFile, classTreeNodes);
    }

    /**
     * Finds child packages and classes for given package node.
     */
    private void findChildNodesForDirFile(WebFile dirFile, List<ClassTree.ClassTreeNode> classTreeNodes)
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
        String className = filePathNoExtension.replace('/', '.');
        return className;
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
        if (!aFile.isDir())
            return false;
        if (aFile.getName().indexOf('.') > 0)
            return false;
        String path = aFile.getPath();
        if (isIgnorePath(path))
            return false;
        return true;
    }

    /**
     * Returns whether given WebFile is a package dir.
     */
    private static boolean isClassFile(WebFile aFile)
    {
        String path = aFile.getPath();
        if (!path.endsWith(".class"))
            return false;
        if (isIgnorePath(path))
            return false;
        return true;
    }

    /**
     * Returns the JRT site for module name.
     */
    public static ClassTreeSite getSiteForModuleName(String moduleName)
    {
        WebURL moduleUrl = WebURL.getUrl("jrt:/" + moduleName); assert moduleUrl != null;
        return new ClassTreeSite(moduleUrl.getSite());
    }

    /**
     * Creates a class tree node for given full name and whether name is package.
     */
    private static ClassTree.ClassTreeNode createClassTreeNode(String fullName, boolean isPackage)
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
        ClassTreeSite classTreeSite = ClassTreeSite.getSiteForModuleName(moduleName);
        String classTreeString = writeClassTreeSiteToString(classTreeSite);
        SnapUtils.writeBytes(classTreeString.getBytes(), "/tmp/" + moduleName + ".txt");
    }

    private static String writeClassTreeSiteToString(ClassTreeSite classTreeSite)
    {
        StringBuilder sb = new StringBuilder();
        writeClassTreePackageToStringBuilder(classTreeSite, "", sb);
        return sb.toString();
    }

    private static void writeClassTreePackageToStringBuilder(ClassTreeSite classTreeSite, String packageName, StringBuilder sb)
    {
        List<ClassTree.ClassTreeNode> rootNodes = classTreeSite.getClassTreeNodesForPackageName(packageName);
        List<ClassTree.ClassTreeNode> classNodes = ListUtils.filter(rootNodes, node -> !node.isPackage());
        List<ClassTree.ClassTreeNode> packageNodes = ListUtils.filter(rootNodes, ClassTree.ClassTreeNode::isPackage);

        // Write /package-name
        sb.append('/').append(packageName).append('\n');
        classNodes.forEach(classNode -> writeClassNodeToStringBuilder(classNode, sb, false));
        packageNodes.forEach(packageNode -> writeClassTreePackageToStringBuilder(classTreeSite, packageNode.fullName(), sb));
    }

    private static void writeClassNodeToStringBuilder(ClassTree.ClassTreeNode classNode, StringBuilder sb, boolean isInner)
    {
        Class<?> cls;
        try { cls = Class.forName(classNode.fullName()); }
        catch (ClassNotFoundException e) { System.err.println("Cannot find class " + classNode.fullName()); return; }
        if (!Modifier.isPublic(cls.getModifiers()))
            return;

        if (isInner) sb.append('$');
        sb.append(classNode.simpleName()).append('\n');

        // Recurse for inner classes - For now only getting 1 level of inner classes
        if (!isInner) {
            Class<?>[] innerClasses = cls.getDeclaredClasses();
            Stream.of(innerClasses).forEach(icls -> writeClassNodeToStringBuilder(createClassTreeNode(icls.getName(), false), sb, true));
        }
    }

    public static void main(String[] args)
    {
        writeClassesForModuleName("java.base");
        //writeClassesForModuleName("java.desktop");
    }
}

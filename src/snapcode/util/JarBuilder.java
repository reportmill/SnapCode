package snapcode.util;
import javakit.project.Project;
import snap.util.FilePathUtils;
import snap.web.WebURL;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * A class to build a Jar file.
 */
public class JarBuilder {

    // The Jar URL
    private WebURL  _jarURL;

    // The list of files to add
    private List<File>  _files = new ArrayList<>();

    // THe list of dirs each file is relative to
    private List<File>  _dirs = new ArrayList<>();

    /**
     * Sets the Jar URL.
     */
    public void setJarURL(Object aDest)
    {
        _jarURL = WebURL.getURL(aDest);
    }

    /**
     * Builds the actual file.
     */
    public void build() throws IOException
    {
        // Create manifest
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        // Create Jar output
        File jarFile = _jarURL.getJavaFile();
        JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile), manifest);
        for (int i = 0, iMax = _files.size(); i < iMax; i++) {
            File file = _files.get(i), dir = _dirs.get(i);
            add(file, dir, target);
        }

        // Close
        target.close();
    }

    /**
     * Adds a file.
     */
    public void addFile(Object aFile, Object aDir)
    {
        WebURL url = WebURL.getURL(aFile);
        File file = url.getJavaFile();
        WebURL durl = WebURL.getURL(aDir);
        File dir = durl.getJavaFile();
        if (file != null) {
            _files.add(file);
            _dirs.add(dir);
        } else System.err.println("JarBuilder.addFile: Source not file: " + aFile);
    }

    /**
     * Adds files from a top level directory.
     */
    public void addFiles(Object aTopDir)
    {
        WebURL url = WebURL.getURL(aTopDir);
        File file = url.getJavaFile();

        if (file.isDirectory()) {
            for (File nestedFile : file.listFiles()) {
                WebURL url2 = WebURL.getURL(nestedFile);
                if (nestedFile.isDirectory())
                    addFile(url2, file);
                else {
                    String ext = FilePathUtils.getExtension(url2.getPath()).toLowerCase();
                    if (ext.equals("html") || ext.equals("jar")) continue;
                    addFile(nestedFile, file);
                }
            }
        }
    }

    /**
     * Adds a new file.
     */
    private void add(File aFile, File aDir, JarOutputStream target) throws IOException
    {
        // Get entry path
        String filePath = aFile.getPath().replace("\\", "/");
        String dirPath = aDir.getPath().replace("\\", "/");
        String entryPath = filePath.substring(dirPath.length() + 1);

        // Handle directory
        if (aFile.isDirectory()) {
            if (!entryPath.isEmpty()) {
                if (!entryPath.endsWith("/")) entryPath += "/";
                JarEntry entry = new JarEntry(entryPath);
                entry.setTime(aFile.lastModified());
                target.putNextEntry(entry);
                target.closeEntry();
            }

            for (File nestedFile : aFile.listFiles())
                add(nestedFile, aDir, target);
            return;
        }

        // Handle file
        BufferedInputStream in = null;
        try {
            JarEntry entry = new JarEntry(entryPath);
            entry.setTime(aFile.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(aFile));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }

        //
        finally {
            if (in != null) in.close();
        }
    }

    /**
     * Builds a Jar file for given file from files in given directory.
     */
    public static void build(Object aJarDest, Object aSourceDir) throws IOException
    {
        JarBuilder jb = new JarBuilder();
        jb.setJarURL(aJarDest);
        jb.addFiles(aSourceDir);
        jb.build();
    }

    /**
     * Builds a Jar file for given project.
     */
    public static void build(Project aProj) throws IOException
    {
        // Get build path and jar path
        String buildPath = aProj.getProjectConfig().getBuildPathAbsolute();
        String jarPath = buildPath + '/' + aProj.getName() + ".jar";

        // Create JarBuilder and set jar path
        JarBuilder jb = new JarBuilder();
        jb.setJarURL(jarPath);

        // Iterate over files in build dir. If file is class, dir or exists in src dir, add to jar
        File buildDir = new File(buildPath);
        for (File file : buildDir.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".class") || aProj.getSourceFile('/' + name, false, false) != null)
                jb.addFile(file, buildDir);
        }

        // Build jar
        jb.build();
    }
}
package snapcode.project;
import javakit.project.ProjectConfig;
import snap.props.PropChange;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages read/write of ProjectConfig.
 */
public class ProjectConfigFile {

    // The Project
    private ProjectX _proj;

    // The ProjectConfig
    private ProjectConfig  _projConfig;

    // The web file
    private WebFile  _file;

    // The XML element
    private XMLElement  _xml;

    /**
     * Constructor.
     */
    public ProjectConfigFile(ProjectX aProject)
    {
        _proj = aProject;

        // Create ProjectConfig
        _projConfig = new ProjectConfig(aProject);
        _projConfig.addPropChangeListener(pc -> propConfigDidPropChange(pc));

        // Read file
        readFile();
    }

    /**
     * Returns the ProjectConfig.
     */
    public ProjectConfig getProjectConfig()  { return _projConfig; }

    /**
     * Returns the file.
     */
    public WebFile getFile()
    {
        // If already set, just return
        if (_file != null) return _file;

        // Get file
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath("build.snap");
        if (file == null)
            file = projSite.getFileForPath(".classpath");

        // If missing, create default
        if (file == null)
            file = createFile(_proj);

        // Set/return
        return _file = file;
    }

    /**
     * Returns the XML for file.
     */
    public XMLElement getXML()
    {
        if (_xml != null) return _xml;
        XMLElement xml = createXML();
        return _xml = xml;
    }

    /**
     * Creates the XML for file.
     */
    protected XMLElement createXML()
    {
        WebFile file = getFile();
        if (file != null && file.getExists())
            return XMLElement.readFromXMLSource(file);
        XMLElement xml = new XMLElement("classpath");
        return xml;
    }

    /**
     * Reads the class path from .classpath file.
     */
    public void readFile()
    {
        _xml = null;

        // Get file
        WebFile file = getFile();

        getSourcePathRead();
        getBuildPathRead();
        getLibPathsRead();
        getProjectPathsRead();
    }

    /**
     * Saves the ClassPath to .classpath file.
     */
    public void writeFile() throws Exception
    {
        _xml = new XMLElement("classpath");
        getBuildPathWrite();
        getSrcPathsWrite();
        getLibPathsWrite();

        XMLElement xml = getXML();
        byte[] xmlBytes = xml.getBytes();
        getFile().setBytes(xmlBytes);
        getFile().save();
    }

    /**
     * Called when PropConfig does PropChange.
     */
    private void propConfigDidPropChange(PropChange aPC)
    {
        try { writeFile(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the source path.
     */
    public void getSourcePathRead()
    {
        // Get source path from src classpathentry
        XMLElement[] xmls = getSourcePathXMLs();
        XMLElement xml = xmls.length > 0 ? xmls[0] : null;
        String path = xml != null ? xml.getAttributeValue("path") : null;

        // If no path and /src exists, use it
        WebSite projSite = _proj.getSite();
        if (path == null && projSite.getFileForPath("/src") != null)
            path = "src";

        // Set/return
        _projConfig._srcPath = path;
    }

    /**
     * Returns the build path.
     */
    public void getBuildPathRead()
    {
        // Get source path from output classpathentry
        XMLElement xml = getBuildPathXML();
        String path = xml != null ? xml.getAttributeValue("path") : null;

        // If path not set, use bin
        if (path == null)
            path = "bin";

        // Set/return
        _projConfig._buildPath = path;
    }

    /**
     * Sets the build path.
     */
    public void getBuildPathWrite()
    {
        // Update XML
        XMLElement xml = getBuildPathXML();
        if (xml == null) {
            xml = new XMLElement("classpathentry");
            xml.add("kind", "output");
            getXML().add(xml);
        }

        String _buildPath = _projConfig.getBuildPath();
        if (_buildPath != null)
            xml.add("path", _buildPath);
        else getXML().removeElement(xml);
    }

    /**
     * Returns the source paths.
     */
    public void getSrcPathsRead()
    {
        // Load from lib elements
        List<String> paths = new ArrayList();
        XMLElement[] srcPathXMLs = getSrcXMLs();
        for (XMLElement xml : srcPathXMLs) {
            String path = xml.getAttributeValue("path");
            paths.add(path);
        }

        _projConfig._srcPaths = paths.toArray(new String[0]);
    }

    /**
     * Adds a source path.
     */
    public void getSrcPathsWrite()
    {
        // Add XML for path
        String[] srcPaths = _projConfig.getSrcPaths();
        if (srcPaths == null) return;

        for (String srcPath : srcPaths) {
            String path = getRelativePath(srcPath);
            XMLElement xml = new XMLElement("classpathentry");
            xml.add("kind", "src");
            xml.add("path", path);
            getXML().add(xml);
        }
    }

    /**
     * Returns the paths.
     */
    public void getLibPathsRead()
    {
        // Load from lib elements
        List<String> paths = new ArrayList();
        XMLElement[] libPathXMLs = getLibXMLs();
        for (XMLElement xml : libPathXMLs) {
            String path = xml.getAttributeValue("path");
            paths.add(path);
        }

        // Set/return
        _projConfig._libPaths = paths.toArray(new String[0]);
    }

    /**
     * Adds a library path.
     */
    public void getLibPathsWrite()
    {
        // Add XML for path
        String[] libPaths = _projConfig.getLibPaths();
        if (libPaths == null) return;

        // Add XML for path
        for (String libPath : libPaths) {
            XMLElement xml = new XMLElement("classpathentry");
            xml.add("kind", "lib");
            xml.add("path", libPath);
            getXML().add(xml);
        }
    }

    /**
     * Returns the project paths path.
     */
    public void getProjectPathsRead()
    {
        // Load from ProjectXMLs
        List<String> paths = new ArrayList();
        XMLElement[] projPathXMLs = getProjectPathXMLs();
        for (XMLElement xml : projPathXMLs)
            paths.add(xml.getAttributeValue("path"));

        // Set/return
        _projConfig._projPaths = paths.toArray(new String[0]);
    }

    /**
     * Returns the src classpathentry xmls.
     */
    private XMLElement[] getSrcXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement x : getXML().getElements())
            if ("src".equals(x.getAttributeValue("kind")))
                paths.add(x);
        return paths.toArray(new XMLElement[0]);
    }

    /**
     * Returns the lib classpathentry xmls.
     */
    private XMLElement[] getLibXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement x : getXML().getElements())
            if ("lib".equals(x.getAttributeValue("kind")))
                paths.add(x);
        return paths.toArray(new XMLElement[0]);
    }

    /**
     * Returns the src classpathentry xmls that are in project directory.
     */
    private XMLElement[] getSourcePathXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement src : getSrcXMLs()) {
            String path = src.getAttributeValue("path");
            if (path != null && !path.startsWith("/"))
                paths.add(src);
        }
        return paths.toArray(new XMLElement[0]);
    }

    /**
     * Returns the project classpathentry xmls that are outside project directory.
     */
    private XMLElement[] getProjectPathXMLs()
    {
        List<XMLElement> paths = new ArrayList();
        for (XMLElement src : getSrcXMLs()) {
            String path = src.getAttributeValue("path");
            if (path != null && path.startsWith("/")) paths.add(src);
        }
        return paths.toArray(new XMLElement[0]);
    }

    /**
     * Returns the output classpathentry xml.
     */
    private XMLElement getBuildPathXML()
    {
        for (XMLElement child : getXML().getElements())
            if ("output".equals(child.getAttributeValue("kind")))
                return child;
        return null;
    }

    /**
     * Returns a relative path for given path.
     */
    private String getRelativePath(String aPath)
    {
        String path = aPath;
        if (File.separatorChar != '/') path = path.replace(File.separatorChar, '/');
        if (!aPath.startsWith("/")) return path;
        String root = getProjRootDirPath();
        if (path.startsWith(root)) path = path.substring(root.length());
        return path;
    }


    /**
     * Returns the project root path.
     */
    private String getProjRootDirPath()
    {
        WebSite projSite = _proj.getSite();
        String root = projSite.getRootDir().getJavaFile().getAbsolutePath();
        if (File.separatorChar != '/') root = root.replace(File.separatorChar, '/');
        if (!root.endsWith("/")) root = root + '/';
        if (!root.startsWith("/")) root = '/' + root;
        return root;
    }


    /**
     * Creates the ClassPath file for given project.
     */
    public static WebFile createFile(ProjectX aProj)
    {
        // Get default text
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<classpath>\n");
        sb.append("\t<classpathentry kind=\"src\" path=\"src\"/>\n");
        sb.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n");
        sb.append("\t<classpathentry kind=\"output\" path=\"bin\"/>\n");
        sb.append("</classpath>\n");

        // Create .classpath file
        WebSite site = aProj.getSite();
        WebFile file = site.createFileForPath(".classpath", false);

        // Set default text and save
        file.setText(sb.toString());
        file.save();

        // Return
        return file;
    }
}

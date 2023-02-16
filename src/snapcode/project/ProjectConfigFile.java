package snapcode.project;
import javakit.project.Project;
import javakit.project.ProjectConfig;
import javakit.project.ProjectUtils;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.List;

/**
 * This class manages read/write of ProjectConfig.
 */
public class ProjectConfigFile {

    // The Project
    private Project  _proj;

    // The ProjectConfig
    private ProjectConfig  _projConfig;

    // The web file
    private WebFile  _file;

    // The XML element
    private XMLElement  _xml;

    /**
     * Constructor.
     */
    public ProjectConfigFile(Project aProject)
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
     * Reads the class path from .classpath file.
     */
    public void readFile()
    {
        _xml = null;

        // Get file
        getFile();

        readSourcePath();
        readBuildPath();
        readLibPaths();
        readProjectPaths();
    }

    /**
     * Saves the ClassPath to .classpath file.
     */
    public void writeFile()
    {
        _xml = new XMLElement("classpath");
        writeSourcePath();
        writeBuildPath();
        writeLibPaths();
        writeProjectPaths();

        // Get file and XML bytes
        WebFile configFile = getFile();
        XMLElement xml = getXML();
        byte[] xmlBytes = xml.getBytes();

        // Set bytes and save
        configFile.setBytes(xmlBytes);
        configFile.save();
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
     * Reads the source path.
     */
    public void readSourcePath()
    {
        // Get source path from src classpathentry
        XMLElement[] srcXMLs = getSrcXMLs();
        XMLElement sourcePathXML = ArrayUtils.findMatch(srcXMLs, xml -> isSourcePath(xml));
        String sourcePath = sourcePathXML != null ? sourcePathXML.getAttributeValue("path") : null;

        // If no path and /src exists, use it
        WebSite projSite = _proj.getSite();
        if (sourcePath == null && projSite.getFileForPath("/src") != null)
            sourcePath = "src";

        // Set/return
        _projConfig.setSourcePath(sourcePath);
    }

    /**
     * Reads the build path.
     */
    public void readBuildPath()
    {
        // Get source path from output classpathentry
        List<XMLElement> xmls = getXML().getElements();
        XMLElement buildPathXML = ListUtils.findMatch(xmls, xml -> isBuildKind(xml));
        String buildPath = buildPathXML != null ? buildPathXML.getAttributeValue("path") : null;

        // If path not set, use bin
        if (buildPath == null)
            buildPath = "bin";

        // Set/return
        _projConfig.setBuildPath(buildPath);
    }

    /**
     * Returns the paths.
     */
    public void readLibPaths()
    {
        List<XMLElement> xmls = getXML().getElements();
        List<XMLElement> libXMLs = ListUtils.filter(xmls, xml -> isLibKind(xml));
        XMLElement[] libPathXMLs = libXMLs.toArray(new XMLElement[0]);
        String[] libPaths =  ArrayUtils.map(libPathXMLs, xml -> xml.getAttributeValue("path"), String.class);
        _projConfig.setLibPaths(libPaths);
    }

    /**
     * Returns the project paths path.
     */
    public void readProjectPaths()
    {
        XMLElement[] srcXMLs = getSrcXMLs();
        XMLElement[] projPathXMLs = ArrayUtils.filter(srcXMLs, xml -> isProjectPath(xml));
        String[] projectPaths =  ArrayUtils.map(projPathXMLs, xml -> xml.getAttributeValue("path"), String.class);
        _projConfig.setProjectPaths(projectPaths);
    }

    /**
     * Writes the source path.
     */
    public void writeSourcePath()
    {
        // Update XML
        XMLElement xml = new XMLElement("classpathentry");
        xml.add("kind", "src");
        getXML().add(xml);

        String sourcePath = _projConfig.getSourcePath();
        if (sourcePath != null)
            xml.add("path", sourcePath);
        else getXML().removeElement(xml);
    }

    /**
     * Writes the build path.
     */
    public void writeBuildPath()
    {
        // Update XML
        XMLElement xml = new XMLElement("classpathentry");
        xml.add("kind", "output");
        getXML().add(xml);

        String buildPath = _projConfig.getBuildPath();
        if (buildPath != null)
            xml.add("path", buildPath);
        else getXML().removeElement(xml);
    }

    /**
     * Writes the library paths.
     */
    public void writeLibPaths()
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
     * Writes the project paths.
     */
    public void writeProjectPaths()
    {
        // Add XML for path
        String[] srcPaths = _projConfig.getProjectPaths();
        if (srcPaths == null) return;

        for (String srcPath : srcPaths) {
            String path = ProjectUtils.getRelativePath(_proj, srcPath);
            XMLElement xml = new XMLElement("classpathentry");
            xml.add("kind", "src");
            xml.add("path", path);
            getXML().add(xml);
        }
    }

    /**
     * Returns the XML for file.
     */
    public XMLElement getXML()
    {
        // If already set, just return
        if (_xml != null) return _xml;

        // Create
        XMLElement xml = createXML();

        // Set/return
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
        return new XMLElement("classpath");
    }

    /**
     * Returns the src classpathentry xmls.
     */
    private XMLElement[] getSrcXMLs()
    {
        List<XMLElement> xmls = getXML().getElements();
        List<XMLElement> srcXMLs = ListUtils.filter(xmls, xml -> isSrcKind(xml));
        return srcXMLs.toArray(new XMLElement[0]);
    }

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
            file = createConfigFile();

        // Set/return
        return _file = file;
    }

    /**
     * Creates the ClassPath file for given project.
     */
    public WebFile createConfigFile()
    {
        // Get default text
        String defaultConfigStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<classpath>\n" +
                "\t<classpathentry kind=\"src\" path=\"src\"/>\n" +
                "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" +
                "\t<classpathentry kind=\"output\" path=\"bin\"/>\n" +
                "</classpath>\n";

        // Create .classpath file
        WebSite site = _proj.getSite();
        WebFile file = site.createFileForPath(".classpath", false);

        // Set default text and save
        file.setText(defaultConfigStr);
        file.save();

        // Return
        return file;
    }

    /**
     * Utility methods.
     */
    private boolean isSrcKind(XMLElement xml)  { return "src".equals(xml.getAttributeValue("kind")); }
    private boolean isLibKind(XMLElement xml)  { return "lib".equals(xml.getAttributeValue("kind")); }
    private boolean isBuildKind(XMLElement xml)  { return "output".equals(xml.getAttributeValue("kind")); }
    private boolean isSourcePath(XMLElement xml)
    {
        String path = xml.getAttributeValue("path");
        return path != null && !path.startsWith("/");
    }
    private boolean isProjectPath(XMLElement xml)
    {
        String path = xml.getAttributeValue("path");
        return path != null && path.startsWith("/");
    }
}

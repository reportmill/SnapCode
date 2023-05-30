package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.List;

/**
 * This class manages read/write of ProjectConfig.
 */
public class EclipseBuildFile {

    // The Project
    private Project  _proj;

    // The ProjectConfig
    private BuildFile _projConfig;

    // The XML element
    private XMLElement  _xml;

    /**
     * Constructor.
     */
    public EclipseBuildFile(Project aProject, BuildFile projConfig)
    {
        _proj = aProject;
        _projConfig = projConfig;

        // Get Eclipse file (just return if not there)
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath(".classpath");
        if (file == null || !file.getExists())
            return;

        // Load XML
        _xml = XMLElement.readFromXMLSource(file);
        if (_xml == null)
            return;

        // Read file
        readFile();
    }

    /**
     * Reads the class path from Eclipse .classpath file.
     */
    public void readFile()
    {
        readSourcePath();
        readBuildPath();
        readLibPaths();
        readProjectPaths();
    }

    /**
     * Reads the source path.
     */
    private void readSourcePath()
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
    private void readBuildPath()
    {
        // Get source path from output classpathentry
        List<XMLElement> xmls = _xml.getElements();
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
    private void readLibPaths()
    {
        List<XMLElement> xmls = _xml.getElements();
        List<XMLElement> libXMLs = ListUtils.filter(xmls, xml -> isLibKind(xml));
        XMLElement[] libPathXMLs = libXMLs.toArray(new XMLElement[0]);
        String[] libPaths =  ArrayUtils.map(libPathXMLs, xml -> xml.getAttributeValue("path"), String.class);
        _projConfig.setLibPaths(libPaths);
    }

    /**
     * Returns the project paths path.
     */
    private void readProjectPaths()
    {
        XMLElement[] srcXMLs = getSrcXMLs();
        XMLElement[] projPathXMLs = ArrayUtils.filter(srcXMLs, xml -> isProjectPath(xml));
        String[] projectPaths =  ArrayUtils.map(projPathXMLs, xml -> xml.getAttributeValue("path"), String.class);
        _projConfig.setProjectPaths(projectPaths);
    }

    /**
     * Returns the src classpathentry xmls.
     */
    private XMLElement[] getSrcXMLs()
    {
        List<XMLElement> xmls = _xml.getElements();
        List<XMLElement> srcXMLs = ListUtils.filter(xmls, xml -> isSrcKind(xml));
        return srcXMLs.toArray(new XMLElement[0]);
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

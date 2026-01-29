package snapcode.project;
import snap.util.ListUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import java.util.Collections;
import java.util.List;

/**
 * This class reads a POM file for a maven dependency.
 */
public class MavenPomFile extends MavenFile {

    // The dependencies
    private List<MavenPackage> _dependencies;

    // The XML
    private XMLElement _xml;

    /**
     * Constructor.
     */
    public MavenPomFile(MavenPackage mavenDependency)
    {
        super(mavenDependency, "pom");
    }

    /**
     * Returns dependencies.
     */
    public List<MavenPackage> getDependencies()
    {
        if (_dependencies != null) return _dependencies;

        // Get <dependency> XML elements
        XMLElement xml = getXML();
        XMLElement dependenciesXML = xml != null ? xml.getElement("dependencies") : null;
        List<XMLElement> dependencyXMLs = dependenciesXML != null ? dependenciesXML.getElements("dependency") : null;
        if (dependencyXMLs == null)
            return _dependencies = Collections.emptyList();

        // Get dependencies and return
        return _dependencies = ListUtils.mapNonNull(dependencyXMLs, MavenPomFile::getDependencyForXML);
    }

    /**
     * Creates a maven dependency for dependency xml element.
     */
    private static MavenPackage getDependencyForXML(XMLElement dependencyXML)
    {
        // Get XML elements for group, artifact, version
        XMLElement groupIdXML = dependencyXML.getElement("groupId");
        XMLElement artifactIdXML = dependencyXML.getElement("artifactId");
        XMLElement versionXML = dependencyXML.getElement("version");
        if (groupIdXML == null || artifactIdXML == null || versionXML == null)
            return null;

        // Get groupId, artifactId, version
        String groupId = groupIdXML.getValue();
        String artifactId = artifactIdXML.getValue();
        String version = versionXML.getValue();
        if (groupId == null || groupId.isBlank() || artifactId == null || artifactId.isBlank() || version == null || version.isBlank())
            return null;

        // Create and return maven dependency for id
        return MavenPackage.getMavenPackageForId(groupId + ":" + artifactId + ":" + version);
    }

    /**
     * Returns the XML.
     */
    private XMLElement getXML()
    {
        if (_xml != null) return _xml;
        WebFile pomFile = getLocalFile();
        String xmlString = pomFile.getText();
        if (xmlString == null) {
            System.err.println("MavenPomFile: Can't read pom file: " + pomFile);
            return null;
        }

        // Read and return
        try { return _xml = XMLElement.readXmlFromString(xmlString); }
        catch (Exception e) {
            System.err.println("MavenPomFile.getXML: Error reading file: " + pomFile.getPath());
            System.err.println(e.getMessage());
            return null;
        }
    }
}

package snapcode.project;
import snap.util.ListUtils;
import snap.util.XMLElement;
import java.util.Collections;
import java.util.List;

/**
 * This class reads a POM file for a maven dependency.
 */
public class MavenPomFile extends MavenFile {

    // The dependencies
    private List<MavenDependency> _dependencies;

    // The XML
    private XMLElement _xml;

    /**
     * Constructor.
     */
    public MavenPomFile(MavenPackage mavenPackage)
    {
        super(mavenPackage, "pom");
    }

    /**
     * Returns dependencies.
     */
    public List<MavenDependency> getDependencies()
    {
        if (_dependencies != null) return _dependencies;
        List<XMLElement> dependencyXMLs = getDependencyXMLs();
        return _dependencies = ListUtils.mapNonNull(dependencyXMLs, MavenPomFile::getDependencyForXML);
    }

    /**
     * Returns dependency XML elements.
     */
    private List<XMLElement> getDependencyXMLs()
    {
        XMLElement xml = getXML();
        XMLElement dependenciesXML = xml != null ? xml.getElement("dependencies") : null;
        List<XMLElement> dependencyXMLs = dependenciesXML != null ? dependenciesXML.getElements("dependency") : null;
        return dependencyXMLs != null ? dependencyXMLs : Collections.emptyList();
    }

    /**
     * Creates a maven dependency for dependency xml element.
     */
    private static String getMavenIdForXML(XMLElement dependencyXML)
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

        // Create and return maven id
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Creates a maven package for dependency xml element.
     */
    private static MavenDependency getDependencyForXML(XMLElement dependencyXML)
    {
        String mavenId = getMavenIdForXML(dependencyXML);
        return mavenId != null ? new MavenDependency(mavenId) : null;
    }

    /**
     * Returns the XML.
     */
    private XMLElement getXML()
    {
        if (_xml != null) return _xml;
        return _xml = getLocalFileXml();
    }
}

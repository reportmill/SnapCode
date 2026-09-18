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
    private List<MavenPackage> _dependencies;

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

        // Oh, this is just frickin sad
        if (version.startsWith("[") || version.startsWith("("))
            version = version.substring(1);
        if (version.contains(","))
            version = version.substring(0, version.indexOf(","));
        if (version.contains("+"))
            version = version.replace("+", "");

        // Create and return maven dependency for id
        return MavenPackage.getMavenPackageForId(groupId + ":" + artifactId + ":" + version);
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

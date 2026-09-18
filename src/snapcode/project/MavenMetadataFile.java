package snapcode.project;
import snap.util.ListUtils;
import snap.util.XMLElement;
import java.util.Collections;
import java.util.List;

/**
 * This class reads a metadata-local.xml file for a maven dependency.
 */
public class MavenMetadataFile extends MavenFile {

    // The versions
    private List<String> _versions;

    // The XML
    private XMLElement _xml;

    /**
     * Constructor.
     */
    public MavenMetadataFile(MavenPackage mavenPackage)
    {
        super(mavenPackage, "maven-metadata.xml");
    }

    /**
     * Returns the versions.
     */
    public List<String> getVersions()
    {
        if (_versions != null) return _versions;
        return _versions = getVersionsImpl();
    }

    /**
     * Returns the versions.
     */
    private List<String> getVersionsImpl()
    {
        // Get <versionin> XML elements
        XMLElement xml = getXML();
        XMLElement versioningXML = xml != null ? xml.getElement("versioning") : null;
        XMLElement versionsXML = versioningXML != null ? versioningXML.getElement("versions") : null;
        List<XMLElement> versionXMLs = versionsXML != null ? versionsXML.getElements("version") : null;
        if (versionXMLs == null || versionXMLs.isEmpty())
            return Collections.emptyList();

        // Get version strings and return
        return ListUtils.mapNonNull(versionXMLs, MavenMetadataFile::getVersionStringForVersionXml);
    }

    /**
     * Returns the XML.
     */
    private XMLElement getXML()
    {
        if (_xml != null) return _xml;
        return _xml = getLocalFileXml();
    }

    /**
     * Returns the version string for given version XML, e.g.: <version>2025.01.02</version>.
     */
    private static String getVersionStringForVersionXml(XMLElement xml)
    {
        String version = xml.getValue();
        return !version.isBlank() ? version.trim() : null;
    }
}

package snapcode.project;
import snap.util.ListUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebURL;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This class reads a POM file for a maven dependency.
 */
public class MavenPomFile {

    // The maven dependency
    private MavenDependency _mavenDependency;

    // The dependencies
    private List<MavenDependency> _dependencies;

    // The XML
    private XMLElement _xml;

    /**
     * Constructor.
     */
    public MavenPomFile(MavenDependency mavenDependency)
    {
        _mavenDependency = mavenDependency;
    }

    /**
     * Returns dependencies.
     */
    public List<MavenDependency> getDependencies()
    {
        if (_dependencies != null) return _dependencies;

        // Get <dependency> XML elements
        XMLElement xml = getXML();
        XMLElement dependenciesXML = xml != null ? xml.getElement("dependencies") : null;
        List<XMLElement> dependencyXMLs = dependenciesXML != null ? dependenciesXML.getElements("dependency") : null;
        if (dependencyXMLs == null)
            return _dependencies = Collections.emptyList();

        // Get dependencies and return
        List<MavenDependency> dependencies = ListUtils.mapNonNull(dependencyXMLs, MavenPomFile::getDependencyForXML);
        return _dependencies = dependencies;
    }

    /**
     * Creates a maven dependency for dependency xml element.
     */
    private static MavenDependency getDependencyForXML(XMLElement dependencyXML)
    {
        XMLElement groupIdXML = dependencyXML.getElement("groupId");
        XMLElement artifactIdXML = dependencyXML.getElement("artifactId");
        XMLElement versionXML = dependencyXML.getElement("version");
        if (groupIdXML == null || artifactIdXML == null || versionXML == null)
            return null;
        String groupId = groupIdXML.getValue();
        String artifactId = artifactIdXML.getValue();
        String version = versionXML.getValue();
        if (groupId == null || groupId.isBlank() || artifactId == null || artifactId.isBlank() || version == null || version.isBlank())
            return null;
        String id = groupId + ":" + artifactId + ":" + version;
        return new MavenDependency(id);
    }

    /**
     * Returns the XML.
     */
    private XMLElement getXML()
    {
        if (_xml != null) return _xml;
        WebFile pomFile = getLocalPomFile();
        _mavenDependency.waitForLoad();
        String xmlString = pomFile.getText();
        if (xmlString == null) {
            System.err.println("MavenPomFile: Can't read pom file: " + pomFile);
            return null;
        }
        return _xml = XMLElement.readXmlFromString(xmlString);
    }

    /**
     * Returns the pom file URL in remote repository.
     */
    public WebURL getRemotePomUrl()
    {
        String pomUrlString = _mavenDependency.getRemoteJarUrlString().replace(".jar", ".pom");
        String classifier = _mavenDependency.getClassifier();
        if (classifier != null && !classifier.isBlank())
            pomUrlString = pomUrlString.replace('-' + classifier, "");
        return WebURL.getUrl(pomUrlString);
    }

    /**
     * Returns the local pom file, triggering load if missing.
     */
    public WebFile getLocalPomFile()
    {
        // Create local jar file
        String localPomPath = _mavenDependency.getLocalJarPath().replace(".jar", ".pom");
        String classifier = _mavenDependency.getClassifier();
        if (classifier != null && !classifier.isBlank())
            localPomPath = localPomPath.replace('-' + classifier, "");
        WebFile localPomFile = WebFile.createFileForPath(localPomPath, false);

        // If file doesn't exist, load it
        if (localPomFile != null && !localPomFile.getExists())
            _mavenDependency.loadPackageFiles();

        // Return
        return localPomFile;
    }

    /**
     * Downloads pom file from remote to local cache.
     */
    protected void loadPomFile() throws IOException
    {
        // Get remote and local jar file urls - if either is null, just return
        WebURL remotePomUrl = getRemotePomUrl();
        WebFile localPomFile = getLocalPomFile();
        if (remotePomUrl != null && localPomFile != null)
            MavenDependency.copyUrlToFile(remotePomUrl, localPomFile);
    }
}

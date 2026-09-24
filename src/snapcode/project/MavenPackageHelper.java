package snapcode.project;
import snap.util.*;
import snap.web.WebFile;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Provides helper methods for MavenPackage.
 */
class MavenPackageHelper {

    // The package and artifact
    private MavenPackage _mavenPackage;
    private MavenArtifact _mavenArtifact;
    private XMLElement _localPomFileXML;

    /**
     * Constructor.
     */
    public MavenPackageHelper(MavenPackage mavenPackage)
    {
        _mavenPackage = mavenPackage;
        _mavenArtifact = _mavenPackage.getMavenArtifact();
    }

    /**
     * Returns the local Jar file.
     */
    synchronized WebFile getLocalJarFile() throws IOException
    {
        // If local jar file already exists, just return
        String localJarFilePath = getLocalJarFilePath();
        WebFile localJarFile = WebFile.getFileForPath(localJarFilePath);
        if (localJarFile != null)
            return localJarFile;

        // Download file
        String remoteJarFileUrlString = getRemoteFileUrlStringForType("jar");
        URL remoteJarFileUrl = URI.create(remoteJarFileUrlString).toURL();
        downloadUrlToLocalPath(remoteJarFileUrl, Path.of(localJarFilePath));

        // Return local file which should exist now
        return WebFile.getFileForPath(localJarFilePath);
    }

    /**
     * Returns the local pom file.
     */
    synchronized WebFile getLocalPomFile() throws IOException
    {
        // If local pom file already exists, just return
        String localPomFilePath = getLocalPomFilePath();
        WebFile localPomFile = WebFile.getFileForPath(localPomFilePath);
        if (localPomFile != null)
            return localPomFile;

        // Download file
        String remotePomFileUrlString = getRemoteFileUrlStringForType("pom");
        URL remotePomFileUrl = URI.create(remotePomFileUrlString).toURL();
        downloadUrlToLocalPath(remotePomFileUrl, Path.of(localPomFilePath));

        // Return local file which should exist now
        return WebFile.getFileForPath(localPomFilePath);
    }

    /**
     * Returns the local jar file path.
     */
    String getLocalJarFilePath()  { return getLocalFilePathForType("jar"); }

    /**
     * Returns the local pom file path.
     */
    String getLocalPomFilePath()  { return getLocalFilePathForType("pom"); }

    /**
     * Returns the local file path string.
     */
    String getLocalFilePathForType(String fileType)
    {
        // Get local maven cache path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = SnapEnv.isWebVM ? "maven_cache" : ".m2/repository";
        String localMavenCachePath = FilePathUtils.getChildPath(homeDir, MAVEN_REPO_PATH);

        // Get relative file path
        String relativeFilePath = getRelativeFilePathForType(fileType);
        if (relativeFilePath == null)
            return null;

        // Return combined path
        return FilePathUtils.getChildPath(localMavenCachePath, relativeFilePath);
    }

    /**
     * Returns the remote file URL string.
     */
    String getRemoteFileUrlStringForType(String fileType)
    {
        String repositoryURL = _mavenArtifact.getRemoteRepositoryDirUrlString();
        String relativeFilePath = getRelativeFilePathForType(fileType);
        if (relativeFilePath == null)
            return null;
        return FilePathUtils.getChildPath(repositoryURL, relativeFilePath);
    }

    /**
     * Returns the relative file path (from any maven root).
     */
    private String getRelativeFilePathForType(String fileType)
    {
        String artifactPath = _mavenArtifact.getRelativeArtifactDirPath();

        // Build relative package jar path and return
        String version = _mavenPackage.getVersion();
        String versionPath = version != null ? FilePathUtils.getChildPath(artifactPath, version) : null;
        if (fileType == null)
            return versionPath;

        // Get filename
        String filenameSimple = _mavenPackage.getArtifactId() + '-' + version;
        String classifier = _mavenPackage.getClassifier();
        if (classifier != null && !classifier.isBlank() && fileType.equals("jar"))
            filenameSimple += '-' + classifier;
        String filename = filenameSimple + '.' + fileType;

        // Return path
        return FilePathUtils.getChildPath(versionPath, filename);
    }

    /**
     * Returns the properties.
     */
    Map<String, String> getProperties()
    {
        Map<String, String> properties = new HashMap<>();
        for (XMLElement propertyXML : getPropertyXMLs())
            properties.put(propertyXML.getName(), propertyXML.getValue());
        return properties;
    }

    /**
     * Returns dependency XML elements.
     */
    private List<XMLElement> getPropertyXMLs()
    {
        XMLElement xml = getLocalPomFileXML();
        XMLElement propertiesXML = xml != null ? xml.getElement("properties") : null;
        return propertiesXML != null ? propertiesXML.getElements() : Collections.emptyList();
    }

    /**
     * Returns dependencies.
     */
    List<MavenDependency> getDependencies()
    {
        List<XMLElement> dependencyXMLs = getDependencyXMLs();
        return ListUtils.mapNonNull(dependencyXMLs, this::getDependencyForXML);
    }

    /**
     * Returns dependency XML elements.
     */
    private List<XMLElement> getDependencyXMLs()
    {
        XMLElement xml = getLocalPomFileXML();
        XMLElement dependenciesXML = xml != null ? xml.getElement("dependencies") : null;
        List<XMLElement> dependencyXMLs = dependenciesXML != null ? dependenciesXML.getElements("dependency") : null;
        return dependencyXMLs != null ? dependencyXMLs : Collections.emptyList();
    }

    /**
     * Creates a maven dependency for dependency xml element.
     */
    private String getMavenIdForXML(XMLElement dependencyXML)
    {
        // Get XML elements for group, artifact, version
        XMLElement groupIdXML = dependencyXML.getElement("groupId");
        XMLElement artifactIdXML = dependencyXML.getElement("artifactId");
        XMLElement versionXML = dependencyXML.getElement("version");
        if (groupIdXML == null || artifactIdXML == null)
            return null;

        // Get groupId, artifactId, version
        String groupId = groupIdXML.getValue();
        String artifactId = artifactIdXML.getValue();
        String version = versionXML != null ? versionXML.getValue() : _mavenPackage.getVersion();
        if (groupId == null || groupId.isBlank() || artifactId == null || artifactId.isBlank() || version == null || version.isBlank())
            return null;

        // Create and return maven id
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Creates a maven package for dependency xml element.
     */
    private MavenDependency getDependencyForXML(XMLElement dependencyXML)
    {
        String mavenId = getMavenIdForXML(dependencyXML);
        return mavenId != null ? new MavenDependency(mavenId) : null;
    }

    /**
     * Returns the parent package, if available.
     */
    public MavenPackage getParentPackage()
    {
        XMLElement xml = getLocalPomFileXML();
        XMLElement parentXML = xml != null ? xml.getElement("parent") : null;
        String parentId = parentXML != null ? getMavenIdForXML(parentXML) : null;
        return parentId != null ? MavenPackage.getMavenPackageForId(parentId) : null;
    }

    /**
     * Returns the XML.
     */
    private XMLElement getLocalPomFileXML()
    {
        if (_localPomFileXML != null) return _localPomFileXML;

        String xmlString;
        try { xmlString = _mavenPackage.getLocalPomFile().getText(); }
        catch (IOException e) {
            System.err.println(getClass().getSimpleName() + ".getLocalPomFileXML: Can't read artifact file: " + e.getMessage());
            return null;
        }

        // Read and return
        try { return _localPomFileXML = XMLElement.readXmlFromString(xmlString); }
        catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".getLocalPomFileXML: Error reading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Downloads remote url to local.
     */
    public static void downloadUrlToLocalPath(URL remoteUrl, Path localPath) throws IOException
    {
        // Double-check after we "won" the CAS: maybe file appeared (e.g., created externally)
        if (Files.exists(localPath))
            return;

        // Make sure parent directory exists
        Path parent = localPath.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        // Download to sibling .download path
        Path downloadPath = localPath.resolveSibling(localPath.getFileName() + ".download");

        // Clean up any stale download file
        Files.deleteIfExists(downloadPath);

        // Actual download (basic URL stream). Replace with HttpClient if you want timeouts/headers.
        try (InputStream in = new BufferedInputStream(remoteUrl.openStream())) {
            Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Best effort cleanup
        catch (IOException e) {
            try { Files.deleteIfExists(downloadPath); }
            catch (IOException ignore) { }
            throw e;
        }

        // Atomic move into place (best-effort; may fall back depending on FS)
        try {
            Files.move(downloadPath, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }

        // Handle no atomic support: Do normal move
        catch (AtomicMoveNotSupportedException e) {
            Files.move(downloadPath, localPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

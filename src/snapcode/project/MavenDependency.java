package snapcode.project;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * This class represents a Maven dependency.
 */
public class MavenDependency extends BuildDependency {

    // The parent dependency
    private MavenDependency _parent;

    // The group id string
    private String _groupId;

    // The artifact id string
    private String _artifactId;

    // The version name
    private String _version;

    // The classifier
    private String _classifier;

    // The id string
    private String _id;

    // The maven artifact
    private MavenArtifact _mavenArtifact;

    // The maven package
    private MavenPackage _mavenPackage;

    // The transitive dependencies
    private List<MavenDependency> _dependencies;

    // Whether this dependency artifact is already in dependency tree
    private Boolean _redundant;

    // Constants for properties
    public static final String GroupId_Prop = "GroupId";
    public static final String ArtifactId_Prop = "ArtifactId";
    public static final String Version_Prop = "Version";
    public static final String Classifier_Prop = "Classifier";

    /**
     * Constructor.
     */
    public MavenDependency()
    {
        super();
    }

    /**
     * Constructor with maven id.
     */
    public MavenDependency(String mavenId)
    {
        super();
        setId(mavenId);
    }

    /**
     * Returns the parent.
     */
    public MavenDependency getParent()  { return _parent; }

    /**
     * Returns the type.
     */
    public Type getType()  { return Type.Maven; }

    /**
     * Returns id string.
     */
    @Override
    public String getId()
    {
        if (_id != null) return _id;

        // If any part is invalid, just return
        if (_groupId == null || _groupId.isBlank() || _artifactId == null || _artifactId.isBlank() || _version == null || _version.isBlank())
            return null;

        // Create id string and return
        _id = _groupId + ":" + _artifactId + ":" + _version;
        if (_classifier != null && !_classifier.isBlank())
            _id += ':' + _classifier;
        return _id;
    }

    /**
     * Sets properties for given id string.
     */
    public void setId(String aValue)
    {
        if (Objects.equals(aValue, _id)) return;
        _id = aValue;

        // Set Group, Name, Version
        String[] names = aValue.split(":");
        setGroupId(names.length > 0 ? names[0] : null);
        setArtifactId(names.length > 1 ? names[1] : null);
        setVersion(names.length > 2 ? names[2] : null);
        setClassifier(names.length > 3 ? names[3] : null);
    }

    /**
     * Returns the group id string.
     */
    public String getGroupId()  { return _groupId; }

    /**
     * Sets the group id string.
     */
    public void setGroupId(String aValue)
    {
        if (Objects.equals(aValue, _groupId)) return;
        handlePropChange();
        firePropChange(GroupId_Prop, _groupId, _groupId = aValue);
    }

    /**
     * Returns the artifact id string.
     */
    public String getArtifactId()  { return _artifactId; }

    /**
     * Sets the artifact id string.
     */
    public void setArtifactId(String aValue)
    {
        if (Objects.equals(aValue, _artifactId)) return;
        handlePropChange();
        firePropChange(ArtifactId_Prop, _artifactId, _artifactId = aValue);
    }

    /**
     * Returns the version name.
     */
    public String getVersion()  { return _version; }

    /**
     * Sets the version name.
     */
    public void setVersion(String aValue)
    {
        if (Objects.equals(aValue, _version)) return;
        handlePropChange();
        firePropChange(Version_Prop, _version, _version = aValue);
    }

    /**
     * Returns the classifier.
     */
    public String getClassifier()  { return _classifier; }

    /**
     * Sets the classifier.
     */
    public void setClassifier(String aValue)
    {
        if (Objects.equals(aValue, _classifier)) return;
        handlePropChange();
        firePropChange(Classifier_Prop, _classifier, _classifier = aValue);
    }

    /**
     * Returns the resolved id.
     */
    public String getResolvedId()
    {
        String resolvedGroupId = getResolvedGroupId();
        String resolvedVersion = getResolvedVersion();
        if (resolvedGroupId == getGroupId() && resolvedVersion == getVersion() || getId() == null)
            return getId();

        String resolvedId = resolvedGroupId + ":" + _artifactId + ":" + resolvedVersion;
        if (_classifier != null && !_classifier.isBlank())
            resolvedId += ':' + _classifier;
        return resolvedId;
    }

    /**
     * Returns the resolved group id.
     */
    private String getResolvedGroupId()
    {
        String groupId = getGroupId();
        if (groupId == null || groupId.isBlank())
            return null;
        if (groupId.equals("${project.groupId}")) {
            if (getParent() != null)
                groupId = getParent().getGroupId();
        }

        return groupId;
    }

    /**
     * Returns the resolved version.
     */
    private String getResolvedVersion()
    {
        String version = getVersion();

        switch (version) {
            case "${project.version}" -> { return getParent().getResolvedVersion(); }
            case "${junit.version}" -> { return getMavenArtifact().getLatestVersion(); }
            case "${hamcrestVersion}" -> { return "1.3"; }
        }

        // Oh, this is just frickin sad
        if (version.startsWith("[") || version.startsWith("("))
            version = version.substring(1);
        if (version.contains(","))
            version = version.substring(0, version.indexOf(","));
        if (version.contains("+"))
            version = version.replace("+", "");

        if (version != getVersion()) {
            MavenArtifact mavenArtifact = getMavenArtifact();
            return mavenArtifact.getLatestVersion();
        }

        return version;
    }

    /**
     * Returns the resolved group + artifact id.
     */
    public String getResolvedGroupArtifactId()
    {
        String resolvedGroupId = getResolvedGroupId();
        if (resolvedGroupId == null || _artifactId == null || _artifactId.isBlank())
            return null;
        return resolvedGroupId + ":" + _artifactId;
    }

    /**
     * Returns the maven artifact.
     */
    public MavenArtifact getMavenArtifact()
    {
        if (_mavenArtifact != null) return _mavenArtifact;
        String groupArtifactId = getResolvedGroupArtifactId();
        if (groupArtifactId == null)
            return null;
        return _mavenArtifact = MavenArtifact.getMavenArtifactForId(groupArtifactId);
    }

    /**
     * Returns the maven package.
     */
    public MavenPackage getMavenPackage()
    {
        if (_mavenPackage != null) return _mavenPackage;
        String mavenId = getResolvedId();
        if (mavenId == null)
            return null;
        return _mavenPackage = MavenPackage.getMavenPackageForId(mavenId);
    }

    /**
     * Returns the transitive dependencies.
     */
    public List<MavenDependency> getDependencies()
    {
        if (_dependencies != null) return _dependencies;
        MavenPackage mavenPackage = getMavenPackage();
        if (mavenPackage == null)
            return Collections.emptyList();
        List<MavenDependency> dependencies = mavenPackage.getDependencies();
        dependencies.forEach(dependency -> dependency._parent = this);
        return _dependencies = dependencies;
    }

    /**
     * Override to get class paths for project.
     */
    @Override
    protected String[] getClassPathsImpl()
    {
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null ? new String[] { mavenPackage.getClassPath() } : null;
    }

    /**
     * Returns whether dependency is already in this dependency tree.
     */
    public boolean isRedundant()
    {
        if (_redundant != null) return _redundant;

        String groupArtifactId = getResolvedGroupArtifactId();
        if (groupArtifactId == null)
            return _redundant = false;

        MavenDependency rootDependency = this;
        while (rootDependency._parent != null) rootDependency = rootDependency._parent;
        return _redundant = rootDependency.findDependencyForGroupArtifactId(groupArtifactId) != this;
    }

    /**
     * Returns the first dependency matching given group artifact id, searching recursively.
     */
    private MavenDependency findDependencyForGroupArtifactId(String groupArtifactId)
    {
        if (Objects.equals(getResolvedGroupArtifactId(), groupArtifactId))
            return this;

        for (MavenDependency dependency : getDependencies()) {
            MavenDependency dependencyForArtifactId = dependency.findDependencyForGroupArtifactId(groupArtifactId);
            if (dependencyForArtifactId != null)
                return dependencyForArtifactId;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the status.
     */
    public String getStatus()
    {
        if (isLoaded())
            return "Loaded";
        if (isLoading())
            return "Loading";
        return "Error";
    }

    /**
     * Returns the error.
     */
    public String getError()
    {
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null ? mavenPackage.getError() : null;
    }

    /**
     * Returns whether maven package is loaded.
     */
    public boolean isLoaded()
    {
        MavenArtifact mavenArtifact = getMavenArtifact();
        if (mavenArtifact == null || !mavenArtifact.isLoaded())
            return false;
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null && mavenPackage.isLoaded();
    }

    /**
     * Returns whether maven package is loading.
     */
    public boolean isLoading()
    {
        MavenArtifact mavenArtifact = getMavenArtifact();
        if (mavenArtifact != null && mavenArtifact.isLoading())
            return true;
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null && mavenPackage.isLoading();
    }

    /**
     * Pre-Loads files in background.
     */
    public void preloadPackageFiles()
    {
        CompletableFuture.runAsync(this::loadPackageFiles);
    }

    /**
     * Loads package files.
     */
    public synchronized void loadPackageFiles()
    {
        if (isLoaded())
            return;
        System.out.println("Loading maven dependency: " + getResolvedId());
        MavenArtifact mavenArtifact = getMavenArtifact();
        if (mavenArtifact != null)
            mavenArtifact.loadPackageFiles();
        MavenPackage mavenPackage = getMavenPackage();
        if (mavenPackage != null)
            mavenPackage.loadPackageFiles();
    }

    /**
     * Reloads files.
     */
    public void reloadPackageFiles()
    {
        MavenPackage mavenPackage = getMavenPackage();
        if (mavenPackage != null)
            mavenPackage.reloadPackageFiles();
    }

    /**
     * Called when any property changes.
     */
    private void handlePropChange()
    {
        _classPaths = null; _id = null; _redundant = null;
        _mavenArtifact = null;
        _mavenPackage = null;
        _dependencies = null;
    }

    /**
     * Override to support props for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        super.initProps(aPropSet);
        aPropSet.addPropNamed(GroupId_Prop, String.class);
        aPropSet.addPropNamed(ArtifactId_Prop, String.class);
        aPropSet.addPropNamed(Version_Prop, String.class);
        aPropSet.addPropNamed(Classifier_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String propName)
    {
        return switch (propName) {
            case GroupId_Prop -> getGroupId();
            case ArtifactId_Prop -> getArtifactId();
            case Version_Prop -> getVersion();
            case Classifier_Prop -> getClassifier();
            default -> super.getPropValue(propName);
        };
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public void setPropValue(String propName, Object aValue)
    {
        switch (propName) {
            case GroupId_Prop -> setGroupId(Convert.stringValue(aValue));
            case ArtifactId_Prop -> setArtifactId(Convert.stringValue(aValue));
            case Version_Prop -> setVersion(Convert.stringValue(aValue));
            case Classifier_Prop -> setClassifier(Convert.stringValue(aValue));
            default -> super.setPropValue(propName, aValue);
        }
    }

    /**
     * Override to add parent check.
     */
    @Override
    public boolean equals(Object anObj)  { return super.equals(anObj) && _parent == ((MavenDependency) anObj)._parent; }

    /**
     * Override to add parent.
     */
    @Override
    public int hashCode()  { return Objects.hash(_parent, getId()); }

    @Override
    public String toString()  { return "MavenDependency: " + getId(); }

    /**
     * Loads dependencies deep.
     */
    public static boolean loadDependenciesDeep(List<MavenDependency> dependencies, ActivityMonitor activityMonitor)
    {
        // Preload dependencies
        dependencies.forEach(MavenDependency::preloadPackageFiles);

        // Iterate over each and load if needed
        for (MavenDependency mavenDependency : dependencies) {

            // Load dependency
            if (!mavenDependency.isLoaded()) {
                if (activityMonitor != null)
                    activityMonitor.beginTask("Loading dependency: " + mavenDependency.getArtifactId(), 1);
                mavenDependency.loadPackageFiles();
                if (activityMonitor != null)
                    activityMonitor.endTask();
                if (!mavenDependency.isLoaded())
                    return false;
            }

            // Load child dependencies
            List<MavenDependency> childDependencies = mavenDependency.getDependencies();
            if (!childDependencies.isEmpty())
                loadDependenciesDeep(childDependencies, activityMonitor);
        }

        return true;
    }

    /**
     * Deletes all given dependencies.
     */
    public static void deleteDependencies(List<MavenDependency> dependencies)
    {
        for (MavenDependency mavenDependency : dependencies) {
            if (mavenDependency.isLoaded()) {
                deleteDependencies(mavenDependency.getDependencies());
                MavenArtifact mavenArtifact = mavenDependency.getMavenArtifact();
                if (mavenArtifact != null) {
                    WebFile localPackageDir = mavenArtifact.getLocalArtifactDir();
                    if (localPackageDir != null && localPackageDir.getExists()) {
                        System.out.println("Deleting package dir: " + localPackageDir.getPath());
                        localPackageDir.delete();
                    }
                }
            }
        }
    }
}

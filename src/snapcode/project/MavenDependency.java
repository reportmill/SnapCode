package snapcode.project;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a Maven dependency.
 */
public class MavenDependency extends BuildDependency {

    // The parent dependency
    private MavenDependency _parent;

    // The group name
    private String _group;

    // The product name
    private String _name;

    // The version name
    private String _version;

    // The classifier
    private String _classifier;

    // The id string
    private String _id;

    // The maven package
    private MavenPackage _mavenPackage;

    // The transitive dependencies
    private List<MavenDependency> _dependencies;

    // Whether this dependency artifact is already in dependency tree
    private Boolean _redundant;

    // Constants for properties
    public static final String Group_Prop = "Group";
    public static final String Name_Prop = "Name";
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
     * Constructor with maven id.
     */
    public MavenDependency(MavenDependency parent, MavenPackage mavenPackage)
    {
        super();
        _parent = parent;
        _mavenPackage = mavenPackage;
        setId(mavenPackage.getId());
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
        if (_group == null || _group.isBlank() || _name == null || _name.isBlank() || _version == null || _version.isBlank())
            return null;

        // Create id string and return
        _id = _group + ":" + _name + ":" + _version;
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
        setGroup(names.length > 0 ? names[0] : null);
        setName(names.length > 1 ? names[1] : null);
        setVersion(names.length > 2 ? names[2] : null);
        setClassifier(names.length > 3 ? names[3] : null);
    }

    /**
     * Returns the group name.
     */
    public String getGroup()  { return _group; }

    /**
     * Sets the group name.
     */
    public void setGroup(String aValue)
    {
        if (Objects.equals(aValue, _group)) return;
        handlePropChange();
        firePropChange(Group_Prop, _group, _group = aValue);
    }

    /**
     * Returns the product name.
     */
    public String getName()  { return _name; }

    /**
     * Sets the product name.
     */
    public void setName(String aValue)
    {
        if (Objects.equals(aValue, _name)) return;
        handlePropChange();
        firePropChange(Name_Prop, _name, _name = aValue);
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
     * Returns the artifact id.
     */
    public String getArtifactId()
    {
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null ? mavenPackage.getArtifactId() : null;
    }

    /**
     * Returns the maven package.
     */
    public MavenPackage getMavenPackage()
    {
        if (_mavenPackage != null) return _mavenPackage;
        String mavenId = getId();
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
        List<MavenPackage> transitiveDependencies = mavenPackage != null ? mavenPackage.getTransitiveDependencies() : null;
        if (transitiveDependencies == null)
            return null;
        return _dependencies = ListUtils.map(transitiveDependencies, dep -> new MavenDependency(this, dep));
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
     * Returns the local maven directory file.
     */
    public WebFile getLocalMavenDir()
    {
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null ? mavenPackage.getLocalMavenDir() : null;
    }

    /**
     * Returns the first dependency matching given artifact id.
     */
    public MavenDependency findDependencyForArtifactId(String artifactId)
    {
        MavenDependency rootDependency = this;
        while (rootDependency._parent != null) rootDependency = rootDependency._parent;
        return rootDependency.findDependencyForArtifactIdImpl(artifactId);
    }

    /**
     * Returns the first dependency matching given artifact id.
     */
    private MavenDependency findDependencyForArtifactIdImpl(String artifactId)
    {
        if (Objects.equals(getArtifactId(), artifactId))
            return this;
        for (MavenDependency dependency : getDependencies()) {
            MavenDependency dependencyForArtifactId = dependency.findDependencyForArtifactIdImpl(artifactId);
            if (dependencyForArtifactId != null)
                return dependencyForArtifactId;
        }

        // Return not found
        return null;
    }

    /**
     * Returns whether dependency is already in this dependency tree.
     */
    public boolean isRedundant()
    {
        if (_redundant != null) return _redundant;
        String artifactId = getArtifactId();
        return _redundant = artifactId != null && findDependencyForArtifactId(artifactId) != this;
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
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null && mavenPackage.isLoaded();
    }

    /**
     * Returns whether maven package is loading.
     */
    public boolean isLoading()
    {
        MavenPackage mavenPackage = getMavenPackage();
        return mavenPackage != null && mavenPackage.isLoading();
    }

    /**
     * Pre-Loads files in background.
     */
    public void preloadPackageFiles()
    {
        MavenPackage mavenPackage = getMavenPackage();
        if (mavenPackage != null)
            mavenPackage.preloadPackageFiles();
    }

    /**
     * Loads package files.
     */
    public synchronized void loadPackageFiles()
    {
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
        aPropSet.addPropNamed(Group_Prop, String.class);
        aPropSet.addPropNamed(Name_Prop, String.class);
        aPropSet.addPropNamed(Version_Prop, String.class);
        aPropSet.addPropNamed(Classifier_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        return switch (aPropName) {

            // Group, Name, Version, Classifier
            case Group_Prop -> getGroup();
            case Name_Prop -> getName();
            case Version_Prop -> getVersion();
            case Classifier_Prop -> getClassifier();

            // Do normal version
            default -> super.getPropValue(aPropName);
        };
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        switch (aPropName) {

            // Group, Name, Version, Classifier
            case Group_Prop -> setGroup(Convert.stringValue(aValue));
            case Name_Prop -> setName(Convert.stringValue(aValue));
            case Version_Prop -> setVersion(Convert.stringValue(aValue));
            case Classifier_Prop -> setClassifier(Convert.stringValue(aValue));

            // Do normal version
            default -> super.setPropValue(aPropName, aValue);
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
}

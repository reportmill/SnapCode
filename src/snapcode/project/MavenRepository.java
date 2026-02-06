package snapcode.project;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.Convert;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a maven package repository.
 */
public class MavenRepository extends PropObject {

    // The repository name
    private String _name;

    // The repository Url
    private String _url;

    // A cache of known repos
    private static Map<String, MavenRepository> _knownRepos = new HashMap<>();

    // Constants for properties
    public static final String Name_Prop = "Name";
    public static final String Url_Prop = "Url";

    /**
     * Constructor.
     */
    private MavenRepository(String name)
    {
        _name = name;
    }

    /**
     * Returns the repository name.
     */
    public String getName()  { return _name; }

    /**
     * Sets the repository name.
     */
    private void setName(String aValue)
    {
        _name = aValue;
    }

    /**
     * Returns the repository url.
     */
    public String getUrl()  { return _url; }

    /**
     * Sets the repository url.
     */
    private void setUrl(String aValue)
    {
        _url = aValue;
    }

    /**
     * Override to support props for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        super.initProps(aPropSet);
        aPropSet.addPropNamed(Name_Prop, String.class);
        aPropSet.addPropNamed(Url_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        return switch (aPropName) {
            case Name_Prop -> getName();
            case Url_Prop -> getUrl();
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
            case Name_Prop -> setName(Convert.stringValue(aValue));
            case Url_Prop -> setUrl(Convert.stringValue(aValue));
            default -> super.setPropValue(aPropName, aValue);
        }
    }

    /**
     * Returns a repository for given name.
     */
    public static MavenRepository getRepoForName(String aName)
    {
        MavenRepository repo = _knownRepos.get(aName);
        if (repo != null)
            return repo;
        repo = new MavenRepository(aName);
        _knownRepos.put(aName, repo);
        return repo;
    }
}

package javakit.project;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.ClassUtils;
import snap.util.Convert;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Objects;

/**
 * This class represents a build dependency (e.g. JarFile, Project, Maven Package).
 */
public abstract class BuildDependency extends PropObject {

    // An identifier string
    private String _id;

    // Constants for type
    public enum Type { JarFile, Project, Maven };

    // Constants for properties
    public static final String Id_Prop = "Id";

    /**
     * Constructor.
     */
    private BuildDependency()
    {
        super();
    }

    /**
     * Returns the type.
     */
    public abstract Type getType();

    /**
     * Returns the identifier string.
     */
    public String getId()  { return _id; }

    /**
     * Sets the identifier string.
     */
    public void setId(String aValue)
    {
        if (Objects.equals(aValue, _id)) return;
        firePropChange(Id_Prop, _id, _id = aValue);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        aPropSet.addPropNamed(Id_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        if (aPropName.equals(Id_Prop))
            return getId();
        return super.getPropValue(aPropName);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        if (aPropName.equals(Id_Prop))
            setId(Convert.stringValue(aValue));
        else super.setPropValue(aPropName, aValue);
    }

    /**
     * Returns a dependency for given path.
     */
    public static BuildDependency getDependencyForPath(Project aProject, String aPath)
    {
        // Get dependency for given path - just return null if not found
        Class<? extends BuildDependency> dependencyClass = getDependencyClassForPath(aProject, aPath);
        BuildDependency dependency = dependencyClass != null ? ClassUtils.newInstance(dependencyClass) : null;
        if (dependency == null)
            return null;

        // Get Id string
        String idStr = aPath;

        // If JarFile, reset Id string to relative path to jar file
        if (dependency instanceof JarFileDependency)
            idStr = ProjectUtils.getRelativePath(aProject, aPath);

        // If Project,  reset Id string to project name
        else if (dependency instanceof ProjectDependency)
            idStr = FilePathUtils.getFilename(aPath);

        // Set Id string
        dependency.setId(idStr);

        // Return
        return dependency;
    }

    /**
     * Returns a dependency for given path.
     */
    private static Class<? extends BuildDependency> getDependencyClassForPath(Project aProject, String aPath)
    {
        // If Maven dependency string, return MavenDependency
        if (aPath.contains(":")) {
            String[] names = aPath.split(":");
            if (names.length == 3)
                return MavenDependency.class;
        }

        // Get WebFile for path
        WebSite projSite = aProject.getSite();
        String relativePath = ProjectUtils.getRelativePath(aProject, aPath);
        WebFile snapFile = projSite.getFileForPath(relativePath);
        if (snapFile == null)
            return null;

        // If Jar, return JarFileDependency
        String snapFileType = snapFile.getType();
        if (snapFileType.equals("jar"))
            return JarFileDependency.class;

        // If Project dir, return Project
        if (snapFile.isDir())
            return ProjectDependency.class;

        // Return not found
        return null;
    }

    /**
     * This class represents a JarFile dependency.
     */
    public static class JarFileDependency extends BuildDependency {

        /**
         * Constructor.
         */
        public JarFileDependency()
        {
            super();
        }

        /**
         * Returns the type.
         */
        public Type getType()  { return Type.JarFile; }

        /**
         * Returns the path to the JarFile.
         */
        public String getJarFilePath()  { return getId(); }
    }

    /**
     * This class represents a Project dependency.
     */
    public static class ProjectDependency extends BuildDependency {

        /**
         * Constructor.
         */
        public ProjectDependency()
        {
            super();
        }

        /**
         * Returns the type.
         */
        public Type getType()  { return Type.JarFile; }

        /**
         * Returns the Project name.
         */
        public String getProjectName()  { return getId(); }
    }

    /**
     * This class represents a Maven dependency.
     */
    public static class MavenDependency extends BuildDependency {

        // The group name
        private String _group;

        // The product name
        private String _name;

        // The version name
        private String _version;

        /**
         * Constructor.
         */
        public MavenDependency()
        {
            super();
        }

        /**
         * Returns the type.
         */
        public Type getType()  { return Type.Maven; }

        @Override
        public void setId(String aValue)
        {
            if (Objects.equals(aValue, getId())) return;
            super.setId(aValue);

            // Set Group, Name, Version
            String[] names = aValue.split(":");
            _group = names[0];
            _name = names[1];
            _version = names[2];
        }

        /**
         * Returns the group name.
         */
        public String getGroup()  { return _group; }

        /**
         * Returns the product name.
         */
        public String getName()  { return _name; }

        /**
         * Returns the version name.
         */
        public String getVersion()  { return _version; }
    }
}

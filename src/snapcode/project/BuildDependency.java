package snapcode.project;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Objects;

/**
 * This class represents a build dependency (e.g. JarFile, Project, Maven Package).
 */
public abstract class BuildDependency extends PropObject {

    // The build file that declares this dependency
    protected BuildFile _buildFile;

    // The class paths for this dependency
    protected String[] _classPaths;

    // Constants for type
    public enum Type { JarFile, Project, Maven };

    /**
     * Constructor.
     */
    public BuildDependency()
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
    public abstract String getId();

    /**
     * Returns whether this dependency is resolved.
     */
    public boolean isResolved()  { return getClassPaths() != null; }

    /**
     * Returns the class paths for this dependency.
     */
    public String[] getClassPaths()
    {
        if (_classPaths != null) return _classPaths;
        return _classPaths = getClassPathsImpl();
    }

    /**
     * Returns the class paths for this dependency.
     */
    protected abstract String[] getClassPathsImpl();

    /**
     * Returns the class paths joined by given delimeter.
     */
    public String getClassPathsJoined(String aDelimeter)
    {
        String[] classPaths = getClassPaths();
        return classPaths != null ? String.join(aDelimeter, classPaths) : null;
    }

    /**
     * Standard equals implementation.
     */
    @Override
    public boolean equals(Object anObj)
    {
        if (this == anObj) return true;
        BuildDependency other = anObj instanceof BuildDependency ? (BuildDependency) anObj : null; if (other == null) return false;
        return Objects.equals(getId(), other.getId());
    }

    /**
     * Standard equals implementation.
     */
    @Override
    public int hashCode()  { return Objects.hash(getId()); }

    /**
     * Returns a dependency for given path.
     */
    public static BuildDependency.JarFileDependency getJarFileDependencyForPath(Project aProject, String aPath)
    {
        // Get WebFile for path
        WebSite projSite = aProject.getSite();
        String relativePath = ProjectUtils.getRelativePath(aProject, aPath);
        WebFile snapFile = projSite.getFileForPath(relativePath);
        if (snapFile == null)
            return null;

        // If Jar, return JarFileDependency
        String snapFileType = snapFile.getFileType();
        if (snapFileType.equals("jar")) {
            JarFileDependency jarFileDependency = new JarFileDependency();
            jarFileDependency.setJarPath(snapFile.getPath());
            return jarFileDependency;
        }

        // Return not found
        return null;
    }

    /**
     * This class represents a JarFile dependency.
     */
    public static class JarFileDependency extends BuildDependency {

        // The path to jar
        private String _jarPath;

        // Constants for properties
        public static final String JarPath_Prop = "JarPath";

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
        @Override
        public Type getType()  { return Type.JarFile; }

        /**
         * Override to return JarPath.
         */
        @Override
        public String getId()  { return getJarPath(); }

        /**
         * Returns the path to the JarFile.
         */
        public String getJarPath()  { return _jarPath; }

        /**
         * Sets the path to the JarFile.
         */
        public void setJarPath(String aValue)
        {
            if (Objects.equals(aValue, _jarPath)) return;
            _classPaths = null;
            firePropChange(JarPath_Prop, _jarPath, _jarPath = aValue);
        }

        /**
         * Override to get class paths from jar file path.
         */
        @Override
        protected String[] getClassPathsImpl()
        {
            if (_buildFile == null) return null;
            Project project = _buildFile.getProject();
            String jarFilePathRelative = getJarPath();
            String jarFilePathAbsolute = ProjectUtils.getAbsolutePath(project, jarFilePathRelative, true);
            return new String[] { jarFilePathAbsolute };
        }

        /**
         * Override to support props for this class.
         */
        @Override
        protected void initProps(PropSet aPropSet)
        {
            super.initProps(aPropSet);
            aPropSet.addPropNamed(JarPath_Prop, String.class);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public Object getPropValue(String aPropName)
        {
            if (aPropName.equals(JarPath_Prop))
                return getJarPath();
            return super.getPropValue(aPropName);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public void setPropValue(String aPropName, Object aValue)
        {
            if (aPropName.equals(JarPath_Prop))
                setJarPath(Convert.stringValue(aValue));
            else super.setPropValue(aPropName, aValue);
        }
    }

    /**
     * This class represents a Project dependency.
     */
    public static class ProjectDependency extends BuildDependency {

        // The project name
        private String _projectName;

        // Constants for properties
        public static final String ProjectName_Prop = "ProjectName";

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
        @Override
        public Type getType()  { return Type.Project; }

        /**
         * Override to return project name.
         */
        @Override
        public String getId()  { return getProjectName(); }

        /**
         * Returns the Project name.
         */
        public String getProjectName()  { return _projectName; }

        /**
         * Sets the Project name.
         */
        public void setProjectName(String aValue)
        {
            if (!Objects.equals(aValue, _projectName)) return;
            _classPaths = null;
            firePropChange(ProjectName_Prop, _projectName, _projectName = aValue);
        }

        /**
         * Override to get class paths for project.
         */
        @Override
        protected String[] getClassPathsImpl()
        {
            if (_buildFile == null) return null;
            Project thisProject = _buildFile.getProject();
            String projName = getProjectName();
            Project otherProject = thisProject.getProjectForName(projName);
            if (otherProject == null)
                return null;
            return otherProject.getRuntimeClassPaths();
        }

        /**
         * Override to support props for this class.
         */
        @Override
        protected void initProps(PropSet aPropSet)
        {
            super.initProps(aPropSet);
            aPropSet.addPropNamed(ProjectName_Prop, String.class);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public Object getPropValue(String aPropName)
        {
            if (aPropName.equals(ProjectName_Prop))
                return getProjectName();
            return super.getPropValue(aPropName);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public void setPropValue(String aPropName, Object aValue)
        {
            if (aPropName.equals(ProjectName_Prop))
                setProjectName(Convert.stringValue(aValue));
            else super.setPropValue(aPropName, aValue);
        }
    }
}

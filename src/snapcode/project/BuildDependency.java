package snapcode.project;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

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
     * Returns a dependency for given path.
     */
    public static BuildDependency getDependencyForPath(Project aProject, String aPath)
    {
        // If Maven dependency string, return MavenDependency
        if (aPath.contains(":")) {
            String[] names = aPath.split(":");
            if (names.length == 3) {
                MavenDependency mavenDependency = new MavenDependency();
                mavenDependency.setId(aPath);
                return mavenDependency;
            }
        }

        // Get WebFile for path
        WebSite projSite = aProject.getSite();
        String relativePath = ProjectUtils.getRelativePath(aProject, aPath);
        WebFile snapFile = projSite.getFileForPath(relativePath);
        if (snapFile == null)
            return null;

        // If Jar, return JarFileDependency
        String snapFileType = snapFile.getType();
        if (snapFileType.equals("jar")) {
            JarFileDependency jarFileDependency = new JarFileDependency();
            jarFileDependency.setJarPath(snapFile.getPath());
            return jarFileDependency;
        }

        // If Project dir, return Project
        if (snapFile.isDir()) {
            ProjectDependency projectDependency = new ProjectDependency();
            projectDependency.setProjectName(snapFile.getName());
            return projectDependency;
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

        // The repository url
        private String _repositoryURL;

        // The id string
        private String _id;

        // Constants for properties
        public static final String Group_Prop = "Group";
        public static final String Name_Prop = "Name";
        public static final String Version_Prop = "Version";
        public static final String RepositoryURL_Prop = "RepositoryURL";

        // Constants
        public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

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

        /**
         * Returns id string.
         */
        @Override
        public String getId()
        {
            if (_id != null) return _id;

            // If any part is invalid, just return
            if (_group == null || _name == null || _version == null)
                return null;
            if (_group.length() == 0 || _name.length() == 0 || _version.length() == 0)
                return null;

            // Create id string and return
            return _id = _group + ":" + _name + ":" + _version;
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
            _id = null; _classPaths = null;
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
            _id = null; _classPaths = null;
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
            _id = null; _classPaths = null;
            firePropChange(Version_Prop, _version, _version = aValue);
        }

        /**
         * Returns the repository name.
         */
        public String getRepositoryURL()  { return _repositoryURL; }

        /**
         * Sets the repository name.
         */
        public void setRepositoryURL(String aValue)
        {
            if (Objects.equals(aValue, _repositoryURL)) return;
            _classPaths = null;
            firePropChange(RepositoryURL_Prop, _repositoryURL, _repositoryURL = aValue);
        }

        /**
         * Returns the repository URL or default.
         */
        public String getRepositoryUrlOrDefault()
        {
            if (_repositoryURL != null)
                return _repositoryURL;
            if (_name != null && _name.toLowerCase().contains("reportmill"))
                return "https://reportmill.com/maven";
            return MAVEN_CENTRAL_URL;
        }

        /**
         *
         */
        public String getRepositoryDefaultName()
        {
            if (_name != null && _name.toLowerCase().contains("reportmill"))
                return "ReportMill";
            return "Maven Central";
        }

        /**
         * Override to get class paths for project.
         */
        @Override
        protected String[] getClassPathsImpl()
        {
            // Get local jar file (just return if it doesn't exist)
            WebFile localJarFile = getLocalJarFile();
            if (localJarFile == null)
                return null;

            // Return
            String localJarPath = getLocalJarPath();
            return new String[] { localJarPath };
        }

        /**
         * Returns the jar URL in remote repository.
         */
        public WebURL getRemoteJarURL()
        {
            String urlString = getRemoteJarUrlString();
            return WebURL.getURL(urlString);
        }

        /**
         * Returns the jar URL for local cache directory file.
         */
        public WebURL getLocalJarURL()
        {
            String urlString = getLocalJarPath();
            return WebURL.getURL(urlString);
        }

        /**
         * Returns the local Jar file, fetching it if missing.
         */
        protected WebFile getLocalJarFile()
        {
            // Get local Jar URL (just return if that can't be created)
            WebURL localJarURL = getLocalJarURL();
            if (localJarURL == null)
                return null;

            // Get local Jar file (just return if local file already exists)
            WebFile localJarFile = localJarURL.getFile();
            if (localJarFile != null)
                return localJarFile;

            // Copy maven package to local cache dir
            copyPackageFromRepositoryToLocal();

            // Return
            return localJarURL.getFile();
        }

        /**
         * Returns the remote Jar URL string.
         */
        public String getRemoteJarUrlString()
        {
            String repositoryURL = getRepositoryUrlOrDefault();
            String relativeJarPath = getRelativeJarPath();
            if (repositoryURL == null || relativeJarPath == null)
                return null;
            return FilePathUtils.getChild(repositoryURL, relativeJarPath);
        }

        /**
         * Returns the remote Jar URL string.
         */
        public String getLocalJarPath()
        {
            String homeDir = System.getProperty("user.home");
            String MAVEN_REPO_PATH = ".m2/repository";
            String localMavenCachePath = FilePathUtils.getChild(homeDir, MAVEN_REPO_PATH);
            String relativeJarPath = getRelativeJarPath();
            if (relativeJarPath == null)
                return null;
            return FilePathUtils.getChild(localMavenCachePath, relativeJarPath);
        }

        /**
         * Returns the relative Jar path (from any maven root).
         */
        private String getRelativeJarPath()
        {
            // Get parts - if any are null, return null
            String group = getGroup();
            String packageName = getName();
            String version = getVersion();
            if (group == null || group.length() == 0 ||
                packageName == null || packageName.length() == 0 ||
                version == null || version.length() == 0)
                return null;

            // Build relative package jar path and return
            String groupPath = '/' + group.replace(".", "/");
            String packagePath = FilePathUtils.getChild(groupPath, packageName);
            String versionPath = FilePathUtils.getChild(packagePath, version);
            String jarName = packageName + '-' + version + ".jar";
            return FilePathUtils.getChild(versionPath, jarName);
        }

        /**
         * Copies maven package to local cache dir.
         */
        private void copyPackageFromRepositoryToLocal()
        {
            // Get remote and local jar file urls - if either is null, just return
            WebURL remoteJarURL = getRemoteJarURL();
            WebURL localJarURL = getLocalJarURL();
            if (remoteJarURL == null || localJarURL == null)
                return;

            // Fetch file
            try { copyFileForURLs(remoteJarURL, localJarURL); }
            catch (Exception e) {
                e.printStackTrace();
            }
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
            aPropSet.addPropNamed(RepositoryURL_Prop, String.class);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public Object getPropValue(String aPropName)
        {
            switch (aPropName) {

                // Group, Name, Version, RepositoryURL
                case Group_Prop: return getGroup();
                case Name_Prop: return getName();
                case Version_Prop: return getVersion();
                case RepositoryURL_Prop: return getRepositoryURL();

                // Do normal version
                default: return super.getPropValue(aPropName);
            }
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public void setPropValue(String aPropName, Object aValue)
        {
            switch (aPropName) {

                // Group, Name, Version, RepositoryURL
                case Group_Prop: setGroup(Convert.stringValue(aValue)); break;
                case Name_Prop: setName(Convert.stringValue(aValue)); break;
                case Version_Prop: setVersion(Convert.stringValue(aValue)); break;
                case RepositoryURL_Prop: setRepositoryURL(Convert.stringValue(aValue)); break;

                // Do normal version
                default: super.setPropValue(aPropName, aValue);
            }
        }
    }

    /**
     * Copies a given source URL to given destination url.
     */
    private static void copyFileForURLs(WebURL sourceURL, WebURL destURL)
    {
        // Get bytes from source url
        byte[] sourceBytes = sourceURL.getBytes();
        if (sourceBytes == null || sourceBytes.length == 0)
            throw new RuntimeException("Couldn't download remote jar file: " + sourceURL.getString());

        // Create destination file
        WebSite destSite = destURL.getSite();
        String destFilePath = destURL.getPath();
        WebFile destFile = destSite.createFileForPath(destFilePath, false);

        // Set source bytes in destination file and save
        destFile.setBytes(sourceBytes);
        destFile.save();
    }
}

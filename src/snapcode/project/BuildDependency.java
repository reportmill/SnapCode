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

    // An identifier string
    private String _id;

    // The class paths for this dependency
    protected String[] _classPaths;

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

        // Clear ClassPaths
        _classPaths = null;
    }

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

        /**
         * Override to get class paths from jar file path.
         */
        @Override
        protected String[] getClassPathsImpl()
        {
            if (_buildFile == null) return null;
            Project project = _buildFile.getProject();
            String jarFilePathRelative = getJarFilePath();
            String jarFilePathAbsolute = ProjectUtils.getAbsolutePath(project, jarFilePathRelative, true);
            return new String[] { jarFilePathAbsolute };
        }
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
        private String _repositoryURL = MAVEN_CENTRAL_REPOSITORY_URL;

        // Constants for properties
        public static final String RepositoryURL_Prop = "RepositoryURL";

        // Constants
        public static final String MAVEN_CENTRAL_REPOSITORY_URL = "https://maven.org/maven2";

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
            _group = names.length > 0 ? names[0] : null;
            _name = names.length > 1 ? names[1] : null;
            _version = names.length > 2 ? names[2] : null;
        }

        /**
         * Sets the id.
         */
        private void updateId()
        {
            String group = _group != null ? _group : "";
            String name = _name != null ? _name : "";
            String version = _version != null ? _version : "";
            String id = group + ":" + name + ":" + version;
            setId(id);
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
            firePropChange(RepositoryURL_Prop, _repositoryURL, _repositoryURL = aValue);
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
            _group = aValue;
            updateId();
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
            _name = aValue;
            updateId();
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
            _version = aValue;
            updateId();
        }

        /**
         * Override to get class paths for project.
         */
        @Override
        protected String[] getClassPathsImpl()
        {
            // If local jar file doesn't exist, just return null
            WebURL localJarURL = getLocalJarURL();
            if (localJarURL == null)
                return null;
            WebFile localJarFile = localJarURL.getFile();

            // Copy maven package to local cache dir
            if (localJarFile == null) {
                copyPackageFromRepositoryToLocal();
                localJarFile = localJarURL.getFile();
            }

            // If still null, just return
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
         * Returns the remote Jar URL string.
         */
        public String getRemoteJarUrlString()
        {
            String repositoryURL = getRepositoryURL();
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
            // Get local jar file url
            WebURL localJarURL = getLocalJarURL();
            if (localJarURL == null)
                return;

            // If file exists, just return
            WebFile localJarFile = localJarURL.getFile();
            if (localJarFile != null)
                return;

            // Get remote jar file url
            WebURL remoteJarURL = getRemoteJarURL();
            if (remoteJarURL == null)
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
            aPropSet.addPropNamed(RepositoryURL_Prop, String.class);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public Object getPropValue(String aPropName)
        {
            if (aPropName.equals(RepositoryURL_Prop))
                return getRepositoryURL();
            return super.getPropValue(aPropName);
        }

        /**
         * Override to support props for this class.
         */
        @Override
        public void setPropValue(String aPropName, Object aValue)
        {
            if (aPropName.equals(RepositoryURL_Prop))
                setRepositoryURL(Convert.stringValue(aValue));
            else super.setPropValue(aPropName, aValue);
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

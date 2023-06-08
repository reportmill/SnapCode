package snapcode.project;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * Utility methods to help BuildFile.
 */
public class BuildFileUtils {

    /**
     * Returns the class path for given dependency.
     */
    public static String[] getClassPathsForProjectDependency(Project aProject, BuildDependency aDependency)
    {
        // Handle jar file
        if (aDependency instanceof BuildDependency.JarFileDependency) {
            BuildDependency.JarFileDependency jarFileDependency = (BuildDependency.JarFileDependency) aDependency;
            String jarFilePathRelative = jarFileDependency.getJarFilePath();
            String jarFilePathAbsolute = ProjectUtils.getAbsolutePath(aProject, jarFilePathRelative, true);
            return new String[] { jarFilePathAbsolute };
        }

        // Handle project
        if (aDependency instanceof BuildDependency.ProjectDependency) {
            BuildDependency.ProjectDependency projDependency = (BuildDependency.ProjectDependency) aDependency;
            String projName = projDependency.getProjectName();
            Project project = aProject.getProjectForName(projName);
            return project.getRuntimeClassPaths();
        }

        // Handle Maven package
        if (aDependency instanceof BuildDependency.MavenDependency)
            return getClassPathsForMavenDependency((BuildDependency.MavenDependency) aDependency);

        // Can't happen
        throw new RuntimeException("BuildDependency.getClassPathsForProjectDependency: Unknown type: " + aDependency);
    }

    /**
     * Returns the class path for given dependency.
     */
    public static String[] getClassPathsForMavenDependency(BuildDependency.MavenDependency mavenDependency)
    {
        // Get Repo path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = ".m2/repository";
        String repoPath = FilePathUtils.getChild(homeDir, MAVEN_REPO_PATH);

        // Get path to jar file
        String groupName = mavenDependency.getGroup();
        String packageName = mavenDependency.getName();
        String versionString = mavenDependency.getVersion();
        String jarPathAbsolute = getMavenPackageJarPath(repoPath, groupName, packageName, versionString);

        // If file doesn't exist, try to fetch it
        WebURL jarUrl = WebURL.getURL(jarPathAbsolute);
        if (jarUrl == null)
            return null;
        WebFile jarFile = jarUrl.getFile();
        if (jarFile == null) {
            try { BuildFileUtils.fetchMavenPackage(groupName, packageName, versionString, jarUrl); }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Return
        return new String[] { jarPathAbsolute };
    }

    /**
     * Fetches the maven package.
     */
    public static void fetchMavenPackage(String groupName, String packageName, String version, WebURL jarURL) throws Exception
    {
        // Get page URL
        String repository = "https://reportmill.com/maven";
        String urlString = getMavenPackageJarPath(repository, groupName, packageName, version);
        WebURL packageURL = WebURL.getURL(urlString);
        if (packageURL == null)
            throw new RuntimeException("Couldn't create URL for: " + urlString);

        // Get bytes
        byte[] packageBytes = packageURL.getBytes();
        if (packageBytes == null || packageBytes.length == 0)
            throw new RuntimeException("Couldn't download remote jar file: " + packageURL);

        // Write to jar file
        WebSite jarSite = jarURL.getSite();
        String jarPath = jarURL.getPath();
        WebFile jarFile = jarSite.createFileForPath(jarPath, false);
        jarFile.setBytes(packageBytes);
        jarFile.save();
    }

    /**
     * Returns the package path for given components.
     */
    public static String getMavenPackageJarPath(String repository, String groupName, String packageName, String version)
    {
        String groupPath = '/' + groupName.replace(".", "/");
        String packagePath = FilePathUtils.getChild(groupPath, packageName);
        String versionPath = FilePathUtils.getChild(packagePath, version);
        String jarName = packageName + '-' + version + ".jar";
        String jarPath = FilePathUtils.getChild(versionPath, jarName);
        return repository != null ? repository + jarPath : jarPath;
    }
}
